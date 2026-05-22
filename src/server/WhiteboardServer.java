package server;

import model.DrawingElement;
import protocol.MessageType;
import protocol.WhiteboardMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardServer implements Runnable {
    private final String host;
    private final int port;
    private final Object stateLock = new Object();
    private final List<DrawingElement> drawingState = new ArrayList<>();
    private final List<ClientHandler> clients = new ArrayList<>();

    public WhiteboardServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) {
            System.out.println("Whiteboard server listening on " + host + ":" + port);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "whiteboard-client-handler").start();
            }
        } catch (IOException exception) {
            System.err.println("Server stopped: " + exception.getMessage());
        }
    }

    private void addClient(ClientHandler client) throws IOException {
        synchronized (stateLock) {
            clients.add(client);
            client.send(WhiteboardMessage.state(drawingState));
        }
    }

    private void removeClient(ClientHandler client) {
        synchronized (stateLock) {
            clients.remove(client);
        }
    }

    private void handleDrawing(DrawingElement element) {
        synchronized (stateLock) {
            if (!drawingState.contains(element)) {
                drawingState.add(element);
            }
            broadcast(WhiteboardMessage.draw(null, element));
        }
    }

    private void broadcast(WhiteboardMessage message) {
        List<ClientHandler> disconnected = new ArrayList<>();
        for (ClientHandler client : clients) {
            try {
                client.send(message);
            } catch (IOException exception) {
                disconnected.add(client);
            }
        }
        clients.removeAll(disconnected);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private String username = "unknown";

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());

                Object firstMessage = input.readObject();
                if (!(firstMessage instanceof WhiteboardMessage joinMessage)
                        || joinMessage.getType() != MessageType.JOIN) {
                    send(WhiteboardMessage.error("First message must be JOIN."));
                    return;
                }

                username = joinMessage.getUsername();
                addClient(this);
                System.out.println(username + " joined the whiteboard.");

                while (true) {
                    Object object = input.readObject();
                    if (object instanceof WhiteboardMessage message && message.getType() == MessageType.DRAW) {
                        handleDrawing(message.getDrawingElement());
                    }
                }
            } catch (EOFException exception) {
                System.out.println(username + " disconnected.");
            } catch (IOException | ClassNotFoundException exception) {
                System.err.println("Client error for " + username + ": " + exception.getMessage());
            } finally {
                removeClient(this);
                closeSocket();
            }
        }

        synchronized void send(WhiteboardMessage message) throws IOException {
            output.writeObject(message);
            output.flush();
            output.reset();
        }

        private void closeSocket() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
