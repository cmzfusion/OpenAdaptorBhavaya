package org.bhavaya.ui.freechart;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Feb-2008
 * Time: 11:52:50
 */
public class DefaultChartViewConfiguration implements ChartViewConfiguration {

    //don't provide access to the Properties map directly -
    //in case we want to change the implementation in the future to store the config as fields
    private Properties configurationProperties = new Properties();

    private static final String CHART_TYPE_KEY = "ChartType";
    private static final String GROUP_OR_SERIES_COLUMN_NAMES_KEY = "GroupOrSeriesTableColumns";
    private static final String X_VALUES_COLUMN_KEY = "XValuesTableColumn";
    private static final String Y_VALUES_COLUMN_KEY = "YValuesTableColumn";
    private static final String CHART_TITLE_KEY = "ChartTitle";
    private static final String X_AXIS_LABEL_KEY = "TimeAxisLabel";
    private static final String Y_AXIS_LABEL_KEY = "ValueAxisLabel";
    private static final String SHOW_LEGEND_KEY = "ShowLegend";
    private static final String SHOW_TOOLTIPS_KEY = "ShowTooltips";
    private static final String GENERATE_URLS_KEY = "GenerateURLs";
    private static final String REPAINT_DELAY_KEY = "RepaintDelay";
    private static final String FILTER_NAN_VALUES_KEY = "FilterNaNValues";

    public void setChartType(ChartType chartType) {
        configurationProperties.setProperty(CHART_TYPE_KEY, chartType.name());
    }

    public ChartType getChartType() {
        return ChartType.valueOf(configurationProperties.getProperty(CHART_TYPE_KEY));
    }

    public String[] getGroupOrSeriesTableColumns() {
        return getAsStringArray(GROUP_OR_SERIES_COLUMN_NAMES_KEY);
    }

    public void setGroupOrSeriesTableColumns(String... columns) {
        putAsStringArray(GROUP_OR_SERIES_COLUMN_NAMES_KEY, columns);
    }

    public void clearGroupOrSeriesTableColumns() {
        configurationProperties.remove(GROUP_OR_SERIES_COLUMN_NAMES_KEY);
    }

    public String getXValuesTableColumn() {
        return configurationProperties.getProperty(X_VALUES_COLUMN_KEY);
    }

    public void setXValuesTableColumn(String columnName) {
        configurationProperties.setProperty(X_VALUES_COLUMN_KEY, columnName);
    }

    public String getYValuesTableColumn() {
        return configurationProperties.getProperty(Y_VALUES_COLUMN_KEY);
    }

    public void setYValuesTableColumn(String columnName) {
        configurationProperties.setProperty(Y_VALUES_COLUMN_KEY, columnName);
    }

    public String getChartTitle() {
        return configurationProperties.getProperty(CHART_TITLE_KEY);
    }

    public void setChartTitle(String chartTitle) {
        configurationProperties.setProperty(CHART_TITLE_KEY, chartTitle);
    }

    public String getXAxisLabel() {
        return configurationProperties.getProperty(X_AXIS_LABEL_KEY);
    }

    public void setXAxisLabel(String label) {
        configurationProperties.setProperty(X_AXIS_LABEL_KEY, label);
    }

    public String getYAxisLabel() {
        return configurationProperties.getProperty(Y_AXIS_LABEL_KEY);
    }

    public void setYAxisLabel(String value) {
        configurationProperties.setProperty(Y_AXIS_LABEL_KEY, value);
    }

    public boolean isShowLegend() {
        return Boolean.valueOf(configurationProperties.getProperty(SHOW_LEGEND_KEY, "false"));
    }

    public void setShowLegend(boolean isShowLegend) {
        configurationProperties.setProperty(SHOW_LEGEND_KEY, String.valueOf(isShowLegend));
    }

    public boolean isShowTooltips() {
        return Boolean.valueOf(configurationProperties.getProperty(SHOW_TOOLTIPS_KEY, "false"));
    }

    public void setShowTooltips(boolean isShowTooptips) {
        configurationProperties.setProperty(SHOW_TOOLTIPS_KEY, String.valueOf(isShowTooptips));
    }

    public boolean isGenerateURLs() {
        return Boolean.valueOf(configurationProperties.getProperty(GENERATE_URLS_KEY, "false"));
    }

    public void setGenerateURLs(boolean isGenerateURLs) {
        configurationProperties.setProperty(GENERATE_URLS_KEY, String.valueOf(isGenerateURLs));
    }

    public int getRepaintDelay() {
        return Integer.valueOf(configurationProperties.getProperty(REPAINT_DELAY_KEY, "2000"));
    }

    public void setRepaintDelay(int delay) {
        configurationProperties.setProperty(REPAINT_DELAY_KEY, String.valueOf(delay));
    }

    public boolean isFilterNanValues() {
        return Boolean.valueOf(configurationProperties.getProperty(FILTER_NAN_VALUES_KEY, "false"));
    }
    
    public void setFilterNanValues(boolean filter) {
        configurationProperties.setProperty(FILTER_NAN_VALUES_KEY, String.valueOf(filter));
    }

    public List<String> getAllRequiredColumns() {
        Set<String> columns = new TreeSet<String>();
        columns.addAll(Arrays.asList(getGroupOrSeriesTableColumns()));
        addIfNotNull(columns, getXValuesTableColumn());
        addIfNotNull(columns, getYValuesTableColumn());
        return Arrays.asList(columns.toArray(new String[columns.size()]));
    }

    private void addIfNotNull(Set<String> columns, String xValuesColumn) {
        if ( xValuesColumn != null ) {
            columns.add(xValuesColumn);
        }
    }

    /**
     * @return a comma separated string property as a String[]
     */
    private String[] getAsStringArray(String property) {
        String columns = configurationProperties.getProperty(property);
        return (columns == null || columns.length() == 0 ) ? new String[0] : columns.split(",");
    }

     /**
     * Store a String[] as a comma separated string property
     * Does not support the case where the String values to be stored already contain commas
     */
    private void putAsStringArray(String property, String[] values) {
        StringBuilder sb = new StringBuilder();
        for ( String col : values ) {
            if ( sb.length() > 0 ) {
                sb.append(",");
            }
            sb.append(col);
        }
        configurationProperties.setProperty(property, sb.toString());
    }

    public int hashcode() {
        return configurationProperties.hashCode();
    }

    public boolean equals(Object o ) {
        boolean result = false;
        if ( o instanceof ChartViewConfiguration) {
            result = ((DefaultChartViewConfiguration)o).configurationProperties.equals(configurationProperties);
        }
        return result;
    }


}
