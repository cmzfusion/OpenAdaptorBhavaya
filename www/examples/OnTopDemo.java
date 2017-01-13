
import org.bhavaya.ui.NativeWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Date: 08-Mar-2004
 * Time: 14:27:31
 */
public class OnTopDemo {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Always on top demo");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Window is always on top");

        final NativeWindow window = NativeWindow.getInstance(frame);

        JButton flashButton = new JButton(new AbstractAction("flash icon") {
            public void actionPerformed(ActionEvent e) {
                window.flashTaskbarIcon();
            }
        });

        panel.add(label);
        panel.add(flashButton);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        SwingUtilities.invokeLater(new Runnable(){
                   public void run(){
                        window.setAlwaysOnTop(true);
                   }
               });

    }
}
