package common;


/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Subclass of Payload that includes clientName.
 * Used for syncing client names with the Server.
 */
public class ConnectionPayload extends Payload {
    private String clientName;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String toString() {
        return super.toString() +
                String.format(" ClientName: [%s]", getClientName());
    }
}
