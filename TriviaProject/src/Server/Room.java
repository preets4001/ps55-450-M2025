package TriviaProject.src.Server;

import java.util.concurrent.ConcurrentHashMap;

import TriviaProject.src.Client.TextFX.Color;
import TriviaProject.src.Common.*;
import TriviaProject.src.Client.TextFX;
import TriviaProject.src.Exceptions.*;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Represents a chat Room on the server.
 * Manages connected clients, relays messages, and handles create/join/leave logic.
 * Fully Milestone 1 ready (Lobby, room creation, multiple clients).
 */
public class Room implements AutoCloseable {
    private final String name;
    private volatile boolean isRunning = false;
    private final ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<>();

    public static final String LOBBY = "lobby";

    private void info(String message) {
        System.out.println(TextFX.colorize(
                String.format("Room[%s]: %s", name, message), Color.PURPLE));
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
        if (!isRunning) return;
        if (clientsInRoom.containsKey(client.getClientId())) {
            info("Client already exists in room");
            return;
        }
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        client.sendResetUserList();
        syncExistingClients(client);
        joinStatusRelay(client, true); // Notify all in room that client joined
    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) return;
        if (!clientsInRoom.containsKey(client.getClientId())) {
            info("Client not found in room");
            return;
        }
        ServerThread removedClient = clientsInRoom.get(client.getClientId());
        if (removedClient != null) {
            joinStatusRelay(removedClient, false); // Notify that client left
            clientsInRoom.remove(client.getClientId());
            autoCleanup();
        }
    }

    private void syncExistingClients(ServerThread incomingClient) {
        clientsInRoom.values().forEach(existing -> {
            if (existing.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendClientInfo(
                        existing.getClientId(),
                        existing.getClientName(),
                        RoomAction.JOIN, true);
                if (failedToSync) {
                    System.out.println("Removing disconnected " + existing.getDisplayName());
                    disconnect(existing);
                }
            }
        });
    }

    private void joinStatusRelay(ServerThread client, boolean didJoin) {
        clientsInRoom.values().removeIf(existing -> {
            String msg = String.format("Room[%s] %s %s the room",
                    getName(),
                    client.getClientId() == existing.getClientId() ? "You" : client.getDisplayName(),
                    didJoin ? "joined" : "left");
            long senderId = client == null ? Constants.DEFAULT_CLIENT_ID : client.getClientId();
            boolean failedSync = !existing.sendClientInfo(
                    client.getClientId(),
                    client.getClientName(),
                    didJoin ? RoomAction.JOIN : RoomAction.LEAVE);
            boolean failedMsg = !existing.sendMessage(senderId, msg);
            if (failedMsg || failedSync) {
                System.out.println("Removing disconnected " + existing.getDisplayName());
                disconnect(existing);
            }
            return failedMsg;
        });
    }

    protected synchronized void relay(ServerThread sender, String message) {
        if (!isRunning) return;

        String senderName = sender == null ? "Room[" + getName() + "]" : sender.getDisplayName();
        final long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        final String formattedMessage = String.format("%s: %s", senderName, message);

        info(String.format("Sending to %s clients: %s", clientsInRoom.size(), formattedMessage));

        clientsInRoom.values().removeIf(existing -> {
            boolean failed = !existing.sendMessage(senderId, formattedMessage);
            if (failed) {
                System.out.println("Removing disconnected " + existing.getDisplayName());
                disconnect(existing);
            }
            return failed;
        });
    }

    private synchronized void disconnect(ServerThread client) {
        if (!isRunning) return;
        ServerThread disconnecting = clientsInRoom.remove(client.getClientId());
        if (disconnecting != null) {
            clientsInRoom.values().removeIf(existing -> {
                boolean failed = !existing.sendClientInfo(disconnecting.getClientId(),
                        disconnecting.getClientName(), RoomAction.LEAVE);
                if (failed) disconnect(existing);
                return failed;
            });
            relay(null, disconnecting.getDisplayName() + " disconnected");
            disconnecting.disconnect();
        }
        autoCleanup();
    }

    protected synchronized void disconnectAll() {
        info("Disconnecting all clients...");
        if (!isRunning) return;
        clientsInRoom.values().removeIf(client -> {
            disconnect(client);
            return true;
        });
        info("All clients disconnected.");
    }

    private void autoCleanup() {
        if (!LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    @Override
    public void close() {
        if (!clientsInRoom.isEmpty()) {
            relay(null, "Room is shutting down, moving clients to Lobby");
            clientsInRoom.values().removeIf(client -> {
                try {
                    Server.INSTANCE.joinRoom(LOBBY, client);
                } catch (RoomNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            });
        }
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        clientsInRoom.clear();
        info("Closed");
    }

    // --- Handlers for client actions ---
    public void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.createRoom(roomName);
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            info("Room not found (shouldn't happen)");
            e.printStackTrace();
        } catch (DuplicateRoomException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format("Room %s already exists", roomName));
        }
    }

    public void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format("Room %s doesn't exist", roomName));
        }
    }

    protected synchronized void handleDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    protected synchronized void handleReverseText(ServerThread sender, String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        relay(sender, reversed);
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        relay(sender, text);
    }
}
