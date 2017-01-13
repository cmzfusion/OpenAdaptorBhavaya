package org.bhavaya.ui.compass;

import org.bhavaya.ui.*;
import org.bhavaya.ui.compass.CompassTypeHierarchy;
import org.bhavaya.ui.compass.CompassTaxonomyDefinition;
import org.bhavaya.ui.compass.CompassNode;
import org.bhavaya.ui.compass.CompassLaunchTarget;
import org.bhavaya.util.Log;
import org.bhavaya.util.Task;
import org.bhavaya.util.TaskQueue;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class Compass extends JPanel {
    private static final Log log = Log.getCategory(Compass.class);

    private CompassTaxonomyDefinition[] taxonomyDefinitions;
    private CompassLaunchTarget[] targets;
    private JTabbedPane centralTabbedPane;
    private ColumnViewTree columnViewTree;
    private JComponent searchResultsPanel;
    private ArrayListModel searchResultsListModel;
    private ArrayListModel matchingTargetsListModel;
    private JList targetList;
    private SearchResult selectedSearchResult;
    private ListTable resultsTable;
    private JTree correspondingHierarchyTree;
    private CompassRootNode compassRootNode;
    private JPanel noSearchResultSelectedPanel;
    private JScrollPane correspondingHierarchyPanel;
    private CardPanel hierarchyCardPanel;
    private JPanel loadingSearchResultPanel;
    private TaskQueue taskQueue;
    private List postLaunchActions = new ArrayList();
    private Box searchGroupingPanel;
    private JPanel targetPanel;

    public Compass(CompassTaxonomyDefinition[] taxonomyDefinitions, CompassLaunchTarget[] targets) {
        super(new SpringLayout());
        this.taxonomyDefinitions = taxonomyDefinitions;
        this.targets = targets;

        setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel searchLabel = new JLabel("Search");
        final JTextField searchTextField = new JTextField(10);
        searchTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search(((JTextField) e.getSource()).getText());
            }
        });
        searchTextField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (e.getOppositeComponent() != null) {
                    searchTextField.setSelectionStart(0);
                    searchTextField.setSelectionEnd(searchTextField.getText().length());
                }
            }
        });

        searchGroupingPanel = new Box(BoxLayout.X_AXIS);
        searchGroupingPanel.add(searchLabel);
        searchGroupingPanel.add(Box.createHorizontalStrut(3));
        searchGroupingPanel.add(searchTextField);

        columnViewTree = createCategoryBrowser();
        columnViewTree.addSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                fireSelectedSearchResult(createSearchResultFromTreeSelection(e.getNewLeadSelectionPath()));
            }
        });
        searchResultsPanel = createSearchResultsPanel();

        centralTabbedPane = new JTabbedPane();
        centralTabbedPane.addTab("Categories", columnViewTree);
        centralTabbedPane.addTab("Search Results", searchResultsPanel);
        centralTabbedPane.setEnabledAt(1, false);
        centralTabbedPane.addChangeListener(new SelectedTabChangedListener());

        matchingTargetsListModel = new ArrayListModel();
        targetList = new JList(matchingTargetsListModel);
        targetList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                CompassLaunchTarget compassTarget = (CompassLaunchTarget) value;
                label.setIcon(compassTarget.getIcon());
                label.setText(compassTarget.getName());
                return label;
            }
        });
        targetList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    launchSelectedTarget();
                }
            }
        });
        TitlePanel targetLabel = new TitlePanel("Available Views");

        JScrollPane targetListScrollPane = new JScrollPane(targetList);
        Border coolBorder = targetListScrollPane.getBorder();
        targetListScrollPane.setBorder(null);

        targetPanel = new JPanel(new BorderLayout());
        targetPanel.add(targetLabel, BorderLayout.NORTH);
        targetPanel.add(targetListScrollPane, BorderLayout.CENTER);
        targetPanel.setBorder(coolBorder);

        JButton launchButton = new JButton(new LaunchAction());

        add(searchGroupingPanel);
        add(centralTabbedPane);
        add(targetPanel);
        add(launchButton);

        // Layout is just a fraction too complex for other layouts.
        SpringLayout springLayout = (SpringLayout) getLayout();
        springLayout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, launchButton);
        springLayout.putConstraint(SpringLayout.SOUTH, this, 5, SpringLayout.SOUTH, launchButton);
        springLayout.putConstraint(SpringLayout.WEST, searchGroupingPanel, 5, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, searchGroupingPanel, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, centralTabbedPane, 0, SpringLayout.WEST, searchGroupingPanel);
        springLayout.putConstraint(SpringLayout.NORTH, centralTabbedPane, 15, SpringLayout.SOUTH, searchGroupingPanel);
        springLayout.putConstraint(SpringLayout.EAST, targetPanel, 150, SpringLayout.WEST, targetPanel);
        springLayout.putConstraint(SpringLayout.WEST, targetPanel, 5, SpringLayout.EAST, centralTabbedPane);
        springLayout.putConstraint(SpringLayout.SOUTH, targetPanel, 0, SpringLayout.SOUTH, centralTabbedPane);
        springLayout.putConstraint(SpringLayout.NORTH, targetPanel, 21, SpringLayout.NORTH, centralTabbedPane);
        springLayout.putConstraint(SpringLayout.EAST, launchButton, 0, SpringLayout.EAST, targetPanel);
        springLayout.putConstraint(SpringLayout.NORTH, launchButton, 10, SpringLayout.SOUTH, targetPanel);

        taskQueue = new TaskQueue("Compass");
        taskQueue.start();
    }

    private SearchResult createSearchResultFromTreeSelection(TreePath treePath) {
        if (treePath == null || treePath.getLastPathComponent() == null) return null;

        TreeNode treeNode = (TreeNode) treePath.getLastPathComponent();
        if (treeNode instanceof CompassNode) {
            CompassNode sqlTaxonomyNode = (CompassNode) treeNode;
            CompassTypeHierarchy typeHierarchy = sqlTaxonomyNode.getTypeHierarchy();
            return new SearchResult(typeHierarchy, sqlTaxonomyNode.getDescription(),
                    sqlTaxonomyNode.getKey(), 0);
        }
        return null;
    }

    private void fireSelectedSearchResult(SearchResult searchResult) {
        matchingTargetsListModel.clear();
        if (searchResult != null) {
            for (int i = 0; i < targets.length; i++) {
                CompassLaunchTarget target = targets[i];
                if (target.handlesResult(searchResult.getKey(), searchResult.getTypeHierarchy().getType())) {
                    matchingTargetsListModel.add(target);
                }
            }
        }
        selectedSearchResult = searchResult;
        if (log.isDebug())log.debug("User selected: " + searchResult);
    }

    private void search(final String text) {
        if (text.length() == 0) {
            return;
        }

        searchResultsListModel.clear();
        centralTabbedPane.setEnabledAt(1, true);
        centralTabbedPane.setSelectedComponent(searchResultsPanel);

        ArrayList taskList = new ArrayList();
        for (int i = 0; i < taxonomyDefinitions.length; i++) {
            final CompassTaxonomyDefinition taxonomyDefinition = taxonomyDefinitions[i];

            CompassTypeHierarchy typeHierarchy = taxonomyDefinition.getTypeHierarchy();
            do {
                final CompassTypeHierarchy typeHierarchy1 = typeHierarchy;
                taskList.add(new Task("Searching in '" + typeHierarchy1.getCompassTaxonomyDefinition().getName() + "" +
                        ":" + typeHierarchy1.getName() + "'") {
                    public void run() throws Throwable {
                        Collection matchesForString = typeHierarchy1.findMatchesForString(text);
                        for (Iterator iterator = matchesForString.iterator(); iterator.hasNext();) {
                            CompassTypeHierarchy.Result result = (CompassTypeHierarchy.Result) iterator.next();
                            final SearchResult searchResult = new SearchResult(typeHierarchy1, result.getDescription(), result.getKey(), 0);
                            if (typeHierarchy1.isKeyWithinTaxonomy(taxonomyDefinition.getRootParentKey(), searchResult.getKey())) {
                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        searchResultsListModel.add(searchResult);
                                    }
                                });
                            }
                        }
                    }
                });
                typeHierarchy = typeHierarchy.getNextType();
            } while (typeHierarchy != null);
        }

        UIUtilities.runTasksWithProgressDialog(this, "Searching", (Task[]) taskList.toArray(new Task[taskList.size()]),
                Thread.NORM_PRIORITY, true);
    }

    private JComponent createSearchResultsPanel() {
        searchResultsListModel = new ArrayListModel();
        resultsTable = new ListTable(new ListTable.ListModelTableModel(searchResultsListModel,
                new String[]{"categoryName", "resultName"},
                new String[]{"Category", "Result"}), new double[]{0.2d});
        resultsTable.setBackground(Color.white);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, resultsTable.getIntercellSpacing().height));
        resultsTable.setPreferredScrollableViewportSize(new Dimension(450, 195));
        resultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int selectedRow = resultsTable.getSelectedRow();
                SearchResult searchResult = selectedRow > -1 ? (SearchResult) searchResultsListModel.getElementAt(selectedRow) : null;
                fireSelectedSearchResult(searchResult);
                fireCorrespondingHierarchyUpdate(searchResult);
            }
        });

        JScrollPane resultsTableScrollPane = new JScrollPane(resultsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultsTableScrollPane.getViewport().setBackground(Color.white);

        correspondingHierarchyTree = new JTree(new DefaultMutableTreeNode("Root"));
        correspondingHierarchyTree.setToggleClickCount(0);
        correspondingHierarchyTree.setVisibleRowCount(6);
        correspondingHierarchyTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    TreePath path = correspondingHierarchyTree.getPathForLocation(e.getX(), e.getY());
                    columnViewTree.setSelection(createColumnViewTreePathFromCorrespondingHierarchyPath(path));
                    centralTabbedPane.setSelectedIndex(0);
                }
            }
        });

        correspondingHierarchyPanel = new JScrollPane(correspondingHierarchyTree,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JLabel noResultSelectedLabel = new JLabel("No search result selected");
        noResultSelectedLabel.setHorizontalAlignment(JLabel.CENTER);
        noSearchResultSelectedPanel = new JPanel(new BorderLayout());
        noSearchResultSelectedPanel.add(noResultSelectedLabel);

        JTree loadingLabel = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Loading...", true)));
        loadingLabel.setToggleClickCount(0);
        loadingLabel.setVisibleRowCount(6);
        JScrollPane loadingLabelScrollPane = new JScrollPane(loadingLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        loadingSearchResultPanel = new JPanel(new BorderLayout());
        loadingSearchResultPanel.add(loadingLabelScrollPane);

        hierarchyCardPanel = new CardPanel();
        hierarchyCardPanel.addComponent(noSearchResultSelectedPanel);
        hierarchyCardPanel.addComponent(loadingSearchResultPanel);
        hierarchyCardPanel.addComponent(correspondingHierarchyPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, resultsTableScrollPane, hierarchyCardPanel);
        splitPane.resetToPreferredSizes();

        return splitPane;
    }

    /**
     * @param hierarchyPath a path in the <code>correspondingHierarchyTree</code>.
     * @return a path in the <code>columnViewTree</code>
     */
    private TreePath createColumnViewTreePathFromCorrespondingHierarchyPath(TreePath hierarchyPath) {
        return new TreePath(((DefaultMutableTreeNode) hierarchyPath.getLastPathComponent()).getUserObjectPath());
    }

    private void fireCorrespondingHierarchyUpdate(final SearchResult searchResult) {
        if (searchResult == null) {
            hierarchyCardPanel.setSelectedComponent(noSearchResultSelectedPanel);
        } else {
            hierarchyCardPanel.setSelectedComponent(loadingSearchResultPanel);

            Task loadingTask = new Task("Finding parent path") {
                public void run() throws Throwable {
                    TreePath treePath = findTreePathFromKeyPath(createKeyPathFromSearchResult(searchResult));
                    if (log.isDebug())log.debug("User selected search result: " + treePath);

                    final TreeNode rootNode = createTreeNodeBranchFromTreePath(treePath);
                    final TreePath treePathFromHierarchyModel = createTreePathFromHierarchyModel(rootNode);
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            correspondingHierarchyTree.setModel(new DefaultTreeModel(rootNode));
                            correspondingHierarchyTree.expandPath(treePathFromHierarchyModel.getParentPath());
                            hierarchyCardPanel.setSelectedComponent(correspondingHierarchyPanel);
                        }
                    });
                }

                // Force replacement in a set.  All of these tasks are equal.
                public boolean equals(Object obj) {
                    return true;
                }

                public int hashCode() {
                    return 1;
                }
            };
            taskQueue.addTask(loadingTask);
        }
    }

    private TreePath createTreePathFromHierarchyModel(TreeNode rootNode) {
        ArrayList nodeList = new ArrayList();
        TreeNode childNode = rootNode;
        do {
            nodeList.add(childNode);
            childNode = !childNode.isLeaf() ? childNode.getChildAt(0) : null;
        } while (childNode != null);
        return new TreePath(nodeList.toArray(new Object[nodeList.size()]));
    }

    private TreeNode createTreeNodeBranchFromTreePath(TreePath treePath) {
        TreeNode node = (TreeNode) treePath.getLastPathComponent();

        // Create a collection of treeNodes that contain the treeNodes from category browser.
        ArrayList nodePath = new ArrayList();
        DefaultMutableTreeNode lastNode = null;
        do {
            DefaultMutableTreeNode branchNode = new DefaultMutableTreeNode(node);
            nodePath.add(branchNode);
            if (lastNode != null) branchNode.add(lastNode);
            lastNode = branchNode;
        } while ((node = node.getParent()) != null);

        return lastNode;
    }

    private TreePath findTreePathFromKeyPath(Object[] keyPath) {
//        assert keyPath.length > 1 : "Keypath too short";
        ArrayList nodePath = new ArrayList(keyPath.length);

//        // The first level of the tree contains different nodetype to the rest.
//        CompassTaxonomyDefinitionNode thisDefinitionNode = null;
//        for (int i = 0; i < compassRootNode.getChildCount(); i++) {
//            CompassTaxonomyDefinitionNode definitionNode = (CompassTaxonomyDefinitionNode) compassRootNode.getChildAt(i);
//            if (definitionNode.taxonomyDefinition == keyPath[0]) {
//                thisDefinitionNode = definitionNode;
//                break;
//            }
//        }
//        nodePath.add(thisDefinitionNode);
//        TreeNode parent = thisDefinitionNode;

        // Now the rest of the path.
        TreeNode parent = compassRootNode;
        for (int i = 0; i < keyPath.length; i++) {
            CompassNode childNode = findCompassNodeFromParent(parent, keyPath[i]);
            if (childNode == null)
                throw new RuntimeException("Cannot find child.  Search results probably returned a result" +
                        " at the leaf node level which isn't actually in the tree.  This could be caused by mismatching criteria " +
                        "in search sql and the getchild sql.");
            nodePath.add(childNode);
            parent = childNode;
        }
        return new TreePath(nodePath.toArray(new Object[nodePath.size()]));
    }

    private CompassNode findCompassNodeFromParent(TreeNode parent, Object key) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            CompassNode compassNode = (CompassNode) parent.getChildAt(i);
            if (compassNode.getKey().equals(key)) {
                return compassNode;
            }
        }
        return null;
    }

    private Object[] createKeyPathFromSearchResult(SearchResult searchResult) {
        Object nodeKey = searchResult.getKey();
        CompassTypeHierarchy typeHierarchy = searchResult.getTypeHierarchy();
        ArrayList keyPathList = new ArrayList();
        while (typeHierarchy != null) {
            Object parentKey = typeHierarchy.findParentKeyOfThisType(nodeKey);

            if (parentKey != null) {
                if (Utilities.equals(typeHierarchy.getCompassTaxonomyDefinition().getRootParentKey(), nodeKey)) break;

                keyPathList.add(nodeKey);
                nodeKey = parentKey;
            } else {
                typeHierarchy = typeHierarchy.getPrevType();
                parentKey = typeHierarchy != null ? typeHierarchy.findParentKeyOfNextType(nodeKey) : null;
                if (parentKey != null) {
                    if (Utilities.equals(typeHierarchy.getCompassTaxonomyDefinition().getRootParentKey(), nodeKey)) break;

                    keyPathList.add(nodeKey);
                    nodeKey = parentKey;
                }
            }
        }
        keyPathList.add(searchResult.typeHierarchy.getCompassTaxonomyDefinition().getRootParentKey());
        Collections.reverse(keyPathList);
        return keyPathList.toArray(new Object[keyPathList.size()]);
    }

    private ColumnViewTree createCategoryBrowser() {
        compassRootNode = new CompassRootNode(taxonomyDefinitions);
        return new ColumnViewTree(new DefaultTreeModel(compassRootNode));
    }

    public void setDisposeOnLaunch(final Window window) {
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        addPostLaunchAction(new Runnable() {
            public void run() {
                window.dispose();
                dispose();
            }
        });
    }

    public void addPostLaunchAction(Runnable runnable) {
        this.postLaunchActions.add(runnable);
    }

    public boolean removePostLaunchAction(Runnable runnable) {
        return this.postLaunchActions.remove(runnable);
    }

    public Box getSearchGroupingPanel() {
        return searchGroupingPanel;
    }

    public JPanel getTargetPanel() {
        return targetPanel;
    }

    public JTabbedPane getCentralTabbedPane() {
        return centralTabbedPane;
    }

    public class LaunchAction extends AuditedAbstractAction {
        public LaunchAction() {
            super("Launch", "Launch Compass Target");
        }

        public void auditedActionPerformed(ActionEvent e) {
            launchSelectedTarget();
        }
    }

    private void launchSelectedTarget() {
        CompassLaunchTarget launchTarget = (CompassLaunchTarget) targetList.getSelectedValue();
        if (launchTarget == null && targets.length == 1) {
            launchTarget = targets[0];
        }
        if (launchTarget != null && selectedSearchResult != null) {
            launchTarget.launch(selectedSearchResult.getKey(), selectedSearchResult.getResultName(),
                    selectedSearchResult.getTypeHierarchy().getType());
        }

        Iterator iterator = postLaunchActions.iterator();
        while (iterator.hasNext()) {
            ((Runnable) iterator.next()).run();
        }
    }

    protected void dispose() {
        taskQueue.dispose();
    }

    public static class CompassRootNode extends StaticListTreeNode {
        private CompassTaxonomyDefinition[] taxonomyDefinitions;

        public CompassRootNode(CompassTaxonomyDefinition[] taxonomyDefinitions) {
            super(null);
            this.taxonomyDefinitions = taxonomyDefinitions;
        }

        protected void init() {
            for (int i = 0; i < taxonomyDefinitions.length; i++) {
                CompassTaxonomyDefinition taxonomyDefinition = taxonomyDefinitions[i];
                add(new CompassNode(this, taxonomyDefinition.getRootParentKey(), taxonomyDefinition.toString(), taxonomyDefinition.getTypeHierarchy()));
            }
        }

        public String toString() {
            return "Compass Root";
        }
    }

    public static class SearchResult {
        private String resultName;
        private Object key;
        private int priority;
        private CompassTypeHierarchy typeHierarchy;

        public SearchResult(CompassTypeHierarchy typeHierarchy, String resultName, Object key, int priority) {
            this.typeHierarchy = typeHierarchy;
            this.resultName = resultName;
            this.key = key;
            this.priority = priority;
        }

        public String getResultName() {
            return resultName;
        }

        public Object getKey() {
            return key;
        }

        public int getPriority() {
            return priority;
        }

        public CompassTypeHierarchy getTypeHierarchy() {
            return typeHierarchy;
        }

        public String getCategoryName() {
            return typeHierarchy.getName();
        }

        public String toString() {
            return typeHierarchy.getName() + "/" + resultName;
        }
    }

    private class SelectedTabChangedListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            if (centralTabbedPane.getSelectedComponent() == columnViewTree) {
                TreePath selectedPath = columnViewTree.getSelection();
                fireSelectedSearchResult(selectedPath != null ? createSearchResultFromTreeSelection(selectedPath) : null);
            } else {
                boolean valueSelected = resultsTable.getSelectedRow() >= 0;
                fireSelectedSearchResult((SearchResult) (valueSelected ? searchResultsListModel.getElementAt(resultsTable.getSelectedRow()) : null));
            }
        }
    }

    /**
     * basically intended to be available as an action to perform after launch (by calling addPostLaunchAction)
     */
    private static class DisposeAfterLaunchAction extends AbstractAction {
        private Window disposeAfterLaunch;
        private Compass compass;

        /**
         * @param disposeAfterLaunch the window to dispose after launching. Can be null
         */
        public DisposeAfterLaunchAction(Window disposeAfterLaunch, Compass compass) {
            super("Dispose after launch");
            this.disposeAfterLaunch = disposeAfterLaunch;
            this.compass = compass;
        }

        public void actionPerformed(ActionEvent e) {
            if (disposeAfterLaunch != null) disposeAfterLaunch.dispose();
            compass.dispose();
        }
    }
}
