package ui;

import model.DrawingTool;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public class WhiteBoardFrame extends JFrame {
    private static final Color[] PALETTE = {
            Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY,
            Color.WHITE, Color.RED, Color.PINK, Color.ORANGE,
            Color.YELLOW, Color.GREEN, new Color(0, 128, 0), Color.CYAN,
            Color.BLUE, new Color(0, 0, 128), Color.MAGENTA, new Color(128, 0, 128)
    };

    private final DrawingCanvas canvas = new DrawingCanvas();

    public WhiteBoardFrame() {
        this("Standalone Shared Whiteboard - Phase 1A", true);
    }

    public WhiteBoardFrame(String title, boolean showClearButton) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createToolBar(showClearButton), BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(createColorPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    private JToolBar createToolBar(boolean showClearButton) {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JComboBox<DrawingTool> toolSelector = new JComboBox<>(DrawingTool.values());
        toolSelector.addActionListener(event -> {
            DrawingTool selectedTool = (DrawingTool) toolSelector.getSelectedItem();
            if (selectedTool != null) {
                canvas.setCurrentTool(selectedTool);
            }
        });

        Integer[] strokeSizes = {1, 2, 3, 5, 8, 12, 16, 24};
        JComboBox<Integer> strokeSelector = new JComboBox<>(strokeSizes);
        strokeSelector.setSelectedItem(3);
        strokeSelector.addActionListener(event -> {
            Integer selectedSize = (Integer) strokeSelector.getSelectedItem();
            if (selectedSize != null) {
                canvas.setCurrentStrokeSize(selectedSize);
            }
        });

        toolbar.add(new JLabel("Tool: "));
        toolbar.add(toolSelector);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Stroke: "));
        toolbar.add(strokeSelector);

        if (showClearButton) {
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(event -> canvas.clear());
            toolbar.addSeparator();
            toolbar.add(clearButton);
        }

        return toolbar;
    }

    private JPanel createColorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        panel.add(new JLabel("Colours:"), BorderLayout.WEST);

        JPanel colorGrid = new JPanel(new GridLayout(2, 8, 4, 4));
        for (Color color : PALETTE) {
            JButton button = new JButton();
            button.setBackground(color);
            button.setOpaque(true);
            button.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            button.setPreferredSize(new Dimension(28, 28));
            button.setToolTipText(colorName(color));
            button.addActionListener(event -> canvas.setCurrentColor(color));
            colorGrid.add(button);
        }

        panel.add(colorGrid, BorderLayout.CENTER);
        return panel;
    }

    private String colorName(Color color) {
        if (Color.BLACK.equals(color)) return "Black";
        if (Color.DARK_GRAY.equals(color)) return "Dark gray";
        if (Color.GRAY.equals(color)) return "Gray";
        if (Color.LIGHT_GRAY.equals(color)) return "Light gray";
        if (Color.WHITE.equals(color)) return "White";
        if (Color.RED.equals(color)) return "Red";
        if (Color.PINK.equals(color)) return "Pink";
        if (Color.ORANGE.equals(color)) return "Orange";
        if (Color.YELLOW.equals(color)) return "Yellow";
        if (Color.GREEN.equals(color)) return "Green";
        if (Color.CYAN.equals(color)) return "Cyan";
        if (Color.BLUE.equals(color)) return "Blue";
        if (Color.MAGENTA.equals(color)) return "Magenta";
        return "Custom colour";
    }
}
