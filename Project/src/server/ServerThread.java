package server;

import client.TextFX;
import client.TextFX.Color;
import common.*;

import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete;

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
            case JOIN -> payload.setPayloadType(PayloadType.ROOM_JOIN);
            case LEAVE -> payload.setPayloadType(PayloadType.ROOM_LEAVE);
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
            case CLIENT_CONNECT -> setClientName(((ConnectionPayload) incoming).getClientName().trim());
            case DISCONNECT -> {
                if (currentRoom != null) currentRoom.handleDisconnect(this);
            }
            case MESSAGE -> {
                if (currentRoom != null) currentRoom.handleMessage(this, incoming.getMessage());
            }
            case REVERSE -> {
                if (currentRoom != null) currentRoom.handleReverseText(this, incoming.getMessage());
            }
            case ROOM_CREATE -> {
                if (currentRoom != null) currentRoom.handleCreateRoom(this, incoming.getMessage());
            }
            case ROOM_JOIN -> {
                if (currentRoom != null) currentRoom.handleJoinRoom(this, incoming.getMessage());
            }
            case ROOM_LEAVE -> {
                if (currentRoom != null) currentRoom.handleJoinRoom(this, Room.LOBBY);
            }
            case ANSWER -> {
                if (currentRoom != null) currentRoom.handleAnswer(this, incoming.getMessage());
            }
            default -> System.out.println(TextFX.colorize("Unknown payload type received", Color.RED));
        }
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}
