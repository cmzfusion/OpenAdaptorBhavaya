package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.Log;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;

/**
 * General thread health monitor.  Can automatically detect, log and alert deadlock conditions.  Can also provide
 * ThreadDumps without using the hazardous ThreadDump class.
 *
 * @author Brendon McLean
 */
public class ThreadDiagnosticContext extends MBeanOnlyDiagnosticContext implements ThreadDiagnosticContextMBean {
    private static final Log log = Log.getCategory(ThreadDiagnosticContext.class);

    private static ThreadDiagnosticContext instance;
    private static final int PERIOD = 10000;

    private static final String MESSAGE = "%s\n";
    private static final String WAITING_FOR_LOCK = "\n'%s' waiting for lock '%s', held by '%s'";
    private static final String NOT_WAITING_FOR_LOCK = "\n'%s' not waiting for any locks";
    private static final String LINE_1 = "\n   %s.%s()";
    private static final String LINE_2 = " <native>";
    private static final String LINE_3 = " - (%s:%d)";
    private static final String NEWLINE = "\n";

    private static Map<String, String> LOG_FORMAT_STRINGS = new HashMap<String, String>();
    private static Map<String, String> HTML_FORMAT_STRINGS = new HashMap<String, String>();

    static {
        LOG_FORMAT_STRINGS.put(MESSAGE, MESSAGE);
        LOG_FORMAT_STRINGS.put(WAITING_FOR_LOCK, WAITING_FOR_LOCK);
        LOG_FORMAT_STRINGS.put(NOT_WAITING_FOR_LOCK, NOT_WAITING_FOR_LOCK);
        LOG_FORMAT_STRINGS.put(LINE_1, LINE_1);
        LOG_FORMAT_STRINGS.put(LINE_2, LINE_2);
        LOG_FORMAT_STRINGS.put(LINE_3, LINE_3);
        LOG_FORMAT_STRINGS.put(NEWLINE, NEWLINE);

        HTML_FORMAT_STRINGS.put(MESSAGE, "<br><b>%s</b><br>");
        HTML_FORMAT_STRINGS.put(WAITING_FOR_LOCK, "<br>'<b>%s</b>' waiting for lock '<b>%s</b>', held by '<b>%s</b>'<br>");
        HTML_FORMAT_STRINGS.put(NOT_WAITING_FOR_LOCK, "<br>'<b>%s</b>' not waiting for any locks<br>");
        HTML_FORMAT_STRINGS.put(LINE_1, "<br>...%s.%s()");
        HTML_FORMAT_STRINGS.put(LINE_2, " <native>");
        HTML_FORMAT_STRINGS.put(LINE_3, " - (<font color=\"blue\">%s:%d</font>)");
        HTML_FORMAT_STRINGS.put(NEWLINE, "<br>");
    }

    private Timer timer;
    private boolean sendDiagnosticOnError = false;


    public static synchronized ThreadDiagnosticContext getInstance() {
        if (instance == null) {
            instance = new ThreadDiagnosticContext();
        }
        return instance;
    }

    private ThreadDiagnosticContext() {
        super("Threads", null);
        timer = new Timer("ThreadWatchDog");
        timer.schedule(new TimerTask() {
            public void run() {
                pollForDeadlocks();
            }
        }, PERIOD, PERIOD);
    }

    public void setSendDiagnosticOnError(boolean sendDiagnosticOnError) {
        this.sendDiagnosticOnError = sendDiagnosticOnError;
    }

    public Object createMBean() {
        return this;
    }

    public void performThreadDump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadMXBean.getAllThreadIds();
        logThreadIds("Thread Dump", threadIds);
    }

    private void pollForDeadlocks() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadMXBean.findMonitorDeadlockedThreads();

        if (deadlockedThreadIds != null) {
            logThreadIds("Deadlock detected", deadlockedThreadIds);
            timer.cancel();
        }
    }

    private void logThreadIds(String message, long[] threadIds) {
        log.warn(createStory(message, threadIds, false));
        if (sendDiagnosticOnError) {
            ApplicationDiagnostics.getHeadlessInstance().sendDiagnosticReport(createStory(message, threadIds, true));
        }
    }

    private String createStory(String message, long[] threadIds, boolean style) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        StringBuilder story = new StringBuilder(String.format(getFormat(MESSAGE, style), message));
        for (long threadId : threadIds) {
            story.append(createThreadStory(threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE), style));
        }

        return story.toString();
    }

    protected static String getFormat(String key, boolean style) {
        Map<String, String> sourceMap = style ? HTML_FORMAT_STRINGS : LOG_FORMAT_STRINGS;
        return sourceMap.get(key);
    }

    public static String createThreadStory(ThreadInfo edtInfo, boolean style) {
        StringBuilder story = new StringBuilder();

        if (edtInfo.getLockOwnerId() == -1) {
            story.append(String.format(getFormat(NOT_WAITING_FOR_LOCK, style), edtInfo.getThreadName()));
        } else {
            story.append(String.format(getFormat(WAITING_FOR_LOCK, style), edtInfo.getThreadName(), edtInfo.getLockName(),
                    edtInfo.getLockOwnerName()));
        }

        story.append(formatStackTrace(edtInfo.getStackTrace(), style));

        return story.toString();
    }

    private static Object formatStackTrace(StackTraceElement[] stackTrace, boolean style) {
        if (stackTrace.length < 1) return "";
        StringBuilder story = new StringBuilder(formatElement(stackTrace[0], style));
        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement stackTraceElement = stackTrace[i];
            story.append(formatElement(stackTraceElement, style));
        }
        return story.append(getFormat(NEWLINE, style));
    }

    private static String formatElement(StackTraceElement stackTraceElement, boolean style) {
        StringBuilder story = new StringBuilder(String.format(getFormat(LINE_1, style), stackTraceElement.getClassName(), stackTraceElement.getMethodName()));
        if (stackTraceElement.isNativeMethod()) {
            story.append(getFormat(LINE_2, style));
        } else {
            story.append(String.format(getFormat(LINE_3, style), stackTraceElement.getFileName(), stackTraceElement.getLineNumber()));
        }

        return story.toString();
    }
}
