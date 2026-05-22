import client.WhiteboardClient;
import server.WhiteboardServer;
import ui.WhiteBoardFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CreateWhiteBoard {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java CreateWhiteBoard <serverIPAddress> <serverPort> <username>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        WhiteboardServer server = new WhiteboardServer(host, port);
        new Thread(server, "whiteboard-server").start();
        pauseForServerStartup();

        SwingUtilities.invokeLater(() -> openClientWindow(host, port, username, "Manager"));
    }

    private static void pauseForServerStartup() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void openClientWindow(String host, int port, String username, String role) {
        WhiteBoardFrame frame = new WhiteBoardFrame("Whiteboard - " + username + " (" + role + ")", false);
        WhiteboardClient client = new WhiteboardClient(username, frame);
        try {
            client.connect(host, port);
            frame.setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not connect to server: " + exception.getMessage(),
                    "Whiteboard",
                    JOptionPane.ERROR_MESSAGE
            );
            frame.dispose();
        }
    }
}
