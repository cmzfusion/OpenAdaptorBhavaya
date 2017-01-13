package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.Log;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Substitute EventQueue that can detect when the EDT is blocked or just running an event that is taking too long.
 * Can provide all relevant stack traces in the event of degraded performance.
 *
 * @author Brendon McLean
 */
public class EventQueueWatchLogger implements EventQueueWatchdog.Listener {
    private static final Log log = Log.getCategory(EventQueueWatchLogger.class);

    private static Map<String, String> LOG_FORMAT_STRINGS = new HashMap<String, String>();
    private static Map<String, String> HTML_FORMAT_STRINGS = new HashMap<String, String>();

    protected static final String EVENT_RUNNING_ON_EDT = "Event running on EDT for %.1fs\n";
    protected static final String EVENT = "Event: %s \"%s\"\n";
    protected static final String SOURCE = "Source: %s \"%s\"\n";
    protected static final String EDT_BLOCKED = "EDT blocked for %.1fs\n";

    static {
        LOG_FORMAT_STRINGS.put(EVENT_RUNNING_ON_EDT, EVENT_RUNNING_ON_EDT);
        LOG_FORMAT_STRINGS.put(EVENT, EVENT);
        LOG_FORMAT_STRINGS.put(SOURCE, SOURCE);
        LOG_FORMAT_STRINGS.put(EDT_BLOCKED, EDT_BLOCKED);

        HTML_FORMAT_STRINGS.put(EVENT_RUNNING_ON_EDT, "<br><b>Event running on EDT for %.1fs</b><br><table border=1>");
        HTML_FORMAT_STRINGS.put(EVENT, "<tr><td>Event:</td><td><b>%s</b> \"<i>%s</i>\"</td></tr>");
        HTML_FORMAT_STRINGS.put(SOURCE, "<tr><td>Source: </td><td><b>%s</b> \"<i>%s</i>\"</td></tr></table>");
        HTML_FORMAT_STRINGS.put(EDT_BLOCKED, "<br><b>EDT blocked for %.1fs</b><br>");
    }

    public void eventLagging(AWTEvent e, long duration, long edtThreadId) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo edtInfo = threadMXBean.getThreadInfo(edtThreadId, Integer.MAX_VALUE);

        switch (edtInfo.getThreadState()) {
            case BLOCKED:
            case WAITING:
            case TIMED_WAITING: {
                log.warn(edtBlocked(edtInfo, duration, false));
                ApplicationDiagnostics.getInstance().sendDiagnosticReport(edtBlocked(edtInfo, duration, true).toString());
                break;
            }
            default: {
                log.warn(longRunningEvent(e, edtInfo, duration, false));
                ApplicationDiagnostics.getInstance().sendDiagnosticReport(longRunningEvent(e, edtInfo, duration, true).toString());
            }
        }
    }

    protected String getFormat(String key, boolean style) {
        Map<String, String> sourceMap = style ? HTML_FORMAT_STRINGS : LOG_FORMAT_STRINGS;
        return sourceMap.get(key);
    }

    private StringBuilder longRunningEvent(AWTEvent e, ThreadInfo edtInfo, long duration, boolean style) {
        StringBuilder story = new StringBuilder(String.format(getFormat(EVENT_RUNNING_ON_EDT, style), duration / 1000d));
        story.append(String.format(getFormat(EVENT, style), e.getClass().getName(), e.toString()));
        story.append(String.format(getFormat(SOURCE, style), e.getSource().getClass().getName(), e.getSource().toString()));
        story.append(ThreadDiagnosticContext.createThreadStory(edtInfo, style));

        return story;
    }

    private StringBuilder edtBlocked(ThreadInfo edtInfo, long duration, boolean style) {
        StringBuilder story = new StringBuilder(String.format(getFormat(EDT_BLOCKED, style), duration / 1000d));

        ThreadInfo threadInfo = edtInfo;
        do {
            story.append(ThreadDiagnosticContext.createThreadStory(threadInfo, style));

            long lockOwnerId = threadInfo.getLockOwnerId();
            if (lockOwnerId == -1) break;
            threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(lockOwnerId, Integer.MAX_VALUE);
        } while (true);

        return story;
    }
}
