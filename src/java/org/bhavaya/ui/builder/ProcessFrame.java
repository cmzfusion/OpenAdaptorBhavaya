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

package org.bhavaya.ui.builder;

import org.bhavaya.ui.ArrayListModel;
import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.util.IOUtilities;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class ProcessFrame extends JFrame {
    private static final Log log = Log.getCategory(ProcessFrame.class);
    private Process process;

    public ProcessFrame(Process process, String name, Container owner, Rectangle bounds) throws HeadlessException {
        super(name);
        this.process = process;

        Font font = new Font("Courier New", Font.PLAIN, 12);
        LogPanel logPanel = new LogPanel(1000, font);
        setContentPane(logPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        menu.add(new JMenuItem(new ClearLogPanelAction(logPanel)));
        menu.addSeparator();
        menu.add(new JMenuItem(new CloseAction(this, "Exit " + name)));
        menuBar.add(menu);
        setJMenuBar(menuBar);

        addStreamToLogPanel(logPanel, process.getErrorStream(), name + "-serr", Level.SEVERE);
        addStreamToLogPanel(logPanel, process.getInputStream(), name + "-sout", Level.INFO);
        if (bounds == null) {
            Rectangle ownerBounds = owner.getBounds();
            bounds = new Rectangle((int) ownerBounds.getX(), (int) ownerBounds.getY(), (int) (ownerBounds.getWidth() * 0.9), (int) (ownerBounds.getHeight() * 0.9));
            setBounds(bounds);
            UIUtilities.centreInContainer(owner, this, 10, 10);
        } else {
            setBounds(bounds);
            if (!UIUtilities.getDefaultScreenSizeWithoutAdjustment().contains(bounds)) UIUtilities.centreInContainer(owner, this, 10, 10);
        }
    }

    public void dispose() {
        try {
            process.destroy();
        } catch (Throwable e) {
            log.error(e);
        }
        super.dispose();
    }

    private static void addStreamToLogPanel(final LogPanel logPanel, final InputStream stream, final String threadName, final Level level) {
        Runnable readerRunnable = new Runnable() {
            public void run() {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(stream));
                    String line = reader.readLine();
                    while (line != null) {
                        logPanel.logMessage(new LogMessage(level, line));
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    log.info(threadName + " terminating");
                    IOUtilities.closeReader(reader);
                }
            }
        };
        Thread thread = new Thread(readerRunnable, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private static class LogPanel extends JPanel {
        private ArrayListModel model;
        private JList view;
        private int maxNoOfEvents;
        private java.util.List messagesToLog;

        public LogPanel(int maxNoOfEvents, Font font) {
            super(new BorderLayout());

            this.maxNoOfEvents = maxNoOfEvents;
            model = new ArrayListModel();
            view = new JList(model);
            view.setFont(font);
            view.setCellRenderer(new LogPanel.Renderer());
            add(new JScrollPane(view));

            messagesToLog = new ArrayList();
            Timer timer = new Timer(500, new LoggingActionListener(messagesToLog, model, view, maxNoOfEvents));
            timer.start();
        }

        public void logMessage(final LogMessage message) {
            synchronized (messagesToLog) {
                clip(messagesToLog, maxNoOfEvents);
                messagesToLog.add(message);
            }
        }

        private static void clip(java.util.List list, int maxNoOfEvents) {
            if (list.size() >= maxNoOfEvents) {
                java.util.List tail = new ArrayList(list.subList(maxNoOfEvents / 2, list.size() - 1));
                list.clear();
                list.addAll(tail);
            }
        }

        public void clear() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    model.clear();
                }
            });
        }

        private class Renderer extends JTextArea implements ListCellRenderer {
            public Renderer() {
                setLineWrap(false);
                setAlignmentY(Component.TOP_ALIGNMENT);
                setAlignmentX(Component.LEFT_ALIGNMENT);
                setOpaque(true);
            }

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                LogMessage logMessage = (LogMessage) value;

                if (logMessage.getLevel().equals(Level.SEVERE)) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLUE);
                }

                setFont(list.getFont());
                setText(logMessage.getMessage());
                return this;
            }
        }

        private static class LoggingActionListener implements ActionListener {
            private java.util.List messagesToLog;
            private ArrayListModel model;
            private int maxNoOfEvents;
            private JList view;

            public LoggingActionListener(java.util.List messagesToLog, ArrayListModel model, JList view, int maxNoOfEvents) {
                this.messagesToLog = messagesToLog;
                this.model = model;
                this.view = view;
                this.maxNoOfEvents = maxNoOfEvents;
            }

            public void actionPerformed(ActionEvent e) {
                LogMessage[] logMessages = null;

                synchronized (messagesToLog) {
                    if (messagesToLog.size() > 0) {
                        logMessages = (LogMessage[]) messagesToLog.toArray(new LogMessage[messagesToLog.size()]);
                        messagesToLog.clear();
                    }
                }

                if (logMessages != null) {
                    for (int i = 0; i < logMessages.length; i++) {
                        LogMessage logMessage = logMessages[i];
                        model.add(logMessage);
                    }

                    clip(model, maxNoOfEvents);
                    view.ensureIndexIsVisible(model.size() - 1);
                }
            }
        }
    }

    private static class LogMessage {
        private Level level;
        private String message;

        public LogMessage(Level level, String message) {
            this.level = level;
            this.message = message;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class ClearLogPanelAction extends AbstractAction {
        private final LogPanel logPanel;

        public ClearLogPanelAction(LogPanel logPanel) {
            super("Clear");
            this.logPanel = logPanel;
        }

        public void actionPerformed(ActionEvent e) {
            logPanel.clear();
        }
    }
}
