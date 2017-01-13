package org.bhavaya.ui.dataset;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.collection.ListEvent;
import org.bhavaya.ui.TitlePanel;
import org.bhavaya.util.Describeable;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Abstract base class for BeanCollectionSelectors.  Handles high level logic and
 * allows extending classes to implement their own actions for launching collections.
 *
 * @author James Langley
 * @version $Revision: 1.7 $
 */
public abstract class AbstractBeanCollectionSelector extends JPanel {
    protected static final String ROOT = "Collections";
    private static final String ACTIVE_LIST_ELEMENT_ICON = "active_list_element.gif";
    private JTree beanCollectionTree;
    protected java.util.List beanCollectionGroups;
    private JToolBar toolBar;
    private String selectorName;

    public AbstractBeanCollectionSelector() {
        this("BeanCollectionSelector");
    }

    public AbstractBeanCollectionSelector(String selectorName) {
        super(new BorderLayout());
        this.selectorName = selectorName;
        beanCollectionGroups = Arrays.asList(BeanCollectionGroup.getEnabledInstances());
        beanCollectionTree = new JTree(new BeanCollectionTreeModel(beanCollectionGroups));
        beanCollectionTree.setName(selectorName + "-JTree");
        ToolTipManager.sharedInstance().registerComponent(beanCollectionTree);
        ensureExpanded();
        beanCollectionTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        beanCollectionTree.setCellRenderer(new AbstractBeanCollectionSelector.TreeCellRenderer());
        beanCollectionTree.addMouseListener(new AbstractBeanCollectionSelector.MouseHandler(beanCollectionTree));
        beanCollectionTree.setToggleClickCount(0);
        JScrollPane listScrollPane = new JScrollPane(beanCollectionTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);

        TitlePanel titlePanel = new TitlePanel("Data Collections");
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(toolBar, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(listScrollPane, BorderLayout.CENTER);
        setName(selectorName);
    }

    protected void addToolbarButton(JButton button) {
        toolBar.add(button);
    }

    protected JTree getBeanCollectionTree() {
        return beanCollectionTree;
    }

    private void ensureExpanded() {
        for (Iterator iterator = beanCollectionGroups.iterator(); iterator.hasNext();) {
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) iterator.next();
            beanCollectionTree.expandPath(new TreePath(new Object[]{ROOT, beanCollectionGroup}));
        }
    }

    public void updateUI() {
        super.updateUI();
        if (beanCollectionTree != null) beanCollectionTree.setCellRenderer(new TreeCellRenderer());
    }

    /**
     * Called by the base class to get actions for the right click popup menu for a particular
     * BeanCollectionGroup.
     *
     * @param popupMenu
     * @param beanCollectionGroup
     */
    protected abstract void addActionsForBeanCollectionGroup(JPopupMenu popupMenu, BeanCollectionGroup beanCollectionGroup);

    protected abstract void addActionsForBeanCollection(JPopupMenu popupMenu, TreePath location);

    protected abstract void beanCollectionGroupDoubleClicked(BeanCollectionGroup beanCollectionGroup);

    protected abstract void beanCollectionDoubleClicked(BeanCollection beanCollection);

    protected static class BeanCollectionTreeModel implements TreeModel {
        private java.util.List listeners;
        private java.util.List beanCollectionGroups;

        public BeanCollectionTreeModel(java.util.List enabledBeanCollectionGroupList) {
            listeners = new ArrayList();

            this.beanCollectionGroups = enabledBeanCollectionGroupList;
            for (Iterator iterator = enabledBeanCollectionGroupList.iterator(); iterator.hasNext();) {
                final BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) iterator.next();
                beanCollectionGroup.addCollectionListener(new AbstractBeanCollectionSelector.BeanCollectionListener(this, beanCollectionGroup));
            }
        }

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        public Object getRoot() {
            return AbstractBeanCollectionSelector.ROOT;
        }

        public Object getChild(Object parent, int index) {
            if (parent == AbstractBeanCollectionSelector.ROOT) {
                return beanCollectionGroups.get(index);
            } else if (parent instanceof BeanCollectionGroup) {
                return ((BeanCollectionGroup) parent).get(index);
            } else {
                return null;
            }
        }

        public int getChildCount(Object parent) {
            if (parent == AbstractBeanCollectionSelector.ROOT) {
                return beanCollectionGroups.size();
            } else if (parent instanceof BeanCollectionGroup) {
                return ((BeanCollectionGroup) parent).size();
            } else {
                return 0;
            }
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == AbstractBeanCollectionSelector.ROOT) {
                return beanCollectionGroups.indexOf(child);
            } else if (parent instanceof BeanCollectionGroup) {
                return ((BeanCollectionGroup) parent).indexOf(child);
            } else {
                return 0;
            }
        }

        public boolean isLeaf(Object node) {
            return node instanceof BeanCollection && !(node instanceof BeanCollectionGroup);
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            // Not implemented.
        }

    }

    private static class BeanCollectionListener implements CollectionListener {
        private BeanCollectionTreeModel model;
        private BeanCollection beanCollection;

        public BeanCollectionListener(BeanCollectionTreeModel model, BeanCollection beanCollection) {
            this.model = model;
            this.beanCollection = beanCollection;
        }

        public void collectionChanged(ListEvent e) {
            for (Iterator iterator = model.listeners.iterator(); iterator.hasNext();) {
                TreeModelListener listener = (TreeModelListener) iterator.next();
                if (e.getType() == ListEvent.COMMIT) {
                    TreeModelEvent event = new TreeModelEvent(model, new TreePath(new Object[]{AbstractBeanCollectionSelector.ROOT, beanCollection}));
                    listener.treeStructureChanged(event);
                }
            }
        }
    }

    private static class TreeCellRenderer extends DefaultTreeCellRenderer {
        private Icon activeListElementIcon = ImageIconCache.getImageIcon(ACTIVE_LIST_ELEMENT_ICON);

        public TreeCellRenderer() {
            super();
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            label.setToolTipText(null);

            if (value instanceof BeanCollection && !(value instanceof BeanCollectionGroup)) {
                label.setIcon(activeListElementIcon);

                if (value instanceof Describeable) {
                    Describeable describeable = (Describeable) value;
                    String tooltipText = describeable.getDescription();
                    if (tooltipText != null) {
                        tooltipText = Utilities.wrapWithSplitOnNewLine(tooltipText, 80);
                        label.setToolTipText("<HTML>" + tooltipText.replaceAll("\n", "<BR>") + "</HTML>");
                    }
                }
            }

            return label;
        }
    }

    private class MouseHandler extends MouseAdapter {
        private JTree tree;

        public MouseHandler(JTree tree) {
            this.tree = tree;
        }

        public void mousePressed(MouseEvent e) {
            checkPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            checkPopup(e);
        }

        private void checkPopup(MouseEvent e) {
            TreePath location = tree.getPathForLocation(e.getX(), e.getY());
            if (location != null) {
                Object o = location.getLastPathComponent();

                if (e.isPopupTrigger()) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    popupMenu.setName(selectorName + "-JPopupMenu");
                    if (o instanceof BeanCollectionGroup) {
                        BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) o;
                        addActionsForBeanCollectionGroup(popupMenu, beanCollectionGroup);
                    } else {
                        if (o instanceof BeanCollection) {
                            tree.setSelectionPath(location);
                            addActionsForBeanCollection(popupMenu, location);
                        }
                    }
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
                    }
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
            TreePath location = tree.getPathForLocation(e.getX(), e.getY());
            if (location != null) {
                Object o = location.getLastPathComponent();

                // Double left-click
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    if (o instanceof BeanCollectionGroup) {
                        beanCollectionGroupDoubleClicked((BeanCollectionGroup) o);
                    } else if (o instanceof BeanCollection) {
                        beanCollectionDoubleClicked((BeanCollection) o);
                    }
                }
            }
        }
    }
}
