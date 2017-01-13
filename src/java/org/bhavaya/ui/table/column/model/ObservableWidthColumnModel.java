package org.bhavaya.ui.table.column.model;

import org.bhavaya.ui.table.FixedColumnTable;

import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 17-Sep-2009
* Time: 16:39:15
*
* Refactored from 'ModelAwareColumnModel' in FixedColumnTable
*/
public class ObservableWidthColumnModel extends FixedColumnTable.ModelAwareColumnModel {

    private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

    public ObservableWidthColumnModel() {
    }

    public void addColumnWidthListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public void removeColumnWidthListener(ChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }

    public void addColumn(TableColumn column) {
        super.addColumn(column);
        recalcWidthCache();
    }

    public void removeColumn(TableColumn column) {
        super.removeColumn(column);
        recalcWidthCache();
    }

    /**
     * Used instead of the traditional fireChanged method because this method basically
     * amounts to the same thing.
     */
    protected void recalcWidthCache() {
        super.recalcWidthCache();
        for (Iterator iterator = changeListeners.iterator(); iterator.hasNext();) {
            ChangeListener changeListener = (ChangeListener) iterator.next();
            changeListener.stateChanged(changeEvent);
        }
    }

}
