package org.bhavaya.ui.table;

import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.Collection;

/**
 * A component which acts like a table where it is possible to fix certain columns to the left while letting the
 * rest scroll.  There are a couple of points to note: Firstly, this is by no means a drop-in replacement for a
 * JTable.  I don't really think any feasible implementations of this concept could accomplish this without creating
 * two JTables; one for the fixed columns and one for the scrollable columns.  It is therefore twice as much hassle
 * to use a JTable in that doing things like setting renderers and such will now require one to do this twice; once
 * for each underlying JTable.  I am toying with the idea of making the Fixed Table attempt to copy the scrolling table
 * (which I think of the primary table) via property change listeners.  Perhaps that would simplify things?
 * <p/>
 * <h3>A note on column selection</h3>
 * Column selection is a very strange beast in the JTable.  I can't really think of a good reason why anyone would want
 * column selection without row selection.  Who needs to select a whole column?  Perhaps its foolish for a programmer
 * to second guess all possible requirements of a table.  Then there's the cell selection.  Because this is implemented
 * via a row and column model it probably behaves differently to how a user expects.  For instance having  a cell
 * selection of only (1,1), (3,3) is impossible.  Instead you will get (1,1), (1,3), (3,1) and (3,3).  I don't really
 * think this is a problem though unless you're planning to take on Excel.  There are further oddities.  Even with
 * row selection enabled, you can still have selected columns.  If you have row-only selection enabled and you start
 * dragging a selection between columns 2 and 5, JTable will return 4 selected columns through the getSelectedColumns
 * method.  It returns column 5 via the getSelectedColumn method though which I think is more useful.  As such I don't
 * plan to support multiple column selection.
 *
 * @author Brendon McLean
 * @version $Revision: 1.19 $
 */
public class FixedColumnTable extends JScrollPane {

    protected ScrollToolBar scrollToolbar;

    private FixedForCustomRowHeightsJTable fixedTable;
    private FixedForCustomRowHeightsJTable scrollableTable;
    private TableModel tableModel;

    private JViewport rowHeaderView;
    private final FixedColumnTableColumns fixedTableColumns;


    public FixedColumnTable(TableModel tableModel) {
        super();
        this.tableModel = tableModel;
        this.fixedTableColumns = new FixedColumnTableColumns(tableModel);

        this.scrollableTable = createScrollableTable(tableModel, fixedTableColumns.getScrollableColumnModel());
        this.scrollableTable.setName("ScrollableTable");
        this.fixedTable = createFixedTable(tableModel, fixedTableColumns.getFixedColumnModel());
        this.fixedTable.setName("FixedTable");

        // We need to tell the scroll pane that the rowHeader needs resizing whenever the the columns in the
        // fixed Table have been resized/need more space.  The scrolling table has a scroll bar for this purpose
        fixedTableColumns.getFixedColumnModel().addColumnWidthListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                adjustFixedTableSize();
            }
        });

        rowHeaderView = new JViewport();
        rowHeaderView.setView(fixedTable);
        rowHeaderView.setPreferredSize(fixedTable.getPreferredSize());
        fixedTable.getColumnModel().addColumnModelListener(new FixedTableSizingColumnModelListener());

        rowHeaderView.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JViewport fixedViewport = getRowHeader();
                JViewport mainViewport = getViewport();

                int fixedHeight = fixedViewport.getViewSize().height;
                int mainHeight = mainViewport.getViewSize().height;

                if (fixedHeight == mainHeight) {
                    int fixedY = fixedViewport.getViewPosition().y;
                    int mainY = mainViewport.getViewPosition().y;

                    if (mainY != fixedY) {
                        Point p = mainViewport.getViewPosition();
                        p.y = fixedY;
                        mainViewport.setViewPosition(p);
                    }
                }
            }
        });


        fixedTable.addHoverListener(new FixedForCustomRowHeightsJTable.HoverListener(){
            public void hoverMoved(int row) {
                scrollableTable.setHoverRow(row);
            }
        });
        scrollableTable.addHoverListener(new FixedForCustomRowHeightsJTable.HoverListener(){
            public void hoverMoved(int row) {
                fixedTable.setHoverRow(row);
            }
        });

        setViewportView(scrollableTable);
        setRowHeader(rowHeaderView);
        setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());

        scrollToolbar = new ScrollToolBar(scrollableTable);

        initBehaviour();

    }

    public FixedColumnTableColumns getTableColumns() {
        return fixedTableColumns;
    }

    public void addColumnGroupListener(FixedColumnTableColumns.ColumnGroupListener l) {
        fixedTableColumns.addColumnGroupListener(l);
    }

    protected void adjustFixedTableSize() {
        Dimension size = rowHeaderView.getSize();
        size.width = fixedTable.getTableHeader().getPreferredSize().width;
        rowHeaderView.setPreferredSize(size);
        this.revalidate();
        this.repaint();
    }

    public void setColumnFixed(int modelIndex, boolean fixed) {
        fixedTableColumns.setColumnFixed(modelIndex, fixed);
    }

    /**
     * @param tableModelIndex, the index in the table model
     */
    public boolean isColumnFixed(int tableModelIndex) {
        return fixedTableColumns.isTableModelColumnInFixedTable(tableModelIndex);
    }

    public JTable getFixedTable() {
        return fixedTable;
    }

    public JTable getScrollableTable() {
        return scrollableTable;
    }

    public TableModel getModel() {
        return tableModel;
    }

    private void initBehaviour() {
        fixedTable.setSelectionModel(new SlavedListSelectionModel(scrollableTable.getSelectionModel()));

        fixedTable.getActionMap().put("selectNextColumn", new MoveColumnAction(fixedTable, scrollableTable, true,
                fixedTable.getActionMap().get("selectNextColumn")));
        scrollableTable.getActionMap().put("selectPreviousColumn", new MoveColumnAction(scrollableTable, fixedTable, false,
                scrollableTable.getActionMap().get("selectPreviousColumn")));

        fixedTable.addFocusListener(new ColumnSelectionFocusAdapter(scrollableTable.getColumnModel().getSelectionModel()));
        scrollableTable.addFocusListener(new ColumnSelectionFocusAdapter(fixedTable.getColumnModel().getSelectionModel()));

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            fixedTableColumns.distributeColumn(i, false);
        }
    }

    public void setSelectedRow(int rowIndex) {
        fixedTable.setRowSelectionInterval(rowIndex, rowIndex);
    }

    public void setSelectedRows(Collection<Integer> rows) {
        fixedTable.getSelectionModel().clearSelection();
        for ( Integer row : rows) {
            fixedTable.getSelectionModel().addSelectionInterval(row, row);
        }
    }

    public void setSelectedColumn(int combinedColIndex) {
        JTable table = getFixedOrScrollableTable(combinedColIndex);
        int colIndexInTable = fixedTableColumns.getColIndexInColumnModel(combinedColIndex);
        table.getColumnModel().getSelectionModel().setSelectionInterval(colIndexInTable, colIndexInTable);
    }

    /**
     * Select the cell at the given row and column, where column index is the combined column model index
     * index as the columns are shown in the fixed/scrollable table view
     */
    public void setSelectedCellAndGrabFocus(int row, int combinedColIndex) {
        JTable table = getFixedOrScrollableTable(combinedColIndex);
        int colIndexInTable = fixedTableColumns.getColIndexInColumnModel(combinedColIndex);
        fixedTable.clearSelection();
        scrollableTable.clearSelection();
        table.changeSelection(row, colIndexInTable, false, false);
        table.grabFocus();
    }

    protected void scrollToFirstSelectedCell() {
        int columnIndex = getSelectedColumnIndex();
        int rowIndex = fixedTable.getSelectedRow();
        if ( columnIndex != -1 && rowIndex != -1) {
            scrollToCell(rowIndex, columnIndex);
        }
    }

    /**
     * Scroll the table to show the cell at the given row and column, where column index is the combined column model index
     * index as the columns are shown in the fixed/scrollable table view
     */
    public void scrollToCell(int rowIndex, int combinedColumnIndex) {
        JTable table = getFixedOrScrollableTable(combinedColumnIndex);
        int colModelIndex = fixedTableColumns.getColIndexInColumnModel(combinedColumnIndex);
        if ( colModelIndex != -1 ) {
            UIUtilities.scrollToCenter(table, rowIndex, colModelIndex);
        }
    }

    /**
     * Scroll to show the column at combinedColumnIndex in the combined table.
     * If combinedColumnIndex is actually in the fixed table, nothing will happen
     */
    public void scrollToColumn(int combinedColumnIndex) {
        if ( ! fixedTableColumns.isColIndexInFixedTable(combinedColumnIndex)) {
            int colIndex = fixedTableColumns.getColIndexInColumnModel(combinedColumnIndex);
            if ( colIndex != -1) {
                int row = fixedTable.getSelectedRow();
                row = ( row == -1 ) ? 0 : row;
                UIUtilities.scrollToCenter(scrollableTable, row, colIndex);
            }
        }
    }

    /**
     * @return the column index in the tableModel which provides the data for the column at the combinedColumnModelIndex
     */
    public int getTableModelColIndexInFixedOrScrollableTable(int combinedColumnIndex) {
        TableColumn c = getColumn(combinedColumnIndex);
        return c != null ? c.getModelIndex() : -1;
    }

    private JTable getFixedOrScrollableTable(int combinedColumnIndex) {
        boolean fixed = fixedTableColumns.isColIndexInFixedTable(combinedColumnIndex);
        return fixed ? fixedTable : scrollableTable;
    }

    protected FixedForCustomRowHeightsJTable createScrollableTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        FixedForCustomRowHeightsJTable table = new TrackViewportJTable(tableModel, tableColumnModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        return table;
    }

    protected FixedForCustomRowHeightsJTable createFixedTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        FixedForCustomRowHeightsJTable table = new TrackViewportJTable(tableModel, tableColumnModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        return table;
    }

    public TableColumn createTableColumn(int i) {
        return fixedTableColumns.createTableColumn(i);
    }

    public ListSelectionModel getSelectionModel() {
        return scrollableTable.getSelectionModel();
    }

    /**
     * This nifty little class gets rid of some strange column selection artifacts when you have tables with column
     * selection enabled.  For instance, you may set your tables (via subclassing) to allowing single column selection.
     * As there are actually two tables it suddenly becomes possible to have two columns selected.  This class used the
     * focus gained event of one table to cancel any selection on the other.  ListSelectionEvents seemed more complicated
     * than focus.
     */
    private class ColumnSelectionFocusAdapter extends FocusAdapter {
        private ListSelectionModel tableColumnSelectionModel;

        public ColumnSelectionFocusAdapter(ListSelectionModel tableColumnSelectionModel) {
            this.tableColumnSelectionModel = tableColumnSelectionModel;
        }

        public void focusGained(FocusEvent e) {
            tableColumnSelectionModel.clearSelection();
        }
    }

    public TableColumn getColumn(Object id) {
        return fixedTableColumns.getColumn(id);
    }

    public TableColumn getColumn(int combinedColumnIndex) {
        return fixedTableColumns.getColumn(combinedColumnIndex);
    }

    /**
     * @return the combined column index of the selected column in the fixed/scrollable view
     */
    public int getSelectedColumnIndex() {
        int fixedIndex = fixedTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int scrollableIndex = scrollableTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (fixedTable.hasFocus() && fixedIndex != -1 && fixedTable.getColumnCount() > 0) {
            return fixedIndex;
        } else if (scrollableIndex != -1 && scrollableTable.getColumnCount() > 0) {
            return fixedTableColumns.getFixedColumnCount() + scrollableIndex ;
        } else if (fixedIndex != -1 && fixedTable.getColumnCount() > 0) {
            return fixedIndex;
        } else {
            return -1;
        }
    }

    public TableColumn getSelectedColumn() {
        int combinedColumnIndex = getSelectedColumnIndex();
        return getColumn(combinedColumnIndex);
    }

    public int getColumnCount() {
        return fixedTableColumns.getColumnCount();
    }

    public Iterator getColumns() {
        return fixedTableColumns.getColumns();
    }

    public void refreshColumnGroups() {
        fixedTableColumns.refreshColumnGroups();
    }

    private class MoveColumnAction extends AbstractAction {
        private JTable sourceTable;
        private JTable nextTable;
        private boolean increment;
        private Action action;

        public MoveColumnAction(JTable sourceTable, JTable nextTable, boolean increment, Action action) {
            super("MoveColumnAction");

            this.sourceTable = sourceTable;
            this.nextTable = nextTable;
            this.increment = increment;
            this.action = action;
        }

        public void actionPerformed(ActionEvent e) {
            int currentFocusedColumn = sourceTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();

            if (increment) {
                if (currentFocusedColumn == (sourceTable.getColumnCount() - 1)) {
                    if (nextTable.getColumnCount() > 0) {
                        nextTable.getColumnModel().getSelectionModel().setAnchorSelectionIndex(0);
                        nextTable.requestFocus();
                    }
                } else {
                    action.actionPerformed(e);
                }
            } else {
                if (currentFocusedColumn == 0) {
                    if (nextTable.getColumnCount() > 0) {
                        nextTable.getColumnModel().getSelectionModel().setAnchorSelectionIndex(nextTable.getColumnCount() - 1);
                        nextTable.requestFocus();
                    }
                } else {
                    action.actionPerformed(e);
                }
            }
        }
    }

    public static void main(String[] args) {
        final Object[][] data = new Object[][]{
                {"1", "11", "A", "", "", "", "", ""},
                {"2", "22", "", "B", "", "", "", ""},
                {"3", "33", "", "", "C", "", "", ""},
                {"4", "44", "", "", "", "D", "", ""},
                {"5", "55", "", "", "", "", "E", ""},
                {"6", "66", "", "", "", "", "", "F"}};
        final String[] column = new String[]{"fixed 1", "fixed 2", "a", "b", "c", "d", "e", "f"};

        AbstractTableModel tableModel = new AbstractTableModel() {
            public int getRowCount() {
                return data.length;
            }

            public int getColumnCount() {
                return column.length;
            }

            public String getColumnName(int c) {
                return column[c];
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return data[rowIndex][columnIndex];
            }
        };

        final FixedColumnTable fixedColumnTable = new FixedColumnTable(tableModel);
        fixedColumnTable.setColumnFixed(0, true);
        fixedColumnTable.setColumnFixed(1, true);

        JFrame frame = new JFrame("Fixed Column Table Test");
        frame.getContentPane().add(fixedColumnTable);
        frame.pack();
        frame.setVisible(true);
    }

    public static class ModelAwareColumnModel extends DefaultTableColumnModel {

        public boolean containsColumnWithModelIndex(int modelIndex) {
            for (Iterator iterator = tableColumns.iterator(); iterator.hasNext();) {
                TableColumn tableColumn = (TableColumn) iterator.next();
                if (tableColumn.getModelIndex() == modelIndex) return true;
            }
            return false;
        }

        public TableColumn getColumnWithModelIndex(int modelIndex) {
            for (Iterator iterator = tableColumns.iterator(); iterator.hasNext();) {
                TableColumn tableColumn = (TableColumn) iterator.next();
                if (tableColumn.getModelIndex() == modelIndex) return tableColumn;
            }
            return null;
        }
    }

    //when fixed columns are resized we end up with the rowHeaderView the wrong size unless we update this
    private class FixedTableSizingColumnModelListener implements TableColumnModelListener {
        public void columnAdded(TableColumnModelEvent e) {}
        public void columnRemoved(TableColumnModelEvent e) {}
        public void columnMoved(TableColumnModelEvent e) {}
        public void columnSelectionChanged(ListSelectionEvent e) {}

        public void columnMarginChanged(ChangeEvent e) {
            rowHeaderView.setPreferredSize(fixedTable.getPreferredSize());
        }
    }
}

/**
 * A ListSelectionModel that proxies to another LSM for all methods with the exception of
 * insert and removeIndexInterval methods.  This class only really makes sense for use with
 * FixedColumnTable
 *
 * @author Brendon McLean
 * @version $Revision: 1.19 $
 */
class SlavedListSelectionModel implements ListSelectionModel {
    private ListSelectionModel master;

    public SlavedListSelectionModel(ListSelectionModel master) {
        this.master = master;
    }

    public void removeListSelectionListener(ListSelectionListener x) {
        master.removeListSelectionListener(x);
    }

    public void setSelectionInterval(int index0, int index1) {
        master.setSelectionInterval(index0, index1);
    }

    public void addSelectionInterval(int index0, int index1) {
        master.addSelectionInterval(index0, index1);
    }

    public void removeSelectionInterval(int index0, int index1) {
        master.removeSelectionInterval(index0, index1);
    }

    public int getMinSelectionIndex() {
        return master.getMinSelectionIndex();
    }

    public int getMaxSelectionIndex() {
        return master.getMaxSelectionIndex();
    }

    public boolean isSelectedIndex(int index) {
        return master.isSelectedIndex(index);
    }

    public int getAnchorSelectionIndex() {
        return master.getAnchorSelectionIndex();
    }

    public void setAnchorSelectionIndex(int index) {
        master.setAnchorSelectionIndex(index);
    }

    public int getLeadSelectionIndex() {
        return master.getLeadSelectionIndex();
    }

    public void setLeadSelectionIndex(int index) {
        master.setLeadSelectionIndex(index);
    }

    public void clearSelection() {
        master.clearSelection();
    }

    public boolean isSelectionEmpty() {
        return master.isSelectionEmpty();
    }

    public void insertIndexInterval(int index, int length, boolean before) {
        // Ignored.  Will be handled by master
    }

    public void removeIndexInterval(int index0, int index1) {
        // Ignored.  Will be handled by master
    }

    public void setValueIsAdjusting(boolean valueIsAdjusting) {
        master.setValueIsAdjusting(valueIsAdjusting);
    }

    public boolean getValueIsAdjusting() {
        return master.getValueIsAdjusting();
    }

    public void setSelectionMode(int selectionMode) {
        master.setSelectionMode(selectionMode);
    }

    public int getSelectionMode() {
        return master.getSelectionMode();
    }

    public void addListSelectionListener(ListSelectionListener x) {
        master.addListSelectionListener(x);
    }
}

