package org.bhavaya.ui;

import org.bhavaya.util.Condition;
import org.bhavaya.util.Observable;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class ComponentStateController {
    public interface UpdateStateStrategy {
        public void updateState(Component component, boolean conditionTrue);
    }

    public static UpdateStateStrategy ENABLE = new UpdateStateStrategy() {
        public void updateState(Component component, boolean conditionTrue) {
            component.setEnabled(conditionTrue);
            component.setFocusable(conditionTrue);
        }
    };

    public static UpdateStateStrategy EDITABLE = new UpdateStateStrategy() {
        public void updateState(Component component, boolean conditionTrue) {
            if (component instanceof JTextComponent) {
                ((JTextComponent) component).setEditable(conditionTrue);
            } else {
                component.setEnabled(conditionTrue);
            }
            component.setFocusable(conditionTrue);
        }
    };

    private PropertyChangeListener listener;
    private Observable bean;
    private HashMap components;
    private UpdateStateStrategy updateStateStrategy;

    public ComponentStateController(Observable bean, UpdateStateStrategy updateStateStrategy) {
        this.listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateState();
            }
        };
        this.bean = bean;
        this.bean.addPropertyChangeListener(listener);
        this.components = new HashMap();
        this.updateStateStrategy = updateStateStrategy;
    }

    public void addStateControlledComponent(Component component, Condition condition) {
        components.put(component, condition);
        updateState(component, condition);
    }

    public void dispose() {
        bean.removePropertyChangeListener(listener);
    }

    private void updateState(Component component, Condition condition) {
        updateStateStrategy.updateState(component, condition.isTrue());
    }

    public void updateState() {
        for (Iterator iterator = components.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Component controlledComponent = (Component) entry.getKey();
            Condition condition = (Condition) entry.getValue();
            updateState(controlledComponent, condition);
        }
    }
}
