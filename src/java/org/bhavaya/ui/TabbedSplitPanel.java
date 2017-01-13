/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This is an extension of SplitControlPanel, that locks the position of the SplitPanel
 * that is under this class's control to the position of the button (or tab) that
 * activates it.
 *
 * The class consists of a gutter and a component that acts as the main view.  The main view
 * is a SplitPanel created by this class.
 * The gutter can be aligned on the left, right or bottom of the panel.  Example
 *
 * <code>
 *   TabbedSplitPanel tsp = new TabbedSplitPanel(new JPanel(), TabbedSplitPanel.LEFT);
 *   tsp.addMenuPanel(menuPanel1);
 *   tsp.addMenuPanel(menuPanel2);
 * </code>
 *
 * In this example, the TabbedSplitPanel will create a view with a gutter on left hand side that contains two
 * buttons.  These buttons will set MenuPanels on the internally created splitPanel. The splitPanel's
 * main view is an empty JPanel.
 *
 * @author
 * @version $Revision: 1.2 $
 */
public class TabbedSplitPanel extends JPanel {
    private static final Object[] ORIENTATION_BORDER_LAYOUT_MAP = new Object[]{BorderLayout.WEST, BorderLayout.EAST, BorderLayout.SOUTH};
    private static int[] ORIENTATION_TABTEXT_ROTATE_MAP = new int[]{TabTextIcon.CCW, TabTextIcon.CW, TabTextIcon.NONE};
    public static final int LEFT = SplitPanel.LEFT;
    public static final int RIGHT = SplitPanel.RIGHT;
    public static final int BOTTOM = SplitPanel.BOTTOM;

    private Gutter gutter;
    private int orientation;
    private SplitPanel splitPanel;
    private Component component;

    /**
     * Constructs a TabbedSplitPanel with no MenuPanels.
     *
     * @param orientation The location of the gutter.  Can be SplitPanel.LEFT, RIGHT or BOTTOM.
     * //@param splitPanel The SplitPanel that this class will be controlling.  This is most often also the component.
     * @param component The component that sits next the gutter (the main view).  This is often the splitpanel.
     * @see #addMenuPanel(MenuPanel)
     */
    public TabbedSplitPanel(Component component,int orientation) {
        super(new BorderLayout());

        this.orientation = orientation;
        this.splitPanel = new SplitPanel(component, orientation);
        this.component = splitPanel;
        this.gutter = new Gutter(orientation);

        add(gutter, ORIENTATION_BORDER_LAYOUT_MAP[orientation]);
        add(this.component, BorderLayout.CENTER);  // specifically this.component and not component
    }

    public Component getComponent() {
        return component;
    }

    /**
     * Adds a MenuPanel to this class and constructs a button in the gutter to represent it.  If a user clicks
     * on the button, the menu panel will be sent to the SplitPanel member object and the MenuPanel will appear.
     */
    public void addMenuPanel(MenuPanel menuPanel) {
        int splitterOffset = (orientation != SplitPanel.BOTTOM) ? menuPanel.getPreferredSize().width : menuPanel.getPreferredSize().height;
        menuPanel.setSplitterOffset(splitterOffset);
        gutter.addButton(new TabButton(splitPanel, menuPanel, orientation));
    }

    private static class Gutter extends JPanel {
        private static int BUTTON_SPACING = 3;

        private SpringLayout springLayout;

        private String leadingEdge;
        private String trailingEdge;
        private String leftSideEdge;
        private String rightSideEdge;
        private Component referenceComponent;

        public Gutter(int orientation) {
            super();
            this.springLayout = new SpringLayout();

            boolean topOrBotton = orientation == SplitPanel.TOP || orientation == SplitPanel.BOTTOM;
            this.leadingEdge = topOrBotton ? SpringLayout.WEST : SpringLayout.NORTH;
            this.trailingEdge = topOrBotton ? SpringLayout.EAST : SpringLayout.SOUTH;
            this.leftSideEdge = topOrBotton ? SpringLayout.NORTH : SpringLayout.WEST;
            this.rightSideEdge = topOrBotton ? SpringLayout.SOUTH : SpringLayout.EAST;
            this.referenceComponent = this;

            setOpaque(true);
            setLayout(springLayout);
        }

        public void addButton(Component button) {
            add(button);
            if (getComponentCount() == 1) {
                springLayout.putConstraint(leadingEdge, button, BUTTON_SPACING, leadingEdge, referenceComponent);
                springLayout.putConstraint(leftSideEdge, button, 2, leftSideEdge, referenceComponent);
                springLayout.putConstraint(rightSideEdge, this, 2, rightSideEdge, button);
            } else {
                springLayout.putConstraint(leadingEdge, button, BUTTON_SPACING, trailingEdge, referenceComponent);
                springLayout.putConstraint(leftSideEdge, button, 0, leftSideEdge, referenceComponent);
                springLayout.putConstraint(rightSideEdge, button, 0, rightSideEdge, referenceComponent);
            }
            this.referenceComponent = button;
        }
    }

    private static class TabButton extends JPanel {
        private static final int ICON_X_PAD = 6;
        private static final int ICON_Y_PAD = 2;

        private SplitPanel splitPanel;
        private MenuPanel menuPanel;
        private TabTextIcon tabTextIcon;
        private int orientation;

        private boolean selected = false;
        private boolean pressed = false;
        private boolean armed = false;
        private boolean over = false;


        public TabButton(SplitPanel splitPanel, MenuPanel menuPanel, int orientation) {
            super(new BorderLayout());

            this.splitPanel = splitPanel;
            this.menuPanel = menuPanel;
            this.orientation = orientation;
            this.tabTextIcon = new TabTextIcon(menuPanel.getName(), ORIENTATION_TABTEXT_ROTATE_MAP[orientation]);

            setBorder(null);
            setFocusable(false);

            addMouseListener(new MouseHandler());

            splitPanel.addChangeListener(new ChangeHandler());
        }

        public void updateUI() {
            super.updateUI();
            if (menuPanel != null) SwingUtilities.updateComponentTreeUI(menuPanel);
        }

        private void updateSplitPanel() {
            if (splitPanel.getVisibleMenuPanel() == menuPanel) {
                splitPanel.setVisibleMenuPanel(null);
            } else {
                splitPanel.setVisibleMenuPanel(menuPanel);
            }
        }

        private class MouseHandler extends MouseAdapter {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    pressed = true;
                    armed = true;
                    repaint();
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && pressed) {
                    pressed = false;
                    armed = false;
                    updateSplitPanel();
                }
            }

            public void mouseExited(MouseEvent e) {
                over = false;
                if (pressed) armed = false;
                repaint();
            }

            public void mouseEntered(MouseEvent e) {
                over = true;
                if (pressed) armed = true;
                repaint();
            }
        }

        private class ChangeHandler implements ChangeListener {
            public void stateChanged(ChangeEvent e) {
                selected = splitPanel.getVisibleMenuPanel() == menuPanel;
                repaint();
            }
        }

        public void paint(Graphics g) {
            super.paint(g);

            Color normalColor = getBackground();
            Color rolloverColor = UIUtilities.createDarkerColor(getBackground(), 0.08f);
            Color darkerColour = UIUtilities.createDarkerColor(rolloverColor, 0.18f);

            int x = (getWidth() - tabTextIcon.getIconWidth()) / 2;
            int y = (getHeight() - tabTextIcon.getIconHeight()) / 2;

            Color oldColor = g.getColor();
            if (selected || armed) {
                g.setColor(darkerColour);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
            } else if (over) {
                g.setColor(darkerColour);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
            } else {
                g.setColor(normalColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.setColor(oldColor);
            tabTextIcon.paintIcon(this, g, x, y);
        }

        public Dimension getPreferredSize() {
            boolean topOrBotton = orientation == SplitPanel.TOP || orientation == SplitPanel.BOTTOM;
            int xPad = topOrBotton ? ICON_X_PAD * 2 : ICON_Y_PAD * 2;
            int yPad = topOrBotton ? ICON_Y_PAD * 2 : ICON_X_PAD * 2;
            return new Dimension(tabTextIcon.getIconWidth() + xPad, tabTextIcon.getIconHeight() + yPad);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }
}
