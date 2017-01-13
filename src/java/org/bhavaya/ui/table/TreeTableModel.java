package org.bhavaya.ui.table;

import gnu.trove.TIntArrayList;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * todo: not yet finished. Need to deal with remove updates. inserts work.
 * todo: can't remember if it handles value changes. Should not be difficult to add this stuff ;o)
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class TreeTableModel extends AbstractTableModel implements TabularBeanAssociation, KeyedColumnTableModel {
    protected KeyedColumnTableModel underlyingTableModel;

    private JTree underlyingTree;   //the model behaves like the view of a normal tree, so we use a full tree as the model!

    private List hierarchy = new ArrayList();
    private TIntArrayList hierachyColumnIndicies = new TIntArrayList();

    private FullTreeModel treeModel;

    public TreeTableModel(KeyedColumnTableModel underlyingTableModel) {
        this.underlyingTableModel = underlyingTableModel;
    }

    /**
     * the columns to use for each depth of tree node. Highest first
     *
     * @param columnKeys
     */
    public void setTreeHierarchy(List columnKeys) {
        hierarchy = columnKeys;
        treeModel = new FullTreeModel(underlyingTableModel);
        underlyingTree = new JTree() {
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof GroupedRowManager.GroupedRow) {
                    GroupedRowManager.GroupedRow groupedRow = (GroupedRowManager.GroupedRow) value;
                    GroupedRowManager manager = groupedRow.getManager();
                    if (manager instanceof NonLeafTreeLevel) {
                        int depth = ((NonLeafTreeLevel) manager).getDepth();
                        if (depth > 0) {
                            return groupedRow.getCellValue(depth - 1).toString();
                        }
                    }
                }
                return null;
            }
        };
        underlyingTree.setRootVisible(false);
        TreeViewListener treeViewListener = new TreeViewListener();
        underlyingTree.addTreeWillExpandListener(treeViewListener);
        underlyingTree.addTreeExpansionListener(treeViewListener);
        underlyingTree.setModel(treeModel);
    }

    public JTree getUnderlyingTree() {
        return underlyingTree;
    }

    private void updateColumnIndicies() {
        hierachyColumnIndicies.clear();
        for (Iterator iterator = hierarchy.iterator(); iterator.hasNext();) {
            Object key = (Object) iterator.next();
            int columnIndex = underlyingTableModel.getColumnIndex(key);
            hierachyColumnIndicies.add(columnIndex);
        }
        hierachyColumnIndicies.sort();
    }

    public int getColumnCount() {
        return underlyingTableModel.getColumnCount() + 1;
    }

    public int getRowCount() {
        return underlyingTree.getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        TreePath pathForRow = underlyingTree.getPathForRow(rowIndex);
        Object node = pathForRow.getLastPathComponent();
        Object value;
        if (columnIndex == 0) {
            value = node;
        } else {
            TreeLevelHandler handlerForNode = treeModel.getHandlerForNode(node);
            value = handlerForNode.getColumnValue(node, columnIndex - 1);
        }
        return value;
    }

    public Object[] getBeansForLocation(int rowIndex, int columnIndex) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSingleBean(int rowIndex, int columnIndex) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getColumnIndex(Object columnKey) {
        return underlyingTableModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int index) {
        return underlyingTableModel.getColumnKey(index);
    }

    public TreeModel getTreeModel() {
        return treeModel;
    }

    protected void adjustColumnIndexAndFire(TableModelEvent e) {
        int column = e.getColumn();
        TableModelEvent newE = new TableModelEvent(this, e.getFirstRow(), e.getLastRow(), column + 1, e.getType());
        fireTableChanged(newE);
    }

    private class FullTreeModel implements TreeModel {
        private Object root;
        private ArrayList depthHandlers = new ArrayList();
        private ArrayList listeners = new ArrayList();

        public FullTreeModel(KeyedColumnTableModel underlyingModel) {
            depthHandlers.add(new LeafHandler(this, hierarchy.size(), underlyingModel));

            for (int depth = hierarchy.size(); depth >= 0; depth--) {
                final HashSet keyGroup = new HashSet();
                for (int i = 0; i < depth; i++) {
                    keyGroup.add(hierarchy.get(i));
                }


                TreeLevelGroupedModel groupedTableModel = new TreeLevelGroupedModel(this, depth, underlyingModel);
                groupedTableModel.setGroupedKeyDefinition(new GroupedTableModel.GroupedKeyDefinition() {
                    public boolean isGroupKeyColumn(Object columnKey) {
                        return keyGroup.contains(columnKey);
                    }
                });
                depthHandlers.add(0, groupedTableModel.getHandler());
                underlyingModel = groupedTableModel;
            }
            root = depthHandlers.get(0);
            NonLeafTreeLevel rootHandler = (NonLeafTreeLevel) root;
            rootHandler.createGroupedRow(); //always show a row
        }

        public Object getRoot() {
            return root;
        }

        public TreeLevelHandler getHandlerForNode(Object node) {
            if (node instanceof TreeLevelHandler) {
                return (TreeLevelHandler) node;
            } else if (node instanceof LeafRow) {
                return getTreeLevelHandler(depthHandlers.size() - 1);
            } else if (node instanceof GroupedRowManager.GroupedRow) {
                GroupedRowManager.GroupedRow row = (GroupedRowManager.GroupedRow) node;
                GroupedRowManager manager = row.getManager();
                return (NonLeafTreeLevel) manager;
            } else {
                throw new IllegalArgumentException("Node should be either GruopedRow or LeafRow");
            }
        }

        public int getChildCount(Object parent) {
            return getHandlerForNode(parent).getChildCount(parent);
        }

        public boolean isLeaf(Object node) {
            return node instanceof LeafRow;
        }

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        public Object getChild(Object parent, int index) {
            return getHandlerForNode(parent).getChild(parent, index);
        }

        public int getIndexOfChild(Object parent, Object child) {
            return getHandlerForNode(parent).getIndexOfChild(parent, child);
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getColumnKeyForDepth(int depth) {
            if (depth < hierarchy.size()) return hierarchy.get(depth);
            return null;
        }

        public TreeLevelHandler getHandlerForDepth(int depth) {
            return (TreeLevelHandler) depthHandlers.get(depth);
        }

        public TreePath getPathToRoot(Object node) {
            TreeLevelHandler handlerForNode = getHandlerForNode(node);
            int pathDepth = handlerForNode.getDepth() + 2;
            Object[] pathToRoot = new Object[pathDepth];

            for (int depth = pathDepth - 1; depth >= 0; depth--) {
                pathToRoot[depth] = node;
                if (node != getRoot()) {
                    handlerForNode = handlerForNode.getParentDepthHandler();
                    node = handlerForNode.getNodeContainingChild(node);  //parent node
                }
            }
            return new TreePath(pathToRoot);
        }

        public void fireNodeAdded(TreePath parentPath, int childIndex, Object node) {
            TreeModelEvent insertEvent = new TreeModelEvent(this, parentPath,
                    new int[]{childIndex}, new Object[]{node});

            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                TreeModelListener listener = (TreeModelListener) iterator.next();
                listener.treeNodesInserted(insertEvent);
            }

            TreePath pathToNode = parentPath.pathByAddingChild(node);
            int newRow = getUnderlyingTree().getRowForPath(pathToNode);
            if (newRow >= 0) {
                TreeTableModel.this.fireTableRowsInserted(newRow, newRow);
            }
        }

        public void fireNodeRemoved(TreePath parentPath, int childIndex, Object node) {
            TreeModelEvent removeEvent = new TreeModelEvent(this, parentPath,
                    new int[]{childIndex}, new Object[]{node});

            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                TreeModelListener listener = (TreeModelListener) iterator.next();
                listener.treeNodesRemoved(removeEvent);
            }

            TreePath pathToNode = parentPath.pathByAddingChild(node);
            int newRow = getUnderlyingTree().getRowForPath(pathToNode);
            if (newRow >= 0) {
                TreeTableModel.this.fireTableRowsInserted(newRow, newRow);
            }
        }

        public TreeLevelHandler getTreeLevelHandler(int i) {
            return (TreeLevelHandler) depthHandlers.get(i);
        }
    }

    public class NonLeafTreeLevel extends GroupedRowManager implements TreeLevelHandler {
        private TreeLevelGroupedModel groupedModel;
        private int depth;

        public NonLeafTreeLevel(TreeLevelGroupedModel groupedModel, int depth, KeyedColumnTableModel sourceTableModel, CellChangeListener listener) {
            super(sourceTableModel, listener);
            this.groupedModel = groupedModel;
            this.depth = depth;
        }

        public GroupedTableModel getGroupedModel() {
            return groupedModel;
        }

        public Object getRoot() {
            return this;
        }

        public boolean isLeaf(Object node) {
            return false;
        }

        /**
         * @param childNode should be a child belonging to the depth below this one. If child node belongs to
         *                  this level, then the level root will be returned
         * @return the owner of the child. the owner object will come from this tree depth.
         */
        public Object getNodeContainingChild(Object childNode) {
            int childSourceRowNumber;
            if (childNode instanceof GroupedRow) {
                GroupedRow row = (GroupedRow) childNode;
                NonLeafTreeLevel nonLeafTreeLevel = (NonLeafTreeLevel) row.getManager();
                if (nonLeafTreeLevel == this) return getRoot();

                int childDepth = nonLeafTreeLevel.getDepth();
                assert (childDepth == getDepth() + 1) : "That child was not one of mine!";
                childSourceRowNumber = row.getRowNumber();
            } else if (childNode instanceof LeafRow) {
                LeafRow row = (LeafRow) childNode;
                childSourceRowNumber = row.getRowNumber();
            } else {
                throw new IllegalArgumentException("ChildNode is not an instance of GroupedRow or LeafRow");
            }

            GroupedRow parent = getGroupedRowForSourceRow(childSourceRowNumber);
            return parent;
        }

        public int getChildCount(Object node) {
            getGroupedModel().getRows();
            if (node == getRoot()) return getGroupedRowCount();
            return ((GroupedRow) node).getSourceRowCount();
        }

        public Object getChild(Object node, int childNumber) {
            getGroupedModel().getRows();
            if (node == getRoot()) return getGroupedRow(childNumber);

            int sourceRowIndex = ((GroupedRowManager.GroupedRow) node).getSourceRowIndex(childNumber);
            TreeLevelHandler childHandler = getChildDepthHandler();
            return childHandler.getChild(childHandler.getRoot(), sourceRowIndex);
        }

        private TreeLevelHandler getChildDepthHandler() {
            return (TreeLevelHandler) getFullTreeModel().getTreeLevelHandler(depth + 1);
        }

        private FullTreeModel getFullTreeModel() {
            return groupedModel.getFullTreeModel();
        }

        public TreeLevelHandler getParentDepthHandler() {
            if (depth == 0) return this;
            int parentDepth = depth - 1;
            return getFullTreeModel().getTreeLevelHandler(parentDepth);
        }

        public int getIndexOfChild(Object parent, Object child) {
            getGroupedModel().getRows();
            if (parent == getRoot()) {
                return ((GroupedRow) child).getRowNumber();
            }

            //todo: speed this method up maybe?
            int groupLength = getChildCount(parent);
            for (int i = 0; i < groupLength; i++) {
                if (getChild(parent, i) == child) return i;
            }
            return -1;
        }

        public void addSourceRowToGroup(GroupedRowManager.GroupedRow row, int sourceRow, boolean sendEvents) {
            int childCount = row.getSourceRowCount();
            super.addSourceRowToGroup(row, sourceRow, sendEvents);
            if (sendEvents) {
                TreePath parentPath = getFullTreeModel().getPathToRoot(row);
                Object child = getChild(row, childCount);
                getFullTreeModel().fireNodeAdded(parentPath, childCount, child);
            }
        }

        public void removeSourceRowFromGroup(GroupedRowManager.GroupedRow row, int sourceRow, GroupedRowManager.OldValuesAccessor oldValues, boolean sendEvents) {
            //todo: think about this!!!
            int childCount = row.getSourceRowCount();
            super.removeSourceRowFromGroup(row, sourceRow, oldValues, sendEvents);
//            if (sendEvents) {
//                TreePath parentPath = getFullTreeModel().getPathToRoot(row);
//                Object child = getChild(row, childCount);
//                getFullTreeModel().fireNodeRemoved(parentPath, childCount, child);
//            }
        }

        public int getDepth() {
            return depth;
        }

        public Object getColumnValue(Object node, int column) {
            int rowNumber = ((GroupedRowManager.GroupedRow) node).getRowNumber();
            return getGroupedModel().getValueAt(rowNumber, column);
        }

        public void addTreeModelListener(TreeModelListener l) {
            throw new UnsupportedOperationException("You can't listen to this. go away");
        }

        public void removeTreeModelListener(TreeModelListener l) {
            throw new UnsupportedOperationException("You can't listen to this. go away");
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException("i haven't thought about this");
        }
    }


    private class TreeLevelGroupedModel extends GroupedTableModel {
        private FullTreeModel fullTreeModel;
        private int depth;
        private TableTransform transform;

        public TreeLevelGroupedModel(FullTreeModel fullTreeModel, int depth, KeyedColumnTableModel underlyingModel) {
            super();
            this.fullTreeModel = fullTreeModel;
            this.depth = depth;
            setSourceModel(underlyingModel);
        }

        protected GroupedRowManager createGroupedRowManager() {
            NonLeafTreeLevel newManager = new NonLeafTreeLevel(this, depth, getSourceModel(), new GroupedTableModel.GroupedRowCellChangeFirer(this));
            this.transform = new TreeLevelTableTrandform(newManager);
            this.addTableModelListener(new NonLeafCellChangeListener(transform));
            return newManager;
        }

        public NonLeafTreeLevel getHandler() {
            return (NonLeafTreeLevel) getGroupedRowManager();
        }

        public FullTreeModel getFullTreeModel() {
            return fullTreeModel;
        }

        private class NonLeafCellChangeListener implements TableModelListener {
            private TableTransform transform;

            public NonLeafCellChangeListener(TableTransform transform) {
                this.transform = transform;
            }

            public void tableChanged(TableModelEvent e) {
                if (TableUtilities.isDimensionUnchanged(e)) {
                    TableModelEvent newEvent = TableUtilities.remapCoordinates(transform, e);
                    if (newEvent != null) {
                        TreeTableModel.this.fireTableChanged(newEvent);
                    }
                }
            }
        }

    }


    private interface TreeLevelHandler extends TreeModel {
        /**
         * @param child
         * @return
         */
        public Object getNodeContainingChild(Object child);

        public int getDepth();

        public Object getColumnValue(Object node, int column);

        public TreeLevelHandler getParentDepthHandler();
    }

    private static class LeafRow {
        private int rowNumber;

        public LeafRow(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }
    }

    private class LeafHandler implements TreeLevelHandler, TableModelListener {
        private FullTreeModel treeModel;
        private int depth;
        private TableModel tableModel;
        private ArrayList nodes = new ArrayList();
        private boolean nodesInvalid = true;
        private TableTransform transform;

        public LeafHandler(FullTreeModel treeModel, int depth, TableModel tableModel) {
            this.treeModel = treeModel;
            this.depth = depth;
            this.tableModel = tableModel;
            this.transform = new TreeLevelTableTrandform(this);
            tableModel.addTableModelListener(this);
        }

        public TreeLevelHandler getParentDepthHandler() {
            TreeLevelHandler treeLevelHandler = treeModel.getTreeLevelHandler(depth - 1);
            return (NonLeafTreeLevel) treeLevelHandler;
        }

        public Object getNodeContainingChild(Object child) {
            throw new UnsupportedOperationException("A leaf handler has no children");
        }

        public int getDepth() {
            return depth;
        }

        public Object getRoot() {
            return this;
        }

        public int getChildCount(Object parent) {
            if (parent == getRoot()) return getNodes().size();
            return 0;
        }

        public boolean isLeaf(Object node) {
            return (node == getRoot());
        }

        public void addTreeModelListener(TreeModelListener l) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeTreeModelListener(TreeModelListener l) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getChild(Object parent, int index) {
            if (parent == getRoot()) return getNodes().get(index);
            throw new UnsupportedOperationException("A leaf handler has no children");
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == getRoot()) return getNodes().indexOf(child);
            throw new UnsupportedOperationException("A leaf handler has no children");
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getColumnValue(Object node, int column) {
            int rowNumber = ((LeafRow) node).getRowNumber();
            return tableModel.getValueAt(rowNumber, column);
        }

        public void tableChanged(TableModelEvent e) {
            if (isNodesInvalid()) return;

            if (TableUtilities.isAllDataChanged(e)) {
                setNodesInvalid();
            } else if (TableUtilities.isRowCountChanged(e)) {
                updateRowCache(e);
            } else if (TableUtilities.isDimensionUnchanged(e)) {
                TableModelEvent newEvent = TableUtilities.remapCoordinates(transform, e);
                if (newEvent != null) {
                    TreeTableModel.this.fireTableChanged(newEvent);
                }
            }
        }

        private void updateRowCache(TableModelEvent e) {
            int firstAffected = Math.max(0, e.getFirstRow());
            int rowCount = tableModel.getRowCount();
            int lastAffected = Math.min(rowCount, e.getLastRow());
            if (TableUtilities.isRowInsert(e) || TableUtilities.isAllDataChanged(e)) {
                for (int row = firstAffected; row <= lastAffected; row++) {
                    LeafRow newRow = new LeafRow(row);
                    getNodes().add(row, newRow);
                }
            } else if (TableUtilities.isRowDelete(e)) {
                for (int row = lastAffected; row >= firstAffected; row--) {
                    getNodes().remove(row);
                }
            }

            //correct remaining node indicies
            for (int row = lastAffected + 1; row < rowCount; row++) {
                LeafRow node = (LeafRow) getNodes().get(row);
                node.setRowNumber(row);
            }
        }

        private void setNodesInvalid() {
            nodesInvalid = true;
        }

        private boolean isNodesInvalid() {
            return nodesInvalid;
        }

        private ArrayList getNodes() {
            if (nodesInvalid) {
                nodes.clear();
                int lastRow = tableModel.getRowCount();
                for (int row = 0; row < lastRow; row++) {
                    LeafRow newRow = new LeafRow(row);
                    nodes.add(row, newRow);
                }
                nodesInvalid = false;
            }
            return nodes;
        }
    }

    private class TreeViewListener implements TreeWillExpandListener, TreeExpansionListener {
        private int firstRow = -1;
        private int lastRow = -1;

        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            workOutFirstAndLast(path);

        }

        private void workOutFirstAndLast(TreePath path) {
            firstRow = underlyingTree.getRowForPath(path);

            Object node = path.getLastPathComponent();
            Object parent = path.getPathComponent(path.getPathCount() - 2);
            int childIndex = treeModel.getIndexOfChild(parent, node);
            int siblingIndex = childIndex + 1;
            if (siblingIndex < treeModel.getChildCount(parent)) {
                Object sibling = treeModel.getChild(parent, siblingIndex);
                Object[] siblingPath = path.getPath();
                siblingPath[siblingPath.length - 1] = sibling;
                lastRow = underlyingTree.getRowForPath(new TreePath(siblingPath));
            } else {
                lastRow = underlyingTree.getRowCount();
            }
        }

        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        }

        public void treeCollapsed(TreeExpansionEvent event) {
            fireTableRowsDeleted(firstRow, lastRow);
        }

        public void treeExpanded(TreeExpansionEvent event) {
            workOutFirstAndLast(event.getPath());
            fireTableRowsInserted(firstRow, lastRow);
        }
    }

    private class TreeLevelTableTrandform implements TableTransform {
        private TreeLevelHandler levelHandler;

        public TreeLevelTableTrandform(TreeLevelHandler levelHandler) {
            this.levelHandler = levelHandler;
        }

        public int mapSourceRowToRow(int sourceRow) {
            Object levelRoot = levelHandler.getRoot();
            Object child = levelHandler.getChild(levelRoot, sourceRow);
            TreePath pathToRoot = treeModel.getPathToRoot(child);
            int newRow = getUnderlyingTree().getRowForPath(pathToRoot);
            return newRow;
        }

        public int mapSourceColumnToColumn(int sourceColumn) {
            return sourceColumn + 1;
        }

        public TableModel getModel() {
            return TreeTableModel.this;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}



