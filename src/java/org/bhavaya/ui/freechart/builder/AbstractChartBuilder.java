package org.bhavaya.ui.freechart.builder;

import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.ui.table.FilteredTableModel;
import org.bhavaya.ui.freechart.*;
import org.bhavaya.collection.BeanCollection;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Mar-2008
 * Time: 11:55:40
 */
public abstract class AbstractChartBuilder implements ChartBuilder {

    public JFreeChart buildChart(ChartViewConfiguration config, KeyedColumnTableModel chartTableModel) throws InvalidColumnException {

        KeyedColumnTableModel tableModelToUse = getTableModelForChart(config, chartTableModel);
        XYDataset xyDataset = createXYDataset(config, tableModelToUse);
        JFreeChart freeChart = createJFreeChart(config, xyDataset);
        setupRenderer(config, xyDataset, freeChart);
        return freeChart;
    }

    public AnalyticsTableModel buildTableModel(ChartViewConfiguration chartViewConfiguration, BeanCollection beanCollection) throws InvalidColumnException {

        BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, false);
        beanCollectionTableModel.addColumnLocators(chartViewConfiguration.getAllRequiredColumns());
        return new AnalyticsTableModel(beanCollectionTableModel);
    }

    public java.util.List<JComponent> getViewControls(ChartViewConfiguration chartViewConfiguration, AnalyticsTableModel chartTableModel, JFreeChart freeChart) throws InvalidColumnException {
        return new ArrayList<JComponent>();
    }

    public ChartPanel buildChartPanel(JFreeChart chart) {
        return new ChartPanel(chart);
    }

    //subclasses may override
    protected KeyedColumnTableModel getTableModelForChart(ChartViewConfiguration config, KeyedColumnTableModel keyedTableModel) {
        //JFreeCharts sometimes seem not to work if there are NaNs or nulls in the data being plotted
        //The safest approach is to filter out all rows NaN and null values before plotting the chart
        KeyedColumnTableModel tableModelToUse = keyedTableModel;
        if ( config.isFilterNanValues() ) {
             tableModelToUse = createNanNullAndInfinityFilteredTableModel(
                     keyedTableModel,
                     config.getXValuesTableColumn(),
                     config.getYValuesTableColumn()
             );
        }
        return tableModelToUse;
    }

    //filtering may be integrated into AnalyticsTableModel in the future
    public FilteredTableModel createNanNullAndInfinityFilteredTableModel(KeyedColumnTableModel keyedTableModel, final String... valueColumns) {
        final FilteredTableModel filteredTableModel = new FilteredTableModel(keyedTableModel);
        for ( String col : valueColumns ) {
                filteredTableModel.addFilters(
                        new FilteredTableModel.ColumnValueFilter(col, Double.NaN, false),
                        new FilteredTableModel.ColumnValueFilter(col, Double.POSITIVE_INFINITY, false),
                        new FilteredTableModel.ColumnValueFilter(col, Double.NEGATIVE_INFINITY, false),
                        new FilteredTableModel.ColumnValueFilter(col, null, false)
                );
        }
        return filteredTableModel;
    }

    //subclasses may override, leave in the unused config parameter
    protected void setupRenderer(ChartViewConfiguration config, XYDataset tableXYDataSet, JFreeChart chart) {
        addColorTrackingRenderer(tableXYDataSet, chart);
    }

    /**
     * Add a color tracking renderer to decorate the chart's existing renderer
     */
    protected void addColorTrackingRenderer(XYDataset XYDataset, JFreeChart chart) {
        XYItemRenderer r = chart.getXYPlot().getRenderer();
        if ( r instanceof AbstractXYItemRenderer) {
            ColorTrackingXYItemRenderer colorTrackingRenderer = new ColorTrackingXYItemRenderer((AbstractXYItemRenderer)r, XYDataset);
            chart.getXYPlot().setRenderer(colorTrackingRenderer);
        }
    }


    //subclasses may override
    protected XYDataset createXYDataset(ChartViewConfiguration config, KeyedColumnTableModel tableModelToUse) throws InvalidColumnException {
        return new TableXYDataSet(
                tableModelToUse,
                config.getGroupOrSeriesTableColumns(),
                config.getXValuesTableColumn(),
                config.getYValuesTableColumn(),
                config.getRepaintDelay()
        );
    }

    public abstract JFreeChart createJFreeChart(ChartViewConfiguration config, XYDataset xyDataset);

    
    public static void showTestingTable(TableModel tableModel) {
        //TODO - take out this testing table
        JTable table = new JTable(tableModel);
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JScrollPane(table));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
