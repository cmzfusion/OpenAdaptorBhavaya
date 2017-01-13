package org.bhavaya.ui;

import org.bhavaya.util.MemoryWatchdog;
import org.bhavaya.util.NamedExecutors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Component to display memory usage as a shaded bar, with text to show used and maximum memory.
 * The text colour will change to red when a certain threshold is reached (80% of available memory)
 * then flash when a second threshold is reached (90% of available memory)
 * User: Jonathan Moore
 * Date: 17/06/11
 * Time: 12:00
 */
public class MemoryComponent extends JComponent {

    private static final int BORDER = 3;
    private static final int FLASHING_DELAY = 500;
    private static final int NON_FLASHING_DELAY = 5000;
    private static final Color TEXT_COLOR_NORMAL = Color.BLACK;
    private static final Color TEXT_COLOR_WARN = new Color(200, 0, 40);
    private static final Color SHADED_REGION_COLOR = new Color(150, 170, 200);

    private long usedMemory = 0;
    private long usedMemoryMB = 0;
    private long max = MemoryWatchdog.getInstance().getMaxMemory();
    private int maxMB = convertToMB(max);
    private long flashThreshold = (long) (max * 0.9);
    private long warnThreshold = (long) (max * 0.8);
    private Color textColor = TEXT_COLOR_NORMAL;
    private static ScheduledExecutorService repaintExecutor = NamedExecutors.newSingleThreadScheduledExecutor("MemoryComponentRepaint");

    public MemoryComponent() {
        super();
        setPreferredSize(new Dimension(110, 22));
        setOpaque(false);
        new TimedTask(NON_FLASHING_DELAY, repaintExecutor).start();
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        //Position it above component as will usually be on the status bar
        return new Point(event.getX(), -15);
    }

    public void setUsedMemory(long usedMemory) {
        long oldValue = this.usedMemory;
        this.usedMemory = usedMemory;
        this.usedMemoryMB = convertToMB(usedMemory);
        firePropertyChange("usedMemory", oldValue, usedMemory);
        setToolTipText("Total available memory: "+maxMB+"M, Used: "+usedMemoryMB+"M");
        repaint();
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }


    /**
     * Task that checks and updates the current memory usage
     */
    private class TimedTask extends VariableDelaySwingTask {
        private boolean flashOn = false;
        private MemoryWatchdog memoryWatchdog;

        protected TimedTask(int delay, ScheduledExecutorService executorService) {
            super(delay, executorService);
            memoryWatchdog = MemoryWatchdog.getInstance();
        }

        @Override
        protected void doRunOnEventThread() {
            long newUsedMemory = memoryWatchdog.getUsedMemory();
            if(newUsedMemory >= flashThreshold) {
                if(usedMemory < flashThreshold) {
                    //going from non-flashing to flashing so change the delay to be quicker
                    setDelay(FLASHING_DELAY);
                }
                flashOn = !flashOn;
                setTextColor(flashOn ? TEXT_COLOR_WARN : TEXT_COLOR_NORMAL);
            } else {
                if(usedMemory >= flashThreshold) {
                    //going from flashing to non-flashing so change the delay to be slower
                    setDelay(NON_FLASHING_DELAY);
                }
                flashOn = false;
                setTextColor(newUsedMemory > warnThreshold ? TEXT_COLOR_WARN : TEXT_COLOR_NORMAL);
            }
            setUsedMemory(newUsedMemory);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //Draw border
        g.setColor(Color.GRAY);
        g.drawRect(BORDER, BORDER, getWidth() - (2 * BORDER), getHeight() - (2 * BORDER));

        //Draw shaded region
        double pct = (double)usedMemory / (double)max;
        int width = (int)(pct * (getWidth()-(2*BORDER)-2));
        g.setColor(SHADED_REGION_COLOR);
        g.fillRect(BORDER+1, BORDER+1, width, getHeight() - (2*BORDER) - 1);

        //Draw text
        String text = usedMemoryMB+"M of "+maxMB+"M";
        g.setColor(textColor);
        FontMetrics fontMetrics = g.getFontMetrics();
        int x = ((getWidth() - 2*BORDER - 2 - fontMetrics.stringWidth(text))/2) + BORDER + 1;
        int y = ((getHeight() - 2*BORDER - 2 - fontMetrics.getHeight())/2) + BORDER + fontMetrics.getHeight() - 1;
        g.drawString(text, x, y);
    }

    private int convertToMB(long bytes) {
        return Math.round(((float) bytes / (1024 * 1024)));
    }
}
