package org.bhavaya.javafxui;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.bhavaya.util.MemoryWatchdog;
import org.bhavaya.util.NamedExecutors;

import java.util.concurrent.ScheduledExecutorService;

/**
 * JavaFx component for displaying used memory.
 * Similar to Swing version MemoryComponent
 * User: ga2mop0
 * Date: 30/07/13
 * Time: 11:59
 */
public class JavaFxMemoryComponent extends StackPane {
    private static final int FLASHING_DELAY = 500;
    private static final int NON_FLASHING_DELAY = 5000;
    private static final Color TEXT_COLOR_NORMAL = Color.BLACK;
    private static final Color TEXT_COLOR_WARN = Color.rgb(200, 0, 40);

    private LongProperty usedMemoryProperty = new SimpleLongProperty();
    private long max = MemoryWatchdog.getInstance().getMaxMemory();
    private final int maxMB = convertToMB(max);
    private final long flashThreshold = (long) (max * 0.9);
    private final long warnThreshold = (long) (max * 0.8);
    private final Label text;
    private final ProgressBar progressBar;
    private static ScheduledExecutorService memoryCheckExecutor = NamedExecutors.newSingleThreadScheduledExecutor("JavaFxMemoryComponent");

    public JavaFxMemoryComponent() {
        super();

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(110);
        progressBar.setPrefHeight(22);

        text = new Label();
        text.setTooltip(new Tooltip());
        text.setTextFill(TEXT_COLOR_NORMAL);

        getChildren().addAll(progressBar, text);

        usedMemoryProperty.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                long usedMB = convertToMB(newValue.longValue());
                text.getTooltip().setText("Total available memory: " + maxMB + "M, Used: " + usedMB + "M");
                text.setText(usedMB+"M of "+maxMB+"M");

                double memoryFraction = (double)newValue.longValue() / (double)max;
                progressBar.setProgress(memoryFraction);
            }
        });

        new TimedTask(NON_FLASHING_DELAY, memoryCheckExecutor).start();
    }

    /**
     * Task that checks and updates the current memory usage
     */
    private class TimedTask extends VariableDelayJavaFxTask {
        private boolean flashOn = false;
        private MemoryWatchdog memoryWatchdog;

        protected TimedTask(int delay, ScheduledExecutorService executorService) {
            super(delay, executorService);
            memoryWatchdog = MemoryWatchdog.getInstance();
        }

        @Override
        protected void doRunOnJavaFxThread() {
            long newUsedMemory = memoryWatchdog.getUsedMemory();
            if(newUsedMemory >= flashThreshold) {
                if(usedMemoryProperty.get() < flashThreshold) {
                    //going from non-flashing to flashing so change the delay to be quicker
                    setDelay(FLASHING_DELAY);
                }
                flashOn = !flashOn;
                text.setTextFill(flashOn ? TEXT_COLOR_WARN : TEXT_COLOR_NORMAL);
            } else {
                if(usedMemoryProperty.get() >= flashThreshold) {
                    //going from flashing to non-flashing so change the delay to be slower
                    setDelay(NON_FLASHING_DELAY);
                }
                flashOn = false;
                text.setTextFill(newUsedMemory > warnThreshold ? TEXT_COLOR_WARN : TEXT_COLOR_NORMAL);
            }
            usedMemoryProperty.set(newUsedMemory);
        }
    }

    private int convertToMB(long bytes) {
        return Math.round(((float) bytes / (1024 * 1024)));
    }
}
