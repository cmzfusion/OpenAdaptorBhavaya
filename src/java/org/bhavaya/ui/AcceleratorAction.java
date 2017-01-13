package org.bhavaya.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public interface AcceleratorAction {

    public String getActionName();

    public KeyStroke getKeyStroke();

    public abstract void actionPerformed(AcceleratorActionEvent event);

    public class AcceleratorActionEvent extends ActionEvent {
        Object[] beans;

        public AcceleratorActionEvent(Object source, int id, String command, Object[] beans) {
            super(source, id, command);
            this.beans = beans;
        }

        public AcceleratorActionEvent(Object source, int id, String command, int modifiers, Object[] beans) {
            super(source, id, command, modifiers);
            this.beans = beans;
        }

        public AcceleratorActionEvent(Object source, int id, String command, long when, int modifiers, Object[] beans) {
            super(source, id, command, when, modifiers);
            this.beans = beans;
        }

        public Object[] getBeans() {
            return beans;
        }
    }
}

