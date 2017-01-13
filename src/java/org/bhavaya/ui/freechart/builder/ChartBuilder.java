package org.bhavaya.ui.freechart.builder;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;
import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.freechart.InvalidColumnException;
import org.bhavaya.ui.freechart.ChartViewConfiguration;
import org.bhavaya.collection.BeanCollection;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Feb-2008
 * Time: 14:18:04
 *
 * Build a chart from a BeanCollection in two stages -
 * first a table model which performs any necessary grouping/sorting
 * then the JFreechart instance
 */
public interface ChartBuilder {

    /**
     * @return an AnalyticsTableModel containing the source data for the chart
     */
    AnalyticsTableModel buildTableModel( ChartViewConfiguration chartViewConfiguration, BeanCollection beanCollection) throws InvalidColumnException;

    /**
     * @return the JFreeChart instance to display
     */
    JFreeChart buildChart( ChartViewConfiguration chartViewConfiguration, KeyedColumnTableModel chartTableModel) throws InvalidColumnException;

    /**
     * @return a list of components which will be displayed above the chart, typically these will be filters or controls which change the way the chart is presented
     */
    public java.util.List<JComponent> getViewControls(ChartViewConfiguration chartViewConfiguration, AnalyticsTableModel chartTableModel, JFreeChart freeChart) throws InvalidColumnException;

    /**
     * @return a ChartPanel for the given chart
     */
    ChartPanel buildChartPanel(JFreeChart chart);
}
