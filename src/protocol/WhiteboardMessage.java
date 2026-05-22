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

    private WhiteboardMessage(
            MessageType type,
            String username,
            String text,
            DrawingElement drawingElement,
            List<DrawingElement> drawingState
    ) {
        this.type = type;
        this.username = username;
        this.text = text;
        this.drawingElement = drawingElement;
        this.drawingState = drawingState == null ? null : new ArrayList<>(drawingState);
    }

    public static WhiteboardMessage join(String username) {
        return new WhiteboardMessage(MessageType.JOIN, username, null, null, null);
    }

    public static WhiteboardMessage state(List<DrawingElement> drawingState) {
        return new WhiteboardMessage(MessageType.STATE, null, null, null, drawingState);
    }

    public static WhiteboardMessage draw(String username, DrawingElement drawingElement) {
        return new WhiteboardMessage(MessageType.DRAW, username, null, drawingElement, null);
    }

    public static WhiteboardMessage error(String text) {
        return new WhiteboardMessage(MessageType.ERROR, null, text, null, null);
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
}
