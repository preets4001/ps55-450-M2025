package client;

import client.Interfaces.ICardControlsnew;
import client.Interfaces.IConnectionEvents;
import client.Interfaces.IRoomEvents;
import client.Views.ChatGameView;
import client.Views.ConnectionView;
import client.Views.MenuBar;
import client.Views.ReadyView;
import client.Views.RoomsView;
import client.Views.UserDetailsView;
import common.Constants;
import common.LoggerUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ClientUI extends JFrame implements ICardControlsnew, IConnectionEvents, IRoomEvents {
    private CardLayout cardLayout = new CardLayout();
    private Container frameContainer;
    private JPanel cardContainer;
    private JPanel activeCardViewPanel;
    private CardViewName activeCardViewEnum;
    private String originalTitle = "";
    private JMenuBar menuBar;
    private JLabel currentRoomLabel = new JLabel(Constants.NOT_CONNECTED);

    // Views
    private ConnectionView connectionView;
    private UserDetailsView userDetailsView;
    private ChatGameView chatGameView;
    private RoomsView roomsView;
    private ReadyView readyView;

    {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024);
        config.setFileCount(1);
        config.setLogLocation("client-ui.log");
        LoggerUtil.INSTANCE.setConfig(config);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ClientUI("MT85-Client");
            } catch (Throwable t) {
                LoggerUtil.INSTANCE.severe("Unhandled exception in main thread", t);
            }
        });
    }

    public ClientUI(String title) {
        super(title);
        originalTitle = title;
        setMinimumSize(new Dimension(400, 400));
        setSize(getMinimumSize());
        setLocationRelativeTo(null);
        Client.INSTANCE.registerCallback(this);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                int response = JOptionPane.showConfirmDialog(cardContainer,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        Client.INSTANCE.sendDisconnect();
                    } catch (NullPointerException | IOException e) {
                        LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }
        });

        menuBar = new MenuBar(this);
        this.setJMenuBar(menuBar);

        frameContainer = getContentPane();
        cardContainer = new JPanel();
        cardContainer.setLayout(cardLayout);

        frameContainer.add(currentRoomLabel, BorderLayout.NORTH);
        frameContainer.add(cardContainer, BorderLayout.CENTER);

        // Initialize & register views
        connectionView = new ConnectionView(this);
        userDetailsView = new UserDetailsView(this);
        chatGameView = new ChatGameView(this);
        roomsView = new RoomsView(this);
        roomsView.setVisible(false);

        // Ready view (so the Ready button can be shown during READY phase)
        readyView = new ReadyView();
        readyView.setName(CardViewName.READY.name());
        registerView(CardViewName.READY.name(), readyView);

        // Start on username screen
        showView(CardViewName.USER_INFO.name());

        pack();
        setVisible(true);
    }

    private void findAndSetCurrentView() {
        Component panel = List.of(cardContainer.getComponents()).stream().filter(Component::isVisible)
                .findFirst().orElseThrow();
        if (panel != null) {
            activeCardViewPanel = (JPanel) panel;
            activeCardViewEnum = Enum.valueOf(CardViewName.class, activeCardViewPanel.getName());

            if (!Client.INSTANCE.isMyClientIdSet() && CardViewName.viewRequiresConnection(activeCardViewEnum)) {
                showView(CardViewName.CONNECT.name());
                setSize(getMinimumSize());
                revalidate();
            }
        }
        LoggerUtil.INSTANCE.fine("Current View: " + activeCardViewPanel.getName());
    }

    @Override
    public void nextView() {
        cardLayout.next(cardContainer);
        findAndSetCurrentView();
    }

    @Override
    public void previousView() {
        cardLayout.previous(cardContainer);
        findAndSetCurrentView();
    }

    @Override
    public void showView(String viewName) {
        cardLayout.show(cardContainer, viewName);
        findAndSetCurrentView();
    }

    @Override
    public void showView(CardViewName viewEnum) {
        showView(viewEnum.name());
    }

    @Override
    public void registerView(String viewName, JPanel panelView) {
        cardContainer.add(panelView, viewName);
    }

    @Override
    public void connect() {
        String username = userDetailsView.getUsername();
        String host = connectionView.getHost();
        int port = connectionView.getPort();

        setTitle(String.format("%s - %s", originalTitle, username));
        boolean ok = Client.INSTANCE.connect(host, port, username);
        if (!ok) {
            JOptionPane.showMessageDialog(
                this,
                "Could not connect to " + host + ":" + port + ".\nStart the server, then try again.",
                "Connection failed",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        // Wait for onReceiveClientId(...) to advance the screen.
    }

    @Override
    public void onClientDisconnect(long clientId) {
        if (!CardViewName.viewRequiresConnection(activeCardViewEnum)) {
            LoggerUtil.INSTANCE.warning("Received onClientDisconnect while in a view prior to CHAT");
            return;
        }
        if (Client.INSTANCE.isMyClientId(clientId)) {
            currentRoomLabel.setText(Constants.NOT_CONNECTED);
            showView(CardViewName.CONNECT);
        }
    }

    @Override
    public void onReceiveClientId(long clientId) {
        LoggerUtil.INSTANCE.fine("Received client id: " + clientId);

        // Show READY panel first so the Ready button is visible during ready-check
        showView(CardViewName.READY.name());

        // If you prefer going straight to chat/game:
        // showView(CardViewName.CHAT_GAME_SCREEN);
        // chatGameView.showChatOnlyView();

        setSize(new Dimension(600, 600));
        revalidate();
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        LoggerUtil.INSTANCE.fine(String.format("onRoomAction: clientId=%d, roomName=%s, isJoin=%b, isQuiet=%b",
                clientId, roomName, isJoin, isQuiet));
        if (Client.INSTANCE.isMyClientId(clientId) && isJoin) {
            currentRoomLabel.setText(String.format("Room: %s", roomName));
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused
    }
}
