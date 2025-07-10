package TriviaProject.src.Server;

import TriviaProject.src.Common.Constants;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Represents a connected client on the server.
 * Stores clientId and clientName; supports resetting and displayName formatting.
 */
public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String username) {
        this.clientName = username;
    }

    /**
     * Returns display name in format: name#id
     */
    public String getDisplayName() {
        return String.format("%s#%s", this.clientName, this.clientId);
    }

    /**
     * Resets the User to default state.
     */
    public void reset() {
        this.clientId = Constants.DEFAULT_CLIENT_ID;
        this.clientName = null;
    }
}
