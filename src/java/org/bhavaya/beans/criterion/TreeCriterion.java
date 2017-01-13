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

package org.bhavaya.beans.criterion;

import org.bhavaya.beans.Column;
import org.bhavaya.util.*;

import java.util.*;

/**
 * Description
 *
 * @author Tim Parker
 * @version $Revision: 1.6 $
 */
public class TreeCriterion extends EnumerationCriterion {

    private static final String TREE_PARENT_KEY_COLUMN = "parentKeyColumn";
    private static final String TREE_ROOT_KEY = "rootKey";

    private String treeParentKeyColumn;
    private Object rootKey;

    private SelectableEnumElement[] selectedRightOperand;

    /**
     * Map of parent EnumElement -> List of children EnumElement
     */
    private Map parentIdToChildren;

    /**
     * child key -> parent key
     */
    private Map childToParentId;

    /**
     * key -> EnumElement
     */
    private Map idToElement;

    private EnumElement root;

    static {
        BeanUtilities.addPersistenceDelegate(TreeCriterion.class, new BhavayaPersistenceDelegate(new String[]{"id", "selectedRightOperand"}));
    }

    public TreeCriterion(String id, SelectableEnumElement[] rightOperand) {
        super(id);
        this.selectedRightOperand = rightOperand;
    }

    protected void loadProperties(PropertyGroup properties) {
        super.loadProperties(properties);
        this.treeParentKeyColumn = properties.getProperty(TREE_PARENT_KEY_COLUMN);
        this.rootKey = properties.getProperty(TREE_ROOT_KEY);
    }

    public String getCriterionType() {
        return BasicCriterion.TREE;
    }

    /**
     * Gets all leaf nodes that are selected according the member variables includedNodes and excludedNodes
     *
     * @return all selected leaf nodes of the tree
     */
    private EnumElement[] getSelectedLeaves() {
        if (selectedRightOperand == null) {
            return null;
        }
        Set includedNodes = new HashSet(selectedRightOperand.length);
        Set excludedNodes = new HashSet();
        for (int i = 0; i < selectedRightOperand.length; i++) {
            if (selectedRightOperand[i].isSelected()) {
                includedNodes.add(selectedRightOperand[i]);
            } else {
                excludedNodes.add(selectedRightOperand[i]);
            }
        }
        Set result = new HashSet();
        for (Iterator i = includedNodes.iterator(); i.hasNext();) {
            populateWithSubtree(result, (EnumElement) i.next(), excludedNodes);
        }
        return (EnumElement[]) result.toArray(new EnumElement[result.size()]);
    }

    /**
     * Populates the given set with the node and all it's child EnumElement
     *
     * @param parent        the top node in the subtree
     * @param toPopulate    the set to populate
     * @param doNotNavigate the set of subtrees that should not be navigated
     */
    private void populateWithSubtree(Set toPopulate, EnumElement parent, Set doNotNavigate) {
        EnumElement[] children = getChildren(parent);
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                if (!doNotNavigate.contains(children[i])) {
                    populateWithSubtree(toPopulate, children[i], doNotNavigate);
                }
            }
        }
        toPopulate.add(parent);
    }

    /**
     * Returns the immediate children of the supplied element
     *
     * @param enumElement the element that the child of are required
     * @return the immediate children of the element
     */
    private EnumElement[] getChildren(EnumElement enumElement) {
        List children = (List) parentIdToChildren.get(enumElement.getId());
        if (children != null) {
            return (EnumElement[]) children.toArray(new EnumElement[children.size()]);
        } else {
            return null;
        }
    }

    /**
     * Loads the map {@link #parentIdToChildren}
     */
    private void loadTree() {
        checkFail();


        Collection result = getEnumResultMap();
        int count = 0;
        idToElement = new HashMap(result.size());
        childToParentId = new HashMap(result.size());
        parentIdToChildren = new HashMap(result.size());

        String[] enumKeyColumns = Column.columnsToNames(getEnumKeyColumns());
        for (Iterator itar = result.iterator(); itar.hasNext();) {
            Object record = itar.next();
            Object key = Utilities.createKey(enumKeyColumns, record);
            String description = (String) Generic.get(record, getEnumDescriptionColumn().getName());
            Object parentKey = Generic.get(record, treeParentKeyColumn);
            EnumElement element = new EnumElement(key, description);
            idToElement.put(key, element);

            //add as a child to the parent key
            List children = (List) parentIdToChildren.get(parentKey);
            if (children == null) {
                children = new ArrayList();
                parentIdToChildren.put(parentKey, children);
            }
            children.add(element);

            //store the child key to parent key
            childToParentId.put(key, parentKey);

            if (count == 0 && rootKey == null) {
                root = element;
            } else if (rootKey != null && rootKey.equals(key.toString())) {//TODO: rootKey should have a type specified rather than using toString() comparison on id
                root = element;
            }
            count++;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public EnumElement getRoot() {
        ensureLoaded();
        return root;
    }

    public SelectableEnumElement[] getSelectedRightOperand() {
        return selectedRightOperand;
    }

    public void setSelectedRightOperand(SelectableEnumElement[] selectedRightOperand) {
        this.selectedRightOperand = selectedRightOperand;
    }

    public static class SelectableEnumElement extends EnumElement {
        private boolean selected;

        public SelectableEnumElement(Object id, Object description, boolean selected) {
            super(id, description);
            this.selected = selected;
        }

        public SelectableEnumElement() {
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    public EnumElement getChild(EnumElement parent, int index) {
        ensureLoaded();
        List list = (List) parentIdToChildren.get(parent.getId());
        return list == null ? null : (EnumElement) list.get(index);
    }

    public int getChildCount(EnumElement parent) {
        ensureLoaded();
        List list = (List) parentIdToChildren.get(parent.getId());
        return list == null ? 0 : list.size();
    }

    public int getIndexOfChild(EnumElement parent, EnumElement child) {
        ensureLoaded();
        List list = (List) parentIdToChildren.get(parent.getId());
        return list == null ? -1 : list.indexOf(child);
    }

    public EnumElement getParent(EnumElement child) {
        ensureLoaded();
        //TODO: rootKey should have a type specified rather than using toString() comparison on id
        if (rootKey != null && child.getId().toString().equals(rootKey)) {
            return null;
        } else {
            return (EnumElement) idToElement.get(childToParentId.get(child.getId()));
        }
    }

    public String getDescription() {
        StringBuffer displayString = new StringBuffer("");
        displayString.append(getName()).append(" ").append(getOperator()).append(" (");

        if (selectedRightOperand != null) {
            int count = 0;
            for (int i = 0; i < selectedRightOperand.length; i++) {
                if (selectedRightOperand[i].isSelected()) {
                    if (count > 0) {
                        displayString.append(", ");
                    }
                    count++;
                    displayString.append(selectedRightOperand[i]);
                }
            }
            displayString.append(")");
            if (count != selectedRightOperand.length) {
                displayString.append(" NOT IN (");
                count = 0;
                for (int i = 0; i < selectedRightOperand.length; i++) {
                    if (!selectedRightOperand[i].isSelected()) {
                        if (count > 0) {
                            displayString.append(", ");
                        }
                        count++;
                        displayString.append(selectedRightOperand[i]);
                    }
                }
                displayString.append(")");
            }
        }

        displayString.append(")");
        return displayString.toString();
    }

    private void ensureLoaded() {
        if (parentIdToChildren == null) {
            loadTree();
            setRightOperand(getSelectedLeaves());
        }
    }

    public Object getRightOperand() {
        ensureLoaded();
        return super.getRightOperand();
    }
}