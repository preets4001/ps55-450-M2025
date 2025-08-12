package TriviaProject.src.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import TriviaProject.src.Common.Payload;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Base server-side thread that wraps a single client connection.
 * Provides input/output streams, handles Payloads, tracks client info and Room,
 * and supports clean disconnects. Fully Milestone 1 ready.
 */
public abstract class BaseServerThread extends Thread {

    protected boolean isRunning = false;
    protected ObjectOutputStream out; 
    protected Socket client; 
    private User user = new User();
    protected Room currentRoom;

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) throw new NullPointerException("Room argument can't be null");
        if (room == currentRoom) {
            System.out.println(String.format("ServerThread set to same room [%s], was this intentional?", room.getName()));
        }
        currentRoom = room;
    }

    public boolean isRunning() { return isRunning; }

    public void setClientId(long clientId) { this.user.setClientId(clientId); }

    public long getClientId() { return this.user.getClientId(); }

    protected void setClientName(String clientName) {
        this.user.setClientName(clientName);
        onInitialized();
    }

    public String getClientName() { return this.user.getClientName(); }

    public String getDisplayName() { return this.user.getDisplayName(); }

    protected abstract void info(String message);

    protected abstract void onInitialized();

    protected abstract void processPayload(Payload payload);

    protected boolean sendToClient(Payload payload) {
        if (!isRunning) return true;
        try {
            info("Sending to client: " + payload);
            out.writeObject(payload);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending to client — likely disconnected.");
            cleanup();
            return false;
        }
    }

    protected void disconnect() {
        if (!isRunning) return;
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt();
        cleanup();
    }

    @Override
    public void run() {
        info("Thread starting");
        try (
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream())
        ) {
            this.out = out;
            isRunning = true;

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (getClientName() == null || getClientName().isBlank()) {
                        info("Client name not received — disconnecting.");
                        disconnect();
                    }
                }
            }, 3000);

            Payload fromClient;
            while (isRunning) {
                try {
                    fromClient = (Payload) in.readObject();
                    if (fromClient != null) {
                        info("Received from client: " + fromClient);
                        processPayload(fromClient);
                    } else {
                        throw new IOException("Connection interrupted");
                    }
                } catch (ClassCastException | ClassNotFoundException cce) {
                    System.err.println("Error reading object: " + cce.getMessage());
                    cce.printStackTrace();
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        info("Thread interrupted during read (disconnect)");
                        break;
                    }
                    info("IO exception during read");
                    break;
                }
            }
        } catch (Exception e) {
            info("General Exception — client disconnected?");
            e.printStackTrace();
        } finally {
            if (currentRoom != null) {
                    currentRoom.handleDisconnect((ServerThread) this);
            }
            isRunning = false;
            info("Thread loop exited. Cleaning up connection.");
            cleanup();
        }
    }

    protected void cleanup() {
        info("ServerThread cleanup start");
        try {
            currentRoom = null;
            out.close();
            client.close();
            user.reset();
            info("Closed server-side socket");
        } catch (IOException e) {
            info("Client already closed");
        }
        info("ServerThread cleanup end");
    }
}
