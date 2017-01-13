package org.bhavaya.ui.table;

import org.bhavaya.util.Log;
import org.bhavaya.util.Transform;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.HashMap;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.4 $
 */
public class TransformTableModel extends AbstractTableModel implements TabularBeanAssociation, ChainableTableModel {
    private static final Log log = Log.getCategory(TransformTableModel.class);

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private HashMap columnToTransform = new HashMap();
    private HashMap transformClassToInstance = new HashMap(); // cache instances of transform classes
    private TableModelEventHandler eventHandler = new TableModelEventHandler();
    private KeyedColumnTableModel sourceModel;

    public Class getTransformClassForColumn(Object columnKey) {
        return (Class) columnToTransform.get(columnKey);
    }

    public void setTransformClassForColumn(Object columnKey, Class transformClass) {
        if (transformClass == null) {
            columnToTransform.remove(columnKey);
        } else {
            columnToTransform.put(columnKey, transformClass);
        }
        fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1, getColumnIndex(columnKey), TableModelEvent.UPDATE));
    }

    public int getTransformationCount() {
        return columnToTransform.size();
    }

    public int getColumnCount() {
        return (sourceModel == null) ? 0 : sourceModel.getColumnCount();
    }

    public int getRowCount() {
        return (sourceModel == null) ? 0 : sourceModel.getRowCount();
    }

    private Transform getTransform(Class transformClass) {
        Transform transform = (Transform) transformClassToInstance.get(transformClass);
        if (transform == null) {
            try {
                transform = (Transform) transformClass.newInstance();
                transformClassToInstance.put(transformClass, transform);
            } catch (Throwable t) {
                log.error("Couldn't instantiate transformation " + transformClass, t);
            }
        }
        return transform;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (sourceModel == null) return null;
        Object value = sourceModel.getValueAt(rowIndex, columnIndex);
        Object columnKey = getColumnKey(columnIndex);
        if (columnKey != null && columnToTransform.containsKey(columnKey)) {
            Transform transform = getTransform((Class) columnToTransform.get(columnKey));
            if (transform != null && value != CachedObjectGraph.DATA_NOT_READY) {
                value = transform.execute(value);
            }
        }
        return value;
    }

    public Object[] getBeansForLocation(int rowIndex, int columnIndex) {
        if (sourceModel instanceof TabularBeanAssociation) {
            return ((TabularBeanAssociation) sourceModel).getBeansForLocation(rowIndex, columnIndex);
        } else {
            return EMPTY_OBJECT_ARRAY;
        }
    }

    public boolean isSingleBean(int row, int column) {
        if (sourceModel instanceof TabularBeanAssociation) {
            return ((TabularBeanAssociation) sourceModel).isSingleBean(row, column);
        } else {
            return false;
        }
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceModel;
    }

    public void setSourceModel(KeyedColumnTableModel sourceModel) {
        if (this.sourceModel != null) {
            this.sourceModel.removeTableModelListener(eventHandler);
        }
        this.sourceModel = sourceModel;
        if (this.sourceModel != null) {
            this.sourceModel.addTableModelListener(eventHandler);
        }
        fireTableStructureChanged();
    }

    public void fireTableChanged(TableModelEvent e) {
        //Need to transform the new value
        TableModelEvent eventToFire = e;
        if (e instanceof CellsInColumnUpdatedEvent) {
            CellsInColumnUpdatedEvent event = (CellsInColumnUpdatedEvent) e;
            Object key = getColumnKey(e.getColumn());
            if (key != null && columnToTransform.containsKey(key)) {
                Transform transform = getTransform((Class) columnToTransform.get(key));
                if (transform != null) {
                    Object oldValue = event.getOldValue() == CachedObjectGraph.DATA_NOT_READY ? event.getOldValue() : transform.execute(event.getOldValue());
                    Object newValue = event.getNewValue() == CachedObjectGraph.DATA_NOT_READY ? event.getNewValue() : transform.execute(event.getNewValue());
                    CellsInColumnUpdatedEvent newEvent = new CellsInColumnUpdatedEvent(
                        (TableModel)event.getSource(),
                        event.getRows(),
                        event.getColumn(),
                        oldValue,
                        newValue
                    );
                    eventToFire = newEvent;
                }
            }
        }
        super.fireTableChanged(eventToFire);
    }

    public int getColumnIndex(Object columnKey) {
        if (sourceModel == null) return -1;
        return sourceModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int index) {
        if (sourceModel == null) return null;
        return sourceModel.getColumnKey(index);
    }

    public String getColumnName(int column) {
        if (sourceModel == null) return "";
        return sourceModel.getColumnName(column);
    }

    public Class getColumnClass(int column) {
        if (sourceModel == null) return super.getColumnClass(column);
        return sourceModel.getColumnClass(column);
    }

    void setColumnTransformMap(HashMap columnTotransform) {
        this.columnToTransform = columnTotransform;
        fireTableDataChanged();
    }

    HashMap getColumnTransformMap() {
        return columnToTransform;
    }

    private class TableModelEventHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            fireTableChanged(e);
        }
    }
}
