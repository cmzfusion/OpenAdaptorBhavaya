package org.bhavaya.ui.table;

import org.bhavaya.util.Switch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An execution manager for a group of CachedObjectGraph instances.  This manager will give CachedObjectGraph
 * instances with user priority execution preference over other instances.  In other words, emphasis is placed
 * on loading instances visible to the user over those invisible to the user (in the background).
 *
 * @see CachedObjectGraph#setExecutionController(org.bhavaya.ui.table.CachedObjectGraph.ExecutionController)
 */
public class UserPriorityExecutionManager {
    private final String mutexLock = "UserPriorityExecutionManagerLock";

    private Set executingUserPriorityControllers = new HashSet();
    private Set nonUserPriorityControllers = new HashSet();
    private boolean allowBackgroundRun;


    public CachedObjectGraph.ExecutionController createExecutionController() {
        return new ExecutionControllerImpl();
    }

    private void checkControllerStatus() {
        allowBackgroundControllersToRun(executingUserPriorityControllers.size() == 0);
    }

    private void allowBackgroundControllersToRun(boolean allowRun) {
        if (this.allowBackgroundRun != allowRun) {
            this.allowBackgroundRun = allowRun;
            for (Iterator iterator = nonUserPriorityControllers.iterator(); iterator.hasNext();) {
                ExecutionControllerImpl executionController = (ExecutionControllerImpl) iterator.next();
                executionController.executionSwitch.setOn(allowRun);
            }
        }
    }

    private class ExecutionControllerImpl implements CachedObjectGraph.ExecutionController {
        private boolean userPriority;
        private boolean waiting;
        private Switch executionSwitch = new Switch();

        public void setWaiting(boolean waiting) {
            this.waiting = waiting;
            updateStatus();
        }

        private void updateStatus() {
            synchronized (mutexLock) {
                if (userPriority) {
                    nonUserPriorityControllers.remove(this);
                    if (this.waiting) {
                        executingUserPriorityControllers.remove(this);
                    } else {
                        executingUserPriorityControllers.add(this);
                    }
                    executionSwitch.setOn(true);
                } else {
                    executingUserPriorityControllers.remove(this);
                    nonUserPriorityControllers.add(this);
                }
                checkControllerStatus();
            }
        }

        public void setUserPriority(boolean userPriority) {
            this.userPriority = userPriority;
            updateStatus();
        }

        public void waitForExecutionSignal() {
            try {
                executionSwitch.waitFor();
            } catch (InterruptedException e) {
            }
        }
    }
}
