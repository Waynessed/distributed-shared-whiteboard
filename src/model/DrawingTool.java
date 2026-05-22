package model;

public enum DrawingTool {
    FREE_DRAW("Free Draw"),
    ERASER("Eraser"),
    LINE("Line"),
    RECTANGLE("Rectangle"),
    OVAL("Circle/Oval"),
    TRIANGLE("Triangle"),
    TEXT("Text");

    private final String displayName;

    DrawingTool(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
