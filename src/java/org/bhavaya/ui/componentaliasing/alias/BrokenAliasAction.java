package org.bhavaya.ui.componentaliasing.alias;

import org.bhavaya.ui.view.ViewUtils;
import org.bhavaya.ui.view.View;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: ga2adaz
 * Date: 23/07/12
 * Time: 15:25
 * To change this template use File | Settings | File Templates.
 */
public class BrokenAliasAction extends AbstractAction{

    String viewName;
    long viewId;
    final String aliasedComponentIdentifier;

    public BrokenAliasAction(long viewId, String aliasedComponentIdentifier) {
        super();
        this.viewId = viewId;
        this.aliasedComponentIdentifier = aliasedComponentIdentifier;
        this.putValue(Action.SHORT_DESCRIPTION, viewName+"_"+aliasedComponentIdentifier);
        this.putValue(Action.SMALL_ICON, getIcon());
        //try and get the viewName. The view may not be open though.
        getViewName();
    }

    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, "Alias button requires " + getViewName() + " view to be open.");
    }

    public Object getIcon() {
        return ImageIconCache.getImageIcon("navigate_cross.png");
    }

    public String getViewName() {
        if(viewName == null) {
            View view =  ViewUtils.getView(viewId);
            if(view != null) {
                viewName = view.getName();
            }
        }
        return viewName;
    }
}
