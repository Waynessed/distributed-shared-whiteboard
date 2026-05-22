package client;

import model.DrawingElement;
import protocol.MessageType;
import protocol.WhiteboardMessage;
import ui.DrawingCanvas;
import ui.WhiteBoardFrame;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private volatile boolean closing;

    public WhiteboardClient(String username, WhiteBoardFrame frame) {
        this.username = username;
        this.frame = frame;
        this.canvas = frame.getCanvas();
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        send(WhiteboardMessage.join(username));
        frame.setKickListener(this::kickUser);
        frame.setChatListener(this::sendChat);
        frame.setFileActionListeners(
                this::newBoard,
                this::openBoard,
                this::saveBoard,
                this::saveBoardAs,
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
                canvas.setDrawingListener(this::sendDrawing);
                frame.setManagerMode(message.isManager());
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
        } else if (message.getType() == MessageType.USER_LIST) {
            SwingUtilities.invokeLater(() -> frame.setUsers(message.getUsers()));
        } else if (message.getType() == MessageType.APPROVAL_REQUEST) {
            askManagerForApproval(message.getUsername());
        } else if (message.getType() == MessageType.KICKED) {
            showError(message.getText());
            close();
            SwingUtilities.invokeLater(frame::dispose);
        } else if (message.getType() == MessageType.SERVER_SHUTDOWN) {
            showError(message.getText());
            close();
            SwingUtilities.invokeLater(frame::dispose);
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
            send(WhiteboardMessage.replaceBoard(List.of()));
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
            List<DrawingElement> loadedElements = readDrawingElements(selectedFile);
            currentFile = selectedFile;
            send(WhiteboardMessage.replaceBoard(loadedElements));
        } catch (IOException | ClassNotFoundException exception) {
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

    private List<DrawingElement> readDrawingElements(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream fileInput = new ObjectInputStream(new FileInputStream(file))) {
            Object object = fileInput.readObject();
            if (!(object instanceof List<?> loadedList)) {
                throw new IOException("File does not contain a whiteboard drawing list.");
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
        try (ObjectOutputStream fileOutput = new ObjectOutputStream(new FileOutputStream(file))) {
            fileOutput.writeObject(canvas.getElementsCopy());
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
}
