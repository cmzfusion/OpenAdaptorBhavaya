package org.bhavaya.ui.freechart.builder;

import org.bhavaya.ui.freechart.ChartViewConfiguration;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Feb-2008
 * Time: 14:19:07
 */
public class DefaultChartBuilderFactory implements ChartBuilderFactory {

    public ChartBuilder createChartBuilder(ChartViewConfiguration chartViewConfiguration) {

        ChartBuilder chartBuilder = null;
        switch (chartViewConfiguration.getChartType()) {
            case timeSeriesChart:
                chartBuilder = new TimeSeriesChartBuilder();
                break;
        }
        return chartBuilder;
    }
}
