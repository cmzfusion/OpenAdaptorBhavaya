package org.bhavaya.ui.table.column.model;

import javax.swing.table.TableColumn;
import java.util.List;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 18-Sep-2009
 * Time: 15:00:42
 *
 * A column which can be assoicated with a group, and can be hidden in a ColumnHidingColumnModel
 */
public class HidableTableColumn extends TableColumn {

    private static final HidableTableColumn NULL_COLUMN = new HidableTableColumn();
    private HidableTableColumn previousColumn = NULL_COLUMN;
    private HidableTableColumn nextColumn = NULL_COLUMN;
    private ColumnGroup columnGroup;

    public HidableTableColumn() {
    }

    public HidableTableColumn(int modelIndex) {
        super(modelIndex);
    }

    public HidableTableColumn(int modelIndex, int columnWidth) {
        super(modelIndex, columnWidth);
    }

    //a static factory method to avoid the potential for confusion when the persistence delegate tries
    //to call a constructor with two arguments
    public static HidableTableColumn createColumnWithHeader(String header, int modelIndex) {
        HidableTableColumn c = new HidableTableColumn(modelIndex);
        c.setHeaderValue(header);
        return c;
    }

    public HidableTableColumn(Object headerValue) {
        setHeaderValue(headerValue);
    }

    public HidableTableColumn(TableColumn c) {
        setIdentifier(c.getIdentifier());
        setModelIndex(c.getModelIndex());
        setHeaderValue(c.getHeaderValue());
        setPreferredWidth(c.getPreferredWidth());
        setWidth(c.getWidth());
    }

    public void addToHiddenColumnList(HidableTableColumn column) {
        if ( nextColumn == NULL_COLUMN) {
            nextColumn = column;
            nextColumn.previousColumn = this;
        } else {
            nextColumn.addToHiddenColumnList(column);
        }
    }

    //link any hidden columns assoicated with this column to the end of
    //the list of linked columns assoicated with the target
    public void relinkHiddenColumns(HidableTableColumn targetCol) {
        targetCol.addToHiddenColumnList(nextColumn);
        nextColumn = NULL_COLUMN;
    }

    //this hidden column is being removed, remove it from its current linked list of hidden cols
    public void removeFromHiddenColsList() {
        previousColumn.nextColumn = nextColumn;
        if ( nextColumn != NULL_COLUMN) {
            nextColumn.previousColumn = previousColumn;
        }
    }

    public void detachToMakeVisible() {
        if (previousColumn != NULL_COLUMN) {
            previousColumn.nextColumn = NULL_COLUMN;
        }
        previousColumn = NULL_COLUMN;
    }

    //visible columns are top level column in the column model
    //hidden columns are in a linked list associated with a visible column
    public boolean isHidden() {
        return previousColumn != NULL_COLUMN;
    }

    public void acceptVisitor(HidableColumnVisitor v) {
        v.visited(this);
    }

    public int getColumnCount() {
        return getAllColumns().size();
    }

    public ColumnGroup getColumnGroup() {
        return columnGroup;
    }

    public void setColumnGroup(ColumnGroup columnGroup) {
        this.columnGroup = columnGroup;
    }

    /**
     * @return  a List containing this column and any linked hidden columns
     */
    public List<HidableTableColumn> getAllColumns() {
        List<HidableTableColumn> cols = new LinkedList<HidableTableColumn>();
        HidableTableColumn c = this;
        while ( c != NULL_COLUMN) {
            cols.add(c);
            c = c.nextColumn;
        }
        return cols;
    }

    /**
     * @return a List containing any linked hidden columns
     */
    public List<HidableTableColumn> getHiddenColumns() {
        List<HidableTableColumn> cols = new LinkedList<HidableTableColumn>();
        if ( nextColumn != NULL_COLUMN) {
            cols.addAll(nextColumn.getAllColumns());
        }
        return cols;
    }

    public String toString() {
        return getHeaderValue() != null ? getHeaderValue().toString() : super.toString();
    }

   /**
    * Operations which require hiding/showing of groups of columns are implemented using Visitor pattern
    */
    public static interface HidableColumnVisitor<E> {

        void visited(HidableTableColumn h);

        /**
         * has the visitor finished its work (processing can stop without visiting more columns)
         */
        boolean isFinished();

        /**
         * @return true, if the operation was successful (not applicable for all visitor types)
         */
        boolean isSuccessful();


        E getResult();
    }
}
