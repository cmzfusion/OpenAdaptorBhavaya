package org.bhavaya.ui.freechart;

import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.bhavaya.ui.freechart.DelayedColumnChangeListener;
import org.bhavaya.ui.freechart.InvalidColumnException;
import org.bhavaya.ui.table.KeyedColumnTableModel;

import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Feb-2008
 * Time: 11:43:39
 *
 * A superclass for table driven data sets, used to provide source data for a JFreeChart
 * Adds functionality to subscribe to TableModelUpdates and notify subclasses when the data in the source
 * TableModel has changed. 
 */
public abstract class TableDrivenDataSet implements Dataset, DelayedColumnChangeListener.ColumnChangeListener {

    private Set<DatasetChangeListener> changeListeners = new HashSet<DatasetChangeListener>();
    private DatasetGroup datasetGroup;
    private DelayedColumnChangeListener delayedColumnChangeListener;

    public TableDrivenDataSet(KeyedColumnTableModel tableModel) {
        this(tableModel, 0);
    }

    public TableDrivenDataSet(KeyedColumnTableModel tableModel, int repaintDelay) {
        delayedColumnChangeListener = new DelayedColumnChangeListener(tableModel, repaintDelay);
        delayedColumnChangeListener.addColumnChangeListener(this);
    }

    protected void setValueColumns(String... valueColumns) throws InvalidColumnException {
        delayedColumnChangeListener.setValueColumns(valueColumns);
    }

    protected void setCategoryColumns(String... categoryColumns) throws InvalidColumnException {
        delayedColumnChangeListener.setCategoryColumns(categoryColumns);
    }

    //////////////////////////////////////////////
    // DataSet methods

    public void addChangeListener(DatasetChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(DatasetChangeListener listener) {
        changeListeners.remove(listener);
    }

    public DatasetGroup getGroup() {
        return datasetGroup;
    }

    public void setGroup(DatasetGroup group) {
        datasetGroup = group;
    }

    protected void fireDatasetChanged() {
        DatasetChangeEvent changeEvent = new DatasetChangeEvent(this, this);
        for (DatasetChangeListener datasetChangeListener : changeListeners ) {
            datasetChangeListener.datasetChanged(changeEvent);
        }
    }

}
