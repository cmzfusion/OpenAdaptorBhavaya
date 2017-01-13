package org.bhavaya.javafxui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.bhavaya.ui.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Created with IntelliJ IDEA.
 * User: ga2mop0
 * Date: 29/07/13
 * Time: 17:08
 */
public class JavaFxMenuTabButton extends JavaFxTabButton {

    private SplitControlPanel controlPanel;
    private SplitPanel splitPanel;
    private MenuPanel menuPanel;
    private MenuTabButtonAlternateAction alternateAction;

    public JavaFxMenuTabButton(final SplitControlPanel controlPanel, final SplitPanel splitPanel, final MenuPanel menuPanel, MenuTabButtonAlternateAction alternateAction) {
        super(menuPanel.getDisplayName(), menuPanel.getMnemonic(), menuPanel.getName(), true);
        this.controlPanel = controlPanel;
        this.splitPanel = splitPanel;
        this.menuPanel = menuPanel;
        this.alternateAction = alternateAction;

        splitPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                controlPanel.cancelHidePanelTimer();
                setSelected(splitPanel.getVisibleMenuPanel() == menuPanel);
            }
        });

        onActionProperty().setValue(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                updateSplitPanel();
            }
        });
    }

    private void updateSplitPanel() {
        if(alternateAction == null || alternateAction.performAlternateAction()) {
            //Either no alternate action or we should continue anyway
            controlPanel.cancelHidePanelTimer();
            menuPanel.stopPanelButtonFlashing();
            if (splitPanel.getVisibleMenuPanel() == menuPanel) {
                splitPanel.setVisibleMenuPanel(null);
            } else {
                splitPanel.setVisibleMenuPanel(menuPanel, true);
            }
            SplitControlPanel.setAllPanelsHiddenByF12(false);
        } else {
            //clicking the button will have selected or deselected, so if not showing/hiding splitpanel we should toggle the selected mode
            toggleSelected();
        }
    }
}
