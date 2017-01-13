package org.bhavaya.ui.compass;

import org.bhavaya.ui.StaticListTreeNode;
import org.bhavaya.ui.compass.CompassTypeHierarchy;
import org.bhavaya.util.Utilities;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Eh?
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class CompassNode extends StaticListTreeNode {
    private Object key;
    private String description;
    private CompassTypeHierarchy typeHierarchy;

    public CompassNode(TreeNode parent, Object key, String description, CompassTypeHierarchy typeHierarchy) {
        super(parent, typeHierarchy.getNodeIsLeafOverride());
        this.key = key;
        this.description = description;
        this.typeHierarchy = typeHierarchy;
    }

    public Object getKey() {
        return key;
    }

    public String toString() {
        return description;
    }

    public String getDescription() {
        return description;
    }

    public CompassTypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    protected void init() {
        List childrenOfThisType = typeHierarchy.getChildrenOfThisType(key);
        Utilities.sort(childrenOfThisType, Utilities.COMPARATOR);
        for (Iterator iterator = childrenOfThisType.iterator(); iterator.hasNext();) {
            CompassTypeHierarchy.Result childResult = (CompassTypeHierarchy.Result) iterator.next();
            add(new CompassNode(this, childResult.getKey(), childResult.getDescription(), typeHierarchy));
        }

        CompassTypeHierarchy nextType = typeHierarchy.getNextType();
        List childrenOfNextType = typeHierarchy.getChildrenOfNextType(key);
        Collections.sort(childrenOfNextType, Utilities.COMPARATOR);
        for (Iterator iterator = childrenOfNextType.iterator(); iterator.hasNext();) {
            CompassTypeHierarchy.Result childResult = (CompassTypeHierarchy.Result) iterator.next();
            add(new CompassNode(this, childResult.getKey(), childResult.getDescription(), nextType));
        }
    }
}
