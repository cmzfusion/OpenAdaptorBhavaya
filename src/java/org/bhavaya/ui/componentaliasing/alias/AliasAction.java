package org.bhavaya.ui.componentaliasing.alias;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * Created by ga2adaz
 * An action that is powered by the aliased action
 */
public interface AliasAction extends PropertyChangeListener, Action {


    /**
     * Gets the action that is powering this alias button.
     * @return
     */
    public WeakReference<Action> getAliasedAction();

    /**
     * Gets the action that is going to be used when the action being aliased is not available anymore
     * @return
     */
    public WeakReference<BrokenAliasAction> getBrokenAliasAction();

    public boolean isUnderlyingActionAvailable();

    public void actionPerformed(ActionEvent e);

    public Object getValue(String key);

    public void propertyChange(PropertyChangeEvent evt);

}
