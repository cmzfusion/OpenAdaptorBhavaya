package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.CondVar;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */
public class ThreadUtilities {
    public static void quietAquire(Sync sync) {
        boolean wasInterrupted = Thread.interrupted();
        for (; ;) {
            try {
                sync.acquire();
                break;
            } catch (InterruptedException ex) {
                wasInterrupted = true;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static void quietAwait(CondVar condition) {
        boolean wasInterrupted = Thread.interrupted();
        try {
            condition.await();
        } catch (InterruptedException e) {
            wasInterrupted = true;
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static ThreadFactory createNamedThreadFactory(final String name) {
        SecurityManager s = System.getSecurityManager();
        final ThreadGroup group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();

        return new ThreadFactory() {
            final AtomicInteger threadNumber = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r, name + threadNumber.getAndIncrement(), 0);
                if (t.isDaemon()) t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
    }

    public static ExecutorService newNamedCachedPool(String name) {
        return Executors.newCachedThreadPool(createNamedThreadFactory(name));
    }

    public static ExecutorService newNamedSingleThreadExecutor() {
        return new SynchronousExecutionService();
    }

    public static ExecutorService newNamedFixedThreadExecutor(String name, int numThreads) {
        SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(numThreads, numThreads,
                0L, TimeUnit.MILLISECONDS,
                queue,
                createNamedThreadFactory(name));
        threadPoolExecutor.setRejectedExecutionHandler(new WaitingRejectedExecutionHandler());
        return threadPoolExecutor;
    }

    private static class WaitingRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
            }
        }
    }
}
