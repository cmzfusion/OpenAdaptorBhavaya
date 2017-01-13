package org.bhavaya.ui.table;

import java.beans.PropertyChangeEvent;

/**
 * Implementation of CachedObjectGraph that adds changes recieved to a queue
 * This should probably use an interface rather than reference to the BeanCollectionTableModel
 * User: ga2mhana
 * Date: 18/01/11
 * Time: 14:30
 */
public class QueuedCachedObjectGraph extends CachedObjectGraph {

    protected BeanCollectionTableModel beanCollectionTableModel;

    public QueuedCachedObjectGraph(Class beanType, boolean asynchronous, String name,
                                   BeanCollectionTableModel beanCollectionTableModel) {
        super(beanType, asynchronous, name);
        this.beanCollectionTableModel = beanCollectionTableModel;
    }

    protected void receivePropertyChange(CachedObjectGraph.CachedProperties cachedProperties, PropertyChangeEvent event) {
        beanCollectionTableModel.getChangeQueue().addChange(cachedProperties, event);
    }

    protected void processPropertyChange(CachedObjectGraph.CachedProperties cachedProperties, PropertyChangeEvent event) {
        super.receivePropertyChange(cachedProperties, event);
    }

    public Object getChangeLock() {
        return beanCollectionTableModel.getChangeLock();
    }
}