package org.bhavaya.util;

import java.awt.*;


/**
 * Casual observation of this class may lead one to suspect that the strain is finally taking its toll on the
 * embattled author.  What this class does is transfer a Swing.invokeLater call from an arbitrary to a thread
 * that has the same threadgroup as the classloader.
 * <p/>
 * Imagine that inside the invokeLater there's a piece of code calling
 * <code>Thread.currentThread().getThreadGroup()</code> and making as assumption from that threadgroup; perhaps something
 * dodgy.  This code makes sure that any invokeLaters happen on a thread that belongs to same threadgroup as the
 * thread that loaded this class.  This ultimately stops a duplicate AWTEventQueue from being creating by RMI
 * threads.
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class Workaround4711515_RMIThreadContext {
    private static final Object runnableLock = new Object();

    private static Runnable transferRunnable;

    static {
        Thread transferThread = new Thread(Thread.currentThread().getThreadGroup(), new Runnable() {
            public void run() {
                while (true) {
                    try {
                        // Wait until RMI thread signals runnable waiting
                        synchronized (runnableLock) {
                            runnableLock.wait();
                            EventQueue.invokeLater(transferRunnable);
                            runnableLock.notify();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Workaround4711515_RMIThreadContext");
        transferThread.setPriority(Thread.NORM_PRIORITY);
        transferThread.setDaemon(true);
        transferThread.start();
    }

    public static synchronized void invokeLater(Runnable runnable) {
        synchronized (runnableLock) {
            transferRunnable = runnable;
            transferRunnable.notify();

            try {
                runnableLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}