package org.bhavaya.ui.table;

import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * Date: 08-Oct-2008
 * Time: 15:52:04
 *
 * This class contains the logic to update the fixed and scrollable column models when the user
 * changes the view configuration for the analytics table
 */
public class ViewConfigurationColumnUpdater extends AbstractAnalyticsTableColumnModelUpdater {

    private AnalyticsTableModel analyticsTableModel;
    private TableColumnModel persistedColumnModel;
    private ColumnHidingColumnModel destinationColumnModel;
    private ColumnCreator columnCreator;

    public ViewConfigurationColumnUpdater(AnalyticsTableModel analyticsTableModel, TableColumnModel persistedColumnModel, ColumnHidingColumnModel destinationColumnModel, ColumnCreator columnCreator) {
        this.analyticsTableModel = analyticsTableModel;
        this.persistedColumnModel = persistedColumnModel;
        this.destinationColumnModel = destinationColumnModel;
        this.columnCreator = columnCreator;
    }

    /**
     * Reconfigure our column model to match the persisted column model
     */
    public void updateColumns() {
        //first show all columns in our current column model (some may be hidden) so that we can reposition them
        //to match the columns in the persisted model. Once the columns are updated we will hide the appropriate
        //groups as per the new view configuration
        destinationColumnModel.showAllColumns();

        removeColsNotInPersistedModel();
        createAndConfigureRequiredCols();
        repositionColumns(analyticsTableModel, destinationColumnModel);
    }

    private void removeColsNotInPersistedModel() {

        //get the ids for columns with identifiers which still exist in the tableModel
        Set idsToDisplay = getIdentifiersForValidCols(persistedColumnModel);

        //get the TableColumn instances in destinationColumnModel which don't have identifiers in idsToDisplay
        Set<TableColumn> colsToRemove = getColumnsToRemove(destinationColumnModel, idsToDisplay);

        for (TableColumn col : colsToRemove) {
            if (!isPivoted(col) ) {
                //don't remove pivoted columns, which are auto generated according to the source table data
                destinationColumnModel.removeColumn(col);
            }
        }
    }

    private Set<TableColumn> getColumnsToRemove(ColumnHidingColumnModel destinationColumnModel, Set colIdsToDisplay) {
        Set<TableColumn> columnsToRemove = new HashSet<TableColumn>();
        for (HidableTableColumn column : destinationColumnModel.getAllColumns()) {
            Object identifier = column.getIdentifier();
            if (!colIdsToDisplay.contains(identifier)) {
                columnsToRemove.add(column);
            }
        }
        return columnsToRemove;
    }

    private Set getIdentifiersForValidCols(TableColumnModel persistedModel) {
        Set columnIdentifiers = new HashSet();
        for (int i = 0; i < persistedModel.getColumnCount(); i++) {
            Object identifier = persistedModel.getColumn(i).getIdentifier();
            if (analyticsTableModel.getColumnIndex(identifier) != -1) {
                columnIdentifiers.add(identifier);
            }
        }
        return columnIdentifiers;
    }


    private void createAndConfigureRequiredCols() {
        //Iterate backwards adding columns to the front of the destination model as we go.
        //This preserve the order of columns in the persisted model
        for (int i = persistedColumnModel.getColumnCount() - 1; i >= 0; i--) {
            TableColumn persistedColumn = persistedColumnModel.getColumn(i);

            int index = findCurrentIndex(destinationColumnModel, persistedColumn);
            if (index >= 0) {
                configureExistingColumn(destinationColumnModel, persistedColumn, index);
            } else {
                addNewColumn(destinationColumnModel, persistedColumn);
            }
        }
    }

    private void configureExistingColumn(TableColumnModel destinationColumnModel, TableColumn persistedColumn, int indexInDestColModel) {
        TableColumn viewColumn = destinationColumnModel.getColumn(indexInDestColModel);
        configureColumn(persistedColumn, viewColumn);
        moveColumnToFront(destinationColumnModel, indexInDestColModel);
    }

    private void addNewColumn(TableColumnModel destinationColumnModel, TableColumn persistedColumn) {
        Object identifier = persistedColumn.getIdentifier();

        //no not create 'unlocked' missing pivot generated columns, these are auto generated
        if (! analyticsTableModel.isPivotGeneratedColumn(identifier)) {
            int modelIndex = analyticsTableModel.getColumnIndex(identifier);
            if ( modelIndex >= 0) {
                TableColumn newColumn = columnCreator.createTableColumn(modelIndex);
                columnCreator.configureColumnAndAddToModel(analyticsTableModel, destinationColumnModel, newColumn);
                configureColumn(persistedColumn, newColumn);
                moveColumnToFront(destinationColumnModel, destinationColumnModel.getColumnIndex(identifier));
            }
        }
    }

    private void configureColumn(TableColumn persistedColumn, TableColumn newColumn) {
        newColumn.setPreferredWidth(persistedColumn.getPreferredWidth());
        newColumn.setWidth(persistedColumn.getWidth());
        if ( (persistedColumn instanceof HidableTableColumn)) {
            ((HidableTableColumn)newColumn).setColumnGroup(
                ((HidableTableColumn)persistedColumn).getColumnGroup()
            );
        }
    }

    private void moveColumnToFront(TableColumnModel destinationColumnModel, int fromIndex) {
        destinationColumnModel.moveColumn(fromIndex, 0);
    }


    private int findCurrentIndex(TableColumnModel destinationColumnModel, TableColumn persistedColumn) {
        int viewColumnIdx = -1;
        // Try to find the persisted column's counterpart in the current columnModel
        try {
            viewColumnIdx = destinationColumnModel.getColumnIndex(persistedColumn.getIdentifier());
        } catch (IllegalArgumentException e) {
            //not present
        }
        return viewColumnIdx;
    }

    private boolean isPivoted(TableColumn col) {
        return analyticsTableModel.isPivotGeneratedColumn(col.getIdentifier());
    }

}
