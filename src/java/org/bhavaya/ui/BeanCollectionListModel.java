/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.collection.ListEvent;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ListDataListener;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */
public class BeanCollectionListModel extends AbstractListModel {
    private static final Log log = Log.getCategory(BeanCollectionListModel.class);

    private BeanCollection beanCollection;
    private Object[] snapShot;
    private ListModelCollectionListener collectionListener;

    public BeanCollectionListModel(BeanCollection beanCollection) {
        this.beanCollection = beanCollection;
        collectionListener = new ListModelCollectionListener();
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        if (this.beanCollection != null) {
            this.beanCollection.removeCollectionListener(collectionListener);
        }
        this.beanCollection = beanCollection;
        this.beanCollection.addCollectionListener(collectionListener);
        updateSnapShot();
        fireContentsChanged(this, -1, -1);
    }

    //Added to fix bug caused by change to DefaultPersistenceDelegate in Java 6
    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    public int getSize() {
        return snapShot.length;
    }

    public Object getElementAt(int index) {
        return snapShot[index];
    }

    public synchronized void addListDataListener(ListDataListener l) {
        if (super.getListDataListeners().length == 0) {
            beanCollection.addCollectionListener(collectionListener);
            updateSnapShot();
        }
        super.addListDataListener(l);
    }

    private void updateSnapShot() {
        snapShot = beanCollection.toArray(new Object[beanCollection.size()]);
    }

    public synchronized void removeListDataListener(ListDataListener l) {
        super.removeListDataListener(l);
        if (super.getListDataListeners().length == 0) {
            dispose();
        }
    }

    public synchronized void dispose() {
        if (log.isDebug()) log.debug("Disposing :" + this);
        beanCollection.removeCollectionListener(collectionListener);
    }

    private class ListModelCollectionListener implements CollectionListener {
        private Runnable runnable;

        public ListModelCollectionListener() {
            runnable = new Runnable() {
                public void run() {
                    updateSnapShot();
                    fireContentsChanged(BeanCollectionListModel.this, -1, -1);
                }
            };
        }

        public void collectionChanged(ListEvent e) {
            if (e.getType() == ListEvent.COMMIT) {
                UIUtilities.runInDispatchThread(runnable);
            }
        }
    }
}