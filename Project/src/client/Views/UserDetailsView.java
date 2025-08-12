// File: src/client/Views/UserDetailsView.java
package client.Views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import client.CardViewName;
import client.Interfaces.ICardControlsnew;

public class UserDetailsView extends JPanel {
    private static final long serialVersionUID = 1L;

    private String username;

    private final JTextField userField = new JTextField();
    private final JLabel userError = new JLabel();

    public UserDetailsView(final ICardControlsnew controls) {
        super(new BorderLayout(10, 10));
        setName(CardViewName.USER_INFO.name());
        controls.registerView(CardViewName.USER_INFO.name(), this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Username
        content.add(new JLabel("Username:"));
        userField.setToolTipText("Enter the name you'll use in the game");
        content.add(userField);
        userError.setVisible(false);
        content.add(userError);

        content.add(Box.createRigidArea(new Dimension(0, 200)));

        // Buttons row
        JPanel buttons = new JPanel();

        JButton previousButton = new JButton("Previous");
        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controls.previousView();
            }
        });

        JButton connectButton = new JButton("Continue");
        connectButton.setToolTipText("Proceed to connection (host/port)");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String incoming = userField.getText() == null ? "" : userField.getText().trim();
                if (incoming.isEmpty()) {
                    userError.setText("Username must be provided");
                    userError.setVisible(true);
                    return;
                }
                userError.setVisible(false);
                username = incoming;

                // Move to the ConnectionView so the user can enter host/port.
                controls.showView(CardViewName.CONNECT.name());
            }
        });

        buttons.add(previousButton);
        buttons.add(connectButton);

        content.add(Box.createVerticalGlue());
        content.add(buttons);

        add(content, BorderLayout.CENTER);
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    /** Read by ClientUI.connect() when the actual Connect button is pressed on ConnectionView. */
    public String getUsername() {
        return username;
    }

    /** Optional helper if you want to prefill programmatically. */
    public void setUsername(String name) {
        this.username = name;
        this.userField.setText(name);
        this.userError.setVisible(false);
    }
}
