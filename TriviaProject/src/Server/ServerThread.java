package TriviaProject.src.Server;

import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

import TriviaProject.src.Client.TextFX;
import TriviaProject.src.Client.TextFX.Color;
import TriviaProject.src.Common.*;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Represents one client on the server side. Handles Payloads,
 * relays messages to Room, supports commands like create/join/leave Room.
 * Fully ready for Milestone 1 + future extensions.
 */
public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete; // callback to Server

    @Override
    protected void info(String message) {
        System.out.println(TextFX.colorize(
                String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "Callback cannot be null");
        info("ServerThread created");
        this.client = myClient;
        this.onInitializationComplete = onInitializationComplete;
    }

    // ---------- Send*() Methods ----------

    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }

    protected boolean sendResetUserList() {
        return sendClientInfo(Constants.DEFAULT_CLIENT_ID, null, RoomAction.JOIN);
    }

    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action) {
        return sendClientInfo(clientId, clientName, action, false);
    }

    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action, boolean isSync) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN: payload.setPayloadType(PayloadType.ROOM_JOIN); break;
            case LEAVE: payload.setPayloadType(PayloadType.ROOM_LEAVE); break;
            default: break;
        }
        if (isSync) {
            payload.setPayloadType(PayloadType.SYNC_CLIENT);
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        return sendToClient(payload);
    }

    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }

    protected boolean sendMessage(long clientId, String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setMessage(message);
        payload.setClientId(clientId);
        return sendToClient(payload);
    }

    // ---------- Payload Handling ----------

    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;

            case DISCONNECT:
                if (currentRoom != null) {
                    currentRoom.handleDisconnect(this);
                }
                break;

            case MESSAGE:
                if (currentRoom != null) {
                    currentRoom.handleMessage(this, incoming.getMessage());
                }
                break;

            case REVERSE:
                if (currentRoom != null) {
                    currentRoom.handleReverseText(this, incoming.getMessage());
                }
                break;

            case ROOM_CREATE:
                if (currentRoom != null) {
                    currentRoom.handleCreateRoom(this, incoming.getMessage());
                }
                break;

            case ROOM_JOIN:
                if (currentRoom != null) {
                    currentRoom.handleJoinRoom(this, incoming.getMessage());
                }
                break;

            case ROOM_LEAVE:
                if (currentRoom != null) {
                    currentRoom.handleJoinRoom(this, Room.LOBBY);
                }
                break;

            default:
                System.out.println(TextFX.colorize("Unknown payload type received", Color.RED));
                break;
        }
    }

    @Override
    protected void onInitialized() {
        // Callback: tell the Server this thread is ready.
        onInitializationComplete.accept(this);
    }
}
