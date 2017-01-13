package org.bhavaya.ui;

import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is now a well behaved modal dialog that blocks only events from its owner Window.
 * It behaves in a similar way to a normal Dialog except that it isn't synchronous.  On other words
 * show always returns immediately unlike a normal JDialog.  You can either use a WindowClosing listener
 * or use the addWindowClosingActionListener() convenience method.  Unfortunately this restriction is
 * <strong>fundamental</strong> to the design of Swing (I promise!).
 *
 * @author Brendon Mclean
 * @author Andrew J. Dean
 * @version $Revision: 1.2 $
 */
public class AsyncOwnerModalDialog extends JDialog {
    private static final Log log = Log.getCategory(AsyncOwnerModalDialog.class);

    private static ModalEventQueue modalEventQueue = new ModalEventQueue();
    private ArrayList listeners = new ArrayList();


    public AsyncOwnerModalDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title, false);
    }

    public AsyncOwnerModalDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title, false);
    }

    public void show() {
        startFilteringEvents();
        super.show();
    }

    public void hide() {
        boolean willHide = isVisible();
        super.hide();

        if (willHide) {
            stopFilteringEvents();
            fireWindowClosing();
            if (log.isDebug())log.debug("Dialog hidden - event filter removed");
        }
    }

    private void fireWindowClosing() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ActionListener actionListener = (ActionListener) iterator.next();
            actionListener.actionPerformed(new ActionEvent(this, 0, "Dialog closing"));
        }
    }

    public void addWindowClosingActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void setModal(boolean modal) {
        // By nature these are always modal, but the underlying JDialog must not be
        if (!modal) log.error("You cannot setModal(false) on an OwnerModalDialog!");
    }

    private void startFilteringEvents() {
        if (log.isDebug())log.debug("startFilteringEvents");
        modalEventQueue.addModalLock(getOwner(), this);
    }

    private void stopFilteringEvents() {
        if (log.isDebug())log.debug("stopFilteringEvents");
        modalEventQueue.removeModalLock(getOwner());
    }

    private static class ModalEventQueue extends EventQueue {
        private Map ownerToDialogMap = new HashMap();

        private ModalEventQueue() {
        }

        public void addModalLock(Window owner, AsyncOwnerModalDialog dialog) {
            if (ownerToDialogMap.size() == 0) {
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(this);
            }
            ownerToDialogMap.put(owner, dialog);
        }

        public void removeModalLock(Window owner) {
            Object owneReturned = ownerToDialogMap.remove(owner);
            if (ownerToDialogMap.size() == 0 && owneReturned != null) {
                System.out.println("OwnerModalDialog$ModalEventQueue.removeModalLock");
                pop();
            }
        }

        protected void dispatchEvent(AWTEvent event) {
            if (accept(event)) super.dispatchEvent(event);
        }

        public boolean accept(AWTEvent event) {
            AsyncOwnerModalDialog dialog = (AsyncOwnerModalDialog) ownerToDialogMap.get(event.getSource());
            if (dialog != null) {
                boolean eventTypeBlocked = eventTypeBlocked(event);
                if (windowEventRequiresAction(event)) {
                    dialog.toFront();
                    Toolkit.getDefaultToolkit().beep();
                }
                return !eventTypeBlocked;
            }
            return true;
        }

        private boolean eventTypeBlocked(AWTEvent event) {
            return event instanceof KeyEvent || mouseClickEvent(event);
        }

        private boolean mouseClickEvent(AWTEvent event) {
            if (!(event instanceof MouseEvent)) return false;
            // Wee!  Look at me!  I feel like a C programmer.  Tie-die tshirts are cool!
            switch (event.getID()) {
                case MouseEvent.MOUSE_RELEASED:
                case MouseEvent.MOUSE_CLICKED:
                case MouseEvent.MOUSE_PRESSED:
                    return true;
                default:
                    return false;
            }
        }

        private boolean windowEventRequiresAction(AWTEvent event) {
            if (!(event instanceof WindowEvent)) return false;
            return event.getID() == WindowEvent.WINDOW_ACTIVATED
                    || event.getID() == WindowEvent.WINDOW_GAINED_FOCUS;
        }
    }


    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        for (int i = 0; i < 2; i++) {
            final JFrame frame = new JFrame();
            frame.getContentPane().add(new JButton(new AbstractAction("Dialog") {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("About to show dialog");
                    AsyncOwnerModalDialog dialog = new AsyncOwnerModalDialog(frame, "testD");
                    dialog.getContentPane().add(new JButton("Test"));
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.pack();
                    dialog.addWindowClosingActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            System.out.println("Closed Dialog");
                        }
                    });
                    dialog.show();
                }
            }));
            frame.pack();
            frame.show();
        }
    }
}
