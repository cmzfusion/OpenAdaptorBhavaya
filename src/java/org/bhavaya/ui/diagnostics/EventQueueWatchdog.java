package org.bhavaya.ui.diagnostics;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Sep 19, 2006
 * Time: 2:13:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventQueueWatchdog extends EventQueue {
    private static EventQueueWatchdog instance;

    public interface Listener {
        public void eventLagging(AWTEvent e, long duration, long edtThreadId);
    }

    private long edtThreadId = Long.MIN_VALUE;
    private boolean panicing;

    private final Object monitorLock = new Object();

    private AWTEvent watchEvent;
    private long eventStartTime;

    private Timer timer;

    public EventQueueWatchdog() {
        timer = new Timer("EventQueueWatchDog");
    }

    public void install() {
        synchronized(EventQueueWatchdog.class) {
            if (instance != null) throw new RuntimeException("Already installed");
            instance = this;
            EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eventQueue.push(instance);
        }
    }

    public static EventQueueWatchdog getInstance() {
        synchronized(EventQueueWatchdog.class) {
            return instance;
        }
    }

    public void addLagListener(long pollPeriod, long maxEventTime, Listener listener) {
        timer.schedule(new CheckTask(listener, maxEventTime), pollPeriod, pollPeriod);
    }

    public void addDefaultLagListener(long pollPeriod, long maxEventTime) {
        timer.schedule(new CheckTask(new EventQueueWatchLogger(), maxEventTime), pollPeriod, pollPeriod);
    }

    public boolean isPanicing() {
        return panicing;
    }

    protected void dispatchEvent(AWTEvent event) {
        if (edtThreadId == Long.MIN_VALUE) edtThreadId = Thread.currentThread().getId();
        watchEvent(event, System.currentTimeMillis());
        super.dispatchEvent(event);
        stopWatching();
    }

    private void stopWatching() {
        synchronized (monitorLock) {
            this.watchEvent = null;
            this.panicing = false;
        }
    }

    private void watchEvent(AWTEvent event, long startTime) {
        synchronized (monitorLock) {
            this.watchEvent = event;
            this.eventStartTime = startTime;
        }
    }

    private class CheckTask extends TimerTask {
        private Listener listener;
        private long maxEventTime;
        private AWTEvent lastFiredEvent;

        public CheckTask(Listener listener, long maxEventTime) {
            this.listener = listener;
            this.maxEventTime = maxEventTime;
        }

        public void run() {
            boolean fireEvent = false;
            long duration = 0;
            AWTEvent event = null;
            synchronized(monitorLock) {
                if (watchEvent != null) {
                    duration = System.currentTimeMillis() - eventStartTime;
                    if (duration > maxEventTime && lastFiredEvent != watchEvent) {
                        fireEvent = true;
                        event = watchEvent;
                        lastFiredEvent = watchEvent;
                        panicing = true;
                    }
                }
            }

            // Kept out of the synchronised block to avoid slowing the EDT
            if (fireEvent) {
                listener.eventLagging(event, duration, edtThreadId);
            }
        }
    }
}
