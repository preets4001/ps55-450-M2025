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
    ANSWER,
    ROOM_LIST, // list of rooms
    READY, // client to trigger themselves as ready, server to sync the related status of a
              // particular client
    SYNC_READY, // quiet version of READY, used to sync existing ready status of clients in a
                   // GameRoom
    RESET_READY, // trigger to tell the client to reset their whole local list's ready status
                    // (saves network requests)
    PHASE, // syncs current phase of session (used as a switch to only allow certain logic
              // to execute)
    TURN, // example of taking a turn and syncing a turn action
    SYNC_TURN, // quiet version of TURN, used to sync existing turn status of clients in a
                  // GameRoom
    RESET_TURN, // trigger to tell client to reset their local list turn status
    TIME, 
}
