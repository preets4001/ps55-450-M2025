package server;

import client.TextFX;
import client.TextFX.Color;
import common.*;
import exception.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Room implements AutoCloseable {

    private final String name;
    private volatile boolean isRunning = false;
    private final ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<>();

    public static final String LOBBY = "lobby";
    private final ScheduledExecutorService roundScheduler = Executors.newSingleThreadScheduledExecutor();

    // Game state
    private List<Question> questions = new ArrayList<>();
    private boolean gameStarted = false;
    private Map<Long, String> currentAnswers = new ConcurrentHashMap<>();
    private String currentPhase = "idle";
    private Question lastQuestion = null;  // ✅ Added for round scoring

    private void info(String message) {
        System.out.println(TextFX.colorize(String.format("Room[%s]: %s", name, message), Color.PURPLE));
    }

    public Room(String name) {
        this.name = name;
        isRunning = true;
        info("Created");
    }

    public String getName() {
        return this.name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning || clientsInRoom.containsKey(client.getClientId())) {
            return;
        }

        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        client.sendResetUserList();
        syncExistingClients(client);
        joinStatusRelay(client, true);

        if (!gameStarted && clientsInRoom.size() >= 1) {
            startGame();
        }
    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning || !clientsInRoom.containsKey(client.getClientId())) {
            return;
        }

        joinStatusRelay(client, false);
        clientsInRoom.remove(client.getClientId());
        autoCleanup();
    }

    private void syncExistingClients(ServerThread incomingClient) {
        clientsInRoom.values().forEach(existing -> {
            if (existing.getClientId() != incomingClient.getClientId()) {
                boolean failed = !incomingClient.sendClientInfo(
                        existing.getClientId(), existing.getClientName(), RoomAction.JOIN, true);
                if (failed) {
                    disconnect(existing);
                }
            }
        });
    }

    private void joinStatusRelay(ServerThread client, boolean didJoin) {
        clientsInRoom.values().removeIf(existing -> {
            String msg = String.format("Room[%s] %s %s the room", getName(),
                    client.getClientId() == existing.getClientId() ? "You" : client.getDisplayName(),
                    didJoin ? "joined" : "left");

            boolean syncFailed = !existing.sendClientInfo(client.getClientId(), client.getClientName(),
                    didJoin ? RoomAction.JOIN : RoomAction.LEAVE);
            boolean msgFailed = !existing.sendMessage(client.getClientId(), msg);

            if (syncFailed || msgFailed) {
                disconnect(existing);
            }
            return msgFailed;
        });
    }

    protected synchronized void relay(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }

        String senderName = sender == null ? "Room[" + getName() + "]" : sender.getDisplayName();
        long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        String formattedMessage = String.format("%s: %s", senderName, message);
        info("Sending: " + formattedMessage);

        clientsInRoom.values().removeIf(client -> {
            boolean failed = !client.sendMessage(senderId, formattedMessage);
            if (failed) {
                disconnect(client);
            }
            return failed;
        });
    }

    private synchronized void disconnect(ServerThread client) {
        if (!isRunning) {
            return;
        }
        clientsInRoom.remove(client.getClientId());
        relay(null, client.getDisplayName() + " disconnected");
        client.disconnect();
        autoCleanup();
    }

    protected synchronized void disconnectAll() {
        clientsInRoom.values().forEach(this::disconnect);
        info("All clients disconnected.");
    }

    private void autoCleanup() {
        if (!LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    @Override
    public void close() {
        relay(null, "Room is shutting down.");
        clientsInRoom.values().forEach(client -> {
            try {
                Server.INSTANCE.joinRoom(LOBBY, client);
            } catch (RoomNotFoundException e) {
                e.printStackTrace();
            }
        });
        clientsInRoom.clear();
        roundScheduler.shutdown();
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        info("Closed");
    }

    // -------------------- Game Logic --------------------
    private void loadQuestionsFromFile(String filePath) {
        System.out.println("Looking for file at :  " + new java.io.File(filePath).getAbsolutePath());
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String category = line.trim();
                String questionText = reader.readLine().trim();
                Map<String, String> options = new LinkedHashMap<>();
                for (int i = 0; i < 4; i++) {
                    String optionLine = reader.readLine();
                    if (optionLine != null && optionLine.contains("=")) {
                        String[] parts = optionLine.split("=", 2);
                        options.put(parts[0].trim(), parts[1].trim());
                    }
                }
                String correctAnswer = reader.readLine().trim();
                questions.add(new Question(category, questionText, options, correctAnswer));
            }
            info("Loaded " + questions.size() + " questions.");
        } catch (IOException e) {
            info("Failed to load questions: " + e.getMessage());
        }
    }

    private void startGame() {
        loadQuestionsFromFile("Question.txt");
        if (questions.isEmpty()) {
            relay(null, "❌ No Questions found. Game can't be started.");
            return;
        }
        gameStarted = true;
        relay(null, "🎮 Game started!");
        nextRound();
    }

    private void nextRound() {
        if (questions.isEmpty()) {
            relay(null, "✅ Game over! No more questions.");
            gameStarted = false;
            return;
        }

        Question question = questions.remove(new Random().nextInt(questions.size()));
        lastQuestion = question; // ✅ FIXED

        currentAnswers = new ConcurrentHashMap<>();
        clientsInRoom.keySet().forEach(id -> currentAnswers.put(id, null));

        currentPhase = "choosing";

        QAPayload payload = new QAPayload();
        payload.setCategory(question.getCategory());
        payload.setQuestion(question.getQuestion());
        payload.setOptions(question.getOptions());

        clientsInRoom.values().forEach(client -> client.sendToClient(payload));
        info("New round started: " + question.getQuestion());

        roundScheduler.schedule(() -> {
            relay(null, "⏰ Time's up! Moving to next question.");
            onRoundEnd();
        }, 30, TimeUnit.SECONDS);
    }

    private void onRoundEnd() {
        currentPhase = "reviewing";

        List<Map.Entry<Long, String>> correctPlayers = new ArrayList<>();
        String correctAnswer = lastQuestion != null ? lastQuestion.getCorrectAnswer() : null;

        for (Map.Entry<Long, String> entry : currentAnswers.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(correctAnswer)) {
                correctPlayers.add(entry);
            }
        }

        int maxPoints = 10;
        for (int i = 0; i < correctPlayers.size(); i++) {
            int points = Math.max(1, maxPoints - i * 3);
            long clientId = correctPlayers.get(i).getKey();
            ServerThread player = clientsInRoom.get(clientId);
            if (player != null) {
                player.getUser().addPoints(points);
                player.sendMessage(Constants.DEFAULT_CLIENT_ID, " You earned " + points + " points!");
            }
        }

        List<ServerThread> sortedPlayers = new ArrayList<>(clientsInRoom.values());
        sortedPlayers.sort((a, b) -> Integer.compare(b.getUser().getPoints(), a.getUser().getPoints()));

        StringBuilder scoreboard = new StringBuilder(" Current Scores:\n");
        for (ServerThread st : sortedPlayers) {
            scoreboard.append(st.getDisplayName()).append(": ")
                    .append(st.getUser().getPoints()).append(" points\n");
        }

        relay(null, scoreboard.toString().trim());

        if (questions.isEmpty()) {
            relay(null, "Game over! No more questions.");
            gameStarted = false;
        } else {
            roundScheduler.schedule(this::nextRound, 5, TimeUnit.SECONDS);
        }
    }

    private void onSessionEnd() {
        currentPhase = "ended";

        List<ServerThread> sortedPlayers = new ArrayList<>(clientsInRoom.values());
        sortedPlayers.sort((a, b) -> Integer.compare(b.getUser().getPoints(), a.getUser().getPoints()));

        StringBuilder finalBoard = new StringBuilder(" Game Over! Final Scores:\n");
        for (ServerThread player : sortedPlayers) {
            finalBoard.append(player.getDisplayName())
                    .append(": ")
                    .append(player.getUser().getPoints())
                    .append(" points\n");
        }

        relay(null, finalBoard.toString().trim());

        for (ServerThread player : clientsInRoom.values()) {
            player.getUser().reset();
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Game session ended. Your score has been reset.");
        }

        gameStarted = false;
        questions.clear();
        currentAnswers.clear();
        currentPhase = "idle";

        relay(null, " Ready check required to begin a new session.");
    }

    protected synchronized void handleAnswer(ServerThread sender, String answer) {
        if (!"choosing".equals(currentPhase)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, " You can't answer right now.");
            return;
        }

        currentAnswers.put(sender.getClientId(), answer);
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID, " Answer received: " + answer);
        info(sender.getDisplayName() + " answered: " + answer);

        boolean allAnswered = currentAnswers.values().stream().allMatch(Objects::nonNull);
        if (allAnswered) {
            info("All players answered. Ending round early.");
            onRoundEnd();
        }
    }

    public void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.createRoom(roomName);
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (DuplicateRoomException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Room already exists.");
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Room not found.");
        }
    }

    public void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Room not found.");
        }
    }

    protected synchronized void handleDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    protected synchronized void handleReverseText(ServerThread sender, String text) {
        relay(sender, new StringBuilder(text).reverse().toString());
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        relay(sender, text);
    }
}
