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

import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.PropertyModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Description
 *
 * @author Daniel Van Enckevort
 * @version $Revision: 1.7 $
 */
public class BeanPathSelector extends JTree {
    private static Rectangle clickBoxCheckRectangle = null;

    private BeanCollectionTableModel beanCollectionTableModel;
    private SelectionModel selectionModel;
    private PropertyToolTipFactory propertyToolTipFactory = new BeanpathSelectorPropertyToolTipFactory();

    public BeanPathSelector(Class beanType, FilteredTreeModel.Filter addPropertyFilter, FilteredTreeModel.Filter addChildrenFilter, SelectionModel selectionModel) {
        super(new FilteredTreeModel(BeanPropertyTreeModel.getInstance(beanType), addPropertyFilter, addChildrenFilter, FilteredTreeModel.DEFAULT_INLINE_CHILDREN_FILTER));
        this.selectionModel = selectionModel;
        init();
    }

    public BeanPathSelector(BeanCollectionTableModel beanCollectionTableModel, FilteredTreeModel.Filter addPropertyFilter, FilteredTreeModel.Filter addChildrenFilter, SelectionModel selectionModel) {
        super(new FilteredTreeModel(BeanPropertyTreeModel.getInstance(beanCollectionTableModel.getBeanType()), addPropertyFilter, addChildrenFilter, FilteredTreeModel.DEFAULT_INLINE_CHILDREN_FILTER));
        this.beanCollectionTableModel = beanCollectionTableModel;
        this.selectionModel = selectionModel;
        this.beanCollectionTableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getFirstRow() == TableModelEvent.HEADER_ROW && e.getLastRow() == TableModelEvent.HEADER_ROW) {
                    repaint();
                }
            }
        });
        init();
    }

    private void init() {
        setToggleClickCount(-1);
        setExpandsSelectedPaths(true);
        setScrollsOnExpand(true);

        addMouseListener(new BeanPropertyTreeMouseHandler(getBeanPathSelectionModel()));

        //set the renderer so we see nodes with check boxes as their icons.
        setCellRenderer(new PropertyTreeCellRenderer(getBeanPathSelectionModel()));
        putClientProperty("JTree.lineStyle", "Angled");
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void updateUI() {
        super.updateUI();
        if (beanCollectionTableModel != null) setCellRenderer(new PropertyTreeCellRenderer(getBeanPathSelectionModel()));
    }

    protected static Rectangle getClickBoxCheckRectangle() {
        if (clickBoxCheckRectangle == null) {
            CheckIcon checkIcon = new CheckIcon(true);
            JLabel testLabel = new JLabel("Fred", checkIcon, JLabel.LEFT);
            Insets insets = testLabel.getInsets();
            clickBoxCheckRectangle = new Rectangle(insets.left, testLabel.getInsets().top, checkIcon.getIconWidth(), checkIcon.getIconWidth());
        }
        return clickBoxCheckRectangle;
    }

    public void dispose() {
        ((FilteredTreeModel) getModel()).dispose();
        setModel(null);
    }

    public void fireStructureChanged() {
        ((FilteredTreeModel) getModel()).fireStructureChanged();
    }

    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) {
            TreePath selectionPath = getSelectionPath();
            if (selectionPath == null) return;

            BeanPropertyTreeNode property = (BeanPropertyTreeNode) selectionPath.getLastPathComponent();
            if (!property.isPropertyGroup() && PropertyModel.getInstance(property.getAttribute().getType()).isSelectable()) {
                selectionModel.locatorSelected(property.getBeanPath());
                return;
            }

            if (isExpanded(selectionPath)) {
                collapsePath(selectionPath);
            } else {
                expandPath(selectionPath);
            }
        } else {
            super.processKeyEvent(e);
        }
    }

    /**
     * This method adds or removes the selected nodes in the tree.
     * @param add true means selected nodes will be added.
     */
    public void setSelected(boolean add) {
        int selectedRows[] = getSelectionRows();
        if (selectedRows == null) return;

        for (int i = 0; i < selectedRows.length; i++) {
            BeanPropertyTreeNode property = (BeanPropertyTreeNode) getPathForRow(selectedRows[i]).getLastPathComponent();

            if (!property.isPropertyGroup() && PropertyModel.getInstance(property.getAttribute().getType()).isSelectable()) {
                if (add ^ getBeanPathSelectionModel().isSelected(property.getBeanPath())) {
                    getBeanPathSelectionModel().locatorSelected(property.getBeanPath());
                }
            }
        }
    }

    @Override
    public void clearSelection() {
        super.clearSelection();
        if(getBeanPathSelectionModel() != null) {
            getBeanPathSelectionModel().clearSelected();
        }
    }

    public SelectionModel getBeanPathSelectionModel() {
        return selectionModel;
    }

    public void reset() {
        ((FilteredTreeModel) getModel()).reset();
    }

    public String getToolTipText(MouseEvent event) {
        return propertyToolTipFactory.getToolTipText(event);
    }

    public JToolTip createToolTip() {
        return propertyToolTipFactory.createToolTip();
    }

    public static abstract class SelectionModel {
        private boolean multipleSelectionEnabled;

        public SelectionModel(boolean multipleSelectionEnabled) {
            this.multipleSelectionEnabled = multipleSelectionEnabled;
        }

        public boolean isMultipleSelectionEnabled() {
            return multipleSelectionEnabled;
        }

        public abstract void locatorSelected(String columnLocator);

        public abstract boolean isSelected(String columnLocator);

        public void selectionComplete() {
            //Override to listen for double click
        }

        public void clearSelected() {
            //Override to add specific functionality for clearing selection
        }
    }

    /**
     * This renderer is respsonsible for displaying the selection.
     */
    public static class PropertyTreeCellRenderer extends DefaultTreeCellRenderer {
        private Icon checkOn = new CheckIcon(true);
        private Icon checkOff = new CheckIcon(false);

        private Font normalFont;
        private Font singleSelectedFont;
        private SelectionModel beanPathSelectionModel;
        private boolean highlightPropertyGroup = true;

        public PropertyTreeCellRenderer(SelectionModel beanPathSelectionModel) {
            this.beanPathSelectionModel = beanPathSelectionModel;
        }

        public void setHighlightPropertyGroup(boolean highlight) {
            highlightPropertyGroup = highlight;
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (!(value instanceof BeanPropertyTreeNode)) return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            BeanPropertyTreeNode property = (BeanPropertyTreeNode) value;
            String text = getStringForValue(value);
            JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
            if (normalFont == null) {
                normalFont = c.getFont();
                singleSelectedFont = normalFont.deriveFont(Font.BOLD);
            }

            if (highlightPropertyGroup && property.isPropertyGroup()) {
                c.setFont(c.getFont().deriveFont(Font.ITALIC));
                c.setForeground(Color.blue);
            } else {
                if (!beanPathSelectionModel.isMultipleSelectionEnabled()) {
                    if (!property.isRoot()) {
                        c.setIcon(null);
                        c.setFont(beanPathSelectionModel.isSelected(property.getBeanPath()) ? singleSelectedFont : normalFont);
                    } else {
                        c.setFont(normalFont);
                    }
                } else {
                    c.setFont(normalFont);
                    if (!property.isRoot()) {
                        if (!PropertyModel.getInstance(property.getAttribute().getType()).isSelectable()) {
//                            c.setIcon(blankIcon);
                            c.setIcon(null);
                        } else {
                            c.setIcon(beanPathSelectionModel.isSelected(property.getBeanPath()) ? checkOn : checkOff);
                        }
                    }
                }
            }
            return c;
        }

        protected String getStringForValue(Object value) {
            BeanPropertyTreeNode property = (BeanPropertyTreeNode) value;
            return property.getDisplayName();
        }

    }


    /**
     * an icon that draws a check box.
     * the drawing code has been ripped from MetalIconFactory.CheckBoxIcon
     */
    protected static class CheckIcon implements Icon {
        boolean selected;
        int controlSize = 13;

        public CheckIcon(boolean selected) {
            this.selected = selected;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            drawFlush3DBorder(g, x, y, controlSize, controlSize);

            if (selected) {
                g.setColor(MetalLookAndFeel.getControlInfo());
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

    public static class BeanPropertyTreeMouseHandler extends MouseAdapter {
        private SelectionModel beanPathSelectionModel;

        public BeanPropertyTreeMouseHandler(SelectionModel beanPathSelectionModel) {
            this.beanPathSelectionModel = beanPathSelectionModel;
        }

        public void mousePressed(MouseEvent e) {
            JTree tree = (JTree) e.getSource();
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                if(e.getClickCount() == 2) {
                    beanPathSelectionModel.selectionComplete();
                } else {
                    boolean valid;
                    BeanPropertyTreeNode property = ((BeanPropertyTreeNode) path.getLastPathComponent());

                    if (!property.isPropertyGroup() && PropertyModel.getInstance(property.getAttribute().getType()).isSelectable()) {
                        if (beanPathSelectionModel.isMultipleSelectionEnabled()) {
                            Rectangle rowRectangle = tree.getRowBounds(tree.getRowForLocation(e.getX(), e.getY()));
                            Rectangle checkBoxRectangle = new Rectangle(rowRectangle.x + getClickBoxCheckRectangle().x, rowRectangle.y + getClickBoxCheckRectangle().y,
                                                                        getClickBoxCheckRectangle().width, getClickBoxCheckRectangle().height);
                            valid = checkBoxRectangle.contains(e.getPoint());
                        } else {
                            valid = true;
                        }

                        String columnIdentifier = property.getBeanPath();
                        if (columnIdentifier != null && columnIdentifier.length() > 0 && valid) {
                            beanPathSelectionModel.locatorSelected(columnIdentifier);
                        }
                    }
                }
            }
        }
    }

    private class BeanpathSelectorPropertyToolTipFactory extends PropertyToolTipFactory {
        public String getToolTipText(MouseEvent event) {
            TreePath path = getPathForLocation(event.getX(), event.getY());
            if (path != null && path.getPathCount() > 1) {
                BeanPropertyTreeNode property = ((BeanPropertyTreeNode) path.getLastPathComponent());
                BeanPropertyTreeNode rootProperty = property.getRootProperty();
                return getToolTipText(rootProperty.getAttribute().getType(), property.getBeanPath());
            }
            return null;
        }
    }
}


