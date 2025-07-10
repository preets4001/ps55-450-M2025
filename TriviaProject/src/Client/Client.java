package TriviaProject.src.Client;

// Ps55 Date: 2025-07-08

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import TriviaProject.src.Client.TextFX.Color;
import TriviaProject.src.Common.*;
import TriviaProject.src.Server.User;

public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    private volatile boolean isRunning = true;
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<Long, User>();
    private User myUser = new User();

    private Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) return false;
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Could not connect to server");
            e.printStackTrace();
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;

        if (text.startsWith(Constants.COMMAND_TRIGGER)) {
            text = text.substring(1);

            if (isConnection("/" + text)) {
                if (myUser.getClientName() == null || myUser.getClientName().isEmpty()) {
                    System.out.println(TextFX.colorize("Please set your name via /name <name> before connecting", Color.RED));
                    return true;
                }
                String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
                connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                sendClientName(myUser.getClientName());
                wasCommand = true;

            } else if (text.startsWith(Command.NAME.command)) {
                text = text.replace(Command.NAME.command, "").trim();
                if (text.isEmpty()) {
                    System.out.println(TextFX.colorize("This command requires a name", Color.RED));
                    return true;
                }
                myUser.setClientName(text);
                System.out.println(TextFX.colorize("Name set to " + myUser.getClientName(), Color.YELLOW));
                wasCommand = true;

            } else if (text.equalsIgnoreCase(Command.LIST_USERS.command)) {
                System.out.println(TextFX.colorize("Known clients:", Color.CYAN));
                knownClients.forEach((key, value) -> {
                    System.out.println(TextFX.colorize(String.format("%s%s", value.getDisplayName(),
                            key == myUser.getClientId() ? " (you)" : ""), Color.CYAN));
                });
                wasCommand = true;

            } else if (Command.QUIT.command.equalsIgnoreCase(text)) {
                close();
                wasCommand = true;

            } else if (Command.DISCONNECT.command.equalsIgnoreCase(text)) {
                sendDisconnect();
                wasCommand = true;

            } else if (text.startsWith(Command.REVERSE.command)) {
                text = text.replace(Command.REVERSE.command, "").trim();
                sendReverse(text);
                wasCommand = true;

            } else if (text.startsWith(Command.CREATE_ROOM.command)) {
                text = text.replace(Command.CREATE_ROOM.command, "").trim();
                if (text.isEmpty()) {
                    System.out.println(TextFX.colorize("This command requires a room name", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.CREATE);
                wasCommand = true;

            } else if (text.startsWith(Command.JOIN_ROOM.command)) {
                text = text.replace(Command.JOIN_ROOM.command, "").trim();
                if (text.isEmpty()) {
                    System.out.println(TextFX.colorize("This command requires a room name", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.JOIN);
                wasCommand = true;

            } else if (text.startsWith(Command.LEAVE_ROOM.command)) {
                sendRoomAction(text, RoomAction.LEAVE);
                wasCommand = true;
            }
        }
        return wasCommand;
    }

    private void sendRoomAction(String roomName, RoomAction roomAction) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (roomAction) {
            case CREATE:
                payload.setPayloadType(PayloadType.ROOM_CREATE);
                break;
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                System.out.println(TextFX.colorize("Invalid room action", Color.RED));
                return;
        }
        sendToServer(payload);
    }

    private void sendReverse(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.REVERSE);
        sendToServer(payload);
    }

    private void sendDisconnect() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.DISCONNECT);
        sendToServer(payload);
    }

    private void sendMessage(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.MESSAGE);
        sendToServer(payload);
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush();
        } else {
            System.out.println("Not connected to server. Use `/connect host:port` first.");
        }
    }

    public void start() throws IOException {
        System.out.println("Client starting");
        CompletableFuture.runAsync(this::listenToInput).join();
    }

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject();
                if (fromServer != null) {
                    processPayload(fromServer);
                } else {
                    System.out.println("Server disconnected");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Connection dropped");
        } finally {
            closeServerConnection();
        }
    }

    private void processPayload(Payload payload) {
        switch (payload.getPayloadType()) {
            case CLIENT_CONNECT: break;
            case CLIENT_ID: processClientData(payload); break;
            case DISCONNECT: processDisconnect(payload); break;
            case MESSAGE: processMessage(payload); break;
            case REVERSE: processReverse(payload); break;
            case ROOM_CREATE: break; // for later
            case ROOM_JOIN: case ROOM_LEAVE: case SYNC_CLIENT: processRoomAction(payload); break;
            default: System.out.println(TextFX.colorize("Unhandled payload type", Color.YELLOW)); break;
        }
    }

    private void processClientData(Payload payload) {
        if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            System.out.println(TextFX.colorize("Client ID already set", Color.YELLOW));
        }
        myUser.setClientId(payload.getClientId());
        myUser.setClientName(((ConnectionPayload) payload).getClientName());
        knownClients.put(myUser.getClientId(), myUser);
        System.out.println(TextFX.colorize("Connected", Color.GREEN));
    }

    private void processDisconnect(Payload payload) {
        if (payload.getClientId() == myUser.getClientId()) {
            knownClients.clear();
            myUser.reset();
            System.out.println(TextFX.colorize("You disconnected", Color.RED));
        } else if (knownClients.containsKey(payload.getClientId())) {
            User disconnectedUser = knownClients.remove(payload.getClientId());
            System.out.println(TextFX.colorize(disconnectedUser.getDisplayName() + " disconnected", Color.RED));
        }
    }

    private void processRoomAction(Payload payload) {
        if (!(payload instanceof ConnectionPayload)) return;
        ConnectionPayload cp = (ConnectionPayload) payload;
        if (cp.getClientId() == Constants.DEFAULT_CLIENT_ID) {
            knownClients.clear();
            return;
        }
        switch (cp.getPayloadType()) {
            case ROOM_LEAVE:
                knownClients.remove(cp.getClientId());
                if (cp.getMessage() != null) System.out.println(TextFX.colorize(cp.getMessage(), Color.YELLOW));
                break;
            case ROOM_JOIN: case SYNC_CLIENT:
                if (cp.getMessage() != null) System.out.println(TextFX.colorize(cp.getMessage(), Color.GREEN));
                if (!knownClients.containsKey(cp.getClientId())) {
                    User user = new User();
                    user.setClientId(cp.getClientId());
                    user.setClientName(cp.getClientName());
                    knownClients.put(cp.getClientId(), user);
                }
                break;
            default:
                break;
        }
    }

    private void processMessage(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.BLUE));
    }

    private void processReverse(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.PURPLE));
    }

    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input");
            while (isRunning) {
                String userInput = si.nextLine();
                if (!processClientCommand(userInput)) {
                    sendMessage(userInput);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println("Client terminated");
    }

    private void closeServerConnection() {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (server != null) server.close(); } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
