package ui;

import model.DrawingTool;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

public class WhiteBoardFrame extends JFrame {
    private static final Color[] PALETTE = {
            Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY,
            Color.WHITE, Color.RED, Color.PINK, Color.ORANGE,
            Color.YELLOW, Color.GREEN, new Color(0, 128, 0), Color.CYAN,
            Color.BLUE, new Color(0, 0, 128), Color.MAGENTA, new Color(128, 0, 128)
    };

    private final DrawingCanvas canvas = new DrawingCanvas();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JButton kickButton = new JButton("Kick User");
    private final JButton quitButton = new JButton("Quit");
    private final JMenu fileMenu = new JMenu("File");
    private final JTextArea chatArea = new JTextArea(8, 22);
    private final JTextField chatInput = new JTextField();
    private Consumer<String> kickListener;
    private Consumer<String> chatListener;
    private Runnable newBoardListener;
    private Runnable openBoardListener;
    private Runnable saveBoardListener;
    private Runnable saveAsBoardListener;
    private Runnable closeBoardListener;
    private Runnable quitListener;

    public WhiteBoardFrame() {
        this("Standalone Shared Whiteboard - Phase 1A", true);
    }

    public WhiteBoardFrame(String title, boolean showClearButton) {
        super(title);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());

        add(createToolBar(showClearButton), BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(createUserPanel(), BorderLayout.EAST);
        add(createChatPanel(), BorderLayout.WEST);
        add(createColorPanel(), BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                quit();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public DrawingCanvas getCanvas() {
        return canvas;
    }

    public void setUsers(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
    }

    public void setManagerMode(boolean managerMode) {
        kickButton.setEnabled(managerMode);
        fileMenu.setEnabled(managerMode);
        quitButton.setText(managerMode ? "Close Board" : "Leave");
    }

    public void setKickListener(Consumer<String> kickListener) {
        this.kickListener = kickListener;
    }

    public void setChatListener(Consumer<String> chatListener) {
        this.chatListener = chatListener;
    }

    public void setFileActionListeners(
            Runnable newBoardListener,
            Runnable openBoardListener,
            Runnable saveBoardListener,
            Runnable saveAsBoardListener,
            Runnable closeBoardListener
    ) {
        this.newBoardListener = newBoardListener;
        this.openBoardListener = openBoardListener;
        this.saveBoardListener = saveBoardListener;
        this.saveAsBoardListener = saveAsBoardListener;
        this.closeBoardListener = closeBoardListener;
    }

    public void setQuitListener(Runnable quitListener) {
        this.quitListener = quitListener;
    }

    public void addChatMessage(String message) {
        chatArea.append(message + System.lineSeparator());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
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

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        fileMenu.setEnabled(false);
        addMenuItem("New", () -> runIfSet(newBoardListener));
        addMenuItem("Open", () -> runIfSet(openBoardListener));
        addMenuItem("Save", () -> runIfSet(saveBoardListener));
        addMenuItem("Save As", () -> runIfSet(saveAsBoardListener));
        addMenuItem("Close", () -> runIfSet(closeBoardListener));
        menuBar.add(fileMenu);
        return menuBar;
    }

    private void addMenuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> action.run());
        fileMenu.add(item);
    }

    private void runIfSet(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(150, 0));

        kickButton.setEnabled(false);
        kickButton.addActionListener(event -> {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null && kickListener != null) {
                kickListener.accept(selectedUser);
            }
        });
        quitButton.addActionListener(event -> quit());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        buttonPanel.add(kickButton);
        buttonPanel.add(quitButton);

        panel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(230, 0));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatInput.addActionListener(event -> sendChatMessage());

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(event -> sendChatMessage());

        JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(new JLabel("Chat"), BorderLayout.NORTH);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty() && chatListener != null) {
            chatListener.accept(text);
            chatInput.setText("");
        }
    }

    private void quit() {
        if (quitListener != null) {
            quitListener.run();
        } else {
            dispose();
        }
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
