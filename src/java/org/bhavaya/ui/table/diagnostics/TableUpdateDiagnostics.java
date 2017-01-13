package org.bhavaya.ui.table.diagnostics;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class to log diagnostics update counts for columns in tables
 * User: ga2mhan
 * Date: 23/09/11
 * Time: 09:51
 */
public class TableUpdateDiagnostics {
    private static TableUpdateDiagnostics instance = new TableUpdateDiagnostics();
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("hh:mm:ss");
        }
    };

    private boolean updateLogEnabled = false;
    private long maximumLogDurationMs = 120000;
    private long updateSessionStarted;
    private long updateSessionStopped;

    private Map<String, Map<String, ColumnUpdateCount>> columnUpdatesByViewName = new HashMap<String, Map<String, ColumnUpdateCount>>();

    public static TableUpdateDiagnostics getInstance() {
        return instance;
    }

    public boolean isUpdateLogEnabled() {
        return updateLogEnabled;
    }

    public void setUpdateLogEnabled(boolean updateLogEnabled) {
        if(this.updateLogEnabled != updateLogEnabled) {
            this.updateLogEnabled = updateLogEnabled;
            if(updateLogEnabled) {
                updateSessionStarted = System.currentTimeMillis();
                updateSessionStopped = -1;
                columnUpdatesByViewName.clear();
            } else {
                updateSessionStopped = System.currentTimeMillis();
            }
        }
    }

    public long getMaximumLogDurationMs() {
        return maximumLogDurationMs;
    }

    public void setMaximumLogDurationMs(long maximumLogDurationMs) {
        this.maximumLogDurationMs = maximumLogDurationMs;
    }

    public void addUpdate(String viewName, String column) {
        if(updateLogEnabled) {
            if(System.currentTimeMillis() - updateSessionStarted > maximumLogDurationMs) {
                setUpdateLogEnabled(false);
            } else {
                Map<String, ColumnUpdateCount> columnUpdateMap = columnUpdatesByViewName.get(viewName);
                if(columnUpdateMap == null) {
                    columnUpdateMap = new HashMap<String, ColumnUpdateCount>();
                    columnUpdatesByViewName.put(viewName, columnUpdateMap);
                }
                ColumnUpdateCount count = columnUpdateMap.get(column);
                if(count == null) {
                    count = new ColumnUpdateCount(viewName, column);
                    columnUpdateMap.put(column, count);
                }
                count.incrementCount();
            }
        }
    }

    public String getUpdateDetails(String viewName) {
        Date started = new Date(updateSessionStarted);
        Date stopped = updateSessionStopped > updateSessionStarted ? new Date(updateSessionStopped) : new Date();
        long duration = (stopped.getTime() - started.getTime())/1000;
        if(viewName != null && viewName.trim().length() != 0) {
            return getUpdateDetailsForView(viewName.trim(), started, stopped, duration);
        }
        return getUpdateDetailsForAllViews(started, stopped, duration);
    }

    private String getUpdateDetailsForView(String viewName, Date started, Date stopped, long duration) {
        TreeSet<ColumnUpdateCount> columnUpdateCounts = new TreeSet<ColumnUpdateCount>(new ColumnUpdateCountComparator());
        Map<String, ColumnUpdateCount> columnUpdates = columnUpdatesByViewName.get(viewName);
        if(columnUpdates != null) {
            columnUpdateCounts.addAll(columnUpdates.values());
        }
        StringBuilder sb = new StringBuilder();
        if(columnUpdateCounts.isEmpty()) {
            sb.append("No table updates have been recorded for view \"").append(viewName).append("\".");
        } else {
            sb.append("Updates to view \"").append(viewName).append("\"");
            sb.append(" between ").append(DATE_FORMAT.get().format(started)).append(" and ").append(DATE_FORMAT.get().format(stopped));
            sb.append("<table border=1>");
            sb.append("<tr><th>Column</th><th>Update Count</th><th>Average Updates per Second</th></tr>");
            for(ColumnUpdateCount columnUpdateCount : columnUpdateCounts) {
                sb.append("<tr><td>").append(columnUpdateCount.columnPath).append("</td><td>").append(columnUpdateCount.updates).append("</td><td>").append(columnUpdateCount.updates/duration).append("</td></tr>");
            }
            sb.append("</table>");

        }
        return sb.toString();
    }

    private String getUpdateDetailsForAllViews(Date started, Date stopped, long duration) {
        TreeSet<ColumnUpdateCount> columnUpdateCounts = new TreeSet<ColumnUpdateCount>(new ColumnUpdateCountComparator());
        for(Map<String, ColumnUpdateCount> columnUpdates : columnUpdatesByViewName.values()) {
            columnUpdateCounts.addAll(columnUpdates.values());
        }
        StringBuilder sb = new StringBuilder();
        if(columnUpdateCounts.isEmpty()) {
            sb.append("No table updates have been recorded.");
        } else {
            sb.append("Updates to views between ").append(DATE_FORMAT.get().format(started)).append(" and ").append(DATE_FORMAT.get().format(stopped));
            sb.append("<table border=1>");
            sb.append("<tr><th>View</th><th>Column</th><th>Update Count</th><th>Average Updates per Second</th></tr>");
            for(ColumnUpdateCount columnUpdateCount : columnUpdateCounts) {
                sb.append("<tr><td>").append(columnUpdateCount.viewName).append("</td><td>").append(columnUpdateCount.columnPath).append("</td><td>").append(columnUpdateCount.updates).append("</td><td>").append(columnUpdateCount.updates/duration).append("</td></tr>");
            }
            sb.append("</table>");

        }
        return sb.toString();
    }


    private class ColumnUpdateCount {
        String viewName;
        String columnPath;
        volatile int updates = 0;

        ColumnUpdateCount(String viewName, String columnPath) {
            this.viewName = viewName;
            this.columnPath = columnPath;
        }

        void incrementCount() {
            updates++;
        }
    }

    private class ColumnUpdateCountComparator implements Comparator<ColumnUpdateCount> {
        public int compare(ColumnUpdateCount o1, ColumnUpdateCount o2) {
            int result = o2.updates - o1.updates;
            if(result == 0) {
                result = o1.viewName.compareTo(o2.viewName);
            }
            return result != 0 ? result : o1.columnPath.compareTo(o2.columnPath);
        }
    };

}
