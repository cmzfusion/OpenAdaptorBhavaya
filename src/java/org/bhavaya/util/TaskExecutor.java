package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.CondVar;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.List;

/**
 * Abstract class for executing deferred tasks in sequence.
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */
public abstract class TaskExecutor {
    private static final Log log = Log.getCategory(TaskExecutor.class);

    private static final int ONE_MINUTE = 60000;

    protected volatile int tasksAdded;
    protected volatile int tasksCompleted;
    protected volatile int tasksFailed;

    private String logPrefix;

    private List listeners;
    private Object listenersLock = new Object();

    private Task currentlyExecutingTask;

    private String name;
    private boolean verbose;
    private long timeOfLastLog;

    protected volatile boolean disposed = false;
    protected volatile boolean paused = false;

    private Sync tasksCompleteLock;
    private Sync executionLock;
    private CondVar tasksCompleteCondition;
    private CondVar executionCondition;

    private TaskRunner taskRunner;


    protected TaskExecutor(String name, boolean verbose) {
        this.name = name;
        this.verbose = verbose;
        disposed = false;

        tasksCompleteLock = new Mutex();
        tasksCompleteCondition = new CondVar(tasksCompleteLock);

        executionLock = new Mutex();
        executionCondition = new CondVar(executionLock);
    }

    public final void addListener(Listener listener) {
        synchronized (listenersLock) {
            listeners = Utilities.add(listener, listeners);
        }
    }

    public final void removeListener(Listener listener) {
        synchronized (listenersLock) {
            listeners = Utilities.remove(listener, listeners);
        }
    }

    protected final void fireChanged() {
        logDiagnostics();

        List listenersSnapshot;
        synchronized (listenersLock) {
            listenersSnapshot = listeners;
        }
        if (listenersSnapshot == null) return;

        for (int i = 0; i < listenersSnapshot.size(); i++) {
            Listener listener = (Listener) listenersSnapshot.get(i);
            listener.changed(tasksAdded, tasksCompleted, tasksFailed, currentlyExecutingTask); // there are race conditions in the values sent, assume listeners require approximate values
        }
    }

    private void logDiagnostics() {
        long time = System.currentTimeMillis();
        if (time - timeOfLastLog > ONE_MINUTE) {
            log.info(getLogPrefix() + "tasksAdded: " + tasksAdded + ", tasksCompleted: " + tasksCompleted + ", tasksFailed: " + tasksFailed + ", currentlyExecutingTask: " + currentlyExecutingTask);
            timeOfLastLog = time;
        }
    }

    protected final void logVerboseMessage(String message) {
        if (verbose) {
            log.info(message);
        } else if (log.isDebug()) {
            log.debug(message);
        }
    }


    protected final String getLogPrefix() {
        return logPrefix;
    }

    protected final void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public String getName() {
        return name;
    }

    public void start() {
        if (disposed) {
            log.error(getLogPrefix() + "attempting to start disposed instance");
            return;
        }

        ThreadUtilities.quietAquire(getTaskLock().readLock());
        try {
            if (taskRunner != null) return;
            taskRunner = new TaskRunner(getName());
            log.info(getLogPrefix() + "starting with " + getQueueSize() + " tasks");
            taskRunner.start();
        } finally {
            getTaskLock().readLock().release();
        }

        startImpl();
    }

    public final void dispose() {
        if (disposed) return;

        ThreadUtilities.quietAquire(getTaskLock().writeLock());
        try {
            log.info(getLogPrefix() + "shutting down");
            disposeImpl();
            signalExecutionCondition();
        } finally {
            getTaskLock().writeLock().release();
        }

        fireChanged();
        disposed = true;
    }

    /**
     * Suspend the task queue. Tasks added after this method has been called will not be run until the <code>resume</code>
     * method has been called.
     * <p/>
     * If there is a task being executed when <code>pause</code> is called, <code>pause</code> will not return until
     * the task has finished execution.
     */
    public final void pause() {
        if (disposed) return;
        paused = true;
        pauseImpl();
    }

    /**
     * Cause the task queue to start executing tasks. You will only need to use this method if you have
     * suspended the task queue by calling the <code>pause</code> method.
     */
    public final void resume() {
        if (disposed) return;
        paused = false;
        resumeImpl();
    }

    public final int getQueueSize() {
        ThreadUtilities.quietAquire(getTaskLock().readLock());
        try {
            return getQueueSizeImpl();
        } finally {
            getTaskLock().readLock().release();
        }
    }

    public void addTask(Task task) {
        addTask(task, null);
    }

    public final void addTask(Task task, Object metadata) {
        ThreadUtilities.quietAquire(getTaskLock().writeLock());
        boolean overwriting;
        try {
            overwriting = addTaskImpl(task, metadata);
        } finally {
            getTaskLock().writeLock().release();
        }

        if (overwriting) {
            logVerboseMessage(getLogPrefix() + "replaced task  (" + task + ")");
        } else {
            logVerboseMessage(getLogPrefix() + "added task " + tasksAdded + " (" + task + ")");
            tasksAdded++;
            if (!paused) {
                signalExecutionCondition();
            } else {
                logVerboseMessage(getLogPrefix() + " task " + tasksAdded + " (" + task + ") will not be run as queue has been paused");
            }
        }

        fireChanged();
    }

    protected abstract boolean addTaskImpl(Task task, Object metadata);

    protected abstract int getQueueSizeImpl();

    protected abstract Task pollNextTask();

    protected abstract ReadWriteLock getTaskLock();

    protected void startImpl() {
    }

    protected void disposeImpl() {
    }

    protected void pauseImpl() {
        if (currentlyExecutingTask == null) return;
        awaitTasksCompleteCondition();
    }

    protected void resumeImpl() {
        signalExecutionCondition();
    }

    protected final void awaitTasksCompleteCondition() {
        ThreadUtilities.quietAquire(tasksCompleteLock);
        try {
            ThreadUtilities.quietAwait(tasksCompleteCondition);
        } finally {
            tasksCompleteLock.release();
        }
    }

    protected final void signalTasksCompleteCondition() {
        tasksCompleteCondition.broadcast();
    }

    protected final void awaitExecutionCondition() {
        ThreadUtilities.quietAquire(executionLock);
        try {
            ThreadUtilities.quietAwait(executionCondition);
        } finally {
            executionLock.release();
        }
    }

    protected final void signalExecutionCondition() {
        executionCondition.signal();
    }


    public interface Listener {
        public void changed(int tasksAdded, int tasksCompleted, int tasksFailed, Task currentlyExecutingTask);
    }

    protected class TaskRunner extends Thread {
        public TaskRunner(String name) {
            super("TaskRunner(" + name + ")");
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY - 1);
        }

        public void run() {
            try {
                while (true) {
                    while (currentlyExecutingTask == null) {
                        if (disposed) return;
                        if (paused) awaitExecutionCondition();

                        if ((currentlyExecutingTask = pollNextTask()) == null) {
                            awaitExecutionCondition();
                        }
                    }
                    fireChanged();

                    runTaskWithLogging();

                    ThreadUtilities.quietAquire(getTaskLock().readLock());
                    currentlyExecutingTask = null;
                    signalTasksCompleteCondition();
                    getTaskLock().readLock().release();

                    fireChanged();
                    Thread.yield();
                }
            } catch (Throwable e) {
                // if this thread ever dies for any reason we want to know about it, e.g. a NoClassDefFoundError
                log.error(e);
            }
        }

        private void runTaskWithLogging() {
            logVerboseMessage(getLogPrefix() + "about to run task " + tasksCompleted + " (" + currentlyExecutingTask + ")");
            long startTime = System.currentTimeMillis();
            try {
                currentlyExecutingTask.run();
                logVerboseMessage(getLogPrefix() + "completed task " + tasksCompleted + " (" + currentlyExecutingTask + ") in " + (System.currentTimeMillis() - startTime) + " millis");
            } catch (Throwable t) {
                tasksFailed++;
                log.error(getLogPrefix() + "execution error, discarding task " + tasksCompleted + " (" + currentlyExecutingTask + ") in" + (System.currentTimeMillis() - startTime) + " millis", t);
            }
            tasksCompleted++;
        }
    }
}
