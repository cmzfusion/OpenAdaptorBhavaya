package org.bhavaya.ui;

/**
 * Factory for creating a tab button for a MenuPanel.
 * Implementations are responsible for adding the button to the appropriate container
 * User: ga2mop0
 * Date: 29/07/13
 * Time: 15:19
 */
public interface MenuTabButtonFactory {
    public void createTabButtonForMenuPanel(SplitControlPanel controlPanel, SplitPanel splitPanel, MenuPanel menuPanel, int orientation, MenuTabButtonAlternateAction alternateAction);
}
