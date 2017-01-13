package org.bhavaya.javafxui;

import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.ui.diagnostics.ThreadDiagnosticContext;
import org.bhavaya.util.Log;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Logger implementation of JavaFxEventQueueWatchdog.Listener
 * User: Jon Moore
 * Date: 29/11/13
 * Time: 15:07
 */
public class JavaFxEventQueueWatchLogger implements JavaFxEventQueueWatchdog.Listener {
    private static final Log log = Log.getCategory(JavaFxEventQueueWatchLogger.class);

    private static Map<String, String> LOG_FORMAT_STRINGS = new HashMap<>();
    private static Map<String, String> HTML_FORMAT_STRINGS = new HashMap<>();

    protected static final String EVENT_RUNNING_ON_FAT = "Event running on FX application thread for %.1fs\n";
    protected static final String FAT_BLOCKED = "FX application thread blocked for %.1fs\n";

    static {
        LOG_FORMAT_STRINGS.put(EVENT_RUNNING_ON_FAT, EVENT_RUNNING_ON_FAT);
        LOG_FORMAT_STRINGS.put(FAT_BLOCKED, FAT_BLOCKED);

        HTML_FORMAT_STRINGS.put(EVENT_RUNNING_ON_FAT, "<br><b>Event running on FX application thread for %.1fs</b><br>");
        HTML_FORMAT_STRINGS.put(FAT_BLOCKED, "<br><b>FX application thread blocked for %.1fs</b><br>");
    }

    @Override
    public void eventLagging(long duration, long fatThreadId) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(fatThreadId, Integer.MAX_VALUE);

        switch (threadInfo.getThreadState()) {
            case BLOCKED:
            case WAITING:
            case TIMED_WAITING: {
                log.warn(fxAppThreadBlocked(threadInfo, duration, false));
                ApplicationDiagnostics.getInstance().sendDiagnosticReport(fxAppThreadBlocked(threadInfo, duration, true).toString());
                break;
            }
            default: {
                log.warn(longRunningEvent(threadInfo, duration, false));
                ApplicationDiagnostics.getInstance().sendDiagnosticReport(longRunningEvent(threadInfo, duration, true).toString());
            }
        }
    }

    protected String getFormat(String key, boolean style) {
        Map<String, String> sourceMap = style ? HTML_FORMAT_STRINGS : LOG_FORMAT_STRINGS;
        return sourceMap.get(key);
    }

    private StringBuilder longRunningEvent(ThreadInfo edtInfo, long duration, boolean style) {
        StringBuilder story = new StringBuilder(String.format(getFormat(EVENT_RUNNING_ON_FAT, style), duration / 1000d));
        story.append(ThreadDiagnosticContext.createThreadStory(edtInfo, style));
        return story;
    }

    private StringBuilder fxAppThreadBlocked(ThreadInfo fxAppThreadInfo, long duration, boolean style) {
        StringBuilder story = new StringBuilder(String.format(getFormat(FAT_BLOCKED, style), duration / 1000d));

        ThreadInfo threadInfo = fxAppThreadInfo;
        do {
            story.append(ThreadDiagnosticContext.createThreadStory(threadInfo, style));

            long lockOwnerId = threadInfo.getLockOwnerId();
            if (lockOwnerId == -1) break;
            threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(lockOwnerId, Integer.MAX_VALUE);
        } while (true);

        return story;
    }

}
