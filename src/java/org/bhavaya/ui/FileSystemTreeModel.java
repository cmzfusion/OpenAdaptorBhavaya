package org.bhavaya.ui;

import org.bhavaya.util.Utilities;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.Enumeration;

/**
 * Useful for testing trees.  And may actually be useful to someone.  Started looking on the net but gave up after half
 * and hour of trawling through overly complex code.
 */
public class FileSystemTreeModel extends DefaultTreeModel {
    public FileSystemTreeModel(String path) {
        this(new File(path));
    }

    public FileSystemTreeModel(File root) {
        super(new FileNode(root, null));
    }

    public static class FileNode implements TreeNode {
        private File file;
        private FileNode parent;
        private File[] subdirectories;
        private FileNode[] children;
        private boolean inited = false;

        public FileNode(File file, FileNode parent) {
            this.file = file;
            this.parent = parent;
            this.subdirectories = file.listFiles();
        }

        public int getChildCount() {
            return subdirectories == null ? 0 : subdirectories.length;
        }

        private void init() {
            if (!inited) {
                children = new FileNode[subdirectories == null ? 0 : subdirectories.length];
                for (int i = 0; i < children.length; i++) {
                    File subdirectory = subdirectories[i];
                    children[i] = new FileNode(subdirectory, this);
                }
                inited = true;
            }
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public boolean isLeaf() {
            return subdirectories == null || subdirectories.length == 0;
        }

        public Enumeration children() {
            init();
            return new ArrayEnumeration(children);
        }

        public TreeNode getParent() {
            return parent;
        }

        public TreeNode getChildAt(int childIndex) {
            init();
            return children[childIndex];
        }

        public int getIndex(TreeNode node) {
            return Utilities.identityIndexOf(children, node);
        }

        public String toString() {
            return file.getName();
        }
    }
}
