package TriviaProject.src.Common;

/**
 * UCID: ps55
 * Date: 7/5/2025
 * Summary: Defines types of Payloads sent between Client and Server.
 * Used to route different actions like connect, message, room operations, etc.
 */
public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passes name)
    CLIENT_ID,      // server sends client id back
    SYNC_CLIENT,    // silent syncing of clients in room
    DISCONNECT,     // disconnect action
    ROOM_CREATE, 
    ROOM_JOIN,
    ROOM_LEAVE,
    REVERSE,        // reverse message (future milestone)
    MESSAGE         // normal chat message
}
