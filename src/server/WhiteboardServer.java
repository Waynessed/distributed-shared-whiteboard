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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WhiteboardServer implements Runnable {
    private final String host;
    private final int port;
    private final Object stateLock = new Object();
    private final List<DrawingElement> drawingState = new ArrayList<>();
    private final List<ClientHandler> clients = new ArrayList<>();
    private final Map<String, PendingJoin> pendingJoins = new HashMap<>();
    private ClientHandler manager;
    private ServerSocket serverSocket;
    private boolean running = true;

    public WhiteboardServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket openedSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) {
            serverSocket = openedSocket;
            System.out.println("Whiteboard server listening on " + host + ":" + port);
            while (running) {
                Socket socket = openedSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "whiteboard-client-handler").start();
            }
        } catch (SocketException exception) {
            if (running) {
                System.err.println("Server socket error: " + exception.getMessage());
            } else {
                System.out.println("Server shut down by manager.");
            }
        } catch (IOException exception) {
            if (running) {
                System.err.println("Server error: " + exception.getMessage());
            } else {
                System.out.println("Server shut down by manager.");
            }
        }
    }

    private void removeClient(ClientHandler client) {
        synchronized (stateLock) {
            if (clients.remove(client)) {
                if (client == manager) {
                    manager = null;
                }
                broadcastUserList();
            }
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

    private void handleChat(WhiteboardMessage message) {
        synchronized (stateLock) {
            broadcast(WhiteboardMessage.chat(message.getUsername(), message.getText()));
        }
    }

    private void handleReplaceBoard(ClientHandler requester, List<DrawingElement> newState) {
        synchronized (stateLock) {
            if (requester != manager) {
                try {
                    requester.send(WhiteboardMessage.error("Only the manager can change the board file."));
                } catch (IOException ignored) {
                }
                return;
            }

            drawingState.clear();
            drawingState.addAll(newState);
            broadcast(WhiteboardMessage.state(drawingState));
        }
    }

    private void handleNewBoard(ClientHandler requester) {
        synchronized (stateLock) {
            if (requester != manager) {
                try {
                    requester.send(WhiteboardMessage.error("Only the manager can create a new board."));
                } catch (IOException ignored) {
                }
                return;
            }

            drawingState.clear();
            broadcast(WhiteboardMessage.state(drawingState));
            broadcast(WhiteboardMessage.chat("System", "The manager created a new whiteboard."));
        }
    }

    private void handleServerShutdown(ClientHandler requester) {
        synchronized (stateLock) {
            if (requester != manager) {
                try {
                    requester.send(WhiteboardMessage.error("Only the manager can close the whiteboard."));
                } catch (IOException ignored) {
                }
                return;
            }

            running = false;
            broadcast(WhiteboardMessage.serverShutdown("The manager closed the whiteboard."));
            for (ClientHandler client : new ArrayList<>(clients)) {
                client.closeSocket();
            }
            clients.clear();
            closeServerSocket();
        }
    }

    private void handleLeave(ClientHandler client) {
        synchronized (stateLock) {
            removeClient(client);
            client.closeSocket();
        }
    }

    private void handleApprovalResponse(WhiteboardMessage message) {
        synchronized (stateLock) {
            PendingJoin pendingJoin = pendingJoins.get(message.getUsername());
            if (pendingJoin != null) {
                pendingJoin.complete(message.isApproved());
            }
        }
    }

    private void handleKick(ClientHandler requester, String usernameToKick) {
        synchronized (stateLock) {
            if (requester != manager) {
                try {
                    requester.send(WhiteboardMessage.error("Only the manager can kick users."));
                } catch (IOException ignored) {
                }
                return;
            }

            ClientHandler target = findClient(usernameToKick);
            if (target == null || target == manager) {
                return;
            }

            try {
                target.send(WhiteboardMessage.kicked("You were removed by the manager."));
            } catch (IOException ignored) {
            }
            target.closeSocket();
            clients.remove(target);
            broadcastUserList();
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

    private void broadcastUserList() {
        broadcast(WhiteboardMessage.userList(getUsernames()));
    }

    private List<String> getUsernames() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.username);
        }
        return usernames;
    }

    private boolean usernameTaken(String username) {
        return findClient(username) != null || pendingJoins.containsKey(username);
    }

    private ClientHandler findClient(String username) {
        for (ClientHandler client : clients) {
            if (client.username.equals(username)) {
                return client;
            }
        }
        return null;
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private String username = "unknown";
        private boolean managerClient;
        private boolean intentionalClose;

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

                username = joinMessage.getUsername() == null ? "" : joinMessage.getUsername().trim();
                if (!joinWhiteboard()) {
                    return;
                }
                System.out.println(username + " joined the whiteboard.");

                while (true) {
                    Object object = input.readObject();
                    if (object instanceof WhiteboardMessage message) {
                        handleClientMessage(message);
                    }
                }
            } catch (EOFException exception) {
                if (!intentionalClose) {
                    System.out.println(username + " disconnected.");
                }
            } catch (IOException | ClassNotFoundException exception) {
                if (!intentionalClose && running) {
                    System.err.println("Client error for " + username + ": " + exception.getMessage());
                }
            } finally {
                removeClient(this);
                closeSocket();
            }
        }

        private boolean joinWhiteboard() throws IOException {
            PendingJoin pendingJoin = null;

            synchronized (stateLock) {
                if (username.isBlank()) {
                    send(WhiteboardMessage.joinRejected("Username cannot be blank."));
                    return false;
                }
                if (usernameTaken(username)) {
                    send(WhiteboardMessage.joinRejected("Username is already in use."));
                    return false;
                }
                if (manager == null) {
                    manager = this;
                    managerClient = true;
                    clients.add(this);
                    send(WhiteboardMessage.joinAccepted(true));
                    send(WhiteboardMessage.state(drawingState));
                    broadcastUserList();
                    return true;
                }

                pendingJoin = new PendingJoin(username);
                pendingJoins.put(username, pendingJoin);
                manager.send(WhiteboardMessage.approvalRequest(username));
            }

            boolean approved;
            try {
                approved = pendingJoin.waitForDecision();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                approved = false;
            }

            synchronized (stateLock) {
                pendingJoins.remove(username);
                if (!approved) {
                    send(WhiteboardMessage.joinRejected("The manager rejected your join request."));
                    return false;
                }
                if (usernameTaken(username)) {
                    send(WhiteboardMessage.joinRejected("Username is already in use."));
                    return false;
                }

                clients.add(this);
                send(WhiteboardMessage.joinAccepted(false));
                send(WhiteboardMessage.state(drawingState));
                broadcastUserList();
                return true;
            }
        }

        private void handleClientMessage(WhiteboardMessage message) {
            if (message.getType() == MessageType.DRAW) {
                handleDrawing(message.getDrawingElement());
            } else if (message.getType() == MessageType.CHAT) {
                handleChat(message);
            } else if (message.getType() == MessageType.NEW_BOARD) {
                handleNewBoard(this);
            } else if (message.getType() == MessageType.APPROVAL_RESPONSE && managerClient) {
                handleApprovalResponse(message);
            } else if (message.getType() == MessageType.KICK) {
                handleKick(this, message.getUsername());
            } else if (message.getType() == MessageType.REPLACE_BOARD) {
                handleReplaceBoard(this, message.getDrawingState());
            } else if (message.getType() == MessageType.SERVER_SHUTDOWN) {
                handleServerShutdown(this);
            } else if (message.getType() == MessageType.LEAVE) {
                handleLeave(this);
            }
        }

        synchronized void send(WhiteboardMessage message) throws IOException {
            output.writeObject(message);
            output.flush();
            output.reset();
        }

        private void closeSocket() {
            intentionalClose = true;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private static class PendingJoin {
        private final String username;
        private final CountDownLatch decisionLatch = new CountDownLatch(1);
        private boolean approved;

        PendingJoin(String username) {
            this.username = username;
        }

        void complete(boolean approved) {
            this.approved = approved;
            decisionLatch.countDown();
            System.out.println(username + (approved ? " was approved." : " was rejected."));
        }

        boolean waitForDecision() throws InterruptedException {
            if (!decisionLatch.await(60, TimeUnit.SECONDS)) {
                return false;
            }
            return approved;
        }
    }
}
