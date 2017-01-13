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

package org.bhavaya.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class WeakPropertyChangeListener implements PropertyChangeListener {
    private static final ReferenceQueue referenceQueue = new ReferenceQueue();
    private Object observableOrObservables;
    private Reference listenerOwnerReference;
    private String property;

    static {
        TimerTask referenceQueuePoller = new ReferenceQueuePollerRunnable();
        Utilities.getApplicationTimer().schedule(referenceQueuePoller, 2000, 2000);
    }

    protected abstract void propertyChange(Object listenerOwner, PropertyChangeEvent evt);

    protected WeakPropertyChangeListener(List observables, Object listenerOwner, String property) {
        this.observableOrObservables = observables;
        this.listenerOwnerReference = new WeakPropertyChangeListenerReference(listenerOwner, referenceQueue, this);
        this.property = property;
    }

    protected WeakPropertyChangeListener(org.bhavaya.util.Observable[] observables, Object listenerOwner, String property) {
        this(Arrays.asList(observables), listenerOwner, property);
    }

    protected WeakPropertyChangeListener(org.bhavaya.util.Observable observable, Object listenerOwner, String property) {
        this.observableOrObservables = observable;
        this.listenerOwnerReference = new WeakPropertyChangeListenerReference(listenerOwner, referenceQueue, this);
        this.property = property;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Object listenerOwner = listenerOwnerReference.get();
        if (listenerOwner == null) {
            return;
        }

        propertyChange(listenerOwner, evt);
    }


    private static class WeakPropertyChangeListenerReference extends WeakReference {
        private WeakPropertyChangeListener propertyChangeListener;

        public WeakPropertyChangeListenerReference(Object listenerOwner, ReferenceQueue queue, WeakPropertyChangeListener propertyChangeListener) {
            super(listenerOwner, queue);
            this.propertyChangeListener = propertyChangeListener;
        }

        public WeakPropertyChangeListener getPropertyChangeListener() {
            return propertyChangeListener;
        }
    }

    private static class ReferenceQueuePollerRunnable extends TimerTask {
        public void run() {
            WeakPropertyChangeListenerReference reference = (WeakPropertyChangeListenerReference) referenceQueue.poll();
            int i = 0;
            while (reference != null) {
                i++;
                if (i % 50 == 0) Thread.yield(); // be kind to other threads
                WeakPropertyChangeListener listener = reference.getPropertyChangeListener();
                if (listener.observableOrObservables instanceof Observable) {
                    Observable observable = (Observable) listener.observableOrObservables;
                    removeListener(listener, observable, listener.property);
                } else if (listener.observableOrObservables instanceof List) {
                    List observables = (List) listener.observableOrObservables;
                    for (Iterator iterator = observables.iterator(); iterator.hasNext();) {
                        org.bhavaya.util.Observable observable = (org.bhavaya.util.Observable) iterator.next();
                        removeListener(listener, observable, listener.property);
                    }
                }
                reference = (WeakPropertyChangeListenerReference) referenceQueue.poll();
            }
        }

        private void removeListener(PropertyChangeListener propertyChangeListener, Observable observable, String property) {
            if (property == null) {
                observable.removePropertyChangeListener(propertyChangeListener);
            } else {
                observable.removePropertyChangeListener(property, propertyChangeListener);
            }
        }
    }
}
