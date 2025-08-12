package server;

import client.TextFX;
import client.TextFX.Color;
import common.*;
import exception.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UCID: ps55
 * Summary: Manages rooms and incoming connections. Singleton server class.
 */
public class Server {
    public static final Server INSTANCE = new Server();

    private final int PORT = 3000;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private long clientIdCounter = 1; // Used for generating unique client IDs

    public synchronized long getNextClientId() {
        return clientIdCounter++;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            log("Listening on port " + PORT);

            createRoom(Room.LOBBY);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                log("Client connected!");
                ServerThread thread = new ServerThread(clientSocket, this::onServerThreadInitialized);
                thread.setClientId(getNextClientId()); // ← important
                thread.start();
            }
        } catch (IOException e) {
            log("Error accepting connection");
            e.printStackTrace();
        } catch (DuplicateRoomException ex) {
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                isRunning = false;
                serverSocket.close();
                log("Closing server socket");
            }
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        } finally {
            log("Server Stopped");
        }
    }

    private void log(String message) {
        System.out.println(TextFX.colorize("Server: " + message, Color.YELLOW));
    }

    private void onServerThreadInitialized(ServerThread thread) {
        try {
            joinRoom(Room.LOBBY, thread);
        } catch (RoomNotFoundException e) {
            log("Lobby not found: " + e.getMessage());
        }
    }

    public synchronized void createRoom(String roomName) throws DuplicateRoomException {
        if (rooms.containsKey(roomName)) {
            throw new DuplicateRoomException("Room already exists: " + roomName);
        }
        Room room = new Room(roomName);
        rooms.put(roomName, room);
        log("Created new Room: " + roomName);
    }

    public synchronized void joinRoom(String roomName, ServerThread client) throws RoomNotFoundException {
        Room room = rooms.get(roomName);
        if (room == null) {
            throw new RoomNotFoundException("Room not found: " + roomName);
        }

        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().removeClient(client);
        }

        room.addClient(client);
    }

    public synchronized void removeRoom(Room room) {
        if (room == null) return;
        rooms.remove(room.getName());
        log("Removed room: " + room.getName());
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
