package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SyncMap;

import java.util.*;

/**
 * A simple priority based TaskExecutor supporting three different priorities.  Tasks priorities can be adjusted after
 * addition.
 */
public class PriorityTaskQueue extends TaskExecutor {
    public static final class Priority {
        private String name;

        private Priority(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public static final Priority PRIORITY_LOW = new Priority("Low");
    public static final Priority PRIORITY_NORMAL = new Priority("Normal");
    public static final Priority PRIORITY_HIGH = new Priority("High");

    private ReadWriteLock taskLock = new ReentrantWriterPreferenceReadWriteLock();
    private Map lowPriorityTask = new SyncMap(new LinkedHashMap(), taskLock);
    private Map normalPriorityTask = new SyncMap(new LinkedHashMap(), taskLock);
    private Map highPriorityTask = new SyncMap(new LinkedHashMap(), taskLock);


    public PriorityTaskQueue(String name, boolean verbose) {
        super(name, verbose);
        setLogPrefix("PriorityTaskQueue(" + name + "): ");
    }

    public int getQueueSizeImpl() {
        return lowPriorityTask.size() + normalPriorityTask.size() + highPriorityTask.size();
    }

    protected ReadWriteLock getTaskLock() {
        return taskLock;
    }

    protected void disposeImpl() {
        lowPriorityTask.clear();
        normalPriorityTask.clear();
        highPriorityTask.clear();
    }

    protected boolean addTaskImpl(Task task, Object metadata) {
        Map tasks;
        if (metadata == PRIORITY_LOW) tasks = lowPriorityTask;
        else if (metadata == PRIORITY_NORMAL || metadata == null) tasks = normalPriorityTask;
        else if (metadata == PRIORITY_HIGH) tasks = highPriorityTask;
        else throw new IllegalArgumentException("Metadata is not a valid priority");

        return tasks.put(task, task) != null;
    }

    public boolean adjustTaskPriority(Task task, Priority newPriority) {
        ThreadUtilities.quietAquire(taskLock.writeLock());
        try {
            if (removeTask(task)) {
                addTaskImpl(task, newPriority);
            } else {
                return false;
            }
        } finally {
            taskLock.writeLock().release();
        }
        return true;
    }

    private boolean removeTask(Task task) {
        boolean removed;
        if (!(removed = (lowPriorityTask.remove(task) == task))) {
            if (!(removed = (normalPriorityTask.remove(task)) == task)) {
                removed = highPriorityTask.remove(task) == task;
            }
        }
        return removed;
    }

    protected Task pollNextTask() {
        ThreadUtilities.quietAquire(taskLock.writeLock());
        try {
            Task task = null;
            if ((task = pollQueue(highPriorityTask)) == null) {
                if ((task = pollQueue(normalPriorityTask)) == null) {
                    task = pollQueue(lowPriorityTask);
                }
            }
            return task;
        } finally {
            taskLock.writeLock().release();
        }
    }

    private Task pollQueue(Map taskMap) {
        Task task = null;
        if (taskMap.size() > 0) {
            // Take the first object from the set, a little bit of a hack but does the trick.
            Iterator iterator = taskMap.entrySet().iterator();
            task = (Task) ((Map.Entry) iterator.next()).getValue();
            iterator.remove();
        }
        return task;
    }
}
