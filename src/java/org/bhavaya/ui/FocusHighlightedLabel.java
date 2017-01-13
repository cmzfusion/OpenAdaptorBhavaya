package org.bhavaya.ui;

import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class FocusHighlightedLabel extends JLabel {
    private static final Log log = Log.getCategory(FocusHighlightedLabel.class);
    private HashSet<JComponent> focusableComponents = new HashSet<JComponent>();

    public FocusHighlightedLabel(String labelText, final JComponent formElement) {
        super(labelText);

        if (formElement.isFocusable()) {
            focusableComponents.add(formElement);
        }
        addFocusableComponents(formElement.getComponents());

        try {
            Runnable action = new Runnable() {
                public void run() {
                    boolean focusable = false;
                    for (JComponent focusableComponent : focusableComponents) {
                        if (focusableComponent.isFocusOwner()) {
                            focusable = true;
                        }
                    }

                    if (focusable) {
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (getFont().isBold()) {
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                }
            };

            for (JComponent focusableComponent : focusableComponents) {
                Method addMethod = focusableComponent.getClass().getMethod("addFocusListener", FocusListener.class);
                addMethod.invoke(focusableComponent, UIUtilities.triggerMethodOnEvent(FocusListener.class, null, action, "run"));
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void addFocusableComponents(Component[] components) {
        for (Component component : components) {
            if (component instanceof JPanel || component instanceof CardPanel || component instanceof Box)
                addFocusableComponents(((JComponent) component).getComponents());
            else if (component.isFocusable() && component instanceof JComponent)
                focusableComponents.add((JComponent) component);
        }
    }

}
