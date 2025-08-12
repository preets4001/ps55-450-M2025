package client.Views;

import client.CardViewName;
import client.Interfaces.ICardControlsnew;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar {
    public MenuBar(ICardControlsnew controls) {
        JMenu roomsMenu = new JMenu("Rooms");
        JMenuItem roomsSearch = new JMenuItem("Show Panel");

        // Use e instead of _
        roomsSearch.addActionListener(e -> {
            controls.showView(CardViewName.ROOMS.name());
        });

        roomsMenu.add(roomsSearch);
        this.add(roomsMenu);
    }
}
