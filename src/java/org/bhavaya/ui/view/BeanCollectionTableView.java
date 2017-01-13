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

package org.bhavaya.ui.view;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.LoadProgressMonitor;
import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.ui.*;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.QuantityRenderer;
import org.bhavaya.ui.table.formula.*;
import org.bhavaya.util.*;
import org.bhavaya.util.MutableBoolean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * The name is misleading.  The differences between a TableView and a BeanCollectionTableView are:
 * <p/>
 * 1. Loading is decorated with progress dialogs and done in background thread
 * 2. BeanCollectionTableViews are persistable, TableViews are not (not for good reason).
 * 3. Warning when loading excessive data.
 * <p/>
 * Both take BeanCollections as construction arguments!
 *
 * @author Daniel van Enckevort
 * @author Brendon McLean
 * @version $Revision: 1.35.4.1 $
 */

public class BeanCollectionTableView extends TableView {
    private static final Log log = Log.getCategory(BeanCollectionTableView.class);
    private static final int DEFAULT_WARNING_ROW_COUNT = -1;
    private static int warningRowCount;
    private static final ImageIcon MANAGE_FORMULA_ICON = ImageIconCache.getImageIcon("formula.png");

    private CardPanel cardPanel;
    private BeanCollection beanCollection = null;

    private LoadingPanel loadingComponent;
    private LoadingPanel waitingComponent;
    private Component loadedComponent;
    private Task loadingTask;
    private Box errorComponent;
    private Map loadTasks = new HashMap();

    private boolean checkRowCount = false;

    private CachedObjectGraph.ExecutionController cachedObjectGraphExecutionController;

    static {
        BeanUtilities.addPersistenceDelegate(BeanCollectionTableView.class, new BhavayaPersistenceDelegate(new String[]{"name", "tabTitle", "frameTitle", "recordType", "viewConfigurationId", "beanCollection"}));

        warningRowCount = DEFAULT_WARNING_ROW_COUNT;
        String warningRowCountString = ApplicationProperties.getApplicationProperties().getProperty("warningDataLoadRowCount");
        if (warningRowCountString != null && warningRowCountString.length() > 0) {
            try {
                warningRowCount = Integer.parseInt(warningRowCountString);
            } catch (NumberFormatException e) {
                log.error(e);
            }
        }
    }

    /**
     * Creates a TableView for the given bean collection
     *
     * @param beanCollection can be null, if so, override createBeanCollection to initialise
     */
    public BeanCollectionTableView(String name, String tabTitle, String frameTitle, Class recordType, String viewConfigurationId, BeanCollection beanCollection) {
        this(name, tabTitle, frameTitle, new BeanCollectionTableModel(recordType, true, name), viewConfigurationId, beanCollection);
    }

    /**
     * Creates a TableView for the given bean collection but allows you to specify the beanCollectionTableModel that will be used
     *
     * @param beanCollection can be null, if so, override createBeanCollection to initialise
     */
    public BeanCollectionTableView(String name, String tabTitle, String frameTitle, BeanCollectionTableModel beanCollectionTableModel, String viewConfigurationId, BeanCollection beanCollection) {
        super(name, tabTitle, frameTitle, beanCollectionTableModel, viewConfigurationId);
        this.beanCollection = beanCollection;
    }

    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        //if this is a criterion bean collection, setup any further criteria required to restrict the view
        //n.b do this first, before call to super.setCollection()
        //this is because super.setCollection adds a listener to the bean collection, which currently means
        //that any subsequent change to the criteria triggers a load - we want to avoid loading the collection until all criteria are set up
        addViewLocalCriteria(beanCollection);
        super.setBeanCollection(beanCollection);
        this.beanCollection = beanCollection;
    }

    private void addViewLocalCriteria(BeanCollection beanCollection) {
        if ( getViewContext() instanceof CollectionRestrictor) {
//            setLoadDecorator(beanCollection, false);
//            cardPanel.setSelectedComponent(loadingComponent);
            ((CollectionRestrictor)getViewContext()).restrictCollection(beanCollection);
        }
    }

    protected Component getLoadedComponent() {
        return super.getComponent();
    }

    public Component getComponent() {
        init();
        return cardPanel;
    }

    public boolean isCheckRowCount() {
        return checkRowCount;
    }

    public void setCheckRowCount(boolean checkRowCount) {
        this.checkRowCount = checkRowCount;
    }

    protected void initImpl() {
        super.initImpl();

        setLoadDecorator(beanCollection, false);
        waitingComponent = new LoadingPanel("Waiting to load data");
        loadingComponent = new LoadingPanel("Loading data...");

        cardPanel = new CardPanel();
        cardPanel.addComponent(waitingComponent);
        cardPanel.addComponent(loadingComponent);

        cachedObjectGraphExecutionController = ApplicationContext.getInstance().createCachedObjectGraphExecutionController();
        cachedObjectGraphExecutionController.setUserPriority(false);
        beanCollectionTableModel.setCachedObjectGraphExecutionController(cachedObjectGraphExecutionController);

        loadingTask = new Task("BeanCollectionTableView load data for: " + getName()) {
            public void run() {
                try {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            cardPanel.setSelectedComponent(loadingComponent);
                        }
                    });

                    Profiler.Task executionTask = Profiler.taskStarted(Profiler.MAJOR_METHOD, "Load BeanCollection: " + getName());
                    try {
                        beanCollection.size(); //if the collection retrieves its data lazily, this will kick off a loader
                    } finally {
                        Profiler.taskStopped(executionTask);
                    }

                    if (!isDisposed() && errorComponent == null) { // CriteriaBeanCollectionLoadDecorator may have disposed the view based a user request
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    loadedComponent = getLoadedComponent();
                                    cardPanel.addComponent(loadedComponent);
                                    cardPanel.remove(waitingComponent);
                                    beanCollectionTableModel.setBeanCollection(beanCollection);
                                    cardPanel.setSelectedComponent(loadedComponent);
                                } catch (Throwable e) {
                                    handleError(e);
                                }
                            }
                        });
                    }
                } catch (Throwable e) {
                    handleError(e);
                }
            }
        };
        ApplicationContext.getInstance().addGuiTask(loadingTask);

        // When this component is visible to the user, boost its loading task priority.
        cardPanel.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                cardPanel.removeHierarchyListener(this);

                if (cardPanel.isVisible()) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            setUserPriority();
                        }
                    });
                }
                
                cardPanel.addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) {
                        setUserPriority();
                    }

                    public void componentHidden(ComponentEvent e) {
                        ApplicationContext.getInstance().setGuiTaskPriority(loadingTask, PriorityTaskQueue.PRIORITY_NORMAL);
                        cachedObjectGraphExecutionController.setUserPriority(false);
                    }
                });
            }
        });

        getAnalyticsTable().setDefaultRenderer(FormulaResult.class, new FormulaResultRenderer());
    }

    protected JToolBar createEditControlToolBar(boolean pivoted){
        JToolBar toolBar = super.createEditControlToolBar(pivoted);

        if(FormulaUtils.formulasEnabled()) {
            JButton formulaEditingButton = new JButton(MANAGE_FORMULA_ICON);
            formulaEditingButton.setToolTipText("Manage Formulas");
            formulaEditingButton.addActionListener(new ManageFormulasAction());
            toolBar.add(formulaEditingButton);
        }

        return toolBar;
    }

    private void setUserPriority() {
        ApplicationContext.getInstance().setGuiTaskPriority(loadingTask, PriorityTaskQueue.PRIORITY_HIGH);
        log.info("Adjusted task to max priority: " + loadingTask);
        cachedObjectGraphExecutionController.setUserPriority(true);
    }

    protected void disposeImpl() {
        super.disposeImpl();
        waitingComponent.dispose();
        loadingComponent.dispose();
        cardPanel.removeAll();
        setLoadDecorator(beanCollection, true);
        beanCollection = null;
        loadTasks = null;
    }

    private void handleError(Throwable e) {
        log.error("Could not display table view " + getName() + " due to exception", e);
        if (errorComponent == null) {
            errorComponent = UIUtilities.createErrorComponent("<html>There was a problem loading the data.<br>Please go to the Help/Diagnotics menu and send a report to support</html>");
        }
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                cardPanel.addComponent(errorComponent);
                cardPanel.setSelectedComponent(errorComponent);
            }
        });
    }

    private void setLoadDecorator(BeanCollection beanCollection, boolean nullify) {
        if (beanCollection instanceof CriteriaBeanCollection) {
            setLoadDecorator((CriteriaBeanCollection) beanCollection, nullify);
        }

        BeanCollection[] sourceBeanCollections = beanCollection.getBeanCollections();
        for (int i = 0; i < sourceBeanCollections.length; i++) {
            BeanCollection sourceBeanCollection = sourceBeanCollections[i];
            if (sourceBeanCollection instanceof CriteriaBeanCollection && getRecordType().isAssignableFrom(sourceBeanCollection.getType())) {
                setLoadDecorator((CriteriaBeanCollection) sourceBeanCollection, nullify);
            }
        }
    }

    private void setLoadDecorator(CriteriaBeanCollection criteriaBeanCollection, boolean nullify) {
        CriteriaBeanCollectionLoadDecorator loadDecorator = nullify ? null : new CriteriaBeanCollectionLoadDecorator(criteriaBeanCollection, warningRowCount);
        criteriaBeanCollection.setLoadDecorator(loadDecorator);
    }

    private class CriteriaBeanCollectionLoadDecorator implements CriteriaBeanCollection.LoadDecorator {
        private CriteriaBeanCollection criteriaBeanCollection;
        private int maxRowCount;

        public CriteriaBeanCollectionLoadDecorator(CriteriaBeanCollection criteriaBeanCollection, int maxRowCount) {
            this.criteriaBeanCollection = criteriaBeanCollection;
            this.maxRowCount = maxRowCount;
        }

        public void run(final CriteriaBeanCollection.LoadRunnable loadRunnable, final boolean firstLoad) {
            LoadTask loadTask = getLoadTask(loadRunnable, firstLoad);

            if (firstLoad) {
                loadTask.run();
            } else {
                ApplicationContext.getInstance().addGuiTask(loadTask);
            }
        }

        private LoadTask getLoadTask(CriteriaBeanCollection.LoadRunnable loadRunnable, boolean firstLoad) {
            // We are caching instances of the task, so if load gets called repeatedly, we ignore
            // one task if we are still running a previous one.  This happens because TaskQueue uses a Set to
            // to hold its tasks.
            // Need to do this to prevent spams from clients sending ALL_ROW events to the beanCollection

            java.util.List taskKey = new ArrayList(2);
            taskKey.add(loadRunnable);
            taskKey.add(new Boolean(firstLoad));

            LoadTask loadTask = (LoadTask) loadTasks.get(taskKey);
            if (loadTask == null) {
                loadTask = new LoadTask(loadRunnable, firstLoad);
                loadTasks.put(taskKey, loadTask);
            }
            return loadTask;
        }

        private class LoadTask extends Task {
            private boolean firstLoad;
            private CriteriaBeanCollection.LoadRunnable loadRunnable;

            public LoadTask(CriteriaBeanCollection.LoadRunnable loadRunnable, boolean firstLoad) {
                super("BeanCollectionTableView load data for: " + BeanCollectionTableView.this.getName());
                this.firstLoad = firstLoad;
                this.loadRunnable = loadRunnable;
            }

            public void run() {
                try {
                    // This only happens when the user does a data refresh or the notification server is down and
                    // we're in polling mode.
                    if (!firstLoad) {
                        Runnable setLoadingComponentRunnable = new Runnable() {
                            public void run() {
                                cardPanel.setSelectedComponent(loadingComponent);
                            }
                        };
                        if (EventQueue.isDispatchThread()) {
                            setLoadingComponentRunnable.run();
                        } else {
                            EventQueue.invokeAndWait(setLoadingComponentRunnable);
                        }
                    }

                    final MutableBoolean tooManyRows = new MutableBoolean(false);
                    final LoadProgressMonitor progressModel = new DefaultLoadingProgressMonitor();

                    if (checkRowCount) {
                        checkRowCount = false;

                        final int rowCount = criteriaBeanCollection.getLoadRowCount();

                        if (rowCount > 0 && warningRowCount >= 0) {
                            tooManyRows.value = rowCount > maxRowCount;
                            if (tooManyRows.value) {
                                Runnable confirmLoadRunnable = new Runnable() {
                                    public void run() {
                                        String message = "About to load " + rowCount + " " + Utilities.getPluralName(ClassUtilities.getDisplayName(BeanCollectionTableView.this.getBeanCollection().getType())) + "  for '" + BeanCollectionTableView.this.getName() + "'.";
                                        log.info(message);

                                        int ret = JOptionPane.showConfirmDialog(getComponent(), message + "  Do you wish to continue?", "Large data load", JOptionPane.YES_NO_OPTION);

                                        if (ret == JOptionPane.YES_OPTION) {
                                            tooManyRows.value = false;
                                            log.info("User confirmed large data load.");
                                        } else {
                                            log.info("User cancelled large data load.");
                                        }
                                    }
                                };


                                if (EventQueue.isDispatchThread()) {
                                    confirmLoadRunnable.run();
                                } else {
                                    EventQueue.invokeAndWait(confirmLoadRunnable);
                                }
                            }
                        }
                    }

                    if (firstLoad && tooManyRows.value) { // close the view
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                Workspace.getInstance().removeView(BeanCollectionTableView.this);
                                Workspace.getInstance().disposeView(BeanCollectionTableView.this);
                            }
                        });
                        return;
                    }

                    if (!tooManyRows.value) {
                        UIUtilities.runInDispatchThread(new Runnable() {
                            public void run() {
                                loadingComponent.setProgressModel(progressModel);
                            }
                        });
                        loadRunnable.run(progressModel);
                    }

//                    if (!firstLoad) {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                cardPanel.setSelectedComponent(loadedComponent);
                            }
                        });
//                    }
                } catch (Throwable e) { // Possible RuntimeException e.g. from BeanFactory
                    handleError(e);
                }
            }
        }

        private class DefaultLoadingProgressMonitor extends DefaultBoundedRangeModel implements LoadProgressMonitor {
            private Stack loadingStack = new Stack();

            public DefaultLoadingProgressMonitor() {
                super(0, 0, 0, 100);
            }

            public void loadingStackPop() {
                loadingStack.pop();

                if (loadingStack.isEmpty()) {
                    UIUtilities.runInDispatchThread(new Runnable() {
                        public void run() {
                            loadingComponent.setProgressText("");
                        }
                    });
                } else {
                    updateMessage();
                }
            }

            public void loadingStackPush(Object object) {
                loadingStack.push(object);
                updateMessage();
            }

            private void updateMessage() {
                Class beanType = ((BeanFactory) loadingStack.peek()).getType();
                final String displayName = BeanCollectionGroup.getDefaultInstance(beanType).getPluralDisplayName();
                UIUtilities.runInDispatchThread(new Runnable() {
                    public void run() {
                        loadingComponent.setProgressText(displayName + ": " + getValue());
                    }
                });
            }

            public void setValue(final int newValue) {
                UIUtilities.runInDispatchThread(new Runnable() {
                    public void run() {
                        if (newValue > getMaximum()) setMaximum(getMaximum() * 2);
                        DefaultLoadingProgressMonitor.super.setValue(newValue);
                    }
                });
            }
        }
    }

    protected final class ManageFormulasAction extends AuditedAbstractAction {
        public ManageFormulasAction() {
            super("Manage Formulas");
        }

        public void auditedActionPerformed(ActionEvent event) {
            try {
                new FormulaManagementDialog(UIUtilities.getFrameParent(getComponent()), beanCollectionTableModel);
            } catch (FormulaException e) {
                AlwaysOnTopJOptionPane.showMessageDialog(getComponent(), "Unable to open formula editor:\n"+e.getMessage(),
                        "Error Opening Formula Editor", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}