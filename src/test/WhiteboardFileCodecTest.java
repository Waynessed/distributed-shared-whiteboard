package test;

import model.DrawingElement;
import model.DrawingTool;
import model.WhiteboardFileCodec;

import java.awt.Color;
import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardFileCodecTest {
    public static void main(String[] args) throws Exception {
        List<DrawingElement> original = new ArrayList<>();

        DrawingElement freeDraw = new DrawingElement(DrawingTool.FREE_DRAW, Color.BLUE, 5);
        freeDraw.addPoint(new Point(10, 20));
        freeDraw.addPoint(new Point(30, 40));
        original.add(freeDraw);

        DrawingElement rectangle = new DrawingElement(DrawingTool.RECTANGLE, Color.RED, 3);
        rectangle.addPoint(new Point(50, 60));
        rectangle.setEndPoint(new Point(100, 130));
        original.add(rectangle);

        original.add(DrawingElement.textElement(Color.BLACK, 4, new Point(75, 80), "hello whiteboard"));

        Path file = Files.createTempFile("whiteboard-codec-test", ".wbt");
        WhiteboardFileCodec.save(file, original);
        List<DrawingElement> loaded = WhiteboardFileCodec.load(file);
        Files.deleteIfExists(file);

        assertEquals(original.size(), loaded.size(), "element count");
        for (int i = 0; i < original.size(); i++) {
            assertElementEquivalent(original.get(i), loaded.get(i), "element " + i);
        }

        System.out.println("WhiteboardFileCodecTest passed.");
    }

    private static void assertElementEquivalent(DrawingElement expected, DrawingElement actual, String label) {
        assertEquals(expected.getTool(), actual.getTool(), label + " tool");
        assertEquals(expected.getColor(), actual.getColor(), label + " color");
        assertEquals(expected.getStrokeSize(), actual.getStrokeSize(), label + " stroke");
        assertEquals(expected.getText(), actual.getText(), label + " text");
        assertEquals(expected.getPoints().size(), actual.getPoints().size(), label + " point count");
        for (int i = 0; i < expected.getPoints().size(); i++) {
            assertEquals(expected.getPoints().get(i), actual.getPoints().get(i), label + " point " + i);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
