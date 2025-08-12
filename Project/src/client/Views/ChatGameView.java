package client.Views;

import client.CardViewName;
import client.Interfaces.ICardControlsnew;
import client.Interfaces.IPhaseEvent;
import client.Interfaces.IRoomEvents;
import common.Constants;
import common.Phase;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class ChatGameView extends JPanel implements IRoomEvents, IPhaseEvent {
    private final ChatView chatView;
    private final GameView gameView;
    private final JSplitPane splitPane;

    public ChatGameView(ICardControlsnew controls) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        setName(CardViewName.CHAT_GAME_SCREEN.name());
        controls.registerView(CardViewName.CHAT_GAME_SCREEN.name(), this);

        chatView = new ChatView(controls);
        gameView = new GameView(controls);
        gameView.setVisible(false);
        gameView.setBackground(Color.BLUE);
        chatView.setBackground(Color.GRAY);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gameView, chatView);
        splitPane.setResizeWeight(0.6);
        splitPane.setOneTouchExpandable(false);
        splitPane.setEnabled(false);

        add(splitPane, BorderLayout.CENTER);

        gameView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.6);
            }
        });

        showChatOnlyView();
    }

    public void showGameView() {
        gameView.setVisible(true);
        splitPane.setDividerLocation(0.6);
    }

    public void showChatOnlyView() {
        gameView.setVisible(false);
        chatView.setVisible(true);
        splitPane.setDividerLocation(1.0);
        revalidate();
        repaint();
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {}

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (isJoin && Constants.DEFAULT_ROOM.equals(roomName)) {
            showChatOnlyView();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        showGameView();
    }
}
