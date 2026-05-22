package protocol;

public enum MessageType {
    JOIN,
    LEAVE,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    STATE,
    DRAW,
    CHAT,
    CHAT_HISTORY,
    NEW_BOARD,
    USER_LIST,
    APPROVAL_REQUEST,
    APPROVAL_RESPONSE,
    KICK,
    KICKED,
    REPLACE_BOARD,
    SERVER_SHUTDOWN,
    ERROR
}
