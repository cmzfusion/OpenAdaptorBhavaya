package org.bhavaya.ui;

import org.bhavaya.util.Log;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Warning.  This class is fundamentally flawed in design!!!
 *
 * @author Brendon Mclean
 * @author Andrew J. Dean
 * @version $Revision: 1.4.38.1 $
 */
public class OwnerModalDialog extends JDialog {
    private static final Log log = Log.getCategory(OwnerModalDialog.class);

    public OwnerModalDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title, Dialog.ModalityType.DOCUMENT_MODAL);
    }

    public OwnerModalDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title, Dialog.ModalityType.DOCUMENT_MODAL);
    }

    @Override
    public void setVisible(boolean b) {
        log.debug("Dialog Visible => " + b);
        super.setVisible(b);
    }

    public void setModal(boolean modal) {
        // By nature these are always modal, but the underlying JDialog must not be
        if (!modal) log.error("You cannot setModal(false) on an OwnerModalDialog!");
    }


    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        for (int i = 0; i < 2; i++) {
            final JFrame frame = new JFrame();
            frame.getContentPane().add(new JButton(new AbstractAction("Dialog") {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("About to show dialog");
                    JDialog dialog = new OwnerModalDialog(frame, "testD");
                    JButton testMe = new JButton("Test");
                    testMe.addActionListener(new ActionListener(){
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            System.out.println("...dlg pressed test");
                        }
                    });
                    dialog.getContentPane().add(testMe);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.pack();
                    dialog.show();
                    System.out.println("Closed Dialog");
                }
            }));
            frame.pack();
            frame.show();
        }
    }
}
