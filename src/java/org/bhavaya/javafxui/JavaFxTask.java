package org.bhavaya.javafxui;

import org.bhavaya.javafxui.JavaFxUtilities;

/**
 * Task that defers it's action to the JavaFx event thread.
 * User: ga2mop0
 * Date: 30/07/13
 * Time: 11:54
 */
public abstract class JavaFxTask implements Runnable {
    public final void run() {
        Runnable r = new Runnable() {
            public void run() {
                runOnJavaFxThread();
            }
        };
        JavaFxUtilities.runLater(r);
    }

    protected abstract void runOnJavaFxThread();
}
