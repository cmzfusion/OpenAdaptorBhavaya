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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.*;
import org.bhavaya.util.TaskQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Daniel van Enckevort
 * @version $Revision: 1.17 $
 */
public class SearchableBeanPathSelector extends JPanel {

    private static final Log log = Log.getCategory(SearchableBeanPathSelector.class);

    private static final ImageIcon BUSY_ICON = ImageIconCache.getImageIcon("hourglass.gif");
    private static final ImageIcon CLEAR_ICON = ImageIconCache.getImageIcon("clear.png");

    private static boolean quickSearchAcceptSubstring = false;

    private static final int MAX_SEARCH_DEPTH = 7;
    private static final int MAX_PRE_FILTER_SEARCH_RESULTS = 250000;
    private static final HashSet<Integer> navigationKeyCodeSet = new HashSet<Integer>(Arrays.asList(
        new Integer(KeyEvent.VK_ENTER),
        new Integer(KeyEvent.VK_UP), new Integer(KeyEvent.VK_DOWN), new Integer(KeyEvent.VK_LEFT), new Integer(KeyEvent.VK_RIGHT),
        new Integer(KeyEvent.VK_HOME), new Integer(KeyEvent.VK_END),
        new Integer(KeyEvent.VK_PAGE_DOWN), new Integer(KeyEvent.VK_PAGE_UP)));


    private BeanPathSelector beanPathSelector;
    private SearchField searchBox;
    private SearchResults searchResults;
    private CardPanel treeOrList;
    private static final ConcurrentMap<BeanPropertyTreeNode, List<BeanPropertyTreeNode>> FLATTENED_TREES = new ConcurrentHashMap<BeanPropertyTreeNode, List<BeanPropertyTreeNode>>();
    private static final List<BeanPropertyTreeNode> PLACEHOLDER = Collections.unmodifiableList(new ArrayList<BeanPropertyTreeNode>());
    private SearchResultsTreeModel searchResultsModel;
    private JTextField dummySearchBox;
    private CardPanel textfieldCardPanel;
    private JScrollPane treeScrollPane;
    private JScrollPane listScrollPane;

    private NarrowerTask currentNarrowerTask;
    private TaskQueue narrowerTaskQueue;
    private List<java.util.regex.Pattern> restrictionPatterns;
    private JLabel hourGlassLabel = new JLabel((Icon)null);
    JPanel treePanel;

    public SearchableBeanPathSelector(Class beanType, FilteredTreeModel.Filter addPropertyFilter, FilteredTreeModel.Filter addChildrenFilter, BeanPathSelector.SelectionModel selectionModel) {
        this.restrictionPatterns = new ArrayList<java.util.regex.Pattern>();
        this.beanPathSelector = new BeanPathSelector(beanType, addPropertyFilter, addChildrenFilter, selectionModel) {
            protected void processKeyEvent(KeyEvent e) {
                if (!processSelectorKeyEvent(e)) {
                    super.processKeyEvent(e);
                }
            }
        };
        init(selectionModel);
    }

    public SearchableBeanPathSelector(BeanCollectionTableModel beanCollectionTableModel, FilteredTreeModel.Filter addPropertyFilter, FilteredTreeModel.Filter addChildrenFilter, BeanPathSelector.SelectionModel selectionModel) {
        this(beanCollectionTableModel, addPropertyFilter, addChildrenFilter, selectionModel, new ArrayList<java.util.regex.Pattern>());
    }

    public SearchableBeanPathSelector(BeanCollectionTableModel beanCollectionTableModel, FilteredTreeModel.Filter addPropertyFilter, FilteredTreeModel.Filter addChildrenFilter, BeanPathSelector.SelectionModel selectionModel, List<java.util.regex.Pattern> restrictionPatterns) {
        this.restrictionPatterns = restrictionPatterns;
        this.beanPathSelector = new BeanPathSelector(beanCollectionTableModel, addPropertyFilter, addChildrenFilter, selectionModel) {
            protected void processKeyEvent(KeyEvent e) {
                if (!processSelectorKeyEvent(e)) {
                    super.processKeyEvent(e);
                }
            }
        };

        beanCollectionTableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getFirstRow() == TableModelEvent.HEADER_ROW && e.getLastRow() == TableModelEvent.HEADER_ROW) {
                    searchResults.repaint();
                }
            }
        });
        init(selectionModel);
    }

    private void init(BeanPathSelector.SelectionModel selectionModel) {
        setLayout(new BorderLayout());

        dummySearchBox = new JTextField("Quick Search");
        dummySearchBox.setForeground(Color.lightGray);
        dummySearchBox.setMaximumSize(new Dimension(dummySearchBox.getMaximumSize().width, dummySearchBox.getPreferredSize().height));

        searchBox = new SearchField();
        searchBox.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                doNarrow();
            }

            public void removeUpdate(DocumentEvent e) {
                doNarrow();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
        searchBox.setMaximumSize(new Dimension(searchBox.getMaximumSize().width, searchBox.getPreferredSize().height));

        JButton clearButton = new JButton(new ClearAction());
        clearButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        clearButton.setFocusable(false);

        textfieldCardPanel = new CardPanel();
        textfieldCardPanel.addComponent(dummySearchBox);
        textfieldCardPanel.addComponent(searchBox);
        textfieldCardPanel.setSelectedComponent(dummySearchBox);
        dummySearchBox.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                textfieldCardPanel.setSelectedComponent(searchBox);
                searchBox.requestFocusInWindow();
            }
        });
        searchBox.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary() && searchBox.getText().length() == 0) {
                    textfieldCardPanel.setSelectedComponent(dummySearchBox);
                }
            }
        });

        treeOrList = new CardPanel();
        searchResultsModel = new SearchResultsTreeModel();
        searchResults = new SearchResults();

        searchResults.setCellRenderer(new SearchResultsTreeCellRenderer(selectionModel, searchResultsModel));
        searchResults.addMouseListener(new BeanPathSelector.BeanPropertyTreeMouseHandler(selectionModel));

        treeScrollPane = new JScrollPane(beanPathSelector);
        listScrollPane = new JScrollPane(searchResults);

        treePanel = new JPanel();
        final JPanel breadPanel = new JPanel();
        breadPanel.add(new JLabel("  "));
        breadPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
        treePanel.setLayout(new BorderLayout());
        treePanel.add(breadPanel, BorderLayout.NORTH);
        treePanel.add(treeScrollPane);

        beanPathSelector.addTreeSelectionListener(new TreeSelectionListener(){
            public void valueChanged(TreeSelectionEvent e) {
                TreePath tp = e.getNewLeadSelectionPath();
                buildBreadcrumbs(tp, breadPanel);
            }
        });
        beanPathSelector.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath tp = event.getPath();
                buildBreadcrumbs(tp, breadPanel);
            }
            public void treeCollapsed(TreeExpansionEvent event) {
                TreePath tp = event.getPath();
                buildBreadcrumbs(tp, breadPanel);
            }
        });


        treeOrList.addComponent(treePanel);
        treeOrList.addComponent(listScrollPane);
        treeOrList.setSelectedComponent(treePanel);

        Box textFieldAndClearButton = new Box(BoxLayout.X_AXIS);
        textFieldAndClearButton.add(textfieldCardPanel);
        textFieldAndClearButton.add(clearButton);
        textFieldAndClearButton.add(hourGlassLabel);

        add(textFieldAndClearButton, BorderLayout.NORTH);
        add(treeOrList, BorderLayout.CENTER);

    }

    void buildBreadcrumbs(TreePath tp, JPanel breadPanel){

        breadPanel.removeAll();
        if(tp == null){
           return;
        }
        for(Object obj : tp.getPath()){
            Breadcrumb bc = new Breadcrumb(beanPathSelector, (DefaultMutableTreeNode)obj);
            breadPanel.add(bc);
        }
        treePanel.doLayout();
        treePanel.invalidate();
        treePanel.validate();
        treePanel.repaint();
    }

    private boolean processSelectorKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED && isValidPropertyChar(e.getKeyChar())) {
            searchBox.replaceSelection("" + e.getKeyChar());
            textfieldCardPanel.setSelectedComponent(searchBox);
            searchBox.requestFocusInWindow();
            return true;
        }
        return false;
    }

    public void updateUI() {
        if (searchResults != null) searchResults.setCellRenderer(new SearchResultsTreeCellRenderer(beanPathSelector.getBeanPathSelectionModel(), searchResultsModel));
    }

    private void doNarrow() {
        if (narrowerTaskQueue == null) {
            narrowerTaskQueue = new TaskQueue("BeanPathSelector.Narrower");
            narrowerTaskQueue.start();
        }
        if (currentNarrowerTask != null) currentNarrowerTask.cancel();
        currentNarrowerTask = new NarrowerTask(searchBox.getText().toLowerCase());
        narrowerTaskQueue.addTask(currentNarrowerTask);
    }

    /**
     * Narrows the field list in a background thread.
     */
    private class NarrowerTask extends Task {
        private String searchText;
        boolean cancelSearch = false;
        private static final int TYPING_DELAY = 50;

        public NarrowerTask(String searchText) {
            super("NarrowerTask " + searchText);
            this.searchText = searchText;
        }

        synchronized void cancel() {
            cancelSearch = true;  // cancel current search
        }

        private synchronized boolean checkSearchCancelled() {
            if(cancelSearch) {
                stopProgress();
            }
            return cancelSearch;
        }

        public void run() {
            if (checkSearchCancelled()) return;
            if (searchText.length() == 0) { // just switch to tree view
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        treeOrList.setSelectedComponent(treePanel);
                    }
                });
                return;
            }
            startProgress();
            final List<BeanPropertyTreeNode> filtered = new ArrayList<BeanPropertyTreeNode>();
            for (Iterator iterator = getFlattenedTree().iterator(); iterator.hasNext();) {
                if (checkSearchCancelled()) break;
                BeanPropertyTreeNode property = (BeanPropertyTreeNode) iterator.next();
                if (quickSearchAcceptSubstring) {
                    if (property.getDisplayName().toLowerCase().contains(searchText)) {
                        filtered.add(property);
                    }
                } else {
                    if (property.getDisplayName().toLowerCase().startsWith(searchText)) {
                        filtered.add(property);
                    }
                }
            }
            try {
                /**
                 * Slight delay to give the user time to type another character - GUI update is time costly.
                 */
                Thread.sleep(TYPING_DELAY);
            } catch (InterruptedException e) {
                // ignore
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (checkSearchCancelled()) return;
                    searchResultsModel.setRootChildren(filtered);
                    treeOrList.setSelectedComponent(listScrollPane);
                    searchResults.clearSelection();
                    searchBox.requestFocusInWindow();
                    stopProgress();
                }
            });
        }

        private void startProgress() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    hourGlassLabel.setIcon(BUSY_ICON);
                }
            });
        }

        private void stopProgress() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    hourGlassLabel.setIcon((Icon)null);
                }
            });
        }

        public boolean equals(Object obj) {
            return (obj instanceof NarrowerTask);
        }

        public int hashCode() {
            return 100; // all objects use the same hash code so they replace each other in the task queue
        }
    }

    /*
     * Get the flattened tree from the static map, or create as necessary.
     */
    private List<BeanPropertyTreeNode> getFlattenedTree() {
        BeanPropertyTreeNode root = (BeanPropertyTreeNode) beanPathSelector.getModel().getRoot();
        List<BeanPropertyTreeNode> flattenedTree = FLATTENED_TREES.putIfAbsent(root, PLACEHOLDER);
        if (flattenedTree == null) {
            //No other threads have started the flattening for this class, so we need to flatten the tree
            flattenedTree = flattenTree(root);
            FLATTENED_TREES.put(root, flattenedTree);
        } else if (flattenedTree == PLACEHOLDER) {
            //Tree is currently being flattened by another thread, so wait for it if required
            //Flattening should happen up front so we shouldn't need to wait, but just in case there's a problem set a timeout
            long timeout = System.currentTimeMillis()+30000;
            while (flattenedTree == PLACEHOLDER && System.currentTimeMillis() < timeout) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error(e);
                }
                flattenedTree = FLATTENED_TREES.get(root);
            }
        }
        return flattenedTree;
    }

    /**
     * breadth first walks the beanPathSelector from the given node (non-inclusive), adding nodes to the given searchResults
     *                                                            to
     * @param propertyNode Root property node to flatten from
     * @return List of flattened
     */
    private List<BeanPropertyTreeNode> flattenTree(BeanPropertyTreeNode propertyNode) {
        List<BeanPropertyTreeNode> list = new ArrayList<BeanPropertyTreeNode>();
        int nextListItem = -1;
        while (nextListItem < list.size() && list.size() < MAX_PRE_FILTER_SEARCH_RESULTS) {
            if (nextListItem > -1) propertyNode = list.get(nextListItem);

            if (propertyNode.getLevel() >= MAX_SEARCH_DEPTH) break;
            int count = beanPathSelector.getModel().getChildCount(propertyNode);
            for (int i = 0; i < count; i++) {
                BeanPropertyTreeNode child = (BeanPropertyTreeNode)beanPathSelector.getModel().getChild(propertyNode, i);
                String beanPath = child.getBeanPath();
                if ( ! matchesRestrictionPatterns(beanPath) ) {
                    list.add(child);
                }
            }
            nextListItem++;
        }

        if ( list.size() >= MAX_PRE_FILTER_SEARCH_RESULTS) {
            log.warn("Tree search resulted in " + list.size() + " beanpath nodes, you probably need to set a restriction pattern to suppress any cycles in the tree");
        }
        return list;
    }

    public void selectPath(String path) {
        beanPathSelector.clearSelection();
        TreePath treePath = findTreePath(path);
        if(treePath != null) {
            beanPathSelector.setSelectionPath(findTreePath(path));
            setSelected(true);
        }
    }

    private TreePath findTreePath(String path) {
        if(path == null || path.length() == 0) {
            return null;
        }
        BeanPropertyTreeNode propertyNode = (BeanPropertyTreeNode) beanPathSelector.getModel().getRoot();
        List<BeanPropertyTreeNode> treePath = new ArrayList<BeanPropertyTreeNode>();
        treePath.add(propertyNode);
        String[] pathElements = path.split("\\.");
        String subPath = "";
        for(int i=0; i<pathElements.length; i++) {
            subPath = subPath + (i>0 ? "." : "") + pathElements[i];
            propertyNode = findChild(propertyNode, subPath);
            if(propertyNode == null) {
                log.warn("Path "+path+" is invalid");
                return null;
            }
            treePath.add(propertyNode);
        }
        return new TreePath(treePath.toArray());
    }

    private BeanPropertyTreeNode findChild(BeanPropertyTreeNode node, String path) {
        int count = beanPathSelector.getModel().getChildCount(node);
        for (int i = 0; i < count; i++) {
            BeanPropertyTreeNode child = (BeanPropertyTreeNode)beanPathSelector.getModel().getChild(node, i);
            String beanPath = child.getBeanPath();
            if(path.equals(beanPath)) {
                return child;
            }
        }
        return null;
    }

    private boolean matchesRestrictionPatterns(String beanPath) {
        boolean matched = false;
        for ( java.util.regex.Pattern pattern : restrictionPatterns) {
            //the surrounding . are a work around for suspected bug with reg exp groups (or maybe its just my bad reg exp..). at any rate it makes things easier
            if ( pattern.matcher("." + beanPath + ".").find() ) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    public void dispose() {
        beanPathSelector.dispose();
    }

    public void reset() {
        beanPathSelector.reset();
    }

    public void setSelected(boolean add) {
        beanPathSelector.setSelected(add);
    }

    private static boolean isNavigationKey(KeyEvent e) {
        int keyCode = e.getKeyCode();
        return navigationKeyCodeSet.contains(new Integer(keyCode));
    }

    public static boolean isQuickSearchAcceptSubstring() {
        return quickSearchAcceptSubstring;
    }

    public static void setQuickSearchAcceptSubstring(boolean quickSearchAcceptSubstring) {
        SearchableBeanPathSelector.quickSearchAcceptSubstring = quickSearchAcceptSubstring;
    }

    private boolean isValidPropertyChar(char c) {
        return Character.isLetterOrDigit(c)
                || Character.isSpaceChar(c)
                || c == '_'
                || c == '('
                || c == ')';
    }

    private class SearchResults extends JTree {
        private PropertyToolTipFactory propertyToolTipFactory = new BeanpathSelectorPropertyToolTipFactory();

        public SearchResults() {
            super(searchResultsModel);
            setRootVisible(false);
            setShowsRootHandles(true);
            putClientProperty("JTree.lineStyle", "None");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        protected void processKeyEvent(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) {
                TreePath selectionPath = getSelectionPath();
                if (selectionPath != null) {
                    BeanPropertyTreeNode property = (BeanPropertyTreeNode) selectionPath.getLastPathComponent();
                    if (!property.isPropertyGroup() && PropertyModel.getInstance(property.getAttribute().getType()).isSelectable()) {
                        beanPathSelector.getBeanPathSelectionModel().locatorSelected(property.getBeanPath());
                        return;
                    }

                    if (isExpanded(selectionPath)) {
                        collapsePath(selectionPath);
                    } else {
                        expandPath(selectionPath);
                    }
                }
            } else {
                super.processKeyEvent(e);
            }
        }

        public String getToolTipText(MouseEvent event) {
            return propertyToolTipFactory.getToolTipText(event);
        }

        public JToolTip createToolTip() {
            return propertyToolTipFactory.createToolTip();
        }

        private class BeanpathSelectorPropertyToolTipFactory extends PropertyToolTipFactory {
            public String getToolTipText(MouseEvent event) {
                TreePath path = getPathForLocation(event.getX(), event.getY());
                if (path != null && path.getPathCount() > 1) {
                    BeanPropertyTreeNode property = ((BeanPropertyTreeNode) path.getLastPathComponent());
                    BeanPropertyTreeNode rootProperty = property.getRootProperty();
                    return getToolTipText(rootProperty.getAttribute().getType(), property.getBeanPath());
                }
                return null;
            }
        }
    }

    private class SearchField extends JTextField {
        protected void processKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED && isNavigationKey(e)) {
                Component selectedComponent = treeOrList.getSelectedComponent();
                if (selectedComponent == listScrollPane) {
                    searchResults.processKeyEvent(e);
                } else if (selectedComponent == treeScrollPane) {
                    beanPathSelector.processKeyEvent(e);
                }
            } else {
                super.processKeyEvent(e);
            }
        }
    }


    private class SearchResultsTreeModel implements TreeModel {
        private IndexedSet root = new IndexedSet();
        private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

        public void setRootChildren(Collection children) {
            root.clear();
            root.addAll(children);
            fireModelChanged();
        }

        public Object getChild(Object parent, int index) {
            if (parent == root) return root.get(index);
            return beanPathSelector.getModel().getChild(parent, index);
        }

        public int getChildCount(Object parent) {
            if (parent == root) return root.size();
            return beanPathSelector.getModel().getChildCount(parent);
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == root) return root.indexOf(child);
            return beanPathSelector.getModel().getIndexOfChild(parent, child);
        }

        public Object getRoot() {
            return root;
        }

        public boolean isLeaf(Object node) {
            if (node == root) return root.size() == 0;
            return beanPathSelector.getModel().isLeaf(node);
        }

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        private void fireModelChanged() {
            TreeModelEvent e = new TreeModelEvent(this, new Object[]{root});
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                TreeModelListener l = (TreeModelListener) iterator.next();
                l.treeStructureChanged(e);
            }
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        public boolean isTopLevelChild(Object value) {
            return root.contains(value);
        }
    }

    private static class SearchResultsTreeCellRenderer extends BeanPathSelector.PropertyTreeCellRenderer {
        private SearchResultsTreeModel model;

        public SearchResultsTreeCellRenderer(BeanPathSelector.SelectionModel beanPathSelectionModel, SearchResultsTreeModel model) {
            super(beanPathSelectionModel);
            this.model = model;
        }

        protected String getStringForValue(Object value) {
            BeanPropertyTreeNode property = (BeanPropertyTreeNode) value;

            if (model.isTopLevelChild(value)) {
                StringBuilder itemString = new StringBuilder();
                BeanPropertyTreeNode[] pathFromRoot = property.getPropertyPathFromRoot();

                for (int i = 1; i < pathFromRoot.length; i++) {
                    BeanPropertyTreeNode parentProperty = pathFromRoot[i];
                    itemString.append(parentProperty.getDisplayName());
                    if (i < pathFromRoot.length - 1) itemString.append(" - ");
                }
                return itemString.toString();
            } else {
                return property.getDisplayName();
            }
        }
    }

    private class ClearAction extends AuditedAbstractAction {
        public ClearAction() {
            super("", null, "Clear searchable box");
            putValue(Action.SMALL_ICON, CLEAR_ICON);
        }

        public void auditedActionPerformed(ActionEvent e) {
            searchBox.setText("");
            textfieldCardPanel.setSelectedComponent(dummySearchBox);
        }
    }
}
