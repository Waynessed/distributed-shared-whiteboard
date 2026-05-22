package model;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class WhiteboardFileCodec {
    private static final String HEADER = "SHARED_WHITEBOARD_TEXT_V1";
    private static final String FIELD_SEPARATOR = "\t";

    private WhiteboardFileCodec() {
    }

    public static void save(Path path, List<DrawingElement> elements) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (DrawingElement element : elements) {
                writer.write("element");
                writer.write(FIELD_SEPARATOR);
                writer.write(element.getTool().name());
                writer.write(FIELD_SEPARATOR);
                writer.write(toHexColor(element.getColor()));
                writer.write(FIELD_SEPARATOR);
                writer.write(Integer.toString(element.getStrokeSize()));
                writer.write(FIELD_SEPARATOR);
                writer.write(formatPoints(element.getPoints()));
                writer.write(FIELD_SEPARATOR);
                writer.write(encodeText(element.getText()));
                writer.newLine();
            }
        }
    }

    public static List<DrawingElement> load(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new IOException("Not a shared whiteboard text file.");
            }

            List<DrawingElement> elements = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                elements.add(parseElement(line));
            }
            return elements;
        }
    }

    private static DrawingElement parseElement(String line) throws IOException {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 6 || !"element".equals(fields[0])) {
            throw new IOException("Invalid drawing element line.");
        }

        DrawingTool tool = DrawingTool.valueOf(fields[1]);
        Color color = parseColor(fields[2]);
        int strokeSize = Integer.parseInt(fields[3]);
        List<Point> points = parsePoints(fields[4]);
        String text = decodeText(fields[5]);

        if (tool == DrawingTool.TEXT) {
            if (points.isEmpty()) {
                throw new IOException("Text element has no position.");
            }
            return DrawingElement.textElement(color, strokeSize, points.get(0), text);
        }

        DrawingElement element = new DrawingElement(tool, color, strokeSize);
        for (Point point : points) {
            element.addPoint(point);
        }
        return element;
    }

    private static String formatPoints(List<Point> points) {
        List<String> formatted = new ArrayList<>();
        for (Point point : points) {
            formatted.add(point.x + "," + point.y);
        }
        return String.join(";", formatted);
    }

    private static List<Point> parsePoints(String value) throws IOException {
        List<Point> points = new ArrayList<>();
        if (value.isBlank()) {
            return points;
        }

        String[] pairs = value.split(";");
        for (String pair : pairs) {
            String[] coordinates = pair.split(",", -1);
            if (coordinates.length != 2) {
                throw new IOException("Invalid point: " + pair);
            }
            points.add(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
        }
        return points;
    }

    private static String toHexColor(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color parseColor(String value) {
        return new Color(Integer.parseInt(value.substring(1), 16));
    }

    private static String encodeText(String text) {
        if (text == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
