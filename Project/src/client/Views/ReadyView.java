package client.Views;

import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import client.Client;

public class ReadyView extends JPanel {
    public ReadyView() {
        JButton readyButton = new JButton("Ready");
        // FIX: use a valid lambda parameter name
        readyButton.addActionListener(e -> {
            try {
                Client.INSTANCE.sendReady(true); // mark me ready
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        this.add(readyButton);
    }
}
