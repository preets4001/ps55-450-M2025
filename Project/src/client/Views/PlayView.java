package client.Views;

import client.Client;
import common.Phase;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;

public class PlayView extends JPanel {
    private final JPanel buttonPanel = new JPanel();

    public PlayView(String name){
        this.setName(name);

        // example user interaction
        JButton doSomething = new JButton("Do Something");
        doSomething.addActionListener(e -> {
            try {
                // sendDoTurn(...) isn't defined; use sendMessage(...) for now
                Client.INSTANCE.sendMessage("example");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        buttonPanel.add(doSomething);
        this.add(buttonPanel);
    }

    public void changePhase(Phase phase){
        if (phase == Phase.READY) {
            buttonPanel.setVisible(false);
        } else if (phase == Phase.IN_PROGRESS) {
            buttonPanel.setVisible(true);
        }
    }
}
