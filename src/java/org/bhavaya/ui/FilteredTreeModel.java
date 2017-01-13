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

import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */

public class FilteredTreeModel implements TreeModel {

    private static final Log LOG = Log.getCategory(FilteredTreeModel.class);

    private ArrayList listeners;
    private TreeModelListener delegateListener;
    private TreeModel delegate;
    private FilteredTreeModel.Filter addPropertyFilter;
    private FilteredTreeModel.Filter addChildrenFilter;
    private Filter inlineChildrenFilter;
    private Map parentToChildren;

    public FilteredTreeModel(TreeModel delegate, Filter addPropertyFilter, Filter addChildrenFilter, Filter inlineChildrenFilter) {
        this.delegate = delegate;
        this.addPropertyFilter = addPropertyFilter;
        this.addChildrenFilter = addChildrenFilter;
        this.inlineChildrenFilter = inlineChildrenFilter;

        listeners = new ArrayList();
        //This shouldn't be necessary but we have had endless loops calling remove on a standard HashMap
        //Have added some thread checking when accessing this map
        parentToChildren = new ConcurrentHashMap();

        delegateListener = new DelegateTreeModelListener();
        delegate.addTreeModelListener(delegateListener);
    }

    private List getChildrenForParent(Object parent) {
        checkThread();
        List children = (List) parentToChildren.get(parent);

        if (children == null) {
            int childCount = delegate.getChildCount(parent);

            if (childCount > 0 && allowChildren(parent)) {
                children = new ArrayList(childCount);
                addChildrenToList(parent, children);
                Utilities.sort(children);
            }

            if (children == null || children.size() == 0) children = Collections.EMPTY_LIST;

            parentToChildren.put(parent, children);
        }
        return children;
    }

    private void addChildrenToList(Object parent, List children) {
        int childCount = delegate.getChildCount(parent);

        for (int i = 0; i < childCount; i++) {
            Object property = delegate.getChild(parent, i);
            if (allowProperty(property)) {
                if (inlineChildren(property)) {
                    addChildrenToList(property, children);
                } else {
                    children.add(property);
                }
            }
        }
    }

    public void dispose() {
        this.addPropertyFilter = null;
        this.addChildrenFilter = null;
        delegate.removeTreeModelListener(delegateListener);
    }

    public Object getRoot() {
        return delegate.getRoot();
    }

    public Object getChild(Object parent, int index) {
        return getChildrenForParent(parent).get(index);
    }

    public int getChildCount(Object parent) {
        return getChildrenForParent(parent).size();
    }

    public boolean isLeaf(Object node) {
        return getChildrenForParent(node).size() == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.valueForPathChanged(path, newValue);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return getChildrenForParent(parent).indexOf(child);
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void fireStructureChanged() {
        TreeModelEvent e = new TreeModelEvent(this, new TreePath(getRoot()));
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            TreeModelListener l = (TreeModelListener) iterator.next();
            l.treeStructureChanged(e);
        }
    }

    public void fireStructureChanged(TreePath path) {
        TreeModelEvent e = new TreeModelEvent(this, path);
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            TreeModelListener l = (TreeModelListener) iterator.next();
            l.treeStructureChanged(e);
        }
    }

    public void fireNodesChanged(TreeModelEvent e) {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            TreeModelListener l = (TreeModelListener) iterator.next();
            l.treeNodesChanged(e);
        }
    }

    private boolean allowProperty(Object node) {
        BeanPropertyTreeNode property = (BeanPropertyTreeNode) node;

        if (property.isPropertyGroup()) {
            return allowPropertyGroup(node);
        } else {
            return filter(addPropertyFilter, property);
        }
    }

    private boolean allowPropertyGroup(Object node) {
        BeanPropertyTreeNode property = (BeanPropertyTreeNode) node;

        BeanPropertyTreeNode rootNode = property.getRootProperty();
        Class rootClass = rootNode.getAttribute().getType();
        Class propertyClass = property.getAttribute().getType();
        String beanPath = property.getBeanPath();

        if (propertyClass.getName().indexOf('$') != -1) return false; // dont allow inner classes as propertyGroups

        // must have at least one valid declared property
        Attribute[] declaredAttributes = PropertyModel.getInstance(propertyClass).getDeclaredAttributes();
        for (int j = 0; j < declaredAttributes.length; j++) {
            Attribute declaredAttribute = declaredAttributes[j];
            String declaredPropertyBeanPath = beanPath.length() > 0 ? beanPath + "." + declaredAttribute.getName() : declaredAttribute.getName();
            if (addPropertyFilter.isValidProperty(rootClass, propertyClass, declaredAttribute.getType(), declaredAttribute.getName(), declaredPropertyBeanPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean allowChildren(Object node) {
        return filter(addChildrenFilter, (BeanPropertyTreeNode) node);
    }

    private boolean inlineChildren(Object node) {
        BeanPropertyTreeNode property = (BeanPropertyTreeNode) node;
        if (!property.isPropertyGroup()) return false;
        return filter(inlineChildrenFilter, property);
    }

    private static boolean filter(Filter filter, BeanPropertyTreeNode property) {
        BeanPropertyTreeNode rootNode = property.getRootProperty();
        Class rootClass = rootNode.getAttribute().getType();
        boolean root = property == rootNode;

        BeanPropertyTreeNode parentProperty = property.getParentProperty();
        Class parentClass = root ? null : parentProperty.getAttribute().getType();

        Class propertyClass = property.getAttribute().getType();
        String beanPath = property.getBeanPath();
        String propertyName = property.getAttribute().getName();

        return filter.isValidProperty(rootClass, parentClass, propertyClass, propertyName, beanPath);
    }

    private class DelegateTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            handleChange(e);
        }

        public void treeNodesInserted(TreeModelEvent e) {
            handleChange(e);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            handleChange(e);
        }

        private void handleChange(TreeModelEvent e) {
            checkThread();
            // property can get hidden - so we can use just the tree node changed events
            TreePath treePath = e.getTreePath();
            parentToChildren.remove(treePath.getLastPathComponent());
            fireStructureChanged(treePath.getPathCount() == 1 ? treePath : treePath.getParentPath());
        }

        public void treeStructureChanged(TreeModelEvent e) {
            checkThread();
            parentToChildren.clear();
            fireStructureChanged();
        }
    }

    public static interface Filter {
        public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath);
    }

    public static final Filter DEFAULT_ADD_PROPERTY_FILTER = new Filter() {

        public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
            if (
                    propertyClass == Class.class ||
                    propertyClass == Object.class ||
                    propertyClass.isArray() ||
                    propertyClass == LoadClosure.class ||
                    Collection.class.isAssignableFrom(propertyClass) ||
                    Map.class.isAssignableFrom(propertyClass)) {
                return false;
            }

            if (propertyClass == boolean.class && propertyName.equals("lazy")) {
                return false;
            }

            if (parentClass != beanPathRootClass && propertyName.equals("count")) {
                return false;
            }

            Type type = Generic.getType(parentClass);
            if (type.attributeExists(propertyName) && !type.getAttribute(propertyName).isReadable()) {
                return false;
            }

            boolean hidden = PropertyModel.getInstance(parentClass).isHidden(propertyName);
            hidden |= PropertyModel.getInstance(beanPathRootClass).isHidden(propertyBeanPath);
            return !hidden;
        }
    };

    public static final Filter DEFAULT_ADD_CHILDREN_FILTER = new Filter() {
        public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
            if (parentClass == null) { // root
                return true;
            }

            if (Quantity.class.isAssignableFrom(propertyClass)) {
                return parentClass != Quantity.class;
            }

            if (LookupValue.class.isAssignableFrom(propertyClass)|| java.util.Date.class.isAssignableFrom(propertyClass)
            || java.lang.String.class.isAssignableFrom(propertyClass) || ClassUtilities.isPrimitiveTypeOrClass(propertyClass)) {
                return false;
            }

            return true;
        }
    };

    public static final Filter DEFAULT_INLINE_CHILDREN_FILTER = new Filter() {
        public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
            return PropertyModel.getInstance(propertyClass).isInlined();
        }
    };

    public void reset() {
        checkThread();
        parentToChildren.clear();
        fireStructureChanged();
    }

    private void checkThread() {
        if(!SwingUtilities.isEventDispatchThread()) {
            String message = "Attempting to access Map in FilteredTreeModel from outside event thread";
            if(ApplicationDiagnostics.getInstance().sendDiagnosticReportOnlyOnce(message)) {
                LOG.warn(message, new Throwable());
            }
        }
    }
}
