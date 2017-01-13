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

package org.bhavaya.ui;

import org.bhavaya.util.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Daniel "never goes home" van Enckevort
 * @version $Revision: 1.6 $
 */

public class BeanPropertyTreeModel extends DefaultTreeModel {

    private static final Map instances = new HashMap();
    private PropertyChangeHandler propertyChangeHandler;

    public static BeanPropertyTreeModel getInstance(Class type) {
        BeanPropertyTreeModel model = (BeanPropertyTreeModel) instances.get(type);
        if (model == null) {
            model = new BeanPropertyTreeModel(type);
            instances.put(type, model);
        }
        return model;
    }

    protected BeanPropertyTreeModel(Class type) {
        super(null);
        propertyChangeHandler = new PropertyChangeHandler(this);
        PropertyModel.addPropertyDisplayNameChangeListener(propertyChangeHandler);
        DefaultBeanPropertyTreeNode root = new DefaultBeanPropertyTreeNode(getTopLevelAttribute(type));
        setRoot(root);
    }

    /**
     * warning, this does not fire tree model events. you need to do that yourself if
     * you use this method
     */
    private void addChildrenToNode(DefaultBeanPropertyTreeNode parentNode) {
        addSubClassNodes(parentNode);

        Class type = parentNode.getAttribute().getType();
        Attribute[] attributes;

        if (parentNode == getRoot() || !parentNode.isPropertyGroup()) {
            attributes = PropertyModel.getInstance(type).getAttributes(); // if root or a real property get all attributes not just declared ones
        } else {
            attributes = PropertyModel.getInstance(type).getDeclaredAttributes();
        }

        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            DefaultBeanPropertyTreeNode subProperty = new DefaultBeanPropertyTreeNode(attribute);
            parentNode.add(subProperty);
        }
    }

    /**
     * if parentNode represents an abstract type then this method will add PropertyGroup nodes that contain just
     * the declared properties for each known concrete type of the abstract type.
     * e.g. parentNode represents a Trade, this might add 2 propertyGroup nodes, one containing properties specific to
     * FxTrade, the other containing properties specific to BondTrade
     */
    private void addSubClassNodes(DefaultBeanPropertyTreeNode parentNode) {
        //add nodes containing the properties for concrete types
        Class[] subTypes = null;
        if (!parentNode.isPropertyGroup() && parentNode.getParentProperty() != null) {
            Attribute parentAttribute = parentNode.getParentProperty().getAttribute();
            subTypes = PropertyModel.getInstance(parentAttribute.getType()).getValidTypesForProperty(parentNode.getAttribute().getName());
        }
        if (subTypes == null || subTypes.length == 0) {
            subTypes = PropertyModel.getInstance(parentNode.getAttribute().getType()).getSubClasses();
        }

        for (int i = 0; i < subTypes.length; i++) {
            Class subType = subTypes[i];
            Attribute attribute = getTopLevelAttribute(subType);
            PropertyGroup propertyGroup = new PropertyGroup(attribute);
            parentNode.add(propertyGroup);
        }
    }

    private Attribute getTopLevelAttribute(Class type) {
        String typeName = Utilities.getDisplayName(ClassUtilities.getUnqualifiedClassName(type));
        return new DefaultAttribute(typeName, type);
    }

    private void propertyNameChanged(DefaultBeanPropertyTreeNode property, Class parentType, String propertyName) {
        if (property == null || !property.inflated) return;
        for (int i = 0; i < property.getChildCount(); i++) {
            DefaultBeanPropertyTreeNode child = (DefaultBeanPropertyTreeNode) property.getChildAt(i);
            String childPropertyName = child.getAttribute().getName();
            if (parentType.isAssignableFrom(property.getAttribute().getType()) && Utilities.equals(propertyName, childPropertyName)) {
                fireTreeNodesChanged(this, property.getPath(), new int[]{i}, new Object[]{child});
            } else {
                propertyNameChanged(child, parentType, propertyName);
            }
        }
    }

    private void propertyHiddenStatusChanged(DefaultBeanPropertyTreeNode property, Class parentType, String propertyName) {
        if (property == null || !property.inflated) return;
        for (int i = 0; i < property.getChildCount(); i++) {
            DefaultBeanPropertyTreeNode child = (DefaultBeanPropertyTreeNode) property.getChildAt(i);
            String childPropertyName = child.getAttribute().getName();
            if (parentType.isAssignableFrom(property.getAttribute().getType()) && Utilities.equals(propertyName, childPropertyName)) {
                fireTreeNodesChanged(this, property.getPath(), new int[]{i}, new Object[]{child});
            } else {
                propertyHiddenStatusChanged(child, parentType, propertyName);
            }
        }
    }

    private static class PropertyChangeHandler extends PropertyModel.WeakPropertyChangeListener {
        public PropertyChangeHandler(Object listenerOwner) {
            super(listenerOwner);
        }

        public void displayNameChanged(Object listenerOwner, final Class parentType, final String propertyName, String newDisplayName) {
            final BeanPropertyTreeModel beanPropertyTreeModel = (BeanPropertyTreeModel) listenerOwner;

            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    beanPropertyTreeModel.propertyNameChanged((DefaultBeanPropertyTreeNode) beanPropertyTreeModel.getRoot(), parentType, propertyName);
                }
            });
        }

        public void hiddenStatusChanged(Object listenerOwner, final Class parentType, final String propertyName, boolean hidden) {
            final BeanPropertyTreeModel beanPropertyTreeModel = (BeanPropertyTreeModel) listenerOwner;

            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    beanPropertyTreeModel.propertyHiddenStatusChanged((DefaultBeanPropertyTreeNode) beanPropertyTreeModel.getRoot(), parentType, propertyName);
                }
            });
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Nested Classes

    private class DefaultBeanPropertyTreeNode extends DefaultMutableTreeNode implements BeanPropertyTreeNode, Comparable, DynamicObjectType.AttributeListener {
        private Attribute attribute;
        protected String beanPath;
        private boolean inflated;

        public DefaultBeanPropertyTreeNode(Attribute attribute) {
            this.attribute = attribute;
            if (DynamicObservable.class.isAssignableFrom(attribute.getType())) {
                DynamicObjectType type = (DynamicObjectType) PropertyModel.getInstance(attribute.getType()).getGenericType();
                type.addAttributeListener(this);
            }
        }

        /**
         * maybe speed this up by doing a lookup?
         */
        public BeanPropertyTreeNode[] findProperty(String propertyName) {
            ArrayList matches = new ArrayList();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                DefaultBeanPropertyTreeNode childProperty = (DefaultBeanPropertyTreeNode) getChildAt(i);
                if (childProperty.isPropertyGroup()) {
                    BeanPropertyTreeNode[] childMatches = childProperty.findProperty(propertyName);
                    if (childMatches.length > 0) {
                        matches.addAll(Arrays.asList(childMatches));
                    }
                } else {
                    if (childProperty.getAttribute().getName().equals(propertyName)) return new DefaultBeanPropertyTreeNode[]{childProperty};
                }
            }
            return (BeanPropertyTreeNode[]) matches.toArray(new BeanPropertyTreeNode[matches.size()]);
        }

        public void add(MutableTreeNode property) {
            if (property != null) {
                super.add(property);
            }
        }

        public Object getUserObject() {
            return this;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DefaultBeanPropertyTreeNode)) return false;
            DefaultBeanPropertyTreeNode other = (DefaultBeanPropertyTreeNode) obj;
            if (!attribute.equals(other.attribute)) return false;
            if (!Utilities.equals(getParent(), other.getParent())) return false;
            return true;
        }

        public int hashCode() {
            int hashCode = 1;
            hashCode = (31 * hashCode) + attribute.hashCode();
            hashCode = (31 * hashCode) + (getParent() == null ? 0 : getParent().hashCode());
            return hashCode;
        }

        public String toString() {
            return attribute.getName();
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public int getChildCount() {
            if (!inflated) {
                inflated = true;
                addChildrenToNode(this);
            }
            return super.getChildCount();
        }

        /**
         * returns a '.' separated bean path for the current property
         * e.g. the "bearer" field of the object referenced by the "bond" field of the root bean is
         * located as "bond.bearer"
         */
        public String getBeanPath() {
            if (beanPath == null) {
                if (this == getRootProperty()) {
                    beanPath = "";
                } else {
                    BeanPropertyTreeNode parentProperty = getParentProperty();
                    String parentPath = parentProperty.getBeanPath();
                    beanPath = parentPath.length() > 0 ? parentPath + "." + attribute.getName() : attribute.getName();
                }
            }
            return beanPath;
        }

        public String getDisplayName() {
            if (this == getRoot()) return attribute.getName();
            return PropertyModel.getInstance(getParentProperty().getAttribute().getType()).getDisplayName(attribute.getName());
        }

        public BeanPropertyTreeNode getParentProperty() {
            return (BeanPropertyTreeNode) getParent();
        }

        public BeanPropertyTreeNode getRootProperty() {
            return (BeanPropertyTreeNode) getRoot();
        }

        public BeanPropertyTreeNode[] getPropertyPathFromRoot() {
            TreeNode[] nodes = getPath();
            BeanPropertyTreeNode[] properties = new BeanPropertyTreeNode[nodes.length];
            for (int i = 0; i < properties.length; i++) {
                properties[i] = (BeanPropertyTreeNode) nodes[i];
            }
            return properties;
        }

        public boolean isPropertyGroup() {
            return false;
        }

        public int compareTo(Object o) {
            DefaultBeanPropertyTreeNode other = (DefaultBeanPropertyTreeNode) o;
            return getAttribute().getName().compareTo(other.getAttribute().getName());
        }

        public void attributeChange(DynamicObjectType.AttributeChangeEvent evt) {
            if (evt.getAction() == DynamicObjectType.AttributeChangeEvent.ACTION_ADD) {
                DefaultBeanPropertyTreeNode newNode = new DefaultBeanPropertyTreeNode(evt.getAttribute());
                add(newNode);
                fireTreeNodesInserted(BeanPropertyTreeModel.this, getPath(), new int[]{getChildCount()-1}, new Object[]{newNode});
            } else if (evt.getAction() == DynamicObjectType.AttributeChangeEvent.ACTION_REMOVE) {
                for (int i = 0; i < getChildCount(); i++) {
                    DefaultBeanPropertyTreeNode child = (DefaultBeanPropertyTreeNode) getChildAt(i);
                    Attribute attribute = child.getAttribute();
                    if (Utilities.equals(attribute.getName(), evt.getAttribute().getName())) {
                        remove(i);
                        fireTreeNodesRemoved(BeanPropertyTreeModel.this, getPath(), new int[]{i}, new Object[]{child});
                        return;
                    }
                }
            }
        }
    }

    private class PropertyGroup extends DefaultBeanPropertyTreeNode {
        private PropertyGroup(Attribute attribute) {
            super(attribute);
        }

        /**
         * this property does not refer to any actual bean path
         */
        public String getBeanPath() {
            if (beanPath == null) {
                DefaultBeanPropertyTreeNode parent = (DefaultBeanPropertyTreeNode) getParent();
                beanPath = parent.getBeanPath();
            }
            return beanPath;
        }

        public String getDisplayName() {
            return getAttribute().getName();
        }

        public boolean isPropertyGroup() {
            return true;
        }

        public int compareTo(Object o) {
            DefaultBeanPropertyTreeNode other = (DefaultBeanPropertyTreeNode) o;
            if (isPropertyGroup() && !other.isPropertyGroup()) {
                return -1;
            }
            return super.compareTo(o);
        }

    }
}
