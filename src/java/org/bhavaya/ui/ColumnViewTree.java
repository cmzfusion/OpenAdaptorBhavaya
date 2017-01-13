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

import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Task;
import org.bhavaya.util.TaskQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * This class borrows from NeXTStep's Column Browser component now ubiquitous in Mac OS X.  For compatabilities sake
 * (and not for the love of the API) this class works of the same interfaces used by JTree (where sensible).  In case
 * anyone's wondering, I was umming and ahhhing about whether this should be scrollable and seperate from JScrollPane
 * or whether it should contain a JScrollPane.  In the end, I decided that anyone who wants to use this without a
 * scroll pane needs to visit the hospital promptly and ask for an EEG.
 * <p/>
 * Basic functioning.
 * <p/>
 * Tree levels are displayed as Lists (could be Trees in future?).  There are three types of Lists: Parent Lists, focal
 * lists and child lists.  At anyone one time there can be only one focal list and one child list.  There are an
 * arbitrary number of parent lists.
 */
public class ColumnViewTree extends JPanel {
    private static final int MIN_NUMBER_OF_COLUMNS = 3;

    private static final int SCROLL_TIME = 300;

    private TreeModel treeModel;
    private TreePath selection = null;
    private ArrayList selectionListeners = new ArrayList();
    private ArrayList columnComponents = new ArrayList();
    private JComponent containerPane;
    private JScrollPane scrollPane;
    private ListSelectionHandler listSelectionHandler;
    private FocusHandler focusHandler;
    private Dimension preferredSize;
    private Dimension prototypeScrollPanePreferredSize;
    private static final String LEFT_ARROW_PRESSED = "Left Arrow Non-Num";
    private static final String RIGHT_ARROW_PRESSED = "Right Arrow Non-Num";
    private final MoveFocusedColumnAction MOVE_FOCUSED_COLUMN_BACKWARDS = new MoveFocusedColumnAction(-1);
    private final MoveFocusedColumnAction MOVE_FOCUSED_COLUMN_FORWARDS = new MoveFocusedColumnAction(+1);

    private ListCellRenderer listCellRenderer = new ColumnViewListCellRenderer();

    private java.util.Timer animationTimer = new java.util.Timer("ColumnViewAnimator");
    private int sourceX;
    private Rectangle targetRect;
    private long accelerationStartTime;
    private Runnable deferredRemovalTask;
    private TimerTask timerTask;
    private int preferredColumnWidth;
    private int preferredVisibleRowCount;
    private boolean suppressListSelectionHandler = false;
    private static final int DIVIDER_WIDTH = 7;
    private DefaultListModel loadingModel;
    private DefaultListCellRenderer loadingCellRenderer;
    private TaskQueue backgroundLoaderTaskQueue;
    private HashSet loadedNodesSet = new HashSet();
    private Task currentTask;


    public ColumnViewTree(TreeModel treeModel) {
        this(treeModel, 150, 20);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        try {
            backgroundLoaderTaskQueue.dispose();
        } catch (Throwable t) {
            System.out.println("t = " + t);
        }
    }

    public ColumnViewTree(TreeModel treeModel, int preferredColumnWidth, int preferredVisibleRowCount) {
        super(new BorderLayout());
        setTreeModel(treeModel);

        this.preferredColumnWidth = preferredColumnWidth;
        this.preferredVisibleRowCount = preferredVisibleRowCount;

        JList list = new JList();
        list.setFixedCellWidth(preferredColumnWidth);
        list.setFixedCellHeight(16);

        list.setVisibleRowCount(preferredVisibleRowCount);
        JScrollPane prototypeListComponent = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        prototypeListComponent.setBorder(null);
        prototypeScrollPanePreferredSize = prototypeListComponent.getPreferredSize();
        Component prototypeListComponentInScrollPane = new JScrollPane(prototypeListComponent, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Dimension prototypeListComponentInScrollPanePreferredSize = new Dimension(prototypeScrollPanePreferredSize.width * 3 + 2 * DIVIDER_WIDTH,
                prototypeListComponentInScrollPane.getPreferredSize().height);

        containerPane = new ScrollablePanel(prototypeListComponentInScrollPanePreferredSize);
        scrollPane = new JScrollPane(containerPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        preferredSize = scrollPane.getPreferredSize();

        add(scrollPane, BorderLayout.CENTER);

        listSelectionHandler = new ListSelectionHandler();
        focusHandler = new FocusHandler();
        loadingModel = new DefaultListModel();
        loadingModel.addElement("Loading...");

        loadingCellRenderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                listCellRendererComponent.setForeground(Color.gray);
                return listCellRendererComponent;
            }
        };
        backgroundLoaderTaskQueue = new TaskQueue("ColumnViewTree Background Thread");
        backgroundLoaderTaskQueue.start();

        setSelection(null);
    }

    public TreeModel getTreeModel() {
        return treeModel;
    }

    public void setTreeModel(TreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public TreePath getSelection() {
        return selection;
    }

    public void setSelection(TreePath selection) {
        TreePath oldSelection = this.selection;
        this.selection = selection;

        if (selection == null) {
            setColumnCount(0, false);
            JList firstColumn = (JList) columnComponents.get(0);
            firstColumn.setModel(new TreeNodeListModel((TreeNode) treeModel.getRoot()));
            firstColumn.setEnabled(true);

            // Clear all
            for (int i = 1; i < columnComponents.size(); i++) {
                setColumnBlank(((JList) columnComponents.get(i)));
            }
        } else {
            Object[] pathComponents = selection.getPath();

            setColumnCount(pathComponents.length, false);
            JList column = null;
            for (int i = 1; i < pathComponents.length; i++) {
                Object pathComponent = pathComponents[i];
                column = (JList) columnComponents.get(i - 1);

                suppressListSelectionHandler = true;
                final int index = ((TreeNodeListModel) column.getModel()).indexOf(pathComponent);
                column.setSelectedIndex(index);
                column.ensureIndexIsVisible(index);
                setNextColumnsModelUsingListsSelectedValue(column, false);
                suppressListSelectionHandler = false;
            }

            if (column != null) column.requestFocus();
        }
        fireSelectionChanged(oldSelection);
    }

    public void addSelectionListener(TreeSelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionListener(TreeSelectionListener listener) {
        selectionListeners.remove(listener);
    }

    private void fireSelectionChanged(TreePath oldSelection) {
        for (Iterator iterator = selectionListeners.iterator(); iterator.hasNext();) {
            TreeSelectionListener treeSelectionListener = (TreeSelectionListener) iterator.next();
            treeSelectionListener.valueChanged(new TreeSelectionEvent(this, getSelection(), false,
                    oldSelection, getSelection()));
        }
    }

    private void setColumnCount(int columnCount, boolean deferColumnRemoval) {
        final int newColumnCount = Math.max(columnCount, MIN_NUMBER_OF_COLUMNS);
        final int sizeDifference = newColumnCount - columnComponents.size();
        final boolean add = sizeDifference > 0;
        Runnable task = new Runnable() {
            public void run() {
                for (int i = 0; i < Math.abs(sizeDifference); i++) {
                    if (add) {
                        addColumn();
                    } else {
                        removeColumn();
                    }
                }
                assert newColumnCount == columnComponents.size();
                revalidate();
                repaint();
            }
        };

        if (!add && deferColumnRemoval) {
            this.deferredRemovalTask = task;
        } else {
            task.run();
        }
    }

    public Dimension getPreferredSize() {
        return preferredSize;
    }

    private void removeColumn() {
        JList column = (JList) columnComponents.remove(columnComponents.size() - 1);
        column.removeListSelectionListener(listSelectionHandler);
        column.removeFocusListener(focusHandler);

        JScrollPane scrollPane = (JScrollPane) column.getParent().getParent();
        containerPane.remove(scrollPane);
        if (containerPane.getComponentCount() >= 1) containerPane.remove(containerPane.getComponentCount() - 1);
    }

    private void addColumn() {
        JList column = new JList();
        column.setFixedCellWidth(preferredColumnWidth);
        column.setFixedCellHeight(16);
        column.setVisibleRowCount(preferredVisibleRowCount);
        column.setEnabled(false);
        column.addListSelectionListener(listSelectionHandler);
        column.addFocusListener(focusHandler);
        column.setCellRenderer(listCellRenderer);

        column.getInputMap(JPanel.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT_ARROW_PRESSED);
        column.getInputMap(JPanel.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), LEFT_ARROW_PRESSED);
        column.getInputMap(JPanel.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT_ARROW_PRESSED);
        column.getInputMap(JPanel.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), RIGHT_ARROW_PRESSED);
        column.getActionMap().put(LEFT_ARROW_PRESSED, MOVE_FOCUSED_COLUMN_BACKWARDS);
        column.getActionMap().put(RIGHT_ARROW_PRESSED, MOVE_FOCUSED_COLUMN_FORWARDS);

        columnComponents.add(column);

        JScrollPane scrollPane = new JScrollPane(column, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        if (containerPane.getComponentCount() > 0) containerPane.add(new DividerComponent(DIVIDER_WIDTH));
        containerPane.add(scrollPane);
    }


    private static class TreeNodeListModel implements ListModel {
        private TreeNode treeNode;

        public TreeNodeListModel(TreeNode treeNode) {
            this.treeNode = treeNode;
        }

        public int getSize() {
            return treeNode.getChildCount();
        }

        public Object getElementAt(int index) {
            return treeNode.getChildAt(index);
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }

        public int indexOf(Object child) {
            return treeNode.getIndex((TreeNode) child);
        }
    }

    private class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            // Ignore transient (scrolling), only react to when selection settles.
            if (!e.getValueIsAdjusting() && !suppressListSelectionHandler) {
                JList activeColumn = (JList) e.getSource();
                setNextColumnsModelUsingListsSelectedValue(activeColumn, true);
                setSelectionFromTreeNode((TreeNode) activeColumn.getSelectedValue());
            }
        }
    }

    private void setSelectionFromTreeNode(TreeNode treeNode) {
        TreePath oldSelection = this.selection;
        this.selection = treeNode != null ? UIUtilities.createTreePathFromTreeNode(treeNode) : null;
        fireSelectionChanged(oldSelection);
    }

    private void setNextColumnsModelUsingListsSelectedValue(JList activeColumn, boolean asynchronous) {
        final TreeNode selectedNode = (TreeNode) activeColumn.getSelectedValue();

        int activeColumnIndex = columnComponents.indexOf(activeColumn);

        if (columnComponents.size() > (activeColumnIndex + 1)) {
            final JList nextColumn = (JList) columnComponents.get(activeColumnIndex + 1);

            if (selectedNode != null) {
                loadColumnFromNode(nextColumn, selectedNode,asynchronous);
            } else {
                setColumnBlank(nextColumn);
            }
        }
    }

    private void setColumnBlank(final JList nextColumn) {
        nextColumn.setModel(new DefaultListModel());
        nextColumn.setEnabled(false);
    }

    private class FocusHandler implements FocusListener {
        public void focusGained(FocusEvent e) {
            JList activeColumn = (JList) e.getSource();
            int activeColumnIndex = columnComponents.indexOf(activeColumn);

            setColumnCount(activeColumnIndex + 1 + 1, true); // +1 for index->size.  +1 to go one bigger

            final TreeNode currentSelection = (TreeNode) activeColumn.getSelectedValue();
            if (currentSelection != null) {
                JList nextColumn = (JList) columnComponents.get(activeColumnIndex + 1);
                if (nextColumn.getModel() instanceof TreeNodeListModel) {
                    loadColumnFromNode(nextColumn, currentSelection, true);
                }
            }
            setSelectionFromTreeNode(currentSelection);

            // clear remaining models
            for (int i = activeColumnIndex + 2; i < columnComponents.size(); i++) {
                setColumnBlank((JList) columnComponents.get(i));
            }

            scrollFocusedColumnToCentre(activeColumnIndex);
        }

        public void focusLost(FocusEvent e) { // Not interested
        }
    }

    private void loadColumnFromNode(final JList nextColumn, final TreeNode currentSelection, final boolean asynchronous) {
        nextColumn.setModel(loadingModel);
        nextColumn.setCellRenderer(loadingCellRenderer);
        nextColumn.setEnabled(false);

        final Task task = new Task("Load List Data") {
            public void run() throws Throwable {
                final TreeNodeListModel model = new TreeNodeListModel(currentSelection);
                model.getSize(); // Trigger lazy loads.
                if (currentTask == this) {
                    UIUtilities.runInDispatchThread(new Runnable() {
                        public void run() {
                            nextColumn.setModel(model);
                            nextColumn.setCellRenderer(listCellRenderer);
                            nextColumn.setEnabled(true);
                        }
                    });
                    loadedNodesSet.add(currentSelection);
                }
            }

            public boolean equals(Object obj) {
                return true;
            }

            public int hashCode() {
                return 1;
            }
        };

        try {
            currentTask = task;
            if (!asynchronous || loadedNodesSet.contains(currentSelection)) {
                task.run();
            } else {
                backgroundLoaderTaskQueue.addTask(task);
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private class MoveFocusedColumnAction extends AbstractAction {
        private int moveDirection;

        public MoveFocusedColumnAction(int moveDirection) {
            super("ArrowKey");
            this.moveDirection = moveDirection;
        }

        public void actionPerformed(ActionEvent e) {
            final Component focusOwner = (Component) e.getSource();
            final int focusedIndex = columnComponents.indexOf(focusOwner);

            // User tried to move left on very first column
            if (focusedIndex < 0) return;

            final JList nextList = (JList) columnComponents.get(focusedIndex + moveDirection);

            if (nextList.getModel().getSize() > 0 && nextList.getModel() != loadingModel) {
                nextList.requestFocus();

                // requestFocus actually just posts an event on the eventqueue so to keep execution order, we do the same.
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (nextList.getSelectedIndex() == -1) {
                            nextList.setSelectedIndex(0);
                            nextList.ensureIndexIsVisible(0);
                            scrollFocusedColumnToCentre(focusedIndex + moveDirection);
                        }
                    }
                });
            }
        }
    }

    private void scrollFocusedColumnToCentre(final int focusedIndex) {
        this.sourceX = scrollPane.getViewport().getViewPosition().x;
        this.targetRect = getRectOfColumn(focusedIndex);
        this.accelerationStartTime = System.currentTimeMillis();

        if (sourceX == targetRect.x) {
            if (deferredRemovalTask != null) {
                deferredRemovalTask.run();
                deferredRemovalTask = null;
            }
            return;
        }

        // a = 2 s/t^2.
        final double acceleration = 2d * ((targetRect.x - sourceX) / 2d) / Math.pow(SCROLL_TIME / 2d, 2d);

        if (timerTask != null) {
            timerTask.cancel();
        }

        timerTask = new TimerTask() {
            public void run() {
                long deltaT = System.currentTimeMillis() - accelerationStartTime;

                if (deltaT >= SCROLL_TIME) {
                    cancel();
                    scroll(targetRect);
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            if (deferredRemovalTask != null) {
                                deferredRemovalTask.run();
                                deferredRemovalTask = null;
                            }
                            ColumnViewTree.this.repaint();
                        }
                    });
                    timerTask = null;
                    return;
                }

                int offset;
                if (deltaT < (SCROLL_TIME / 2)) {
                    offset = (int) (0.5 * acceleration * Math.pow(deltaT, 2));
                } else {
                    offset = (targetRect.x - sourceX) -
                            ((int) (0.5 * acceleration * Math.pow(SCROLL_TIME - deltaT, 2)));
                }
                scroll(new Rectangle(sourceX + offset, targetRect.y, targetRect.width, targetRect.height));
            }
        };
        animationTimer.scheduleAtFixedRate(timerTask, 0, 30);
    }

    private void scroll(final Rectangle rectangle) {
        final JPanel component = ((JPanel) scrollPane.getViewport().getView());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                component.scrollRectToVisible(rectangle);
            }
        });
    }

    private Rectangle getRectOfColumn(int activeColumnIndex) {
        activeColumnIndex = Math.max(activeColumnIndex, 1);
        return new Rectangle((DIVIDER_WIDTH + prototypeScrollPanePreferredSize.width) * (activeColumnIndex - 1), 0,
                prototypeScrollPanePreferredSize.width * 3 + DIVIDER_WIDTH * 2, prototypeScrollPanePreferredSize.height);
    }

    private static class ColumnViewListCellRenderer implements ListCellRenderer {
        private DefaultListCellRenderer leafNodeCellRender = new DefaultListCellRenderer();
        private DefaultListCellRenderer parentNodeCellRender = new DefaultListCellRenderer();
        private JPanel parentCellGroupingComponent;
        private JLabel imageComponent;

        public ColumnViewListCellRenderer() {
            imageComponent = new JLabel(ImageIconCache.getImageIcon("column_view_next.png"));
            imageComponent.setHorizontalAlignment(JLabel.CENTER);
            Dimension preferredSize = imageComponent.getPreferredSize();
            imageComponent.setBorder(new EmptyBorder(0, 3, 0, 3));
            imageComponent.setMaximumSize(new Dimension(preferredSize.width, Integer.MAX_VALUE));
            imageComponent.setMinimumSize(new Dimension(preferredSize.width, 0));
            imageComponent.setOpaque(true);

            parentCellGroupingComponent = new JPanel(new BorderLayout());
            parentCellGroupingComponent.add(parentNodeCellRender, BorderLayout.CENTER);
            parentCellGroupingComponent.add(imageComponent, BorderLayout.EAST);
            parentCellGroupingComponent.setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) value;

                if (!treeNode.isLeaf()) {
                    parentNodeCellRender.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    Color background = parentNodeCellRender.getBackground();
                    parentCellGroupingComponent.setBackground(background);
                    imageComponent.setBackground(background);

                    parentCellGroupingComponent.setBorder(parentNodeCellRender.getBorder());
                    parentCellGroupingComponent.setToolTipText(value.toString());
                    parentNodeCellRender.setBorder(null);
                    return parentCellGroupingComponent;
                } else {
                    leafNodeCellRender.setIcon(ImageIconCache.getImageIcon("column_view_next.png"));
                    leafNodeCellRender.setToolTipText(value.toString());
                    return leafNodeCellRender.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            } else {
                return parentNodeCellRender;
            }
        }
    }

    private static class CustomFlowLayout implements LayoutManager {
        public void removeLayoutComponent(Component comp) {
        }

        public void layoutContainer(Container parent) {
            Component[] components = parent.getComponents();
            Insets insets = parent.getInsets();
            Dimension parentSize = parent.getSize();

            int x = insets.left;
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                Dimension preferredSize = component.getPreferredSize();
                component.setBounds(x, insets.top, component.getPreferredSize().width, parentSize.height - insets.top - insets.bottom);
                x += 0 + preferredSize.width;
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension minimumSize = new Dimension();
            Insets insets = parent.getInsets();

            Component[] components = parent.getComponents();
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                minimumSize.width += component.getMinimumSize().width;
                minimumSize.height += component.getMinimumSize().height;
                if (i != components.length - 1) minimumSize.width += 0;
            }

            minimumSize.width += insets.left + insets.right;
            minimumSize.height += insets.top + insets.bottom;
            return minimumSize;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension preferredSize = new Dimension();
            Insets insets = parent.getInsets();

            Component[] components = parent.getComponents();
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                preferredSize.width += component.getPreferredSize().width;
                preferredSize.height += component.getPreferredSize().height;

                if (i != components.length - 1) preferredSize.width += 0;
            }

            preferredSize.width += insets.left + insets.right;
            preferredSize.height += insets.top + insets.bottom;
            return preferredSize;
        }
    }

    private static class DividerComponent extends JPanel {
        private Color darkerColor;
        private Color lighterColor;


        public DividerComponent(int dividerWidth) {
            this.darkerColor = UIUtilities.createDarkerColor(getBackground(), 0.15f);
            this.lighterColor = UIUtilities.createBrighterColor(getBackground(), 0.45f);
            setPreferredSize(new Dimension(dividerWidth, getPreferredSize().height));
            setMinimumSize(new Dimension(dividerWidth, getPreferredSize().height));
        }

        public void paint(Graphics g) {
            Dimension size = getSize();

            g.setColor(lighterColor);
            g.drawLine(0, 0, 0, size.height);
            g.setColor(darkerColor);
            g.drawLine(size.width - 1, 0, size.width - 1, size.height - 1);
        }
    }

    private class ScrollablePanel extends JPanel implements Scrollable {
        private Dimension preferredViewportSize;

        public ScrollablePanel(Dimension preferredViewportSize) {
            super(new CustomFlowLayout());
            this.preferredViewportSize = preferredViewportSize;
        }

        public boolean getScrollableTracksViewportHeight() {
            return true;
        }

        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredViewportSize;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return preferredViewportSize.width;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return prototypeScrollPanePreferredSize.width + DIVIDER_WIDTH;
        }
    }

    public static void main(String[] args) {
        ColumnViewTree columnViewTree = new ColumnViewTree(new FileSystemTreeModel("C:\\"));
        JFrame frame = new JFrame();
        frame.getContentPane().add(columnViewTree);
        frame.pack();
        frame.show();
    }
}


