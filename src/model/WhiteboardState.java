package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<DrawingElement> elements;
    private final LocalDateTime savedAt;

    public WhiteboardState(List<DrawingElement> elements) {
        this.elements = new ArrayList<>(elements);
        this.savedAt = LocalDateTime.now();
    }

    public List<DrawingElement> getElements() {
        return new ArrayList<>(elements);
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
