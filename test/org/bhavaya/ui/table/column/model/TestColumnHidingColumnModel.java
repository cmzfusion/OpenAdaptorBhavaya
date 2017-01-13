package org.bhavaya.ui.table.column.model;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 19-Sep-2009
 * Time: 12:24:08
 * To change this template use File | Settings | File Templates.
 */
public class TestColumnHidingColumnModel extends TestCase {

    private ColumnHidingTestTableModel columnHidingTestTableModel;
    private ColumnHidingJTable columnHidingJTable;
    private ColumnHidingColumnModel columnHidingColumnModel;

    public void setUp() {
        columnHidingTestTableModel = new ColumnHidingTestTableModel(); 
        columnHidingJTable = new ColumnHidingJTable(columnHidingTestTableModel);
        columnHidingColumnModel = (ColumnHidingColumnModel)columnHidingJTable.getColumnModel();
    }

    public void testHideColumn() {
        checkColumnCounts(4, 4);
        checkVisibleColumns("A", "B", "C", "D");

        HidableTableColumn bColumn = columnHidingColumnModel.getColumnByHeader("B");
        assertFalse(bColumn.isHidden());

        columnHidingColumnModel.hideColumn(bColumn);
        assertTrue(bColumn.isHidden());
        checkColumnCounts(3, 4);
        checkVisibleColumns("A", "C", "D");

        columnHidingColumnModel.showColumn(bColumn);
        assertFalse(bColumn.isHidden());
        checkColumnCounts(4, 4);
        checkVisibleColumns("A", "B", "C", "D");
    }

    public void testHideAndShowAllColumnsKeepsColumnOrder() {
        hideColumnWithHeaders("B", "A", "D", "C");
        checkColumnCounts(0, 4);

        showColumnWithHeaders("C", "A", "B", "D");
        checkVisibleColumns("A", "B", "C", "D");
    }

    public void testMoveVisibleColumnDoesNotAffectHiddenColumns() {
        hideColumnWithHeaders("D");
        checkVisibleColumns("A", "B", "C");
        columnHidingColumnModel.moveColumn(2, 0);
        checkVisibleColumns("C", "A", "B");
        showColumnWithHeaders("D");
        checkVisibleColumns("C", "A", "B", "D");
    }

    public void testMoveVisibleColumnDoesNotAffectHiddenColumns2() {
        hideColumnWithHeaders("A");
        checkVisibleColumns("B", "C", "D");

        //when you hide a column it gets associated with the nearest visible column on its left
        //A is now associated with invisible 'anchor' column which is always leftmost.
        //so even if column D is moved to index zero, the hidden col A is still to its left and takes index zero when shown
        columnHidingColumnModel.moveColumn(2, 0);
        checkVisibleColumns("D", "B", "C");
        showColumnWithHeaders("A");
        checkVisibleColumns("A", "D", "B", "C");
    }

    public void testMoveVisibleColumnDoesNotAffectHiddenColumns3() {
        hideColumnWithHeaders("D");
        checkVisibleColumns("A", "B", "C");

        //once hidden, D is associated with C, the visible column on its left
        //so moving column A to appear after C will move A also to the right of the hidden D
        //we cannot tell from the move gesture alone whether the user would want this or not -
        //but at least this behaviour is consistent
        columnHidingColumnModel.moveColumn(0, 2);
        checkVisibleColumns("B", "C", "A");
        showColumnWithHeaders("D");
        checkVisibleColumns("B", "C", "D", "A");
    }

    public void testMoveVisibleColumnDoesNotAffectHiddenColumns4() {
        hideColumnWithHeaders("C", "D");
        checkVisibleColumns("A", "B");

        //C and D are currently associated with the visible B
        //when we move column B, we unlink these cols from B and relink them to A
        //so when we show them again, the hidden columns have not moved and are still rightmost
        columnHidingColumnModel.moveColumn(1, 0);
        checkVisibleColumns("B", "A");
        showColumnWithHeaders("D", "C");
        checkVisibleColumns("B", "A", "C", "D");
    }

    public void testShowAllColumns() {
        hideColumnWithHeaders("D", "C", "A", "B");
        checkColumnCounts(0, 4);
        columnHidingColumnModel.showAllColumns();
        checkVisibleColumns("A", "B", "C", "D");
        checkColumnCounts(4, 4);
    }

    public void testHideAndShowInGroup() {
        addToColumnGroup("group1", "A", "C");
        addToColumnGroup("group2", "B");
        hideColumnsInGroup(new ColumnGroup("group1"));  //new instance to test uses .equals
        checkVisibleColumns("B", "D");
        hideColumnsInGroup("group2");
        checkVisibleColumns("D");
        showColumnsInGroup("group1");
        checkVisibleColumns("A", "C", "D");
        showColumnsInGroup(new String("group2")); //new instance to test uses .equals
        checkVisibleColumns("A", "B", "C", "D");

        //now check what happens when we reassign a column to a different group
        addToColumnGroup("group1", "B");
        hideColumnsInGroup("group1");
        checkVisibleColumns("D");
    }

    public void testRemoveWithNoHiddenColumns() {
        removeColumnWithHeader("B");
        checkVisibleColumns("A", "C", "D");
    }

    public void testRemoveWithUnaffectedHiddenColumn() {
        addToColumnGroup("group1", "C");
        hideColumnsInGroup("group1");
        removeColumnWithHeader("A");
        checkVisibleColumns("B", "D");       
        showColumnsInGroup("group1");
        checkVisibleColumns("B", "C", "D");
    }

    //here the hidden column C and D are linked to removed column B
    //when we remove B we have to relink the hidden cols to A so we don't remove them too
    public void testRemoveWithLinkedHiddenColumn() {
        addToColumnGroup("group1", "C", "D");
        hideColumnsInGroup("group1");
        removeColumnWithHeader("B");
        checkVisibleColumns("A");
        showColumnsInGroup("group1");
        checkVisibleColumns("A", "C", "D");
    }

    public void testRemoveWithLinkedHiddenColumn2() {
        addToColumnGroup("group1", "B", "C");
        hideColumnsInGroup("group1");
        removeColumnWithHeader("A");
        checkVisibleColumns("D");
        showColumnsInGroup("group1");
        checkVisibleColumns("B", "C", "D");
    }

    public void testRemoveHiddenColumns() {
        addToColumnGroup("group1", "B", "C");
        hideColumnsInGroup("group1");
        checkVisibleColumns("A", "D");
        assertEquals(4, columnHidingColumnModel.getAllColumns().size());
        removeColumnWithHeader("B");
        assertEquals(3, columnHidingColumnModel.getAllColumns().size());
        columnHidingColumnModel.showAllColumns();
        checkVisibleColumns("A", "C", "D");
    }

    public void testRemoveHiddenColumns2() {
        addToColumnGroup("group1", "A", "B");
        hideColumnsInGroup("group1");
        checkVisibleColumns("C", "D");
        removeColumnWithHeader("B");
        showColumnsInGroup("group1");
        checkVisibleColumns("A", "C", "D");
    }

    public void testRemoveHiddenColumns3() {
        addToColumnGroup("group1", "B", "C");
        hideColumnsInGroup("group1");
        checkVisibleColumns("A", "D");
        removeColumnWithHeader("B");
        removeColumnWithHeader("C");
        showColumnsInGroup("group1");
        checkVisibleColumns("A", "D");
    }

    private void removeColumnWithHeader(String colToRemove) {
        columnHidingColumnModel.removeColumn(columnHidingColumnModel.getColumnByHeader(colToRemove));
    }
    
    private void hideColumnsInGroup(String groupName) {
        columnHidingColumnModel.hideColumnsInGroup(groupName);
    }
    
    private void showColumnsInGroup(String groupName) {
        columnHidingColumnModel.showColumnsInGroup(groupName);            
    }
    
    private void hideColumnsInGroup(ColumnGroup group) {
        columnHidingColumnModel.hideColumnsInGroup(group);
    }
    
    private void hideColumnWithHeaders(String... headers) {
        for ( String header : headers ) {
            columnHidingColumnModel.hideColumn(columnHidingColumnModel.getColumnByHeader(header));
        }
    }

    private void showColumnWithHeaders(String... headers) {
        for ( String header : headers ) {
            columnHidingColumnModel.showColumn(columnHidingColumnModel.getColumnByHeader(header));
        }
    }

    private void addToColumnGroup(String groupName, String... headers) {
        ColumnGroup g = new ColumnGroup(groupName);
        for ( String header : headers ) {
            columnHidingColumnModel.getColumnByHeader(header).setColumnGroup(g);
        }
    }

    private void checkVisibleColumns(String... headers) {
        assertEquals(Arrays.asList(headers), columnHidingColumnModel.getVisibleColumnHeaders());
    }

    private void checkColumnCounts(int allColumnCount, int columnCount) {
        assertEquals(allColumnCount, columnHidingColumnModel.getColumnCount());
        assertEquals(columnCount, columnHidingColumnModel.getAllColumns().size());
    }



}
