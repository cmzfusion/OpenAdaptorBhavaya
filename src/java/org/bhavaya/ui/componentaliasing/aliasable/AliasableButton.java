package org.bhavaya.ui.componentaliasing.aliasable;

import org.bhavaya.ui.componentaliasing.listeners.AbstractPopUpMenuHandler;
import org.bhavaya.ui.componentaliasing.listeners.AliasableAwtComponentPopUpMenuListener;

import javax.swing.*;

/**
 * A simple wrapper around JButton to create a button which is aliasable. Pressing it would trigger a pop up menu allowing
 * the user to select the view the aliased button should be placed on. REQUIRES JButton to use action.
 */
public class AliasableButton implements AliasableAwtComponent<JButton> {

    private JButton underlyingJButton;
    private JButton aliasableButton;
    private AbstractPopUpMenuHandler abstractPopUpMenuListener;

    /**
     * @param underlyingJButton
     * @param homeViewId
     */
    public AliasableButton(JButton underlyingJButton, long homeViewId,AbstractPopUpMenuHandler abstractPopUpMenuListener) {
        this.underlyingJButton = underlyingJButton;
        this.aliasableButton = new JButton();
        init(abstractPopUpMenuListener);
    }

    /**
     * @param underlyingJButton
     * @param homeViewId
     */
    public AliasableButton(JButton underlyingJButton, long homeViewId) {
        this.underlyingJButton = underlyingJButton;
        this.aliasableButton = new JButton();
        AliasableAwtComponentPopUpMenuListener aliasPopUpMenuListener = new AliasableAwtComponentPopUpMenuListener(getAliasableComponentIdentifier(), homeViewId);
        init(aliasPopUpMenuListener);
    }

    private void init(AbstractPopUpMenuHandler abstractPopUpMenuListener) {
        aliasableButton.addMouseListener(null);
        setPopUpMenuListenerListener(abstractPopUpMenuListener);
        aliasableButton.setAction(underlyingJButton.getAction());
    }


    public String getAliasableComponentIdentifier() {
        Action action = underlyingJButton.getAction();
        return action.getClass().getName();
    }

    public void setPopUpMenuListenerListener(AbstractPopUpMenuHandler abstractPopUpMenuListener) {
        aliasableButton.removeMouseListener(this.abstractPopUpMenuListener);
        this.abstractPopUpMenuListener = abstractPopUpMenuListener;
        aliasableButton.addMouseListener(abstractPopUpMenuListener);
    }

    public JButton getAliasableComponent() {
        return aliasableButton;
    }

}
