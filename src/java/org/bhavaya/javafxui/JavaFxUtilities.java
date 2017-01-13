package org.bhavaya.javafxui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: ga2mop0
 * Date: 30/07/13
 * Time: 10:02
 * To change this template use File | Settings | File Templates.
 */
public class JavaFxUtilities {
    private static final Log LOG = Log.getCategory(JavaFxUtilities.class);

    private JavaFxUtilities() {}

    public static void initJavaFx() {
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new JFXPanel(); // initializes JavaFX environment
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.error("Error initialising JavaFx", e);
        }
    }

    public static <T> T runAndWait(final Callable<T> callable) throws Throwable {
        final ReturnValueHolder<T> returnValueHolder = new ReturnValueHolder<>();
        final CountDownLatch latch = new CountDownLatch(1);
        runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    returnValueHolder.returnValue = callable.call();
                } catch (Throwable e) {
                    returnValueHolder.throwable = e;
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();
        if(returnValueHolder.throwable != null) {
            throw returnValueHolder.throwable;
        }
        return returnValueHolder.returnValue;
    }

    public static void runLater(Runnable r) {
        Platform.runLater(wrapRunnable(r));
    }

    private static Runnable wrapRunnable(Runnable r) {
        return JavaFxEventQueueWatchdog.getInstance().isMonitoringEnabled() ? new JavaFxMonitorRunnable(r) : r;
    }

    public static void runOnJavaFxThread(Runnable r) {
        r = wrapRunnable(r);
        if(Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static class ReturnValueHolder<T> {
        private T returnValue;
        private Throwable throwable;
    }

    private static class JavaFxMonitorRunnable implements Runnable {
        private final Runnable delegate;

        JavaFxMonitorRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            JavaFxEventQueueWatchdog.getInstance().watchRunnable(delegate);
        }
    }
}
