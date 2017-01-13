package org.bhavaya.ui.table;

import junit.framework.TestCase;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;
import java.util.Arrays;

import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.ui.table.FilteredTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 29-Feb-2008
 * Time: 15:59:59
 *
 * does not yet test table model event generation but that needs work anyway to make it more efficient
 */
public class TestFilteredTableModel extends TestCase {

    private FilteredTableModel filteredTableModel;
    private KeyedDefaultTableModel defaultTableModel;
    private Vector tableRows;

    public void setUp() {

        Vector<String> colNames = new Vector<String>();
        colNames.add("Integer");
        colNames.add("Value");

        tableRows = new Vector();
        defaultTableModel = new KeyedDefaultTableModel(tableRows, colNames);
        defaultTableModel.addRow(new Object[] {0, "key1"});
        defaultTableModel.addRow(new Object[] {1, "key2"});
        defaultTableModel.addRow(new Object[] {2, "key2"});
        defaultTableModel.addRow(new Object[] {3, "key3"});
        defaultTableModel.addRow(new Object[] {4, "key3"});
        defaultTableModel.addRow(new Object[] {5, "key3"});
        defaultTableModel.addRow(new Object[] {6, "key4"});
        defaultTableModel.addRow(new Object[] {7, "key4"});
        defaultTableModel.addRow(new Object[] {8, "key4"});
        defaultTableModel.addRow(new Object[] {9, "key4"});

        filteredTableModel = new FilteredTableModel(defaultTableModel);
    }

    public void testInitialPopulation() {
        assertEquals(filteredTableModel.getRowCount(), 10);
    }

    public void testAddAndSetFilters() {
        filteredTableModel.addFilters(new FilteredTableModel.ColumnValueFilter("Value", "key4"));
        testRowsInFilteredModel(6,7,8,9);

        //set filters clears any existing ones
        filteredTableModel.setFilters(new FilteredTableModel.ColumnValueFilter("Value", "key1"));
        testRowsInFilteredModel(0);

        //no row can pass both filters
        filteredTableModel.addFilters(new FilteredTableModel.ColumnValueFilter("Value", "key2"));
        testRowsInFilteredModel();

        //set filters clears any existing ones
        filteredTableModel.setFilters(new FilteredTableModel.ColumnValueFilter("Value", "key1"));
        testRowsInFilteredModel(0);
    }

    public void testInsertRows() {
        filteredTableModel.addFilters(new FilteredTableModel.ColumnValueFilter("Value", "key4"));
        assertEquals(4, filteredTableModel.getRowCount());

        defaultTableModel.insertRow(0, new Object[] { 1000, "key4" });
        assertEquals(5, filteredTableModel.getRowCount());
        testRowsInFilteredModel(1000, 6, 7, 8, 9);

        defaultTableModel.insertRow(8, new Object[] { 1001, "key4" });
        testRowsInFilteredModel(1000, 6, 1001, 7, 8, 9);

        insertRowsToModel(8, new Object[] { 1002, "key4"}, new Object[] { 1003, "key4"});
        testRowsInFilteredModel(1000, 6, 1002, 1003, 1001, 7, 8, 9);

        defaultTableModel.insertRow(14, new Object[] { 1004, "key4" });
        testRowsInFilteredModel(1000, 6, 1002, 1003, 1001, 7, 8, 9, 1004);

        //inserting a row which doesn't pass the filter should have no effect
        defaultTableModel.insertRow(14, new Object[] { 1000, "key5" });
        testRowsInFilteredModel(1000, 6, 1002, 1003, 1001, 7, 8, 9, 1004);
    }


    public void testDeleteRows() {
        filteredTableModel.addFilters(new FilteredTableModel.ColumnValueFilter("Value", "key4"));
        assertEquals(4, filteredTableModel.getRowCount());

        //row at start
        defaultTableModel.removeRow(6);
        testRowsInFilteredModel(7,8,9);

        deleteRowsFromUnderlyingModel(6, 7);
        testRowsInFilteredModel(9);

        //row 2 doesnt pass the filter so deleting it should have no effect
        defaultTableModel.removeRow(2);
        testRowsInFilteredModel(9);

        //row at end
        defaultTableModel.removeRow(5);
        testRowsInFilteredModel();

    }

    public void testFilterNegation() {
        //should return everything except rows with key4
        filteredTableModel.addFilters(new FilteredTableModel.ColumnValueFilter("Value", "key4", false));
        assertEquals(6, filteredTableModel.getRowCount());

        defaultTableModel.addRow(new Object[] {10, Double.NaN});
        defaultTableModel.addRow(new Object[] {11, Float.NaN});
        assertEquals(8, filteredTableModel.getRowCount());

        //set filters clears the current filters, then add filters to filter out the NaNa
        filteredTableModel.setFilters(
                new FilteredTableModel.ColumnValueFilter("Value", Float.NaN, false),
                new FilteredTableModel.ColumnValueFilter("Value", Double.NaN, false));
        assertEquals(10, filteredTableModel.getRowCount());

    }

    private void insertRowsToModel(int index, Object[]... rows) {
        int currentIndex = index;
        for ( Object[] row : rows ) {
            Vector v = new Vector(Arrays.asList(row));
            tableRows.insertElementAt(v, currentIndex++);
        }
        defaultTableModel.fireTableRowsInserted(index, currentIndex - 1);
    }

    private void deleteRowsFromUnderlyingModel(int indexStart, int indexEnd) {
        for ( int index = indexEnd; index >= indexStart; index -- ) {
            tableRows.removeElementAt(index);
        }
        defaultTableModel.fireTableRowsDeleted(indexStart, indexEnd);
    }

    private void testRowsInFilteredModel(int... rowIndices) {
        assertEquals( rowIndices.length, filteredTableModel.getRowCount() );

        for ( int loop=0; loop < filteredTableModel.getRowCount(); loop ++ ) {
            assertEquals(rowIndices[loop], filteredTableModel.getValueAt(loop, 0));
        }
    }

    public static class KeyedDefaultTableModel extends DefaultTableModel implements KeyedColumnTableModel {
        private Vector colNames;

        public KeyedDefaultTableModel(Vector data, Vector colNames) {
            super(data, colNames);
            this.colNames = colNames;
        }

        public int getColumnIndex(Object columnKey) {
            return colNames.indexOf(columnKey);
        }

        public Object getColumnKey(int index) {
           return colNames.get(index);
        }
    }

}
