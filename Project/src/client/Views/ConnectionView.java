package client.Views;

import client.CardViewName;
import client.Interfaces.ICardControlsnew;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public class ConnectionView extends JPanel {
    private final JTextField hostField = new JTextField(16);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(3000, 1, 65535, 1));

    public ConnectionView(ICardControlsnew controls) {
        setLayout(new GridBagLayout());
        setName(CardViewName.CONNECT.name());
        controls.registerView(CardViewName.CONNECT.name(), this);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        // Host
        g.gridx = 0; g.gridy = 0; add(new JLabel("Host:"), g);
        g.gridx = 1; g.gridy = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; add(hostField, g);

        // Port
        g.gridx = 0; g.gridy = 1; g.fill = GridBagConstraints.NONE; g.weightx = 0; add(new JLabel("Port:"), g);
        g.gridx = 1; g.gridy = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; add(portSpinner, g);

        // Connect button
        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> controls.connect());
        g.gridx = 1; g.gridy = 2; g.fill = GridBagConstraints.NONE; g.weightx = 0;
        add(connectBtn, g);

        // Sensible defaults
        hostField.setText("localhost");
        portSpinner.setValue(3000);
    }

    public String getHost() { return hostField.getText().trim(); }
    public int getPort() { return ((Number)portSpinner.getValue()).intValue(); }

    // Optional setters for demos/tests
    public void setHost(String host) { hostField.setText(host); }
    public void setPort(int port) { portSpinner.setValue(port); }
}
