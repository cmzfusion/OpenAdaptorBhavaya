package org.bhavaya.ui.diagnostics;

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Utilities;

import java.awt.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Feb 21, 2006
 * Time: 12:34:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Profiler {
    private static final Log log = Log.getCategory(Profiler.class);

    public static final String STARTUP = "Startup";
    public static final String SQL = "Sql";
    public static final String SQL_EXECUTE = "Sql_Execute";
    public static final String SQL_INFLATE = "Sql_Inflate";
    public static final String MAJOR_METHOD = "Method";
    public static final String MINOR_METHOD = "Minor_Method";

    public interface Listener {
        void warning(Task[] stack, long endTime);

        void info(Task task, long endTime);
    }

    public static class TaskType {
        private String name;
        private long maximumTaskCompletionTime;
        private boolean reportAllDurations;
        private boolean recordMetrics;

        private TaskType(String name, long maximumTaskCompletionTime, boolean reportAllDurations, boolean recordMetrics) {
            this.name = name;
            this.maximumTaskCompletionTime = maximumTaskCompletionTime;
            this.reportAllDurations = reportAllDurations;
            this.recordMetrics = recordMetrics;
        }

        public String getName() {
            return name;
        }

        public long getMaximumTaskCompletionTime() {
            return maximumTaskCompletionTime;
        }

        public boolean isReportAllDurations() {
            return reportAllDurations;
        }

        public boolean isRecordMetrics() {
            return recordMetrics;
        }
    }

    private static class Metrics {
        private long minTaskTime;
        private long maxTaskTime;
        private TreeSet<Task> longestTasks = new TreeSet<Task>();
        private int count;
        private double averageTaskTime;

        public double getAverageTaskTime() {
            return averageTaskTime;
        }

        public void setAverageTaskTime(double averageTaskTime) {
            this.averageTaskTime = averageTaskTime;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void checkIfLongestTask(Task task) {
            longestTasks.add(task);
            if (longestTasks.size() > 5) {
                longestTasks.remove(longestTasks.last());
            }
        }

        public long getMaxTaskTime() {
            return maxTaskTime;
        }

        public void setMaxTaskTime(long maxTaskTime) {
            this.maxTaskTime = maxTaskTime;
        }

        public long getMinTaskTime() {
            return minTaskTime;
        }

        public void setMinTaskTime(long minTaskTime) {
            this.minTaskTime = minTaskTime;
        }
    }

    public static class Task implements Comparable {
        private TaskType type;
        private long startTime;
        private String detail;
        private long maximumCompletionTime;
        public long duration;

        private Task(TaskType type, long startTime, String detail, long maximumTaskCompletionTime) {
            this.type = type;
            this.startTime = startTime;
            this.detail = detail;
            this.maximumCompletionTime = maximumTaskCompletionTime;
        }

        public TaskType getType() {
            return type;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getDetail() {
            return detail;
        }

        public long getDuration() {
            return duration;
        }

        public long getMaximumCompletionTime() {
            return maximumCompletionTime;
        }

        public int compareTo(Object o) {
            Task task = (Task) o;
            return duration > task.duration
                    ? -1
                    : duration == task.duration ? 0 : 1;
        }
    }


    private static Map<Thread, LinkedList<Task>> threadTaskStack = new HashMap<Thread, LinkedList<Task>>();
    private static Map<TaskType, Metrics> typeMetrics = new HashMap<TaskType, Metrics>();
    private static HashSet<Task> metaTaskSet = new HashSet<Task>();
    private static HashMap<String, TaskType> taskTypes;


    private static final ArrayList<Listener> listenerList = new ArrayList<Listener>();

    public static void addListener(Listener listener) {
        synchronized (listenerList) {
            listenerList.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        synchronized (listenerList) {
            listenerList.remove(listener);
        }
    }

    public static void addDefaultLogListener() {
        addListener(new DefaultLogListener());
    }

    public static Task taskStarted(String taskType, String taskDetail) {
        return taskStarted(taskType, taskDetail, -1);
    }

    public static Task taskStarted(String taskType, String taskDetail, long maximumTaskTime) {
        long startTime = System.currentTimeMillis();
        synchronized (Profiler.class) {
            Task task = new Task(getTaskType(taskType), startTime, taskDetail, maximumTaskTime);
            getTaskStack().addFirst(task);
            return task;
        }
    }

    public static Task metaTaskStarted(String taskType) {
        return metaTaskStarted(taskType, null);
    }

    public static Task metaTaskStarted(String taskType, String taskDetail) {
        return metaTaskStarted(taskType, taskDetail, -1);
    }

    public static Task metaTaskStarted(String taskType, String taskDetail, long maximumTaskTime) {
        long startTime = System.currentTimeMillis();
        synchronized (Profiler.class) {
            Task task = new Task(getTaskType(taskType), startTime, taskDetail, maximumTaskTime);
            metaTaskSet.add(task);
            return task;
        }
    }

    public static void metaTaskStopped(Task task) {
        long endTime = System.currentTimeMillis();
        synchronized (Profiler.class) {
            boolean taskRemoved = metaTaskSet.remove(task);

            assert taskRemoved;

            task.duration = endTime - task.startTime;
            TaskType taskType = task.type;

            if (task.maximumCompletionTime != -1 && (task.duration > task.maximumCompletionTime)) {
                fireWarning(new Task[]{task}, endTime);
            } else if (task.duration > taskType.maximumTaskCompletionTime) {
                fireWarning(new Task[]{task}, endTime);
            } else {
                fireInfo(task, endTime);
            }

            if (taskType.recordMetrics) {
                recordMetrics(task);
            }
        }
    }

    public static void taskStopped(Task task) {
        if (task == null) return;

        long endTime = System.currentTimeMillis();
        synchronized (Profiler.class) {
            LinkedList<Task> taskStack = getTaskStack();

            Task removedTask = null;
            while (taskStack.size() > 0 && (removedTask = taskStack.removeFirst()) != task) {
                log.warn("Task \"" + task.detail + "\" not on top of stack.  Removing task \"" + removedTask.detail + "\"");
            }

            if (taskStack.size() == 0) {

                //otherwise we may hold onto Thread instances for threads which have terminated
                removeTaskStackForThread();

                if ( removedTask != task) {
                    log.warn("Task not found while emptying stack: " + task.detail);
                    return;
                }
            }

            task.duration = endTime - task.startTime;
            TaskType taskType = task.type;

            if (task.maximumCompletionTime != -1 && (task.duration > task.maximumCompletionTime)) {
                Task[] stack = taskStack.toArray(new Task[taskStack.size()]);
                Task[] mergedArray = Utilities.unionArrays(new Task[]{removedTask}, stack);
                fireWarning(mergedArray, endTime);
            } else if (task.duration > taskType.maximumTaskCompletionTime) {
                Task[] stack = taskStack.toArray(new Task[taskStack.size()]);
                Task[] mergedArray = Utilities.unionArrays(new Task[]{removedTask}, stack);
                fireWarning(mergedArray, endTime);
            } else {
                fireInfo(task, endTime);
            }

            if (taskType.recordMetrics) {
                recordMetrics(task);
            }
        }
    }

    public synchronized static TaskType getTaskType(String taskName) {
        if (taskTypes == null) {
            taskTypes = new HashMap<String, TaskType>();
            try {
                PropertyGroup[] taskTypeGroups = ApplicationProperties.getApplicationProperties().getGroups("profiler.taskType");
                for (PropertyGroup taskTypeGroup : taskTypeGroups) {
                    String name = taskTypeGroup.getProperty("name");
                    long maximumTaskCompletionTime = Long.valueOf(taskTypeGroup.getProperty("maximumTaskCompletionTime"));
                    boolean reportAllDurations = Boolean.valueOf(taskTypeGroup.getProperty("reportAllDurations"));
                    boolean recordMetrics = Boolean.valueOf(taskTypeGroup.getProperty("recordMetrics"));
                    TaskType taskType = new TaskType(name, maximumTaskCompletionTime, reportAllDurations, recordMetrics);
                    taskTypes.put(name, taskType);
                }
            } catch (Throwable t) {
                log.info("No profiler.taskType group in application properties");
            }
            addDefaultTypes(taskTypes);
        }

        TaskType taskType = taskTypes.get(taskName);
        if (taskType == null) throw new RuntimeException("Taskname \"" + taskName + "\") not defined in application " +
                "properties files under group taskProfiler.taskType");

        return taskType;
    }

    private static void addDefaultTypes(HashMap<String, TaskType> taskTypes) {
        HashMap<String, TaskType> defaults = new HashMap<String, TaskType>();
        defaults.put(STARTUP, new TaskType(STARTUP, Long.MAX_VALUE, true, true));
        defaults.put(SQL, new TaskType(SQL, 10 * 1000, false, true));
        defaults.put(SQL_EXECUTE, new TaskType(SQL_EXECUTE, Long.MAX_VALUE, false, false));
        defaults.put(SQL_INFLATE, new TaskType(SQL_INFLATE, Long.MAX_VALUE, false, false));
        defaults.put(MAJOR_METHOD, new TaskType(MAJOR_METHOD, Long.MAX_VALUE, true, false));
        defaults.put(MINOR_METHOD, new TaskType(MINOR_METHOD, Long.MAX_VALUE, false, false));

        for (Map.Entry<String, TaskType> entry : defaults.entrySet()) {
            if (!taskTypes.containsKey(entry.getKey())) {
                taskTypes.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void recordMetrics(Task task) {
        Metrics metrics = getMetricsForType(task.type);

        metrics.averageTaskTime = (metrics.averageTaskTime * metrics.count + task.duration) / (metrics.count + 1);
        metrics.count++;
        if (metrics.minTaskTime > task.duration) metrics.minTaskTime = task.duration;
        if (metrics.maxTaskTime < task.duration) metrics.maxTaskTime = task.duration;
        metrics.checkIfLongestTask(task);
    }

    private static Metrics getMetricsForType(TaskType type) {
        Metrics metrics = typeMetrics.get(type);
        if (metrics == null) {
            metrics = new Metrics();
            typeMetrics.put(type, metrics);
        }
        return metrics;
    }

    private static void fireWarning(Profiler.Task[] stack, long endTime) {
        for (Listener listener : listenerList) {
            listener.warning(stack, endTime);
        }
    }

    private static void fireInfo(Task task, long endTime) {
        for (Listener listener : listenerList) {
            listener.info(task, endTime);
        }
    }

    private static LinkedList<Task> getTaskStack() {
        LinkedList<Task> taskStack = threadTaskStack.get(Thread.currentThread());
        if (taskStack == null) {
            taskStack = new LinkedList<Task>();
            threadTaskStack.put(Thread.currentThread(), taskStack);
        }
        return taskStack;
    }

    private static void removeTaskStackForThread() {
        threadTaskStack.remove(Thread.currentThread());
    }

    private static class DefaultLogListener implements Listener {
        private Log log = Log.getCategory("PROFILER");

        public void warning(Task[] stack, long endTime) {
            printTaskStack("Task exceeded warning threshold of: " + stack[0].getType().getMaximumTaskCompletionTime() + "ms", stack, endTime);
        }

        public void info(Task task, long endTime) {
            if (task.getType().isReportAllDurations()) {
                long duration = endTime - task.startTime;
                String message = getSummaryMessage(task, duration);
                log.info(message);
            }
        }

        private void printTaskStack(String headerString, Task[] stack, long endTime) {
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append(headerString);
            for (Task task : stack) {
                long duration = endTime - task.startTime;
                buffer.append("\n....");
                buffer.append(getSummaryMessage(task, duration));
            }
            log.warn(buffer.toString());
        }
    }

    private static String getSummaryMessage(Task task, long duration) {
        return task.detail == null
                ? task.type.name + ": " + duration + "ms"
                : task.type.name + ": " + duration + "ms (" + task.detail + ")";
    }

    public static interface ProfilerMBeanAdaptorMBean {
        public String reportRunningTasks();

        public String reportTaskMetrics();
    }

    public static class ProfilerDiagnosticContext extends DiagnosticContext {
        public ProfilerDiagnosticContext() {
            super("Profiler", null);
        }

        public Component createComponent() {
            return null;
        }

        public String createHTMLDescription() {
            return createRunningTasksHTML() + "<br><br>" + createTaskMetricsHTML();
        }

        public MenuGroup[] createMenuGroups() {
            return new MenuGroup[0];
        }

        public Object createMBean() {
            return new ProfilerMBeanAdaptor();
        }

        public Attachment[] createAttachments() {
            return new Attachment[0];
        }
    }

    private static String createRunningTasksHTML() {
        StringBuffer fastStringBuffer = new StringBuffer();
        long endTime = System.currentTimeMillis();

        synchronized (Profiler.class) {
            DiagnosticUtilities.contextHeader(fastStringBuffer, "Meta tasks");

            if (metaTaskSet.size() == 0) {
                fastStringBuffer.append("<i>None</i>");
            } else {
                DiagnosticUtilities.tableHeader(fastStringBuffer);
                for (Task task : Profiler.metaTaskSet) {
                    long duration = endTime - task.startTime;
                    String name = task.type.name;
                    String detail = task.detail == null ? "<i>N/A</i>" : task.detail;
                    DiagnosticUtilities.tableRow(fastStringBuffer, new Object[]{name, detail, duration});
                }
                DiagnosticUtilities.tableFooter(fastStringBuffer);
            }

            fastStringBuffer.append("<br/><br/>");

            if (threadTaskStack.size() == 0) {
                fastStringBuffer.append("<i>None</i>");
            } else {
                for (Map.Entry<Thread, LinkedList<Task>> entry : threadTaskStack.entrySet()) {
                    Thread thread = entry.getKey();
                    LinkedList<Task> stack = entry.getValue();

                    if (stack.size() > 0) {
                        DiagnosticUtilities.contextHeader(fastStringBuffer, thread.getName());
                        DiagnosticUtilities.tableHeader(fastStringBuffer);
                        for (Task task : stack) {
                            long duration = endTime - task.startTime;
                            String name = task.type.name;
                            String detail = task.detail == null ? "<i>N/A</i>" : task.detail;
                            DiagnosticUtilities.tableRow(fastStringBuffer, new Object[]{name, detail, duration});
                        }
                        DiagnosticUtilities.tableFooter(fastStringBuffer);
                        fastStringBuffer.append("<br/><br/>");
                    }
                }
            }


            return fastStringBuffer.toString();
        }
    }

    private static String createTaskMetricsHTML() {
        synchronized (Profiler.class) {
            StringBuffer buffer = new StringBuffer();

            DiagnosticUtilities.contextHeader(buffer, "Metrics");
            DiagnosticUtilities.tableHeader(buffer);
            DiagnosticUtilities.tableHeaderRow(buffer, new Object[]{"Type", "Count", "Max time", "Avg. time", "Min Time", "Longest 5 Tasks"});
            for (Map.Entry<TaskType, Metrics> entry : Profiler.typeMetrics.entrySet()) {
                TaskType type = entry.getKey();
                Metrics metrics = entry.getValue();
                DiagnosticUtilities.tableRow(buffer, new Object[]{type.name, metrics.count, metrics.maxTaskTime,
                        metrics.averageTaskTime, metrics.minTaskTime, longestTaskDetail(metrics)});
            }
            DiagnosticUtilities.tableFooter(buffer);

            return buffer.toString();
        }
    }

    private static String longestTaskDetail(Metrics metrics) {
        FastStringBuffer fsb = new FastStringBuffer();
        fsb.append("<table border=1 width=100%>");
        for (Task task : metrics.longestTasks) {
            fsb.append("<tr>");
            fsb.append("<td>" + task.duration + "</td><td>&nbsp;");
            if (task.detail != null) fsb.append(task.detail);
            fsb.append("</td></li>");
        }
        fsb.append("</table>");
        return fsb.toString();
    }

    private static class ProfilerMBeanAdaptor implements ProfilerMBeanAdaptorMBean {
        public String reportRunningTasks() {
            return createRunningTasksHTML();
        }

        public String reportTaskMetrics() {
            return createTaskMetricsHTML();
        }
    }
}
