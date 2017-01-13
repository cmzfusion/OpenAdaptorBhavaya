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

package org.bhavaya.ui.dataset;

import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.TableViewConfigurationMap;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.TableView;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

/**
 * ----------------------------------
 * notes Nick April 08
 * The edit bean collection functionality was causing issues in which criteria were lost -
 * When an edit takes place, existing views are now updated with a clone of the source collection.
 * This is necessary because some views/view contexts provide controls which can be used to
 * apply additional criteria to the collection, on a per-view-instance basis.
 * ----------------------------------
 *
 * @author Brendon McLean
 * @version $Revision: 1.17 $
 */
public class BeanCollectionSelector extends AbstractBeanCollectionSelector {
    private static final Log log = Log.getCategory(BeanCollectionSelector.class);

    private static final String ADD_ICON = "add.gif";
    private static final String REMOVE_ICON = "remove.gif";
    private static final String EDIT_ICON = "edit.gif";
    private static final String RUN_ICON = "run.gif";

    public BeanCollectionSelector() {
        JButton addButton = new JButton(new AddFromToolbarAction(getBeanCollectionTree(), beanCollectionGroups));
        addButton.setFocusable(false);
        addButton.setToolTipText("Add New Data Collection");

        JButton removeButton = new JButton(new RemoveAction(false, getBeanCollectionTree()));
        removeButton.setFocusable(false);
        removeButton.setToolTipText("Remove Data Collection");

        JButton editButton = new JButton(new EditAction(false, getBeanCollectionTree()));
        editButton.setFocusable(false);
        editButton.setToolTipText("Edit Data Collection");

        JButton runButton = new JButton(new RunAction(false, getBeanCollectionTree()));
        runButton.setText(null);
        runButton.setFocusable(false);
        runButton.setToolTipText("View Data Collection");

        addToolbarButton(addButton);
        addToolbarButton(removeButton);
        addToolbarButton(editButton);
        addToolbarButton(runButton);
    }

    protected void addActionsForBeanCollectionGroup(JPopupMenu popupMenu, BeanCollectionGroup beanCollectionGroup) {
        popupMenu.add(new BeanCollectionSelector.AddAction(beanCollectionGroup, getBeanCollectionTree()));
        popupMenu.add(new BeanCollectionSelector.SetDefaultViewConfiguration(beanCollectionGroup, getBeanCollectionTree()));
        popupMenu.add(new BeanCollectionSelector.ViewAllAction(beanCollectionGroup));
    }

    protected void addActionsForBeanCollection(JPopupMenu popupMenu, TreePath location) {
        Object o = location.getLastPathComponent();
        popupMenu.add(new BeanCollectionSelector.RunAction(true, getBeanCollectionTree()));
        popupMenu.add(new BeanCollectionSelector.EditAction(true, getBeanCollectionTree()));
        popupMenu.add(new BeanCollectionSelector.RemoveAction(true, getBeanCollectionTree()));
        BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) location.getPathComponent(location.getPathCount() - 2);
        JMenu copyToMenu = getCopyToActions(beanCollectionGroup, (BeanCollection) o);
        if (copyToMenu != null) popupMenu.add(copyToMenu);
        popupMenu.add(new BeanCollectionSelector.ExportAction(getBeanCollectionTree()));
        popupMenu.addSeparator();
        addActionsForBeanCollectionGroup(popupMenu, beanCollectionGroup);
    }

    protected void beanCollectionGroupDoubleClicked(BeanCollectionGroup beanCollectionGroup) {
        new BeanCollectionSelector.AddAction(beanCollectionGroup, getBeanCollectionTree()).actionPerformed(new ActionEvent(this, 0, Action.ACTION_COMMAND_KEY));
    }

    protected void beanCollectionDoubleClicked(BeanCollection beanCollection) {
        new BeanCollectionSelector.RunAction(false, getBeanCollectionTree()).actionPerformed(new ActionEvent(this, 0, Action.ACTION_COMMAND_KEY));
    }

    private JMenu getCopyToActions(BeanCollectionGroup fromBeanCollectionGroup, BeanCollection fromCollection) {
        JMenu copyTo = new JMenu("Copy to");
        if (!(fromCollection instanceof CriteriaBeanCollection)) return null;

        CriteriaBeanCollection criteriaBeanCollection = (CriteriaBeanCollection) fromCollection;

        BeanCollectionGroup[] beanCollectionGroups = BeanCollectionGroup.getEnabledInstances();

        for (int i = 0; i < beanCollectionGroups.length; i++) {
            BeanCollectionGroup beanCollectionGroup = beanCollectionGroups[i];

            if (beanCollectionGroup != fromBeanCollectionGroup && CriteriaBeanCollection.class.isAssignableFrom(beanCollectionGroup.getCollectionType())) {
                Class beanType = beanCollectionGroup.getBeanType();
                CriterionGroup primaryCriteria = criteriaBeanCollection.getPrimaryCriteria();
                if (primaryCriteria.isValidForBeanType(beanType)) {
                    BeanCollection beanCollection = beanCollectionGroup.newBeanCollection(primaryCriteria);
                    if (beanCollection != null) {
                        copyTo.add(new BeanCollectionSelector.CopyToTypeAction(beanCollectionGroup, beanCollection));
                    }
                }
            }
        }

        if (copyTo.getSubElements().length == 0) return null;
        return copyTo;
    }

    private static class CopyToTypeAction extends AuditedAbstractAction {
        private BeanCollectionGroup toBeanCollectionGroup;
        private BeanCollection beanCollection;

        public CopyToTypeAction(BeanCollectionGroup toBeanCollectionGroup, BeanCollection beanCollection) {
            this.toBeanCollectionGroup = toBeanCollectionGroup;
            this.beanCollection = beanCollection;
            putValue(Action.NAME, toBeanCollectionGroup.getPluralDisplayName());
        }

        public void auditedActionPerformed(ActionEvent e) {
            toBeanCollectionGroup.add(beanCollection);
        }
    }

    private static class AddFromToolbarAction extends AuditedAbstractAction {
        private JTree tree;
        private List beanCollectionGroups;

        public AddFromToolbarAction(JTree tree, List beanCollectionGroups) {
            this.tree = tree;
            this.beanCollectionGroups = beanCollectionGroups;
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(ADD_ICON));
            setEnabled(isEnabled());
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            // If there is only one type, then just go straight to the dialog.
            if (beanCollectionGroups.size() == 1) {
                new AddAction((BeanCollectionGroup) beanCollectionGroups.get(0), tree).actionPerformed(e);
            }
            // Else choose the one using the selection
            else {
                TreePath path = tree.getSelectionPath();

                BeanCollectionGroup beanCollectionGroup;
                if (path.getLastPathComponent() instanceof BeanCollectionGroup) {
                    beanCollectionGroup = (BeanCollectionGroup) path.getLastPathComponent();
                } else if (path.getParentPath().getLastPathComponent() instanceof BeanCollectionGroup) {
                    beanCollectionGroup = (BeanCollectionGroup) path.getParentPath().getLastPathComponent();
                } else {
                    return;
                }
                new AddAction(beanCollectionGroup, tree).actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            TreePath path = tree.getSelectionPath();
            return path != null && (path.getLastPathComponent() instanceof BeanCollectionGroup || path.getLastPathComponent() instanceof BeanCollection);
        }
    }

    private static class AddAction extends AuditedAbstractAction {
        private JTree tree;
        private BeanCollectionGroup beanCollectionGroup;

        public AddAction(BeanCollectionGroup beanCollectionGroup, JTree tree) {
            this.tree = tree;
            this.beanCollectionGroup = beanCollectionGroup;
            putValue(Action.NAME, "Add...");
        }

        public void auditedActionPerformed(ActionEvent e) {
            BeanCollection newBeanCollection = beanCollectionGroup.editBeanCollection(null, tree, "Create new collection");
            if (newBeanCollection != null) {
                beanCollectionGroup.add(newBeanCollection);
                tree.makeVisible(new TreePath(new Object[]{ROOT, beanCollectionGroup, newBeanCollection}));
            }
        }

        @Override
        public boolean isEnabled() {
            return beanCollectionGroup.isAddEnabled();
        }
    }


    private static class RemoveAction extends AuditedAbstractAction {
        private JTree tree;

        public RemoveAction(boolean showText, JTree tree) {
            this.tree = tree;
            if (showText) {
                putValue(Action.NAME, "Remove");
            } else {
                putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(REMOVE_ICON));
            }
            setEnabled(isEnabled());
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            TreePath path = tree.getSelectionPath();
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) path.getParentPath().getLastPathComponent();
            BeanCollection beanCollection = (BeanCollection) path.getLastPathComponent();
            beanCollectionGroup.remove(beanCollection);
        }

        public boolean isEnabled() {
            TreePath path = tree.getSelectionPath();
            return path != null && path.getLastPathComponent() instanceof BeanCollection && !(path.getLastPathComponent() instanceof BeanCollectionGroup);
        }
    }

    private static class EditAction extends AuditedAbstractAction {
        private JTree tree;

        public EditAction(boolean showText, JTree tree) {
            this.tree = tree;

            if (showText) {
                putValue(Action.NAME, "Edit...");
            } else {
                putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(EDIT_ICON));
            }
            setEnabled(isEnabled());
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            TreePath path = tree.getSelectionPath();
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) path.getParentPath().getLastPathComponent();
            BeanCollection beanCollection = (BeanCollection) path.getLastPathComponent();
            int indexOfBeanCollection = beanCollectionGroup.indexOf(beanCollection);

            BeanCollection newBeanCollection = beanCollectionGroup.editBeanCollection(beanCollection, tree, "Edit collection");
            if (newBeanCollection != null) {
                beanCollectionGroup.set(indexOfBeanCollection, newBeanCollection);
                //Find all the open tabs that are based on the same view and update them
                updateAllDependentTabs(newBeanCollection);
            }
        }

        public void updateAllDependentTabs(BeanCollection beanCollection) {
            String enabled = ApplicationProperties.getApplicationProperties().getProperty("editCollectionUpdatesViews");
            if ("true".equalsIgnoreCase(enabled)) {
                String targetBeanCollectionName = beanCollection.toString();
                Collection<View> views = Workspace.getInstance().getViews();
                for (View view : views) {
                    BeanCollection viewBeanCollection = view.getBeanCollection();
                    //TODO fails for composite views needs to be refactored
                    if (viewBeanCollection != null) {
                        String beanCollectionName = viewBeanCollection.toString();
                        if (targetBeanCollectionName.equals(beanCollectionName) && beanCollection.getType() == viewBeanCollection.getType()) {
                            if (beanCollection.getClass() == viewBeanCollection.getClass()) {
                                BeanCollection clonedBeanCollection = (BeanCollection) BeanUtilities.verySlowDeepCopy(beanCollection);
                                view.setBeanCollection(clonedBeanCollection);
                            }
                        }
                    }
                }
            }
        }

        public boolean isEnabled() {
            TreePath path = tree.getSelectionPath();
            return path != null && path.getLastPathComponent() instanceof BeanCollection && !(path.getLastPathComponent() instanceof BeanCollectionGroup);
        }
    }

    private static class RunAction extends AuditedAbstractAction {
        private JTree tree;

        public RunAction(boolean showText, JTree tree) {
            this.tree = tree;

            if (showText) {
                putValue(Action.NAME, "Run");
            } else {
                putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(RUN_ICON));
            }
            setEnabled(isEnabled());
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            TreePath path = tree.getSelectionPath();
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) path.getParentPath().getLastPathComponent();
            BeanCollection beanCollection = (BeanCollection) BeanUtilities.verySlowDeepCopy(path.getLastPathComponent());

            String viewName = getViewName(beanCollectionGroup, beanCollection);
            String viewTabTitle = getViewTabTitle(beanCollectionGroup, beanCollection);
            String viewFrameTitle = getViewFrameTitle(beanCollectionGroup, beanCollection);

            beanCollectionGroup.viewBeanCollectionAsTable(viewName, viewTabTitle, viewFrameTitle, beanCollection);
        }

        private String getViewName(BeanCollectionGroup beanCollectionGroup, BeanCollection beanCollection) {
            return beanCollectionGroup + " in '" + beanCollection + "'";
        }

        private String getViewTabTitle(BeanCollectionGroup beanCollectionGroup, BeanCollection beanCollection) {
            return beanCollectionGroup + " - " + beanCollection;
        }

        private String getViewFrameTitle(BeanCollectionGroup beanCollectionGroup, BeanCollection beanCollection) {
            return getViewTabTitle(beanCollectionGroup, beanCollection);
        }

        public boolean isEnabled() {
            TreePath path = tree.getSelectionPath();
            return path == null || path.getParentPath() == null ? false : path.getParentPath().getLastPathComponent() instanceof BeanCollectionGroup;
        }
    }

    private static class ViewAllAction extends AuditedAbstractAction {
        private BeanCollectionGroup beanCollectionGroup;

        public ViewAllAction(BeanCollectionGroup beanCollectionGroup) {
            super("View All", "View all from BeanCollection");
            this.beanCollectionGroup = beanCollectionGroup;
        }

        public void auditedActionPerformed(ActionEvent e) {
            viewAllForBeanCollectionGroup(beanCollectionGroup);
        }

        public boolean isEnabled() {
            return beanCollectionGroup.isViewAllable();
        }
    }

    public static void viewAllForBeanCollectionGroup(BeanCollectionGroup beanCollectionGroup) {
        BeanCollection beanCollection = beanCollectionGroup.newBeanCollection(new CriterionGroup("All", Criterion.ALL_CRITERION));

        String viewName = "All " + beanCollectionGroup;
        String viewTitle = beanCollectionGroup + " - " + beanCollection;

        beanCollectionGroup.viewBeanCollectionAsTable(viewName, viewTitle, viewTitle, beanCollection);
    }

    private static class ExportAction extends AuditedAbstractAction {
        private JTree tree;

        public ExportAction(JTree tree) {
            this.tree = tree;
            putValue(Action.NAME, "Export...");
            setEnabled(isEnabled());
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            TreePath path = tree.getSelectionPath();

            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) path.getParentPath().getLastPathComponent();
            BeanCollection beanCollection = (BeanCollection) BeanUtilities.verySlowDeepCopy(path.getLastPathComponent());
            if (beanCollectionGroup == null || beanCollection == null) return;

            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException
            File file = null;

            try {
                JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
                fileChooser.setDialogTitle("Export Data Collection");
                fileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
                    }

                    public String getDescription() {
                        return "Bhavaya compatible object format (*.xml)";
                    }
                });

                int returnVal = fileChooser.showSaveDialog(UIUtilities.getWindowParent((Component) e.getSource()));
                if (returnVal != JFileChooser.APPROVE_OPTION) return;
                file = fileChooser.getSelectedFile();
            } finally {
                System.setSecurityManager(backup);
            }

            try {
                Configuration configuration = new Configuration();
                configuration.putObject("1", beanCollectionGroup.getId());
                configuration.putObject("2", beanCollection);
                Configuration.FileConfigurationSourceSink.saveConfiguration(file.getAbsolutePath(), configuration);
            } catch (Exception e1) {
                log.error(e1);
            }
        }

        public boolean isEnabled() {
            TreePath path = tree.getSelectionPath();
            return path == null || path.getParentPath() == null ? false : path.getParentPath().getLastPathComponent() instanceof BeanCollectionGroup;
        }
    }

    public static class ImportAction extends AuditedAbstractAction {
        public ImportAction() {
            super("Import Data Collections...");
        }

        public void auditedActionPerformed(ActionEvent e) {
            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException
            File[] files = null;

            try {
                JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setDialogTitle("Import Data Collections");
                fileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
                    }

                    public String getDescription() {
                        return "Bhavaya compatible object format (*.xml)";
                    }
                });

                int returnVal = fileChooser.showOpenDialog(UIUtilities.getWindowParent((Component) e.getSource()));
                if (returnVal != JFileChooser.APPROVE_OPTION) return;
                files = fileChooser.getSelectedFiles();
            } finally {
                System.setSecurityManager(backup);
            }

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                try {
                    InputStream stream = new BufferedInputStream(new FileInputStream(file));
                    importCollection(stream);
                } catch (Exception e1) {
                    log.error(e1);
                }
            }
        }
    }

    public static void importCollection(InputStream stream) throws Exception {
        Configuration configuration = Configuration.FileConfigurationSourceSink.loadConfiguration(stream);
        if (configuration != null) {
            String beanCollectionGroupId = configuration.getObject("1", null, String.class);
            BeanCollection beanCollection = configuration.getObject("2", null, BeanCollection.class);
            if (beanCollectionGroupId != null && beanCollection != null) {
                BeanCollectionGroup beanCollectionGroup = BeanCollectionGroup.getInstance(beanCollectionGroupId);
                if (beanCollectionGroup != null) {
                    beanCollectionGroup.add(beanCollection);
                }
            }
        }
    }

    private static class SetDefaultViewConfiguration extends AuditedAbstractAction {
        private BeanCollectionGroup beanCollectionGroup;
        private JComponent owner;

        public SetDefaultViewConfiguration(BeanCollectionGroup beanCollectionGroup, JComponent owner) {
            super("Set Default View...");
            this.beanCollectionGroup = beanCollectionGroup;
            this.owner = owner;
        }

        public void auditedActionPerformed(ActionEvent e) {
            TableViewConfigurationMap instance = TableViewConfigurationMap.getInstance(beanCollectionGroup.getBeanType());
            List viewConfigList = Arrays.asList(instance.getNames());
            final JComboBox viewConfigComboBox = new JComboBox(viewConfigList.toArray());
            String defaultViewConfiguration = beanCollectionGroup.getDefaultViewConfiguration();
            if (defaultViewConfiguration == null) defaultViewConfiguration = instance.getDefaultViewConfigurationId();
            if (viewConfigList.contains(beanCollectionGroup.getDefaultViewConfiguration()))
                viewConfigComboBox.setSelectedItem(defaultViewConfiguration);

            viewConfigComboBox.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

            final JDialog dialog = new JDialog(UIUtilities.getFrameParent(owner), "Default View", true);
            JButton okButton = new JButton(new AuditedAbstractAction("Ok", "Setting Default View Configuration") {
                public void auditedActionPerformed(ActionEvent e) {
                    String selectedViewConfiguration = (String) viewConfigComboBox.getSelectedItem();
                    if ("Default".equals(selectedViewConfiguration)) selectedViewConfiguration = null;
                    beanCollectionGroup.setDefaultViewConfiguration(selectedViewConfiguration);
                    dialog.dispose();
                }
            });
            JButton cancelButton = new JButton(new AuditedAbstractAction("Cancel") {
                public void auditedActionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(viewConfigComboBox, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setContentPane(mainPanel);
            dialog.pack();
            UIUtilities.centreInContainer(UIUtilities.getFrameParent(owner), dialog, 0, 0);

            dialog.show();
        }
    }

}
