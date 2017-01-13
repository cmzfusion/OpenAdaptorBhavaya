package org.bhavaya.ui.freechart.builder;

import org.bhavaya.ui.freechart.ChartViewConfiguration;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Feb-2008
 * Time: 14:28:11
 */
public class TimeSeriesChartBuilder extends AbstractChartBuilder {

    public JFreeChart createJFreeChart(ChartViewConfiguration config, XYDataset tableXYDataSet) {
        return ChartFactory.createTimeSeriesChart(
            config.getChartTitle(),
            config.getXAxisLabel(),
            config.getYAxisLabel(),
            tableXYDataSet,
            config.isShowLegend(),
            config.isShowTooltips(),
            config.isGenerateURLs()
        );
    }

}
