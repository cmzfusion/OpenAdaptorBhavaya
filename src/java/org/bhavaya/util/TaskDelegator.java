package org.bhavaya.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created using IntelliJ IDEA.
 *
 * @author Andrew J. Dean
 * @version $Revision: 1.3 $
 */
public class TaskDelegator implements TaskDelegatorMBean {
    private static final Log log = Log.getCategory(TaskDelegator.class);
    private int numberOfTaskQueues;
    private Map taskQueuesMap; // Once 1.5 <Integer, TaskQueue>
    private String name;

    public TaskDelegator(String name, int numberOfTaskQueues) {
        this.name = name;
        init(numberOfTaskQueues);
    }

    private void init(int numberOfTaskQueues) {
        this.numberOfTaskQueues = numberOfTaskQueues;

        taskQueuesMap = new HashMap(numberOfTaskQueues);

        for (int i=0; i < numberOfTaskQueues; i++) {
            taskQueuesMap.put(new Integer(i), createTaskQueue(i));
        }
    }

    private TaskQueue createTaskQueue(int i) {
        TaskQueue taskQueue = new TaskQueue(name + i, false);
        taskQueue.start();
        return taskQueue;
    }

    public void addTask(Task task) {
        getQueueForKey(Math.abs(task.hashCode())).addTask(task);
    }

    private TaskQueue getQueueForKey(int key) {
        int queueId = key % numberOfTaskQueues;
        TaskQueue taskQueue = (TaskQueue) taskQueuesMap.get(new Integer(queueId));
        if (taskQueue == null) {
            log.error("Unable to find queue for key " + key);
            taskQueue = (TaskQueue) taskQueuesMap.get(new Integer(0));
        }
        return taskQueue;
    }

    public int getNumberOfTaskQueues() {
        return numberOfTaskQueues;
    }

    public String getNumberOfTasksInQueues() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Iterator iterator = taskQueuesMap.keySet().iterator(); iterator.hasNext(); ) {
            Integer taskId = (Integer) iterator.next();
            stringBuffer.append("Queue:").append(taskId);
            stringBuffer.append(" Tasks:").append(((TaskQueue) taskQueuesMap.get(taskId)).getQueueSize()).append(" ");
        }
        return stringBuffer.toString();
    }
}
