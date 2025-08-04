package common;


public enum PayloadType {
    CLIENT_CONNECT,
    CLIENT_ID,
    SYNC_CLIENT,
    DISCONNECT,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    REVERSE,
    MESSAGE,

    // ✅ Required for Milestone 2
    QUESTION,
    POINTS,
    ANSWER
}
