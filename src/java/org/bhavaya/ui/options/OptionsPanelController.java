package org.bhavaya.ui.options;

import org.bhavaya.util.ValidationException;

import javax.swing.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public abstract class OptionsPanelController {

    public static final String PROPERTY_CHANGED = "changed";

    public abstract Icon getIcon();

    public abstract String getCategoryName();

    /**
     * Reload original configuration. Discard any unsaved changes.
     */
    public abstract void reload();

    /**
     * Apply configuration changes.
     */
    public abstract void applyChanges();

    /**
     * Cancel any previously applied config changes (note: only those applied after last applyChanges call).
     */
    public abstract void cancel();

    /**
     * Validate the config changes.
     */
    public abstract void validate() throws ValidationException;

    public abstract JComponent getComponent();

    public String toString() {
        return getCategoryName();
    }
}
