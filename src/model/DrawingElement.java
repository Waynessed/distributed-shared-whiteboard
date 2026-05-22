package model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DrawingElement implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();
    private final DrawingTool tool;
    private final Color color;
    private final int strokeSize;
    private final List<Point> points = new ArrayList<>();
    private String text;

    public DrawingElement(DrawingTool tool, Color color, int strokeSize) {
        this.tool = tool;
        this.color = color;
        this.strokeSize = strokeSize;
    }

    public static DrawingElement textElement(Color color, int strokeSize, Point location, String text) {
        DrawingElement element = new DrawingElement(DrawingTool.TEXT, color, strokeSize);
        element.addPoint(location);
        element.text = text;
        return element;
    }

    public DrawingTool getTool() {
        return tool;
    }

    public String getId() {
        return id;
    }

    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public Color getColor() {
        return color;
    }

    public int getStrokeSize() {
        return strokeSize;
    }

    public String getText() {
        return text;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void setEndPoint(Point point) {
        if (points.isEmpty()) {
            points.add(point);
        } else if (points.size() == 1) {
            points.add(point);
        } else {
            points.set(1, point);
        }
    }

    public void draw(Graphics2D graphics) {
        if (points.isEmpty()) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (tool) {
            case FREE_DRAW, ERASER -> drawPath(g);
            case LINE -> drawLine(g);
            case RECTANGLE -> drawRectangle(g);
            case OVAL -> drawOval(g);
            case TRIANGLE -> drawTriangle(g);
            case TEXT -> drawText(g);
        }

        g.dispose();
    }

    private void drawPath(Graphics2D g) {
        if (points.size() == 1) {
            Point point = points.get(0);
            g.fillOval(point.x, point.y, strokeSize, strokeSize);
            return;
        }

        for (int i = 1; i < points.size(); i++) {
            Point start = points.get(i - 1);
            Point end = points.get(i);
            g.drawLine(start.x, start.y, end.x, end.y);
        }
    }

    private void drawLine(Graphics2D g) {
        if (points.size() < 2) {
            return;
        }
        Point start = points.get(0);
        Point end = points.get(1);
        g.draw(new Line2D.Double(start, end));
    }

    private void drawRectangle(Graphics2D g) {
        Rectangle2D rectangle = boundsFromTwoPoints();
        if (rectangle != null) {
            g.draw(rectangle);
        }
    }

    private void drawOval(Graphics2D g) {
        Rectangle2D bounds = boundsFromTwoPoints();
        if (bounds != null) {
            g.draw(new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
        }
    }

    private void drawTriangle(Graphics2D g) {
        if (points.size() < 2) {
            return;
        }

        Point start = points.get(0);
        Point end = points.get(1);
        double midX = (start.x + end.x) / 2.0;

        Path2D triangle = new Path2D.Double();
        triangle.moveTo(midX, start.y);
        triangle.lineTo(start.x, end.y);
        triangle.lineTo(end.x, end.y);
        triangle.closePath();
        g.draw(triangle);
    }

    private void drawText(Graphics2D g) {
        if (text == null || text.isBlank()) {
            return;
        }
        Point location = points.get(0);
        int fontSize = Math.max(12, strokeSize * 5);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        g.drawString(text, location.x, location.y);
    }

    private Rectangle2D boundsFromTwoPoints() {
        if (points.size() < 2) {
            return null;
        }

        Point start = points.get(0);
        Point end = points.get(1);
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle2D.Double(x, y, width, height);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DrawingElement that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
