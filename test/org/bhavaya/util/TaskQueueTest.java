package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.util.Task;
import org.bhavaya.util.TaskQueue;
import org.bhavaya.util.TaskExecutor;
import org.bhavaya.util.PriorityTaskQueue;

/**
 * Description
 *
 * @author James Langley
 * @version $Revision: 1.1 $
 */
public class TaskQueueTest extends TestCase {

    private static int currentPosition = 0;
    private boolean errorEncountered;
    private Task[] tasks = new Task[4];

    public void testTaskQueueReplace() {
        TaskExecutor taskQueue = new TaskQueue("Test", true);
        testExecutor(taskQueue);
    }

    public void testPriorityTaskQueueReplace() {
        TaskExecutor taskQueue = new PriorityTaskQueue("Test", true);
        testExecutor(taskQueue);
    }

    private void testExecutor(TaskExecutor taskExecutor) {
        currentPosition = 0;
        errorEncountered = false;

        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new TestTask("" + ((char)('A' + i)), i);
            taskExecutor.addTask(tasks[i]);
        }
        LastTask task = new LastTask();
        TestTask replaceTask = new TestTask("B", 1);
        tasks[1] = replaceTask;
        taskExecutor.addTask(task);
        taskExecutor.addTask(replaceTask);
        taskExecutor.start();


        synchronized(task) {
            try {
                task.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (errorEncountered) {
            assertTrue("Error", false);
        }
    }

    private class TestTask extends Task {
        private int expected;
        public TestTask(String name, int expected) {
            super(name);
            this.expected = expected;
        }

        public void run() throws AbortTaskException, Throwable {
            if (expected != currentPosition) {
                errorEncountered = true;
            }
            assertTrue(expected == currentPosition);
            assertTrue(tasks[expected] == this);
            currentPosition++;
        }

        public boolean equals(Object obj) {
            if (obj instanceof TestTask) {
                return getName().equals(((TestTask)obj).getName());
            } else {
                return super.equals(obj);
            }
        }

        public int hashCode() {
            return getName().hashCode();
        }
    }

    private class LastTask extends Task {
        public LastTask() {
            super("Last Task");
        }
        public void run() throws AbortTaskException, Throwable {
            synchronized(this) {
                notify();
            }
        }
    }
}
