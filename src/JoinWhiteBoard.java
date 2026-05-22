import client.WhiteboardClient;
import ui.WhiteBoardFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class JoinWhiteBoard {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java JoinWhiteBoard <serverIPAddress> <serverPort> <username>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        SwingUtilities.invokeLater(() -> {
            WhiteBoardFrame frame = new WhiteBoardFrame("Whiteboard - " + username, false);
            WhiteboardClient client = new WhiteboardClient(username, frame.getCanvas());
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
        });
    }
}
