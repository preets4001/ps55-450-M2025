package TriviaProject.src.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import TriviaProject.src.Exceptions.*;
import TriviaProject.src.Client.TextFX;
import TriviaProject.src.Client.TextFX.Color;

/**
 * UCID:Ps55
 * Date: 2025-07-08
 
 */
public enum Server {
    INSTANCE; // Singleton instance

    private int port = 3000;

    // Thread-safe map of room name -> Room
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    private boolean isRunning = true;
    private long nextClientId = 0;

    private void info(String message) {
        System.out.println(TextFX.colorize("Server: " + message, Color.YELLOW));
    }

    private Server() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info("JVM is shutting down. Cleaning up...");
            shutdown();
        }));
    }

    /**
     * Gracefully disconnect all clients and clear rooms.
     */
    private void shutdown() {
        try {
            rooms.values().removeIf(room -> {
                room.disconnectAll();
                return true; // Remove all rooms
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the server socket, creates the Lobby, and accepts clients.
     */
    private void start(int port) {
        this.port = port;
        info("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            createRoom(Room.LOBBY); // Create the default Lobby

            while (isRunning) {
                info("Waiting for next client...");
                Socket incomingClient = serverSocket.accept();
                info("Client connected!");

                ServerThread serverThread = new ServerThread(incomingClient, this::onServerThreadInitialized);
                serverThread.start();
            }

        } catch (DuplicateRoomException e) {
            System.err.println(TextFX.colorize("Lobby already exists (shouldn't happen)", Color.RED));
        } catch (IOException e) {
            System.err.println(TextFX.colorize("Error accepting connection", Color.RED));
            e.printStackTrace();
        } finally {
            info("Closing server socket");
        }
    }

    /**
     * Called when a ServerThread is ready.
     * Assigns a unique ID and adds client to Lobby.
     */
    private void onServerThreadInitialized(ServerThread serverThread) {
        nextClientId = Math.max(++nextClientId, 1);
        serverThread.setClientId(nextClientId);
        serverThread.sendClientId();
        info("*" + serverThread.getDisplayName() + " initialized*");
        try {
            joinRoom(Room.LOBBY, serverThread);
            info("*" + serverThread.getDisplayName() + " added to Lobby*");
        } catch (RoomNotFoundException e) {
            info("*Error adding " + serverThread.getDisplayName() + " to Lobby*");
            e.printStackTrace();
        }
    }

    /**
     * Creates a new Room if it doesn't exist.
     */
    protected void createRoom(String name) throws DuplicateRoomException {
        String nameCheck = name.toLowerCase();
        if (rooms.containsKey(nameCheck)) {
            throw new DuplicateRoomException("Room " + name + " already exists");
        }
        Room room = new Room(name);
        rooms.put(nameCheck, room);
        info("Created new Room: " + name);
    }

    /**
     * Moves a client into the specified Room.
     */
    protected void joinRoom(String name, ServerThread client) throws RoomNotFoundException {
        String nameCheck = name.toLowerCase();
        if (!rooms.containsKey(nameCheck)) {
            throw new RoomNotFoundException("Room " + name + " not found");
        }
        Room currentRoom = client.getCurrentRoom();
        if (currentRoom != null) {
            info("Removing " + client.getDisplayName() + " from " + currentRoom.getName());
            currentRoom.removeClient(client);
        }
        Room next = rooms.get(nameCheck);
        next.addClient(client);
    }

    /**
     * Removes a Room from the map when empty.
     */
    protected void removeRoom(Room room) {
        rooms.remove(room.getName().toLowerCase());
        info("Removed Room: " + room.getName());
    }

    /**
     * Broadcasts a message to all Rooms (for future usage).
     */
    private synchronized void relayToAllRooms(ServerThread sender, String message) {
        final String formattedMessage = String.format("%s: %s",
                sender == null ? "Server" : sender.getDisplayName(), message);
        rooms.values().forEach(room -> room.relay(sender, formattedMessage));
    }

    /**
     * Example public method to broadcast everywhere.
     */
    public synchronized void broadcastMessageToAllRooms(ServerThread sender, String message) {
        relayToAllRooms(sender, message);
    }

    /**
     * Starts the server from the command line.
     */
    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = Server.INSTANCE;
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // Ignore and use default
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
