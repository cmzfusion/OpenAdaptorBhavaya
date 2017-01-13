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


/**
 * Runs tasks after a set period, if the task is reset before the previous task is run
 * then the previous task is discarded.
 *
 * @author Daniel Van Enckevort
 * @version $Revision: 1.2 $
 */
public class TaskScheduler {
    private static final Log log = Log.getCategory(TaskScheduler.class);

    private Runnable task;
    private long timeTillExecute;
    private long taskChangePeriod;
    private Thread taskRunner;
    private boolean close;

    public TaskScheduler(final long taskChangePeriod, final boolean runTaskInOwnThread) {
        this.taskChangePeriod = taskChangePeriod;
        this.timeTillExecute = 0;
        this.close = false;

        taskRunner = Utilities.newThread(new Runnable() {
            public void run() {
                synchronized (taskRunner) {
                    while (!close) {
                        try {
                            if (log.isDebug()) log.debug("Waiting for: " + timeTillExecute);
                            // when timeTillExecute is 0 will wait indefinetly until a task is set
                            // and the thread in interrupted.
                            taskRunner.wait(timeTillExecute);
                            timeTillExecute = 0;
                            if (task != null) {
                                if (log.isDebug()) log.debug("Running task: " + task.toString());
                                if (runTaskInOwnThread) {
                                    Utilities.newThread(task, task.toString(), false).start();
                                } else {
                                    task.run();
                                }
                                if (log.isDebug()) log.debug("Finished task: " + task.toString());
                                task = null;
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }, "TaskScheduler-" + taskChangePeriod, true);

        taskRunner.start();
    }


    public void setTask(Runnable task) {
        synchronized (taskRunner) {
            if (this.task != null) if (log.isDebug()) log.debug("Removing task: " + this.task.toString());
            if (log.isDebug()) log.debug("Adding task: " + task.toString());
            this.task = task;
            timeTillExecute = taskChangePeriod;
            taskRunner.interrupt();
        }
    }

    public void close() {
        synchronized (taskRunner) {
            close = true;
            taskRunner.interrupt();
        }
    }
}
