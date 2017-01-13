package org.bhavaya.ui;

import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */
public class LookAndFeelManager {
    private static final Log log = Log.getCategory(LookAndFeelManager.class);

    public void installLookAndFeel() {
        LookAndFeel lookAndFeel = UIUtilities.createDefaultLookAndFeel();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
        } catch (UnsupportedLookAndFeelException e) {
            log.error(e);
        }
    }

    public void addChangeListener(ChangeListener listener) {
    }

    public void removeChangeListener(ChangeListener listener) {
    }

    public JMenu getMenu() {
        return null;
    }
}
