package ui;

import model.DrawingElement;
import model.DrawingTool;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class DrawingCanvas extends JPanel {
    private final List<DrawingElement> elements = new ArrayList<>();
    private DrawingListener drawingListener;
    private DrawingElement activeElement;
    private DrawingTool currentTool = DrawingTool.FREE_DRAW;
    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean drawingEnabled = true;

    public DrawingCanvas() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 600));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                handleMouseReleased(event.getPoint());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setCurrentTool(DrawingTool currentTool) {
        this.currentTool = currentTool;
    }

    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
    }

    public void setCurrentStrokeSize(int currentStrokeSize) {
        this.currentStrokeSize = currentStrokeSize;
    }

    public void setDrawingListener(DrawingListener drawingListener) {
        this.drawingListener = drawingListener;
    }

    public void setDrawingEnabled(boolean drawingEnabled) {
        this.drawingEnabled = drawingEnabled;
        if (!drawingEnabled) {
            activeElement = null;
            repaint();
        }
    }

    public void addRemoteElement(DrawingElement element) {
        if (!elements.contains(element)) {
            elements.add(element);
            repaint();
        }
    }

    public void setElements(List<DrawingElement> newElements) {
        elements.clear();
        elements.addAll(newElements);
        activeElement = null;
        repaint();
    }

    public List<DrawingElement> getElementsCopy() {
        return new ArrayList<>(elements);
    }

    public void clear() {
        elements.clear();
        activeElement = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        for (DrawingElement element : elements) {
            element.draw(g);
        }
        if (activeElement != null && !elements.contains(activeElement)) {
            activeElement.draw(g);
        }
        g.dispose();
    }

    private void handleMousePressed(Point point) {
        if (!drawingEnabled) {
            return;
        }

        if (currentTool == DrawingTool.TEXT) {
            addText(point);
            return;
        }

        Color drawingColor = currentTool == DrawingTool.ERASER ? getBackground() : currentColor;
        activeElement = new DrawingElement(currentTool, drawingColor, currentStrokeSize);
        activeElement.addPoint(point);

        if (currentTool == DrawingTool.FREE_DRAW || currentTool == DrawingTool.ERASER) {
            elements.add(activeElement);
        }

        repaint();
    }

    private void handleMouseDragged(Point point) {
        if (!drawingEnabled || activeElement == null) {
            return;
        }

        if (currentTool == DrawingTool.FREE_DRAW || currentTool == DrawingTool.ERASER) {
            activeElement.addPoint(point);
        } else {
            activeElement.setEndPoint(point);
        }

        repaint();
    }

    private void handleMouseReleased(Point point) {
        if (!drawingEnabled || activeElement == null) {
            return;
        }

        if (currentTool == DrawingTool.FREE_DRAW || currentTool == DrawingTool.ERASER) {
            activeElement.addPoint(point);
            notifyDrawingFinished(activeElement);
        } else {
            activeElement.setEndPoint(point);
            elements.add(activeElement);
            notifyDrawingFinished(activeElement);
        }

        activeElement = null;
        repaint();
    }

    private void addText(Point point) {
        String text = JOptionPane.showInputDialog(this, "Enter text:");
        if (text == null || text.isBlank()) {
            return;
        }

        DrawingElement textElement = DrawingElement.textElement(currentColor, currentStrokeSize, point, text);
        elements.add(textElement);
        notifyDrawingFinished(textElement);
        repaint();
    }

    private void notifyDrawingFinished(DrawingElement element) {
        if (drawingListener != null) {
            drawingListener.drawingFinished(element);
        }
    }

    public interface DrawingListener {
        void drawingFinished(DrawingElement element);
    }
}
