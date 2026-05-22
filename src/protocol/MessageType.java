package protocol;

public enum MessageType {
    JOIN,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    STATE,
    DRAW,
    USER_LIST,
    APPROVAL_REQUEST,
    APPROVAL_RESPONSE,
    KICK,
    KICKED,
    ERROR
}
