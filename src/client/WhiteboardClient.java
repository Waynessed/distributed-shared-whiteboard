package client;

import model.DrawingElement;
import protocol.MessageType;
import protocol.WhiteboardMessage;
import ui.DrawingCanvas;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WhiteboardClient {
    private final String username;
    private final DrawingCanvas canvas;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public WhiteboardClient(String username, DrawingCanvas canvas) {
        this.username = username;
        this.canvas = canvas;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        send(WhiteboardMessage.join(username));
        canvas.setDrawingListener(this::sendDrawing);

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
            showError("Disconnected from the server.");
        } catch (IOException | ClassNotFoundException exception) {
            showError("Network error: " + exception.getMessage());
        }
    }

    private void handleMessage(WhiteboardMessage message) {
        if (message.getType() == MessageType.STATE) {
            SwingUtilities.invokeLater(() -> canvas.setElements(message.getDrawingState()));
        } else if (message.getType() == MessageType.DRAW) {
            DrawingElement element = message.getDrawingElement();
            SwingUtilities.invokeLater(() -> canvas.addRemoteElement(element));
        } else if (message.getType() == MessageType.ERROR) {
            showError(message.getText());
        }
    }

    private void sendDrawing(DrawingElement element) {
        try {
            send(WhiteboardMessage.draw(username, element));
        } catch (IOException exception) {
            showError("Could not send drawing event: " + exception.getMessage());
        }
    }

    private synchronized void send(WhiteboardMessage message) throws IOException {
        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                canvas,
                message,
                "Whiteboard Connection",
                JOptionPane.ERROR_MESSAGE
        ));
    }
}
