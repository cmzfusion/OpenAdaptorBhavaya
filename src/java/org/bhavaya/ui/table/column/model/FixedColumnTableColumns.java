package org.bhavaya.ui.table.column.model;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 17-Sep-2009
 * Time: 16:56:07
 *
 * This class provides methods to manage the columns for FixedColumnTable
 *
 * There are actually two separate column models, one for the fixed column table and the other for the scrollable, but
 * in general this class presents a unified view to client classes, in which combinedColumnIndex is the overall index
 * as it appears in the fixed + scrollable table
 *
 * Operations which affect only column model state have been moved into this class from FixedColumnTable
 */
public class FixedColumnTableColumns {

    private ColumnHidingColumnModel fixedColumnModel = new ColumnHidingColumnModel();
    private ColumnHidingColumnModel scrollableColumnModel = new ColumnHidingColumnModel();
    private SortedSet<ColumnGroup> columnGroups = new TreeSet<ColumnGroup>();
    private List<ColumnGroupListener> groupListeners = new ArrayList<ColumnGroupListener>();
    private TableModel tableModel;

    public FixedColumnTableColumns() {
    }

    public FixedColumnTableColumns(ColumnHidingColumnModel fixedColumnModel, ColumnHidingColumnModel scrollableColumnModel) {
        this.fixedColumnModel = fixedColumnModel;
        this.scrollableColumnModel = scrollableColumnModel;
    }

    public FixedColumnTableColumns(TableModel tableModel) {
        this.tableModel = tableModel;
    }

    public ObservableWidthColumnModel getFixedColumnModel() {
        return fixedColumnModel;
    }

    public ObservableWidthColumnModel getScrollableColumnModel() {
        return scrollableColumnModel;
    }
    
    public boolean isTableModelColumnInFixedTable(int tableModelIndex) {
        return fixedColumnModel.containsColumnWithModelIndex(tableModelIndex);
    }

    public boolean isColIndexInFixedTable(int combinedColumnIndex) {
        return combinedColumnIndex < fixedColumnModel.getColumnCount();
    }

    public int getColIndexInColumnModel(int combinedColumnIndex) {
        return isColIndexInFixedTable(combinedColumnIndex) ?
                combinedColumnIndex :
                combinedColumnIndex - fixedColumnModel.getColumnCount();
    }

    public int getFixedColumnCount() {
        return fixedColumnModel.getColumnCount();
    }

    public int getColumnCount() {
        return getFixedColumnCount() + getScrollableColumnCount();
    }

    public HidableTableColumn getColumn(int combinedColumnIndex) {
        HidableTableColumn result = null;
        if ( combinedColumnIndex >= 0 && combinedColumnIndex <= getColumnCount()) {
            TableColumnModel model = getColumnModel(combinedColumnIndex);
            int colIndexInTable = getColIndexInColumnModel(combinedColumnIndex);
            result = (HidableTableColumn)model.getColumn(colIndexInTable);
        }
        return result;
    }

    public TableColumn getColumn(Object id) {
        for (Iterator iterator = getColumns(); iterator.hasNext();) {
            TableColumn tableColumn = (TableColumn) iterator.next();

            //I inlined Utilites.equals() to work on this class in isolation without dragging in the rest of bhavaya
            Object o1 = tableColumn.getIdentifier();
            if ((o1 == null) ? (id == null) : o1.equals(id)) return tableColumn;
        }
        throw new IllegalArgumentException("Column not found");
    }

    public void setColumnFixed(int modelIndex, boolean fixed) {
        distributeColumn(modelIndex, fixed);
        if (!fixed) {
            scrollableColumnModel.moveColumn(scrollableColumnModel.getColumnCount() - 1, 0);
        }
    }

    public void distributeColumn(int modelIndex, boolean fixed) {
        ObservableWidthColumnModel sourceTable = fixed ? scrollableColumnModel : fixedColumnModel;
        ObservableWidthColumnModel targetTable = fixed ? fixedColumnModel : scrollableColumnModel;

        if (sourceTable.containsColumnWithModelIndex(modelIndex)) {
            TableColumn tableColumn = sourceTable.getColumnWithModelIndex(modelIndex);
            sourceTable.removeColumn(tableColumn);
            targetTable.addColumn(tableColumn);
        } else {
            targetTable.addColumn(createTableColumn(modelIndex));
        }
    }

    public TableColumn createTableColumn(int i) {
        HidableTableColumn tableColumn = new HidableTableColumn(i);
        tableColumn.setHeaderValue(tableModel.getColumnName(i));
        return tableColumn;
    }

    /**
     * @return An iterator for all visible columns (hidden columns not included)
     */
    public Iterator getColumns() {
        return new Iterator() {
            private int fixedIndex = 0;
            private int scrollableIndex = 0;

            public boolean hasNext() {
                return fixedIndex < getFixedColumnCount() || scrollableIndex < getScrollableColumnCount();
            }

            public Object next() {
                if (fixedIndex < getFixedColumnCount()) {
                    return fixedColumnModel.getColumn(fixedIndex++);
                } else if (scrollableIndex < getScrollableColumnCount()) {
                    return scrollableColumnModel.getColumn(scrollableIndex++);
                } else {
                    throw new IllegalStateException("No more elements");
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove not supported in this implementation");
            }
        };
    }

    /**
     * @return all columns, whether hidden or not
     */
    public List<HidableTableColumn> getAllColumns() {
        List<HidableTableColumn> columns = new ArrayList<HidableTableColumn>();
        columns.addAll(fixedColumnModel.getAllColumns());
        columns.addAll(scrollableColumnModel.getAllColumns());
        return columns;
    }

    public ColumnGroup getColumnGroup(String groupName) {
        ColumnGroup result = null;
        for ( ColumnGroup g : columnGroups) {
            if ( groupName.equals(g.getGroupName())) {
                result = g;
            }
        }
        return result;
    }

    public boolean containsColumnGroupWithName(String groupName) {
        return columnGroups.contains(new ColumnGroup(groupName));
    }

    private ColumnHidingColumnModel getColumnModel(int combinedColumnIndex) {
        return isColIndexInFixedTable(combinedColumnIndex) ? fixedColumnModel : scrollableColumnModel;
    }

    private int getScrollableColumnCount() {
        return scrollableColumnModel.getColumnCount();
    }

    public int getCombinedColumnIndex(TableColumn c) {
        int result = fixedColumnModel.indexOf(c);
        if ( result == -1 ) {
            result = scrollableColumnModel.indexOf(c);
            if ( result != -1) {
                result += fixedColumnModel.getColumnCount();
            }
        }
        return result;
    }

    /**
     * Move a column between oldIndex and newIndex in the combined view.
     * You cannot move a column between fixed and scrollable models (the method will return false).
     * Instead this must be done by first 'fixing' or 'unfixing' the column.
     * 
     * @return true, if the move was possible and successful
     */
    public boolean moveColumn(int oldIndex, int newIndex) {
        boolean result = false;
        if ( isColIndexInFixedTable(oldIndex) == isColIndexInFixedTable(newIndex)) {
            ColumnHidingColumnModel c = getColumnModel(oldIndex);
            int indexFrom = getColIndexInColumnModel(oldIndex);
            int indexTo = getColIndexInColumnModel(newIndex);
            if ( indexTo >= 0 && indexTo < c.getColumnCount()) {
                c.moveColumn(indexFrom, indexTo);
                result = true;
            }
        }
        return result;
    }

    private void fireGroupsChanged() {
        SortedSet<ColumnGroup> newGroups = getColumnGroups();
        List<ColumnGroupListener> listenerSnapshot = new ArrayList<ColumnGroupListener>(groupListeners);
        for ( ColumnGroupListener l : listenerSnapshot) {
            l.groupsChanged(newGroups);
        }
    }

    /**
     * Refresh column groups to reflect the groups in the fixed and scrollable column models
     *
     * For each group, ensure that the columns in the ColumnHidingColumnModel are either
     * hidden or visible, according to the group's current visibility.
     */
    public void refreshColumnGroups() {
        SortedSet<ColumnGroup> groups = findGroupsFromColumns();
        refreshGroupVisibility(groups);

        if ( ! groups.equals(this.columnGroups)) {
            this.columnGroups = groups;
            fireGroupsChanged();
        }
    }

    //When we load a view config, the groups are deserialized along with the column list
    //However for each group the group.isHidden() state has not get been applied to the column
    //model, which by defaults will show all columns, so we need to hide the appropriate groups here
    private void refreshGroupVisibility(SortedSet<ColumnGroup> groups) {
        for ( ColumnGroup g : groups ) {
            if ( g.isHidden() ) {
                hideColumnsInGroup(g);
            }
        }
    }

    private SortedSet<ColumnGroup> findGroupsFromColumns() {
        SortedSet<ColumnGroup> groups = new TreeSet<ColumnGroup>();
        for ( HidableTableColumn c : getAllColumns()) {
            if ( c.getColumnGroup() != null) {
                groups.add(c.getColumnGroup());
            }
        }
        return groups;
    }

    public SortedSet<ColumnGroup> getColumnGroups() {
        return new TreeSet<ColumnGroup>(columnGroups);
    }

    public void addColumnGroupListener(ColumnGroupListener l) {
        groupListeners.add(l);
    }

    public void showColumnsInGroup(ColumnGroup columnGroup) {
        fixedColumnModel.showColumnsInGroup(columnGroup);
        scrollableColumnModel.showColumnsInGroup(columnGroup);
    }

    public void hideColumnsInGroup(ColumnGroup columnGroup) {
        fixedColumnModel.hideColumnsInGroup(columnGroup);
        scrollableColumnModel.hideColumnsInGroup(columnGroup);
    }

    public interface ColumnGroupListener {
        void groupsChanged(SortedSet<ColumnGroup> newGroups);
    }
}
