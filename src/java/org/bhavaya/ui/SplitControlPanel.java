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
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.ref.WeakReference;

/**
 * The class consists of a gutter and a component that acts as the main view.  The gutter can be aligned on the
 * left, right or bottom of the panel.  This class also contains a reference to the a SplitPanel which this
 * class is responsible for controlling.  Example:
 * <p/>
 * <code>
 * SplitControlPanel scp = new SplitControlPanel(SplitPanel.LEFT, splitPanel, splitPanel);
 * scp.addMenuPanel(menuPanel1);
 * scp.addMenuPanel(menuPanel2);
 * </code>
 * <p/>
 * In this example, the SplitControlPanel will create a view with a gutter on left hand side that contain two
 * buttons.  These buttons will set MenuPanels on the splitPanel.
 * <p/>
 *
 * @author
 * @version $Revision: 1.9.4.2 $
 */
public class SplitControlPanel extends JPanel {
    private static final Object[] ORIENTATION_BORDER_LAYOUT_MAP = new Object[]{BorderLayout.WEST, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.NORTH};
    private static int[] ORIENTATION_TABTEXT_ROTATE_MAP = new int[]{TabTextIcon.CCW, TabTextIcon.CW, TabTextIcon.NONE, TabTextIcon.NONE};

    private Gutter gutter;
    private int orientation;
    private SplitPanel splitPanel;
    private Component component;
    private Timer hidePanelTimer;

    private static boolean allPanelsHiddenByF12 = false;
    private static KeyEventPostProcessorImpl keyEventProcessor;
    private MenuPanel panelToRestore;

    private MenuTabButtonFactory menuTabButtonFactory = new MenuTabButtonFactory() {
        @Override
        public void createTabButtonForMenuPanel(SplitControlPanel controlPanel, SplitPanel splitPanel, MenuPanel menuPanel, int orientation, MenuTabButtonAlternateAction alternateAction) {
            DefaultMenuTabButton button = new DefaultMenuTabButton(controlPanel, splitPanel, menuPanel, orientation);
            menuPanel.setFlashingTabButton(button);
            if(gutter != null) {
                gutter.addButton(button);
            }
        }
    };

    static {
        keyEventProcessor = new KeyEventPostProcessorImpl();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(keyEventProcessor);
    }

    /**
     * Constructs a SplitControlPanel with no MenuPanels.
     *
     * @param orientation The location of the gutter.  Can be SplitPanel.LEFT, RIGHT or BOTTOM.
     * @param splitPanel  The SplitPanel that this class will be controlling.  This is most often also the component.
     * @param component   The component that sits next the gutter (the main view).  This is often the splitpanel.
     * @see #addMenuPanel(MenuPanel)
     */
    public SplitControlPanel(int orientation, SplitPanel splitPanel, Component component) {
        this(orientation, splitPanel, component, true);
    }

    /**
     * Constructs a SplitControlPanel with no MenuPanels.
     *
     * @param orientation   The location of the gutter.  Can be SplitPanel.LEFT, RIGHT or BOTTOM.
     * @param splitPanel    The SplitPanel that this class will be controlling.  This is most often also the component.
     * @param component     The component that sits next the gutter (the main view).  This is often the splitpanel.
     * @param gutterVisible True if the gutter should be displayed. If this value is set false the owner takes
     *                      responsibility for displaying menu panel buttons
     * @see #addMenuPanel(MenuPanel)
     */

    public SplitControlPanel(int orientation, SplitPanel splitPanel, Component component, boolean gutterVisible) {
        super(new BorderLayout());

        this.orientation = orientation;
        this.splitPanel = splitPanel;
        this.component = component;

        if(gutterVisible) {
            this.gutter = new Gutter(orientation);
            add(gutter, ORIENTATION_BORDER_LAYOUT_MAP[orientation]);
        }
        add(component, BorderLayout.CENTER);

        keyEventProcessor.registerSplitControlPanel(this);
    }

    public Component getComponent() {
        return component;
    }

    public void setMenuTabButtonFactory(MenuTabButtonFactory menuTabButtonFactory) {
        this.menuTabButtonFactory = menuTabButtonFactory;
    }

    /**
     * Adds a MenuPanel to this class and constructs a button in the gutter to represent it.  If a user clicks
     * on the button, the menu panel will be sent to the SplitPanel member object and the MenuPanel will appear.
     */
    public void addMenuPanel(final MenuPanel menuPanel) {
        addMenuPanel(menuPanel, null);
    }

    public void addMenuPanel(final MenuPanel menuPanel, MenuTabButtonAlternateAction alternateAction) {
        menuTabButtonFactory.createTabButtonForMenuPanel(this, splitPanel, menuPanel, ORIENTATION_TABTEXT_ROTATE_MAP[orientation], alternateAction);
        registerHotKey(menuPanel);
        menuPanel.getComponent().addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                cancelHidePanelTimer();
                menuPanel.stopPanelButtonFlashing();
            }
        });
    }

    public void addToGutter(Component component) {
        if(gutter != null) {
            gutter.addComponentToEnd(component);
        }
    }

    private void registerHotKey(MenuPanel menuPanel) {
        if (menuPanel.getMnemonic() != KeyEvent.VK_UNDEFINED) {
            keyEventProcessor.registerHotKeyForPanel(this, menuPanel, menuPanel.getMnemonic());
        }
    }

    private void restoreHiddenPanels() {
        if (panelToRestore != null) {
            setVisibleMenuPanel(panelToRestore);
        }
    }

    private void hidePanels() {
        panelToRestore = splitPanel.getVisibleMenuPanel();
        setVisibleMenuPanel(null);
    }

    /**
     * Determines whether this component will be displayed on the screen
     * if it's displayable.
     * @return <code>true</code> if the component and all of its ancestors
     * 		are visible, <code>false</code> otherwise
     */
    private boolean isRecursivelyVisible() {
        /**
         * Doesn't do the thing in a recursive way but does exactly the same as the same named method in Component class.
         */
        if (!isVisible()) return false;
        Component component = this;
        while ((component = component.getParent()) != null) {
            if (!component.isVisible()) return false;
        }
        return true;
    }

    public void setVisibleMenuPanel(MenuPanel menuPanel) {
        setVisibleMenuPanel(menuPanel, 0);
    }

    public void setVisibleMenuPanel(MenuPanel menuPanel, int hideAfterMillis) {
        if (splitPanel.getVisibleMenuPanel() != menuPanel) {
            allPanelsHiddenByF12 = false;
            splitPanel.setVisibleMenuPanel(menuPanel);
            if (hideAfterMillis > 0) {
                setupHidePanelTimer(hideAfterMillis);
            }
        }
    }

    private synchronized void setupHidePanelTimer(int hideAfterMillis) {
        if (hidePanelTimer != null) cancelHidePanelTimer();
        hidePanelTimer = new Timer(hideAfterMillis, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized (SplitControlPanel.this) {
                    hidePanelTimer = null;
                }
                splitPanel.setVisibleMenuPanel(null);
            }
        });
        hidePanelTimer.setRepeats(false);
        hidePanelTimer.start();
    }

    public synchronized void cancelHidePanelTimer() {
        if (hidePanelTimer != null) {
            hidePanelTimer.stop();
            hidePanelTimer = null;
        }
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

            boolean topOrBottom = orientation == SplitPanel.TOP || orientation == SplitPanel.BOTTOM;
            this.leadingEdge = topOrBottom ? SpringLayout.WEST : SpringLayout.NORTH;
            this.trailingEdge = topOrBottom ? SpringLayout.EAST : SpringLayout.SOUTH;
            this.leftSideEdge = topOrBottom ? SpringLayout.NORTH : SpringLayout.WEST;
            this.rightSideEdge = topOrBottom ? SpringLayout.SOUTH : SpringLayout.EAST;
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

        public void addComponentToEnd(Component component) {
            add(component);
            springLayout.putConstraint(trailingEdge, component, -BUTTON_SPACING, trailingEdge, this);
            springLayout.putConstraint(leftSideEdge, this, 10, leftSideEdge, component);
            springLayout.putConstraint(rightSideEdge, component, 0, rightSideEdge, this);
        }
    }

    private static class KeyEventPostProcessorImpl implements KeyEventPostProcessor {
        private Map controlPanelList = new WeakHashMap();
        private HashMap hotKeyToPanelMap = new HashMap();

        /**
         * Registers the control panel to support Ctrl+Shift+F12 hide/restore functionality. 
         * @param splitControlPanel
         */
        public void registerSplitControlPanel(SplitControlPanel splitControlPanel) {
            controlPanelList.put(splitControlPanel, null);
        }

        public void registerHotKeyForPanel(SplitControlPanel splitControlPanel, MenuPanel menuPanel, int mnemonic) {
            Integer key = new Integer(mnemonic);
            Map listOfPanels = (Map) hotKeyToPanelMap.get(key);
            if (listOfPanels == null) {
                listOfPanels = new WeakHashMap();
                hotKeyToPanelMap.put(key, listOfPanels);
            }
            listOfPanels.put(menuPanel, new WeakReference(splitControlPanel));
        }

        public boolean postProcessKeyEvent(KeyEvent e) {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if (e.getModifiers() == (KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK) && e.getKeyCode() == KeyEvent.VK_F12) {
                // hide all visible panels / restore before hide state
                if (allPanelsHiddenByF12) {
                    restoreAllHiddenPanels();
                } else {
                    hideAllPanels();
                }
                return true;
            } else if (hotKeyToPanelMap.size() > 0 && e.getModifiers() == KeyEvent.ALT_MASK) {
                return processPanelHotkey(e);
            } else {
                return false;
            }
        }

        /**
         * Processes side panel hotkey press e.g. Alt + <mnemonics>
         */
        private boolean processPanelHotkey(KeyEvent e) {
            int keyCode = e.getKeyCode();
            Map listOfPanels = (Map) hotKeyToPanelMap.get(new Integer(keyCode));
            if (listOfPanels != null) {
                boolean singlePanelForHotKey = listOfPanels.size() == 1;
                Iterator iterator = listOfPanels.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    MenuPanel menuPanel = (MenuPanel) entry.getKey();
                    if (menuPanel == null) continue;  // Weak entries are only expunged on put()/get()/etc.

                    WeakReference weakReference = (WeakReference) entry.getValue();
                    SplitControlPanel splitControlPanel = (SplitControlPanel) weakReference.get();
                    if (splitControlPanel == null) continue; // Can also be garbage collected.

                    Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
                    boolean inFocusedWindow = focusedWindow.isAncestorOf(splitControlPanel);
                    boolean panelVisible = splitControlPanel.isRecursivelyVisible();

                    boolean proceed = panelVisible && (singlePanelForHotKey || inFocusedWindow);
                    if (proceed) {
                        splitControlPanel.cancelHidePanelTimer();
                        menuPanel.stopPanelButtonFlashing();
                        if (splitControlPanel.splitPanel.getVisibleMenuPanel() == menuPanel) {
                            splitControlPanel.splitPanel.setVisibleMenuPanel(null);
                        } else {
                            splitControlPanel.splitPanel.setVisibleMenuPanel(menuPanel, true);
                        }
                    }
                }
                allPanelsHiddenByF12 = false;
                return true;
            } else {
                return false;
            }
        }

        private void hideAllPanels() {
            Iterator keySetIterator = controlPanelList.keySet().iterator();
            //noinspection WhileLoopReplaceableByForEach
            while (keySetIterator.hasNext()) {
                SplitControlPanel controlPanel = (SplitControlPanel) keySetIterator.next();
                if (controlPanel != null) controlPanel.hidePanels();
            }
            allPanelsHiddenByF12 = true;
        }

        private void restoreAllHiddenPanels() {
            Iterator keySetIterator = controlPanelList.keySet().iterator();
            while (keySetIterator.hasNext()) {
                SplitControlPanel controlPanel = (SplitControlPanel) keySetIterator.next();
                if (controlPanel != null) controlPanel.restoreHiddenPanels();
            }
            allPanelsHiddenByF12 = false;
        }
    }

    public static void setAllPanelsHiddenByF12(boolean allPanelsHiddenByF12) {
        SplitControlPanel.allPanelsHiddenByF12 = allPanelsHiddenByF12;
    }
}
