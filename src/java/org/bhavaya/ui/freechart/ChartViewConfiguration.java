package org.bhavaya.ui.freechart;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 30-Apr-2008
 * Time: 12:36:37
 *
 * ChartViewConfiguration properties define how data is mapped from a bean collection to an AnalyticsTableModel,
 * the sorting/grouping/pivoting applied to the table model, and how the chart is generated from the table data.
 *
 * Each chart type requires a different set of configuration properties.
 * We could handle this by creating different subclasses of an abstract ChartViewConfiguration superclass,
 * each with the correct set of fields - but we would end up with a large number or subclasses.
 * Another possible solution would be to have one config class with fields to hold the superset of all properties.
 * Yet another alternative is to wrap a Properties instance or Map to store the properties, like this initial implementation
 *
 * I think using the Properties approach has some possible advantages, among these -
 * - inherit the equals and hashcode implementation
 * - inherit the shallow clone implementation, which is guaranteed safe since String properties are immutable,
 *   this can be used to take a snapshot of state which is useful to rollback changes etc.
 * - possible serialization to properties file for persistence (as well as compatibility with bean persistence)
 * - more resilient to changes in config parameters, will not crash if somebody has an outdated property in their config
 */
public interface ChartViewConfiguration {
    void setChartType(ChartType chartType);

    ChartType getChartType();

    String[] getGroupOrSeriesTableColumns();

    void setGroupOrSeriesTableColumns(String... columns);

    void clearGroupOrSeriesTableColumns();

    String getXValuesTableColumn();

    void setXValuesTableColumn(String columnName);

    String getYValuesTableColumn();

    void setYValuesTableColumn(String columnName);

    String getChartTitle();

    void setChartTitle(String chartTitle);

    String getXAxisLabel();

    void setXAxisLabel(String label);

    String getYAxisLabel();

    void setYAxisLabel(String value);

    boolean isShowLegend();

    void setShowLegend(boolean isShowLegend);

    boolean isShowTooltips();

    void setShowTooltips(boolean isShowTooptips);

    boolean isGenerateURLs();

    void setGenerateURLs(boolean isGenerateURLs);

    int getRepaintDelay();

    void setRepaintDelay(int delay);

    boolean isFilterNanValues();

    void setFilterNanValues(boolean filter);

    List<String> getAllRequiredColumns();

    public static enum ChartType {
        timeSeriesChart,
        XYScatterChart
    }
}
