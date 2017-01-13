/**
 * Description
 *
 * @author Tim Parker
 * @version $Revision: 1.1 $
 */
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

import org.bhavaya.beans.criterion.TreeCriterion;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TreeCriterionCheckModel implements CheckModel {
    private Map statesByPath = new HashMap();

    private TreeCriterion criterion;

    public TreeCriterionCheckModel(TreeCriterion criterion) {
        this.criterion = criterion;
        initState();
    }

    private void initState() {
        TreeCriterion.SelectableEnumElement[] selected = criterion.getSelectedRightOperand();
        if (selected != null) {
            for (int i = 0; i < selected.length; i++) {
                TreeCriterion.EnumElement element = selected[i];
                ArrayList list = new ArrayList();
                do {
                    list.add(0, element);
                    element = criterion.getParent(element);
                } while (element != null);
                statesByPath.put(new TreePath(list.toArray(new Object[list.size()])), new Boolean(selected[i].isSelected()));
            }
        }
    }

    public TreePath[] getPathsWithState() {
        if (statesByPath.size() > 0) {
            return (TreePath[]) statesByPath.keySet().toArray(new TreePath[statesByPath.size()]);
        } else {
            return null;
        }
    }

    public TreeCriterion getCriterion() {
        try {
            TreeCriterion clone = (TreeCriterion) criterion.clone();
            clone.setSelectedRightOperand(getRightOperand());
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            //should not happen
            throw new IllegalStateException("TreeCriterion should be cloneable");
        }
    }

    public void setCriterion(TreeCriterion criterion) {
        this.criterion = criterion;
    }

    public boolean isChecked(TreePath path) {
        return getCurrentState(path);
    }

    public boolean subtreeVaries(TreePath path) {
        if (getChildCount(path) > 0) {
            for (Iterator i = statesByPath.keySet().iterator(); i.hasNext();) {
                TreePath anotherPath = (TreePath) i.next();
                if (!path.equals(anotherPath) && path.isDescendant(anotherPath)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean getCurrentState(TreePath path) {
        if (path == null) {
            return false;
        }
        Boolean state = (Boolean) statesByPath.get(path);
        if (state != null) {
            return state.booleanValue();
        } else {
            return getCurrentState(path.getParentPath());
        }
    }

    public void nodeToggled(TreePath path) {
        boolean currentState = getCurrentState(path);
        boolean parentCurrentState = getCurrentState(path.getParentPath());

        //set the current state only if we will be different to the parent state
        boolean newState = !currentState;
        if (newState == parentCurrentState) {
            statesByPath.remove(path);
        } else {
            //remove all descendants setting as we are now overriding again
            for (Iterator i = statesByPath.keySet().iterator(); i.hasNext();) {
                if (path.isDescendant((TreePath) i.next())) {
                    i.remove();
                }
            }
            statesByPath.put(path, new Boolean(newState));
        }
    }

    public Object getRoot() {
        return criterion.getRoot();
    }

    public Object getChild(Object parent, int index) {
        return criterion.getChild((TreeCriterion.EnumElement) parent, index);
    }

    public int getChildCount(Object parent) {
        if (parent instanceof TreePath) {
            parent = ((TreePath) parent).getLastPathComponent();
        }
        return criterion.getChildCount((TreeCriterion.EnumElement) parent);
    }

    public boolean isLeaf(Object node) {
        return criterion.getChildCount((TreeCriterion.EnumElement) node) == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        return criterion.getIndexOfChild((TreeCriterion.EnumElement) parent, (TreeCriterion.EnumElement) child);
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }


    public TreeCriterion.SelectableEnumElement[] getRightOperand() {
        ArrayList result = new ArrayList(statesByPath.size());
        for (Iterator i = statesByPath.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            TreePath path = (TreePath) entry.getKey();
            TreeCriterion.EnumElement element = (TreeCriterion.EnumElement) path.getLastPathComponent();
            Boolean selected = (Boolean) entry.getValue();
            result.add(new TreeCriterion.SelectableEnumElement(element.getId(), element.getDescription(), selected.booleanValue()));
        }
        return (TreeCriterion.SelectableEnumElement[]) result.toArray(new TreeCriterion.SelectableEnumElement[result.size()]);
    }
}
