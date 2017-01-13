package org.bhavaya.ui.diagnostics;

import org.apache.log4j.PatternLayout;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A non-graphical log reporting context.  Creates a table of exceptions, provides a source for the log files.
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */
public class LogControlDiagnosticContext extends DiagnosticContext {
    private static final Log log = Log.getCategory(LogControlDiagnosticContext.class);

    private static final int MAX_EXCEPTION_STRINGS_IN_REPORT = 25;


    public LogControlDiagnosticContext() {
        super("Log Control", null);
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        ApplicationDiagnostics.LoggingEventBean[] loggingEventBeans;
        List listModel = HeadlessApplicationDiagnostics.getHeadlessInstance().getExceptionLogStatements();
        synchronized (listModel) {
            loggingEventBeans = (ApplicationDiagnostics.LoggingEventBean[]) listModel.toArray(new ApplicationDiagnostics.LoggingEventBean[listModel.size()]);
        }

        StringBuffer buffer = new StringBuffer("<b><font face=arial>Exceptions");
        if (loggingEventBeans.length > MAX_EXCEPTION_STRINGS_IN_REPORT) buffer.append(" (Last ").append(MAX_EXCEPTION_STRINGS_IN_REPORT).append(")");
        buffer.append("</font></b><br>");

        if (loggingEventBeans.length == 0) {
            buffer.append("<i><font face=arial>None</font></i>");
        } else {
            buffer.append("<table border=1>");

            PatternLayout patternLayout = new PatternLayout("%d{ISO8601} - %-5p - %-20t - %-23.23c{1} - %m%n");
            int endIndex = (loggingEventBeans.length > MAX_EXCEPTION_STRINGS_IN_REPORT) ? MAX_EXCEPTION_STRINGS_IN_REPORT : loggingEventBeans.length;
            for (int i = 0; i < endIndex; i++) {
                buffer.append("<tr><td><font face=arial>");

                buffer.append(patternLayout.format(loggingEventBeans[i].getLoggingEvent()));
                buffer.append("<br>");

                String[] exceptionStack = loggingEventBeans[i].getThrowableStrings();
                for (int j = 0; j < exceptionStack.length; j++) {
                    if (j > 0) buffer.append("<br>....");
                    buffer.append(exceptionStack[j]);
                }

                buffer.append("</font></td></tr>");
            }

            buffer.append("</table>");
        }
        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        JCheckBoxMenuItem toggleDebuggingCheckBox = new JCheckBoxMenuItem("Debug Logging");
        toggleDebuggingCheckBox.setModel(new JToggleButton.ToggleButtonModel() {
            public boolean isSelected() {
                return ApplicationDiagnostics.getInstance().isDebugLoggingEnabled();
            }

            public void setSelected(boolean b) {
                if (b == isSelected()) return;
                super.setSelected(b);
                ApplicationDiagnostics.getInstance().setDebugLoggingEnabled(b);
            }

        });

        JCheckBoxMenuItem beanToScreenDelaygCheckBox = new JCheckBoxMenuItem("Bean To Screen Delay Logging");
        beanToScreenDelaygCheckBox.setModel(new JToggleButton.ToggleButtonModel() {
            public boolean isSelected() {
                return ApplicationDiagnostics.getInstance().isLogBeanToScreenDelays();
            }

            public void setSelected(boolean b) {
                if (b == isSelected()) return;
                super.setSelected(b);
                ApplicationDiagnostics.getInstance().setLogBeanToScreenDelays(b);
            }

        });

        MenuGroup menuGroup = new MenuGroup("Settings", KeyEvent.VK_S);
        menuGroup.addElement(new MenuGroup.MenuItemElement(toggleDebuggingCheckBox));
        menuGroup.addElement(new MenuGroup.MenuItemElement(beanToScreenDelaygCheckBox));
        menuGroup.addElement(new MenuGroup.MenuItemElement(new JCheckBoxMenuItem(new ToggleFocusLoggingAction())));
        return new MenuGroup[]{menuGroup};
    }

    private String getLog(List logStatements) {
        ApplicationDiagnostics.LoggingEventBean[] loggingStatements;
        synchronized (logStatements) {
            loggingStatements = (ApplicationDiagnostics.LoggingEventBean[]) logStatements.toArray(new ApplicationDiagnostics.LoggingEventBean[logStatements.size()]);
        }

        PatternLayout patternLayout = new PatternLayout("%d{ISO8601} - %-5p - %-20t - %-23.23c{1} - %m%n");
        StringBuffer buffer = new StringBuffer(loggingStatements.length * 100);
        for (int i = loggingStatements.length - 1; i >= 0; i--) {
            appendLogEventBean(loggingStatements[i], buffer, patternLayout);
        }
        return buffer.toString();
    }

    private void appendLogEventBean(ApplicationDiagnostics.LoggingEventBean loggingStatement, StringBuffer buffer, PatternLayout patternLayout) {
        if (loggingStatement != null) {
            buffer.append(patternLayout.format(loggingStatement.getLoggingEvent()));
            if (loggingStatement.getThrowableStrings() != null) {
                String[] exceptionStack = loggingStatement.getThrowableStrings();

                for (int j = 0; j < exceptionStack.length; j++) {
                    if (j > 0) buffer.append("   ");
                    buffer.append(exceptionStack[j]);
                    buffer.append("\n");
                }
            }
        }
    }

    public Attachment[] createAttachments() {
        List allLogList = ApplicationDiagnostics.getInstance().getAllLogStatements();
        List exceptionsList = ApplicationDiagnostics.getInstance().getExceptionLogStatements();

        List attachmentList = new ArrayList(2);
        attachmentList.add(new Attachment("logfile.txt", getLog(allLogList).getBytes()));
        if (exceptionsList.size() > MAX_EXCEPTION_STRINGS_IN_REPORT) {
            attachmentList.add(new Attachment("exceptions.txt", getLog(exceptionsList).getBytes()));
        }

        return (Attachment[]) attachmentList.toArray(new Attachment[attachmentList.size()]);
    }

    private static class ToggleFocusLoggingAction extends AuditedAbstractAction {
        private AWTEventListener focusListener;

        public ToggleFocusLoggingAction() {
            focusListener = new FocusEventListener();
            putValue(Action.NAME, "Focus Logging");
        }

        public void auditedActionPerformed(ActionEvent e) {
            JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
            if (checkBoxMenuItem.isSelected()) {
                Toolkit.getDefaultToolkit().addAWTEventListener(focusListener, AWTEvent.FOCUS_EVENT_MASK);
            } else {
                Toolkit.getDefaultToolkit().removeAWTEventListener(focusListener);
            }
        }
    }

    private static class FocusEventListener implements AWTEventListener {
        public void eventDispatched(AWTEvent event) {
            FocusEvent focusEvent = (FocusEvent) event;
            boolean focusLost = focusEvent.getID() == FocusEvent.FOCUS_LOST;
            if (log.isDebug()) {
                log.debug("Focus " + (focusLost ? "lost" : "gained")
                        + " event: " + render(focusEvent.getComponent())
                        + (focusLost ? " -> " : " <- ")
                        + render(focusEvent.getOppositeComponent()));
            }
        }

        private String render(Component component) {
            return component == null ? "null" : component.getClass().getName();
        }
    }
}
