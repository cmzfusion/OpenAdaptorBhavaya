package org.bhavaya.ui.freechart;

import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.util.Log;
import org.bhavaya.ui.freechart.InvalidColumnException;
import org.bhavaya.util.WeakReferenceTimer;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.*;
import java.util.Set;
import java.util.HashSet;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 26-Feb-2008
 * Time: 15:34:52
 *
 * A class which listens to a table model for changes to values in specific columns
 * If the model changes, any registered ColumnChangeListener instances will be informed,
 * but this notification is delayed/throttled. This can be useful in the case where we
 * don't want every update from the table model to be processed (e.g. don't want to redraw a
 * chart on every update, for performance reasons)
 *
 * Updates from the source table model are placed into three categories:
 * - 1. updates affecting category/series columns
 * - 2. updates affecting value columns only.
 * - 3. updates which have no effect on the chart, no action required!
 *
 * For this to work correctly, it is necessary to set the columns names which identify categories/values
 */
public class DelayedColumnChangeListener implements TableModelListener {

    private static final Log log = Log.getCategory(DelayedColumnChangeListener.class);

    private int[] categoryColumnIndices = new int[0];
    private int[] valueColumnsIndices = new int[0];

    private String[] categoryColumnLocators = new String[0];
    private String[] valueColumnLocators = new String[0];

    private int scheduledUpdateType;
    private boolean timerStarted;
    private int timerDelay;
    private static final int DEFAULT_TIMER_DELAY = 2000;

    private Set<ColumnChangeListener> columnChangeListeners = new HashSet<ColumnChangeListener>();

    //must hold a reference to this to prevent it being garbage collected when wrapped as a weak reference listener
    private TableUpdatesTimerListener timerListener = new TableUpdatesTimerListener();
    private KeyedColumnTableModel tableModel;

    public DelayedColumnChangeListener(KeyedColumnTableModel tableModel) {
        this(tableModel, DEFAULT_TIMER_DELAY);
    }

    public DelayedColumnChangeListener(KeyedColumnTableModel tableModel, int timerDelay) {
        this.tableModel = tableModel;
        this.timerDelay = timerDelay;
        tableModel.addTableModelListener(this);
    }

    public void setValueColumns(String... valueColumns) throws InvalidColumnException {
        this.valueColumnLocators = valueColumns;
        this.valueColumnsIndices = getColumnIndexes(valueColumns);
    }

    public void setCategoryColumns(String[] categoryColumns) throws InvalidColumnException {
        this.categoryColumnLocators = categoryColumns;
        this.categoryColumnIndices = getColumnIndexes(categoryColumnLocators);
    }

    public int[] getValueColumnsIndices() {
        return valueColumnsIndices;
    }

    public int[] getCategoryColumnIndices() {
        return categoryColumnIndices;
    }

    private void findColumnIndexes() {
        try {
            valueColumnsIndices = getColumnIndexes(valueColumnLocators);
            categoryColumnIndices = getColumnIndexes(categoryColumnLocators);
        } catch (InvalidColumnException e) {
            log.error(e);
        }
    }

    private int[] getColumnIndexes(String[] colLocators) throws InvalidColumnException {
        int[] indices = new int[colLocators.length];
        for ( int loop=0 ; loop < colLocators.length ; loop++) {
            indices[loop] = findColumnIndex(colLocators[loop]);
        }
        return indices;
    }

    private int findColumnIndex(String columnName) throws InvalidColumnException {
        int index = tableModel.getColumnIndex(columnName);
        if ( index == -1 ) {
            throw new InvalidColumnException(columnName);
        }
        return index;
    }

    public void tableChanged(TableModelEvent e) {
        switch ( e.getType() ) {
            case ( TableModelEvent.UPDATE ) :
                if ( e.getFirstRow() == TableModelEvent.HEADER_ROW ) {
                    setCategoriesChangedFlag();
                    setStructureChangedFlag();
                    findColumnIndexes();
                }
                else {
                    processColumnUpdate(e.getColumn());
                }
                break;
            default :
                //everything other than update requires a category recalc
                setCategoriesChangedFlag();
        }

        //if timer delay is zero propagate changes immediately, don't use the 'delay' function
        if ( timerDelay == 0 ) {
            fireEventIfDataChanged();
        }
    }

    public void addColumnChangeListener(ColumnChangeListener columnChangeListener) {
        startUpdateTimer(); 
        columnChangeListeners.add(columnChangeListener);
    }

    public void removeColumnChangeListener(ColumnChangeListener columnChangeListener) {
        columnChangeListeners.remove(columnChangeListener);
    }

    private void startUpdateTimer() {
        if ( ! timerStarted && timerDelay > 0 ) {
            System.out.println("Timer started");
            Timer updateTimer = WeakReferenceTimer.createTimer(timerDelay, timerListener);
            updateTimer.setRepeats(true);
            updateTimer.start();
            timerStarted = true;
        }
    }

    private class TableUpdatesTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            fireEventIfDataChanged();
        }
    }

    private void fireEventIfDataChanged() {
        if ( scheduledUpdateType != 0 ) {
            fireColumnsChanged();
            scheduledUpdateType = 0;
        }
    }

    private void fireColumnsChanged() {
        ColumnChangeEvent colEvent = new ColumnChangeEvent(scheduledUpdateType);
        for (ColumnChangeListener l : columnChangeListeners) {
            l.columnsChanged(colEvent);
        }
    }


    private void processColumnUpdate(int col) {
        if ( col == TableModelEvent.ALL_COLUMNS) {
            setCategoriesChangedFlag();
        }
        else if ( isCategoryColumn(col)) {
            setCategoriesChangedFlag();
        }
        else if ( isValueColumn(col)) {
            setValuesChangedFlag();
        }
    }

    private boolean isValueColumn(int column) {
        return containsCol(valueColumnsIndices, column);
    }

    private boolean isCategoryColumn(int column) {
        return containsCol(categoryColumnIndices, column);
    }

    private boolean containsCol(int[] columns, int colToFind) {
        boolean found = false;
        for ( int col : columns ) {
            if ( col == colToFind ) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void setCategoriesChangedFlag() {
        scheduledUpdateType |= ColumnChangeEvent.CATEGORIES_CHANGED_FLAG;
    }

    private void setValuesChangedFlag() {
        scheduledUpdateType |= ColumnChangeEvent.VALUES_CHANGED_FLAG;
    }

    private void setStructureChangedFlag() {
        scheduledUpdateType |= ColumnChangeEvent.STRUCTURE_CHANGED_FLAG;
    }

    /**
     * A listener to receive delayed notifications when data in specific columns
     * in the table model has been changed
     */
    public static interface ColumnChangeListener {

        void columnsChanged(ColumnChangeEvent colEvent);
    }

    public static class ColumnChangeEvent {

        private static final int VALUES_CHANGED_FLAG = 1;
        private static final int CATEGORIES_CHANGED_FLAG = 2;
        private static final int STRUCTURE_CHANGED_FLAG = 4;
        private int updateType;

        public ColumnChangeEvent( int updateType ) {
            this.updateType = updateType;
        }

        public boolean isValuesChange() {
            return (this.updateType & VALUES_CHANGED_FLAG) > 0;
        }

        public boolean isCategoriesChange() {
            return (this.updateType & CATEGORIES_CHANGED_FLAG) > 0;
        }

        //has the table model structure changed
        public boolean isStructureChange() {
            return (this.updateType & STRUCTURE_CHANGED_FLAG) > 0;
        }
    }

}
