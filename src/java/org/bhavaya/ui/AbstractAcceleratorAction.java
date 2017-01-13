package org.bhavaya.ui;

import javax.swing.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public abstract class AbstractAcceleratorAction implements AcceleratorAction {
    String actionName;
    KeyStroke keyStroke;

    protected AbstractAcceleratorAction(String actionName, KeyStroke keyStroke) {
        this.actionName = actionName;
        this.keyStroke = keyStroke;
    }

    public String getActionName() {
        return actionName;
    }

    public KeyStroke getKeyStroke() {
        return keyStroke;
    }

    public abstract void actionPerformed(AcceleratorActionEvent event);

}
