/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Use this to handle calculation dependencies between fields.
 * i.e. when any of the given fields change, the given update method will run and set a new value for a specific field
 * This class handles any sistuation you can throw at it. Cyclic dependencies, null values, anything. I just haven't
 * written the "enableWorldDomination()" method yet.
 *
 *
 * @author Dan
 * @version $Revision: 1.4 $
 */
public class DependencyMap extends HashMap {
    private static final Log log = Log.getCategory(DependencyMap.class);

    private static Map dependencyMaps = new HashMap();
    private Map monitoredInstances = new WeakHashMap();

//    private Map updateMethodMap = new HashMap();
    private Class dependencyForClass;
    private String name;

    public static DependencyMap getInstance(Dependant dependant) {
        return getInstance(dependant, null);
    }

    public static DependencyMap getInstance(Dependant dependant, String name) {
        Object key = new DependantKey(dependant.getClass(), name);
        DependencyMap dependencyMap = (DependencyMap) dependencyMaps.get(key);
        if (dependencyMap == null) {
            dependencyMap = new DependencyMap(dependant.getClass(), name);
            dependant.addDependencies(dependencyMap, name);
            dependencyMaps.put(key, dependencyMap);
        }
        return dependencyMap;
    }

    private DependencyMap(Class c, String name) {
        dependencyForClass = c;
        this.name = name;
    }

    public void monitor(Observable instance) {
        if (!monitoredInstances.containsKey(instance)) {
            UpdateRippler rippler = new UpdateRippler(instance);
            monitoredInstances.put(instance, rippler);
            instance.addPropertyChangeListener(rippler);
        }
    }

    public boolean isMonitoring(Observable instance) {
        return monitoredInstances.containsKey(instance);
    }

    public void updateAll(Observable instance) {
        boolean wasPreviouslyMonitoring = monitoredInstances.containsKey(instance);
        if (!wasPreviouslyMonitoring) {
            monitor(instance);
        }
        UpdateRippler rippler = (UpdateRippler) monitoredInstances.get(instance);

//        if (log.isDebug()) log.debug("Update all will cause a 'SET' on these fields: "+allTriggerFields);

        //get every field that anything is dependent upon
        //iterate these and fire property changes for each
        Set allTriggerFields = keySet();
        for (Iterator iterator = allTriggerFields.iterator(); iterator.hasNext();) {
            String field = (String) iterator.next();
            Object value = Generic.get(instance, field);
            PropertyChangeEvent evt = new PropertyChangeEvent(instance, field, value, value);
            rippler.propertyChange(evt);
        }

        if (!wasPreviouslyMonitoring) {
            ignore(instance);
        }
//        if (log.isDebug()) log.debug("Done update");
//        if (log.isDebug()) log.debug("");
    }

    public void ignore(Observable instance) {
        UpdateRippler rippler = (UpdateRippler) monitoredInstances.remove(instance);
        if (rippler != null) {
            instance.removePropertyChangeListener(rippler);
        }
    }

    /**
     * Touches the field and marks it as the last updated value.
     * <p>
     * This information is used to resolve deadlock in field dependencies - most recently changed/touched field
     * that is part of a cyclic dependency is considered to have right value and will not be updated.
     * This breaks the deadlock cycle.
     * <p>
     * Touching the field instead of resetting its value doesn't trigger the dependent fields to be updated.
     * This might be desired behaviour when you don't want to trigger calculations unless the value of the field
     * has been actually changed.
     */
    public void touchField(String field, Object instance) {
        UpdateRippler rippler = (UpdateRippler) monitoredInstances.get(instance);
        if (rippler != null) {
            rippler.touchField(field);
        }
    }

    public void addDependency(String field, String updateMethod, String[] dependsOn) {
        UpdateMethodInfo updateDetails = new UpdateMethodInfo(field, updateMethod, dependsOn);
        for (int i = 0; i < dependsOn.length; i++) {
            String fieldWithDependancy = dependsOn[i];
            List dependentsList = (List) get(fieldWithDependancy);
            if (dependentsList == null) {
                dependentsList = new ArrayList();
                put(fieldWithDependancy, dependentsList);
            }
            dependentsList.add(updateDetails);
        }
    }


    /**
     * gets an iterator for all the UpdateMethodInfo objects that store update info for when the given
     * field changes
     */
    public Iterator getAllDependantsOnField(String field) {
        List dependantsOnField = (List) get(field);
        if (dependantsOnField == null) {
            return Utilities.EMPTY_ITERATOR;
        }
        return dependantsOnField.iterator();
    }

    /**
     * gets an iterator for all fields that cause others to need an update
     */
    public Iterator getAllFieldsWithDependents() {
        return keySet().iterator();
    }

    public boolean hasDependants(String field) {
        return get(field) != null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyMap)) return false;
        DependencyMap other = (DependencyMap) o;
        return Utilities.equals(dependencyForClass, other.dependencyForClass) && Utilities.equals(name, other.name);
    }

    public int hashCode() {
        return (dependencyForClass.getName() + name).hashCode();
    }

    public String toString() {
        return "DependencyMap for: " + dependencyForClass.getName() + " " + name;
    }


    private class UpdateRippler implements PropertyChangeListener {
        private boolean acceptChanges = true;
        private HashMap lastRecentlySetFields = new HashMap();
        private int propertyChangeCounter = 1;
        private WeakReference monitoredInstance = null;

        public UpdateRippler(Observable monitoredInstance) {
            this.monitoredInstance = new WeakReference(monitoredInstance);
        }

        private void touchField(String field) {
            MutableLong aLong = (MutableLong) lastRecentlySetFields.get(field);
            if (aLong == null) {
                aLong = new MutableLong(propertyChangeCounter);
            } else {
                aLong.setLong(propertyChangeCounter);
            }
            propertyChangeCounter++;

            lastRecentlySetFields.put(field, aLong);
        }

        private long getLastUpdateTime(String field) {
            MutableLong lastUsed = (MutableLong) lastRecentlySetFields.get(field);
            if (lastUsed == null) {
                return 0;
            } else {
                return lastUsed.getLong();
            }
        }


        public void propertyChange(PropertyChangeEvent evt) {
            String fieldName = evt.getPropertyName();
            if (acceptChanges) {
                //the update was not a result of executing the dependency map
                //if (log.isDebug()) log.debug("Dependency map recieved updated value for " + fieldName);
                //update our "recently set" tracking
                if (DependencyMap.this.hasDependants(fieldName)) {
                    touchField(fieldName);

                    //we want to ignore the changes that the ripple will trigger
                    acceptChanges = false;
                    //if (log.isDebug()) log.debug("");
                    //                    if (log.isDebug()) log.debug(System.identityHashCode(evt.getSource()) + ": Updating dependants for field:" + fieldName + " value changed to: " + evt.getNewValue());
                    updateDependants(fieldName);
                    //accept propertyChanges again
                    acceptChanges = true;
                }
            }
        }

        private void updateDependants(String field) {
            Object monitoredObject = monitoredInstance.get();
            if (monitoredObject == null) return;
            ToDoList todoList = new ToDoList(field, monitoredObject);
            while (!todoList.isEmpty()) {
                //find the first update method that we can call, and call it

                Iterator todoIter = todoList.getAllFields();
                while (todoIter.hasNext()) { //while we still have fields to process
                    String fieldToUpdate = (String) todoIter.next();

                    UpdateMethodInfo methodInfo = todoList.findReadyUpdateMethodForField(fieldToUpdate);
                    if (methodInfo != null) {
                        try {
//                            if (log.isDebug()) log.debug(System.identityHashCode(instance) + ": Calling " + methodInfo.getUpdateMethod().getName() + " for field " + fieldToUpdate);
                            methodInfo.getUpdateMethod().invoke(monitoredObject);
                        //} catch (DoNotContinueUpdateException e) {
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof DoNotContinueUpdateException) {
                                log.info("Not continuing update due to: " + e.getTargetException().getMessage());
                                return;
                            } else {
                                throw new RuntimeException("Could not execute update method for field:" + fieldToUpdate, e);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Could not execute update method for field:" + fieldToUpdate, e);
                        }
                        todoList.removeAllUpdatesForField(fieldToUpdate);
                        //reset the todolist iterator because the todolist has changed
                        todoIter = todoList.getAllFields();
                    }
                }//no more fields that can be processed

                //we either have completed everything in the todo set, or we have a deadlock
                //(i.e. every field left in the todo is waiting on another field before it can progress)
                if (!todoList.isEmpty()) {
                    //got deadlock.
//                    if (log.isDebug()) log.debug("Deadlocked while updating dependants of " + field + " (value = " + Generic.get(instance, field));
                    String deadlockCause = todoList.breakDeadlock();
//                    if (log.isDebug()) log.debug(deadlockCause + " was the most recently updated field that was part of a deadlock loop. Used current value to break deadlock: " + Generic.get(instance, deadlockCause));
                }
            }//do while todolist is not empty
        }


        /**
         * todoList's keys are field names.
         * the value is a List of UpdateMethodInfo objects
         *
         * The purpose is to store which fields need an update, and which method (or choice of methods)
         * can be used to do this update.
         *
         */
        private class ToDoList {
            private Map fieldToUpdateMethods = new HashMap();
            private Object instance;

            private final int WAITING = 0;
            private final int READY_TO_RUN = 1;

            public ToDoList(String field, Object instance) {
                this.instance = instance;
                addTodosForField(field);
                //if anyone does something that will overwrite the given field, ignore that request
                removeAllUpdatesForField(field);
            }

            public Iterator getAllFields() {
                return fieldToUpdateMethods.keySet().iterator();
            }

            public boolean isEmpty() {
                return fieldToUpdateMethods.isEmpty();
            }

            /**
             * for the given field, find all update methods that need this field to execute
             * However, any update method is only allowed to be added to the todolist if it does not
             * update a locked field.
             */
            private void addTodosForField(String field) {
                //get all the dependents on this field
                Iterator allDependents = DependencyMap.this.getAllDependantsOnField(field);
                while (allDependents.hasNext()) {
                    UpdateMethodInfo info = (UpdateMethodInfo) allDependents.next();

                    Set updateMethodsSet = (Set) fieldToUpdateMethods.get(info.getField());
                    if (updateMethodsSet == null) {
                        updateMethodsSet = new HashSet();
                        fieldToUpdateMethods.put(info.getField(), updateMethodsSet);

//                        if (log.isDebug()) log.debug("An update to "+field+" causes an update to "+info.getField()+" via "+info.getUpdateMethod().getName());
                        //we have now added a new field to the todolist, we need to add everything
                        //that it will update
                        addTodosForField(info.getField());
                    }
                    updateMethodsSet.add(info);
                }
            }

            public UpdateMethodInfo findReadyUpdateMethodForField(String fieldToUpdate) {
                //look at each method we might use to update the given field.
                //run it if we have all the dependencies satisfied
                Set updateMethodList = (Set) fieldToUpdateMethods.get(fieldToUpdate);
                Iterator listIter = updateMethodList.iterator();

                while (listIter.hasNext()) {
                    UpdateMethodInfo methodInfo = (UpdateMethodInfo) listIter.next();
                    int shouldRunUpdate = canRunUpdate(methodInfo);
                    if (shouldRunUpdate == READY_TO_RUN) {
                        return methodInfo;
                    }
                }
                return null;
            }


            /**
             * returns true if we are
             */
            private int canRunUpdate(UpdateMethodInfo updateMethod) {
                //we can do the update if its required fields are not in the todoSet
                String[] requiredFields = updateMethod.getRequiredFields();

                //for each required field, see if it is in the todolist.
                //if not, then we can run the method
                for (int i = 0; i < requiredFields.length; i++) {
                    if (fieldToUpdateMethods.containsKey(requiredFields[i])) {
                        return WAITING;
                    }
                }
                return READY_TO_RUN;
            }


            private void removeAllUpdatesForField(String field) {
                fieldToUpdateMethods.remove(field);
            }

            /**
             * find a deadlock cause and solve it.
             * @return the name of the field it used to resolve the deadlock
             */
            public String breakDeadlock() {
                //to resolve this, we need to find a field that is part of a cyclic dependency
                //then we simply say that this field has the right value and does not need to be
                //updated. i.e. its current value is ok as it is. This will break a cycle.

                Set seenFields = new HashSet();
                Set loopFields = new HashSet();

                Iterator allBlockedFields = getAllFields();
                while (allBlockedFields.hasNext()) {
                    String field = (String) allBlockedFields.next();
                    findDeadlockLoops(field, seenFields, loopFields, new Stack());
                }

                //look through the list of loopFields to find the most recently set one
//                if (log.isDebug()) log.debug("This is a list of all fields that form deadlock loops: "+loopFields);
                String deadlockCause = getMostRecentlyUpdatedField(loopFields);
                removeAllUpdatesForField(deadlockCause);
                return deadlockCause;
            }

            /**
             * for the given field, this method will traverse the "depends on" relationships in order to
             * determine all nodes that participate in a deadlock loop. (it puts everything it finds into loopMembers)
             */
            private void findDeadlockLoops(String startField, Set seenSoFar, Set loopMemebers, Stack searchPath) {
//                if (log.isDebug()) log.debug("   Find deadlock loops starting at: "+startField);
                try {
                    searchPath.push(startField);
//                    log.indent(1);
                    if (seenSoFar.contains(startField)) {
                        //might have got loop
                        storeLoopMembers(searchPath, loopMemebers);
                    } else {
                        seenSoFar.add(startField);
//                        if (log.isDebug()) log.debug("   Checking dependants of "+startField);
                        Iterator allDependants = DependencyMap.this.getAllDependantsOnField(startField);
                        while (allDependants.hasNext()) {
                            UpdateMethodInfo updateMethodInfo = (UpdateMethodInfo) allDependants.next();
                            String dependantField = updateMethodInfo.getField();
                            //if this field is one that is still in our todo list then use it to search for
                            //deadlock loops
                            if (fieldToUpdateMethods.containsKey(dependantField)) {
                                findDeadlockLoops(dependantField, seenSoFar, loopMemebers, searchPath);
                            }
                        }
                    }
                } finally {
                    searchPath.pop();
//                    log.indent(-1);
                }
            }

            /**
             * call this method when you might have found a path that is cyclic and want to record the fields
             * involved in the loop
             * i.e. the last item in "pathWithCycle" occurs twice, all fields in between are part of the loop
             */
            private void storeLoopMembers(Stack pathWithCycle, Set loopMembers) {
                String loopCause = (String) pathWithCycle.peek();
                int firstLoopCauseIdx = pathWithCycle.indexOf(loopCause);
                //if the one we found was before the last item in the stack, then we have a cycle.
                //record it
                if (firstLoopCauseIdx < pathWithCycle.size() - 1) {
                    StringBuffer cycleStr = new StringBuffer();
                    for (int i = firstLoopCauseIdx; i < pathWithCycle.size(); i++) {
                        loopMembers.add(pathWithCycle.get(i));
                        cycleStr.append(pathWithCycle.get(i)).append(", ");
                    }
//                    if (log.isDebug()) log.debug("      found cycle: "+cycleStr);
                }
            }

            private String getMostRecentlyUpdatedField(Set fieldSet) {
                String bestField = (String) fieldSet.iterator().next();
                long bestUpdateTime = Long.MIN_VALUE;

                Iterator it = fieldSet.iterator();
                while (it.hasNext()) {
                    String field = (String) it.next();
                    long updateTime = UpdateRippler.this.getLastUpdateTime(field);
                    if (updateTime > bestUpdateTime) {
                        bestField = field;
                        bestUpdateTime = updateTime;
                    }
                }
                return bestField;
            }
        }
    }

    private class UpdateMethodInfo {
        private Method updateMethod;
        private String field;
        private String[] requiredFields;

        public UpdateMethodInfo(String field, String updateMethodName, String[] requiredFields) {
            try {
                Method updateMethod = dependencyForClass.getMethod(updateMethodName);

                this.updateMethod = updateMethod;
                this.requiredFields = requiredFields;
                this.field = field;
            } catch (Exception e) {
                throw new RuntimeException("Could not get update method: " + updateMethodName, e);
            }
        }

        public Method getUpdateMethod() {
            return updateMethod;
        }

        public String[] getRequiredFields() {
            return requiredFields;
        }

        public String getField() {
            return field;
        }
    }

    private static final class MutableLong {
        long mutableLong;

        public MutableLong(long mutableLong) {
            this.mutableLong = mutableLong;
        }

        public void setLong(long l) {
            mutableLong = l;
        }

        public long getLong() {
            return mutableLong;
        }
    }

    public static interface Dependant {
        public void addDependencies(DependencyMap dependencyMap, String name);
    }

    private static class DependantKey {
        private Class beanType;
        private String dependencyGroup;

        public DependantKey(Class beanType, String dependencyGroup) {
            this.beanType = beanType;
            this.dependencyGroup = dependencyGroup;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DependantKey)) return false;

            final DependantKey dependantKey = (DependantKey) o;

            if (!beanType.equals(dependantKey.beanType)) return false;
            if (dependencyGroup != null ? !dependencyGroup.equals(dependantKey.dependencyGroup) : dependantKey.dependencyGroup != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = beanType.hashCode();
            result = 29 * result + (dependencyGroup != null ? dependencyGroup.hashCode() : 0);
            return result;
        }
    }

    /**
     * Throwing this exception in the update method will cause the updates to stop.
     * This can be used when for instance user doesn't approve the change.
     */
    public static class DoNotContinueUpdateException extends Exception {
        public DoNotContinueUpdateException() {
        }

        public DoNotContinueUpdateException(String message) {
            super(message);
        }

        public DoNotContinueUpdateException(String message, Throwable cause) {
            super(message, cause);
        }

        public DoNotContinueUpdateException(Throwable cause) {
            super(cause);
        }
    }
}