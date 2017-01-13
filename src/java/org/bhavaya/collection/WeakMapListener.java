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

package org.bhavaya.collection;

import org.bhavaya.util.Utilities;

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
public abstract class WeakMapListener implements MapListener {
    private static final ReferenceQueue referenceQueue = new ReferenceQueue();
    private List observableMaps;
    private Reference listenerOwnerReference;

    static {
        TimerTask referenceQueuePoller = new ReferenceQueuePollerRunnable();
        Utilities.getApplicationTimer().schedule(referenceQueuePoller, 2000, 2000);
    }

    protected abstract void mapChanged(Object mapListenerOwner, MapEvent mapEvent);

    protected WeakMapListener(List observableMaps, Object listenerOwner) {
        this.observableMaps = observableMaps;
        this.listenerOwnerReference = new WeakMapListenerReference(listenerOwner, referenceQueue, this);
    }

    protected WeakMapListener(ObservableMap[] observableMaps, Object listenerOwner) {
        this(Arrays.asList(observableMaps), listenerOwner);
    }

    protected WeakMapListener(ObservableMap observableMap, Object listenerOwner) {
        this(new ObservableMap[]{observableMap}, listenerOwner);
    }

    private List getObservableMaps() {
        return observableMaps;
    }

    public void mapChanged(MapEvent mapEvent) {
        Object mapListenerOwner = listenerOwnerReference.get();
        if (mapListenerOwner == null) {
            return;
        }

        mapChanged(mapListenerOwner, mapEvent);
    }


    private static class WeakMapListenerReference extends WeakReference {
        private WeakMapListener mapListener;

        public WeakMapListenerReference(Object listenerOwner, ReferenceQueue queue, WeakMapListener mapListener) {
            super(listenerOwner, queue);
            this.mapListener = mapListener;
        }

        public WeakMapListener getMapListener() {
            return mapListener;
        }
    }

    private static class ReferenceQueuePollerRunnable extends TimerTask {
        public void run() {
            WeakMapListenerReference reference = (WeakMapListenerReference) referenceQueue.poll();
            int i = 0;
            while (reference != null) {
                i++;
                if (i % 50 == 0) Thread.yield(); // be kind to other threads
                WeakMapListener listener = reference.getMapListener();
                List observableMaps = listener.getObservableMaps();
                for (Iterator iterator = observableMaps.iterator(); iterator.hasNext();) {
                    ObservableMap observableMap = (ObservableMap) iterator.next();
                    observableMap.removeMapListener(listener);
                }
                reference = (WeakMapListenerReference) referenceQueue.poll();
            }
        }
    }
}
