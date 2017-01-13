package org.bhavaya.ui;

/**
 * Task that defers it's action to the event thread.
 * User: ga2mhan
 * Date: 26/09/11
 * Time: 14:07
 */
public abstract class SwingTask implements Runnable {
    public final void run() {
        Runnable r = new Runnable() {
            public void run() {
                runOnEventThread();
            }
        };
        UIUtilities.runInDispatchThread(r);
    }

    protected abstract void runOnEventThread();
}
