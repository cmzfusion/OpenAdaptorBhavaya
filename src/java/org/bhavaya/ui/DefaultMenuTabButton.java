package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: ga2mop0
 * Date: 29/07/13
 * Time: 15:10
 */
public class DefaultMenuTabButton extends TabButton {
    private SplitControlPanel controlPanel;
    private SplitPanel splitPanel;
    private MenuPanel menuPanel;

    public DefaultMenuTabButton(final SplitControlPanel controlPanel, final SplitPanel splitPanel, final MenuPanel menuPanel, int orientation) {
        super(menuPanel.getDisplayName(), menuPanel.getMnemonic(), menuPanel.getName(), orientation);

        this.controlPanel = controlPanel;
        this.splitPanel = splitPanel;
        this.menuPanel = menuPanel;
        splitPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                controlPanel.cancelHidePanelTimer();
                setSelected(splitPanel.getVisibleMenuPanel() == menuPanel);
                repaint();
            }
        });
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSplitPanel();
            }
        });
    }

    public void updateUI() {
        super.updateUI();
        if (menuPanel != null) SwingUtilities.updateComponentTreeUI(menuPanel);
    }

    private void updateSplitPanel() {
        controlPanel.cancelHidePanelTimer();
        menuPanel.stopPanelButtonFlashing();
        if (splitPanel.getVisibleMenuPanel() == menuPanel) {
            splitPanel.setVisibleMenuPanel(null);
        } else {
            splitPanel.setVisibleMenuPanel(menuPanel, true);
        }
        SplitControlPanel.setAllPanelsHiddenByF12(false);
    }
}