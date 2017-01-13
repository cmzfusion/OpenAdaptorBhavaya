package org.bhavaya.ui.table;

import org.bhavaya.util.Log;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A TableModel which can filter rows based on column values
 * Needs some more work to makes sure event firing is efficient before possibly integrating into AnalyticsTableModel
 */
public class FilteredTableModel extends AbstractTableModel implements KeyedColumnTableModel {

    private static final Log log = Log.getCategory(FilteredTableModel.class);

    private KeyedColumnTableModel sourceModel;
    private TreeSet<RowIndex> visibleIndices = new TreeSet<RowIndex>();
    private Set<TableModelFilter> filters = new HashSet<TableModelFilter>();
    private int[] rowMap = new int[0];  //maps rows in filtered model to row index in source model

    public FilteredTableModel( KeyedColumnTableModel sourceModel ) {
        this.sourceModel = sourceModel;
        prepareFilters();
        refresh();
        addSourceModelListener();
    }

    private void refreshAndFireDataChanged() {
        refresh();
        fireTableDataChanged();
    }

    //reapply filter to all rows in source model, prepareFilters must have been called b
    private void refresh() {
        visibleIndices.clear();

        for ( int row = 0; row < sourceModel.getRowCount(); row ++ ) {
            if ( passesFilters(row)) {
                visibleIndices.add(new RowIndex(row));
            }
        }
        recalcRowMap();
    }

    //each filter must find its column index
    private void prepareFilters() {
        for ( TableModelFilter filter : filters ) {
            filter.prepareFilter(sourceModel);
        }
    }

    private void addSourceModelListener() {
        sourceModel.addTableModelListener( new TableModelListener() {
            public void tableChanged(TableModelEvent e) {

                if ( e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    prepareFilters();
                    refresh();
                    fireTableStructureChanged();
                } else if (e.getType() == TableModelEvent.INSERT) {
                    handleInsert(e);
                } else if ( e.getType() == TableModelEvent.DELETE) {
                    handleDelete(e);
                } else if ( e.getType() == TableModelEvent.UPDATE) {
                    handleUpdate(e);
                }
            }

        });
    }

    private void handleDelete(TableModelEvent e) {
        for ( int deletedrow = e.getFirstRow(); deletedrow <= e.getLastRow() ; deletedrow ++ ) {
            visibleIndices.remove(new RowIndex(deletedrow));
        }

        Set<RowIndex> tailSet = visibleIndices.tailSet(new RowIndex(e.getLastRow()));
        for ( RowIndex row : tailSet ) {
            row.row = row.row - (1 + (e.getLastRow() - e.getFirstRow()));
        }

        recalcRowMap();
        fireTableDataChanged();
    }

    private void handleInsert(TableModelEvent e) {

        Set<RowIndex> tailSet = visibleIndices.tailSet(new RowIndex(e.getFirstRow()));
        int newRows = 1 + (e.getLastRow() - e.getFirstRow());
        for ( RowIndex row : tailSet ) {
            row.row = row.row + newRows;
        }

        for ( int insertedrow = e.getFirstRow(); insertedrow <= e.getLastRow() ; insertedrow ++ ) {
            if ( passesFilters(insertedrow)) {
                visibleIndices.add(new RowIndex(insertedrow));
            }
        }

        recalcRowMap();
        //TODO limit the event to the affected rows
        fireTableDataChanged();

    }

    private void handleUpdate(TableModelEvent e) {
        //updates may cause either deletions or additions depending on whether new row values
        //pass the filters. Even if no additions or deletions occur we still need to pass on update
        //events if any rows which are visible in the filtered view are affected.
        //It will require some work to handle this in an optimal way -
        //I will leave this for another days since the deadline looms, like a giant dark looming thing
        refreshAndFireDataChanged();
    }


    private void recalcRowMap() {
        rowMap = new int[visibleIndices.size()];
        int row = 0;
        for ( RowIndex r : visibleIndices)  {
            rowMap[row++] = r.row;
        }
    }

    private boolean passesFilters(int row) {
        boolean result = true;
        for ( TableModelFilter filter : filters ) {
            if ( ! filter.evaluate(sourceModel, row)) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void setFilters(TableModelFilter... f) {
        doClearFilters();
        doAddFilters(f);
        prepareFilters();
        refreshAndFireDataChanged();
    }

    public void addFilters(TableModelFilter... f) {
        doAddFilters(f);
        prepareFilters();
        refreshAndFireDataChanged();
    }

    public void clearFilters() {
        doClearFilters();
        refreshAndFireDataChanged();
    }

    //add filters, fire no events
    private void doAddFilters(TableModelFilter... f) {
        filters.addAll(Arrays.asList(f));
    }

    //clear filters, fire no events
    private void doClearFilters() {
        filters.clear();
    }

    public int getColumnIndex(Object columnKey) {
        return sourceModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int index) {
        return sourceModel.getColumnKey(index);
    }

    public int getRowCount() {
        return rowMap.length;
    }

    public int getColumnCount() {
        return sourceModel.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return sourceModel.getColumnName(columnIndex);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return sourceModel.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return sourceModel.isCellEditable(rowMap[rowIndex], columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return sourceModel.getValueAt(rowMap[rowIndex], columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        sourceModel.setValueAt(aValue, rowMap[rowIndex], columnIndex);
    }

    protected int getSourceModelRow(int row) {
        return rowMap[row];
    }

    private static class RowIndex implements Comparable<RowIndex> {
        private int row;

        public RowIndex(int row) {
            this.row = row;
        }

        public int hashCode() {
            return row;
        }

        public boolean equals(Object o) {
            boolean result = false;
            if ( o instanceof RowIndex) {
                if ( ((RowIndex)o).row == row ) {
                    result = true;
                }
            }
            return result;
        }

        public int compareTo(RowIndex o) {
            return (this.row<o.row ? -1 : (this.row==o.row ? 0 : 1));
        }
    }

    public static class ColumnValueFilter implements TableModelFilter {

        private String columnName;
        private Object value;
        private boolean acceptMatchingValues;
        private int colIndex = -1;

        public ColumnValueFilter(final String columnName, Object value) {
            this(columnName, value, true);
        }

        public ColumnValueFilter(final String columnName, Object value, boolean acceptMatchingValues) {
            this.columnName = columnName;
            this.value = value;
            this.acceptMatchingValues = acceptMatchingValues;

        }

        public void prepareFilter(KeyedColumnTableModel tableModel) {
            findColIndex(tableModel, columnName);
        }

        private void findColIndex(KeyedColumnTableModel tableModel, String columnName) {
            colIndex = tableModel.getColumnIndex(columnName);
            if ( colIndex == -1 ) {
                log.warn(getClass().getName() + " could not find filter column " + columnName + " filter cannot be applied");
            }
        }

        public boolean evaluate(KeyedColumnTableModel tableModel, int rowIndex) {
            boolean result = true;
            if ( colIndex != -1 ) {
                Object o = tableModel.getValueAt(rowIndex, colIndex);
                result = acceptMatchingValues ? testValue(o) : ! testValue(o);
            }
            return result;
        }

        private boolean testValue(Object o) {
            return ( o == value || ( value != null && value.equals(o)));
        }
    }


    public static interface TableModelFilter {

        void prepareFilter(KeyedColumnTableModel tableModel);

        /**
         * @return true, if the row should be included in the filtered model
         */
        boolean evaluate( KeyedColumnTableModel tableModel, int rowIndex);
    }
}
