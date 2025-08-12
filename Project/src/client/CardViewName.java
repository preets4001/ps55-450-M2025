package client;

public enum CardViewName {
 CONNECT, USER_INFO, CHAT, ROOMS, CHAT_GAME_SCREEN, GAME_SCREEN, READY;

    public static boolean viewRequiresConnection(CardViewName check) {
        return check.ordinal() >= CardViewName.CHAT.ordinal();
    }
}


   
