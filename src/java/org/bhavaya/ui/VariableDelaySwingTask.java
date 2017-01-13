package org.bhavaya.ui;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SwingTask that will be run at intervals that may be adjusted.
 * Rather than using scheduleWithFixedDelay we schedule individual events and once the run is completed schedule the next event.
 * That way we can adjust the delay between executions mid run,
 * User: ga2mhan
 * Date: 26/09/11
 * Time: 16:38
 */
public abstract class VariableDelaySwingTask extends SwingTask {
    private volatile int delay;
    private volatile boolean cancelled = false;
    private ScheduledExecutorService executorService;
    private Future future = null;

    protected VariableDelaySwingTask(int delay, ScheduledExecutorService executorService) {
        this.delay = delay;
        this.executorService = executorService;
    }

    public void start() {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                cancelled = false;
                cancelCurrentExecution(true);
                scheduleNextExecution();
            }
        });
    }

    public void stop(final boolean mayInterruptIfRunning) {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                cancelled = true;
                cancelCurrentExecution(mayInterruptIfRunning);
            }
        });
    }

    private void scheduleNextExecution() {
        if(!cancelled) {
            future = executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
    }

    private void cancelCurrentExecution(boolean mayInterruptIfRunning) {
        if(future != null) {
            future.cancel(mayInterruptIfRunning);
            future = null;
        }
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    protected final void runOnEventThread() {
        try {
            doRunOnEventThread();
        } finally {
            scheduleNextExecution();
        }
    }

    protected abstract void doRunOnEventThread();
}
