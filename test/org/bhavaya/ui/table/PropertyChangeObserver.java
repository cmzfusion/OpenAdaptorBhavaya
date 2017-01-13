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

package org.bhavaya.ui.table;

import org.bhavaya.ui.table.GraphChangeListener;
import org.bhavaya.ui.table.PathPropertyChangeEvent;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
class PropertyChangeObserver implements GraphChangeListener {
    private static final Log log = Log.getCategory(PropertyChangeObserver.class);

    private static final String NOTHING_RECEIVED = "nothing received yet";

    public Object observedValue = NOTHING_RECEIVED;
    public Object root;
    public ArrayList history = new ArrayList();
    public boolean wasMulti = false;
    public boolean allAffectSameRoots = true;
    public String name;
    public String filterPath;

    private Object singleLock = new Object();
    private Object multiLock = new Object();

    public PropertyChangeObserver(String name) {
        this(name, null);
    }

    public PropertyChangeObserver(String name, String filterPath) {
        this.name = name;
        this.filterPath = filterPath;
    }

    public synchronized void multipleChange(Collection changes, boolean allAffectSameRoots) {
        if (filterPath == null) {
            log.warn("Got a multi change on listener " + name + " but it has no filter set");
        }

        if (log.isDebug())log.debug(name + " got Multi change:");
        for (Iterator iterator = changes.iterator(); iterator.hasNext();) {
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) iterator.next();
            if (filterPath == null || Generic.beanPathArrayToString(event.getPathFromRoot()).equals(filterPath)) {
                graphChanged(event);
            }
        }
        wasMulti = true;
        this.allAffectSameRoots = allAffectSameRoots;

        synchronized (multiLock) {
            multiLock.notify();
        }
    }

    public synchronized void graphChanged(PathPropertyChangeEvent event) {
        wasMulti = false;
        observedValue = event.getNewValue();
        root = event.getRoots().iterator().next();
        this.allAffectSameRoots = true;

        history.add(event);

        synchronized (singleLock) {
            singleLock.notify();
        }
    }

    public synchronized void waitForSingleEvent(long timeout) throws InterruptedException {
        if (history.size() == 0) {
            synchronized (singleLock) {
                singleLock.wait(timeout);
            }
        }
    }

    public synchronized void waitForMultiEvent(long timeout) throws InterruptedException {
        if (history.size() == 0) {
            synchronized (multiLock) {
                multiLock.wait(timeout);
            }
        }
    }

    public void reset() {
        observedValue = NOTHING_RECEIVED;
        root = null;
        history.clear();
    }
}
