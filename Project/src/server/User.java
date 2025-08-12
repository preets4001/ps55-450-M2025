package server;

public class User {
    private long clientId;
    private String clientName;
    private int points = 0;

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getDisplayName() {
        return clientName + "#" + clientId;
    }

    // ✅ ADD THIS
    public void addPoints(int p) {
        this.points += p;
    }

    // ✅ ADD THIS
    public int getPoints() {
        return points;
    }

    public void reset() {
        clientId = -1;
        clientName = null;
        points = 0;
    }
}
