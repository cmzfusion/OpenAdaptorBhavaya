package org.bhavaya.ui;

import org.bhavaya.collection.WeakHashSet;
import org.bhavaya.util.Observable;
import org.bhavaya.util.WeakPropertyChangeListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;

/**
 * Fires treeNodeChanged event on propertyChange event.
 * Uses weak property change listeners.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class PropertyChangeAwareTreeModel extends DefaultTreeModel {

    public PropertyChangeAwareTreeModel(TreeNode root) {
        super(root);
    }

    public PropertyChangeAwareTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    public Object getRoot() {
        Object root = super.getRoot();
        registerListener(root);
        return root;
    }

    public Object getChild(Object parent, int index) {
        Object child = super.getChild(parent, index);
        registerListener(child);
        return child;
    }

    private void registerListener(Object node) {
        if (node instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode) node;
            Object userObject = mutableTreeNode.getUserObject();
            if (userObject instanceof Observable) {
                registerListener((TreeNode)node, (Observable)userObject);
            }
        }
        if (node instanceof Observable) {
            registerListener((TreeNode)node, (Observable) node);
        }
    }

    private WeakHashSet listenedToObservables = new WeakHashSet();

    private void registerListener(TreeNode treeNode, Observable observable) {
        if (!listenedToObservables.contains(observable)) {
            listenedToObservables.add(observable);
            observable.addPropertyChangeListener(new TreeNodePropertyChangeListener(treeNode, observable, this, null));
        }
    }

    private class TreeNodePropertyChangeListener extends WeakPropertyChangeListener {
        private TreeNode treeNode;
        protected TreeNodePropertyChangeListener(TreeNode treeNode, Observable observable, PropertyChangeAwareTreeModel listenerOwner, String property) {
            super(observable, listenerOwner, property);
            this.treeNode = treeNode;
        }

        protected void propertyChange(Object listenerOwner, PropertyChangeEvent evt) {
            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    nodeChanged(treeNode);
                }
            });
        }
    }
}
