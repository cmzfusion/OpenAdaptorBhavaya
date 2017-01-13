package org.bhavaya.ui.freechart.builder;

import org.bhavaya.ui.freechart.ChartViewConfiguration;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 07-Mar-2008
 * Time: 10:17:20
 */
public class ScatterChartBuilder extends AbstractChartBuilder {

    public JFreeChart createJFreeChart(ChartViewConfiguration config, XYDataset XYdataset) {
        return ChartFactory.createScatterPlot(
            config.getChartTitle(),
            config.getXAxisLabel(),
            config.getYAxisLabel(),
            XYdataset,
            PlotOrientation.VERTICAL,
            config.isShowLegend(),
            config.isShowTooltips(),
            config.isGenerateURLs()
        );

    }

}
