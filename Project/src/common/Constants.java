package common;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Defines shared constants for command parsing,
 * default client IDs, and spacing. Used by Client and Server.
 */
public abstract class Constants {
    public static final String COMMAND_TRIGGER = "/";
    public static final String SINGLE_SPACE = " ";

    // Client id for "no client" / system messages
    public static final long DEFAULT_CLIENT_ID = -1;

    // Label text used in ClientUI before a room is joined
    public static final String NOT_CONNECTED = "Not connected";

    // Default room name used in lobby checks
    public static final String DEFAULT_ROOM = "lobby";

    // Internal channel id for routing game-event messages to the GameEventsView
    public static final long GAME_EVENT_CHANNEL = -2;
}
