package org.bhavaya.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Description
 *
 * @author Brendon McLean
 */
public class SynchronousExecutionService extends AbstractExecutorService {
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private AtomicBoolean terminated = new AtomicBoolean(false);

    private Lock executeLock = new ReentrantLock();


    public void shutdown() {
        shutdown.set(true);
    }

    public List<Runnable> shutdownNow() {
        shutdown.set(true);
        terminated.set(true);
        return new ArrayList<Runnable>();
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            executeLock.lock();
            return true;
        } finally{
            executeLock.unlock();
        }
    }

    public void execute(Runnable command) {
        try {
            executeLock.lock();

            if (shutdown.get()) throw new RejectedExecutionException();
            command.run();
            if (shutdown.get()) terminated.set(true);

        } finally {
            executeLock.unlock();
        }
    }
}
