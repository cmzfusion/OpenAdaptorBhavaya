package org.bhavaya.ui;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public abstract class StaticListTreeNode implements TreeNode {
    private ArrayList childTreeNodes;
    private TreeNode parent;
    private boolean inited = false;

    private Boolean forceLeaf;

    protected StaticListTreeNode(TreeNode parent, Boolean isLeaf) {
        this.parent = parent;
        this.forceLeaf = isLeaf;
    }

    protected StaticListTreeNode(TreeNode parent, boolean isLeaf) {
        this(parent, new Boolean(isLeaf));
    }

    protected StaticListTreeNode(TreeNode parent) {
        this(parent, null);
    }

    public Enumeration children() {
        interntalInit();
        return null;
    }

    protected abstract void init();

    public boolean getAllowsChildren() {
        return false;
    }

    private void interntalInit() {
        if (!inited) {
            childTreeNodes = new ArrayList();
            init();
            inited = true;
        }
    }

    protected void add(TreeNode treeNode) {
        if (inited) throw new RuntimeException("TreeNode already inited.  This is meant to be static");
        childTreeNodes.add(treeNode);
    }

    public TreeNode getChildAt(int childIndex) {
        interntalInit();
        return (TreeNode) childTreeNodes.get(childIndex);
    }

    public int getChildCount() {
        interntalInit();
        return childTreeNodes.size();
    }

    public int getIndex(TreeNode node) {
        interntalInit();
        return childTreeNodes.indexOf(node);
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        if (forceLeaf != null) return forceLeaf.booleanValue();

        interntalInit();
        return childTreeNodes.size() == 0;
    }
}
