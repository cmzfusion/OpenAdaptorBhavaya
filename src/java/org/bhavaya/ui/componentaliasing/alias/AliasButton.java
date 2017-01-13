package org.bhavaya.ui.componentaliasing.alias;

import org.bhavaya.ui.view.ViewUtils;
import org.bhavaya.ui.componentaliasing.listeners.AbstractPopUpMenuHandler;
import org.bhavaya.ui.componentaliasing.listeners.AliasAwtComponentPopUpMenuListener;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;
import org.bhavaya.util.Log;
import org.bhavaya.util.WeakReferencingPropertyChangeListener;

import javax.swing.*;
import java.lang.ref.WeakReference;

/**
 * A simple wrapper around JButton to create an alias button which aliases an existing button. Pressing it would trigger the action triggered by pressing the
 * original button.
 */
public class AliasButton implements AliasAwtComponent<JButton> {

    private static final Log log = Log.getCategory(AliasButton.class);

    private transient AbstractPopUpMenuHandler popUpMenuListener;
    private transient JButton aliasButton = new JButton();
    public String alisableComponentIdentifier;
    public long originatingViewId;
    public long homeViewId;
    transient AliasAction aliasAction;
    transient BrokenAliasAction brokenAliasAction;


    static {
        BeanUtilities.addPersistenceDelegate(AliasButton.class, new BhavayaPersistenceDelegate(new String[]{"alisableComponentIdentifier", "originatingViewId", "homeViewId"}));
    }

    public static String createIdentifier(String alisableComponentIdentifier, long originatingViewId, long homeViewId) {
        return alisableComponentIdentifier + "_" + originatingViewId + "_" + homeViewId;
    }

    public AliasButton(String alisableComponentIdentifier, long originatingViewId, long homeViewId, AbstractPopUpMenuHandler popUpMenuListener) {
        this.alisableComponentIdentifier = alisableComponentIdentifier;
        this.homeViewId = homeViewId;
        this.originatingViewId = originatingViewId;
        init(popUpMenuListener);
    }

    public AliasButton(String alisableComponentIdentifier, long originatingViewId, long homeViewId) {
        this.alisableComponentIdentifier = alisableComponentIdentifier;
        this.homeViewId = homeViewId;
        this.originatingViewId = originatingViewId;
        AliasAwtComponentPopUpMenuListener aliasPopUpMenuListener = new AliasAwtComponentPopUpMenuListener(alisableComponentIdentifier, originatingViewId, homeViewId);
        init(aliasPopUpMenuListener);
    }
    //calls methods on JButton
    private void init(AbstractPopUpMenuHandler abstractPopUpMenuListener) {
        setUpAliasAction();
        setPopUpMenuListener(abstractPopUpMenuListener);
    }

    private void setUpAliasAction() {
        brokenAliasAction = new BrokenAliasAction(originatingViewId, alisableComponentIdentifier);
        WeakReference<BrokenAliasAction> broken = new WeakReference<BrokenAliasAction>(brokenAliasAction);
        WeakReference<Action> action = ViewUtils.getActionFromView(alisableComponentIdentifier, originatingViewId);
        aliasAction = new WeakReferenceAliasAction(action, broken,originatingViewId);
        if(action.get() !=null) {
            action.get().addPropertyChangeListener(new WeakReferencingPropertyChangeListener(aliasAction,action.get()));
        }
        else {
            log.warn("could not find action");
        }
        aliasButton.setAction(aliasAction);
    }

    /**
     * Has a hack that on each redraw it will check if the underlying action is MIA and will try to find if it is.
     * This saves us from having to check if the original action that this button is aliasing is available. This is useful as
     * when the client is started we have no control over the order of which views are restored. So if a view is restored that
     * aliases a button from another view which has
     * yet to be restored it will display a broken button at until such time the other view has become available.
     * @return
     */
    public JButton getAliasComponent() {
        if(!aliasAction.isUnderlyingActionAvailable()) {
            init(popUpMenuListener);
        }
        return aliasButton;
    }

    public String getAliasedComponentIdentifier() {
        return alisableComponentIdentifier;
    }

    public void setPopUpMenuListener(AbstractPopUpMenuHandler abstractPopUpMenuListener) {
        aliasButton.removeMouseListener(this.popUpMenuListener);
        this.popUpMenuListener = abstractPopUpMenuListener;
        aliasButton.addMouseListener(abstractPopUpMenuListener);
    }

    public String getIdentifier() {
        return alisableComponentIdentifier + "_" + originatingViewId + "_" + homeViewId;
    }

    public AliasAction getAliasAction() {
        return aliasAction;
    }

    @Override
    public String toString() {
        return "AliasButton{" +
                "alisableComponentIdentifier='" + alisableComponentIdentifier + '\'' +
                '}';
    }

    /*public boolean equals(Object o) {
        if(o instanceof AliasButton) {
            AliasButton aliasButton = ((AliasButton) o);
            return this.getAliasedComponentIdentifier().equals(aliasButton.getAliasedComponentIdentifier());
        }
        return false;
    }

    public int hashCode() {
        return getIdentifier().hashCode();
    }*/

}
