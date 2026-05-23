package client;

import model.DrawingElement;
import model.WhiteboardFileCodec;
import model.WhiteboardState;
import protocol.MessageType;
import protocol.WhiteboardMessage;
import ui.DrawingCanvas;
import ui.WhiteBoardFrame;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardClient {
    private final String username;
    private final WhiteBoardFrame frame;
    private final DrawingCanvas canvas;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private File currentFile;
    private boolean managerClient;
    private volatile boolean closing;

    public WhiteboardClient(String username, WhiteBoardFrame frame) {
        this.username = username;
        this.frame = frame;
        this.canvas = frame.getCanvas();
        canvas.setDrawingEnabled(false);
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        send(WhiteboardMessage.join(username));
        frame.setKickListener(this::kickUser);
        frame.setChatListener(this::sendChat);
        frame.setQuitListener(this::quitWhiteboard);
        frame.setFileActionListeners(
                this::newBoard,
                this::openBoard,
                this::saveBoard,
                this::saveBoardAs,
                this::exportPng,
                this::closeBoard
        );

        Thread listenerThread = new Thread(this::listenForMessages, "whiteboard-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenForMessages() {
        try {
            while (true) {
                Object object = input.readObject();
                if (object instanceof WhiteboardMessage message) {
                    handleMessage(message);
                }
            }
        } catch (EOFException exception) {
            if (!closing) {
                showError("Disconnected from the server.");
            }
        } catch (IOException | ClassNotFoundException exception) {
            if (!closing) {
                showError("Network error: " + exception.getMessage());
            }
        }
    }

    private void handleMessage(WhiteboardMessage message) {
        if (message.getType() == MessageType.JOIN_ACCEPTED) {
            SwingUtilities.invokeLater(() -> {
                managerClient = message.isManager();
                canvas.setDrawingListener(this::sendDrawing);
                canvas.setDrawingEnabled(true);
                frame.setManagerMode(managerClient);
            });
        } else if (message.getType() == MessageType.JOIN_REJECTED) {
            showError(message.getText());
            close();
            SwingUtilities.invokeLater(frame::dispose);
        } else if (message.getType() == MessageType.STATE) {
            SwingUtilities.invokeLater(() -> canvas.setElements(message.getDrawingState()));
        } else if (message.getType() == MessageType.DRAW) {
            DrawingElement element = message.getDrawingElement();
            SwingUtilities.invokeLater(() -> canvas.addRemoteElement(element));
        } else if (message.getType() == MessageType.CHAT) {
            SwingUtilities.invokeLater(() -> frame.addChatMessage(message.getUsername() + ": " + message.getText()));
        } else if (message.getType() == MessageType.CHAT_HISTORY) {
            SwingUtilities.invokeLater(() -> frame.setChatHistory(message.getChatHistory()));
        } else if (message.getType() == MessageType.USER_LIST) {
            SwingUtilities.invokeLater(() -> frame.setUsers(message.getUsers()));
        } else if (message.getType() == MessageType.APPROVAL_REQUEST) {
            askManagerForApproval(message.getUsername());
        } else if (message.getType() == MessageType.KICKED) {
            showInfo(message.getText());
            close();
            SwingUtilities.invokeLater(frame::dispose);
        } else if (message.getType() == MessageType.SERVER_SHUTDOWN) {
            close();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        frame,
                        message.getText(),
                        "Whiteboard",
                        JOptionPane.INFORMATION_MESSAGE
                );
                frame.dispose();
            });
        } else if (message.getType() == MessageType.ERROR) {
            showError(message.getText());
        }
    }

    private void askManagerForApproval(String requestedUsername) {
        SwingUtilities.invokeLater(() -> {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    requestedUsername + " wants to join your whiteboard. Allow?",
                    "Join Request",
                    JOptionPane.YES_NO_OPTION
            );
            boolean approved = answer == JOptionPane.YES_OPTION;
            try {
                send(WhiteboardMessage.approvalResponse(requestedUsername, approved));
            } catch (IOException exception) {
                showError("Could not send approval response: " + exception.getMessage());
            }
        });
    }

    private void sendDrawing(DrawingElement element) {
        try {
            send(WhiteboardMessage.draw(username, element));
        } catch (IOException exception) {
            showError("Could not send drawing event: " + exception.getMessage());
        }
    }

    private void sendChat(String text) {
        try {
            send(WhiteboardMessage.chat(username, text));
        } catch (IOException exception) {
            showError("Could not send chat message: " + exception.getMessage());
        }
    }

    private synchronized void send(WhiteboardMessage message) throws IOException {
        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private void kickUser(String usernameToKick) {
        if (username.equals(usernameToKick)) {
            showError("The manager cannot kick themselves.");
            return;
        }
        try {
            send(WhiteboardMessage.kick(usernameToKick));
        } catch (IOException exception) {
            showError("Could not send kick request: " + exception.getMessage());
        }
    }

    private void newBoard() {
        int answer = JOptionPane.showConfirmDialog(
                frame,
                "Clear the shared whiteboard for all users?",
                "New Whiteboard",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            send(WhiteboardMessage.newBoard());
            currentFile = null;
        } catch (IOException exception) {
            showError("Could not create a new whiteboard: " + exception.getMessage());
        }
    }

    private void openBoard() {
        JFileChooser chooser = new JFileChooser();
        int answer = chooser.showOpenDialog(frame);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            List<DrawingElement> loadedElements = loadDrawingElements(selectedFile);
            currentFile = selectedFile;
            send(WhiteboardMessage.replaceBoard(loadedElements));
            frame.addChatMessage("System: opened whiteboard from " + selectedFile.getName());
        } catch (IOException exception) {
            showError("Could not open whiteboard: " + exception.getMessage());
        }
    }

    private void saveBoard() {
        if (currentFile == null) {
            saveBoardAs();
            return;
        }
        writeDrawingElements(currentFile);
    }

    private void saveBoardAs() {
        JFileChooser chooser = new JFileChooser();
        int answer = chooser.showSaveDialog(frame);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }

        currentFile = chooser.getSelectedFile();
        writeDrawingElements(currentFile);
    }

    private void exportPng() {
        JFileChooser chooser = new JFileChooser();
        int answer = chooser.showSaveDialog(frame);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = ensurePngExtension(chooser.getSelectedFile());
        BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        canvas.paint(graphics);
        graphics.dispose();

        try {
            ImageIO.write(image, "png", selectedFile);
            frame.addChatMessage("System: exported image to " + selectedFile.getName());
        } catch (IOException exception) {
            showError("Could not export PNG: " + exception.getMessage());
        }
    }

    private File ensurePngExtension(File file) {
        if (file.getName().toLowerCase().endsWith(".png")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".png");
    }

    private void closeBoard() {
        int answer = JOptionPane.showConfirmDialog(
                frame,
                "Close the shared whiteboard for all users?",
                "Close Whiteboard",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            send(WhiteboardMessage.serverShutdown("The manager closed the whiteboard."));
        } catch (IOException exception) {
            showError("Could not close whiteboard: " + exception.getMessage());
        }
    }

    private void quitWhiteboard() {
        if (managerClient) {
            closeBoard();
        } else {
            leaveWhiteboard();
        }
    }

    private void leaveWhiteboard() {
        int answer = JOptionPane.showConfirmDialog(
                frame,
                "Leave the shared whiteboard?",
                "Leave Whiteboard",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            send(WhiteboardMessage.leave(username));
        } catch (IOException exception) {
            showError("Could not notify server before leaving: " + exception.getMessage());
        } finally {
            close();
            SwingUtilities.invokeLater(frame::dispose);
        }
    }

    private List<DrawingElement> loadDrawingElements(File file) throws IOException {
        try {
            return WhiteboardFileCodec.load(file.toPath());
        } catch (IOException textFormatException) {
            try {
                return readSerializedDrawingElements(file);
            } catch (IOException | ClassNotFoundException serializedException) {
                throw new IOException("File is not a valid whiteboard save file.");
            }
        }
    }

    private List<DrawingElement> readSerializedDrawingElements(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream fileInput = new ObjectInputStream(new FileInputStream(file))) {
            Object object = fileInput.readObject();
            if (object instanceof WhiteboardState whiteboardState) {
                return whiteboardState.getElements();
            }
            if (!(object instanceof List<?> loadedList)) {
                throw new IOException("File does not contain a valid whiteboard state.");
            }

            List<DrawingElement> elements = new ArrayList<>();
            for (Object item : loadedList) {
                if (!(item instanceof DrawingElement element)) {
                    throw new IOException("File contains an invalid drawing item.");
                }
                elements.add(element);
            }
            return elements;
        }
    }

    private void writeDrawingElements(File file) {
        try {
            WhiteboardFileCodec.save(file.toPath(), canvas.getElementsCopy());
            frame.addChatMessage("System: saved whiteboard to " + file.getName());
        } catch (IOException exception) {
            showError("Could not save whiteboard: " + exception.getMessage());
        }
    }

    private void close() {
        closing = true;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                frame,
                message,
                "Whiteboard Connection",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private void showInfo(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                frame,
                message,
                "Whiteboard",
                JOptionPane.INFORMATION_MESSAGE
        ));
    }
}
