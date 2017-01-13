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
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.7.4.1 $
 */
public class SplitPanel extends JPanel {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;

    private static final int SPLITTER_THICKNESS = 5;

    private Component main;
    private SplitterComponent splitter;
    private int orientation;
    private String name;

    //The MenuPanels are now in a CardLayout rather than just being added and removed
    //This is due to a bug in JavaFX which cause JFXPanels to not be displayed the second and subsequent
    //times they are added to a container.
    //See https://javafx-jira.kenai.com/browse/RT-30536
    private JPanel menuPanelContainer = new JPanel(new CardLayout());
    private Map<MenuPanel, String> menuPanels = new HashMap<MenuPanel, String>();
    private MenuPanel visibleMenuPanel = null;

    private ArrayList changeListeners = new ArrayList();
    private SplitterMouseHandler splitterMouseHandler;

    public SplitPanel(Component main, int orientation) {
        this(null, main, orientation);
    }

    public SplitPanel(String name, Component main, int orientation) {
        super();

        this.name = name;
        this.main = main;
        this.orientation = orientation;
        this.splitterMouseHandler = new SplitterMouseHandler();
        this.splitter = new SplitterComponent();

        setLayout(new SplitLayout());
        add(main);
        add(splitter);
        add(menuPanelContainer);
        menuPanelContainer.setVisible(false);
    }

    public String getName() {
        return name;
    }

    public Component getComponent() {
        return main;
    }

    public MenuPanel getVisibleMenuPanel() {
        return visibleMenuPanel;
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void setVisibleMenuPanel(final MenuPanel visibleMenuPanel) {
        setVisibleMenuPanel(visibleMenuPanel, false);
    }

    public void setVisibleMenuPanel(final MenuPanel visibleMenuPanel, final boolean requestFocus) {
        if(this.visibleMenuPanel != null) {
            hideCard();
            this.visibleMenuPanel = null;
            invalidate();
            validate();
            fireUpdate();

            if (visibleMenuPanel != null) {
                Timer timer = new Timer(200, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        SplitPanel.this.visibleMenuPanel = visibleMenuPanel;
                        showCard();
                        if (requestFocus) {
                            setFocusToMenuPanel(visibleMenuPanel);
                        }
                        invalidate();
                        validate();
                        fireUpdate();
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        } else {
            this.visibleMenuPanel = visibleMenuPanel;
            if(this.visibleMenuPanel != null) {
                showCard();
                if (requestFocus) {
                    setFocusToMenuPanel(visibleMenuPanel);
                }
            }

            invalidate();
            validate();
            fireUpdate();
        }
    }

    private void showCard() {
        String cardName = menuPanels.get(visibleMenuPanel);
        if(cardName == null) {
            cardName = "CARD_"+menuPanels.size();
            menuPanels.put(visibleMenuPanel, cardName);
            menuPanelContainer.add(visibleMenuPanel, cardName);
        }
        ((CardLayout) menuPanelContainer.getLayout()).show(menuPanelContainer, cardName);
        menuPanelContainer.setVisible(true);
        if(visibleMenuPanel.isResizeable()) {
            splitter.addMouseListener(splitterMouseHandler);
            splitter.addMouseMotionListener(splitterMouseHandler);
        }
        splitter.resetCursor();
    }

    private void hideCard() {
        menuPanelContainer.setVisible(false);
        splitter.removeMouseListener(splitterMouseHandler);
        splitter.removeMouseMotionListener(splitterMouseHandler);
        splitter.resetCursor();
    }

    private void setFocusToMenuPanel(MenuPanel menuPanel) {
        Container focusCycleRootAncestor = menuPanel.getFocusCycleRootAncestor();
        if (focusCycleRootAncestor != null) {
            FocusTraversalPolicy focusTraversalPolicy = focusCycleRootAncestor.getFocusTraversalPolicy();
            if (focusTraversalPolicy != null) {
                Component defaultComponent = focusTraversalPolicy.getDefaultComponent(menuPanel);
                if (defaultComponent != null) {
                    defaultComponent.requestFocusInWindow();
                }
            }
        }
    }

    private void fireUpdate() {
        ChangeEvent event = new ChangeEvent(this);
        for (Iterator iterator = changeListeners.iterator(); iterator.hasNext();) {
            ChangeListener listener = (ChangeListener) iterator.next();
            listener.stateChanged(event);
        }
    }

    private int getSplitterOffset() {
        return visibleMenuPanel == null ? 0 : visibleMenuPanel.getSplitterOffset();
    }

    private void setSplitterOffset(int splitterOffset) {
        int min = 0;
        int max = orientation == SplitPanel.LEFT || orientation == SplitPanel.RIGHT
                ? getWidth() - getInsets().left - getInsets().right - SPLITTER_THICKNESS
                : getHeight() - getInsets().top - getInsets().bottom - SPLITTER_THICKNESS;

        if (splitterOffset < min) {
            visibleMenuPanel.setSplitterOffset(0);
        } else if (splitterOffset > max) {
            visibleMenuPanel.setSplitterOffset(max);
        } else {
            visibleMenuPanel.setSplitterOffset(splitterOffset);
        }
    }

    private boolean menuPanelVisible() {
        return visibleMenuPanel != null;
    }

    private class SplitLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component comp) {
        }

        public void layoutContainer(Container parent) {
            // This is done to force the offset to be valid.
            if (visibleMenuPanel != null) {
                setSplitterOffset(visibleMenuPanel.getSplitterOffset());
            }

            Dimension containerSize = parent.getSize();
            Insets containerInsets = parent.getInsets();

            int x;
            int y;
            int width;
            int height;

            switch (orientation) {
                case LEFT:
                    x = containerInsets.left;
                    y = containerInsets.top;
                    height = containerSize.height - (containerInsets.top + containerInsets.bottom);

                    if (menuPanelVisible()) {
                        width = getSplitterOffset();
                        menuPanelContainer.setBounds(x, y, width, height);
                        x += width;

                        width = SPLITTER_THICKNESS;
                        splitter.setBounds(x, y, width, height);
                        x += width;
                    }

                    width = containerSize.width - containerInsets.right - x;
                    main.setBounds(x, y, width, height);
                    break;

                case RIGHT:
                    x = containerSize.width - containerInsets.right;
                    y = containerInsets.top;
                    height = containerSize.height - (containerInsets.top + containerInsets.bottom);

                    if (menuPanelVisible()) {
                        width = getSplitterOffset();
                        x -= width;
                        menuPanelContainer.setBounds(x, y, width, height);

                        width = SPLITTER_THICKNESS;
                        x -= SPLITTER_THICKNESS;
                        splitter.setBounds(x, y, width, height);
                    }

                    width = x - containerInsets.left;
                    x -= width;
                    main.setBounds(x, y, width, height);
                    break;

                case BOTTOM:
                    x = containerInsets.left;
                    y = containerSize.height - containerInsets.bottom;
                    width = containerSize.width - (containerInsets.left + containerInsets.right);

                    if (menuPanelVisible()) {
                        height = getSplitterOffset();
                        y -= height;
                        menuPanelContainer.setBounds(x, y, width, height);

                        height = SPLITTER_THICKNESS;
                        y -= height;
                        splitter.setBounds(x, y, width, height);
                    }

                    height = y - containerInsets.top;
                    y -= height;
                    main.setBounds(x, y, width, height);
                    break;

                case TOP:
                    x = containerInsets.left;
                    y = containerInsets.top;
                    width = containerSize.width - (containerInsets.left + containerInsets.right);

                    if (menuPanelVisible()) {
                        height = getSplitterOffset();
                        menuPanelContainer.setBounds(x, y, width, height);
                        y += height;

                        height = SPLITTER_THICKNESS;
                        splitter.setBounds(x, y, width, height);
                        y += height;
                    }

                    height = containerSize.height - (containerInsets.bottom + containerInsets.top + y);
                    main.setBounds(x, y, width, height);
                    break;
            }
        }

        public Dimension minimumLayoutSize(Container parent) {
            Insets containerInsets = parent.getInsets();
            Dimension mainMinimumSize = main.getMinimumSize();
            return new Dimension(mainMinimumSize.width + containerInsets.left + containerInsets.right,
                    mainMinimumSize.height + containerInsets.top + containerInsets.bottom);
        }

        public Dimension preferredLayoutSize(Container parent) {
            return minimumLayoutSize(parent);
        }

        public void removeLayoutComponent(Component comp) {
        }
    }

    private class SplitterComponent extends JComponent {
        public SplitterComponent() {
        }

        public void resetCursor() {
            if(visibleMenuPanel.isResizeable()) {
                switch (orientation) {
                    case LEFT:
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                        break;
                    case RIGHT:
                        setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                        break;
                    case BOTTOM:
                        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                        break;
                    case TOP:
                        setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                        break;
                }
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        public void paint(Graphics g) {
            super.paint(g);
//                g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private class SplitterMouseHandler extends MouseAdapter implements MouseMotionListener {
        private int clickDistance;
        private int clickedSplitterOffset;
        private int sign = orientation == LEFT || orientation == TOP ? 1 : -1;

        public void mousePressed(MouseEvent e) {
            clickDistance = getClickDistance(e);
            clickedSplitterOffset = getSplitterOffset();
        }

        public void mouseDragged(MouseEvent e) {
            int offset = clickedSplitterOffset + (getClickDistance(e) - clickDistance);
            System.out.println("Offset = "+offset);
            setSplitterOffset(offset);
            invalidate();
            validate();
        }

        public void mouseMoved(MouseEvent e) {
        }

        private int getClickDistance(MouseEvent e) {
            Point clickPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), SplitPanel.this);
            return sign * (orientation == LEFT || orientation == RIGHT ? clickPoint.x : clickPoint.y);
        }
    }
}
