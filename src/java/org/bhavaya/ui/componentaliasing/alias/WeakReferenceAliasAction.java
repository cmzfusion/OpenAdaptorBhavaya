package org.bhavaya.ui.componentaliasing.alias;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.ViewUtils;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Log;
import org.bhavaya.util.WeakChangeListener;

public class WeakReferenceAliasAction extends AbstractAction implements AliasAction, ChangeListener {

    private static final Log log = Log.getCategory(WeakReferenceAliasAction.class);
    private WeakReference<Action> aliasedAction;
    private WeakReference<BrokenAliasAction> brokenAliasAction;
    private WeakReference<View> viewOfAliasedAction = new WeakReference<View>(null);
    private String cachedViewTabTitle;
    private String shortDesc;
    private ImageIcon cachedImageIcon;


    public WeakReferenceAliasAction(WeakReference<Action> aliasedAction, WeakReference<BrokenAliasAction> brokenAliasAction) {
        this.aliasedAction = aliasedAction;
        this.brokenAliasAction = brokenAliasAction;
    }

    public WeakReferenceAliasAction(WeakReference<Action> aliasedAction, WeakReference<BrokenAliasAction> brokenAliasAction, long viewId) {
        this.aliasedAction = aliasedAction;
        this.brokenAliasAction = brokenAliasAction;
        View view = ViewUtils.getView(viewId);
        if(view != null) {
            this.viewOfAliasedAction = new WeakReference<View>(view);
            view.addChangeListener(new WeakChangeListener(this, view));
        }
        if (aliasedAction.get() != null) {
            String actionName = aliasedAction.get().getValue(Action.SHORT_DESCRIPTION).toString();
            putValue(Action.SHORT_DESCRIPTION, getShortDescription(actionName));
            String imagePath = aliasedAction.get().getValue(Action.SMALL_ICON).toString();
            putValue(Action.SMALL_ICON, getShortcutImage(imagePath));
        }
    }

    private Action getAliasedActionIfAvailable() {
        if (aliasedAction.get() == null) {
            putValue(Action.SMALL_ICON, brokenAliasAction.get().getIcon());
            return brokenAliasAction.get();
        }
        return this.aliasedAction.get();
    }

    public void actionPerformed(ActionEvent e) {
        getAliasedActionIfAvailable().actionPerformed(e);
    }

    public Object getValue(String key) {
        if (key.equals(Action.SHORT_DESCRIPTION)) {
            return shortDesc;
        }
        else if (key.equals(Action.SMALL_ICON)) {
            return cachedImageIcon;
        }
        return getAliasedActionIfAvailable().getValue(key);
    }

    private String getShortDescription(String actionName) {
        String viewName;
        if (this.viewOfAliasedAction.get() != null) {
            viewName = viewOfAliasedAction.get().getTabTitle();
            this.cachedViewTabTitle = viewName;
        }
        else {
            viewName = this.cachedViewTabTitle;
        }
        this.shortDesc = (actionName + "." + viewName);
        return this.shortDesc;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Action.SHORT_DESCRIPTION)) {
            putValue(evt.getPropertyName(), getShortDescription(evt.getNewValue().toString()));
        }
        else if (evt.getPropertyName().equals(Action.SMALL_ICON)) {
            putValue(evt.getPropertyName(), getShortcutImage(evt.getNewValue().toString()));
        }
        else {
            putValue(evt.getPropertyName(), evt.getNewValue());
        }
    }

    private ImageIcon getShortcutImage(String path) {
        File file = new File(path);
        cachedImageIcon = ImageIconCache.getShortcutImageIcon(file.getName());
        return cachedImageIcon;
    }

    public WeakReference<Action> getAliasedAction() {
        return this.aliasedAction;
    }

    public WeakReference<BrokenAliasAction> getBrokenAliasAction() {
        return this.brokenAliasAction;
    }

    public boolean isUnderlyingActionAvailable() {
        boolean isActionNull = this.aliasedAction.get() == null;
        return !isActionNull;
    }

    public void stateChanged(ChangeEvent e) {
        if ((e.getSource() instanceof View)) {
            View view = (View)e.getSource();
            String tabTitle = view.getTabTitle();
            if (!tabTitle.equals(this.cachedViewTabTitle)) {
                String desc = getShortDescription(getAliasedActionIfAvailable().getValue(Action.SHORT_DESCRIPTION).toString());
                putValue(Action.SHORT_DESCRIPTION, desc);
            }
        }
    }
}