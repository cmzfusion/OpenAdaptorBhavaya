/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.bhavaya.util.IOUtilities;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * A component that displays a scrolling list of log statements received from the Log class.
 * <p/>
 * This is now a thoroughly unpleasant class.  Taking a landrover for testdrive with a can of baked beans in your
 * underpants is more pleasant than this class.
 *
 * @author Brendon McLean
 * @version $Revision: 1.12 $
 * @see org.bhavaya.util.Log
 */
public class LogPanel extends JPanel implements Log.Listener {
    private static final Map levelToIconMap = new HashMap();

    private ArrayListModel model = new ArrayListModel();
    private JList logEvents;
    private int maxNoOfEvents;
    private Font font;
    private Map levelToColorMap = new HashMap();

    static {
        levelToIconMap.put(Level.INFO, ImageIconCache.getImageIcon("information.png"));
        levelToIconMap.put(Level.WARN, ImageIconCache.getImageIcon("warning.png"));
        levelToIconMap.put(Level.ERROR, ImageIconCache.getImageIcon("error.png"));
        levelToIconMap.put(Level.FATAL, ImageIconCache.getImageIcon("error.png"));
    }

    public LogPanel() {
        this(500, -1);
    }

    public LogPanel(int fontSize) {
        this(500, fontSize);
    }

    public LogPanel(int maxNoOfEvents, int fontSize) {
        this.maxNoOfEvents = maxNoOfEvents;
        logEvents = new JList(model);
        this.font = logEvents.getFont();
        if (fontSize > 0) {
            this.font = font.deriveFont(font.getStyle(), fontSize);
        }
        logEvents.setCellRenderer(new JLabelRenderer());
        JScrollPane listTableScrollPane = new JScrollPane(logEvents);
        setLayout(new BorderLayout());
        add(listTableScrollPane);

        levelToColorMap.put(Level.FATAL, Color.RED);
        levelToColorMap.put(Level.ERROR, Color.RED);
        levelToColorMap.put(Level.WARN, new Color(128,0,0));
        levelToColorMap.put(Level.INFO, Color.BLUE);
    }

    public void logMessage(final LoggingEvent loggingEvent) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new UpdateLogListEvent(this, loggingEvent));
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof UpdateLogListEvent) {
            model.addAll(((UpdateLogListEvent)e).loggingEvents);
            while (model.size() >= maxNoOfEvents) model.remove(0);
            logEvents.ensureIndexIsVisible(model.size() - 1); // this line takes quite a while to execute, which is the reason to coalesce the events
        } else {
            super.processEvent(e);
        }
    }

    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        if (existingEvent instanceof UpdateLogListEvent && newEvent instanceof UpdateLogListEvent) {
            ((UpdateLogListEvent) existingEvent).coalesce((UpdateLogListEvent) newEvent);
            return existingEvent;
        } else {
            return super.coalesceEvents(existingEvent, newEvent);
        }
    }

    private static class UpdateLogListEvent extends AWTEvent {
        public static final int ID = RESERVED_ID_MAX + 1578;

        private ArrayList loggingEvents = new ArrayList();

        public UpdateLogListEvent(Object source, LoggingEvent loggingEvent) {
            super(source, ID);
            loggingEvents.add(loggingEvent);
        }

        public void coalesce(UpdateLogListEvent event) {
            loggingEvents.addAll(event.loggingEvents);
        }
    }

    public void clear() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                model.clear();
            }
        });
    }

    public void setColor(Level level, Color color) {
        levelToColorMap.put(level, color);
    }

    public Color getColor(Level level) {
        return (Color) levelToColorMap.get(level);
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    /**
     * JLabel renderer that uses HTML to render multiline messages
     */
    private class JLabelRenderer extends JLabel implements ListCellRenderer {
        private DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        public JLabelRenderer() {
            setAlignmentY(Component.TOP_ALIGNMENT);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            LoggingEvent loggingEvent = (LoggingEvent) value;

            synchronized (getTreeLock()) {
                Color color = (Color) levelToColorMap.get(loggingEvent.level);
                if (color != null) {
                    setForeground(color);
                } else {
                    setForeground(list.getForeground());
                }

                Icon icon = (Icon) levelToIconMap.get(loggingEvent.level);
                setIcon(icon);

                setBackground(list.getBackground());

                if (loggingEvent.level.isGreaterOrEqual(Level.FATAL)) {
                    setFont(font.deriveFont(font.getStyle(), font.getSize() + 4));
                } else {
                    setFont(font);
                }

                String message = timeFormat.format(new Date(loggingEvent.timeStamp)) + " - " + loggingEvent.getMessage();
                if (message.indexOf('\n') != -1) {
                    // use HTML to render multiline text
                    if (!BasicHTML.isHTMLString(message)) {
                        message = "<html>" + message + "</html>";
                    }
                    message = message.replaceAll("\n", "<br>");
                }
                setText(message);
                return this;
            }
        }
    }
}
