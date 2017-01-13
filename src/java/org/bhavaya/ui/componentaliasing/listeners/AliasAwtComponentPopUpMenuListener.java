package org.bhavaya.ui.componentaliasing.listeners;

import org.bhavaya.ui.view.ViewUtils;
import org.bhavaya.ui.componentaliasing.alias.AliasButton;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created ga2adaz
 * A popupmenu to be used on toolbar buttons which are aliases of other (aliasable) toolbar buttons. The main purpose is to
 * allow the removal of alias button. //TODO WILL NEEED TO UPDATE CONFIG
 */
public class AliasAwtComponentPopUpMenuListener extends AbstractPopUpMenuHandler {

    private static final Log log = Log.getCategory(AliasAwtComponentPopUpMenuListener.class);

    final String alisableComponentIdentifier;
    final long orginatingViewId;
    final long homeViewId;
    RemoveAliasActionListener removeAliasActionListener;
    JPopupMenu jPopupMenu;

    public AliasAwtComponentPopUpMenuListener(String alisableComponentIdentifier, long orginatingViewId, long homeViewName) {
        jPopupMenu = new  JPopupMenu();
        this.alisableComponentIdentifier = alisableComponentIdentifier;
        this.orginatingViewId = orginatingViewId;
        this.homeViewId =homeViewName;
    }

    public JPopupMenu getPopupMenu() {
        jPopupMenu.removeAll();
        JMenuItem jMenuItem = new JMenuItem("Remove This Alias Button");
        removeAliasActionListener = new RemoveAliasActionListener();
        jMenuItem.addActionListener(removeAliasActionListener);
        jPopupMenu.add(jMenuItem);
        return jPopupMenu;
    }

    class RemoveAliasActionListener implements ActionListener{

        public void actionPerformed(ActionEvent e) {
            View newView = ViewUtils.getView(homeViewId);
            String id = AliasButton.createIdentifier(alisableComponentIdentifier, orginatingViewId, homeViewId);
            newView.removeAliasComponentFromToolbar(id);
            Workspace.getInstance().forceUpdate();
        }
    }
}
