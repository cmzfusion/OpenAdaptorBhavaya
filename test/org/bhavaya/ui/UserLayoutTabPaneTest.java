package org.bhavaya.ui;

import org.bhavaya.ui.UserLayoutTabPane;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class UserLayoutTabPaneTest extends UserLayoutTabPane {
    private static final Log log = Log.getCategory(UserLayoutTabPaneTest.class);

    private interface TabPaneInterface {
        public void addTab(String title, Component component);

        public Component getComponent();
    }

    private static class UserLayoutTabPaneDelegate implements TabPaneInterface {
        private UserLayoutTabPane tabPane;

        public UserLayoutTabPaneDelegate(UserLayoutTabPane tabPane) {
            this.tabPane = tabPane;
        }

        public void addTab(String title, Component component) {
            tabPane.addTab(title, component);
        }

        public Component getComponent() {
            return tabPane;
        }
    }

    private static class JTabbedPaneDelegate implements TabPaneInterface {
        private JTabbedPane tabPane;

        public JTabbedPaneDelegate(JTabbedPane tabPane) {
            this.tabPane = tabPane;
        }

        public void addTab(String title, Component component) {
            tabPane.addTab(title, component);
        }

        public Component getComponent() {
            return tabPane;
        }
    }

    public static void main(String[] args) throws Exception {
//        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        createNewTabFrame(new JTabbedPaneDelegate(new JTabbedPane()));
        createNewTabFrame(new UserLayoutTabPaneDelegate(new UserLayoutTabPane()));
    }

    private static void createNewTabFrame(TabPaneInterface tabPane) {
        JFrame frame = new JFrame("User Layout Tab Pane");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(tabPane.getComponent());

        SaveRestoreAction saveRestoreAction = new SaveRestoreAction(tabPane);
        JButton saveButton = new JButton(saveRestoreAction);
        saveButton.setText("save");
        JButton restoreButton = new JButton(saveRestoreAction);
        restoreButton.setText("restore into new frame");

        frame.getContentPane().add(saveButton, BorderLayout.NORTH);
        frame.getContentPane().add(restoreButton, BorderLayout.SOUTH);

        tabPane.addTab("trades - all books", new JLabel("<html>hello<br>this is<br>some text</html>"));
        tabPane.addTab("supra/agency/shorts", new JLabel("A text area 4"));
        tabPane.addTab("positions - all positions", new JLabel("A text area 5"));
        tabPane.addTab("positions - US positions", new JLabel("A text area 6"));
        tabPane.addTab("positions - EUR positions", new JLabel("A text area 7"));
        tabPane.addTab("positions - GBP positions", new JLabel("A text area 8"));
        tabPane.addTab("positions - JPY positions", new JLabel("A text area 9"));
        tabPane.addTab("positions - DEM positions", new JLabel("A text area 10"));
        tabPane.addTab("positions - FRA positions", new JLabel("A text area 11"));

        JLabel component = new JLabel("Text Area.  Preferred size 400x400");
        component.setPreferredSize(new Dimension(400, 400));
        tabPane.addTab("positions - HKK positions", component);

        frame.pack();
        frame.show();
    }

    private static class SaveRestoreAction extends AbstractAction {
        private final TabPaneInterface tabPane;
        private byte[] saveData;

        public SaveRestoreAction(TabPaneInterface tabPane) {
            this.tabPane = tabPane;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("save")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BeanUtilities.writeObjectToStream(tabPane.getComponent(), bos);
            saveData = bos.toByteArray();
            } else {
                ByteArrayInputStream bis = new ByteArrayInputStream(saveData);
                Component persistedComponent = (Component) BeanUtilities.readObjectFromStream(bis);
                JFrame frame = new JFrame("Persisted Component");
                frame.getContentPane().add(persistedComponent);
                frame.setSize(400, 400);
                frame.show();
            }
        }
    }


    //TODO: Color editor, test remove, add, etc.
}
