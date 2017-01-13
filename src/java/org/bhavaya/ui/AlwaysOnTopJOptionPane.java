package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 31-Mar-2008
 * Time: 18:27:20
 *
 * Workaround for the case where a modal JOptionPane is hidden behind an alwaysOnTop window
 * Setting JOptionPane so that its dialog is also 'always on top' makes it appear in front
 *
 * See bug id 6519416
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6519416
 */
public class AlwaysOnTopJOptionPane
{
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType)  {
        return showOptionDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE, null, null, null);
    }

    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        showOptionDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION, messageType, null, null, null);
    }

    /**
     *  This method is a copy paste from JOptionPane
     *  but a call to set the option dialog alwaysOnTop has been added
     */
    public static int showOptionDialog(Component parentComponent,
                                       Object message, String title, int optionType, int messageType,
                                       Icon icon, Object[] options, Object initialValue)
            throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType,
                optionType, icon,
                options, initialValue);

        pane.setInitialValue(initialValue);
        pane.setComponentOrientation(((parentComponent == null) ?
                JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        JDialog dialog = pane.createDialog(parentComponent, title);
        dialog.setAlwaysOnTop(true);  //This line should be the functional difference between this method and the one in JOptionPane

        pane.selectInitialValue();
        dialog.show();
        dialog.dispose();

        Object selectedValue = pane.getValue();

        if (selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if (options == null) {
            if (selectedValue instanceof Integer)
                return ((Integer) selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        for (int counter = 0, maxCounter = options.length;
             counter < maxCounter; counter++) {
            if (options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }


}
