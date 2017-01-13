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
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Description
 *
 * @author Tim Parker
 * @version $Revision: 1.1 $
 */
public class CheckTree extends JTree {
    private static final int MAX_SEARCH_DEPTH = 5;

    private static final Set navigationKeyCodeSet = new HashSet(Arrays.asList(new Object[]{
        new Integer(KeyEvent.VK_LEFT), new Integer(KeyEvent.VK_RIGHT), new Integer(KeyEvent.VK_UP),
        new Integer(KeyEvent.VK_DOWN), new Integer(KeyEvent.VK_HOME), new Integer(KeyEvent.VK_END),
        new Integer(KeyEvent.VK_PAGE_DOWN), new Integer(KeyEvent.VK_PAGE_UP)}));
    private boolean match;
    private JLabel popupLabel;

    private Rectangle checkBoxClickRectangle;

    private Component popupOwner;
    private int popupXOffset;
    private int popupYOffset;

    private StringBuffer searchNarrowText = new StringBuffer();
    private Popup popup;


    public CheckTree() {

        this.checkBoxClickRectangle = getClickBoxCheckRectangle();

        this.popupLabel = new JLabel("<html><b>Search for: </b></html>");
        this.popupLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        this.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) {
                    searchNarrowText.setLength(0);
                    controlPopup();
                }
            }
        });

        setToggleClickCount(-1);
        setExpandsSelectedPaths(true);
        setScrollsOnExpand(true);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    Rectangle rowRectangle = getRowBounds(getRowForLocation(e.getX(), e.getY()));
                    Rectangle checkBoxRectangle = new Rectangle(rowRectangle.x + checkBoxClickRectangle.x, rowRectangle.y + checkBoxClickRectangle.y,
                            checkBoxClickRectangle.width, checkBoxClickRectangle.height);
                    boolean valid = checkBoxRectangle.contains(e.getPoint());
                    if (valid) {
                        ((CheckModel) treeModel).nodeToggled(path);
                        repaint();
                    }
                }
            }
        });

        setCellRenderer(new CheckBoxTreeCellRenderer());
        putClientProperty("JTree.lineStyle", "Angled");
    }

    public void setModel(TreeModel newModel) {
        if (newModel instanceof CheckModel) {
            super.setModel(newModel);
            TreePath[] statefulPaths = ((CheckModel) newModel).getPathsWithState();
            if (statefulPaths != null) {
                for (int i = 0; i < statefulPaths.length; i++) {
                    makeVisible(statefulPaths[i]);
                }
            }
        }
    }

    public void setPopupOwner(Component popupOwner, int popupXOffset, int popupYOffset) {
        this.popupOwner = popupOwner;
        this.popupXOffset = popupXOffset;
        this.popupYOffset = popupYOffset;
    }

    public Dimension getPopupPreferredSize() {
        return popupLabel.getPreferredSize();
    }

    private Rectangle getClickBoxCheckRectangle() {
        CheckIcon checkIcon = new CheckIcon(true, null);
        JLabel testLabel = new JLabel("Fred", checkIcon, JLabel.LEFT);
        Insets insets = testLabel.getInsets();
        Rectangle rect = new Rectangle(insets.left, testLabel.getInsets().top, checkIcon.getIconWidth(), checkIcon.getIconWidth());
        return rect;
    }

    protected void processKeyEvent(KeyEvent e) {
        if (popupOwner != null) {
            // This stops the root from being collapsed.
            if (getSelectionPath() != null && getSelectionPath().getLastPathComponent() == getModel().getRoot() && e.getKeyCode() == KeyEvent.VK_LEFT) {
                return;
            } else if (isNavigationKey(e)) {
                super.processKeyEvent(e);
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getID() == KeyEvent.KEY_PRESSED && popup == null) {
                ((CheckModel) treeModel).nodeToggled(getSelectionPath());
                repaint();
            } else if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_BACK_SPACE && searchNarrowText.length() > 0) {
                searchNarrowText.setLength(searchNarrowText.length() - 1);
                narrow();
                controlPopup();
            } else if (e.getID() == KeyEvent.KEY_PRESSED && (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                searchNarrowText.setLength(0);
                controlPopup();
            } else if (e.getID() == KeyEvent.KEY_PRESSED && (isValidPropertyChar(e.getKeyChar()))) {
                searchNarrowText.append(e.getKeyChar());
                narrow();
                controlPopup();
            }
        } else {
            super.processKeyEvent(e);
        }
    }

    private boolean isValidPropertyChar(char c) {
        return Character.isLetterOrDigit(c)
                || Character.isSpaceChar(c)
                || c == '_'
                || c == '('
                || c == ')';
    }

    private void controlPopup() {
        if (searchNarrowText.length() > 0) {
            popupLabel.setText("<html><b>Search for:</b> " + (match ? "<font color=black>" : "<font color=red>") + searchNarrowText + "</font></html>");
            Point origin = new Point(popupXOffset, popupYOffset);
            SwingUtilities.convertPointToScreen(origin, popupOwner);

            if (popup != null) {
                popup.hide();
            }

            popup = PopupFactory.getSharedInstance().getPopup(popupOwner, popupLabel, origin.x, origin.y);
            popup.show();
        } else if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    private boolean isNavigationKey(KeyEvent e) {
        int keyCode = e.getKeyCode();
        return navigationKeyCodeSet.contains(new Integer(keyCode));
    }

    private void narrow() {
        String narrowText = searchNarrowText.toString().toLowerCase();
        match = narrow(narrowText, new TreePath(treeModel.getRoot()), 0);
    }

    private boolean narrow(String narrowText, TreePath path, int depth) {
        // A simple pragmatic way to avoid the pitfalls of infinite recursion.
        if (depth == MAX_SEARCH_DEPTH) {
            return false;
        }

        Object parentObject = path.getLastPathComponent();

        // Breadth-first tree walk seems to make most sense from a user point of view
        for (int i = 0; i < treeModel.getChildCount(parentObject); i++) {
            Object child = treeModel.getChild(parentObject, i);
            if (child.toString().toLowerCase().startsWith(narrowText)) {
                TreePath clientPath = path.pathByAddingChild(child);
                setSelectionPath(clientPath);
                scrollPathToVisible(clientPath);
                return true;
            }
        }

        for (int i = 0; i < treeModel.getChildCount(parentObject); i++) {
            if (narrow(narrowText, path.pathByAddingChild(treeModel.getChild(parentObject, i)), depth + 1)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This renderer is respsonsible for displaying the selection.
     */
    private class CheckBoxTreeCellRenderer extends DefaultTreeCellRenderer {
        private Icon checkOn = new CheckIcon(true, null);
        private Icon checkOff = new CheckIcon(false, null);
        private Icon checkPartial = new CheckIcon(true, Color.LIGHT_GRAY);

        public CheckBoxTreeCellRenderer() {
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, sel);
            Icon icon = null;
            if (treeModel instanceof CheckModel) {
                boolean checked = ((CheckModel) treeModel).isChecked(getPathForRow(row));
                if (checked) {
                    CheckModel checkModel = (CheckModel) treeModel;
                    if (checkModel.subtreeVaries(getPathForRow(row))) {
                        icon = checkPartial;
                    } else {
                        icon = checkOn;
                    }
                } else {
                    icon = checkOff;
                }
                c.setIcon(icon);
            }
            return c;
        }
    }


    /**
     * an icon that draws a check box.
     * the drawing code has been ripped from MetalIconFactory.CheckBoxIcon
     */
    private static class CheckIcon implements Icon {
        boolean selected;
        private Color background;
        int controlSize = 13;


        public CheckIcon(boolean selected, Color background) {
            this.selected = selected;
            this.background = background;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            drawFlush3DBorder(g, x, y, controlSize, controlSize);

            if (selected) {
                g.setColor(background != null ? background : MetalLookAndFeel.getControlInfo());
                g.fillRect(x + 3, y + 5, 2, controlSize - 8);

                g.drawLine(x + (controlSize - 4), y + 3, x + 5, y + (controlSize - 6));
                g.drawLine(x + (controlSize - 4), y + 4, x + 5, y + (controlSize - 5));
            }
        }

        public int getIconWidth() {
            return controlSize;
        }

        public int getIconHeight() {
            return controlSize;
        }

        /**
         * This draws the "Flush 3D Border" which is used throughout the Metal L&F
         */
        static void drawFlush3DBorder(Graphics g, int x, int y, int w, int h) {
            g.translate(x, y);
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(0, 0, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRect(1, 1, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControl());
            g.drawLine(0, h - 1, 1, h - 2);
            g.drawLine(w - 1, 0, w - 2, 1);
            g.translate(-x, -y);
        }
    }
}