package org.bhavaya.ui.componentaliasing.listeners;



import org.bhavaya.ui.view.ViewUtils;
import org.bhavaya.ui.componentaliasing.alias.AliasButton;
import org.bhavaya.ui.view.View;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Created ga2adaz
 * A popupmenu to be used on toolbar buttons which are aliasable. The popup menu allows the user to select a view, from a list
 * of the currently open views, and add an aliased button to it.
 */
public class AliasableAwtComponentPopUpMenuListener extends AbstractPopUpMenuHandler {


    private static final Log log = Log.getCategory(AliasableAwtComponentPopUpMenuListener.class);

    private String alisableComponentIdentifier;
    private static String ALL_VIEWS_SELECTION_STRING = "All Open Views";
    private static String SELECTION_PREFIX_STRING = "Add Button to ";
    private long orginatingViewId;

    private CreateAliasActionListener createAliasActionListener;
    JPopupMenu jPopupMenu;

    public AliasableAwtComponentPopUpMenuListener(String alisableComponentIdentifier, long orginatingViewId) {
        super();
        this.alisableComponentIdentifier = alisableComponentIdentifier;
        this.orginatingViewId = orginatingViewId;
        createAliasActionListener = new CreateAliasActionListener();
        jPopupMenu = new JPopupMenu();
    }

    //Repopulated each time as open views change.
    @Override
    public JPopupMenu getPopupMenu() {
        jPopupMenu.removeAll();
        Map<Long,View> selectableViews = ViewUtils.getViews();
        selectableViews.remove(orginatingViewId);
        for(View view : selectableViews.values()) {
            jPopupMenu.add(createJMenuItem(SELECTION_PREFIX_STRING + view.getTabTitle()));
        }
        if(selectableViews.keySet().size() > 1) {
            jPopupMenu.addSeparator();
            jPopupMenu.add(createJMenuItem(ALL_VIEWS_SELECTION_STRING));
        }
        else if(selectableViews.keySet().size() == 0) {
            JMenuItem jMenuItem = createJMenuItem("No open views to add button to");
            jMenuItem.setEnabled(false);
            jPopupMenu.add(jMenuItem);
        }

        return jPopupMenu;
    }

    private JMenuItem createJMenuItem(String label) {
        JMenuItem jMenuItem = new JMenuItem(label);
        jMenuItem.addActionListener(createAliasActionListener);
        return jMenuItem;
    }


    class CreateAliasActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Map<Long,View> selectableViews = ViewUtils.getViews();
            selectableViews.remove(orginatingViewId);
            String selected = e.getActionCommand();

            if (selected.equals(ALL_VIEWS_SELECTION_STRING)) {
                View[] viewsAsArray = selectableViews.values().toArray(new View[0]);
                addAliasButtonToView(alisableComponentIdentifier,viewsAsArray);
            }
            else {
                String viewTabTitle = selected.replaceAll(SELECTION_PREFIX_STRING,"");
                View view = selectableViews.get(ViewUtils.getViewId(viewTabTitle));
                if(view !=null) {
                    addAliasButtonToView(alisableComponentIdentifier,view);
                }
                else {
                    log.error("Should not be null");
                }
            }
        }


        private void addAliasButtonToView(String alisableComponentIdentifier, View... views) {
            for(View view : views) {
                AliasButton aliasButton = new AliasButton(alisableComponentIdentifier, orginatingViewId, view.getViewId());
                view.addAliasComponentToToolbar(aliasButton);

            }
        }

    }







}
