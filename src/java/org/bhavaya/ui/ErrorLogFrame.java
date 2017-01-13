package org.bhavaya.ui;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * JFrame extension for displaying log messages logged to the user category.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class ErrorLogFrame extends JFrame implements Log.Listener {

    private LogPanel errorLogPanel;
    private Level logLevel = Level.ERROR;
    private Level popupLogLevel = Level.ERROR;

    public ErrorLogFrame() {
        this("Errors");
    }

    public ErrorLogFrame(String title) {
        super(title);
        initialize();
    }

    public void setVisible(boolean b) {
        setFocusableWindowState(false); // don't let this thing to grab the focus
        super.setVisible(b);
        if (b) toFront();
        setFocusableWindowState(true);
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        errorLogPanel = new LogPanel();

        Dimension screenSize = UIUtilities.getDefaultScreenSize();
        Dimension preferredSize = new Dimension((int) (screenSize.getWidth() * 0.8), (int) (screenSize.getHeight() * 0.25));
        errorLogPanel.setPreferredSize(preferredSize);

        getContentPane().add(errorLogPanel, BorderLayout.CENTER);

        AbstractAction hideAction = new AuditedAbstractAction("Hide", "Hide Error Log") {
            public void auditedActionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        AbstractAction clearAction = new AuditedAbstractAction("Clear", "Clear Error Log") {
            public void auditedActionPerformed(ActionEvent e) {
                errorLogPanel.clear();
                setVisible(false);
            }
        };

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        toolBar.add(new JButton(clearAction));
        toolBar.add(new JButton(hideAction));
        getContentPane().add(toolBar, BorderLayout.SOUTH);

        pack();
        UIUtilities.centreInScreen(this, 0, 0);
        Log.getUserCategory().addListener(this, Level.INFO); // subscribe to all events and filter them afterwards in logMessage method
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setPopupLevel(Level popupLogLevel) {
        this.popupLogLevel = popupLogLevel;
    }

    public Level getPopupLogLevel() {
        return popupLogLevel;
    }

    public void setLogPanelFont(Font font) {
        errorLogPanel.setFont(font);
    }

    public Font getLogPanelFont() {
        return errorLogPanel.getFont();
    }

    public void logMessage(LoggingEvent loggingEvent) {
        if (loggingEvent.level.isGreaterOrEqual(logLevel)) {
            errorLogPanel.logMessage(loggingEvent);
            if (loggingEvent.level.isGreaterOrEqual(popupLogLevel)) {
                UIUtilities.runInDispatchThread(new Runnable() {
                    public void run() {
                        setVisible(true);
                    }
                });
            }
        }
    }

    public static void main(String[] args) {
        ErrorLogFrame frame = new ErrorLogFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLogLevel(Level.INFO);
        frame.setVisible(true);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                int keyCode = e.getKeyCode();
                Log userLog = Log.getUserCategory();
                switch (keyCode) {
                    case KeyEvent.VK_1:
                        userLog.info("Multiline\nInfo");
                        break;
                    case KeyEvent.VK_2:
                        userLog.warn("Long multiline warning\nz zz zzzzzz z zz zzzzzz zzzz\nzzzzz zzzzzz zz zzzzzz zzzz zzzzz");
                        break;
                    case KeyEvent.VK_3:
                        userLog.error("Error");
                        break;
                    case KeyEvent.VK_4:
                        userLog.fatal("Fatal");
                        break;
                }
                return false;
            }
        });

    }
}
