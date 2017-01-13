package org.bhavaya.ui.freechart;

import java.util.List;

/**
 * Wraps a chart view configuration, subclasses can override methods in order to change aspects
 * of the configuration without affecting wrapped config
 */
public abstract class DecoratedChartViewConfiguration implements ChartViewConfiguration {

    private ChartViewConfiguration c;

    public DecoratedChartViewConfiguration(ChartViewConfiguration c) {
        this.c = c;
    }

    public List<String> getAllRequiredColumns() {
        return c.getAllRequiredColumns();
    }

    public void setChartType(ChartType chartType) {
        c.setChartType(chartType);
    }

    public ChartType getChartType() {
        return c.getChartType();
    }

    public String[] getGroupOrSeriesTableColumns() {
        return c.getGroupOrSeriesTableColumns();
    }

    public void setGroupOrSeriesTableColumns(String... columns) {
        c.setGroupOrSeriesTableColumns(columns);
    }

    public void clearGroupOrSeriesTableColumns() {
        c.clearGroupOrSeriesTableColumns();
    }

    public String getXValuesTableColumn() {
        return c.getXValuesTableColumn();
    }

    public void setXValuesTableColumn(String columnName) {
        c.setXValuesTableColumn(columnName);
    }

    public String getYValuesTableColumn() {
        return c.getYValuesTableColumn();
    }

    public void setYValuesTableColumn(String columnName) {
        c.setYValuesTableColumn(columnName);
    }

    public String getChartTitle() {
        return c.getChartTitle();
    }

    public void setChartTitle(String chartTitle) {
        c.setChartTitle(chartTitle);
    }

    public String getXAxisLabel() {
        return c.getXAxisLabel();
    }

    public void setXAxisLabel(String label) {
        c.setXAxisLabel(label);
    }

    public String getYAxisLabel() {
        return c.getYAxisLabel();
    }

    public void setYAxisLabel(String value) {
        c.setYAxisLabel(value);
    }

    public boolean isShowLegend() {
        return c.isShowLegend();
    }

    public void setShowLegend(boolean isShowLegend) {
        c.setShowLegend(isShowLegend);
    }

    public boolean isShowTooltips() {
        return c.isShowTooltips();
    }

    public void setShowTooltips(boolean isShowTooptips) {
        c.setShowTooltips(isShowTooptips);
    }

    public boolean isGenerateURLs() {
        return c.isGenerateURLs();
    }

    public void setGenerateURLs(boolean isGenerateURLs) {
        c.setGenerateURLs(isGenerateURLs);
    }

    public int getRepaintDelay() {
        return c.getRepaintDelay();
    }

    public void setRepaintDelay(int delay) {
        c.setRepaintDelay(delay);
    }

    public boolean isFilterNanValues() {
        return c.isFilterNanValues();
    }

    public void setFilterNanValues(boolean filter) {
        c.setFilterNanValues(filter);
    }
}
