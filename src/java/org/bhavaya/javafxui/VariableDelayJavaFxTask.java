package org.bhavaya.javafxui;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JavaFxTask that will be run at intervals that may be adjusted.
 * Rather than using scheduleWithFixedDelay we schedule individual events and once the run is completed schedule the next event.
 * That way we can adjust the delay between executions mid run,
 * todo - this should be merged with VariableDelaySwingTask
 * User: ga2mop0
 * Date: 30/07/13
 * Time: 11:56
 */
public abstract class VariableDelayJavaFxTask extends JavaFxTask {
    private volatile int delay;
    private volatile boolean cancelled = false;
    private ScheduledExecutorService executorService;
    private Future future = null;

    protected VariableDelayJavaFxTask(int delay, ScheduledExecutorService executorService) {
        this.delay = delay;
        this.executorService = executorService;
    }

    public void start() {
        JavaFxUtilities.runLater(new Runnable() {
            public void run() {
                cancelled = false;
                cancelCurrentExecution(true);
                scheduleNextExecution();
            }
        });
    }

    public void stop(final boolean mayInterruptIfRunning) {
        JavaFxUtilities.runLater(new Runnable() {
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
    protected void runOnJavaFxThread() {
        try {
            doRunOnJavaFxThread();
        } finally {
            scheduleNextExecution();
        }
    }

    protected abstract void doRunOnJavaFxThread();
}

