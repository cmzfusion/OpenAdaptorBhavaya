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

import org.bhavaya.beans.ForeignKeyProperty;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.collection.CompositeBeanCollection;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public abstract class ViewAssociatedBeanCollectionAction extends AuditedAbstractAction {
    protected String manyProperty;

    public static void addActions(Object[] beanArray, ActionGroup actions, Class beanType) {
        Attribute[] attributes = getAttributes(beanArray);

        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            Class attributeType = attribute.getType();
            String attributeName = attribute.getName();
            if (BeanCollection.class.isAssignableFrom(attributeType)) {
                if (beanArray.length == 1) {
                    actions.addAction(new SingleBean(beanArray[0], attributeName));
                } else if (beanArray.length > 1) {
                    actions.addAction(new ManyBeans(beanType, beanArray, attributeName));
                }
            }
        }
    }

    private static Attribute[] getAttributes(Object[] beanArray) {
        Set attributes = new TreeSet();
        for (int i = 0; i < beanArray.length; i++) {
            Object o = beanArray[i];
            Attribute[] attributesForBean = Generic.getType(o).getAttributes();
            attributes.addAll(Arrays.asList(attributesForBean));
        }
        return (Attribute[]) attributes.toArray(new Attribute[attributes.size()]);
    }

    public ViewAssociatedBeanCollectionAction(String manyProperty) {
        super();
        this.manyProperty = manyProperty;
        putValue(Action.NAME, getActionName());
    }

    public void auditedActionPerformed(ActionEvent e) {
        BeanCollection beanCollection = getBeanCollection();
        View view;
        String tabName = getTabName();
        if (beanCollection != null) {
            BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, true);
            view = new TableView(tabName, tabName, tabName, beanCollectionTableModel, null);
        } else {
            view = new LabelView(tabName, tabName, tabName, "<html>There is no data.</html>", ImageIconCache.getImageIcon("error.png"));
        }
        Workspace.getInstance().displayView(view);
    }

    protected abstract BeanCollection getBeanCollection();

    private String getTabName() {
        return getActionName() + " - " + getDataDescription();
    }

    protected abstract String getDataDescription();

    private String getActionName() {
        String displayName = manyProperty;
        if (manyProperty.endsWith(ForeignKeyProperty.MANY_PROPERTY_SUFFIX)) displayName = manyProperty.substring(0, manyProperty.indexOf(ForeignKeyProperty.MANY_PROPERTY_SUFFIX));
        return Utilities.getDisplayName(displayName);
    }

    public static class SingleBean extends ViewAssociatedBeanCollectionAction {
        private Object bean;

        public SingleBean(Object bean, String manyProperty) {
            super(manyProperty);
            this.bean = bean;
        }

        protected BeanCollection getBeanCollection() {
            return (BeanCollection) Generic.get(bean, manyProperty);
        }

        protected String getDataDescription() {
            return bean.toString();
        }
    }

    public static class ManyBeans extends ViewAssociatedBeanCollectionAction {
        private Object[] beans;
        private Class parentType;

        public ManyBeans(Class parentType, Object[] beans, String manyProperty) {
            super(manyProperty);
            this.beans = beans;
            this.parentType = parentType;
        }

        protected BeanCollection getBeanCollection() {
            List beanCollections = new ArrayList();
            for (int i = 0; i < beans.length; i++) {
                Object bean = beans[i];
                if (Generic.getType(bean).attributeExists(manyProperty)) {
                    Object componentBeanCollection = Generic.get(bean, manyProperty);
                    if (componentBeanCollection != null) beanCollections.add(componentBeanCollection);
                }
            }

            BeanCollection[] beanCollectionsArray = (BeanCollection[]) beanCollections.toArray(new BeanCollection[beanCollections.size()]);
            if (beanCollectionsArray.length > 0) {
                return new CompositeBeanCollection(beanCollectionsArray[0].getType(), beanCollectionsArray);
            } else {
                return null;
            }
        }

        protected String getDataDescription() {
            return beans.length + " " + BeanCollectionGroup.getDefaultInstance(parentType).getPluralDisplayName();
        }
    }

}
