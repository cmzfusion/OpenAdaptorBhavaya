package org.bhavaya.util;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 10/01/11
 * Time: 08:59
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: Nick
 * Date: 14-Dec-2010
 * Time: 18:31:25
 *
 * An easy way to create Executors which name the threads, and configure other aspects of the threads created.
 * For some reason the factory methods on Executors don't give you this option
 */
public class NamedExecutors {

    public static ThreadConfigurer DAEMON_THREAD_CONFIGURER = new ThreadConfigurer() {
        public void configureNewThread(Thread t) {
            t.setDaemon(true);
        }
    };

    public static ThreadConfigurer DEFAULT_THREAD_CONFIGURER = new ThreadConfigurer() {
        public void configureNewThread(Thread t) {
        }
    };

    public static ExecutorService newFixedThreadPool(String executorName, int nThreads) {
        return Executors.newFixedThreadPool(nThreads, new NamedThreadFactory(executorName + "-FixedThreadPool(" + nThreads + ")", DEFAULT_THREAD_CONFIGURER));
    }

    public static ExecutorService newSingleThreadExecutor(String executorName) {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(executorName + "-SingleThreadExecutor", DEFAULT_THREAD_CONFIGURER));
    }

    public static ExecutorService newCachedThreadPool(String executorName) {
        return Executors.newCachedThreadPool(new NamedThreadFactory(executorName + "-CachedThreadPool", DEFAULT_THREAD_CONFIGURER));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String executorName) {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(executorName + "-SingleThreadScheduledExecutor", DEFAULT_THREAD_CONFIGURER));
    }

    public static ScheduledExecutorService newScheduledThreadPool(String executorName, int corePoolSize, ThreadConfigurer threadConfigurer) {
        return Executors.newScheduledThreadPool(corePoolSize, new NamedThreadFactory(executorName + "-ScheduledThreadPool(" + corePoolSize + ")", threadConfigurer));
    }

    public static ExecutorService newFixedThreadPool(String executorName, int nThreads, ThreadConfigurer threadConfigurer) {
        return Executors.newFixedThreadPool(nThreads, new NamedThreadFactory(executorName + "-FixedThreadPool(" + nThreads + ")", threadConfigurer));
    }

    public static ExecutorService newSingleThreadExecutor(String executorName, ThreadConfigurer threadConfigurer) {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(executorName + "-SingleThreadExecutor", threadConfigurer));
    }

    public static ExecutorService newCachedThreadPool(String executorName, ThreadConfigurer threadConfigurer) {
        return Executors.newCachedThreadPool(new NamedThreadFactory(executorName + "-CachedThreadPool", threadConfigurer));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String executorName, ThreadConfigurer threadConfigurer) {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(executorName + "-SingleThreadScheduledExecutor", threadConfigurer));
    }

    public static ScheduledExecutorService newScheduledThreadPool(String executorName, int corePoolSize) {
        return Executors.newScheduledThreadPool(corePoolSize, new NamedThreadFactory(executorName + "-ScheduledThreadPool(" + corePoolSize + ")", DEFAULT_THREAD_CONFIGURER));
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private ThreadGroup threadGroup;
        private AtomicInteger threadNumber;
        private String name;
        private ThreadConfigurer threadConfigurer;

        public NamedThreadFactory(String name, ThreadConfigurer threadConfigurer) {
            this.threadConfigurer = threadConfigurer;
            threadNumber = new AtomicInteger(0);
            this.name = name;
            threadGroup = new ThreadGroup(name);
        }

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(threadGroup, r, new StringBuilder(name).append("-").append(threadNumber.getAndIncrement()).toString());
            threadConfigurer.configureNewThread(thread);
            return thread;
        }
    }

    public static interface ThreadConfigurer {
        void configureNewThread(Thread t);
    }

}
