package org.bhavaya.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Use this action to close dialogs and frames on escape key stroke.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class CloseOnEscapeAction extends AbstractAction {

    private Window window;

    public CloseOnEscapeAction(JDialog dialog) {
        this.window = dialog;
    }
    
    public CloseOnEscapeAction(JFrame frame) {
        this.window = frame;
    }

    public void actionPerformed(ActionEvent e) {
        // we have to handle the popup menu close operation as well here
        MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
        MenuElement path[] = menuSelectionManager.getSelectedPath();
        if(path.length > 4) {
            MenuElement newPath[] = new MenuElement[path.length - 2];
            System.arraycopy(path,0,newPath,0,path.length-2);
            menuSelectionManager.setSelectedPath(newPath);
        } else if (path.length > 0) {
            menuSelectionManager.clearSelectedPath();
        } else {
            int defaultCloseOperation;
            if (window instanceof JFrame) {
                defaultCloseOperation = ((JFrame) window).getDefaultCloseOperation();
            } else {
                defaultCloseOperation = ((JDialog) window).getDefaultCloseOperation();
            }
            switch (defaultCloseOperation) {
                case WindowConstants.HIDE_ON_CLOSE:
                    window.setVisible(false);
                    break;
                case WindowConstants.DISPOSE_ON_CLOSE:
                    window.setVisible(false);
                    window.dispose();
                    break;
                case WindowConstants.DO_NOTHING_ON_CLOSE:
                default:
                    break;
            }
        }
    }
}
