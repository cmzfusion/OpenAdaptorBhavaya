package org.bhavaya.javafxui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX equivalent of EventQueueWatchdog.
 * Doesn't work in exactly the same way as you can't get hold of the event queue in JavaFX.
 * This requires the use of JavaFxUtilities.runOnJavaFxThread()
 * User: Jon Moore
 * Date: 29/11/13
 * Time: 14:30
 */
public class JavaFxEventQueueWatchdog {

    private static JavaFxEventQueueWatchdog instance = new JavaFxEventQueueWatchdog();

    public interface Listener {
        public void eventLagging(long duration, long edtThreadId);
    }

    private Runnable watchRunnable;
    private long eventStartTime;
    private boolean panicking;
    private final Object monitorLock = new Object();
    private ScheduledExecutorService executorService;
    private volatile boolean monitoringEnabled = false;

    private long javaFxThreadId = Long.MIN_VALUE;

    private JavaFxEventQueueWatchdog() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public static JavaFxEventQueueWatchdog getInstance() {
        return instance;
    }

    public void addLagListener(long pollPeriod, long maxEventTime, Listener listener) {
        if(monitoringEnabled) {
            executorService.scheduleAtFixedRate(new CheckTask(listener, maxEventTime), pollPeriod, pollPeriod, TimeUnit.MILLISECONDS);
        }
    }

    public void addDefaultLagListener(long pollPeriod, long maxEventTime) {
        addLagListener(pollPeriod, maxEventTime, new JavaFxEventQueueWatchLogger());
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    void watchRunnable(Runnable runnable) {
        if (javaFxThreadId == Long.MIN_VALUE) javaFxThreadId = Thread.currentThread().getId();
        startWatching(runnable, System.currentTimeMillis());
        runnable.run();
        stopWatching();
    }

    private void stopWatching() {
        synchronized (monitorLock) {
            this.watchRunnable = null;
            this.panicking = false;
        }
    }

    private void startWatching(Runnable runnable, long startTime) {
        synchronized (monitorLock) {
            this.watchRunnable = runnable;
            this.eventStartTime = startTime;
        }
    }

    public boolean isPanicking() {
        return panicking;
    }

    private class CheckTask implements Runnable{
        private Listener listener;
        private long maxEventTime;
        private Runnable lastRun;

        public CheckTask(Listener listener, long maxEventTime) {
            this.listener = listener;
            this.maxEventTime = maxEventTime;
        }

        public void run() {
            boolean fireEvent = false;
            long duration = 0;
            synchronized(monitorLock) {
                if (watchRunnable != null) {
                    duration = System.currentTimeMillis() - eventStartTime;
                    if (duration > maxEventTime && lastRun != watchRunnable) {
                        fireEvent = true;
                        lastRun = watchRunnable;
                        panicking = true;
                    }
                }
            }

            // Kept out of the synchronised block to avoid slowing the EDT
            if (fireEvent) {
                listener.eventLagging(duration, javaFxThreadId);
            }
        }
    }
}
