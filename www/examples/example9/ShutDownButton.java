package example9;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

/**
 * Date: 19-Apr-2004
 * Time: 10:46:59
 */
public class ShutDownButton {

    private String name;
    private String message;
    private String buttonLabel;
    private final ShutDownOperation shutDownOperation;
    private boolean allowExit;
    private String exitWarning;
    private String exitMessage=null;
    private JFrame frame;
    private JPanel panel;
    private JButton button;


    public ShutDownButton (String name, String message, String buttonLabel, final ShutDownOperation shutDownOperation,
                           boolean allowExit, String exitWarning, String exitMessage) {
        this.name=name;
        this.message=message;
        this.buttonLabel=buttonLabel;
        this.allowExit=allowExit;
        this.shutDownOperation=shutDownOperation;
        this.exitWarning=exitWarning;
        this.exitMessage=exitMessage;

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showButton();
            }
        });

    }

    private void showButton (){
        //Make sure we have nice window decorations.
        //JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        frame = new JFrame(name);
        if(allowExit) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new ShutDown());
        }

        panel = new JPanel();
        button = new JButton(buttonLabel);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel label = new JLabel(message);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                shutDownOperation.shutDown();
            }
        });
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label);
        panel.add(button);

        frame.setContentPane(panel);

        //Display the window.
        //frame.pack();
        frame.setSize(230,70);
        frame.setVisible(true);
    }

    private class ShutDown implements WindowListener{
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {

            Object[] options = {"OK", "Cancel"};
            int n=JOptionPane.showOptionDialog(frame, exitMessage,
                    exitWarning,
                    JOptionPane. YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null,
                    options, options[0]);
            if (n==JOptionPane.YES_OPTION){
                shutDownOperation.shutDown();
            }
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }



}
