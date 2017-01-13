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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SyncMap;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of TaskExecutor that executes task in a FIFO queue.
 *
 * @author Brendon McLean
 * @version $Revision: 1.19 $
 */
public class TaskQueue extends TaskExecutor {
    private ReadWriteLock taskLock = new ReentrantWriterPreferenceReadWriteLock();
    private SyncMap tasks = new SyncMap(new LinkedHashMap(), taskLock);


    public TaskQueue(String name) {
        this(name, true);
    }

    public TaskQueue(String name, boolean verbose) {
        super(name, verbose);
        setLogPrefix("TaskQueue(" + name + "): ");
    }

    public void disposeImpl() {
        tasks.clear();
    }

    protected boolean addTaskImpl(Task task, Object metadata) {
        return tasks.put(task, task) != null;
    }

    protected int getQueueSizeImpl() {
        return tasks.size();
    }

    protected ReadWriteLock getTaskLock() {
        return taskLock;
    }

    protected Task pollNextTask() {
        ThreadUtilities.quietAquire(tasks.writerSync());
        try {
            Task task = null;
            if (tasks.size() > 0) {
                // Take the first object from the set, a little bit of a hack but does the trick.
                Iterator iterator = tasks.entrySet().iterator();
                task = (Task) ((Map.Entry) iterator.next()).getValue();
                iterator.remove();
            }
            return task;
        } finally {
            tasks.writerSync().release();
        }
    }
}