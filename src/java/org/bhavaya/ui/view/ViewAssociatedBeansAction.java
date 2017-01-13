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

package org.bhavaya.ui.view;

import org.bhavaya.beans.Schema;
import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.BeanPathTransformerGroup;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.collection.CompositeBeanCollection;
import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.dataset.CriteriaBeanCollectionEditor;
import org.bhavaya.util.Task;
import org.bhavaya.util.Transform;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.14 $
 */
public class ViewAssociatedBeansAction extends AuditedAbstractAction {
    private static final String[] criterionPrefixes = new String[]{"dataset", "shortcut"};
    private Object[] fromObjects;
    private BeanCollectionGroup toBeanCollectionGroup;
    private BeanCollectionGroup fromBeanCollectionGroup;
    private Class joinClass;
    private Transform beanToKeyTransform;

    public interface Filter {
        public boolean evaluate(Object[] fromObjects, BeanCollectionGroup toBeanCollectionGroup, BeanCollectionGroup fromBeanCollectionGroup, Class joinClass);
    }

    public static void addActions(Object[] fromObjects, ActionGroup actions, Filter filter, Transform beanToKeyTransform) {
        BeanCollectionGroup[] beanCollectionGroups = BeanCollectionGroup.getEnabledInstances();
        BeanCollectionGroup fromBeanCollectionGroup = BeanCollectionGroup.getDefaultInstance(fromObjects.getClass().getComponentType());
        Class[] fromJoins = getJoins(fromBeanCollectionGroup.getBeanType());

        Map toViewActionGroups = new TreeMap();

        for (BeanCollectionGroup toBeanCollectionGroup : beanCollectionGroups) {
            if (CriteriaBeanCollectionEditor.class.isAssignableFrom(toBeanCollectionGroup.getCollectionEditorClass())) { // this all works through criterion
                Class toViewType = toBeanCollectionGroup.getBeanType();
                Class[] toJoins = getJoins(toBeanCollectionGroup.getBeanType());
                String toViewDisplayName = toBeanCollectionGroup.getPluralDisplayName();

                Map toViewActions = new TreeMap();

                for (Class fromJoin : fromJoins) {
                    for (Class toJoin : toJoins) {
                        if (toJoin.isAssignableFrom(fromJoin) && !toViewType.isAssignableFrom(fromJoin)) {
                            if (filter == null || filter.evaluate(fromObjects, toBeanCollectionGroup, fromBeanCollectionGroup, toJoin))
                            {
                                Action action = new ViewAssociatedBeansAction(fromObjects, toBeanCollectionGroup, fromBeanCollectionGroup, toJoin, beanToKeyTransform);
                                toViewActions.put(action.getValue(Action.NAME), (action));
                            }
                        }
                    }
                }

                if (toViewActions.size() > 0) {
                    toViewActionGroups.put(toViewDisplayName, toViewActions);
                }
            }
        }

        for (Iterator iterator = toViewActionGroups.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String groupName = (String) entry.getKey();
            Map actionMap = (Map) entry.getValue();
            ActionGroup actionGroup = getActionGroup(groupName, actionMap);
            actions.addActionGroup(actionGroup);
        }
    }

    private static ActionGroup getActionGroup(String groupName, Map actionMap) {
        ActionGroup actionGroup = new ActionGroup(groupName);
        for (Iterator iterator = actionMap.values().iterator(); iterator.hasNext();) {
            Action action = (Action) iterator.next();
            actionGroup.addAction(action);
        }
        return actionGroup;
    }

    private static Class[] getJoins(Class beanType) {
        Set joins = new LinkedHashSet();
        for (String criterionprefix : criterionPrefixes) {
            addJoins(criterionprefix, beanType, joins);
        }
        return (Class[]) joins.toArray(new Class[joins.size()]);
    }

    private static void addJoins(final String criterionPrefix, Class beanType, Set joins) {
        BeanPathTransformerGroup[] groups = BeanPathTransformerGroup.getInstances(criterionPrefix, beanType);
        for (BeanPathTransformerGroup group : groups) {
            if (isValidGroup(group)) {
                Class joinClass = group.getToBeanType();
                joins.add(joinClass);
            }
        }
    }

    private static boolean isValidGroup(BeanPathTransformerGroup group) {
        return Schema.hasInstance(group.getToBeanType()) && (group.getCriterionType().equals(BasicCriterion.BASIC) || group.getCriterionType().equals(BasicCriterion.ENUMERATION));
    }

    public ViewAssociatedBeansAction(Object[] fromObjects, BeanCollectionGroup toBeanCollectionGroup, BeanCollectionGroup fromBeanCollectionGroup, Class joinClass, Transform beanToKeyTransform) {
        super();
        this.fromObjects = fromObjects;
        this.toBeanCollectionGroup = toBeanCollectionGroup;
        this.fromBeanCollectionGroup = fromBeanCollectionGroup;
        this.joinClass = joinClass;
        this.beanToKeyTransform = beanToKeyTransform;
        putValue(Action.NAME, getActionName());
    }

    public void auditedActionPerformed(final ActionEvent e) {
        ApplicationContext.getInstance().addGuiTask(new Task("Open view " + getActionName()) {
            public void run() throws Task.AbortTaskException, Throwable {
                Set joinValues = getJoinValues();
                List beanCollections = getBeanCollections(joinValues);

                BeanCollection beanCollection;
                if (beanCollections.size() == 1) {
                    beanCollection = (BeanCollection) beanCollections.get(0);
                } else if (beanCollections.size() > 1) {
                    beanCollection = new CompositeBeanCollection(toBeanCollectionGroup.getBeanType(), (BeanCollection[]) beanCollections.toArray(new BeanCollection[beanCollections.size()]));
                } else {
                    JOptionPane.showMessageDialog(UIUtilities.getWindowParent((Component) e.getSource()), "No " + BeanCollectionGroup.getDefaultInstance(joinClass).getDisplayName(), "No data", JOptionPane.INFORMATION_MESSAGE, null);
                    return;
                }

                String tabName = getDataDescription(joinValues);
                viewBeanCollectionAsTable(toBeanCollectionGroup, tabName, tabName, tabName, beanCollection);
            }
        });
    }

    protected void viewBeanCollectionAsTable(BeanCollectionGroup toBeanCollectionGroup, String viewName, String viewTabTitle, String viewFrameTitle, BeanCollection beanCollection) {
        toBeanCollectionGroup.viewBeanCollectionAsTable(viewName, viewTabTitle, viewFrameTitle, beanCollection);
    }

    private Set getJoinValues() {
        Set joinValues = new LinkedHashSet();
        for (int i = 0; i < criterionPrefixes.length; i++) {
            String criterionprefix = criterionPrefixes[i];
            addJoinValues(criterionprefix, joinValues);
        }
        return joinValues;
    }

    private void addJoinValues(final String criterionPrefix, Set joinValues) {
        BeanPathTransformerGroup[] fromGroups = BeanPathTransformerGroup.getInstances(criterionPrefix, fromBeanCollectionGroup.getBeanType());
        for (int i = 0; i < fromGroups.length; i++) {
            BeanPathTransformerGroup fromGroup = fromGroups[i];
            if (joinClass.isAssignableFrom(fromGroup.getToBeanType())) {
                for (int j = 0; j < fromObjects.length; j++) {
                    Object fromObject = fromObjects[j];
                    Set joinValuesForGroup = fromGroup.getTransformer(fromBeanCollectionGroup.getBeanType()).tranform(fromObject);
                    if (joinValuesForGroup != null) joinValues.addAll(joinValuesForGroup);
                }
            }
        }
    }

    private List getBeanCollections(Set joinValues) {
        List beanCollections = new ArrayList();
        for (Iterator iterator = joinValues.iterator(); iterator.hasNext();) {
            Object joinValue = iterator.next();
            if (joinValue != null) {
                addBeanCollections(beanCollections, joinValue);
            }
        }
        return beanCollections;
    }

    private void addBeanCollections(List beanCollections, Object joinValue) {
        for (int i = 0; i < criterionPrefixes.length; i++) {
            String criterionprefix = criterionPrefixes[i];
            addBeanCollections(criterionprefix, beanCollections, joinValue);
        }
    }

    private void addBeanCollections(final String criterionPrefix, List beanCollections, Object joinValue) {
        BeanPathTransformerGroup[] toGroups = BeanPathTransformerGroup.getInstances(criterionPrefix, toBeanCollectionGroup.getBeanType());
        for (int j = 0; j < toGroups.length; j++) {
            BeanPathTransformerGroup toGroup = toGroups[j];
            if (joinClass.isAssignableFrom(toGroup.getToBeanType()) && isValidGroup(toGroup)) {
                BeanCollection beanCollection = createBeanCollection(toGroup.getId(), joinValue);
                if (beanCollection != null) beanCollections.add(beanCollection);
            }
        }
    }

    private BeanCollection createBeanCollection(String id, Object joinValue) {
        Object key = getKeyForBean(joinValue);
        if (key == null) return null;
        Criterion newCriterion = new BasicCriterion(id, "=", key);
        String actionDescription = getDataDescription(joinValue);
        CriterionGroup criterionGroup = new CriterionGroup(actionDescription, new Criterion[]{newCriterion});
        return toBeanCollectionGroup.newBeanCollection(criterionGroup);
    }

    protected Object getKeyForBean(Object joinValue) {
        return beanToKeyTransform.execute(joinValue);
    }

    private String getDataDescription(Collection joinValues) {
        return toBeanCollectionGroup.getPluralDisplayName() + " - " + Utilities.truncate(Utilities.asString(joinValues, "-"), 50);
    }

    private String getDataDescription(Object joinValue) {
        return toBeanCollectionGroup.getPluralDisplayName() + " - " + joinValue.toString();
    }

    private String getActionName() {
        return "for " + BeanCollectionGroup.getDefaultInstance(joinClass).getDisplayName();
    }

}
