package client;

// Ps55 Date: 2025-07-08
import common.RoomAction;

import client.Interfaces.IConnectionEvents;
import client.Interfaces.IMessageEvents;
import client.Interfaces.IPhaseEvent;
import client.Interfaces.IRoomEvents;
import client.TextFX.Color;
import common.Command;
import common.ConnectionPayload;
import common.Constants;
import common.Payload;
import common.PayloadType;
import common.ReadyPayload;
import common.RoomAction;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import server.User;

public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    // "/connect 1.2.3.4:12345" or "/connect localhost:12345"
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    private volatile boolean isRunning = true;

    // Known clients in current room/session
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();

    // UI listeners (wired dynamically by instanceof on register)
    private final List<IMessageEvents> messageListeners = new ArrayList<>();
    private final List<IConnectionEvents> connectionListeners = new ArrayList<>();
    private final List<IRoomEvents> roomListeners = new ArrayList<>();
    private final List<IPhaseEvent> phaseListeners = new ArrayList<>();

    // "Me"
    private User myUser = new User();

    private Client() {
        System.out.println("Client Created");
    }

    /* =========================
     * Public helpers used by UI
     * ========================= */
    public boolean isConnected() {
        if (server == null) return false;
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /** UI uses this to decide if it can show chat/game panels. */
    public boolean isMyClientIdSet() {
        return myUser.getClientId() != Constants.DEFAULT_CLIENT_ID;
    }

    public boolean isMyClientId(long id) {
        return myUser.getClientId() == id;
    }

    /** Returns display name for a client id, or a fallback. */
    public String getDisplayNameFromId(long id) {
        if (id == Constants.DEFAULT_CLIENT_ID) return "System";
        if (myUser.getClientId() == id) {
            String dn = myUser.getDisplayName();
            if (dn != null && !dn.isBlank()) return dn;
            String cn = myUser.getClientName();
            if (cn != null && !cn.isBlank()) return cn;
            return "You";
        }
        User u = knownClients.get(id);
        if (u != null) {
            String dn = u.getDisplayName();
            if (dn != null && !dn.isBlank()) return dn;
            String cn = u.getClientName();
            if (cn != null && !cn.isBlank()) return cn;
        }
        return "Client-" + id;
    }

    /**
     * Views call this: we accept any object and register it to the appropriate listener lists
     * via instanceof checks (so ChatView / ClientUI / others just call registerCallback(this)).
     */
    public synchronized void registerCallback(Object events) {
        if (events == null) return;
        if (events instanceof IMessageEvents m && !messageListeners.contains(m)) messageListeners.add(m);
        if (events instanceof IConnectionEvents c && !connectionListeners.contains(c)) connectionListeners.add(c);
        if (events instanceof IRoomEvents r && !roomListeners.contains(r)) roomListeners.add(r);
        if (events instanceof IPhaseEvent p && !phaseListeners.contains(p)) phaseListeners.add(p);
    }

    /* =========================
     * Public UI entry points
     * ========================= */

    /** UI connect method that also sets the username and sends it to the server once connected. */
    public boolean connect(String host, int port, String username) {
        if (username != null && !username.isBlank()) {
            myUser.setClientName(username.trim());
        }
        boolean ok = connectInternal(host, port);
        if (ok) {
            try {
                if (myUser.getClientName() != null && !myUser.getClientName().isBlank()) {
                    sendClientName(myUser.getClientName());
                }
            } catch (IOException e) {
                System.out.println("Failed to send client name: " + e.getMessage());
            }
        }
        return ok;
    }

    /** Public so ChatView and others can call it. */
    public void sendMessage(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.MESSAGE);
        sendToServer(payload);
    }

    /** Public so UI can trigger a clean disconnect. */
    public void sendDisconnect() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.DISCONNECT);
        sendToServer(payload);
    }

    /** Public for room actions from menus/UI. */
   // Client.java
public void sendRoomAction(String roomName, RoomAction roomAction) throws IOException {
    Payload payload = new Payload();
    payload.setMessage(roomName);
    switch (roomAction) {
        case CREATE: payload.setPayloadType(PayloadType.ROOM_CREATE); 
        break;
        case JOIN:   payload.setPayloadType(PayloadType.ROOM_JOIN);   
        break;
        case LEAVE:  payload.setPayloadType(PayloadType.ROOM_LEAVE); 
         break;
        case LIST:   payload.setPayloadType(PayloadType.ROOM_LIST);   
        break; // requires #3
    }
    sendToServer(payload);
}


    /* =========================
     * Legacy console flows
     * ========================= */

    private boolean connectInternal(String address, int port) {
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
                connectInternal(parts[0].trim(), Integer.parseInt(parts[1].trim()));
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

    private void sendReverse(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.REVERSE);
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

    /* =========================
     * Networking + dispatch
     * ========================= */

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
            case CLIENT_CONNECT:
                // handled by server, no UI event here
                break;
            case CLIENT_ID:
                processClientData(payload);
                break;
            case DISCONNECT:
                processDisconnect(payload);
                break;
            case MESSAGE:
                processMessage(payload);
                break;
            case REVERSE:
                processReverse(payload);
                break;
            case ROOM_CREATE:
                // server will typically follow up with SYNC_CLIENT/ROOM_JOIN
                break;
            case ROOM_JOIN:
            case ROOM_LEAVE:
            case SYNC_CLIENT:
                processRoomAction(payload);
                break;
            // Future MS3 events like timers, phase changes, points can be added here:
            // case TIMER: ...
            // case PHASE: ...
            default:
                System.out.println(TextFX.colorize("Unhandled payload type: " + payload.getPayloadType(), Color.YELLOW));
                break;
        }
    }
    // at top of file imports:
// import common.ReadyPayload;  // make sure this exists in your project

public void sendReady(boolean isReady) throws IOException {
    ReadyPayload payload = new ReadyPayload();
    payload.setReady(isReady);
    payload.setPayloadType(PayloadType.READY); // ensure this enum value exists
    sendToServer(payload);
}


    private void processClientData(Payload payload) {
        if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            System.out.println(TextFX.colorize("Client ID already set", Color.YELLOW));
        }
        myUser.setClientId(payload.getClientId());
        if (payload instanceof ConnectionPayload cp) {
            myUser.setClientName(cp.getClientName());
        }
        knownClients.put(myUser.getClientId(), myUser);
        System.out.println(TextFX.colorize("Connected", Color.GREEN));

        // Notify UI listeners
        for (IConnectionEvents l : connectionListeners) {
            try { l.onReceiveClientId(myUser.getClientId()); } catch (Throwable ignored) {}
        }
    }

    private void processDisconnect(Payload payload) {
        long id = payload.getClientId();
        if (id == myUser.getClientId()) {
            knownClients.clear();
            myUser.reset();
            System.out.println(TextFX.colorize("You disconnected", Color.RED));
        } else if (knownClients.containsKey(id)) {
            User disconnectedUser = knownClients.remove(id);
            System.out.println(TextFX.colorize(disconnectedUser.getDisplayName() + " disconnected", Color.RED));
        }

        // Notify UI listeners
        for (IConnectionEvents l : connectionListeners) {
            try { l.onClientDisconnect(id); } catch (Throwable ignored) {}
        }
    }

    private void processRoomAction(Payload payload) {
        if (!(payload instanceof ConnectionPayload)) return;
        ConnectionPayload cp = (ConnectionPayload) payload;

        if (cp.getClientId() == Constants.DEFAULT_CLIENT_ID) {
            knownClients.clear();
            return;
        }

        // Try to pull room name from the message text (server formatted)
        String roomName = extractRoomName(cp.getMessage());

        switch (cp.getPayloadType()) {
            case ROOM_LEAVE:
                knownClients.remove(cp.getClientId());
                if (cp.getMessage() != null) {
                    System.out.println(TextFX.colorize(cp.getMessage(), Color.YELLOW));
                }
                // notify UI
                for (IRoomEvents l : roomListeners) {
                    try { l.onRoomAction(cp.getClientId(), roomName, /*isJoin*/ false, /*isQuiet*/ false); }
                    catch (Throwable ignored) {}
                }
                break;

            case ROOM_JOIN:
            case SYNC_CLIENT:
                if (cp.getMessage() != null) {
                    System.out.println(TextFX.colorize(cp.getMessage(), Color.GREEN));
                }
                if (!knownClients.containsKey(cp.getClientId())) {
                    User user = new User();
                    user.setClientId(cp.getClientId());
                    user.setClientName(cp.getClientName());
                    knownClients.put(cp.getClientId(), user);
                }
                // notify UI
                for (IRoomEvents l : roomListeners) {
                    try { l.onRoomAction(cp.getClientId(), roomName, /*isJoin*/ true, /*isQuiet*/ false); }
                    catch (Throwable ignored) {}
                }
                break;

            default:
                break;
        }
    }

    private void processMessage(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.BLUE));
        // Notify message listeners
        for (IMessageEvents l : messageListeners) {
            try { l.onMessageReceive(payload.getClientId(), payload.getMessage()); } catch (Throwable ignored) {}
        }
    }

    private void processReverse(Payload payload) {
        System.out.println(TextFX.colorize(payload.getMessage(), Color.PURPLE));
        for (IMessageEvents l : messageListeners) {
            try { l.onMessageReceive(payload.getClientId(), payload.getMessage()); } catch (Throwable ignored) {}
        }
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

    /* =========================
     * Helpers
     * ========================= */

    /**
     * Attempts to extract a room name from a human-readable message like
     * "You joined the Room lobby" or "Bob left the Room my-room".
     * Returns null if not found (UI tolerates null).
     */
    private String extractRoomName(String message) {
        if (message == null) return null;
        // common formats: "... Room <name>", case-insensitive
        Pattern p = Pattern.compile("(?i)\\broom\\s+([A-Za-z0-9 _\\-]+)\\b");
        Matcher m = p.matcher(message);
        if (m.find()) {
            String room = m.group(1).trim();
            // normalize common punctuation at end
            if (room.endsWith(".") || room.endsWith("!") || room.endsWith(",")) {
                room = room.substring(0, room.length() - 1).trim();
            }
            return room;
        }
        return null;
    }
}
