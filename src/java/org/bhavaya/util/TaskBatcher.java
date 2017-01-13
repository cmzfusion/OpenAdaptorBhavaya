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

import java.util.LinkedHashMap;

/**
 * Runs tasks in batches.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.19 $
 */
public class TaskBatcher {
    private static final Log log = Log.getCategory(TaskBatcher.class);
    private static final int oneMinute = 60000;

    private String name;
    private Object sleepLock = new Object();
    private boolean sleepNextIteration;
    private LinkedHashMap taskSet;
    private TaskRunner taskRunner;
    private long threadSleepDelay;
    private boolean disposed;
    private long minDelay;
    private long maxDelay;
    private float minCpuLoad;
    private float maxCpuLoad;
    private String logPrefix;

    private BatchRunnable preBatchRunTask;
    private BatchRunnable successfulBatchRunTask;
    private BatchRunnable failedBatchRunTask;
    private boolean verbose;

    private int tasksCompletedSinceLastLog;
    private long timeOfLastLog;
    private long timeToDoLastBatch;
    private long totalNumberOfAddTaskInvocations;
    private long totalNumberOfTasksCompleted;
    private long numberOfTasksInLastBatch;
    private long totalNumberOfBatchesCompleted;

    public TaskBatcher(String name, long minDelay, long maxDelay, float minCpuLoad, float maxCpuLoad) {
        this(name, minDelay, maxDelay, minCpuLoad, maxCpuLoad, false, null, null, null);
    }

    public TaskBatcher(String name, long minDelay, long maxDelay, float minCpuLoad, float maxCpuLoad, boolean verbose, BatchRunnable preBatchRunTask, BatchRunnable successfulBatchRunTask, BatchRunnable failedBatchRunTask) {
        this.name = name;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.minCpuLoad = minCpuLoad;
        this.maxCpuLoad = maxCpuLoad;
        this.preBatchRunTask = preBatchRunTask;
        this.successfulBatchRunTask = successfulBatchRunTask;
        this.failedBatchRunTask = failedBatchRunTask;
        this.verbose = verbose;

        // If a new task is added which is equal to a previous task, the previous task should be replaced with the new task.
        // A Set does not replace objects when you call add, and if you call remove than add on the set, then the order of tasks changes.
        // Using a LinkedHashMap with the task as both a key and value, allows the values to be replaced (even though the key isnt) and the
        // order of tasks to be maintained.
        taskSet = new LinkedHashMap();

        disposed = false;
        sleepNextIteration = true;
        threadSleepDelay = minDelay;

        logPrefix = "TaskBatcher(" + name + "): ";
    }

    public void executeImmediately() {
        synchronized (sleepLock) {
            sleepNextIteration = false;
            sleepLock.notify();
        }
    }

    public long getNumberOfTasksInLastBatch() {
        return numberOfTasksInLastBatch;
    }

    public long getTotalNumberOfTasksCompleted() {
        return totalNumberOfTasksCompleted;
    }

    public long getTotalNumberOfBatchesCompleted() {
        return totalNumberOfBatchesCompleted;
    }

    public long getTimeToDoLastBatch() {
        return timeToDoLastBatch;
    }

    public void addTask(Runnable task) {
        addTask(task, true);
    }

    public void addTask(Runnable task, boolean replaceExistingTask) {
        synchronized (taskSet) {
            addTaskInternal(replaceExistingTask, task);
        }
    }

    public void addTasks(Runnable[] tasks) {
        addTasks(tasks, true);
    }

    public void addTasks(Runnable[] tasks, boolean replaceExistingTask) {
        synchronized (taskSet) {
            for (int i = 0; i < tasks.length; i++) {
                Runnable task = tasks[i];
                addTaskInternal(replaceExistingTask, task);
            }
        }
    }

    private void addTaskInternal(boolean replaceExistingTask, Runnable task) {
        if (disposed) return;
        totalNumberOfAddTaskInvocations++;
        Object oldTask = taskSet.get(task);

        if (replaceExistingTask && oldTask != null) {
            if (task instanceof Comparable) {
                if (((Comparable) task).compareTo(oldTask) >= 0) {
                    taskSet.put(task, task);
                }
            } else {
                taskSet.put(task, task);
            }
        } else if (oldTask == null) {
            taskSet.put(task, task);
        }
    }

    public void start() {
        synchronized (taskSet) {
            if (disposed) {
                log.error(logPrefix + "attempting to start disposed instance");
                return;
            }
            if (taskRunner != null) return;
            log.info(logPrefix + "starting");
            taskRunner = new TaskRunner();
            taskRunner.setPriority(Thread.NORM_PRIORITY);
        }
        taskRunner.start();
    }

    public void dispose() {
        synchronized (taskSet) {
            if (disposed) return;

            log.info(logPrefix + "shutting down");
            disposed = true;
            taskSet.clear();
        }
    }

    private void logDiagnostics(final int tasksCompletedInCall, final long timeAtStartOfBatch) {
        final long now = System.currentTimeMillis();
        timeToDoLastBatch = now - timeAtStartOfBatch;
        numberOfTasksInLastBatch = tasksCompletedInCall;
        totalNumberOfTasksCompleted += tasksCompletedInCall;
        totalNumberOfBatchesCompleted++;
        if (verbose && log.isDebug()) log.debug(logPrefix + "numberOfTasksInLastBatch: " + numberOfTasksInLastBatch + ", timeToDoLastBatch: " + timeToDoLastBatch + ", totalNumberOfAddTaskInvocations: " + totalNumberOfAddTaskInvocations + ", totalNumberOfTasksCompleted: " + totalNumberOfTasksCompleted + ", totalNumberOfBatchesCompleted: " + totalNumberOfBatchesCompleted);

        tasksCompletedSinceLastLog = tasksCompletedSinceLastLog + tasksCompletedInCall;
        if (now - timeOfLastLog > oneMinute) {
            log.info(logPrefix + "tasksCompletedSinceLastLog: " + tasksCompletedSinceLastLog + ", threadSleepDelay: " + threadSleepDelay + ", totalNumberOfAddTaskInvocations: " + totalNumberOfAddTaskInvocations + ", totalNumberOfTasksCompleted: " + totalNumberOfTasksCompleted + ", totalNumberOfBatchesCompleted: " + totalNumberOfBatchesCompleted);
            timeOfLastLog = now;
            tasksCompletedSinceLastLog = 0;
        }
    }

    private class TaskRunner extends Thread {
        public TaskRunner() {
            super("TaskBatcher(" + name + ")");
            setDaemon(true);
        }

        public void run() {
            try {
                while (true) {
                    try {
                        Runnable[] taskSnapshot = null;
                        synchronized (taskSet) {
                            if (disposed) return;

                            if (taskSet.size() > 0) {
                                taskSnapshot = (Runnable[]) taskSet.values().toArray(new Runnable[taskSet.size()]);
                                taskSet.clear();
                            }
                        }

                        if (taskSnapshot != null) {
                            final long timeAtStartOfBatch = System.currentTimeMillis();

                            try {
                                runPreBatchRunTask(taskSnapshot);
                                runMainTasks(taskSnapshot);
                                runSucessfulBatchRunTask(taskSnapshot);
                            } catch (Throwable e) {
                                log.error(logPrefix + "error executing task", e);
                                runFailedBatchRunTask(taskSnapshot);
                            }
                            logDiagnostics(taskSnapshot.length, timeAtStartOfBatch);
                        }

                    } catch (Exception e) {
                        log.error(logPrefix + "error executing thread", e);
                    }

                    try {
                        sleep();
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            } catch (Throwable e) {
                // if this thread ever dies for any reason we want to know about it, e.g. a NoClassDefFoundError
                log.error(e);
            }
        }

        private void sleep() throws InterruptedException {
            synchronized (sleepLock) {
                if (sleepNextIteration) {
                    sleepLock.wait(getThreadSleepDelay());
                }
                sleepNextIteration = true;
            }
        }

        private long getThreadSleepDelay() {
            double currentAverageLoad = CpuLoad.getInstance().getLoad();
            if (currentAverageLoad > maxCpuLoad) threadSleepDelay = Math.min(threadSleepDelay + 10, maxDelay);
            if (currentAverageLoad < minCpuLoad) threadSleepDelay = Math.max(threadSleepDelay - 10, minDelay);
            return threadSleepDelay;

        }

        private void runMainTasks(Runnable[] taskSnapshot) throws Exception {
            long timeAtStartOfTasksRun = System.currentTimeMillis();
            for (int i = 0; i < taskSnapshot.length; i++) {
                Runnable task = taskSnapshot[i];
                task.run();
                if (i > 0 && i % 20 == 0) Thread.yield(); //for a big update, this will hog CPU, allow a breather every 20 messages
            }
            if (verbose && log.isDebug()) log.debug(logPrefix + "tasks took: " + (System.currentTimeMillis() - timeAtStartOfTasksRun) + " millis");
        }

        private void runPreBatchRunTask(Runnable[] taskSnapshot) throws Exception {
            if (preBatchRunTask != null) {
                long timeAtStartOfPreBatchTaskRun = System.currentTimeMillis();
                preBatchRunTask.run(taskSnapshot);
                if (verbose && log.isDebug()) log.debug(logPrefix + "preBatchRunTask took: " + (System.currentTimeMillis() - timeAtStartOfPreBatchTaskRun) + " millis");
            }
        }

        private void runSucessfulBatchRunTask(Runnable[] taskSnapshot) throws Exception {
            if (successfulBatchRunTask != null) {
                long timeAtStartOfSuccessfulBatchRunTask = System.currentTimeMillis();
                successfulBatchRunTask.run(taskSnapshot);
                if (verbose && log.isDebug()) log.debug(logPrefix + "successfulBatchRunTask took: " + (System.currentTimeMillis() - timeAtStartOfSuccessfulBatchRunTask) + " millis");
            }
        }

        private void runFailedBatchRunTask(Runnable[] taskSnapshot) {
            try {
                if (failedBatchRunTask != null) {
                    long timeAtStartOfFailedBatchRunTask = System.currentTimeMillis();
                    failedBatchRunTask.run(taskSnapshot);
                    if (verbose && log.isDebug()) log.debug(logPrefix + "failedBatchRunTask took: " + (System.currentTimeMillis() - timeAtStartOfFailedBatchRunTask) + " millis");
                }
            } catch (Throwable e1) {
                log.error(logPrefix + "error executing failedBatchRunTask task", e1);
            }
        }
    }

    public static interface BatchRunnable {
        public void run(Runnable[] runnables) throws Exception;
    }

    public static interface Runnable {
        public void run() throws Exception;
    }
}
