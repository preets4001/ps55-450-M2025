package TriviaProject.src.Common;

import java.util.HashMap;

/**
 * UCID: your_ucid
 * Date: 2025-07-08
 * Summary: Enum for all supported client commands.
 * Used for mapping input strings to commands.
 */
public enum Command {
    QUIT("quit"),
    DISCONNECT("disconnect"),
    LOGOUT("logout"),
    LOGOFF("logoff"),
    CONNECT("connect"),
    REVERSE("reverse"),
    CREATE_ROOM("createroom"),
    LEAVE_ROOM("leaveroom"),
    JOIN_ROOM("joinroom"),
    NAME("name"),
    LIST_USERS("users");

    private static final HashMap<String, Command> BY_COMMAND = new HashMap<>();

    static {
        for (Command e : values()) {
            BY_COMMAND.put(e.command, e);
        }
    }

    public final String command;

    private Command(String command) {
        this.command = command;
    }

    public static Command stringToCommand(String command) {
        return BY_COMMAND.get(command);
    }
}
