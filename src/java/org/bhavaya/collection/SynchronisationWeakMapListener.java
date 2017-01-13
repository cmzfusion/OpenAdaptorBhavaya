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

import org.bhavaya.util.Log;

/**
 * Synchronises a Synchronisable with an ObservableMap.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class SynchronisationWeakMapListener extends WeakMapListener {
    private static final Log log = Log.getCategory(SynchronisationWeakMapListener.class);
//        private Exception stackTrace = new Exception(); //TODO: keep in for a couple of weeks for extra info

    public interface Synchronisable {
        public void onAllRows();

        public void onCommit();

        public void onInsert(Object key, Object bean);

        public void onUpdate(Object key, Object bean);

        public boolean onDelete(Object key, Object bean);

        public boolean contains(Object key, Object bean);

        public boolean evaluate(Object key, Object bean);

        public String getLogPrefix();
    }

    public SynchronisationWeakMapListener(ObservableMap observableMap, Synchronisable synchronisable) {
        super(observableMap, synchronisable);
    }

    protected void mapChanged(Object mapListenerOwner, MapEvent mapEvent) {
        Synchronisable synchronisable = (Synchronisable) mapListenerOwner;
        String logPrefix = log.isDebug() ? synchronisable.getLogPrefix() : null;

        synchronized (synchronisable) {
            int mapEventType = mapEvent.getType();
            Object key = mapEvent.getKey();
            Object bean = mapEvent.getValue();

            if (mapEventType == MapEvent.ALL_ROWS) {
                if (log.isDebug()) log.debug(logPrefix + "processing ALL_ROWS event");
                synchronisable.onAllRows();
            } else if (mapEventType == MapEvent.COMMIT) {
                if (log.isDebug()) log.debug(logPrefix + "processing COMMIT event");
                synchronisable.onCommit();
            } else if (mapEventType == MapEvent.INSERT) {
                if (!synchronisable.contains(key, bean)) {
                    if (synchronisable.evaluate(key, bean)) {
                        if (log.isDebug()) log.debug(logPrefix + "inserting key: " + key);
                        synchronisable.onInsert(key, bean);
                    } else {
                        if (log.isDebug()) log.debug(logPrefix + "is ignoring insert for key: " + key + " because it is not within criteria");
                    }
                } else {
                    if (log.isDebug()) log.debug(logPrefix + "already has key: " + key);
                }
            } else if (mapEventType == MapEvent.UPDATE && key != null) {
                if (synchronisable.contains(key, bean)) {    //if contains
                    if (synchronisable.evaluate(key, bean)) {
                        if (log.isDebug()) log.debug(logPrefix + "updating key: " + key);
                        synchronisable.onUpdate(key, bean);
                    } else {
                        if (log.isDebug()) log.debug(logPrefix + "deleting key: " + key + " because it is nolonger within criteria");
                        synchronisable.onDelete(key, bean);
                    }
                } else {
                    if (synchronisable.evaluate(key, bean)) {
                        if (log.isDebug()) log.debug(logPrefix + "inserting key: " + key + " because it is now within criteria");
                        synchronisable.onInsert(key, bean);
                    } else {
                        if (log.isDebug()) log.debug(logPrefix + "is ignoring update for key: " + key + " because it is not within criteria");
                    }
                }
            } else if (mapEventType == MapEvent.DELETE && key != null) {
                boolean removed = synchronisable.onDelete(key, bean);
                if (removed) {
                    if (log.isDebug()) log.debug(logPrefix + "deleting key: " + key);
                }
            } else {
                throw new RuntimeException("CriteriaBeanCollection does not know how to handle map event of type: " + mapEventType);
            }
        }
    }

//        public String toString() {
//            StringWriter stringWriter = new StringWriter();
//            PrintWriter writer = new PrintWriter(stringWriter);
//            writer.write(super.toString() + "\nInstantiated at:\n");
//            stackTrace.printStackTrace(writer);
//            return stringWriter.toString();
//        }
}
