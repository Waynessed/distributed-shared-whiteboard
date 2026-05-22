package app;

import ui.WhiteBoardFrame;

import javax.swing.SwingUtilities;

public class StandaloneWhiteBoard {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WhiteBoardFrame frame = new WhiteBoardFrame();
            frame.setVisible(true);
        });
    }
}
