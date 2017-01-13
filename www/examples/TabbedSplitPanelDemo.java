import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.bhavaya.ui.*;

/**
 * Date: 24-Feb-2004
 * Time: 14:33:13
 */
public class TabbedSplitPanelDemo {

    JFrame frame;
    JLabel label;
    JButton aButton;
    static int count=1;

    public TabbedSplitPanelDemo() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

        frame = new JFrame("Tabbed Split Panel Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //1.3+
        createGUI(frame);
        initListener(); // For the button
        frame.setSize(600,600);
        frame.setVisible(true);
    }

    void createGUI(JFrame f) {

        // This is the root TabbedSplitPanel, all other split panels descend from this one
        TabbedSplitPanel centrePanel = new TabbedSplitPanel(new JPanel(),TabbedSplitPanel.RIGHT);
        centrePanel.addMenuPanel(new MenuPanel("Right Menu", new JTextArea("Menu panel for right tabbed split panel"),true));

        TabbedSplitPanel bottomPanel = new TabbedSplitPanel(centrePanel,SplitPanel.BOTTOM);
        MenuPanel mainBottomMenuPanel = new MenuPanel("Main bottom menu", new JTextArea("Contents of bottom menu"), true);
        bottomPanel.addMenuPanel(mainBottomMenuPanel);

        // A panel with a button in it
        JPanel panel = createAMenuView();

        TabbedSplitPanel leftPanel = new TabbedSplitPanel(bottomPanel, SplitPanel.LEFT);
        leftPanel.addMenuPanel(new MenuPanel("Left1", panel, true));
        leftPanel.addMenuPanel(new MenuPanel("Left2", new JTextArea("Left 2 text area"),true));

        // A nested TabbedSplitPanel
        TabbedSplitPanel nestedPanel = new TabbedSplitPanel(new JTextArea("Panel Containing a Nested panel"),SplitPanel.BOTTOM);
        nestedPanel.addMenuPanel(new MenuPanel("Nested bottom menu",new JTextArea("Nested menu panel"),true));
        centrePanel.addMenuPanel(new MenuPanel("Nested",nestedPanel,true));

        f.setContentPane(leftPanel);

    }

    private JPanel createAMenuView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        label = new JLabel("Click the button");
        aButton = new JButton("A button");
        panel.add(aButton);
        panel.add(label);
        return panel;
    }

    void initListener(){
        aButton.addActionListener(new MyActionListener());
    }

    public class MyActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
                label.setText("Click " + count);
                count++;
        }

    }

    public static void main(String[] args) {
        new TabbedSplitPanelDemo();


    }



}

