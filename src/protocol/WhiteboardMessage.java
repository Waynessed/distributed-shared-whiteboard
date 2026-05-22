package protocol;

import model.DrawingElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String username;
    private final String text;
    private final DrawingElement drawingElement;
    private final List<DrawingElement> drawingState;
    private final List<String> users;
    private final boolean approved;
    private final boolean manager;

    private WhiteboardMessage(
            MessageType type,
            String username,
            String text,
            DrawingElement drawingElement,
            List<DrawingElement> drawingState,
            List<String> users,
            boolean approved,
            boolean manager
    ) {
        this.type = type;
        this.username = username;
        this.text = text;
        this.drawingElement = drawingElement;
        this.drawingState = drawingState == null ? null : new ArrayList<>(drawingState);
        this.users = users == null ? null : new ArrayList<>(users);
        this.approved = approved;
        this.manager = manager;
    }

    public static WhiteboardMessage join(String username) {
        return new WhiteboardMessage(MessageType.JOIN, username, null, null, null, null, false, false);
    }

    public static WhiteboardMessage joinAccepted(boolean manager) {
        return new WhiteboardMessage(MessageType.JOIN_ACCEPTED, null, null, null, null, null, true, manager);
    }

    public static WhiteboardMessage joinRejected(String reason) {
        return new WhiteboardMessage(MessageType.JOIN_REJECTED, null, reason, null, null, null, false, false);
    }

    public static WhiteboardMessage state(List<DrawingElement> drawingState) {
        return new WhiteboardMessage(MessageType.STATE, null, null, null, drawingState, null, false, false);
    }

    public static WhiteboardMessage draw(String username, DrawingElement drawingElement) {
        return new WhiteboardMessage(MessageType.DRAW, username, null, drawingElement, null, null, false, false);
    }

    public static WhiteboardMessage chat(String username, String text) {
        return new WhiteboardMessage(MessageType.CHAT, username, text, null, null, null, false, false);
    }

    public static WhiteboardMessage userList(List<String> users) {
        return new WhiteboardMessage(MessageType.USER_LIST, null, null, null, null, users, false, false);
    }

    public static WhiteboardMessage approvalRequest(String username) {
        return new WhiteboardMessage(MessageType.APPROVAL_REQUEST, username, null, null, null, null, false, false);
    }

    public static WhiteboardMessage approvalResponse(String username, boolean approved) {
        return new WhiteboardMessage(MessageType.APPROVAL_RESPONSE, username, null, null, null, null, approved, false);
    }

    public static WhiteboardMessage kick(String username) {
        return new WhiteboardMessage(MessageType.KICK, username, null, null, null, null, false, false);
    }

    public static WhiteboardMessage kicked(String reason) {
        return new WhiteboardMessage(MessageType.KICKED, null, reason, null, null, null, false, false);
    }

    public static WhiteboardMessage replaceBoard(List<DrawingElement> drawingState) {
        return new WhiteboardMessage(MessageType.REPLACE_BOARD, null, null, null, drawingState, null, false, false);
    }

    public static WhiteboardMessage serverShutdown(String reason) {
        return new WhiteboardMessage(MessageType.SERVER_SHUTDOWN, null, reason, null, null, null, false, false);
    }

    public static WhiteboardMessage error(String text) {
        return new WhiteboardMessage(MessageType.ERROR, null, text, null, null, null, false, false);
    }

    public MessageType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }

    public DrawingElement getDrawingElement() {
        return drawingElement;
    }

    public List<DrawingElement> getDrawingState() {
        return drawingState == null ? List.of() : new ArrayList<>(drawingState);
    }

    public List<String> getUsers() {
        return users == null ? List.of() : new ArrayList<>(users);
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isManager() {
        return manager;
    }
}
