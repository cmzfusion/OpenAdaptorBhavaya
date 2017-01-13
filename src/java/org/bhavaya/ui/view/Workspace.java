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

import org.bhavaya.ui.*;
import org.bhavaya.ui.componentaliasing.alias.AliasAwtComponent;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides persistent state for tab panes and frames.
 *
 * @author Brendon McLean
 * @version $Revision: 1.68.2.1.2.6 $
 */

public class Workspace {
    private static final Log log = Log.getCategory(Workspace.class);

    static {
        BeanUtilities.addPersistenceDelegate(FrameConfig.class, new BhavayaPersistenceDelegate(new String[]{"frameBounds", "view"}));
    }

    private static final int SPAWN_OFFSET = 20;

    public static final int USE_TABBED_PANE = 0;
    public static final int USE_NEW_FRAME = 1;

    private static final String CONFIG_KEY = "Workspace";
    private static final String TAB_CONFIG_KEY = "TabViews";
    private static final String TAB_PANE = "TabPane";
    private static final String FRAME_CONFIG_KEY = "FrameViews";
    private static final String APPLICATION_FRAME_BOUNDS_CONFIG_KEY = "ApplicationFrameBounds";
    private static final String APPLICATION_FRAME_EXTENDED_STATE_CONFIG_KEY = "ApplicationFrameExtendedState";
    private static final String ACTIVE_TAB_VIEW_KEY = "ActiveTabView";
    private static final String TAB_TOOLTIPS_ON_KEY = "tabToolTipsOnKey";
    private static final String SHOW_TOOL_BAR = "showToolBar";
    private static final String SHOW_MENU_BAR = "showMenuBar";
    private static final String COLOUMN_COLOUR_HAS_PRIORITY_KEY = "columnColourHasPriority";
    private static final String TOGGLE_MENUBAR_ACTION_KEY = "toggle_menubar";
    private static final String TOOLBAR_GROUP_ORDER_MAP_KEY = "toolbarGroupOrderMap";
    private static final String TOOLBAR_EXPANDED_STATE_MAP_KEY = "toolbarExpandedStateMap";

    private static final KeyStroke TOGGLE_MENUBAR_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK);
    private static final String TOGGLE_TOOLBAR_ACTION_KEY = "toggle_toolbar";
    private static final KeyStroke TOGGLE_TOOLBAR_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK);
    private static final String PRINT_COMPONENT_ACTION_KEY = "print_component";
    private static final KeyStroke PRINT_COMPONENT_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK | InputEvent.ALT_MASK);

    private static final KeyStroke NEXT_TAB_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK);
    private static final KeyStroke NEXT_TAB_KEYSTROKE2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK);
    private static final KeyStroke PREV_TAB_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK);
    private static final KeyStroke PREV_TAB_KEYSTROKE2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK);
    private static final KeyStroke NEXT_VISITED_TAB_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static final KeyStroke NEXT_VISITED_TAB_KEYSTROKE2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static final KeyStroke PREV_VISITED_TAB_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static final KeyStroke PREV_VISITED_TAB_KEYSTROKE2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    private static final long LAST_ACTIVATED_THRESHOLD = Long.getLong("DAYS_SINCE_LAST_ACTIVATED", 90) * 24 * 60 * 60 * 1000;

    private static final Map<String, Color> TAB_COLOURS = new LinkedHashMap<String, Color>();

    static {
        TAB_COLOURS.put("White (Default)", new Color(255, 255, 255));
        TAB_COLOURS.put("Red", new Color(255, 96, 96));
        TAB_COLOURS.put("Orange", new Color(251, 162, 96));
        TAB_COLOURS.put("Green", new Color(96, 255, 107));
        TAB_COLOURS.put("Blue", new Color(96, 171, 255));
        TAB_COLOURS.put("Purple", new Color(242, 96, 255));
    }


    private static Workspace instance;

    // Add a hook to the EventQueue for keyboard accelerators
//    private static Map keyStrokeToActionKeyMap = new HashMap();
//    private static Map actionKeyToActionMap = new HashMap();

    private static final Object lock = new Object();

    private boolean tabToolTipsOn;
    private boolean columnColourHasPriority;
    private boolean showMenuBar;
    private boolean showtoolBar;
    private int defaultNewViewType = USE_TABBED_PANE;
    private Configuration defaultConfiguration;
    private Configuration startupConfiguration;

    private Map<Component, View> componentToView = new HashMap<Component, View>();
    private ArrayList<View> tabViews = new ArrayList<View>();
    private Map<Object, View> windowViews = new LinkedHashMap<Object, View>();

    private GenericFrame applicationFrame;
    private WindowPanel applicationPanel;
    private Rectangle applicationFrameRectangle;
    private int applicationFrameExtendedState;
    private View activeTabView;

    // Only used as interim state between config load and application frame display.
    private ViewChangedHandler changeListener;
    private WindowListener windowListener;

    protected ViewBasedUserLayoutTabPane tabbedPane;
    private final static Color flashColor = new Color(54, 210, 255);
    private final static int changeFadeoutTime = 5000;
    protected HashMap<View, ViewChange> viewToChangeTime = new HashMap<View, ViewChange>();

    private static ScheduledExecutorService tabColourAnim = NamedExecutors.newSingleThreadScheduledExecutor("TabColourAnimator");
    private LookAndFeelManager lookAndFeelManager;
    private ChangeListener lookAndFeelListener;

    private Action[] currentTabSpecificActions = new Action[0];

    private static Map<Class, List<String>> toolbarGroupOrderMap = new HashMap<Class, List<String>>();
    private Boolean useReorderableToolbar = null;
    private static Map<String, Boolean> toolbarExpandedStateMap = new HashMap<String, Boolean>();
    private Map<KeyStroke, Action> actionMap = new ConcurrentHashMap<KeyStroke, Action>();

    public static Workspace getInstance() {
        synchronized (lock) {
            if (instance == null) {
                String workspaceClass = ApplicationProperties.getApplicationProperties().getProperty("workspaceClass");
                if (workspaceClass == null) {
                    instance = new Workspace();
                } else {
                    try {
                        instance = (Workspace) ClassUtilities.getClass(workspaceClass).newInstance();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
                instance.loadState();
            }
        }
        return instance;
    }

    private Configuration getStartupConfiguration() {
        if (startupConfiguration == null) {
            startupConfiguration = Configuration.getRoot(Configuration.STARTUP).getConfiguration(CONFIG_KEY);
        }
        return startupConfiguration;
    }

    private Configuration getDefaultConfiguration() {
        if (defaultConfiguration == null) {
            defaultConfiguration = Configuration.getRoot().getConfiguration(CONFIG_KEY);
        }
        return defaultConfiguration;
    }

    protected Workspace() {
        Configuration.addSaveTask(new SaveTask());
        setTabToolTipsOn(getStartupConfiguration().getObject(TAB_TOOLTIPS_ON_KEY, Boolean.TRUE, Boolean.class));
        setShowMenuBar(getStartupConfiguration().getObject(SHOW_MENU_BAR, Boolean.TRUE, Boolean.class));
        setShowtoolBar(getStartupConfiguration().getObject(SHOW_TOOL_BAR, Boolean.TRUE, Boolean.class));
        setColumnColourHasPriority(getStartupConfiguration().getObject(COLOUMN_COLOUR_HAS_PRIORITY_KEY, Boolean.FALSE, Boolean.class));
        changeListener = new ViewChangedHandler();
        windowListener = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                GenericWindow window = (GenericWindow) e.getSource();
                if(window.isRedockingOnClose()){
                    addTab(removeWindow(window));
            }
                else{
                    removeWindow(window);
                }
            }

            public void windowClosed(WindowEvent e) {
                GenericWindow window = (GenericWindow) e.getSource();
                if(window.isRedockingOnClose()){
                    addTab(removeWindow(window));
            }
                else{
                    removeWindow(window);
                }
            }
        };
    }

    public boolean isShowMenuBar() {
        return showMenuBar;
    }

    public void setShowMenuBar(boolean showMenuBar) {
        this.showMenuBar = showMenuBar;
    }

    public boolean isShowtoolBar() {
        return showtoolBar;
    }

    public void setShowtoolBar(boolean showtoolBar) {
        this.showtoolBar = showtoolBar;
    }
    
    public boolean isTabToolTipsOn() {
        return tabToolTipsOn;
    }

    public void setTabToolTipsOn(boolean tabToolTipsOn) {
        this.tabToolTipsOn = tabToolTipsOn;
    }

    public boolean isColumnColourHasPriority() {
        return columnColourHasPriority;
    }

    public void setColumnColourHasPriority(boolean columnColourHasPriority) {
        this.columnColourHasPriority = columnColourHasPriority;
        // we also need to propogate this property to all the existing view configurations
        Collection tableViewConfigurations = TableViewConfigurationMap.getAllTableViewConfigurations();
        setOnViews(tableViewConfigurations, columnColourHasPriority);
        Collection fixedViewConfigurations = FixedViewConfigurationMap.getAllTableViewConfigurations();
        setOnViews(fixedViewConfigurations, columnColourHasPriority);
    }

    private void setOnViews(Collection viewConfigurationMap, boolean columnColourHasPriority) {
        Iterator iterator = viewConfigurationMap.iterator();
        while (iterator.hasNext()) {
            TableViewConfiguration configuration = (TableViewConfiguration) iterator.next();
            configuration.setColumnColourHasPriority(columnColourHasPriority);
        }
    }

    public void setColoumnColourPriorityOnAllViews(boolean columnColourHasPriority) {
        for (View view : componentToView.values()) {
            setColoumnColourPriorityOnView(view, columnColourHasPriority);
        }
        for (View view : windowViews.values()) {
            setColoumnColourPriorityOnView(view, columnColourHasPriority);
        }
        forceUpdate();
    }

    public void setColoumnColourPriorityOnView(Object view, boolean columnColourHasPriority) {
        if (view != null && view instanceof TableView){
            TableView tableView = (TableView)view;
            AnalyticsTable table = tableView.getAnalyticsTable();
            table.setColumnColourHasPriority(columnColourHasPriority);
        }
    }


    protected MouseListener getTabMouseListener() {
        return new MouseHandler();
    }

    /**
     * If the given view has already been added to the workspace, then this method tries to bring the view into the fore.
     * i.e. if it is a tab view, set the selected tab index. if it is a frame view, try to bring the frame to the front.
     */
    public void displayView(final View view) {
        Runnable r = new Runnable() {
            public void run() {
                displayViewInEDT(view);
            }
        };
        UIUtilities.runInDispatchThread(r);
    }

    private void displayViewInEDT(final View view) {
        // try to find a tab view
        UserLayoutTabPane tabbedPane = getTabbedPane();
        Component viewComponent = view.getComponent();
        UserLayoutTabPane.TabMetaData tabMetaData = tabbedPane.getTabMetaData(viewComponent);
        if (tabMetaData != null) {
            tabbedPane.setSelectedComponent(viewComponent);
            return;
        } else {
            Window windowAncestor = SwingUtilities.getWindowAncestor(view.getComponent());
            if (windowAncestor != null) {
                windowAncestor.toFront();
                return;
            }
        }

        // failed, so add the view
        Task task;
        if (defaultNewViewType == USE_TABBED_PANE) {
            task = new Task(view.getName()) {
                public void run() throws Throwable {
                    addTab(view);
                }
            };
        } else {
            task = new Task(view.getName()) {
                public void run() throws Throwable {
                    addFrame(view);
                }
            };
        }
        UIUtilities.runTaskWithProgressDialog(getTabbedPane(), "Building View", task);
    }

    public void setDefaultNewViewType(int newViewType) {
        this.defaultNewViewType = newViewType;
    }

    public int getDefaultNewViewType() {
        return defaultNewViewType;
    }

    /**
     * todo: the relation between the ApplicationView and the workspace seems rather confused
     * todo: why does the workspace have the tabbed pane, rather than the view?
     */
    public ViewBasedUserLayoutTabPane getTabbedPane() {
        return tabbedPane;
    }

    public GenericWindow getApplicationFrame() {
        if (applicationFrame == null) {
            Runnable r = new Runnable() {
                public void run() {
                    createApplicationFrame();
                }
            };
            if(SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(r);
                } catch (Exception e) {
                    throw new RuntimeException("Error creating application frame", e);
                }
            }
        }
        return applicationFrame;
    }

    private void createApplicationFrame() {
        Log.getPrimaryLoadingLog().info("Building GUI");
        Log.getSecondaryLoadingLog().info(" ");

        //Creation of tabbed pane moved from constructor as was being created off EDT and before look and feel set up
        this.tabbedPane = new ViewBasedUserLayoutTabPane();
        tabbedPane.addTabMouseListener(getTabMouseListener());
        tabbedPane.addChangeListener(new TabChangeHandler());
        tabbedPane.setFocusable(false);
        tabbedPane.addPropertyChangeListener("UI", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (activeTabView != null) setActiveTabView(activeTabView);
            }
        });

        View applicationView = ApplicationContext.getInstance().getApplicationView();
        applicationView.addChangeListener(changeListener);
        applicationFrame = createWindow(applicationView, false);

        Component applicationViewComponent = applicationView.getComponent();
        applicationPanel = new WindowPanel(applicationViewComponent);
        if (isShowtoolBar()) {
            applicationPanel.setJToolBar(createFrameJToolBar(null, applicationFrame));
        }
        applicationFrame.setBounds(applicationFrameRectangle);
        /**
         * For some reason when a frame is initialy ICONIFIED, menu is not displayed after it is restored until it is resized.
         * Workarround is to set it to the NORMAL state instead.
         */
        if (applicationFrameExtendedState == Frame.ICONIFIED) applicationFrameExtendedState = Frame.NORMAL;
        applicationFrame.setExtendedState(applicationFrameExtendedState);
        applicationFrame.setContentPane(applicationPanel);
        applicationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (isShowMenuBar()){
            applicationFrame.setJMenuBar(createFrameJMenuBar(null, applicationFrame));
        }
        applicationFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ApplicationContext.getInstance().exit(true);
            }

            public void windowOpened(WindowEvent e) {
                if (log.isDebug()) log.debug("Workspace.windowOpened");
            }
        });

        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher( new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                if ( actionMap.containsKey(keyStroke) ) {
                    final Action a = actionMap.get(keyStroke);
                    final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), null );
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            a.actionPerformed(ae);
                        }
                    } );
                    return true;
                }
                return false;
            }
        });

        addKeyStrokeAction(TOGGLE_MENUBAR_KEYSTROKE, new ToggleMenuAction() {
            public void setMenuBarOff() {
                Workspace.getInstance().setShowMenuBar(false);
            }

            public void setMenuBarOn() {
                Workspace.getInstance().setShowMenuBar(true);
            }
        });
        addKeyStrokeAction(TOGGLE_TOOLBAR_KEYSTROKE, new ToggleToolbarAction() {
            public void setToolBarOff() {
                Workspace.getInstance().setShowtoolBar(false);
            }

            public void setToolBarOn() {
                Workspace.getInstance().setShowtoolBar(true);
            }
        });
        addKeyStrokeAction(NEXT_TAB_KEYSTROKE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectNextButtonComponent();
            }
        });
        addKeyStrokeAction(NEXT_TAB_KEYSTROKE2, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectNextButtonComponent();
            }
        });
        addKeyStrokeAction(PREV_TAB_KEYSTROKE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectPrevButtonComponent();
            }
        });
        addKeyStrokeAction(PREV_TAB_KEYSTROKE2, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectPrevButtonComponent();
            }
        });
        addKeyStrokeAction(NEXT_VISITED_TAB_KEYSTROKE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectNextHistoricalComponent();
            }
        });
        addKeyStrokeAction(NEXT_VISITED_TAB_KEYSTROKE2, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectNextHistoricalComponent();
            }
        });
        addKeyStrokeAction(PREV_VISITED_TAB_KEYSTROKE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectPrevHistoricalComponent();
            }
        });
        addKeyStrokeAction(PREV_VISITED_TAB_KEYSTROKE2, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.selectPrevHistoricalComponent();
            }
        });

        // last thing after init of application frame
        // activateState can require locks to build app-specific menus, toolbars ext
        // therefore better to get this done early, to prevent AWT-Thread waiting to
        // acquire locks which have been acquired on other threads.
//        activateState();
    }


    public void addKeyStrokeAction(KeyStroke keyStroke, Action action) {
        actionMap.put(keyStroke, action);
    }

    public void installLookAndFeelManager(LookAndFeelManager lookAndFeelManager) {
        if (lookAndFeelListener != null) {
            lookAndFeelManager.removeChangeListener(lookAndFeelListener);
        }

        if (log.isDebug())log.debug("Installed LookAndFeelManager: " + lookAndFeelManager);
        this.lookAndFeelManager = lookAndFeelManager;
        this.lookAndFeelListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                log.info("Changing look and feel.");
                for (Object window : windowViews.keySet()) {
                    if (window instanceof JFrame) {
                        SwingUtilities.updateComponentTreeUI(((JFrame) window).getRootPane());
                    } else if (window instanceof JDialog) {
                        SwingUtilities.updateComponentTreeUI(((JDialog) window).getRootPane());
                    }
                }
                if (applicationFrame != null) {
                    SwingUtilities.updateComponentTreeUI(applicationFrame.getRootPane());
                }
                if(tabbedPane != null) {
                    tabbedPane.updateUI();
                }
            }
        };
        this.lookAndFeelManager.addChangeListener(this.lookAndFeelListener);
        this.lookAndFeelManager.installLookAndFeel();
    }

    //todo: check tab mouse listener
    protected class MouseHandler extends MouseAdapter {
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupTrigger(e);
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupTrigger(e);
            }
        }

        public void mouseClicked(MouseEvent e) {
            Component componentForTabLocation = tabbedPane.getComponentForTabMouseEvent(e);
            if (componentForTabLocation != null) {
                View view = getViewForComponent(componentForTabLocation);
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    removeView(view);
                    addFrame(view);
                } else if (SwingUtilities.isMiddleMouseButton(e)) {
                    removeView(view);
                    disposeView(view);
                }
            }
        }

        protected void popupTrigger(MouseEvent e) {
            Component tab = tabbedPane.getComponentForTabMouseEvent(e);

            if (tab != null) {
                View view = getViewForComponent(tab);

                JPopupMenu popupMenu = new JPopupMenu();

                JMenuItem closeTabMenuItem = new JMenuItem(new CloseTabAction(view));
                JMenuItem floatTabMenuItem = new JMenuItem(new FloatTabAction(view));
                JMenuItem duplicateTabMenuItem = new JMenuItem(new DuplicateTabAction(view));

                popupMenu.add(closeTabMenuItem);
                popupMenu.add(floatTabMenuItem);
                if (BeanUtilities.hasPersistenceDelegate(view.getClass())) {
                    popupMenu.add(duplicateTabMenuItem);
                }
                if (view instanceof AbstractView) {
                    JMenuItem renameViewMenuItem = new JMenuItem(new RenameViewAction(view));
                    popupMenu.add(renameViewMenuItem);
                }

                JMenu setTabColourMenu = new JMenu("Tab Colour");
                for (Map.Entry<String, Color> entry : TAB_COLOURS.entrySet()) {
                    String colourName = entry.getKey();
                    Color color = entry.getValue();
                    setTabColourMenu.add(new SetColourAction(tab, color, colourName));
                }
                popupMenu.add(setTabColourMenu);

                popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
            }
        }
    }

    private class TabChangeHandler implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            Component selectedComponent = tabbedPane.getSelectedComponent();
            setActiveTabView(getViewForComponent(selectedComponent));
        }
    }

    private class RenameViewAction extends AuditedAbstractAction {
        private View view;

        public RenameViewAction(View view) {
            this.view = view;
            putValue(Action.NAME, "Rename Tab");
        }

        public void auditedActionPerformed(ActionEvent e) {
            if (view instanceof AbstractView) {
                AbstractView abstractView = ((AbstractView) view);
                String newViewName = JOptionPane.showInputDialog(UIUtilities.getWindowParent((Component) e.getSource()), "Enter in new name for view", view.getTabTitle());
                if (newViewName != null && newViewName.length() > 0) {
                    abstractView.setTabTitle(newViewName);
                    abstractView.setFrameTitle(newViewName);
                }
            }
        }
    }

    private class CloseTabAction extends AuditedAbstractAction {
        private View view;

        public CloseTabAction(View view) {
            this.view = view;
            putValue(Action.NAME, "Close Tab");
        }

        public void auditedActionPerformed(ActionEvent e) {
            closeTabWithConformation(view);
        }
    }

    private void closeTabWithConformation(View view) {
        Map<View, List<AliasAwtComponent>> dependantViews = getDependantViews(view);

        if (dependantViews.isEmpty()) {
            closeTab(view);
        }
        else {
            String[] options = {"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(null, "Closing this view will remove any alias buttons exported to other views",
                    "Close View",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, "Cancel");
            if (option == 0) {
                String affectedText = getDisplayText(dependantViews);
                log.info("Following Alias buttons are going to be removed " + affectedText);
                cleanUpAliasButtons(dependantViews);
                closeTab(view);
            }
        }
    }

    private void closeTab(View view) {
        removeView(view);
        disposeView(view);
    }

    private String getDisplayText(Map<View,List<AliasAwtComponent>> dependantViews) {
        StringBuilder builder = new StringBuilder();
        for (View view : dependantViews.keySet()) {
            builder.append(view.getName());
            List<AliasAwtComponent> aliasComponentsForRemoval = dependantViews.get(view);
            builder.append(" [");
            for (Iterator<AliasAwtComponent> i = aliasComponentsForRemoval.iterator(); i.hasNext();) {
                AliasAwtComponent aliasComponentForRemoval = i.next();
                builder.append(aliasComponentForRemoval.getAliasAction().getAliasedAction().get().getValue(Action.SHORT_DESCRIPTION));
                if(i.hasNext()){
                    builder.append(", ");
                }
            }
            builder.append(" ]");
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * ask the view to dispose itself
     */
    protected void disposeView(final View view) {
        viewToChangeTime.remove(view);
        ApplicationContext.getInstance().addGuiTask(new Task("Close view: " + view.getTabTitle()) {
            public void run() throws Throwable {
                Runnable runnable = new Runnable() {
                    public void run() {
                        view.dispose();
                    }
                };
                EventQueue.invokeAndWait(runnable);
            }
        });
    }

    private class CloseAllTabsAction extends AuditedAbstractAction {
        public CloseAllTabsAction() {
            putValue(Action.NAME, "Close All Tabs");
        }

        public void auditedActionPerformed(ActionEvent e) {
            while (tabbedPane.getTabCount() > 0) {
                Component component = tabbedPane.getTab(0);
                View tabView = getViewForComponent(component);
                closeTabWithConformation(tabView);
            }
        }
    }

    private class CloseAllTabsButCurrentAction extends AuditedAbstractAction {
        private View view;

        public CloseAllTabsButCurrentAction(View view) {
            this.view = view;
            putValue(Action.NAME, "Close All Tabs But Current");
        }

        public void auditedActionPerformed(ActionEvent e) {
            int componentCount = tabbedPane.getTabCount();
            ArrayList<View> listForRemoval = new ArrayList<View>();
            for (int i = 0; i < componentCount; i++) {
                Component component = tabbedPane.getTab(i);
                View tabView = getViewForComponent(component);
                if (tabView != view) {
                    listForRemoval.add(tabView);
                }
            }

            for (View viewToRemove : listForRemoval) {
                closeTabWithConformation(viewToRemove);
            }
        }
    }

    private class FloatTabAction extends AuditedAbstractAction {
        private View view;

        public FloatTabAction(View view) {
            this.view = view;
            putValue(Action.NAME, "Undock Tab as Window");
        }

        public void auditedActionPerformed(ActionEvent e) {
            removeView(view);
            addFrame(view);
        }
    }

    private class DuplicateTabAction extends AuditedAbstractAction {
        private View view;

        public DuplicateTabAction(View view) {
            this.view = view;
            putValue(Action.NAME, "Duplicate Tab");
        }

        public void auditedActionPerformed(ActionEvent e) {
            View copiedView = (View) BeanUtilities.verySlowDeepCopy(this.view);
            // Singleton views with modified persistence return the same instance, these should not be added as new tabs
            if (copiedView == this.view) {
                return;
            }
            addTab(copiedView);
        }

    }

    private class DuplicateWindowAction extends AuditedAbstractAction {
        private GenericWindow window;

        public DuplicateWindowAction(GenericWindow window) {
            this.window = window;
            putValue(Action.NAME, "Duplicate Window");
        }

        public void auditedActionPerformed(ActionEvent e) {
            FrameConfig frameConfig = getFrameConfig(window);
            addWindow((FrameConfig) BeanUtilities.verySlowDeepCopy(frameConfig));
        }
    }

    
    private GenericWindow getActiveWindow(Object source) {
        if(source instanceof Component) {
            JFrame f = UIUtilities.getFrameParent((Component)source);
            if(f instanceof GenericWindow) {
                return (GenericWindow)f;
            }
        }
        //Shouldn't get here but best to be safe
        return getApplicationFrame();
    }

    
    //todo:check toggle toolbar
    private class ToggleToolbarAction extends AuditedAbstractAction {
       // private GenericWindow window;

        public ToggleToolbarAction() {
            putValue(Action.NAME, "Toggle Toolbar");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
        }

        public void auditedActionPerformed(ActionEvent e) {
            
            GenericWindow window = getActiveWindow(e.getSource());
            Container contentPane = window.getContentPane();
            
            if (contentPane instanceof WindowPanel) {
                WindowPanel windowPanel = ((WindowPanel) contentPane);
                JToolBar toolBar = windowPanel.getJToolBar();
                if (toolBar == null) {
                    View view = getViewForWindow(window);
                    if (view == null){
                        view = getViewForComponent(tabbedPane.getSelectedComponent());
                    }
                    windowPanel.setJToolBar(createFrameJToolBar(view, window));
                    setToolBarOn();
                } else {
                    windowPanel.setJToolBar(null);
                    setToolBarOff();

                    // If the toolbar has focus, and gets hidden it will still have focus.  But now because it isn't part of the containement
                    // hierarchy anymore, it won't pass key events up to window and we won't be able to properly toggle.
                    window.getContentPane().requestFocusInWindow();
                }
                window.validate();
                window.repaint();
            }
        }
        public void setToolBarOn(){}
        public void setToolBarOff(){}
    }

    private class ToggleMenuAction extends AuditedAbstractAction {
        // private GenericWindow window;

        public ToggleMenuAction() {
            putValue(Action.NAME, "Toggle Menu");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
        }

        public void auditedActionPerformed(ActionEvent e) {
        	GenericWindow window = getActiveWindow(e.getSource());
              
            if (window.getJMenuBar() == null) {
                View view = getViewForWindow(window);
                if (view == null) {
                    view = getViewForComponent(tabbedPane.getSelectedComponent());
                }
                window.setJMenuBar(createFrameJMenuBar(view, window));
                setMenuBarOn();
            } else {
                window.setJMenuBar(null);
                setMenuBarOff();
            }

            window.validate();
            window.repaint();
        }
        public void setMenuBarOn(){}
        public void setMenuBarOff(){}

    }

    private class SetBringAllToFrontWhenFocussedAction extends AuditedAbstractAction {
        public SetBringAllToFrontWhenFocussedAction() {
            putValue(Action.NAME, "All windows on same layer");
            putValue(Action.SHORT_DESCRIPTION, "Bring all windows to the front when one window gets focus");
        }

        public void auditedActionPerformed(ActionEvent e) {
            JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
            GenericFrame.setBringAllToFrontWhenFocused(checkBoxMenuItem.isSelected());
        }
    }

    public MenuGroup createWindowMenuGroup(final GenericWindow window) {
        MenuGroup windowsMenuGroup = new MenuGroup("Windows", KeyEvent.VK_W);
        windowsMenuGroup.setHorizontalLayout(MenuGroup.RIGHT);

        // If this isn't the application frame, then add a dock item to the menu.
        if (window != applicationFrame) {
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new DockWindowInTabAction(window))));
        }

        windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new DockAllWindowsInTabAction())));

        if (window == applicationFrame) {
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new CloseAllTabsAction())));
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new CloseAllWindowsAction())));
            JCheckBoxMenuItem checkMenuItem = new JCheckBoxMenuItem(new SetBringAllToFrontWhenFocussedAction());
            checkMenuItem.setSelected(GenericFrame.isBringAllToFrontWhenFocussed());
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(checkMenuItem));
            windowsMenuGroup.addElement(new MenuGroup.SeparatorElement());
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ToggleMenuAction())));
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ToggleToolbarAction())));
            windowsMenuGroup.addElement(new MenuGroup.SeparatorElement());
        }

        if (window != applicationFrame) {
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new DuplicateWindowAction(window)))); 
            JCheckBoxMenuItem toggleAlwaysOnTopMenuItem = new JCheckBoxMenuItem(new ToggleAlwaysOnTopAction(window));
            toggleAlwaysOnTopMenuItem.setSelected(window.isAlwaysOntop());
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(toggleAlwaysOnTopMenuItem));
            windowsMenuGroup.addElement(new MenuGroup.SeparatorElement());
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ToggleMenuAction())));
            windowsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ToggleToolbarAction())));
            windowsMenuGroup.addElement(new MenuGroup.SeparatorElement());
        }

        return windowsMenuGroup;
    }

    private void addTab(final View view) {
        registerView(view);
        getTabbedPane().addView(view);
    }

    private void addTab(final View view, ViewBasedUserLayoutTabPane.ViewBackedTabMetaData tabMetaData, boolean selectNewTab) {
        registerView(view);
        getTabbedPane().addView(view, tabMetaData, selectNewTab);
    }

    protected void registerView(View view) {
        Component component = view.getComponent();
        componentToView.put(component, view);
        view.addChangeListener(changeListener);
    }

    public void removeAndDisposeView(String viewTitle) {
        for(Object o : tabbedPane.getViews()) {
            View view = (View)o;
            if(view.getTabTitle().equals(viewTitle)) {
                removeView(view);
                disposeView(view);
                break;
            }
        }
    }

    public void removeAndDisposeAllViews() {
        //Copy to a new list to avoid a ConcurrentMidificationException
        List views = new ArrayList(tabbedPane.getViews());
        for(Object o : views) {
            View view = (View)o;
            removeView(view);
            disposeView(view);
        }
    }

    public void removeView(View view) {
        tabbedPane.removeView(view);
        tabViews.remove(view);

        view.removeChangeListener(changeListener);
        componentToView.remove(view.getComponent());
    }

    //TODO tidy up getDependantViews and cleanUpAliasButtons
    /**
     * Returns a set of views that contains alias components that rely on actions
     * exported by the specified view. If no views are dependent returns an empty set
     * @param view
     * @return
     */
    public Map<View,List<AliasAwtComponent>> getDependantViews(View view) {
        List<WeakReference<Action>> exportedActions = view.getExportedActions();
        //No actions exported so no alias components to worry about
        if(exportedActions == null || exportedActions.isEmpty())  {
            return new HashMap<View,List<AliasAwtComponent>>();
        }
        Map<View,List<AliasAwtComponent>> dependantViews = new HashMap<View,List<AliasAwtComponent>>();
        Map<Long, View> openViews = ViewUtils.getViews();
        openViews.remove(view.getViewId());
        for(View openView : openViews.values()) {
            Map<String,AliasAwtComponent> aliasComponents = openView.getAliasComponents();
            for (AliasAwtComponent aliasAwtComponent : aliasComponents.values()) {
                Action aliasAction = aliasAwtComponent.getAliasAction().getAliasedAction().get();
                for(WeakReference<Action> actionWeakReference : exportedActions) {
                    Action exportedAction = actionWeakReference.get();
                    if (exportedAction.equals(aliasAction)) {
                        List<AliasAwtComponent> aliasComponentsForRemoval = dependantViews.get(openView);
                        if(aliasComponentsForRemoval == null) {
                            aliasComponentsForRemoval = new ArrayList<AliasAwtComponent>();
                            dependantViews.put(openView,aliasComponentsForRemoval);
                        }
                        aliasComponentsForRemoval.add(aliasAwtComponent);
                        break;
                    }
                }
            }
        }
        return dependantViews;
    }

    private void cleanUpAliasButtons(Map<View,List<AliasAwtComponent>> dependantViews) {
        for (View dependantView: dependantViews.keySet()) {
            List<AliasAwtComponent> aliasComponentsForRemoval = dependantViews.get(dependantView);
            for(AliasAwtComponent aliasAwtComponent : aliasComponentsForRemoval) {
                dependantView.removeAliasComponentFromToolbar(aliasAwtComponent.getIdentifier());
            }
        }
    }

    private void addWindow(final FrameConfig frameConfig) {
        final View view = frameConfig.getView();
        final Rectangle frameBounds = frameConfig.getFrameBounds();
        final Component viewComponent = view.getComponent();

        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                final GenericFrame window = createWindow(view, true);
                addAcceleratorActions(window.getRootPane(), view.getAcceleratorActions());
                window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                WindowPanel windowPanel = new WindowPanel(viewComponent);
                if (frameConfig.isShowToolbar()) {
                    windowPanel.setJToolBar(createFrameJToolBar(view, window));
                }
                window.setContentPane(windowPanel);

                if (frameConfig.isShowingMenu()) {
                    // Note createFrameJMenuBar has an order dependency on window.setContentPane because createWindowMenuGroup()
                    // calls hasToolbar() which looks at the content pane
                    window.setJMenuBar(createFrameJMenuBar(view, window));
                }

                JPanel glassPane = (JPanel) window.getGlassPane();
                glassPane.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(TOGGLE_MENUBAR_KEYSTROKE, TOGGLE_MENUBAR_ACTION_KEY);
                glassPane.getActionMap().put(TOGGLE_MENUBAR_ACTION_KEY, new ToggleMenuAction());
                glassPane.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(TOGGLE_TOOLBAR_KEYSTROKE, TOGGLE_TOOLBAR_ACTION_KEY);
                glassPane.getActionMap().put(TOGGLE_TOOLBAR_ACTION_KEY, new ToggleToolbarAction());
                glassPane.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(PRINT_COMPONENT_KEYSTROKE, PRINT_COMPONENT_ACTION_KEY);
                glassPane.getActionMap().put(PRINT_COMPONENT_ACTION_KEY, new PrintViewAction(view));
                glassPane.setVisible(true);

                window.addWindowListener(windowListener);
                window.setBounds(frameBounds);
                windowViews.put(window, view);
                view.addChangeListener(changeListener);
                if (!UIUtilities.isRectangleOnVisibleMonitor(frameBounds)) UIUtilities.centreInScreen(window, 0, 0);
                window.show();

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (frameConfig.isAlwaysOnTop()) {
                            window.setAlwaysOnTopOld(true);
                            window.setJMenuBar(createFrameJMenuBar(view, window));
                        }
                        focusInitialViewComponent(view);
                    }
                });
            }
        });
    }

    private void addFrame(View view) {
        FrameConfig config = new FrameConfig(getNextFrameBounds(), view);
        config.setShowingMenu(isShowMenuBar());
        config.setShowToolbar(isShowtoolBar());
        addWindow(config);
    }

    /**
     * This method is proof that public fields get abused.  Look what I've gone and done!
     *
     * @return a bounds rectangle the same size as as the tabbedpane, but offset a bit.
     */
    private Rectangle getNextFrameBounds() {
        Rectangle rect = (Rectangle) tabbedPane.getVisibleRect().clone();
        rect.x += SPAWN_OFFSET + tabbedPane.getLocationOnScreen().x;
        rect.y += SPAWN_OFFSET + tabbedPane.getLocationOnScreen().y;
        return rect;
    }

    private View removeWindow(GenericWindow window) {
        View view = getViewForWindow(window);
        view.removeChangeListener(changeListener);
        window.removeWindowListener(windowListener);
        window.dispose();
        windowViews.remove(window);
        return view;
    }

    private GenericFrame createWindow(View view, boolean closeByCntlW) {
        GenericFrame window = new GenericFrame(view.getFrameTitle(), closeByCntlW);

        ImageIcon viewImageIcon = view.getViewContext() != null && view.getViewContext().getImageIcon() != null ? view.getViewContext().getImageIcon() : view.getImageIcon();
        if (viewImageIcon != null) {
            window.setIconImage(viewImageIcon.getImage());
        }
        view.setLastActivated(new Date());

        return window;
    }

    private JToolBar createFrameJToolBar(View view) {
        ToolBarGroup applicationToolbarGroup = ApplicationContext.getInstance().getApplicationView().createToolBarGroup();
        ToolBarGroup viewToolBarGroup = view != null ? view.createToolBarGroup() : new ToolBarGroup("ViewToolBarGroup");
        ToolBarGroup contextToolBarGroup = view != null && view.getViewContext() != null ? view.getViewContext().createToolBarGroup() : new ToolBarGroup("ContextToolBarGroup");

        GroupedToolBar jToolBar = new GroupedToolBar();
        jToolBar.addToolBarGroups(applicationToolbarGroup, viewToolBarGroup, contextToolBarGroup);
        return jToolBar;
    }

    private JToolBar createFrameJToolBar(View view, RootPaneContainer window) {
        return useReorderableToolbar() ? createReorderableGroupedToolBar(view, window) : createFrameJToolBar(view);
    }

    private boolean useReorderableToolbar() {
        if(useReorderableToolbar == null) {
            String property = ApplicationProperties.getApplicationProperties().getProperty("useReorderableToolbar");
            useReorderableToolbar = "true".equalsIgnoreCase(property);
        }
        return useReorderableToolbar;
    }

    private JToolBar createReorderableGroupedToolBar(final View view, RootPaneContainer window) {
        ToolBarGroup[] applicationToolbarGroups = ApplicationContext.getInstance().getApplicationView().createToolBarGroup().splitOnSeparators();
        ToolBarGroup[] viewToolBarGroups = view != null ? view.createToolBarGroup().splitOnSeparators() : new ToolBarGroup[0];
        ToolBarGroup[] contextToolBarGroups = view != null && view.getViewContext() != null ? view.getViewContext().createToolBarGroup().splitOnSeparators() : new ToolBarGroup[0];

        final ReorderableGroupedToolBar jToolBar = new ReorderableGroupedToolBar(window);
        ToolBarGroup[] mergedGroups = applyOrdering(view, Utilities.appendArrays(applicationToolbarGroups, viewToolBarGroups, contextToolBarGroups));
        jToolBar.addToolBarGroups(mergedGroups);
        jToolBar.addPropertyChangeListener("order", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                toolbarGroupOrderMap.put(getGroupOrderKey(view), (List<String>) evt.getNewValue());
            }
        });
        jToolBar.addPropertyChangeListener("expanded", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                toolbarExpandedStateMap.put(getExpandStateKey(view), (Boolean)evt.getNewValue());
            }
        });
        Boolean expanded = toolbarExpandedStateMap.get(getExpandStateKey(view));
        if(expanded != null) {
            jToolBar.setExpanded(expanded);
        }

        return jToolBar;
    }

    private Class getGroupOrderKey(View view) {
        return view == null ? null : view.getClass();
    }

    private String getExpandStateKey(View view) {
        return view == null ? null : view.getName();
    }

    private ToolBarGroup[] applyOrdering(View view, ToolBarGroup[] groups) {
        if(groups == null || groups.length == 0) {
            return groups;
        }
        List<String> order = toolbarGroupOrderMap.get(getGroupOrderKey(view));
        if(order == null || order.isEmpty()) {
            return groups;
        }

        List<ToolBarGroup> oldGroups = new ArrayList<ToolBarGroup>(Arrays.asList(groups));
        List<ToolBarGroup> newGroups = new ArrayList<ToolBarGroup>(groups.length);
        for(String s : order) {
            ToolBarGroup group = findInList(oldGroups, s);
            if(group != null) {
                oldGroups.remove(group);
                newGroups.add(group);
            }
        }
        newGroups.addAll(oldGroups);
        return newGroups.toArray(new ToolBarGroup[newGroups.size()]);
    }

    private ToolBarGroup findInList(List<ToolBarGroup> list, String id) {
        for(ToolBarGroup group : list) {
            if(Utilities.equals(id, group.getId())) {
                return group;
            }
        }
        return null;
    }

    private JMenuBar createFrameJMenuBar(View view, GenericWindow window) {
        MenuGroup[] applicationMenuGroups = ApplicationContext.getInstance().getApplicationView().createMenuGroups(window);
        MenuGroup[] viewMenuGroups = view != null ? view.createMenuGroups(window) : new MenuGroup[]{};
        MenuGroup[] contextMenuGroups = view != null && view.getViewContext() != null ? view.getViewContext().createMenuGroups() : new MenuGroup[]{};

        JMenuBar jMenuBar = new JMenuBar();

        // Do the left menus, (Force the File Menu to the left by doing it last)
        MenuGroup fileMenuGroup = new MenuGroup("File", KeyEvent.VK_F);
        fileMenuGroup.setHorizontalLayout(MenuGroup.LEFT);
        MenuGroup.processMenuGroups(jMenuBar, contextMenuGroups, MenuGroup.LEFT);
        MenuGroup.processMenuGroups(jMenuBar, viewMenuGroups, MenuGroup.LEFT);
        MenuGroup.processMenuGroups(jMenuBar, applicationMenuGroups, MenuGroup.LEFT);
        MenuGroup.processMenuGroups(jMenuBar, new MenuGroup[]{fileMenuGroup}, MenuGroup.LEFT);

        // Do the right menus, (Force the Window and Help menus to be rightmost by doing them last)
        MenuGroup.processMenuGroups(jMenuBar, contextMenuGroups, MenuGroup.RIGHT);
        MenuGroup.processMenuGroups(jMenuBar, viewMenuGroups, MenuGroup.RIGHT);
        MenuGroup.processMenuGroups(jMenuBar, applicationMenuGroups, MenuGroup.RIGHT);
        MenuGroup.processMenuGroups(jMenuBar, new MenuGroup[]{new MenuGroup("Help", KeyEvent.VK_H)}, MenuGroup.RIGHT);

        return jMenuBar;
    }

    protected View getViewForWindow(GenericWindow window) {
        return windowViews.get(window);
    }

    protected View getViewForComponent(Component component) {
        return componentToView.get(component);
    }

    protected View getViewForTab(int tabIndex) {
        return tabViews.get(tabIndex);
    }

    protected int getTabIndexForView(View view) {
        return tabViews.indexOf(view);
    }

    private void setActiveTabView(View view) {
        activeTabView = view;
        if (view != null) {
            viewUpdated(view);
        }
    }

    public Collection<View> getViews() {
        List<View> views = new ArrayList<View>();
        for (View tabView : componentToView.values()) {
            views.add(tabView);
        }
        for (View view : windowViews.values()) {
            views.add(view);
        }
        return views;
    }

    public void forceUpdate() {
        setActiveTabView(activeTabView);
        for (View view : windowViews.values()) {
            if (view != null) {
                viewUpdated(view);
            }
        }
    }

    private void viewUpdated(View view) {
        Component component = view.getComponent();
        UserLayoutTabPane.TabMetaData tabMetaData = tabbedPane.getTabMetaData(component);
        if (tabMetaData == null) {//not a tab, therefore it must be a window
            Window windowAncestor = SwingUtilities.getWindowAncestor(component);
            assert (windowAncestor != getApplicationFrame()) : "apparently a tabView without tabMetaData";
            GenericWindow genericWindow = (GenericWindow) windowAncestor;
            WindowPanel windowPanel = ((WindowPanel) genericWindow.getContentPane());

            genericWindow.setTitle(view.getFrameTitle());
            if (genericWindow.getJMenuBar() != null) genericWindow.setJMenuBar(createFrameJMenuBar(view, genericWindow));
            if (windowPanel.getJToolBar() != null) windowPanel.setJToolBar(createFrameJToolBar(view, genericWindow));
        } else {    //a tab view
            tabMetaData.applyChangesToTab();
            if (view == activeTabView) {
                JMenuBar menuBar = createFrameJMenuBar(view, applicationFrame);
                JToolBar toolBar = createFrameJToolBar(view, applicationFrame);
                if (isShowMenuBar()) {
                    getApplicationFrame().setJMenuBar(menuBar);
                }
                if (isShowtoolBar()) {
                    applicationPanel.setJToolBar(toolBar);
                }
                setCurrentTabSpecificAcceleratorActions(view.getAcceleratorActions());
                focusInitialViewComponent(view);

                menuBar.updateUI();
            }
        }
    }

    private void focusInitialViewComponent(View view) {
        Component c = view.getComponentForInitialFocus();
        if ( c != null ) {
            c.requestFocusInWindow();
        }
    }

    //views can specify accelerator actions which should be enabled when that view is the 'active' view
    private void setCurrentTabSpecificAcceleratorActions(Action[] acceleratorActions) {
        removeAcceleratorActions(applicationFrame.getRootPane(), currentTabSpecificActions);
        currentTabSpecificActions = acceleratorActions;
        addAcceleratorActions(applicationFrame.getRootPane(), currentTabSpecificActions);
    }

    private void addAcceleratorActions(JComponent component, Action[] acceleratorActions) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();
        for ( Action a : acceleratorActions ) {
            inputMap.put(
                    (KeyStroke)a.getValue(Action.ACCELERATOR_KEY),
                    a.getValue(Action.ACTION_COMMAND_KEY)
            );
            actionMap.put(a.getValue(Action.ACTION_COMMAND_KEY), a);
        }
    }

    private void removeAcceleratorActions(JComponent component, Action[] acceleratorActions) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();
        for ( Action a : acceleratorActions ) {
            inputMap.remove((KeyStroke)a.getValue(Action.ACCELERATOR_KEY));
            actionMap.remove(a.getValue(Action.ACTION_COMMAND_KEY));
        }
    }

    protected FrameConfig getFrameConfig(GenericWindow window) {
        FrameConfig config = null;
        View view = getViewForWindow(window);
        if (BeanUtilities.hasPersistenceDelegate(view.getClass())) {
            config = new FrameConfig(window.getBounds(), view);
            config.setShowingMenu(window.getJMenuBar() != null);

            Container contentPane = window.getContentPane();
            if (contentPane instanceof WindowPanel) {
                config.setShowToolbar(((WindowPanel) contentPane).getJToolBar() != null);
            }

            config.setAlwaysOnTop(window.isAlwaysOntop());
        }
        return config;
    }

    private class DockWindowInTabAction extends AuditedAbstractAction {
        private GenericWindow window;

        public DockWindowInTabAction(GenericWindow window) {
            putValue(Action.NAME, "Dock Window as Tab");
            this.window = window;
        }

        public void auditedActionPerformed(ActionEvent e) {
            addTab(removeWindow(window));
        }
    }

    private class DockAllWindowsInTabAction extends AuditedAbstractAction {
        public DockAllWindowsInTabAction() {
            putValue(Action.NAME, "Dock All Windows as Tabs");
        }

        public void auditedActionPerformed(ActionEvent e) {
            GenericWindow[] windowArray = windowViews.keySet().toArray(new GenericWindow[windowViews.keySet().size()]);
            for (GenericWindow window : windowArray) {
                addTab(removeWindow(window));
            }
        }
    }

    private class CloseAllWindowsAction extends AuditedAbstractAction {
        public CloseAllWindowsAction() {
            putValue(Action.NAME, "Close All Windows");
        }

        public void auditedActionPerformed(ActionEvent e) {
            final GenericWindow[] windowArray = windowViews.keySet().toArray(new GenericWindow[windowViews.keySet().size()]);
            ApplicationContext.getInstance().addGuiTask(new Task("Close All Windows") {
                public void run() throws Throwable {
                    for (GenericWindow window : windowArray) {
                        View view = removeWindow(window);
                        disposeView(view);
                    }
                }
            });
        }
    }

    protected class SaveTask extends Task {
        public SaveTask() {
            super("Workspace");
        }

        public void run() {
            getStartupConfiguration().putObject(APPLICATION_FRAME_BOUNDS_CONFIG_KEY, applicationFrame.getNormalBounds());
            getStartupConfiguration().putObject(APPLICATION_FRAME_EXTENDED_STATE_CONFIG_KEY, applicationFrame.getExtendedState());
            getStartupConfiguration().putObject(TAB_TOOLTIPS_ON_KEY, isTabToolTipsOn());
            getStartupConfiguration().putObject(SHOW_MENU_BAR, isShowMenuBar());
            getStartupConfiguration().putObject(SHOW_TOOL_BAR, isShowtoolBar());
            getStartupConfiguration().putObject(COLOUMN_COLOUR_HAS_PRIORITY_KEY, isColumnColourHasPriority());

            View selectedTabView = getViewForComponent(tabbedPane.getSelectedComponent());
            if (selectedTabView != null && BeanUtilities.hasPersistenceDelegate(selectedTabView.getClass())) {
                getDefaultConfiguration().putObject(ACTIVE_TAB_VIEW_KEY, selectedTabView);
            }

            getDefaultConfiguration().putObject(TAB_PANE, getTabbedPane());
            getDefaultConfiguration().putObject(FRAME_CONFIG_KEY, getFrameConfigs());
            getDefaultConfiguration().putObject(TOOLBAR_GROUP_ORDER_MAP_KEY, toolbarGroupOrderMap);
            getDefaultConfiguration().putObject(TOOLBAR_EXPANDED_STATE_MAP_KEY, toolbarExpandedStateMap);
        }

        private FrameConfig[] getFrameConfigs() {
            ArrayList<FrameConfig> list = new ArrayList<FrameConfig>();
            for (Object o : windowViews.keySet()) {
                GenericWindow window = (GenericWindow) o;
                FrameConfig config = getFrameConfig(window);
                if (config != null) {
                    list.add(config);
                }
            }
            return list.toArray(new FrameConfig[list.size()]);
        }
    }

    public static class FrameConfig {
        private Rectangle frameBounds;
        private View view;
        private boolean showingMenu = true;
        private boolean showToolbar = true;
        private boolean alwaysOnTop = false;

        public FrameConfig(Rectangle frameBounds, View view) {
            this.frameBounds = frameBounds;
            this.view = view;
        }

        public Rectangle getFrameBounds() {
            return frameBounds;
        }

        public View getView() {
            return view;
        }

        public boolean isShowingMenu() {
            return showingMenu;
        }

        public void setShowingMenu(boolean showingMenu) {
            this.showingMenu = showingMenu;
        }

        public boolean isShowToolbar() {
            return showToolbar;
        }

        public void setShowToolbar(boolean showToolbar) {
            this.showToolbar = showToolbar;
        }

        public boolean isAlwaysOnTop() {
            return alwaysOnTop;
        }

        public void setAlwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = alwaysOnTop;
        }
    }

    protected void loadState() {
        this.applicationFrameRectangle = getStartupConfiguration().getObject(APPLICATION_FRAME_BOUNDS_CONFIG_KEY, new Rectangle(50, 50, 900, 500), Rectangle.class);
        this.applicationFrameExtendedState = getStartupConfiguration().getObject(APPLICATION_FRAME_EXTENDED_STATE_CONFIG_KEY, Frame.NORMAL, Integer.class);
    }

    public void activateState() {
        log.info("Activate state");
        final Log secondaryLoadingLog = Log.getSecondaryLoadingLog();
        getTabbedPane().init();
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                toolbarGroupOrderMap = getDefaultConfiguration().getObject(TOOLBAR_GROUP_ORDER_MAP_KEY, new HashMap<Class, List<String>>(), Map.class);
                toolbarExpandedStateMap = getDefaultConfiguration().getObject(TOOLBAR_EXPANDED_STATE_MAP_KEY, new HashMap<View, Boolean>(), Map.class);

                // this may seem a bit odd. We are retrieving a persited ViewBasedUserLayoutTabPane, pulling out its views
                // and adding them to our own tab pane. Then we set the "HeaderPanelController".
                // The HeaderPanelController is the bit that remembers the layout of the tabs. Since tabs have no
                // "primary key", the only way we can use a persisted HeaderPanelController in a different tab pane is to
                // use the same instances of the views. This is why we do the following.
                ViewBasedUserLayoutTabPane persistedTabPane = (ViewBasedUserLayoutTabPane) getDefaultConfiguration().getObjectAndRemove(TAB_PANE, new ViewBasedUserLayoutTabPane(), ViewBasedUserLayoutTabPane.class);
                int viewIndex = 0;
                for (Iterator iterator = persistedTabPane.getViews().iterator(); iterator.hasNext(); viewIndex++) {
                    View view = (View) iterator.next();
                    try {
                        secondaryLoadingLog.info("Building View '" + view.getName() + "'");
                        UserLayoutTabPane.TabMetaData tabMetaData = persistedTabPane.getTabMetaData(view);
                        Profiler.Task task = Profiler.taskStarted(Profiler.MAJOR_METHOD, "Add tab: '" + view.getTabTitle() + "'");
                        addTab(view, (ViewBasedUserLayoutTabPane.ViewBackedTabMetaData) tabMetaData, false);
                        Profiler.taskStopped(task);
                    } catch (Exception e) {
                        if (view == null) {
                            log.error("Error unpersisting tabView:  View object was null.  Index in array was " + viewIndex);
                        } else {
                            log.error("Error unpersisting tabView: " + view.getName() + " of type " + ClassUtilities.getUnqualifiedClassName(view.getClass()), e);
                        }
                    }
                }

                //now try to get views from the old persistence mechanism:
                View[] tempTabViews = (View[]) getDefaultConfiguration().getObjectAndRemove(TAB_CONFIG_KEY, new View[]{}, View[].class);
                for (int i = 0; i < tempTabViews.length; i++) {
                    final View view = tempTabViews[i];
                    try {
                        secondaryLoadingLog.info("Building View '" + view.getName() + "'");
                        addTab(view);
                    } catch (Exception e) {
                        if (view == null) {
                            log.error("Error unpersisting tabView:  View object was null.  Index in array was " + i);
                        } else {
                            log.error("Error unpersisting tabView: " + view.getName() + " of type " + ClassUtilities.getUnqualifiedClassName(view.getClass()), e);
                        }
                    }
                }

                getTabbedPane().setHeaderPanelController(persistedTabPane.getHeaderPanelController());

                secondaryLoadingLog.info("Set active tab");
                View selectedView = (View) getDefaultConfiguration().getObjectAndRemove(ACTIVE_TAB_VIEW_KEY, null, View.class);
                if (selectedView != null) {
                    getTabbedPane().setSelectedComponent(selectedView.getComponent());
                }
            }
        });
        secondaryLoadingLog.info("Start Tab Animations");
        tabColourAnim.scheduleWithFixedDelay(new TabFlasher(), 250, 250, TimeUnit.MILLISECONDS);
        applicationFrame.setVisible(true);

        FrameConfig[] tempFrameConfigs = (FrameConfig[]) getDefaultConfiguration().getObjectAndRemove(FRAME_CONFIG_KEY, new FrameConfig[]{}, FrameConfig[].class);
        for (final FrameConfig config : tempFrameConfigs) {
            try {
                secondaryLoadingLog.info("Building View '" + config.getView().getName() + "'");
                addWindow(config);
            } catch (Exception e) {
                log.error("Error unpersisting frameView: " + config.getView().getName() + " of type " + ClassUtilities.getUnqualifiedClassName(config.getView().getClass()), e);
            }
        }
        applicationFrame.toFront();
    }

    public void checkViewActivation() {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                Collection<View> allViews = getViews();
                Collection<View> notRecentlyActivated = new ArrayList<View>(allViews.size());
                for(View view : getViews()) {
                    Date lastActivated = view.getLastActivated();
                    if(lastActivated != null && System.currentTimeMillis() - lastActivated.getTime() > LAST_ACTIVATED_THRESHOLD) {
                        notRecentlyActivated.add(view);
                    }
                }
                if(!notRecentlyActivated.isEmpty()) {
                    ConfirmCloseViewsDialog dialog = new ConfirmCloseViewsDialog(UIUtilities.getFrameParent(getTabbedPane()), notRecentlyActivated);
                    notRecentlyActivated = dialog.getAllSelected();
                    if(!notRecentlyActivated.isEmpty()) {
                        for(View view : notRecentlyActivated) {
                            removeView(view);
                        }
                    }
                }
            }
        });
    }

    public void flashView(View view) {
        ViewChange viewChange = viewToChangeTime.get(view);
        if (viewChange == null) {
            viewChange = new ViewChange(changeFadeoutTime, flashColor, null);
        } else {
            viewChange.timeOfChange = System.currentTimeMillis();
        }

        viewToChangeTime.put(view, viewChange);
    }

    private class ViewChangedHandler implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            View view = (View) e.getSource();

            if (ApplicationContext.getInstance().getApplicationView() == view) {
                for (View aView : componentToView.values()) {
                    viewUpdated(aView);
                }
            } else {
                viewUpdated(view);
            }
        }
    }

    protected static class ViewChange {
        private long timeOfChange;
        private int duration;
        private Color originalColor;
        private Color flashColor;

        public ViewChange(int duration, Color flashColor, Color originalColor) {
            this.duration = duration;
            this.flashColor = flashColor;
            this.originalColor = originalColor;
            this.timeOfChange = System.currentTimeMillis();
        }
    }

    private class TabFlasher extends SwingTask {
        @Override
        public void runOnEventThread() {
            int count = getTabbedPane().getTabCount();
            for (int i = 0; i < count; i++) {
                Component component = getTabbedPane().getTab(i);
                View view = getViewForComponent(component);
                ViewChange viewChange = viewToChangeTime.get(view);
                if (viewChange != null) {
                    UserLayoutTabPane.TabMetaData tabMetaData = getTabbedPane().getTabMetaData(component);
                    if (viewChange.originalColor == null) {
                        viewChange.originalColor = tabMetaData.getColor();
                    }

                    float t = (float) (System.currentTimeMillis() - viewChange.timeOfChange) / (float) viewChange.duration;
                    if (t > 1.0) {
                        viewToChangeTime.remove(view);
                        tabMetaData.setColor(viewChange.originalColor);
                    } else {
                        t = 1 - t;
                        Color newColor = UIUtilities.blend(viewChange.flashColor, viewChange.originalColor, t);
                        tabMetaData.setColor(newColor);
                    }
                }
            }
        }
    }

    public String toString() {
        return "Workspace";
    }

    private static class ToggleAlwaysOnTopAction extends AuditedAbstractAction {
        private final GenericWindow window;

        public ToggleAlwaysOnTopAction(GenericWindow window) {
            super("Always On Top");
            this.window = window;
        }

        public void auditedActionPerformed(ActionEvent e) {
            window.setAlwaysOnTopOld(((JCheckBoxMenuItem) e.getSource()).isSelected());
        }

        public boolean isEnabled() {
            return true;
        }
    }

    private static class PrintViewAction extends AuditedAbstractAction implements Printable {
        private View view;

        public PrintViewAction(View view) {
            super("Print View");
            this.view = view;
        }

        public void auditedActionPerformed(ActionEvent e) {
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(this);
            if (printJob.printDialog())
                try {
                    printJob.print();
                } catch (PrinterException pe) {
                    log.error("Error printing view: " + view.getName(), pe);
                }
        }

        public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
            Component component = view.getComponent();
            if (pageIndex > 0) {
                return (NO_SUCH_PAGE);
            } else {
                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                RepaintManager currentManager = RepaintManager.currentManager(component);
                currentManager.setDoubleBufferingEnabled(false);
                component.paint(g2d);
                RepaintManager currentManager1 = RepaintManager.currentManager(component);
                currentManager1.setDoubleBufferingEnabled(true);
                return (PAGE_EXISTS);
            }
        }
    }

    private class SetColourAction extends AuditedAbstractAction {
        private Component component;
        private Color color;

        public SetColourAction(Component component, Color color, String name) {
            super(color.toString());
            putValue(Action.SMALL_ICON, new ColorIcon(color));
            putValue(Action.NAME, name);
            this.component = component;
            this.color = color;
        }

        public void auditedActionPerformed(ActionEvent e) {
            getTabbedPane().getTabMetaData(component).setColor(color);
        }
    }
}
