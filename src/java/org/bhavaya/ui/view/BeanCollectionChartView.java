package org.bhavaya.ui.view;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.freechart.TableXYDataSet;
import org.bhavaya.ui.freechart.builder.ChartBuilder;
import org.bhavaya.ui.freechart.builder.ChartBuilderFactory;
import org.bhavaya.ui.freechart.builder.DefaultChartBuilderFactory;
import org.bhavaya.ui.freechart.InvalidColumnException;
import org.bhavaya.ui.freechart.ChartViewConfiguration;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.DecimalRenderer;
import org.bhavaya.ui.view.composite.TabbedLayout;
import org.bhavaya.util.Log;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * User: ebbuttn
 * Date: 21-Feb-2008
 * Time: 15:53:26
 * <p/>
 * A chart view which uses a chart builder factory / chart builder to generate charts from a chart configuration
 * This will eventually be a composite view which can flip to display either the chart or table data
 * This class is work in progress - it is likely to change rapidly over the next few weeks
 */
public class BeanCollectionChartView extends AbstractCompositeView {

    private static final Log log = Log.getCategory(BeanCollectionChartView.class);
    private ChartViewConfiguration chartViewConfiguration;
    private BeanCollection beanCollection;
    private AnalyticsTableModel analyticsTableModel;
    private JFreeChart chart;

    public BeanCollectionChartView(String name, String tabTitle, String frameTitle, BeanCollection beanCollection, ChartViewConfiguration chartViewConfiguration) {
        super(name, tabTitle, frameTitle);
        this.beanCollection = beanCollection;
        this.chartViewConfiguration = chartViewConfiguration;
    }

    /**
     * Subclass should override to return a sensible icon
     */
    protected ImageIcon getImageIconForCompositeView() {
        return null;
    }

    public void initImpl() {
        try {
            ChartBuilderFactory chartBuilderFactory = createChartBuilderFactory();
            ChartBuilder chartBuilder = createChartBuilder(chartBuilderFactory);
            analyticsTableModel = buildTableModel(chartBuilder, chartViewConfiguration, beanCollection);
            chart = buildChart(chartBuilder, chartViewConfiguration, analyticsTableModel);
            java.util.List<JComponent> viewControls = getViewControls(chartBuilder, chartViewConfiguration, analyticsTableModel, chart);
            JPanel controlsPanel = createControlsPanel(viewControls);
            AbstractView chartView = new PlainChartView("Chart", "Chart", getFrameTitle(), controlsPanel, buildChartPanel(chartBuilder, chart));
            PlainTableView tableView = new PlainTableView("Data", "Data", getFrameTitle(), analyticsTableModel);
            addChildViews(chartView, tableView);
            setLayoutStrategy(new TabbedLayout(JTabbedPane.BOTTOM));
            postInitChart();
        } catch (Throwable e) {
            displayErrorPanel(e);
            log.error(e);
            e.printStackTrace();
        }
    }

    protected ChartBuilderFactory createChartBuilderFactory() {
        return new DefaultChartBuilderFactory();
    }

    protected ChartBuilder createChartBuilder(ChartBuilderFactory chartBuilderFactory) {
        return chartBuilderFactory.createChartBuilder(chartViewConfiguration);
    }

    protected JFreeChart buildChart(ChartBuilder chartBuilder, ChartViewConfiguration chartViewConfiguration, AnalyticsTableModel analyticsTableModel) throws InvalidColumnException {
        return chartBuilder.buildChart(chartViewConfiguration, analyticsTableModel);
    }

    protected AnalyticsTableModel buildTableModel(ChartBuilder chartBuilder, ChartViewConfiguration chartViewConfiguration, BeanCollection beanCollection) throws InvalidColumnException {
        return chartBuilder.buildTableModel(chartViewConfiguration, beanCollection);
    }

    protected List<JComponent> getViewControls(ChartBuilder chartBuilder, ChartViewConfiguration chartViewConfiguration, AnalyticsTableModel analyticsTableModel, JFreeChart freeChart) throws InvalidColumnException {
        return chartBuilder.getViewControls(chartViewConfiguration, analyticsTableModel, freeChart);
    }

    protected ChartPanel buildChartPanel(ChartBuilder chartBuilder, JFreeChart chart){
        return chartBuilder.buildChartPanel(chart);
    }

    /**
     * Subclasses may override to perform any required post-initialization
     * after the chart has been constructed
     */
    protected void postInitChart() {}

    private JPanel createControlsPanel(java.util.List<JComponent> viewControls ) throws InvalidColumnException {
        JPanel controlsPanel = new JPanel();
        for (JComponent component : viewControls) {
            controlsPanel.add(component);
        }
        return controlsPanel;
    }

    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    protected AnalyticsTableModel getAnalyticsTableModel() {
        return analyticsTableModel;
    }

    protected ChartViewConfiguration getChartViewConfiguration() {
        return chartViewConfiguration;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        //todo - changing the bean collection is not yet implemented for this view
    }

    public void setChartTitle(String chartTitle) {
        getChartViewConfiguration().setChartTitle(chartTitle);
        chart.setTitle(chartTitle);
    }

    public void setColourForSeries(String name, Color colour) {
        XYDataset dataset = chart.getXYPlot().getDataset();
        int series = dataset.indexOf(name);
        if(series >= 0) {
            chart.getXYPlot().getRenderer().setSeriesPaint(series, colour);
            if(dataset instanceof TableXYDataSet) {
                TableXYDataSet tableXYDataSet = (TableXYDataSet)dataset;
                if(tableXYDataSet.isInterpolationEnabled()) {
                    series = tableXYDataSet.indexOfInterpolatedSeries(name);
                    if(series >= 0) {
                        chart.getXYPlot().getRenderer().setSeriesPaint(series, colour);
                    }
                }
            }
        }
    }

    private static class PlainChartView extends AbstractView {
        private JPanel controlsPanel;
        private ChartPanel chartPanel;

        public PlainChartView(String name, String tabTitle, String frameTitle, JPanel controlsPanel, ChartPanel chartPanel) {
            super(name, tabTitle, frameTitle);

            this.controlsPanel = controlsPanel;
            this.chartPanel = chartPanel;
        }

        public ViewContext getViewContext() {
            return new DefaultViewContext(this);
        }

        public Component getComponent() {
            init();

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(controlsPanel, BorderLayout.NORTH);
            mainPanel.add(chartPanel, BorderLayout.CENTER);
            return mainPanel;
        }

        public BeanCollection getBeanCollection() {
            return null;
        }

        public void setBeanCollection(BeanCollection beanCollection) {

        }
    }


    private static class PlainTableView extends AbstractView {
        private AnalyticsTableModel analyticsTableModel;
        private AnalyticsTable analyticsTable;

        public PlainTableView(String name, String tabTitle, String frameTitle, AnalyticsTableModel analyticsTableModel) {
            super(name, tabTitle, frameTitle);
            this.analyticsTableModel = analyticsTableModel;
        }

        public ViewContext getViewContext() {
            return new DefaultViewContext(this);
        }

        public Component getComponent() {
            init();

            JPanel mainPanel = new JPanel(new BorderLayout());
            analyticsTable = new AnalyticsTable(analyticsTableModel, false);
            analyticsTable.setDefaultRenderer(Double.class, new DecimalRenderer(4));
            mainPanel.add(analyticsTable, BorderLayout.CENTER);
            return mainPanel;
        }

        public BeanCollection getBeanCollection() {
            return null;
        }

        public void setBeanCollection(BeanCollection beanCollection) {

        }
    }

}
