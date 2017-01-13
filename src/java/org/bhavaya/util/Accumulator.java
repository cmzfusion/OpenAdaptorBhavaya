package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.CondVar;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * Allows for easy batching of single objects into arrays based on timer thresholds or capacity thresholds.  An
 * accumulator will store objects added by its add method.  These objects will then be sent to a handler for processing
 * when either:
 *  1. T amount of idle time expires.
 *  2. X amount objects have been accumulated.
 */

public class Accumulator<E> {
    public interface Handler {
        void handleBatch(Object[] objects) throws Exception;
    }

    private static final Log log = Log.getCategory(Accumulator.class);

    private volatile long lastRequestTime = 0;
    private final Set<E> requestQueue = new LinkedHashSet<E>();
    private String name;
    private int itemsToTriggerBatch;
    private long idleTimeToTriggerBatch;
    private Handler handler;

    private Mutex triggerLock = new Mutex();
    private CondVar trigger = new CondVar(triggerLock);


    public Accumulator(String name, int itemsToTriggerBatch, long idleTimeToTriggerBatch, Handler handler) {
        this.name = name;
        this.itemsToTriggerBatch = itemsToTriggerBatch;
        this.idleTimeToTriggerBatch = idleTimeToTriggerBatch;
        this.handler = handler;
    }

    public void start() {
        Thread handlerThread = Utilities.newThread(new Runnable() {
            public void run() {
                Accumulator.this.run();
            }
        }, name, true);
        handlerThread.start();
    }

    public void run() {
        while (true) {
            boolean ignoreTime = waitForTrigger();

            long timeSinceLastAdd = System.currentTimeMillis() - lastRequestTime;
            if (ignoreTime || (timeSinceLastAdd > idleTimeToTriggerBatch)) {
                try {
                    Object[] queuedObjects = getQueuedObjects();
                    if (queuedObjects.length > 0) handler.handleBatch(queuedObjects);
                } catch (Exception e) {
                    log.error("Error handling batch", e);
                }
            }
        }
    }

    public void add(E o) {
        synchronized (requestQueue) {
            requestQueue.add(o);
            lastRequestTime = java.lang.System.currentTimeMillis();
            if (requestQueue.size() >= itemsToTriggerBatch) triggerBatch();
        }
    }

    public void addAll(E[] array) {
        synchronized (requestQueue) {
            requestQueue.addAll(Arrays.asList(array));
            lastRequestTime = java.lang.System.currentTimeMillis();
            if (requestQueue.size() >= itemsToTriggerBatch) triggerBatch();
        }
    }

    private boolean waitForTrigger() {
        ThreadUtilities.quietAquire(triggerLock);
        try {
            long start = System.currentTimeMillis();
            trigger.timedwait(idleTimeToTriggerBatch / 2);
            return System.currentTimeMillis() - start < idleTimeToTriggerBatch;
        } catch (InterruptedException e) {
            return false;
        } finally {
            triggerLock.release();
        }
    }

    private void triggerBatch() {
        trigger.signal();
    }

    private Object[] getQueuedObjects() {
        Object[] objects = new Object[0];
        synchronized (requestQueue) {
            if (requestQueue.size() > 0) {
                objects = requestQueue.toArray();
                requestQueue.clear();
            }
        }
        return objects;
    }
}