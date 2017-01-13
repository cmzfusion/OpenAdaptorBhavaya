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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.*;
import org.bhavaya.ui.freechart.GroupedXYDataSet;
import org.bhavaya.ui.freechart.TableModelDataSet;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Encoder;
import java.util.*;
import java.util.List;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.12 $
 */

public class ChartView extends AbstractView {
    private static final Log log = Log.getCategory(ChartView.class);

    private final static String VERTICAL_BAR = "Vertical bar";
    private final static String STACKED_VERTICAL_BAR = "Stacked vertical bar";
    private final static String HORIZONTAL_BAR = "Horizontal bar";
    private final static String STACKED_HORIZONTAL_BAR = "Stacked horizontal bar";
    private final static String PIE = "Pie";
    private final static String LINE = "Line";
    private final static String AREA = "Area";
    private final static String SCATTER_PLOT = "Scatter Plot";

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};
    private static final ImageIcon CHART_ICON = ImageIconCache.getImageIcon("chart_icon_16.gif");
    private static final ImageIcon CREATE_TABLE_ICON = ImageIconCache.getImageIcon("table_icon_add_32.gif");

    static {
        BeanUtilities.addPersistenceDelegate(ChartView.class,
                new BhavayaPersistenceDelegate(new String[]{"name", "tabTitle", "frameTitle", "beanCollection", "configuration"}) {
                    protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                        //don't call any of the setters and getters after construction
                        return;
                    }
                });
    }

    private ChartPanel chartPanel;

    private TableModelDataSet tableModelDataSet;
    private AnalyticsTableModel analyticsModel;
    private BeanCollectionTableModel beanTable = null;

    private JFreeChart chart;

    private String chartType;
    private boolean plot3D;
    private String domainName;
    private String rangeName;
    private ChartControlPanel chartControl;
    private Component viewComponent;

    private ChartViewConfiguration configuration = new ChartViewConfiguration();
    private TableViewConfiguration tableViewConfigurationToUseOnInit = null;
    private BeanCollection beanCollection;

    public ChartView(String name, String tabTitle, String frameTitle, BeanCollection beanCollection) {
        this(name, tabTitle, frameTitle, beanCollection, null);
    }

    public ChartView(String name, String tabTitle, String frameTitle, BeanCollection beanCollection, ChartViewConfiguration configuration) {
        super(name, tabTitle, frameTitle);
        this.chartControl = new ChartControlPanel();
        this.beanCollection = beanCollection;
        this.beanTable = new BeanCollectionTableModel(beanCollection.getType(), false);

        if (configuration != null) {
            setChartViewConfiguration(configuration);
        }
    }

    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        this.beanCollection = beanCollection;
        beanTable.setBeanCollection(beanCollection);
    }

    public void setChartViewConfiguration(ChartViewConfiguration configuration) {
        this.configuration = configuration;
        if (isInited()) {
            applyConfigToModels();
        }
    }

    public ChartViewConfiguration getConfiguration() {
        return configuration;
    }

    public void configureFromTableViewConfiguration(TableViewConfiguration tableViewConfiguration) {
        if (isInited()) {
            if (tableViewConfiguration != null) {
                analyticsModel.setConfiguration(tableViewConfiguration);
                if (!analyticsModel.isGrouped()) {
                    analyticsModel.setGrouped(true);
                }
                updateConfigFromTable();
                if (analyticsModel.isPivoted()) {
                    String pivotDataColumn = analyticsModel.getPivotColumn();
                    String pivotDataName = beanTable.getColumnName(beanTable.getColumnIndex(pivotDataColumn));
                    setRangeName(pivotDataName);
                }
                setDomainName(tableModelDataSet.getDomainName());
            }
        } else {
            this.tableViewConfigurationToUseOnInit = tableViewConfiguration;
        }
    }

    public Collection getRangeAxisLocators() {
        return configuration.getRangeAxisLocators();
    }

    public void addRangeAxisLocator(String rangeAxisLocator) {
        if (configuration.isPivoted()) {
            configuration.getRangeAxisLocators().clear(); //only allowed one data column when pivoting
        }
        configuration.getRangeAxisLocators().add(rangeAxisLocator);
        applyConfigToModels();
    }

    public void removeRangeAxisLocator(String rangeAxisLocator) {
        configuration.getRangeAxisLocators().remove(rangeAxisLocator);
        applyConfigToModels();
    }

    public Collection getDomainAxisLocators() {
        return configuration.getDomainAxisLocators();
    }

    public void addDomainAxisLocator(String domainAxisLocator) {
        configuration.getDomainAxisLocators().add(domainAxisLocator);
        applyConfigToModels();
    }

    public void removeDomainAxisLocator(String domainAxisLocator) {
        configuration.getDomainAxisLocators().remove(domainAxisLocator);
        applyConfigToModels();
    }


    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
        if (chart != null) {
            Plot plot = chart.getPlot();
            if (plot instanceof CategoryPlot) {
                CategoryPlot categoryPlot = (CategoryPlot) plot;
                categoryPlot.getDomainAxis().setLabel(domainName);
            }
        }
    }

    public String getRangeName() {
        return rangeName;
    }

    public void setRangeName(String rangeName) {
        this.rangeName = rangeName;
        if (chart != null) {
            Plot plot = chart.getPlot();
            if (plot instanceof CategoryPlot) {
                CategoryPlot categoryPlot = (CategoryPlot) plot;
                categoryPlot.getRangeAxis().setLabel(rangeName);
            }
        }
    }


    protected void updateConfigFromTable() {
        HashSet domainColumns = new HashSet();
        HashSet rangeColumnKeys = new HashSet();

        List columnIndexesOfDomain = tableModelDataSet.getColumnIndexesOfDomain();
        for (Iterator iterator = columnIndexesOfDomain.iterator(); iterator.hasNext();) {
            Integer index = (Integer) iterator.next();
            String beanPath = (String) analyticsModel.getColumnKey(index.intValue());
            if (beanPath != null) {
                domainColumns.add(beanPath);
            }
        }
        configuration.setDomainAxisLocators(domainColumns);

        if (analyticsModel.isPivoted()) {
            String seriesColumn = analyticsModel.getPivotColumn();
            configuration.setSeriesColumnLocator(seriesColumn);

            String rangeColumn = analyticsModel.getPivotDataColumn();
            ArrayList rangeAxisLocators = new ArrayList();
            rangeAxisLocators.add(rangeColumn);
            configuration.setRangeAxisLocators(rangeAxisLocators);
        } else {
            configuration.setSeriesColumnLocator(null);

            List seriesKeys = tableModelDataSet.getSeriesKeys();
            for (Iterator iterator = seriesKeys.iterator(); iterator.hasNext();) {
                TableModelDataSet.SeriesKey key = (TableModelDataSet.SeriesKey) iterator.next();
                Object columnKey = analyticsModel.getColumnKey(key.getColumnIndex());
                if (columnKey != null) {
                    rangeColumnKeys.add(columnKey);
                }
            }
            configuration.setRangeAxisLocators(rangeColumnKeys);
        }
    }

    /**
     * make changes to the bean table in order to apply the current config to the data models
     */
    private void applyConfigToModels() {
        beanTable.removeAllColumnLocators();
        HashSet allLocators = new HashSet();
        allLocators.addAll(configuration.getDomainAxisLocators());
        allLocators.addAll(configuration.getRangeAxisLocators());

        String seriesColumnLocator = configuration.getSeriesColumnLocator();
        boolean needsPivoting = configuration.isPivoted();
        if (needsPivoting) {
            allLocators.add(seriesColumnLocator);
        }
        beanTable.addColumnLocators(allLocators);

        if (needsPivoting) {
            String dataColumnLocator = (String) configuration.getRangeAxisLocators().iterator().next();
            analyticsModel.setPivotColumn(dataColumnLocator);
            analyticsModel.setPivotColumn(configuration.getSeriesColumnLocator());

            String pivotDataName = beanTable.getColumnName(beanTable.getColumnIndex(dataColumnLocator));
            setRangeName(pivotDataName);
        } else {
            setRangeName(tableModelDataSet.getRangeName());
        }
        analyticsModel.setPivoted(needsPivoting);

        updateAxesNames();
        reRangeGraph();
    }

    private void updateAxesNames() {
        setDomainName(tableModelDataSet.getDomainName());

        if (configuration.isPivoted()) {
            String dataColumnLocator = (String) configuration.getRangeAxisLocators().iterator().next();
            String pivotDataName = beanTable.getColumnName(beanTable.getColumnIndex(dataColumnLocator));
            setRangeName(pivotDataName);
        } else {
            setRangeName(tableModelDataSet.getRangeName());
        }

        setDomainName(tableModelDataSet.getDomainName());
    }

    public ToolBarGroup createToolBarGroup() {
        ToolBarGroup toolBarGroup = super.createToolBarGroup();
        toolBarGroup.addElement(new ToolBarGroup.ActionElement(new CreateTableViewAction()));
        return toolBarGroup;
    }

    protected MenuPanel[] createMenuPanels() {
        MenuPanel editMenuPanel = new MenuPanel("Edit", chartControl, SplitPanel.RIGHT, true);
        editMenuPanel.setSplitterOffset(250);

        return new MenuPanel[]{editMenuPanel};
    }

    protected void disposeImpl() {
        tableModelDataSet.setTableModel(null);
        super.disposeImpl();
    }

    protected void initImpl() {
        super.initImpl();

        analyticsModel = new AnalyticsTableModel(beanTable);
        analyticsModel.setGrouped(true);
        tableModelDataSet = new TableModelDataSet(analyticsModel);

        if (tableViewConfigurationToUseOnInit != null) {
            configureFromTableViewConfiguration(tableViewConfigurationToUseOnInit);
        } else if (configuration != null) {
            applyConfigToModels();
        }

        chartControl.init();

        setChartType(VERTICAL_BAR);
        setPlot3D(false);

        chartPanel.setMaximumDrawHeight(1200);
        chartPanel.setMaximumDrawWidth(1400);

        chartPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        viewComponent = createViewComponent(chartPanel);

//            pluggablePanel = new PluggablePanel(UIUtilities.createLoadingComponent("Waiting to load data"));

        ApplicationContext.getInstance().addGuiTask(new Task("ChartView loading data for " + getName()) {
            public void run() {
                try {
//                        pluggablePanel.setPluggedComponent(UIUtilities.createLoadingComponent("Loading data..."));
                    getBeanCollection().size(); //this ensures that a lazy beanCollection is inflated
                    if (!isDisposed()) { // CriteriaBeanCollectionLoadDecorator may have disposed the view based a user request
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    beanTable.setBeanCollection(getBeanCollection());
//                                        pluggablePanel.setPluggedComponent(BeanCollectionTableView.super.getComponent());
                                } catch (Exception e) {
                                    log.error(e);
//                                        handleError(e);
                                }
                            }
                        });
                    }
                } catch (Throwable e) {
                    log.error(e);
//                        handleError(e);
                }
            }
        });
    }

    private JFreeChart createVerticalBarChart() {
        JFreeChart chart;
        if (isPlot3D()) {
            chart = ChartFactory.createBarChart3D(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.VERTICAL, true, true, false);
        } else {
            chart = ChartFactory.createBarChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.VERTICAL, true, true, false);
        }

        NumberAxis rangeAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        rangeAxis.setAutoTickUnitSelection(true);

        CategoryAxis domainAxis = (CategoryAxis) chart.getCategoryPlot().getDomainAxis();
        domainAxis.setTickMarksVisible(true);
        return chart;
    }

    private JFreeChart createHorizontalBarChart() {
        JFreeChart chart;
        if (isPlot3D()) {
            chart = ChartFactory.createBarChart3D(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.HORIZONTAL, true, true, false);
        } else {
            chart = ChartFactory.createBarChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.HORIZONTAL, true, true, false);
        }
        NumberAxis rangeAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        rangeAxis.setAutoTickUnitSelection(true);
        rangeAxis.setVerticalTickLabels(true);
        rangeAxis.setTickMarksVisible(true);

        CategoryAxis domainAxis = (CategoryAxis) chart.getCategoryPlot().getDomainAxis();
        return chart;
    }

    private JFreeChart createAreaChart() {
        JFreeChart chart;
        chart = ChartFactory.createAreaChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.HORIZONTAL, true, true, false);
        return chart;
    }

    private JFreeChart createLineChart() {
        JFreeChart chart;
        chart = ChartFactory.createLineChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.HORIZONTAL, true, true, false);
        return chart;
    }

    private JFreeChart createScatterChart() {
        JFreeChart chart;
        GroupedXYDataSet dataset = new GroupedXYDataSet(beanCollection);
        HashSet allLocators = new HashSet();
        allLocators.addAll(configuration.getDomainAxisLocators());
        allLocators.addAll(configuration.getRangeAxisLocators());
        dataset.setColumnLocators(allLocators);

        chart = ChartFactory.createScatterPlot("", getDomainName(), getRangeName(), dataset, PlotOrientation.VERTICAL, true, true, false);

        Plot plot = chart.getPlot();
        if (plot instanceof XYPlot && dataset.isXAxisDateAxis()) {
            ValueAxis axis = ((XYPlot) plot).getDomainAxis();
            DateAxis dateAxis = new DateAxis(axis.getLabel());
            /*dateAxis.setAutoRange(true);
            dateAxis.setAutoTickUnitSelection(true);*/
            //dateAxis.setDateFormatOverride(new SimpleDateFormat("dd-MMM-yyyy"));
            ((XYPlot) plot).setDomainAxis(dateAxis);
        }
        return chart;
    }

    private JFreeChart createStackedHorizontalBarChart() {
        JFreeChart chart;
        chart = ChartFactory.createStackedBarChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.HORIZONTAL, true, true, false);

        NumberAxis rangeAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        rangeAxis.setAutoTickUnitSelection(true);
        rangeAxis.setVerticalTickLabels(true);
        rangeAxis.setTickMarksVisible(true);

        CategoryAxis domainAxis = (CategoryAxis) chart.getCategoryPlot().getDomainAxis();

        return chart;
    }

    private JFreeChart createStackedVerticalBarChart() {
        JFreeChart chart;
        if (isPlot3D()) {
            chart = ChartFactory.createStackedBarChart3D(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.VERTICAL, true, true, false);
        } else {
            chart = ChartFactory.createStackedBarChart(getName(), getDomainName(), getRangeName(), tableModelDataSet, PlotOrientation.VERTICAL, true, true, false);
        }
        NumberAxis rangeAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        rangeAxis.setAutoTickUnitSelection(true);

        CategoryAxis domainAxis = (CategoryAxis) chart.getCategoryPlot().getDomainAxis();
        domainAxis.setTickMarksVisible(true);
        return chart;
    }

    private JFreeChart createPieChart() {
        JFreeChart chart;
        if (isPlot3D()) {
            chart = ChartFactory.createPieChart3D(getName(), DatasetUtilities.createPieDatasetForRow(tableModelDataSet, 0), true, true, false);
            chart.getPlot().setForegroundAlpha(0.5f);
        } else {
            chart = ChartFactory.createPieChart(getName(), DatasetUtilities.createPieDatasetForRow(tableModelDataSet, 0), true, true, false);
        }
        return chart;
    }

    private void setChartType(String newSelectedType) {
        chartType = newSelectedType;
        if (newSelectedType.equals(VERTICAL_BAR)) {
            chart = createVerticalBarChart();
        } else if (newSelectedType.equals(STACKED_VERTICAL_BAR)) {
            chart = createStackedVerticalBarChart();
        } else if (newSelectedType.equals(HORIZONTAL_BAR)) {
            chart = createHorizontalBarChart();
        } else if (newSelectedType.equals(STACKED_HORIZONTAL_BAR)) {
            chart = createStackedHorizontalBarChart();
        } else if (newSelectedType.equals(PIE)) {
            chart = createPieChart();
        } else if (newSelectedType.equals(LINE)) {
            chart = createLineChart();
        } else if (newSelectedType.equals(AREA)) {
            chart = createAreaChart();
        } else if (newSelectedType.equals(SCATTER_PLOT)) {
            chart = createScatterChart();
        }

        chart.setBackgroundPaint(new JPanel().getBackground());

        if (chartPanel == null) {
            chartPanel = new ChartPanel(chart, false, true, true, false, true);
            chartPanel.addMouseListener(new PopupMouseListener());

        } else {
            chartPanel.setChart(chart);
        }

        try {
            Plot plot = chart.getPlot();
            ValueAxis axis = null;
            if (plot instanceof CategoryPlot) {
                axis = ((CategoryPlot) plot).getRangeAxis();
            } else if (plot instanceof XYPlot) {
                axis = ((XYPlot) plot).getRangeAxis();
            }
            if (axis != null) {
                axis.setAutoRange(true);
            }
        } catch (Exception e) {
            log.warn("Could not set chart to autoRange.", e);
        }

        chartControl.updateControls();
    }

    public String getChartType() {
        return chartType;
    }

    public Component getComponent() {
        init();
        return viewComponent;
    }

    private TableModelDataSet getTableModelDataSet() {
        return tableModelDataSet;
    }

    public ImageIcon getImageIcon() {
        return CHART_ICON;
    }

    public boolean isPlot3D() {
        return plot3D;
    }

    public void setPlot3D(boolean plot3D) {
        if (this.plot3D != plot3D) {
            this.plot3D = plot3D;
            setChartType(getChartType());
        }
        chartControl.updateControls();
    }

    public void reRangeGraph() {
        try {
            ValueAxis axis = chart.getCategoryPlot().getRangeAxis();
            axis.setAutoRange(true);
            axis.setAutoRange(false);
        } catch (Exception e) {
        }
    }

    protected class ReRangeGraphAction extends AuditedAbstractAction {
        public ReRangeGraphAction() {
            putValue(Action.SHORT_DESCRIPTION, "Re-range the graph axes");
        }

        public void auditedActionPerformed(ActionEvent e) {
            reRangeGraph();
        }
    }

    protected class CreateTableViewAction extends AuditedAbstractAction {
        public CreateTableViewAction() {
            putValue(Action.SMALL_ICON, CREATE_TABLE_ICON);
            putValue(Action.SHORT_DESCRIPTION, "View the underlying table of this chart");
        }

        public void auditedActionPerformed(ActionEvent e) {
            BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(getBeanCollection(), false);
            TableView tableView = new TableView(getName(), getTabTitle(), getFrameTitle(), beanCollectionTableModel, null);

            TableViewConfiguration chartDataConfig = (TableViewConfiguration) BeanUtilities.verySlowDeepCopy(analyticsModel.getConfiguration());
            tableView.getAnalyticsTable().setViewConfiguration(chartDataConfig);
            Workspace.getInstance().displayView(tableView);
        }
    }

    private static Icon blankIcon = ImageIconCache.getImageIcon("blank10by9.gif");
    private static Icon ascendingIcon = ImageIconCache.getImageIcon("sort_ascending.png");
    private static Icon descendingIcon = ImageIconCache.getImageIcon("sort_descending.png");

    private static MultiStateToggleButtonModel.ToggleButtonState[] sortButtonStates = {
        new MultiStateToggleButtonModel.ToggleButtonState(null, blankIcon, false),
        new MultiStateToggleButtonModel.ToggleButtonState(null, ascendingIcon, true),
        new MultiStateToggleButtonModel.ToggleButtonState(null, descendingIcon, true)
    };

    private class ChartControlPanel extends JPanel {

        private JComboBox selectionComboBox;
        private JCheckBox plot3DControl;
        private Set valid3DPlotTypes = new HashSet(Arrays.asList(new String[]{VERTICAL_BAR, STACKED_VERTICAL_BAR, HORIZONTAL_BAR, STACKED_HORIZONTAL_BAR, PIE}));
        private JTabbedPane tabbedPane;

        public void init() {
            setLayout(new BorderLayout());
            add(createMainPanel(), BorderLayout.NORTH);
            add(createAxisEditors(), BorderLayout.CENTER);
        }

        /**
         * Brendon, this is "functionality testing". NOT final UI code. Feel free to clean up ;)
         *
         * @return
         */
        private JPanel createMainPanel() {
            JPanel container = new JPanel();
            RowLayout rowLayout = new RowLayout(350, 15);
            container.setLayout(rowLayout);
            RowLayout.Row row;

            row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.TOP, false);
            container.add(row.addComponent(new JLabel("<html><i>Please note:<br>This control panel is under construction</i></html>")));
            rowLayout.addRow(row);

            row = new RowLayout.Row(3, RowLayout.LEFT, RowLayout.TOP, false);
            selectionComboBox = new JComboBox(new String[]{VERTICAL_BAR, STACKED_VERTICAL_BAR, HORIZONTAL_BAR, STACKED_HORIZONTAL_BAR, PIE, LINE, AREA, SCATTER_PLOT});
            selectionComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object newSelectedType = selectionComboBox.getSelectedItem();
                    if (newSelectedType != null) {
                        setChartType(newSelectedType.toString());
                    }
                }
            });
            container.add(row.addComponent(UIUtilities.createLabelledComponent("Chart type:", selectionComboBox)));

            plot3DControl = new JCheckBox("3D", isPlot3D());
            plot3DControl.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (plot3DControl.isSelected() != isPlot3D()) {
                        setPlot3D(plot3DControl.isSelected());
                    }
                }
            });
            container.add(row.addComponent(plot3DControl));
            rowLayout.addRow(row);


            CustomButtonGroup sortingAxisButtonGroup = new CustomButtonGroup();
            final JToggleButton categorySortButton = new JToggleButton("Category", blankIcon);
            categorySortButton.setModel(new MultiStateToggleButtonModel(categorySortButton, sortButtonStates));
            categorySortButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (categorySortButton.isSelected()) {
                        Integer index = (Integer) getTableModelDataSet().getColumnIndexesOfDomain().iterator().next();
                        int firstDomainColumn = index.intValue();
                        if (firstDomainColumn >= 0) {
                            Object columnKey = analyticsModel.getColumnKey(firstDomainColumn);
                            if (!analyticsModel.isSortingColumn(columnKey)) {
                                analyticsModel.cancelSorting();
                                analyticsModel.addSortingColumn(columnKey, false);
                            } else {
                                analyticsModel.toggleSortingDirection(columnKey);
                            }
                        }
                    } else {
                        analyticsModel.cancelSorting();
                    }
                }
            });
            final JToggleButton valuesSortButton = new JToggleButton("Values", blankIcon);
            valuesSortButton.setModel(new MultiStateToggleButtonModel(valuesSortButton, sortButtonStates));
            valuesSortButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getTableModelDataSet().getSeriesCount() > 0 && valuesSortButton.isSelected()) {
                        int firstRangeColumn = getTableModelDataSet().getSeriesKey(0).getColumnIndex();
                        if (firstRangeColumn >= 0) {
                            Object columnKey = analyticsModel.getColumnKey(firstRangeColumn);
                            if (!analyticsModel.isSortingColumn(columnKey)) {
                                analyticsModel.cancelSorting();
                                analyticsModel.addSortingColumn(columnKey, true);
                            } else {
                                analyticsModel.toggleSortingDirection(columnKey);
                            }
                        }
                    } else {
                        analyticsModel.cancelSorting();
                    }
                }
            });
            sortingAxisButtonGroup.add(categorySortButton);
            sortingAxisButtonGroup.add(valuesSortButton);

            Box sortingControl = Box.createHorizontalBox();
            sortingControl.add(categorySortButton);
            sortingControl.add(Box.createHorizontalStrut(5));
            sortingControl.add(valuesSortButton);

            row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.TOP, false);
            container.add(row.addComponent(UIUtilities.createLabelledComponent("Sort by", sortingControl)));
            rowLayout.addRow(row);
            return container;
        }

        private Component createAxisEditors() {
            tabbedPane = new JTabbedPane(JTabbedPane.TOP);
            tabbedPane.add("<html><b>Categories</b></html>", createDomainAxisEditor());
            tabbedPane.add("<html><b>Values</b></i>", createRangeAxisEditor());
            return tabbedPane;
        }

        private JPanel createDomainAxisEditor() {
            SearchableBeanPathSelector domainPathSelector = new SearchableBeanPathSelector(beanTable,
                    FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER,
                    FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER,
                    new BeanPathSelector.SelectionModel(true) {
                        public void locatorSelected(String columnLocator) {
                            if (getDomainAxisLocators().contains(columnLocator)) {
                                removeDomainAxisLocator(columnLocator);
                            } else {
                                addDomainAxisLocator(columnLocator);
                            }
                        }

                        public boolean isSelected(String columnLocator) {
                            return getDomainAxisLocators().contains(columnLocator);
                        }
                    });
            return domainPathSelector;
        }

        private JPanel createRangeAxisEditor() {
            SearchableBeanPathSelector domainPathSelector = new SearchableBeanPathSelector(beanTable,
                    FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER,
                    FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER,
                    new BeanPathSelector.SelectionModel(true) {
                        public void locatorSelected(String columnLocator) {
                            if (getRangeAxisLocators().contains(columnLocator)) {
                                removeRangeAxisLocator(columnLocator);
                            } else {
                                addRangeAxisLocator(columnLocator);
                            }
                        }

                        public boolean isSelected(String columnLocator) {
                            return getRangeAxisLocators().contains(columnLocator);
                        }
                    });
            return domainPathSelector;
        }

        private void updateControls() {
            selectionComboBox.setSelectedItem(getChartType());
            plot3DControl.setSelected(isPlot3D());
            plot3DControl.setEnabled(valid3DPlotTypes.contains(getChartType()));
        }
    }

    public Object[] getBeansForEntity(ChartEntity entity) {
        int series = 0;
        Object category = null;

        if (entity instanceof CategoryItemEntity) {
            CategoryItemEntity itemEntity = (CategoryItemEntity) entity;
            category = itemEntity.getCategory();
            series = itemEntity.getSeries();
        } else if (entity instanceof PieSectionEntity) {
            PieSectionEntity sectionEntity = (PieSectionEntity) entity;
            category = sectionEntity.getSectionKey();
            series = 0;
        }
        if (category != null) {
            return tableModelDataSet.getBeansForLocation(series, category);
        } else {
            return EMPTY_OBJECT_ARRAY;
        }
    }

    private class PopupMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent event) {
            if (event.isPopupTrigger()) {
                doPopup(event);
            }
        }

        public void mouseReleased(MouseEvent event) {
            if (event.isPopupTrigger()) {
                doPopup(event);
            }
        }

        private void doPopup(MouseEvent event) {
            final ChartEntity entity = chartPanel.getEntityForPoint(event.getX(), event.getY());


            if (entity != null) {
                final Object[] beans = getBeansForEntity(entity);
                if (beans.length > 0) {
                    JPopupMenu popup = new JPopupMenu();

                    JMenuItem drillDown = new JMenuItem("View data in " + entity.getToolTipText() + " (" + beans.length + " rows)");
                    drillDown.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String title = "Rows contributing to " + entity.getToolTipText() + " in view " + ChartView.this.getTabTitle();
                            BeanCollection beanCollection = new DefaultBeanCollection(getBeanCollection().getType());
                            beanCollection.addAll(Arrays.asList(beans));
                            BeanCollectionGroup.getDefaultInstance(beanCollection.getType()).viewBeanCollectionAsTable(title, title, title, beanCollection);

                        }
                    });
                    popup.add(drillDown);
                    popup.show(chartPanel, event.getX(), event.getY());
                }
            }
        }
    }


    public static class ChartViewConfiguration {
        //todo: store chart type, sorting prefs, etc in here
        private Collection domainAxisLocators; //Collection of beanPath strings
        private Collection rangeAxisLocators; //Collection of beanPath strings
        private String seriesColumnLocator; //column to use to determine which series each datapoint is in (i.e. pivot data column)

        public Collection getDomainAxisLocators() {
            return domainAxisLocators;
        }

        public void setDomainAxisLocators(Collection domainAxisLocators) {
            this.domainAxisLocators = domainAxisLocators;
        }

        public Collection getRangeAxisLocators() {
            return rangeAxisLocators;
        }

        public void setRangeAxisLocators(Collection rangeAxisLocators) {
            this.rangeAxisLocators = rangeAxisLocators;
        }

        public String getSeriesColumnLocator() {
            return seriesColumnLocator;
        }

        public void setSeriesColumnLocator(String seriesColumnLocator) {
            this.seriesColumnLocator = seriesColumnLocator;
        }

        public boolean isPivoted() {
            if (seriesColumnLocator != null && getRangeAxisLocators().size() == 1) {  //only allowed one pivot data column
                return true;
            }
            return false;
        }
    }
}
