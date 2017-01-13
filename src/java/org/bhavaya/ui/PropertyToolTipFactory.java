package org.bhavaya.ui;

import org.bhavaya.ui.table.formula.FormulaUtils;
import org.bhavaya.util.Generic;
import org.bhavaya.util.PropertyModel;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.4 $
 */
public abstract class PropertyToolTipFactory implements ToolTipFactory {

    public PropertyToolTipFactory() {
    }

    public String getToolTipText(Class type, String propertyPath, String customName) {
        StringBuffer buffer = new StringBuffer("<html><head><STYLE type='text/css'> body { font-family: Dialog; padding: 1px 3px; text-align: left; ");
        int stylePosition = buffer.length();
        buffer.append(" }</STYLE></head><body><b>");
        int propertyNamePosition = buffer.length(); // insert property name later, keep the position index

        Class parentClass = type;
        PropertyModel propertyModel = null;
        String lastDisplayName = null;

        if(FormulaUtils.isFormulaPath(propertyPath)) {
            lastDisplayName = FormulaUtils.getDisplayNameForPropertyPath(propertyPath);
        } else  {
            String[] propertyPathArray = Generic.beanPathStringToArray(propertyPath);
            if (propertyPathArray.length > 1) {
                buffer.append("<br><small>(");

                for (int i = 0; i < propertyPathArray.length; i++) {
                    String propertyName = propertyPathArray[i];
                    parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(propertyName);
                    propertyModel = PropertyModel.getInstance(parentClass);

                    if (i > 0) buffer.append(" - ");
                    lastDisplayName = propertyModel.getDisplayName(propertyName);
                    buffer.append(lastDisplayName);

                    parentClass = propertyModel.getAttribute(new String[]{propertyName}).getType();
                }
                buffer.append(")</small></b>");
            } else {
                parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(propertyPath);
                propertyModel = PropertyModel.getInstance(parentClass);
                lastDisplayName = propertyModel.getDisplayName(propertyPath);
            }

            assert propertyModel != null;
            assert lastDisplayName != null;

            String description = propertyModel.getDescription(propertyPathArray[propertyPathArray.length - 1]);
            if (description != null) {
                buffer.append("<hr>");
                buffer.append(description);
                if (description.length() > 60) { // would be nicer to use max-width here, but Java doesn't support this yet
                    if ((description.indexOf("<br>") == -1)
                        && (description.indexOf("<BR>") == -1)
                        && (description.indexOf("<p>") == -1)
                        && (description.indexOf("<P>") == -1))
                    {
                        buffer.insert(stylePosition, " width: 300px; ");
                    }
                }
            }
        }


        if (customName != null && !lastDisplayName.equals(customName)) {
            lastDisplayName = lastDisplayName + " " + customName;
        }
        buffer.insert(propertyNamePosition, lastDisplayName);

        buffer.append("</body></html>");
        return buffer.toString();
    }

    public String getToolTipText(Class type, String propertyPath) {
        return getToolTipText(type, propertyPath, null);
    }

    public Point getToolTipLocation(MouseEvent event) {
        return null;
    }

    public JToolTip createToolTip() {
        return new PropertyDescriptionToolTip();
    }

    public static class PropertyDescriptionToolTip extends JToolTip {
        private int dismissDelay;

        public PropertyDescriptionToolTip() {
            addAncestorListener(new AncestorListener() {
                public void ancestorAdded(AncestorEvent event) {
                    dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
                    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE); // keep it displayed while the mouse is in the component area
                }

                public void ancestorRemoved(AncestorEvent event) {
                    ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
                }

                public void ancestorMoved(AncestorEvent event) {
                    // do nothing
                }
            });
        }


    }
}
