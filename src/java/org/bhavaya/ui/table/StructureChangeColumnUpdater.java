package org.bhavaya.ui.table;

import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;
import org.bhavaya.ui.table.column.model.HidableTableColumn;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 17-Oct-2008
 * Time: 14:51:33
 *
 * This class contains the logic to update the TableColumnModels for AnalyticsTable's
 * fixed and floating tables when a table structure change event occurs
 *
 * The ColumnModelUpdater is invoked twice, once on the fixed table column model
 * and once on the scrollable table column model.
 *
 * Columns with identifiers which no longer exist in the analytics table model are removed.
 * Missing columns are always created in the scrollable column model, not the fixed.
 */
public class StructureChangeColumnUpdater extends AbstractAnalyticsTableColumnModelUpdater {

    private AnalyticsTableModel analyticsTableModel;
    private ColumnHidingColumnModel columnModel;
    private ColumnHidingColumnModel oppositeColumnModel;
    private ColumnCreator columnCreator;
    private boolean isFixedTable;

    public StructureChangeColumnUpdater(AnalyticsTableModel analyticsTableModel, ColumnHidingColumnModel columnModel, ColumnHidingColumnModel oppositeColumnModel, ColumnCreator columnCreator, boolean isFixedTable) {
        this.analyticsTableModel = analyticsTableModel;
        this.columnModel = columnModel;
        this.oppositeColumnModel = oppositeColumnModel;
        this.columnCreator = columnCreator;
        this.isFixedTable = isFixedTable;
    }

    public void updateColumns() {
        updateTableModelIndexes();
        removeColsNoLongerInTable();
        updateHeaderValues();
        createMissingColumns();
        repositionColumns(analyticsTableModel, columnModel);
    }

    //update with the current indexes from the table model
    //some will not exist so will get a model index of -1, these will get removed
    private void updateTableModelIndexes() {
        for (HidableTableColumn column : columnModel.getAllColumns()) {
            Object identifier = column.getIdentifier();
            int columnIndex = analyticsTableModel.getColumnIndex(identifier);
            column.setModelIndex(columnIndex);
        }
    }

    private void createMissingColumns() {
        //missing columns are only ever created in the scrollable table
        if ( ! isFixedTable ) {
            //start with all columns and remove the ones we already have in either table's column model
            ArrayList<Integer> columnsToCreate = populateColumnsToCreate();
            removeExistingColumnsFromColsToCreate(columnsToCreate, columnModel);
            removeExistingColumnsFromColsToCreate(columnsToCreate, oppositeColumnModel);
            createNewColumns(columnsToCreate);
        }
    }

    //columns to remove do not exist in the table model so have had their indexes set to -1
    private void removeColsNoLongerInTable() {
        List<TableColumn> columnsToRemove = new ArrayList<TableColumn>();
        for (HidableTableColumn column : columnModel.getAllColumns()) {
            if (column.getModelIndex() < 0) {
                columnsToRemove.add(column);
            }
        }

        for ( TableColumn c : columnsToRemove ) {
            columnModel.removeColumn(c);
        }
    }

    private void removeExistingColumnsFromColsToCreate(ArrayList<Integer> columnsToCreate, ColumnHidingColumnModel colModel) {
        for (HidableTableColumn column : colModel.getAllColumns()) {
            columnsToCreate.remove(new Integer(column.getModelIndex()));
        }
    }

    private void updateHeaderValues() {
        for (HidableTableColumn column : columnModel.getAllColumns()) {
            String colName = analyticsTableModel.getCustomColumnName(column.getModelIndex());
            column.setHeaderValue(colName);
        }
    }

    private void createNewColumns(ArrayList<Integer> columnsToCreate) {
        for ( Integer colIndex : columnsToCreate) {
            TableColumn c = columnCreator.createTableColumn(colIndex);
            columnCreator.configureColumnAndAddToModel(analyticsTableModel, columnModel, c);
        }
    }

    private ArrayList<Integer> populateColumnsToCreate() {
        ArrayList<Integer> columnsToCreate = new ArrayList<Integer>();
        for (int i = 0; i < analyticsTableModel.getColumnCount(); i++) {
            columnsToCreate.add(i);
        }
        return columnsToCreate;
    }

}
