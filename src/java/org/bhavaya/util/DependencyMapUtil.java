package org.bhavaya.util;

import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import java.awt.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class DependencyMapUtil {

    /**
     * Use this to confirm with the user whether we should procede with the change.
     * @param message
     * @throws org.bhavaya.util.DependencyMap.DoNotContinueUpdateException
     */
    public static void confirmChangeWithUser(String message) throws DependencyMap.DoNotContinueUpdateException {
        ConfirmationDialogRunnable confirmDialogRunnable = new ConfirmationDialogRunnable(message);
        confirmDialogRunnable.run();
        if (confirmDialogRunnable.result == JOptionPane.NO_OPTION) {
            throw new DependencyMap.DoNotContinueUpdateException("User selected NO for: " + message);
        }
    }

    private static class ConfirmationDialogRunnable implements Runnable {
        private int result;
        private String message;

        public ConfirmationDialogRunnable(String message) {
            this.message = message;
        }

        public void run() {
            if (!EventQueue.isDispatchThread()) {
                try {
                    EventQueue.invokeAndWait(this);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                runImpl();
            }
        }

        private void runImpl() {
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (owner == null) {
                owner = Workspace.getInstance().getApplicationFrame().getContentPane();
            }
            result = JOptionPane.showConfirmDialog(owner, message, "Confirmation", JOptionPane.YES_NO_OPTION);
        }
    }

}
