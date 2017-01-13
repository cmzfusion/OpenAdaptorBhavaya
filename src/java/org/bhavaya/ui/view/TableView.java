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
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.*;
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.ui.table.*;
import org.bhavaya.ui.table.column.dialog.ColumnManagementDialog;
import org.bhavaya.ui.table.column.dialog.ColumnManagementDialogModel;
import org.bhavaya.ui.table.column.dialog.ColumnHidingToolBarGroup;
import org.bhavaya.ui.table.column.dialog.ColumnListPanel;
import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlightColumnExclusionMap;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlightColumnExclusions;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;
import org.bhavaya.ui.table.formula.FormulaUtils;
import org.bhavaya.ui.table.formula.FormulaResult;
import org.bhavaya.util.*;
import org.bhavaya.util.Observable;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Currently implements a view on the database using several criterion.
 * <p/>
 * Class takes
 * adds Right click on
 * adds Column Chooser
 * adds Stuff.
 * <p/>
 * This class is either explicitly constructed, or by persistence delegate.
 *
 * @author Brendon McLean
 * @version $Revision: 1.93.4.3 $
 */

public class TableView extends AbstractView {
    private static final Log log = Log.getCategory(TableView.class);

    private static final String FLASHING_UPDATES_ON = "flashingUpdatesOn";

    private static final int[] AVAILABLE_FONT_SIZES = {10, 14, 16, 18, 24, 36};
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final String DEFAULT_STRING = "Default (" + DEFAULT_FONT_SIZE + ")";

    private static ImageIcon TABLE_ICON = ImageIconCache.getImageIcon("table_icon_16.gif");
    private static ImageIcon CREATE_CHART_ICON = ImageIconCache.getImageIcon("chart_icon_add_32.gif");
    private static ImageIcon VIEW_IN_EXCEL_ICON = ImageIconCache.getImageIcon("table_icon_view_excel_32.gif");
    private static ImageIcon SAVE_AS_CSV_ICON = ImageIconCache.getImageIcon("table_icon_save_csv_32.gif");
    private static ImageIcon PIVOT_ICON = ImageIconCache.getImageIcon("table_icon_pivot_32.gif");
    private static ImageIcon GROUP_ICON = ImageIconCache.getImageIcon("table_icon_group_32.gif");
    private static ImageIcon MANAGE_COLUMNS_ICON = ImageIconCache.getImageIcon(ColumnListPanel.ADD_TO_COL_GROUP_PATH);

    //    private static ImageIcon USE_COMPLETE_ICON = ImageIconCache.getImageIcon("table_icon_use_complete.png");
    //    private static ImageIcon USE_DATESERIES_ICON = ImageIconCache.getImageIcon("table_icon_use_dateseries.png");
    //    private static ImageIcon SPREAD_DATESERIES_ICON = ImageIconCache.getImageIcon("table_icon_spread_dateserie.png");
    private static ImageIcon ROW_TOTAL_ICON = ImageIconCache.getImageIcon("table_icon_row_total_32.gif");
    private static ImageIcon COL_TOTAL_ICON = ImageIconCache.getImageIcon("table_icon_col_total_32.gif");
    private static ImageIcon ADD_SELECTED_COL_ICON = ImageIconCache.getImageIcon("add.gif");
    private static ImageIcon REMOVE_SELECTED_COL_ICON = ImageIconCache.getImageIcon("remove.gif");
    private static ImageIcon REMOVE_ALL_SELECTED_COL_ICON = ImageIconCache.getImageIcon("remove_all.png");
    private static ImageIcon PIVOT_FLIP_ICON = ImageIconCache.getImageIcon("table_icon_invert_pivot_32.png");

    private static DateTimeRenderer SQL_DATE_RENDERER = new DateTimeRenderer(DateFormatProvider.DEFAULT_DATE_FORMAT, TimeZone.getTimeZone("GMT"));
    private static DateTimeRenderer DATETIME_RENDERER = new DateTimeRenderer(DateFormatProvider.DEFAULT_DATE_FORMAT + " - HH:mm:ss", TimeZone.getDefault());
    private static final DateTimeRenderer TIME_RENDERER = new DateTimeRenderer("HH:mm:ss", TimeZone.getDefault());
    private static DateFunctionRenderer DATE_FUNCTION_RENDERER = new DateFunctionRenderer(DateFormatProvider.DEFAULT_DATE_FORMAT, TimeZone.getTimeZone("GMT"));
    private static DateFunctionEditor DATE_FUNCTION_EDITOR = new DateFunctionEditor(new JTextField(), DateFormatProvider.DEFAULT_DATE_FORMAT, TimeZone.getTimeZone("GMT"));

    private static ApplicationColumnConfigMap columnConfigs;

    protected final static String PIVOT_FEATURE = "pivot";
    private static Set<String> hiddenTableFeatures = new HashSet<String>();

    public static final String SORTING_CHANGE_OPTION = "SortingChangeOption";
    public static final String OLD_SORTING_CHANGE_OPTION = "SortOnDoubleClick";

    public static final String CYCLE_TABLE_ON_ENTER = "CycleTableOnEnter";
    public static final String DISPLAY_FULL_TABLE_CELL_ON_HOVER = "DisplayFullTableCellOnHover";
    public static final String QUICK_SEARCH_ACCEPT_SUBSTRING = "QuickSearchAcceptSubstring";
    public static final String TABLE_ROW_TRACKING = "TableRowTrackingOn";
    public static final String TABLE_ROW_TRACKING_3D = "TableRowTracking3DOn";
    public static final String TABLE_ROW_TRACKING_COLOUR = "TableRowTrackingColour";
    public static final String TABLE_ROW_SELECTED_3D = "TableRowSelected3D";
    public static final String TABLE_SCROLL_TOOLBAR_ON = "TableScrollToolbarOn";


    private static Configuration globalConfiguration;
    private static boolean globalInited = false;
    private static boolean saveAll = false;

    private static volatile boolean enableHighlighterDebugging =
            Boolean.valueOf(System.getProperty("enableHighlighterDebuggingMenuOptions", "false"));

    private Task saveTask;
    private DateFormatUpdateListener dateFormatUpdateListener = new DateFormatUpdateListener();
    private List<java.util.regex.Pattern> searchRestrictionPatterns = new ArrayList<java.util.regex.Pattern>();
    private FilterTablePanel filterTablePanel;

    private int customEditorColIndex;
    private int customEditorRowIndex;
    private ColumnHidingToolBarGroup columnHidingToolBarGroup;
    private ColumnManagementDialog columnManagementDialog;

    static {
        try {
            columnConfigs = new ApplicationColumnConfigMap();
            // Check whether there are any features to hide.
            String[] hiddenTableFeatureStrings = ApplicationProperties.getApplicationProperties().getProperties("hiddenTableFeatures");
            if (hiddenTableFeatureStrings != null) hiddenTableFeatures.addAll(Arrays.asList(hiddenTableFeatureStrings));
        } catch (Throwable e) {
            log.error("Error in static initialiser", e);
        }
    }

    private static synchronized void globalSettingsInit() {
        if (!globalInited) {
            globalConfiguration = Configuration.getRoot().getConfiguration("GlobalTableSettings");
            Configuration.addSaveTask(new GlobalTableSettingsSaveTask());
            updateSortingChangeOption();
            updateCycleTableOption();
            updateDisplayFullCellValueOption();
            updateQuickSearchOption();
            updateDateFormat(DateFormatProvider.getInstance().getDateFormat());
            updateRowTrackingOption();
            updateRowTracking3DOption();
            updateRowTrackingColourOption();
            updateRowSelected3DOption();
            updateTableScrollToolbarMetaOption();

            listenForDateFormatChanges();
            globalInited = true;
        }
    }

    private static void listenForDateFormatChanges() {
        DateFormatProvider.getInstance().addPropertyChangeListener("dateFormat", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String dateFormat = (String) evt.getNewValue();
                updateDateFormat(dateFormat);
            }
        });
    }

    private static void updateDateFormat(String dateFormat) {
        SQL_DATE_RENDERER = new DateTimeRenderer(dateFormat, TimeZone.getTimeZone("GMT"));
        DATETIME_RENDERER = new DateTimeRenderer(dateFormat + " - HH:mm:ss", TimeZone.getDefault());
        DATE_FUNCTION_RENDERER = new DateFunctionRenderer(dateFormat, TimeZone.getTimeZone("GMT"));
        DATE_FUNCTION_EDITOR = new DateFunctionEditor(new JTextField(), dateFormat, TimeZone.getTimeZone("GMT"));
    }

    private static void updateQuickSearchOption() {
        Boolean quickSearchAcceptSubstring = globalConfiguration.getObject(QUICK_SEARCH_ACCEPT_SUBSTRING, Boolean.FALSE, Boolean.class);
        SearchableBeanPathSelector.setQuickSearchAcceptSubstring(quickSearchAcceptSubstring.booleanValue());
    }

    private static void updateCycleTableOption() {
        Boolean cycleTableOnEnter = globalConfiguration.getObject(CYCLE_TABLE_ON_ENTER, Boolean.TRUE, Boolean.class);
        HighlightedTable.setCycleTableOnEnter(cycleTableOnEnter.booleanValue());
    }

    private static void updateDisplayFullCellValueOption() {
        Boolean value = globalConfiguration.getObject(DISPLAY_FULL_TABLE_CELL_ON_HOVER, Boolean.FALSE, Boolean.class);
        HighlightedTable.setDisplayFullCellValueOnHover(value.booleanValue());
    }

    private static void updateRowTrackingOption(){
        Boolean value = globalConfiguration.getObject(TABLE_ROW_TRACKING, Boolean.TRUE, Boolean.class);
        HighlightedTable.setRowTrackingOn(value.booleanValue());
    }
    private static void updateRowTracking3DOption(){
        Boolean value = globalConfiguration.getObject(TABLE_ROW_TRACKING_3D, Boolean.TRUE, Boolean.class);
        HighlightedTable.setRowTrack3DOn(value.booleanValue());

    }
    private static void updateRowSelected3DOption(){
        Boolean value = globalConfiguration.getObject(TABLE_ROW_SELECTED_3D, Boolean.TRUE, Boolean.class);
        HighlightedTable.setRowSelected3DOn(value.booleanValue());

    }


    private static void updateTableScrollToolbarMetaOption(){
        Boolean value = globalConfiguration.getObject(TABLE_SCROLL_TOOLBAR_ON, Boolean.FALSE, Boolean.class);
        HighlightedTable.setTableMouseToolbarMetaOn(value.booleanValue());

    }
    private static void updateRowTrackingColourOption(){
        Color colour = globalConfiguration.getObject(TABLE_ROW_TRACKING_COLOUR, HighlightedTable.TABLE_TRACK_COLOUR, Color.class);
        HighlightedTable.setTrackPaintClr(colour);
    }

    private static void updateSortingChangeOption() {
        int sortingChangeOptionFinal;

        Integer sortingChangeOption = globalConfiguration.getObject(SORTING_CHANGE_OPTION, null, Integer.class);
        if (sortingChangeOption == null) {
            Boolean oldSortingChangeOption = globalConfiguration.getObject(OLD_SORTING_CHANGE_OPTION, null, Boolean.class);
            if (oldSortingChangeOption != null) {
                sortingChangeOptionFinal = oldSortingChangeOption.booleanValue() ? SortedTableModel.SORT_ON_DOUBLE_CLICK : SortedTableModel.SORT_ON_SINGLE_CLICK;
            } else {
                sortingChangeOptionFinal = SortedTableModel.SORT_ON_SINGLE_CLICK;
            }
        } else {
            sortingChangeOptionFinal = sortingChangeOption.intValue();
        }

        SortedTableModel.setSortingChangeOption(sortingChangeOptionFinal);
    }

    protected boolean isFeatureEnabled(String featureName) {
        return !hiddenTableFeatures.contains(featureName);
    }


    private AnalyticsTable analyticsTable;
    private AnalyticsTableModel analyticsTableModel;
    private Component viewComponent;

    protected String viewConfigurationId;
    private TableViewConfigurationMap userViewConfigurationMap;
    private FixedViewConfigurationMap fixedViewConfigurationMap;
    private TableViewConfigurationMap.ViewChangeListener viewChangeListener;
    private TableViewConfiguration lastAppliedConfiguration = null;

    private SearchableBeanPathSelector standardColumnControl;
    private SearchableBeanPathSelector pivotRowControl;
    private SearchableBeanPathSelector pivotColumnControl;
    private SearchableBeanPathSelector pivotDataColumnControl;

    protected Class recordType;
    protected BeanCollectionTableModel beanCollectionTableModel;
    private CardPanel editPanel;

    private JComponent pivotEditControl;
    private JComponent standardEditControl;
    private TableViewSelector tableViewSelector;
    private MenuPanel configurationMenuPanel;
    private MenuPanel editMenuPanel;

    private final HeaderMouseEventHandler headerMouseEventHandler = new HeaderMouseEventHandler();

    public TableView(String name, String tabTitle, String frameTitle, BeanCollectionTableModel beanCollectionTableModel, String viewConfigurationId) {
        super(name, tabTitle, frameTitle);
        assert beanCollectionTableModel != null : "Illegal arguments to TableView constructor";
        this.recordType = beanCollectionTableModel.getBeanType();
        this.viewConfigurationId = viewConfigurationId;
        this.beanCollectionTableModel = beanCollectionTableModel;
    }

    public BeanCollection getBeanCollection() {
        return beanCollectionTableModel.getBeanCollecton();
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        beanCollectionTableModel.setBeanCollection(beanCollection);
    }

    public BeanCollectionTableModel getBeanCollectionTableModel() {
        return beanCollectionTableModel;
    }

    public Class getRecordType() {
        return recordType;
    }

    public MenuGroup[] createMenuGroups(GenericWindow window) {
        List<MenuGroup> list = new ArrayList<MenuGroup>();
        list.addAll(Arrays.asList(super.createMenuGroups(window)));

        MenuGroup tableMenuGroup = new MenuGroup("Table", KeyEvent.VK_T);
        tableMenuGroup.setHorizontalLayout(MenuGroup.RIGHT);

        tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ManageColumnsAction())));
        if (isFeatureEnabled("exportToSpreadSheet")) tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new SaveTableAsCSVAction(false))));
        if (isFeatureEnabled("viewInExcel")) tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ViewInExcelAction(false))));
        if (isFeatureEnabled("createChart")) tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new CreateChartAction(false))));
        tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new FilterTableAction())));
        addHighlighterDebuggingMenuItems(tableMenuGroup);

        list.add(tableMenuGroup);
        return list.toArray(new MenuGroup[list.size()]);
    }

    private void addHighlighterDebuggingMenuItems(MenuGroup tableMenuGroup) {
        if (enableHighlighterDebugging) {
            //these actions can be added to help with table debugging
            Action showScrollableHighlightersAction = new AbstractAction("Show Scrollable Table Highlighters") {
                public void actionPerformed(ActionEvent e) {
                    getAnalyticsTable().showHighlighterChainEditorForScrollableTable();
                }
            };

            Action showFixedHighlightersAction = new AbstractAction("Show Fixed Table Highlighters") {
                public void actionPerformed(ActionEvent e) {
                    getAnalyticsTable().showHighlighterChainEditorForFixedTable();
                }
            };
            tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(showScrollableHighlightersAction)));
            tableMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(showFixedHighlightersAction)));
        }
    }

    public static void setEnableHighlighterDebugging(boolean enableHighlighterDebuggingMenuOptions) {
        TableView.enableHighlighterDebugging = enableHighlighterDebuggingMenuOptions;
    }

    public static boolean isEnableHighlighterDebugging() {
        return enableHighlighterDebugging;
    }

    public static MenuGroup.Element[] getGlobalPreferenceMenuElements() {
        globalSettingsInit();

        //sorting on single/double click or disabled
        JMenu sortingChangeOption = new JMenu("Sorting");
        ButtonGroup sortingChangeButtonGroup = new ButtonGroup();
        addSortingOnOptionToMenu(SortedTableModel.SORT_ON_SINGLE_CLICK, "On single mouse click", sortingChangeOption, sortingChangeButtonGroup);
        addSortingOnOptionToMenu(SortedTableModel.SORT_ON_DOUBLE_CLICK, "On double mouse click", sortingChangeOption, sortingChangeButtonGroup);
        addSortingOnOptionToMenu(SortedTableModel.SORT_ON_DISABLED_CLICK, "Disable mouse", sortingChangeOption, sortingChangeButtonGroup);

        JToggleButton.ToggleButtonModel cycleOnEnterButtonModel = new JToggleButton.ToggleButtonModel() {
            public boolean isSelected() {
                return HighlightedTable.isCycleTableOnEnter();
            }

            public void setSelected(boolean b) {
                super.setSelected(b);
                HighlightedTable.setCycleTableOnEnter(b);
            }
        };

        // not removing this as this might be used by other apps then ebond
        JMenu defaultDateFormatMenu = new JMenu("Default Date Format");
        ButtonGroup dateFormatButtonGroup = new ButtonGroup();
        for (int i = 0; i < DateFormatProvider.DATE_FORMATS.length; i++) {
            JRadioButtonMenuItem jRadioButtonMenuItem = new JRadioButtonMenuItem(new DateFormatProvider.SetDefaultDateFormatAction(DateFormatProvider.DATE_FORMATS[i]));
            dateFormatButtonGroup.add(jRadioButtonMenuItem);
            if (Utilities.equals(DateFormatProvider.getInstance().getDateFormat(), DateFormatProvider.DATE_FORMATS[i].getDateFormat()))
                jRadioButtonMenuItem.setSelected(true);
            defaultDateFormatMenu.add(jRadioButtonMenuItem);
        }

        JCheckBoxMenuItem cycleOnEnterMenuItem = new JCheckBoxMenuItem("Cycle on Enter");
        cycleOnEnterMenuItem.setModel(cycleOnEnterButtonModel);

        JToggleButton.ToggleButtonModel displayFullCellValueButtonModel = new JToggleButton.ToggleButtonModel() {
            public boolean isSelected() {
                return HighlightedTable.isDisplayFullCellValueOnHover();
            }

            public void setSelected(boolean b) {
                super.setSelected(b);
                HighlightedTable.setDisplayFullCellValueOnHover(b);
            }
        };

        JCheckBoxMenuItem displayFullCellValueMenuItem = new JCheckBoxMenuItem("Table Tool Tips");
        displayFullCellValueMenuItem.setModel(displayFullCellValueButtonModel);

        return new MenuGroup.Element[]{
            new MenuGroup.MenuItemElement(displayFullCellValueMenuItem),
            new MenuGroup.MenuItemElement(sortingChangeOption),
            new MenuGroup.MenuItemElement(cycleOnEnterMenuItem),
            new MenuGroup.MenuItemElement(defaultDateFormatMenu)
        };
    }

    private static void addSortingOnOptionToMenu(int sortOnOption, String optionName, JMenu sortingChangeOption, ButtonGroup sortingChangeButtonGroup) {
        JRadioButtonMenuItem jRadioButtonMenuItem = new JRadioButtonMenuItem(new SetSortingOptionAction(sortOnOption, optionName));
        sortingChangeButtonGroup.add(jRadioButtonMenuItem);
        sortingChangeOption.add(jRadioButtonMenuItem);
        if (sortOnOption == SortedTableModel.getSortingChangeOption()) jRadioButtonMenuItem.setSelected(true);
    }

    public Object getInitLock() {
        return getBeanCollectionTableModel().getChangeLock();
    }

    public boolean canCopyView(String viewConfigId) {
        return true;
    }

    protected void initImpl() {
        super.initImpl();
        globalSettingsInit();
        this.viewChangeListener = new ViewChangeHandler();
        this.userViewConfigurationMap = createUserViewConfigurationMap();
        this.fixedViewConfigurationMap = createFixedViewConfigurationMap();
        this.viewConfigurationId = (viewConfigurationId != null) ? viewConfigurationId : userViewConfigurationMap.getDefaultViewConfigurationId();

        analyticsTable = createAnalyticsTable();
        analyticsTableModel = analyticsTable.getAnalyticsTableModel();

        filterTablePanel = new FilterTablePanel(analyticsTable);

        setViewConfigurationIdAndApply(viewConfigurationId);

        // This helps with migration.  First we get the ViewConfiguration, then we set it.  During the set we may
        // make some changes that reflect a new persistence strategy.  We must then insert the view configuration into
        // the map again to prevent the user from having being presented with an unnecessary "Save Changes?" dialog.
        if (!FixedViewConfigurationMap.isFixedView(viewConfigurationId)) userViewConfigurationMap.setViewConfiguration(viewConfigurationId, getLastAppliedConfiguration(), false);

        // Set up the default renderers, could have these property configured.
        analyticsTable.setDefaultRenderer(Quantity.class, new QuantityRenderer());
        analyticsTable.setDefaultRenderer(Double.class, new DecimalRenderer());
        analyticsTable.setDefaultRenderer(Integer.class, new IntegerRenderer());
        analyticsTable.setDefaultRenderer(Float.class, new DecimalRenderer());
        analyticsTable.setDefaultRenderer(ScalableNumber.class, new DecimalRenderer());

        analyticsTable.setDefaultEditor(String.class, new StringEditor(new JTextField()));
        analyticsTable.setDefaultEditor(Double.class, new DecimalEditor(new DecimalTextField("###.#########", 9)));
        analyticsTable.setDefaultEditor(Integer.class, new DecimalEditor(new DecimalTextField("###", 9)));
        analyticsTable.setDefaultEditor(Float.class, new DecimalEditor(new DecimalTextField("###.#########", 9)));

        analyticsTable.setDefaultFilterFormatter(Double.class, new DecimalFormat(".#####"));
        analyticsTable.setDefaultFilterFormatter(Float.class, new DecimalFormat(".#####"));

        DateFormatProvider.getInstance().addPropertyChangeListener("dateFormat", dateFormatUpdateListener);
        updateDateRendererEditorAndFilterFormatter();

        //transfer handlers are intended to be sharable/have reentrant methods so should be ok to reuse the 1 instance
        TransferHandler transferHandler = createTransferHandler();
        analyticsTable.getFixedTable().setTransferHandler(transferHandler);
        analyticsTable.getScrollableTable().setTransferHandler(transferHandler);

        // Set up beanCollectionTableModel's editable fields
        SetStatement[] setStatements = getSetStatements();
        if (setStatements != null) {
            for (int i = 0; i < setStatements.length; i++) {
                beanCollectionTableModel.addSetStatement(setStatements[i]);
            }
        }

        analyticsTable.addTableHeaderMouseListener(headerMouseEventHandler);
        analyticsTable.setBeanActionFactory(new ViewBeanActionFactory(this));

        saveTask = new Task(getName()) {
            public void run() throws Throwable {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            save();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException("Error saving TableView: " + this, e);
                }
            }
        };
        TableViewConfigurationMap.addMapSaveTask(saveTask);

        analyticsTable.setBorder(new BevelBorder(BevelBorder.LOWERED));
        analyticsTable.setBackground(Workspace.getInstance().getTabbedPane().getBackground());
        viewComponent = createViewComponent(filterTablePanel);

        columnHidingToolBarGroup = new ColumnHidingToolBarGroup(analyticsTable.getTableColumns());

        //when groups change, update the group tool bar buttons and fire change to tell the view to recreate the toolbar
        analyticsTable.getTableColumns().addColumnGroupListener(new FixedColumnTableColumns.ColumnGroupListener() {
            public void groupsChanged(SortedSet<ColumnGroup> newGroups) {
                columnHidingToolBarGroup.groupsChanged(newGroups);
                fireViewChanged();
            }
        });

        ConditionalHighlightColumnExclusions exclusions = ConditionalHighlightColumnExclusionMap.getInstance().getExclusionsForClass(getClass());
        getConditionalHighlighter().setExclusions(exclusions);
    }

    public ConditionalHighlighter getConditionalHighlighter() {
        return getAnalyticsTable().getConditionalHighlighter();
    }

    private ColumnManagementDialog getOrCreateColumnEditingWindow() {
        if ( columnManagementDialog == null ) {
            columnManagementDialog = new ColumnManagementDialog(this,
                new ColumnManagementDialogModel(getAnalyticsTable().getTableColumns(), getAnalyticsTable().getConditionalHighlighter(), beanCollectionTableModel)
            );
            columnManagementDialog.setColumnMoveListener(new ColumnListPanel.ColumnMoveListener() {
                public void columnMoved(int columnId) {
                    getAnalyticsTable().scrollToColumn(columnId);
                }
            });
        }
        return columnManagementDialog;
    }

    public ToolBarGroup createToolBarGroup() {
        ToolBarGroup g = super.createToolBarGroup();
        g.addElement(new ToolBarGroup.ToolBarGroupElement(columnHidingToolBarGroup));
        return g;
    }

    public void setFilterEnterAction(Action action) {
        filterTablePanel.setFilterEnterAction(action);
    }

    private void save() {
        if (!saveAll) {
            if (isConfigurationModified()) {
                Workspace.getInstance().displayView(TableView.this);

                if (FixedViewConfigurationMap.isFixedView(viewConfigurationId)) {
                    handleFixedViewSave();
                } else {
                    handleUserViewSave();
                }
            }
        }
    }

    public void showFilterPanel(boolean show) {
        filterTablePanel.showFilterPanel(show);
    }

    public void showFilterPanel() {
        showFilterPanel(true);
    }

     /**
     * Subclasses may override to return a more specific implementation (e.g. to provide drop support)
     * @return TransferHandler for AnalyticsTable
     */
    protected TransferHandler createTransferHandler() {
        return new BeanTableTransferHandler();
    }

    public Action[] getAcceleratorActions() {
        return new Action[] {
                new FilterTableAction()
        };
    }

    public Component getComponentForInitialFocus() {
         return analyticsTable.getScrollableTable();
    }

    private void updateDateRendererEditorAndFilterFormatter() {
        analyticsTable.setDefaultRenderer(Date.class, DATETIME_RENDERER);
        analyticsTable.setDefaultRenderer(java.sql.Date.class, SQL_DATE_RENDERER);
        analyticsTable.setDefaultRenderer(java.sql.Time.class, TIME_RENDERER);
        analyticsTable.setDefaultRenderer(DateFunction.class, DATE_FUNCTION_RENDERER);

        analyticsTable.setDefaultEditor(DateFunction.class, DATE_FUNCTION_EDITOR);

        analyticsTable.setDefaultFilterFormatter(Date.class, DATETIME_RENDERER.getDateFormat());
        analyticsTable.setDefaultFilterFormatter(java.sql.Date.class, SQL_DATE_RENDERER.getDateFormat());
        analyticsTable.setDefaultFilterFormatter(java.sql.Time.class, TIME_RENDERER.getDateFormat());
        analyticsTable.setDefaultFilterFormatter(DateFunction.class, DATE_FUNCTION_RENDERER.getDateFormat());
    }

    protected AnalyticsTable createAnalyticsTable() {
        CustomEditorsAnalyticsTable customEditorsAnalyticsTable = new CustomEditorsAnalyticsTable(beanCollectionTableModel, Boolean.valueOf(ApplicationProperties.getApplicationProperties().getProperty(FLASHING_UPDATES_ON)));
        customEditorsAnalyticsTable.setProfileCellFading(ApplicationDiagnostics.getInstance().isLogBeanToScreenDelays());

        TypeSpecificColumnConfigFactory columnConfigFactory = new TypeSpecificColumnConfigFactory(customEditorsAnalyticsTable.getColumnConfigFactory());
        customEditorsAnalyticsTable.setColumnConfigFactory(columnConfigFactory);

        return customEditorsAnalyticsTable;
    }

    public Component getComponent() {
        init();
        return viewComponent;
    }

    protected ImageIcon getDefaultIcon() {
        return TABLE_ICON;
    }

    public TableViewConfiguration getViewConfiguration() {
        init();
        return analyticsTable.getViewConfiguration();
    }

    public AnalyticsTable getAnalyticsTable() {
        init();
        return analyticsTable;
    }

    public AnalyticsTableModel getAnalyticsTableModel() {
        init();
        return analyticsTableModel;
    }

    protected void setDragEnabled(boolean isEnabled) {
        analyticsTable.setDragEnabled(isEnabled);
    }

    protected void disposeImpl() {
        super.disposeImpl();
        if(tableViewSelector != null) tableViewSelector.dispose();
        if (!FixedViewConfigurationMap.isFixedView(viewConfigurationId)) {
            userViewConfigurationMap.removeConfigChangeListener(viewConfigurationId, viewChangeListener);
        }
        DateFormatProvider.getInstance().removePropertyChangeListener("dateFormat", dateFormatUpdateListener);
        analyticsTable.dispose();
        analyticsTable.setBeanActionFactory(null);
        analyticsTable.removeTableHeaderMouseListener(headerMouseEventHandler);
        analyticsTable = null;
        analyticsTableModel = null;
        if (standardColumnControl != null) standardColumnControl.dispose();
        if (pivotRowControl != null) pivotRowControl.dispose();
        if (pivotColumnControl != null) pivotColumnControl.dispose();
        if (pivotDataColumnControl != null) pivotDataColumnControl.dispose();
        TableViewConfigurationMap.removeMapSaveTask(saveTask);
        beanCollectionTableModel.dispose();
    }

    public String getViewConfigurationId() {
        if (viewConfigurationId == null) {
            return userViewConfigurationMap.getDefaultViewConfigurationId();
        }
        return viewConfigurationId;
    }

    private void handleFixedViewSave() {
        String[] options = new String[] {"Save as new", "Don't save"};
        int option = JOptionPane.showOptionDialog(UIUtilities.currentProgressDialog != null ? UIUtilities.currentProgressDialog : getComponent(),
            "You have unsaved changes on the " + viewConfigurationId + " view for " + BeanCollectionGroup.getDefaultInstance(getRecordType()).getPluralDisplayName() +
                    ".\nAs this is a default view, you will need to save as new to preserve changes.\nDo you want to save them?",
            "Save view?",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        switch (option) {
            case 0:
                tableViewSelector.saveNewView();
                break;
            case 1:
                //do nothing
                break;
        }
    }

    private void handleUserViewSave() {
        String[] options = new String[]{"Save", "Save All", "Save as new", "No"};
        int option = JOptionPane.showOptionDialog(UIUtilities.currentProgressDialog != null ? UIUtilities.currentProgressDialog : getComponent(),
                "You have unsaved changes on the " + viewConfigurationId + " view for " + BeanCollectionGroup.getDefaultInstance(getRecordType()).getPluralDisplayName() + ".\nDo you want to save them?",
                "Save view?",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (option) {
            case 0:
                saveCurrentView();
                break;
            case 1:
                saveAll = true;
                saveCurrentView();
                break;
            case 2:
                tableViewSelector.saveNewView();
                break;
            case 3:
                //do nothing
                break;
        }
    }

    protected void saveCurrentView() {
        setLastAppliedConfiguration(getViewConfiguration());
        userViewConfigurationMap.setViewConfiguration(viewConfigurationId, getLastAppliedConfiguration(), true);
    }

    public void setViewConfigurationIdAndApply(String viewConfigurationId) {
        setViewConfigurationIdAndApply(viewConfigurationId, false);
    }

    /**
     * @param viewConfigurationId
     * @param forceOverwrite      if set then the user will not be consulted about losing changes made to their view
     */
    public void setViewConfigurationIdAndApply(String viewConfigurationId, boolean forceOverwrite) {
        String currentViewConfigurationId = getViewConfigurationId();

        if (!FixedViewConfigurationMap.isFixedView(currentViewConfigurationId)) {
            if (!forceOverwrite && getLastAppliedConfiguration() != null && !getViewConfiguration().equals(getLastAppliedConfiguration())) {
                String[] options = new String[]{"Save", "Save as new", "No", "Cancel"};
                int option = JOptionPane.showOptionDialog(getComponent(), "You have made changes to the " + currentViewConfigurationId + " view.\nDo you want to save them?",
                        "Save view?", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                switch (option) {
                    case 0:
                        saveCurrentView();
                        break;
                    case 1:
                        tableViewSelector.saveNewView();
                        break;
                    case 2:
                        //do nothing
                        break;
                    case 3:
                        if (log.isDebug()) log.debug("Abort setting the view config");
                        return;
                }
            }
        }

        TableViewConfiguration newViewConfiguration = getViewConfiguration(viewConfigurationId);

        if (newViewConfiguration == null) {
            log.info("Could not find config id: " + viewConfigurationId + " using: " + TableViewConfigurationMap.DEFAULT_CONFIG_ID);
            viewConfigurationId = TableViewConfigurationMap.DEFAULT_CONFIG_ID;
            newViewConfiguration = getViewConfiguration(viewConfigurationId);
        }

        if (!FixedViewConfigurationMap.isFixedView(viewConfigurationId)) userViewConfigurationMap.removeConfigChangeListener(this.viewConfigurationId, viewChangeListener);
        this.viewConfigurationId = viewConfigurationId;
        if (!FixedViewConfigurationMap.isFixedView(viewConfigurationId)) userViewConfigurationMap.addConfigChangeListener(this.viewConfigurationId, viewChangeListener);

        analyticsTable.setViewConfiguration(newViewConfiguration);
        setLastAppliedConfiguration(getViewConfiguration());
        if (editPanel != null) editPanel.setSelectedComponent(getEditControl());

        showFilterPanel(newViewConfiguration.isShowFilterFindPanel());

        fireViewChanged();
    }

    protected TableViewConfiguration getViewConfiguration(String viewConfigurationId) {
        return FixedViewConfigurationMap.isFixedView(viewConfigurationId)
                ? fixedViewConfigurationMap.getViewConfiguration(viewConfigurationId)
                : userViewConfigurationMap.getViewConfiguration(viewConfigurationId);
    }

    protected TableViewConfigurationMap createUserViewConfigurationMap() {
        return TableViewConfigurationMap.getInstance(recordType.getName());
    }

    protected FixedViewConfigurationMap createFixedViewConfigurationMap() {
        return FixedViewConfigurationMap.getInstance(recordType.getName());
    }

    public TableViewConfigurationMap getUserViewConfigurationMap() {
        return userViewConfigurationMap;
    }

    public FixedViewConfigurationMap getFixedViewConfigurationMap() {
        return fixedViewConfigurationMap;
    }

    protected MenuPanel[] createMenuPanels() {
        return new MenuPanel[]{getConfigurationsMenuPanel(), getEditMenuPanel()};
    }

    protected MenuPanel getConfigurationsMenuPanel() {
        if (configurationMenuPanel == null) {
            tableViewSelector = new TableViewSelector(this);
            MenuPanel menuPanel = new MenuPanel("Configurations", tableViewSelector, SplitPanel.RIGHT, false, KeyEvent.VK_C, false);
            menuPanel.setSplitterOffset(200);
            configurationMenuPanel = menuPanel;
        }
        return configurationMenuPanel;
    }

    protected MenuPanel getEditMenuPanel() {
        if (editMenuPanel == null) {
            MenuPanel menuPanel = new MenuPanel("Edit", getEditPanel(), SplitPanel.RIGHT, true, KeyEvent.VK_E, false);
            menuPanel.setSplitterOffset(200);
            editMenuPanel = menuPanel;
        }
        return editMenuPanel;
    }

    /**
     * @return SetStatements perform an abstract write operation on the selected beans' property.  For instance, could
     *         send a message to a server.
     */
    private SetStatement[] getSetStatements() {
        if (getViewContext() != null) return getViewContext().getSetStatements();
        return null;
    }

    /**
     * allows a custom table view to override the cell editor used
     */
    protected TableCellEditor getEditorForColumn(Object columnKey, TableCellEditor defaultEditor) {
        if (defaultEditor != null && defaultEditor.getClass() == DecimalEditor.class) {
            TableCellRenderer cellRenderer = getAnalyticsTable().getColumn(columnKey).getCellRenderer();
            if (cellRenderer != null && cellRenderer instanceof DecimalRenderer) {
                int precision = ((DecimalRenderer) cellRenderer).getPrecision();
                ((DecimalEditor) defaultEditor).getDecimalTextField().getEditFormat().setMaximumFractionDigits(precision);
                ((DecimalEditor) defaultEditor).getDecimalTextField().getEditFormat().setMinimumFractionDigits(precision);
                ((DecimalEditor) defaultEditor).getDecimalTextField().getDisplayFormat().setMinimumFractionDigits(precision);
                ((DecimalEditor) defaultEditor).getDecimalTextField().getDisplayFormat().setMinimumFractionDigits(precision);
            }
        }

        return defaultEditor;
    }

    protected TableCellRenderer getRendererForColumn(Object columnKey, TableCellRenderer defaultRenderer) {
        return defaultRenderer;
    }

    public CardPanel getEditPanel() {
        if (editPanel == null) {
            editPanel = new CardPanel();
            editPanel.addComponent(getStandardEditControl());
            editPanel.addComponent(getPivotEditControl());
            editPanel.setSelectedComponent(getEditControl());
            analyticsTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                        editPanel.setSelectedComponent(getEditControl());
                        TableModel sourceModel = (TableModel) e.getSource();
                        sourceModel.removeTableModelListener(this);
                    }
                }
            });
        }
        return editPanel;
    }

    void openEditPanel() {
        splitPanel.setVisibleMenuPanel(getEditMenuPanel());
    }

    private Component getEditControl() {
        return analyticsTableModel != null && analyticsTableModel.isPivoted() ? getPivotEditControl() : getStandardEditControl();
    }

    private JComponent createPivotEditControl() {
        JComponent pivotRowControlComponent = getPivotRowControl();
        pivotRowControlComponent.setBackground(Workspace.getInstance().getTabbedPane().getBackground());

        JComponent pivotColumnControlComponent = getPivotColumnControl();
        pivotColumnControlComponent.setBackground(Workspace.getInstance().getTabbedPane().getBackground());

        JComponent pivotDataControlComponent = getPivotDataControl();
        pivotDataControlComponent.setBackground(Workspace.getInstance().getTabbedPane().getBackground());

        final String[] SELECTION_NAMES = {"Pivot Rows", "Pivot Column", "Pivot Data"};
        final java.util.List SELECTION_NAME_LIST = Arrays.asList(SELECTION_NAMES);
        final JComponent[] SELECTION_COMPONENTS = {pivotRowControlComponent, pivotColumnControlComponent, pivotDataControlComponent};

        final CardPanel cardPanel = new CardPanel();
        cardPanel.addComponent(SELECTION_COMPONENTS[0]);
        cardPanel.addComponent(SELECTION_COMPONENTS[1]);
        cardPanel.addComponent(SELECTION_COMPONENTS[2]);

        final JComboBox selectionComboBox = new JComboBox(SELECTION_NAMES);
        selectionComboBox.setSelectedIndex(0);
        selectionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComponent selectedComponent = SELECTION_COMPONENTS[SELECTION_NAME_LIST.indexOf(selectionComboBox.getSelectedItem())];
                SearchableBeanPathSelector selector = (SearchableBeanPathSelector) selectedComponent;
                selector.reset();
                cardPanel.setSelectedComponent(selectedComponent);
            }
        });

        JToolBar columnControlToolBar = new JToolBar();
        columnControlToolBar.setFloatable(false);
        columnControlToolBar.setOpaque(false);
        columnControlToolBar.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        columnControlToolBar.add(UIUtilities.createLabelledComponent("Select", selectionComboBox));

        Box topPanel = Box.createVerticalBox();
        topPanel.add(createEditControlToolBar(true));
        topPanel.add(createEditControlMenusPanel());
        topPanel.add(columnControlToolBar);

        JPanel container = new JPanel(new BorderLayout());
        container.add(topPanel, BorderLayout.NORTH);
        container.add(cardPanel, BorderLayout.CENTER);
        return container;
    }

    private JComponent getPivotEditControl() {
        if (pivotEditControl == null) {
            pivotEditControl = createPivotEditControl();
        }
        return pivotEditControl;
    }

    private JComponent createStandardEditControl() {
        Box topPanel = Box.createVerticalBox();
        topPanel.add(createEditControlToolBar(false));
        topPanel.add(createEditControlMenusPanel());
        topPanel.add(createColumnControlPanel());

        JPanel container = new JPanel(new BorderLayout());
        container.add(topPanel, BorderLayout.NORTH);
        container.add(getStandardColumnControl(), BorderLayout.CENTER);
        return container;
    }

    protected JToolBar createEditControlToolBar(boolean pivoted) {
        JToolBar tableControlToolBar = new JToolBar();
        tableControlToolBar.setFloatable(false);
        tableControlToolBar.setOpaque(false);
        tableControlToolBar.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        if (isFeatureEnabled("group")) {
            JToggleButton groupingButton = new JToggleButton(GROUP_ICON);
            groupingButton.setModel(new BooleanToggleModel(analyticsTableModel, "grouped"));
            groupingButton.setToolTipText("Toggle Grouping (Aggregation)");
            tableControlToolBar.add(groupingButton);
        }

        if (isFeatureEnabled(PIVOT_FEATURE)) {
            JToggleButton pivotButton = new JToggleButton(PIVOT_ICON);
            pivotButton.setModel(new BooleanToggleModel(analyticsTableModel, "pivoted"));
            pivotButton.setToolTipText("Toggle Pivoting");
            pivotButton.addActionListener(new PivotListener());
            tableControlToolBar.add(pivotButton);
        }

        if (isFeatureEnabled("rowTotal")) {
            JToggleButton rowTotalButton = new JToggleButton(ROW_TOTAL_ICON);
            rowTotalButton.setModel(new BooleanToggleModel(analyticsTableModel, "rowTotallingEnabled"));
            rowTotalButton.setToolTipText("Toggle Row Totals");
            tableControlToolBar.add(rowTotalButton);
        }

        if (pivoted) {
            if (isFeatureEnabled("columnTotals")) {
                JToggleButton colTotalButton = new JToggleButton(COL_TOTAL_ICON);
                colTotalButton.setModel(new BooleanToggleModel(analyticsTableModel, "columnTotallingEnabled"));
                colTotalButton.setToolTipText("Toggle Column Totals");
                tableControlToolBar.add(colTotalButton);
            }

            if (isFeatureEnabled("invertPivot")) {
                JButton invertButton = new JButton(new InvertPivotTableAction());
                invertButton.setToolTipText("Swap Rows and Columns");
                tableControlToolBar.add(invertButton);
            }
        }

        tableControlToolBar.add(new JSeparator(JSeparator.VERTICAL));

        JButton columnEditingButton = new JButton(MANAGE_COLUMNS_ICON);
        columnEditingButton.setToolTipText("Manage Columns");
        columnEditingButton.addActionListener(new ManageColumnsAction());
        tableControlToolBar.add(columnEditingButton);

        return tableControlToolBar;
    }

    private JToolBar createEditControlMenusPanel() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JButton settingsButton = new JButton();
        settingsButton.setAction(new ShowPopupAction(createEditControlMenus(), settingsButton));
        toolBar.add(settingsButton);

        return toolBar;
    }

    protected JMenu[] createEditControlMenus() {
        java.util.List<JMenu> menus = new ArrayList<JMenu>();

        JMenu colourSettingsMenu = createColourSettingsMenu();
        if (colourSettingsMenu.getMenuComponents().length > 0) {
            menus.add(colourSettingsMenu);
        }

        JMenu dateSeriesSettingsMenu = createDateSeriesSettingsMenu();
        if (dateSeriesSettingsMenu.getMenuComponents().length > 0) {
            menus.add(dateSeriesSettingsMenu);
        }

        JMenu fontSettingsMenu = createFontSettingsMenu();
        if (fontSettingsMenu.getMenuComponents().length > 0) {
            menus.add(fontSettingsMenu);
        }

        JMenu quickSearchSettingsMenu = createQuickSearchSettingsMenu();
        if (quickSearchSettingsMenu.getMenuComponents().length > 0) {
            menus.add(quickSearchSettingsMenu);
        }

        return menus.toArray(new JMenu[menus.size()]);
    }

    private JMenu createQuickSearchSettingsMenu() {
        JMenu quickSearchSettingsMenu = new JMenu("Quick Search Settings");
        quickSearchSettingsMenu.setOpaque(false);

        if (isFeatureEnabled("quickSearchSubstring")) {
            JCheckBoxMenuItem option = new JCheckBoxMenuItem("Search for substring");

            JToggleButton.ToggleButtonModel model = new JToggleButton.ToggleButtonModel() {
                public boolean isSelected() {
                    return SearchableBeanPathSelector.isQuickSearchAcceptSubstring();
                }

                public void setSelected(boolean b) {
                    super.setSelected(b);
                    SearchableBeanPathSelector.setQuickSearchAcceptSubstring(b);
                }
            };

            option.setModel(model);
            option.setToolTipText("Search for substring");
            quickSearchSettingsMenu.add(option);
        }

        return quickSearchSettingsMenu;
    }

    private JMenu createColourSettingsMenu() {
        JMenu colourSettingsMenu = new JMenu("Colour Settings");
        colourSettingsMenu.setOpaque(false);

        if (isFeatureEnabled("animated")) {
            JCheckBoxMenuItem animatedOption = new JCheckBoxMenuItem("Highlight cell changes");
            animatedOption.setModel(new BooleanToggleModel(analyticsTable, "animated"));
            animatedOption.setToolTipText("Highlight cell changes");
            colourSettingsMenu.add(animatedOption);
        }

        if (isFeatureEnabled("fading")) {
            JCheckBoxMenuItem fadingOption = new JCheckBoxMenuItem("Fade cell highlights");
            fadingOption.setModel(new BooleanToggleModel(analyticsTable, "fading"));
            fadingOption.setToolTipText("Fade cell highlights");
            colourSettingsMenu.add(fadingOption);
        }

        if (isFeatureEnabled("highlightNewRows")) {
            JCheckBoxMenuItem highlightNewRowsOption = new JCheckBoxMenuItem("Highlight new rows");
            highlightNewRowsOption.setModel(new BooleanToggleModel(analyticsTable, "highlightNewRows"));
            highlightNewRowsOption.setToolTipText("Highlight new rows");
            colourSettingsMenu.add(highlightNewRowsOption);
        }

        if (isFeatureEnabled("columnColourHasPriority")) {
            JCheckBoxMenuItem columnColourHasPriority = new JCheckBoxMenuItem("Column colour has priority");
            columnColourHasPriority.setModel(new BooleanToggleModel(analyticsTable, "columnColourHasPriority"));
            columnColourHasPriority.setToolTipText("Column colour has priority");
            colourSettingsMenu.add(columnColourHasPriority);
        }

        return colourSettingsMenu;
    }


    private JMenu createDateSeriesSettingsMenu() {
        JMenu dateSeriesSettingsMenu = new JMenu("Date Series Settings");
        dateSeriesSettingsMenu.setOpaque(false);

        if (isFeatureEnabled("useDateSeries")) {
            JCheckBoxMenuItem dateSeriesButton = new JCheckBoxMenuItem("Use Date Series");
            dateSeriesButton.setModel(new BooleanToggleModel(analyticsTableModel, "usingDateSeries"));
            dateSeriesButton.setToolTipText("Use Date Series");
            dateSeriesSettingsMenu.add(dateSeriesButton);
        }

        if (isFeatureEnabled("dateSeriesVerbose")) {
            JCheckBoxMenuItem verboseSeriesButton = new JCheckBoxMenuItem("Verbose");
            verboseSeriesButton.setModel(new BooleanToggleModel(analyticsTableModel, "verboseDateSeries"));
            verboseSeriesButton.setToolTipText("View values as either 'begin->end' or just 'end'");
            dateSeriesSettingsMenu.add(verboseSeriesButton);
        }

        if (isFeatureEnabled("useFullSeries")) {
            JCheckBoxMenuItem useFullSeriesButton = new JCheckBoxMenuItem("Show Empty Buckets");
            useFullSeriesButton.setModel(new BooleanToggleModel(analyticsTableModel, "useFullSeries"));
            useFullSeriesButton.setToolTipText("Show Empty Buckets");
            dateSeriesSettingsMenu.add(useFullSeriesButton);
        }

        if (isFeatureEnabled("spreadDateBuckets")) {
            JCheckBoxMenuItem dateBucketSpreadingButton = new JCheckBoxMenuItem("Spread Buckets");
            dateBucketSpreadingButton.setModel(new BooleanToggleModel(analyticsTableModel, "dateBucketSpreading"));
            dateBucketSpreadingButton.setToolTipText("Spread Buckets");
            dateSeriesSettingsMenu.add(dateBucketSpreadingButton);
        }

        return dateSeriesSettingsMenu;
    }

    private JMenu createFontSettingsMenu() {
        // sets the table font size
        JMenu fontSettingsMenu = new JMenu("Font Settings");
        fontSettingsMenu.setOpaque(false);

        if (isFeatureEnabled("fontSizeMenu")) {
            JCheckBoxMenuItem boldMenuItem = new JCheckBoxMenuItem(new ToggleTableFontStyleAction("Bold", Font.BOLD));
            boldMenuItem.setSelected(analyticsTable.getFont().isBold());
            JCheckBoxMenuItem italicsMenuItem = new JCheckBoxMenuItem(new ToggleTableFontStyleAction("Italic", Font.ITALIC));
            italicsMenuItem.setSelected(analyticsTable.getFont().isItalic());

            ButtonGroup fontSizeButtonGroup = new ButtonGroup();
            ActionListener setFontSizeAction = new SetFontSizeAction();
            JMenuItem[] fontSizeMenuItems = new JMenuItem[AVAILABLE_FONT_SIZES.length + 1];

            fontSizeMenuItems[0] = new JRadioButtonMenuItem();
            fontSizeMenuItems[0].setText(DEFAULT_STRING);
            fontSizeMenuItems[0].addActionListener(setFontSizeAction);
            fontSettingsMenu.add(fontSizeMenuItems[0]);
            fontSizeButtonGroup.add(fontSizeMenuItems[0]);

            for (int i = 1; i <= AVAILABLE_FONT_SIZES.length; i++) {
                fontSizeMenuItems[i] = new JRadioButtonMenuItem();
                fontSizeMenuItems[i].setText(Integer.toString(AVAILABLE_FONT_SIZES[i - 1]));
                fontSizeMenuItems[i].addActionListener(setFontSizeAction);
                fontSizeButtonGroup.add(fontSizeMenuItems[i]);
                fontSettingsMenu.add(fontSizeMenuItems[i]);
            }

            int fontSize = getAnalyticsTable().getFontSize();
            for (int i = 0; i < AVAILABLE_FONT_SIZES.length; i++) {
                int availableFontSize = AVAILABLE_FONT_SIZES[i];
                if (availableFontSize == fontSize) {
                    fontSizeMenuItems[i + 1].setSelected(true);
                    break;
                } else if (fontSize == DEFAULT_FONT_SIZE) {
                    fontSizeMenuItems[0].setSelected(true);
                }
            }
            fontSettingsMenu.addSeparator();
            fontSettingsMenu.add(boldMenuItem);
            fontSettingsMenu.add(italicsMenuItem);
        }

        return fontSettingsMenu;
    }

    private JComponent createColumnControlPanel() {
        JToolBar columnControlToolBar = new JToolBar();
        columnControlToolBar.setFloatable(false);
        columnControlToolBar.setOpaque(false);
        columnControlToolBar.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        columnControlToolBar.add(new JButton(new ChangeSelectedColumnsAction(getStandardColumnControl(), true)));
        columnControlToolBar.add(new JButton(new ChangeSelectedColumnsAction(getStandardColumnControl(), false)));
        columnControlToolBar.add(new JButton(new RemoveAllColumnsAction()));
        return columnControlToolBar;
    }

    private JComponent getStandardEditControl() {
        if (standardEditControl == null) {
            standardEditControl = createStandardEditControl();
        }
        return standardEditControl;
    }

    private SearchableBeanPathSelector getStandardColumnControl() {
        if (standardColumnControl == null) {
            standardColumnControl = new SearchableBeanPathSelector(beanCollectionTableModel,
                    FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER,
                    FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER,
                    new StandardColumnSelectionModel(),
                    searchRestrictionPatterns);
        }
        return standardColumnControl;
    }

    private SearchableBeanPathSelector getPivotRowControl() {
        if (pivotRowControl == null) {
            FilteredTreeModel.Filter pivotRowFilter = new FilteredTreeModel.Filter() {
                public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
                    AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
                    return !propertyBeanPath.equals(analyticsTableModel.getPivotColumn())
                            && !propertyBeanPath.equals(analyticsTableModel.getPivotDataColumn())
                            && FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER.isValidProperty(beanPathRootClass, parentClass, propertyClass, propertyName, propertyBeanPath);
                }
            };
            pivotRowControl = new SearchableBeanPathSelector(beanCollectionTableModel,
                    pivotRowFilter,
                    FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER,
                    new StandardColumnSelectionModel(),
                    searchRestrictionPatterns);
        }
        return pivotRowControl;
    }

    private SearchableBeanPathSelector getPivotColumnControl() {
        if (pivotColumnControl == null) {
            FilteredTreeModel.Filter pivotDataFilter = new FilteredTreeModel.Filter() {
                public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
                    return FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER.isValidProperty(beanPathRootClass, parentClass, propertyClass, propertyName, propertyBeanPath)
                            && analyticsTableModel.getTablePivoter().getSuitablePivotColumnFilter().isValidPivotColumn(propertyBeanPath);
                }
            };
            pivotColumnControl = new SearchableBeanPathSelector(beanCollectionTableModel, pivotDataFilter, FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER, new PivotColumnSelectionModel(), searchRestrictionPatterns);
        }
        return pivotColumnControl;
    }

    private SearchableBeanPathSelector getPivotDataControl() {
        if (pivotDataColumnControl == null) {
            FilteredTreeModel.Filter pivotDataFilter = new FilteredTreeModel.Filter() {
                public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
                    boolean validProperty = FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER.isValidProperty(beanPathRootClass, parentClass, propertyClass, propertyName, propertyBeanPath);
                    if (!validProperty) return false;
                    if (analyticsTableModel.getTablePivoter().getSuitableDataColumnFilter().isValidDataColumn(propertyBeanPath)) {
                        return true;
                    }
                    return Observable.class.isAssignableFrom(propertyClass);
                }
            };
            pivotDataColumnControl = new SearchableBeanPathSelector(beanCollectionTableModel, pivotDataFilter, FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER, new PivotDataColumnSelectionModel(), searchRestrictionPatterns);
        }
        return pivotDataColumnControl;
    }

    /**
     * Add a pattern used to restrict search in bean path selector when the user is searching for columns
     * to add to the table view. This must be called before the view is first rendered
     */
    protected void addBeanPathSelectorSearchRestriction(java.util.regex.Pattern p) {
        searchRestrictionPatterns.add(p);
    }

    public boolean isConfigurationModified() {
        return !Utilities.equals(getViewConfiguration(), getLastAppliedConfiguration());
    }

    public ConfigListModel getViewConfigListModel() {
        return new ViewConfigListModel();
    }

    public ListCellRenderer getConfigNameCellRenderer() {
        return new ConfigNameCellRenderer();
    }

    protected JPopupMenu createColumnConfigPopupMenu(Class columnClass, int modelIndex, Object columnKey, boolean pivotGeneratedCol) {
        return new ColumnConfigPopupMenu(
                columnClass,
                modelIndex,
                columnKey,
                pivotGeneratedCol,
                analyticsTableModel,
                analyticsTable,
                getBeanCollection(),
                recordType,
                this
        );
    }

    private class HeaderMouseEventHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            checkPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            checkPopup(e);
        }

        private void checkPopup(MouseEvent e) {
            JTable table = ((JTableHeader) e.getSource()).getTable();
           final int currentColumnIdx = table.columnAtPoint(e.getPoint());

            if (e.isPopupTrigger() && e.getSource() instanceof JTableHeader && currentColumnIdx != -1) {
                Class columnClass = table.getColumnClass(currentColumnIdx);
                final int modelIndex = table.getColumnModel().getColumn(currentColumnIdx).getModelIndex();
                final Object columnKey = analyticsTableModel.getColumnKey(modelIndex);
                final boolean pivotGeneratedCol = columnKey instanceof PivotTableModel.GeneratedColumnKey;

                JPopupMenu columnConfigPopupMenu = createColumnConfigPopupMenu(columnClass, modelIndex, columnKey, pivotGeneratedCol);
                columnConfigPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

    }

    private class ToggleTableFontStyleAction extends ColumnConfigPopupMenu.ToggleFontStyleAction {
        public ToggleTableFontStyleAction(String name, int style) {
            super(name, style);
        }

        public void auditedActionPerformed(ActionEvent e) {
            Font currentFont = analyticsTable.getFont();
            analyticsTable.setFont(currentFont.deriveFont(currentFont.getStyle() ^ getStyle()));
        }
    }



    private class SetFontSizeAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            int tableSize = actionCommand.equals(DEFAULT_STRING) ? DEFAULT_FONT_SIZE : Integer.parseInt(actionCommand);
            analyticsTable.setFontSize(tableSize);
        }
    }

    private static class SetSortingOptionAction extends AuditedAbstractAction {
        private int option;

        public SetSortingOptionAction(int option, String name) {
            super(name, "Set Sorting Option");
            this.option = option;
        }

        public void auditedActionPerformed(ActionEvent e) {
            SortedTableModel.setSortingChangeOption(option);
        }
    }

    private class IntegerRenderer extends DefaultTableCellRenderer {
        private NumberFormat INTEGER_FORMAT = NumberFormat.getInstance();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value == null) return component;

            // A wee hack to decide whether to format Integer or not.
            Object columnKey = analyticsTableModel.getColumnKey(table.getColumnModel().getColumn(column).getModelIndex());
            if (columnKey instanceof String) {
                String propertyName = (String) columnKey;
                if (!propertyName.toLowerCase().endsWith("id")) {
                    component.setText(INTEGER_FORMAT.format(value));
                    component.setHorizontalAlignment(JLabel.RIGHT);
                }
            }

            return this;
        }
    }

    protected final class SaveTableAsCSVAction extends AuditedAbstractAction {
        public SaveTableAsCSVAction(boolean useIcon) {
            putValue(Action.NAME, "Export Table to CSV File...");
            putValue(Action.SHORT_DESCRIPTION, "Save Table in Excel-compatible CSV format");
            if (useIcon) {
                putValue(Action.SMALL_ICON, SAVE_AS_CSV_ICON);
            }
        }

        public void auditedActionPerformed(ActionEvent e) {
            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException

            JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
            fileChooser.setDialogTitle("Save Table to CSV file (Excel compatible spreadsheet format)");
            fileChooser.setSelectedFile(new File(replaceSymbols(getName()) + ".csv"));
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
                }

                public String getDescription() {
                    return "Excel compatible spreadsheet format (*.csv)";
                }
            });

            String fileName;
            int returnVal = fileChooser.showSaveDialog(UIUtilities.getWindowParent((Component) e.getSource()));

            System.setSecurityManager(backup);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fileName = fileChooser.getSelectedFile().getAbsolutePath();
            } else {
                return;
            }

            try {
                IOUtilities.writeStringToFile(fileName, UIUtilities.asCommaDelimitedString(analyticsTable, true));
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(UIUtilities.getWindowParent((Component) e.getSource()), e1.getMessage(), "Error saving data", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private static String replaceSymbols(String string) {
        StringBuffer buffer = new StringBuffer(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == 45 || (c >= 48 && c <= 57) || (c >= 65 && c <= 90) || c == 95 || (c >= 97 && c <= 122)) {
                buffer.append(c);
            } else {
                buffer.append("_");
            }
        }
        return buffer.toString();
    }

    protected final class ManageColumnsAction extends AuditedAbstractAction {
        public ManageColumnsAction() {
            super("Manage Columns");
        }

        public void auditedActionPerformed(ActionEvent e) {
            ColumnManagementDialog w = getOrCreateColumnEditingWindow();
            if ( w.isVisible()) {
                w.setVisible(false);
            } else {
                int state = w.getExtendedState();
                // Clear the iconified bit
                state &= ~Frame.ICONIFIED;
                // Deiconify the frame
                w.setExtendedState(state);
                w.setLocationRelativeTo(getAnalyticsTable());
                w.setVisible(true);
            }
        }
    }

    protected final class ViewInExcelAction extends AuditedAbstractAction {
        public ViewInExcelAction(boolean useIcon) {
            putValue(Action.NAME, "View in Excel");
            putValue(Action.SHORT_DESCRIPTION, "View Table in Microsoft Excel");
            if (useIcon) {
                putValue(Action.SMALL_ICON, VIEW_IN_EXCEL_ICON);
            }
        }

        public void auditedActionPerformed(ActionEvent e) {
            try {
                File file = File.createTempFile(replaceSymbols(getName()), ".csv");
                IOUtilities.writeStringToFile(file, UIUtilities.asCommaDelimitedString(analyticsTable, true));

                String osName = System.getProperty("os.name");

                String cmd;
                if (osName.indexOf("Windows") >= 0) {
                    if (osName.indexOf("98") >= 0 || osName.indexOf("95") >= 0) {
                        cmd = "command /c " + file.getAbsolutePath();
                    } else {
                        cmd = "cmd /c " + file.getAbsolutePath();
                    }
                } else {
                    log.warn("Do not know how to launch file for OS: " + osName);
                    cmd = null;
                }

                if (cmd != null) {
                    if (log.isDebug()) log.debug("Executing external process: " + cmd);
                    Runtime.getRuntime().exec(cmd);
                }
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(UIUtilities.getWindowParent((Component) e.getSource()), e1.getMessage(), "Error creating temporary file", JOptionPane.ERROR_MESSAGE);
                log.error(e1);
            }
        }

        public boolean isEnabled() {
            return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
        }
    }

    protected final class BooleanToggleModel extends JToggleButton.ToggleButtonModel {
        private Object target;
        private String propertyName;

        public BooleanToggleModel(Object target, String propertyName) {
            this.target = target;
            this.propertyName = propertyName;
            super.setSelected(isSelected());
        }

        public boolean isSelected() {
            if (target == null) return false;
            return ((Boolean) Generic.get(target, propertyName)).booleanValue();
        }

        public void setSelected(boolean b) {
            Generic.set(target, propertyName, Boolean.valueOf(b));
            fireViewChanged();
        }
    }

    protected final class CreateChartAction extends AuditedAbstractAction {
        public CreateChartAction(boolean useIcon) {
            putValue(Action.NAME, "View as Chart");
            putValue(Action.SHORT_DESCRIPTION, "View the current Table as a Chart");
            if (useIcon) {
                putValue(Action.SMALL_ICON, CREATE_CHART_ICON);
            }
        }

        public void auditedActionPerformed(ActionEvent evt) {
            BeanCollection newBeanCollection;

            BeanCollection tableBeanCollection = getBeanCollection();
            newBeanCollection = (BeanCollection) BeanUtilities.verySlowDeepCopy(tableBeanCollection);

            TableViewConfiguration currentTableConfigCopy = (TableViewConfiguration) BeanUtilities.verySlowDeepCopy(getAnalyticsTable().getViewConfiguration());
            ChartView chartView = new ChartView(getName(), getTabTitle(), getFrameTitle(), newBeanCollection);
            chartView.configureFromTableViewConfiguration(currentTableConfigCopy);
            JOptionPane.showMessageDialog(getComponent(), "Please be aware that chart views are still under development.");
            Workspace.getInstance().displayView(chartView);
        }
    }

    private class ChangeSelectedColumnsAction extends AuditedAbstractAction {
        private SearchableBeanPathSelector beanPathSelector;
        private boolean add;

        public ChangeSelectedColumnsAction(SearchableBeanPathSelector beanPathSelector, boolean add) {
            putValue(Action.SMALL_ICON, add ? ADD_SELECTED_COL_ICON : REMOVE_SELECTED_COL_ICON);
            putValue(Action.SHORT_DESCRIPTION, add ? "Add selected columns" : "Remove selected columns");
            this.beanPathSelector = beanPathSelector;
            this.add = add;
        }

        public void auditedActionPerformed(ActionEvent e) {
            beanPathSelector.setSelected(add);
        }
    }

    private class RemoveAllColumnsAction extends AuditedAbstractAction {
        public RemoveAllColumnsAction() {
            putValue(Action.SMALL_ICON, REMOVE_ALL_SELECTED_COL_ICON);
            putValue(Action.SHORT_DESCRIPTION, "Remove all columns");
        }

        public void auditedActionPerformed(ActionEvent e) {
            beanCollectionTableModel.removeAllColumnLocators();
        }
    }

    private class InvertPivotTableAction extends AuditedAbstractAction {
        public InvertPivotTableAction() {
            putValue(Action.SMALL_ICON, PIVOT_FLIP_ICON);
            putValue(Action.SHORT_DESCRIPTION, "Swap rows and columns");
        }

        public void auditedActionPerformed(ActionEvent e) {
            getAnalyticsTableModel().invertPivot();
            //nodes may have been added or removed by doing this, so get the trees to reset
            pivotRowControl.reset();
            pivotColumnControl.reset();
            pivotDataColumnControl.reset();
        }
    }

    public static final class DrillDownAction extends AuditedAbstractAction {
        private Object[] sourceData;
        private TableView sourceView;

        public DrillDownAction(Object[] sourceData, TableView sourceView) {
            this.sourceData = sourceData;
            this.sourceView = sourceView;
            String displayName = Utilities.getPluralName(ClassUtilities.getUnqualifiedClassName(sourceView.getRecordType()));
            putValue(Action.NAME, "Drill down to " + displayName + " (" + sourceData.length + " rows)");
        }

        public void auditedActionPerformed(ActionEvent e) {
            BeanCollection drillDownCollection = new DefaultBeanCollection(sourceView.getRecordType());
            drillDownCollection.addAll(Arrays.asList(sourceData));

            BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(drillDownCollection, true);
            TableView drillDownView = new TableView("Drill down from '" + sourceView.getName() + "'",
                    "Drill Down from '" + sourceView.getTabTitle() + "'",
                    "Drill down from '" + sourceView.getFrameTitle() + "'", beanCollectionTableModel, null);
            Workspace.getInstance().displayView(drillDownView);
        }
    }

    private class ViewChangeHandler implements TableViewConfigurationMap.ViewChangeListener {
        public void viewChanged(TableViewConfiguration tableViewConfiguration) {
            if (!tableViewConfiguration.equals(getLastAppliedConfiguration())) { //only bother if it is a new view config
                if (!saveAll && isConfigurationModified()) { //if the user has modified their version of the view...
                    Component selectedComponent = Workspace.getInstance().getTabbedPane().getSelectedComponent();
                    View currentWorkspaceView = Workspace.getInstance().getViewForComponent(selectedComponent);

                    Workspace.getInstance().displayView(TableView.this);
                    Object[] options = new String[]{"Overwrite it", "Save as new configuration"};
                    int ret = JOptionPane.showOptionDialog(getComponent(),
                            "Changes have been made to the " + getViewConfigurationId() + " configuration on another table\n" +
                                    "What do you want to do with the configuration of this table?",
                            "Question", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (ret == 1) {
                        tableViewSelector.saveNewView();
                        Workspace.getInstance().displayView(currentWorkspaceView);
                        return;
                    }
                }
                setViewConfigurationIdAndApply(viewConfigurationId, true);  //force the change
            }
        }
    }

    private class PivotListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            editPanel.setSelectedComponent(getEditControl());
        }
    }

    private class StandardColumnSelectionModel extends BeanPathSelector.SelectionModel {
        public StandardColumnSelectionModel() {
            super(true);
        }

        public void locatorSelected(String columnLocator) {
            beanCollectionTableModel.setColumnVisibleByLocator(columnLocator, !isSelected(columnLocator));
            if (isSelected(columnLocator)) {
                Rectangle visibleRect = analyticsTable.getVisibleRect();
                Point topLeft = new Point(visibleRect.x, visibleRect.y);
                int topVisibleRow = analyticsTable.rowAtPoint(topLeft);

                Rectangle newVisibleRect = analyticsTable.getCellRect(topVisibleRow, analyticsTable.getColumnCount() - 1, true);
                analyticsTable.getViewport().scrollRectToVisible(newVisibleRect);
            }
        }

        public boolean isSelected(String columnLocator) {
            return analyticsTableModel.getColumnIndex(columnLocator) >= 0;
        }
    }

    private class PivotDataColumnSelectionModel extends BeanPathSelector.SelectionModel {
        public PivotDataColumnSelectionModel() {
            super(false);
        }

        public void locatorSelected(String columnLocator) {
            String oldColumn = getAnalyticsTableModel().getPivotDataColumn();
            if (!Utilities.equals(columnLocator, oldColumn)) {
                beanCollectionTableModel.setColumnVisibleByLocator(columnLocator, true);
                getAnalyticsTableModel().setPivotDataColumn(columnLocator);
                beanCollectionTableModel.setColumnVisibleByLocator(oldColumn, false);
            }
        }

        public boolean isSelected(String columnLocator) {
            String pivotDataColumn = getAnalyticsTableModel().getPivotDataColumn();
            return Utilities.equals(pivotDataColumn, columnLocator);
        }
    }

    private class PivotColumnSelectionModel extends BeanPathSelector.SelectionModel {
        public PivotColumnSelectionModel() {
            super(false);
        }

        public void locatorSelected(String columnLocator) {
            String oldColumn = getAnalyticsTableModel().getPivotColumn();
            if (!Utilities.equals(columnLocator, oldColumn)) {
                beanCollectionTableModel.setColumnVisibleByLocator(columnLocator, true);
                getAnalyticsTableModel().setPivotColumn(columnLocator);
                beanCollectionTableModel.setColumnVisibleByLocator(oldColumn, false);
            }
        }

        public boolean isSelected(String columnLocator) {
            String pivotDataColumn = getAnalyticsTableModel().getPivotColumn();
            return Utilities.equals(pivotDataColumn, columnLocator);
        }
    }

    private static class ViewBeanActionFactory implements BeanActionFactory {
        private TableView tableView;

        public ViewBeanActionFactory(TableView tableView) {
            this.tableView = tableView;
        }

        public ActionGroup getActions(Object[] beans) {
            if (beans != null) {
                for (int i = 0; i < beans.length; i++) {
                    Object bean = beans[i];
                    if (bean != null) BeanFactory.resetLazyObject(bean);
                }
            }
            return tableView.getViewContext().getActionsForBeanArray(beans);
        }


        public Action getAction(Object bean, String beanPath) {
            if (bean != null) BeanFactory.resetLazyObject(bean);
            return tableView.getViewContext().getActionForBeanPath(bean, beanPath);
        }

        public AcceleratorAction[] getAcceleratorActions() {
            return tableView.getViewContext().getAcceleratorActions();
        }
    }

    protected Object[] getBeansForEditedCell() {
        return getBeansForCustomEditorsTable();
    }

    protected Object[] getBeansForRenderedCell() {
        return getBeansForCustomEditorsTable();
    }

    private Object[] getBeansForCustomEditorsTable() {
        return analyticsTable.getBeansForLocation(
            customEditorRowIndex,
            customEditorColIndex
        );
    }

    private class CustomEditorsAnalyticsTable extends AnalyticsTable {
        public CustomEditorsAnalyticsTable(KeyedColumnTableModel tableModel, boolean animated) {
            super(tableModel, animated);
        }

        protected FixedForCustomRowHeightsJTable createFixedTable(TableModel tableModel, TableColumnModel tableColumnModel) {
            return new CustomEditorsHighlightedTable(tableModel, tableColumnModel, false);
        }

        protected FixedForCustomRowHeightsJTable createScrollableTable(TableModel tableModel, TableColumnModel tableColumnModel) {
            return new CustomEditorsHighlightedTable(tableModel, tableColumnModel, true);
        }

        public void setProfileCellFading(boolean logBeanToScreenDelays) {
            fixedTable.setProfileCellFading(logBeanToScreenDelays);
            scrollableTable.setProfileCellFading(logBeanToScreenDelays);
        }

        private class CustomEditorsHighlightedTable extends CustomHighlightedTable {

            public CustomEditorsHighlightedTable(TableModel tableModel, TableColumnModel tableColModel, boolean autoCreateColsOnInsert) {
                super(tableModel);
                setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                setColumnModel(tableColModel);
                setAutoCreateColumnsOnInsert(autoCreateColsOnInsert);
            }

            public TableCellEditor getCellEditor(int row, int column) {
                TableColumn col = getColumnModel().getColumn(column);
                Object columnKey = col.getIdentifier();
                customEditorColIndex = col.getModelIndex();
                customEditorRowIndex = row;
                TableCellEditor tableCellEditor = super.getCellEditor(row, column);
                tableCellEditor = getEditorForColumn(columnKey, tableCellEditor);
                return tableCellEditor;
            }

            public TableCellRenderer getCellRenderer(int row, int column) {
                TableColumn col = getColumnModel().getColumn(column);
                Object columnKey = col.getIdentifier();
                customEditorColIndex = col.getModelIndex();
                customEditorRowIndex = row;
                TableCellRenderer cellRenderer = super.getCellRenderer(row, column);
                cellRenderer = getRendererForColumn(columnKey, cellRenderer);
                return cellRenderer;
            }
        }
    }

    private static class GlobalTableSettingsSaveTask extends Task {
        public GlobalTableSettingsSaveTask() {
            super("Global Table Settings");
        }

        public void run() {
            globalConfiguration.putObject(SORTING_CHANGE_OPTION, new Integer(SortedTableModel.getSortingChangeOption()));
            globalConfiguration.putObject(CYCLE_TABLE_ON_ENTER, Boolean.valueOf(HighlightedTable.isCycleTableOnEnter()));
            globalConfiguration.putObject(DISPLAY_FULL_TABLE_CELL_ON_HOVER, Boolean.valueOf(HighlightedTable.isDisplayFullCellValueOnHover()));
            globalConfiguration.putObject(QUICK_SEARCH_ACCEPT_SUBSTRING, Boolean.valueOf(SearchableBeanPathSelector.isQuickSearchAcceptSubstring()));
            globalConfiguration.putObject(TABLE_ROW_TRACKING, Boolean.valueOf(HighlightedTable.isRowTrackingOn()));
            globalConfiguration.putObject(TABLE_ROW_TRACKING_3D, Boolean.valueOf(HighlightedTable.isRowTrack3DOn()));
            globalConfiguration.putObject(TABLE_ROW_TRACKING_COLOUR, HighlightedTable.getTrackPaintClr());
            globalConfiguration.putObject(TABLE_ROW_SELECTED_3D, HighlightedTable.isRowSelected3DOn());
            globalConfiguration.putObject(TABLE_SCROLL_TOOLBAR_ON, HighlightedTable.isTableMouseToolbarMetaOn());
        }
    }

    public static void setSaveAll(boolean saveAll) {
        TableView.saveAll = saveAll;
    }

    public static boolean getSaveAll() {
        return saveAll;
    }

    private class TypeSpecificColumnConfigFactory implements ColumnConfigFactory {
        private ColumnConfigFactory defaultFactory;

        public TypeSpecificColumnConfigFactory(ColumnConfigFactory defaultFactory) {
            this.defaultFactory = defaultFactory;
        }

        public ColumnConfig getColumnConfigForColumn(Object columnKey) {
            Class rootBeanClass = TableView.this.getBeanCollection().getType();
            String beanPath = getAnalyticsTableModel().getBeanPathForColumn(columnKey);
            ColumnConfig applicationSpecificColumnConfig = getApplicationSpecificColumnConfig(rootBeanClass, beanPath);
            if (applicationSpecificColumnConfig != null) {
                return applicationSpecificColumnConfig;
            }
            if (defaultFactory != null) {
                return defaultFactory.getColumnConfigForColumn(columnKey);
            }
            return null;
        }
    }

    /**
     * does a funky search over the given root class and beanpath to find the most suitable column config
     */
    public static ColumnConfig getApplicationSpecificColumnConfig(Class rootBeanClass, String beanPath) {
        String[] path = Generic.beanPathStringToArray(beanPath);
        if(FormulaUtils.isFormulaPath(path)) {
            return columnConfigs.getColumnConfig(FormulaResult.class.getName(), null);
        }

        for (int i = 0; i <= path.length; i++) {
            String[] pathToContainerBean = new String[i];
            System.arraycopy(path, 0, pathToContainerBean, 0, pathToContainerBean.length);

            String[] propertyPath = new String[path.length - i];
            System.arraycopy(path, i, propertyPath, 0, propertyPath.length);

            Class possibleConfigClass;
            if (i > 0) {
                Attribute attribute = PropertyModel.getInstance(rootBeanClass).getAttribute(pathToContainerBean);
                if (attribute == null) return null;

                possibleConfigClass = attribute.getType();
                if (propertyPath.length > 0) {
                    //make sure we find the subclass that actually has a subproperty for propertyPath[0]
                    possibleConfigClass = PropertyModel.getInstance(possibleConfigClass).findMatchingSubclass(propertyPath[0]);
                    if (possibleConfigClass == null) continue;
                }

                // Make sure its not the generated type
                possibleConfigClass = PropertyModel.getInstance(possibleConfigClass).getType();
            } else {
                possibleConfigClass = rootBeanClass;
            }
            if (propertyPath.length > 0) {
                Class testClass = possibleConfigClass.getSuperclass();
                while (testClass != null && Generic.getType(testClass).attributeExists(propertyPath[0])) {
                    possibleConfigClass = testClass;
                    testClass = possibleConfigClass.getSuperclass();
                }
            }

            // Normalise the ints, doubles, etc to their Object equivalents.
            possibleConfigClass = ClassUtilities.typeToClass(possibleConfigClass);

            ColumnConfig columnConfig = columnConfigs.getColumnConfig(possibleConfigClass.getName(), Generic.beanPathArrayToString(propertyPath, false));
            if (columnConfig != null) return columnConfig;
        }
        return null;
    }

    protected TableViewConfiguration getLastAppliedConfiguration() {
        return lastAppliedConfiguration;
    }

    protected void setLastAppliedConfiguration(TableViewConfiguration lastAppliedConfiguration) {
        this.lastAppliedConfiguration = lastAppliedConfiguration;
    }

    private static class ShowPopupAction extends AuditedAbstractAction {
        private JMenu[] menus;
        private JComponent invoker;

        public ShowPopupAction(JMenu[] menus, JComponent invoker) {
            super("Settings...");
            this.menus = menus;
            this.invoker = invoker;
        }

        public void auditedActionPerformed(ActionEvent e) {
            final JPopupMenu popup = new JPopupMenu();
            for (int i = 0; i < menus.length; i++) {
                JMenu menu = menus[i];
                popup.add(menu);
            }
            popup.add(new JMenuItem(new AuditedAbstractAction("Close This Menu") {
                public void auditedActionPerformed(ActionEvent e) {
                    popup.setVisible(false);
                }
            }));
            popup.show(invoker, invoker.getX(), invoker.getY() + invoker.getHeight());
        }
    }


    private class DateFormatUpdateListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateDateRendererEditorAndFilterFormatter();
            analyticsTable.repaint();
        }
    }

    private class FilterTableAction extends AuditedAbstractAction  {

        public FilterTableAction() {
            putValue(Action.NAME, "Filter Table");
            putValue(Action.SHORT_DESCRIPTION, "Filter Table Rows");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
            putValue(Action.ACTION_COMMAND_KEY, "FilterTableAction");
        }

        public void auditedActionPerformed(ActionEvent e) {
            analyticsTable.cancelCellEditing();
            showFilterPanel();
        }
    }

    protected class ViewConfigListModel implements ConfigListModel {
        private ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>();
        private String[] viewConfigurationIds;

        public ViewConfigListModel() {
            viewConfigurationIds = refreshViewConfigurationIds();
        }

        protected String[] refreshViewConfigurationIds() {
            return Utilities.appendAndSortArrays(userViewConfigurationMap.getNames(), fixedViewConfigurationMap.getNames());
        }

        @Override
        public void mapChanged() {
            viewConfigurationIds = refreshViewConfigurationIds();
            fireChanged();
        }

        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        public Object getElementAt(int index) {
            return viewConfigurationIds[index];
        }

        public int getSize() {
            return viewConfigurationIds.length;
        }

        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        private void fireChanged() {
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
            }
        }
    }

    private class ConfigNameCellRenderer extends DefaultListCellRenderer {
        private static final String ACTIVE_LIST_ELEMENT_ICON = "active_list_element.gif";
        private static final String INACTIVE_LIST_ELEMENT_ICON = "inactive_list_element.gif";

        private Font normalFont;
        private Font selectedFont;
        private Icon activeListElementIcon = ImageIconCache.getImageIcon(ACTIVE_LIST_ELEMENT_ICON);
        private Icon inActiveListElementIcon = ImageIconCache.getImageIcon(INACTIVE_LIST_ELEMENT_ICON);

        public ConfigNameCellRenderer() {
            super();
            normalFont = getFont().deriveFont(Font.PLAIN);
            selectedFont = getFont().deriveFont(Font.BOLD);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, isSelected);
            String selectedViewConfigurationId = (String) value;
            String viewConfigurationId = getViewConfigurationId();
            if (FixedViewConfigurationMap.isFixedView(selectedViewConfigurationId)) {
                label.setText(FixedViewConfigurationMap.getDisplayNameForViewId(selectedViewConfigurationId));
                label.setForeground(Color.blue);
            } else {
                label.setForeground(Color.black);
            }

            boolean sameView = selectedViewConfigurationId.equals(viewConfigurationId);
            Font font = sameView ? selectedFont : normalFont;
            if (selectedViewConfigurationId.equals(userViewConfigurationMap.getDefaultViewConfigurationId())) {
                font = font.deriveFont(Font.ITALIC | font.getStyle());
            }

            label.setFont(font);
            label.setIcon(sameView ? activeListElementIcon : inActiveListElementIcon);

            return label;
        }
    }


}
