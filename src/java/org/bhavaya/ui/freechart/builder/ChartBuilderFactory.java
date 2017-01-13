package org.bhavaya.ui.freechart.builder;

import org.bhavaya.ui.freechart.ChartViewConfiguration;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 25-Feb-2008
* Time: 14:21:16
*/
public interface ChartBuilderFactory {
    public ChartBuilder createChartBuilder(ChartViewConfiguration chartViewConfiguration);
}
