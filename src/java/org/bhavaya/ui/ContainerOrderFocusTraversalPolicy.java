package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;

/**
 * ContainerOrderFocusTraversalPolicy with the accept method of the LayoutFocusTraversalPolicy.
 * <p>
 * A FocusTraversalPolicy that determines traversal order based on the order
 * of child Components in a Container.
 *
 * @author Vladimir Hrmo
 */
public class ContainerOrderFocusTraversalPolicy extends java.awt.ContainerOrderFocusTraversalPolicy {

    protected boolean accept(Component aComponent) {
        if (!super.accept(aComponent)) {
            return false;
        } else if (aComponent instanceof JTable) {
            // JTable only has ancestor focus bindings, we thus force it
            // to be focusable by returning true here.
            return true;
        } else if (aComponent instanceof JComboBox) {
            JComboBox box = (JComboBox) aComponent;
            return box.getUI().isFocusTraversable(box);
        } else if (aComponent instanceof JComponent) {
            JComponent jComponent = (JComponent) aComponent;
            InputMap inputMap = jComponent.getInputMap(JComponent.WHEN_FOCUSED);
            while (inputMap != null && inputMap.size() == 0) {
                inputMap = inputMap.getParent();
            }
            if (inputMap != null) {
                return true;
            }
            // Delegate to the fitnessTestPolicy, this will test for the
            // case where the developer has overriden isFocusTraversable to
            // return true.
        }
        return false;
    }

}
