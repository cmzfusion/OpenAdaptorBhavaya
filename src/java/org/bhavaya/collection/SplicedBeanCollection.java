package org.bhavaya.collection;

import org.bhavaya.beans.SplicedBean;
import org.bhavaya.util.Log;

import java.lang.reflect.Constructor;
import java.util.Iterator;


/**
 * See comments in SplicedBean and usage example in SplicedBeanCollectionTest
 *
 * @author Andrew J. Dean
 * @version $Revision: 1.3 $
 */
public class SplicedBeanCollection extends DefaultBeanCollection {
    private static final Log log = Log.getCategory(SplicedBeanCollection.class);
    private final Object lock = this; // Updates to bean collections have to be synchronised on themselves as the table code assumes this
    private final BeanCollection firstBeanCollection;
    private final BeanCollection secondBeanCollection;

    public SplicedBeanCollection(Class splicedBeanClass, BeanCollection firstBeanCollection, BeanCollection secondBeanCollection) {
        super(splicedBeanClass);
        this.firstBeanCollection = firstBeanCollection;
        this.secondBeanCollection = secondBeanCollection;

        CollectionListener collectionListener = new CollectionListener() {
            public void collectionChanged(ListEvent e) {
                updateInternalCollection(e);
            }
        };

        firstBeanCollection.addCollectionListener(collectionListener);
        secondBeanCollection.addCollectionListener(collectionListener);

        initCollection();
    }

    private void initCollection() {
        synchronized (lock) {
            processCollection(firstBeanCollection);
            processCollection(secondBeanCollection);
        }
    }

    private void processCollection(BeanCollection beanCollection) {
        for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
            updateInternalCollection(new ListEvent(beanCollection, ListEvent.INSERT, iterator.next()));
        }
    }

    private void updateInternalCollection(ListEvent listEvent) {
        int eventType = listEvent.getType();
        if (eventType == ListEvent.COMMIT) {
            synchronized (lock) {
                fireCommit();
            }
            return;
        } else if (eventType == ListEvent.ALL_ROWS) {
            synchronized (lock) {
                clear(false, false);
                initCollection();
                fireCollectionChanged(new ListEvent(this, ListEvent.ALL_ROWS, null));
            }
            return;
        }

        Class beanClass = super.getType();
        SplicedBean newSplicedBean;
        try {
            Constructor constructor = beanClass.getConstructor(new Class[]{Object.class});
            newSplicedBean = (SplicedBean) constructor.newInstance(new Object[]{listEvent.getValue()});
        } catch (Exception e) {
            log.error("Error constructing new bean instance in SplicedBeanCollection", e);
            return;
        }

        synchronized (lock) {
            int index = indexOf(newSplicedBean);
            SplicedBean splicedBean;
            if (index != -1) {
                splicedBean = (SplicedBean) get(index);
            } else {
                splicedBean = newSplicedBean;
            }

            if (eventType == ListEvent.INSERT) {
                splicedBean.update(listEvent.getValue());
                if (index == -1) {
                    add(splicedBean, false, true);
                } else {
                    fireCollectionChanged(new ListEvent(this, ListEvent.UPDATE, splicedBean, index));
                }
            } else if (eventType == ListEvent.UPDATE) {
                if (index == -1) {
                    throw new RuntimeException("Problem in logic in SplicedBeanCollection, tried to update a bean that wasn't in combined collection");
                }
                splicedBean.update(listEvent.getValue());
                fireCollectionChanged(new ListEvent(this, ListEvent.UPDATE, splicedBean, index));
            } else if (eventType == ListEvent.DELETE) {
                splicedBean.remove(listEvent.getValue());
                if (!splicedBean.hasData()) {
                    if (!remove(splicedBean, false, true)) {
                        throw new RuntimeException("Problem in logic in SplicedBeanCollection, tried to delete a bean that wasn't in combined collection");
                    }
                } else {
                    fireCollectionChanged(new ListEvent(this, ListEvent.UPDATE, splicedBean, index));
                }
            } else if (eventType == ListEvent.ALL_ROWS) {
                clear(false, false);
                initCollection();
                fireCollectionChanged(new ListEvent(this, ListEvent.ALL_ROWS, null));
            } else {
                throw new RuntimeException("Do not know how to handle collection event of type: " + eventType);
            }
        }
    }
}

