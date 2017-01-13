package org.bhavaya.ui.freechart;

import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.util.Log;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.DomainOrder;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.statistics.Regression;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Feb-2008
 * Time: 11:14:00
 * <pre>
 * This class adapts a KeyedColumnTableModel to the XYDataSet interface
 *
 * Unlike GroupedXYDataSet, this class contains no logic to try to guess the
 * source columns for X and Y values etc. Instead the following source columns must be set explicitly:
 *
 * - The column(s) from the KeyedColumnTableModel which are taken in combination to generate a group key
 * - The numeric or date column used to generate the X value
 * - The numeric column used to generate the Y value
 *
 * When the source data changes, the TableXYDataSet is recalculated thus:
 * - for each row:
 * -   identify the 'group key' from the values in the group key column(s)
 * -   find (or create) the List of series items for that group key
 * -   append a new SeriesItem to the list, taking the values from the x and y columns
 * </pre>
 */
public class TableXYDataSet extends TableDrivenDataSet implements XYDataset {

    private static final Log log = Log.getCategory(TableXYDataSet.class);

    private KeyedColumnTableModel keyedColumnTableModel;
    private String[] groupKeyColumns;
    private String xValueColumn;
    private String yValueColumn;
    private int[] groupKeyColIndexes;
    private int xValColumnIndex;
    private int yValColumnIndex;

    private List<Series> seriesItemsPerGroup = new ArrayList<Series>();
    private List<String> groupKeys = new ArrayList<String>();

    //a fast way of finding the index for a given group key without iterating the list
    private Map<String, Integer> groupKeyToIndex = new HashMap<String,Integer>();

    //Set to true if the an interpolated series should be added for each series
    private boolean addInterpolationSeries = false;

    private static final String DEFAULT_SERIES_NAME = "Default";
    private static final String INTERPOLATED_POSTFIX = " (interpolated)";

    public TableXYDataSet(KeyedColumnTableModel keyedColumnTableModel, String[] groupKeyColumns, String xValueColumn, String yValueColumn, int repaintDelay) throws InvalidColumnException {
        this(keyedColumnTableModel, groupKeyColumns, xValueColumn, yValueColumn, repaintDelay, false);
    }

    public TableXYDataSet(KeyedColumnTableModel keyedColumnTableModel, String[] groupKeyColumns, String xValueColumn, String yValueColumn, int repaintDelay, boolean addInterpolationSeries) throws InvalidColumnException {
        super(keyedColumnTableModel, repaintDelay);
        this.keyedColumnTableModel = keyedColumnTableModel;
        this.groupKeyColumns = groupKeyColumns;
        this.xValueColumn = xValueColumn;
        this.yValueColumn = yValueColumn;
        this.addInterpolationSeries = addInterpolationSeries;

        //columns which if changes mean we need to update the series x/y values
        setValueColumns(xValueColumn, yValueColumn);

        //columns which if changed mean we need to recalc the whole dataset
        setCategoryColumns(groupKeyColumns);
        initChart();
    }

    private void initChart() {
        clearDataSetState();
        try {
            findColumnIndices();
            buildSeries();
            sortSeriesByXValues();
        } catch (InvalidColumnException e) {
            log.error(e);
        }
    }

    private void sortSeriesByXValues() {
        for ( Series series : seriesItemsPerGroup ) {
            series.sort();
        }
    }

    private void clearDataSetState() {
        seriesItemsPerGroup.clear();
        groupKeys.clear();
        groupKeyToIndex.clear();
    }

    protected void buildSeries() {
        StringBuilder stringBuilder = new StringBuilder();
        for ( int row=0; row < keyedColumnTableModel.getRowCount(); row ++ ) {
            stringBuilder.setLength(0);
            String groupKey = calculateGroupNameForRow(stringBuilder, row);
            addSeriesItemForGroup(groupKey, row);
        }

        if(addInterpolationSeries) {
            int size = getSeriesCount();
            for(int i=0; i<size; i++) {
                String groupKey = getSeriesKey(i) + INTERPOLATED_POSTFIX;
                addSeries(new InterpolatedSeries(getSeries(i)), groupKey);
            }
        }
    }

    public boolean isInterpolationEnabled() {
        return addInterpolationSeries;
    }

    public boolean isInterpolationSeries(int series) {
        return getSeries(series) instanceof InterpolatedSeries;
    }

    private void addSeriesItemForGroup(String groupKey, int row) {
        List<TableSeriesItem> seriesForGroup = getOrCreateListOfItemsForSeries(groupKey);
        seriesForGroup.add(new TableSeriesItem(row));
    }

    private String calculateGroupNameForRow(StringBuilder stringBuilder, int row) {
        Object o;
        for ( int seriesCol : groupKeyColIndexes) {
            o = keyedColumnTableModel.getValueAt(row, seriesCol );
            if ( o != null && o != CachedObjectGraph.DATA_NOT_READY) {
                stringBuilder.append(o.toString());
            }
        }

        //n.b. currently if the result of appending group key columns values equals "",
        //or there are no group key columns the row will be assigned to series DEFAULT_SERIES_NAME
        if (stringBuilder.length() == 0 ) {
            stringBuilder.append(DEFAULT_SERIES_NAME);
        }

        return stringBuilder.toString();
    }

    private List<TableSeriesItem> getOrCreateListOfItemsForSeries(String groupName) {
        Series<TableSeriesItem> series;
        Integer groupIndex = groupKeyToIndex.get(groupName);
        if ( groupIndex == null ) {
            series = new BasicSeries();
            addSeries(series, groupName);
        } else {
            series = seriesItemsPerGroup.get(groupIndex);
        }
        return series.getList();
    }

    protected Series getSeries(int index) {
        return seriesItemsPerGroup.get(index);
    }

    protected void addSeries(Series series, String groupKey) {
        seriesItemsPerGroup.add(series);
        groupKeyToIndex.put(groupKey, groupKeyToIndex.size());
        groupKeys.add(groupKey);
    }

    private void findColumnIndices() throws InvalidColumnException {
        groupKeyColIndexes = new int[groupKeyColumns.length];
        for ( int loop=0; loop < groupKeyColumns.length; loop ++ ) {
            groupKeyColIndexes[loop] = findColumnIndex(groupKeyColumns[loop]);
        }
        xValColumnIndex = findColumnIndex(xValueColumn);
        yValColumnIndex = findColumnIndex(yValueColumn);
    }

    private int findColumnIndex(String columnName) throws InvalidColumnException {
        int index = keyedColumnTableModel.getColumnIndex(columnName);
        if ( index == -1 ) {
            throw new InvalidColumnException(columnName);
        }
        return index;
    }

    public void columnsChanged(DelayedColumnChangeListener.ColumnChangeEvent columnChangeEvent) {
        if ( columnChangeEvent.isCategoriesChange() ) {
            initChart();
        } else if ( columnChangeEvent.isValuesChange() ){
            refreshSeriesItemValues(); //change affects values columns only
        }
        fireDatasetChanged();
    }

    //refresh the values of each series item with the data from the table model
    private void refreshSeriesItemValues() {
        for ( Series series : seriesItemsPerGroup) {
            series.refresh();
        }
    }

    //////////////////////////////////////////
    //XYDataSet methods

    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;  //will need to take from chartviewconfig later
    }

    public int getItemCount(int series) {
        return seriesItemsPerGroup.get(series).getList().size();
    }

    public Number getX(int series, int item) {
        return seriesItemsPerGroup.get(series).get(item).getXValue();
    }

    public double getXValue(int series, int item) {
        return seriesItemsPerGroup.get(series).get(item).getXValue().doubleValue();
    }

    public Number getY(int series, int item) {
        return seriesItemsPerGroup.get(series).get(item).getYValue();
    }

    public double getYValue(int series, int item) {
        return seriesItemsPerGroup.get(series).get(item).getYValue().doubleValue();
    }


    //////////////////////////////////////////
    //SeriesDataSet methods

    public int getSeriesCount() {
        return seriesItemsPerGroup.size();
    }

    public String getSeriesName(int i) {
        return getSeriesKey(i).toString();
    }

    public Comparable getSeriesKey(int series) {
        return groupKeys.get(series);
    }

    public int indexOf(Comparable seriesKey) {
        Integer i = groupKeyToIndex.get(seriesKey);
        return i == null ? -1 : i;
    }

    public int indexOfInterpolatedSeries(String name) {
        return indexOf(name+INTERPOLATED_POSTFIX);
    }

    /**
     * @return the row index of the table which is the source for the data for the item 
     */
    public int getSourceRow(int series, int item) {
        return seriesItemsPerGroup.get(series).get(item).getSourceRow();
    }

    protected abstract class Series<T extends SeriesItem> {
        private List<T> list;

        protected Series() {
            this.list = new ArrayList<T>();
        }

        public List<T> getList() {
            return list;
        }

        public T get(int item) {
            return list.get(item);
        }

        public void sort() {
            Collections.sort(list);
        }

        public abstract void refresh();
    }

    protected class BasicSeries extends Series<TableSeriesItem> {

        public void refresh() {
            for ( TableSeriesItem item : getList()) {
                item.refreshValuesFromTableModel();
            }
        }
    }

    private class InterpolatedSeries extends Series<SeriesItem> {
        private Series<SeriesItem> sourceSeries;

        protected InterpolatedSeries(Series<SeriesItem> sourceSeries) {
            super();
            this.sourceSeries = sourceSeries;
            refresh();
        }

        public void refresh() {
            //Create a 2d double array of the source series
            List<SeriesItem> sourceList = sourceSeries.getList();
            double[][] d = new double[sourceList.size()][2];
            for(int i=0; i<d.length; i++) {
                SeriesItem item = sourceList.get(i);
                d[i][0] = item.getXValue().doubleValue();
                d[i][1] = item.getYValue().doubleValue();
            }

            //then create a 2d function from it
            double[] result= Regression.getOLSRegression(d);
            LineFunction2D function = new LineFunction2D(result[0], result[1]);

            //Now create a new series
            for(SeriesItem sourceSeriesItem : sourceList) {
                Number xValue = sourceSeriesItem.getXValue();
                Number yValue = new Double(function.getValue(xValue.doubleValue()));
                SeriesItem targetSeriesItem = new SeriesItem(sourceSeriesItem.getSourceRow(), xValue, yValue);
                getList().add(targetSeriesItem);
            }
        }
    }

    //holds the x/y data for a single item in a series
    protected class SeriesItem implements Comparable<SeriesItem>{
        private Number xValue;
        private Number yValue;
        private int sourceRow;

        SeriesItem(int sourceRow) {
            this.sourceRow = sourceRow;
        }

        SeriesItem(int sourceRow, Number xValue, Number yValue) {
            this.sourceRow = sourceRow;
            setXValue(xValue);
            setYValue(yValue);
        }

        public Number getXValue() {
            return xValue;
        }

        public Number getYValue() {
            return yValue;
        }

        public void setXValue(Number xValue) {
            this.xValue = xValue;
        }

        public void setYValue(Number yValue) {
            this.yValue = yValue;
        }

        public int getSourceRow() {
            return sourceRow;
        }

        public int hashCode() {
            return xValue.hashCode() ^ yValue.hashCode();
        }

        public boolean equals(Object o) {
            boolean result = false;
            if ( o.getClass() == getClass() ) {
                SeriesItem s = (SeriesItem)o;
                result = s.getXValue().equals(xValue) && s.getYValue().equals(yValue);
            }
            return result;
        }

        public int compareTo(SeriesItem i) {
            return ((Double)xValue.doubleValue()).compareTo(i.getXValue().doubleValue());
        }
    }

    //Version of SeriesItem that is a cached copy of the values in the underlying table model
    //this is because the chart update is delayed for performance reasons, so the data displayed by the chart is behind the
    //data in the table model. In the interim periods beteen chart updates we need to keep the chart data consistent
    protected class TableSeriesItem extends SeriesItem {

        private TableSeriesItem(int sourceRow) {
            super(sourceRow);
            refreshValuesFromTableModel();
        }

        protected void refreshValuesFromTableModel() {
            setXValue(getNumberForObject(keyedColumnTableModel.getValueAt(getSourceRow(), xValColumnIndex)));
            setYValue(getNumberForObject(keyedColumnTableModel.getValueAt(getSourceRow(), yValColumnIndex)));
        }

        //JFreeChart renderers should handle NaN values (by not plotting the series item).
        //This saves us recalculating the entire series to remove items which have temporary NaNs
        private Number getNumberForObject(Object o) {
            if (o instanceof Number) {
                return (Number) o;
            } else if (o instanceof java.util.Date) {
                java.util.Date date = (java.util.Date) o;
                return date.getTime();
            }
            return Double.NaN;
        }
    }
}
