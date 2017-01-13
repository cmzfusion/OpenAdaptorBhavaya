package org.bhavaya.ui.table;

import com.od.filtertable.RowFilteringTableModel;
import com.od.filtertable.FilterFormatter;
import com.od.filtertable.TimedIndexTrimmer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.*;

import org.bhavaya.util.Log;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 10-Sep-2008
 * Time: 15:04:14
 *
 * This class extends RowFilteringTableModel, which has no bhavaya dependencies, in order to make it work within Bhavaya
 * An extra requirement is to support column exclusions, in the same manner as the sorted table model.
 * This allows us to exclude columns with rapidly changing data (e.g. prices) from the index maintained by the filter model.
 *
 * The mechanism for specifying column exclusions in the app config uses 'column key', which is a bhavaya-specific concept.
 * To make it work with the RowFilteringTableModel we first have to translate the key to a column name
 * Ssee ExclusionColumnCalculator for more details of the exlusion mechanism
 *
 * The model also supports adding columns to the list of exclusions automatically, if it is determined that the updates
 * for a given column are too frequent. If more than MAX_COL_UPDATE_PER_SECOND take place for a column over the period defined by
 * CHECK_AND_REPORT_PERIOD_MILLIS then that column will be automatically added to the exclusions.
 */
public class ChainableFilteredTableModel extends RowFilteringTableModel implements TabularBeanAssociation, ChainableTableModel, FilterFindTableModel {

    //contains the set of all column keys which have been dymaically excluded, so that we can make this visible in the mBean diagnostic pages
    //ideally they app should add config rules to excluded these upfront
    private static final Set<String> dynamicallyExcludedColumnKeys = Collections.synchronizedSet(new HashSet<String>());

    private static final TimedIndexTrimmer filteredModelIndexTrimmer = new TimedIndexTrimmer(300000);
    private static final int CHECK_AND_REPORT_PERIOD_MILLIS = 15000;
    private static final int MAX_COL_UPDATE_PER_SECOND = 5;
    private static final Log log = Log.getCategory(ChainableFilteredTableModel.class);

    private static ExclusionColumnCalculator exclusionColumnCalculator = ExclusionColumnCalculator.getInstance();
    private List<String> currentlyExcludedColumnNames = new ArrayList<String>();

    private long totalEventProcessingTime;
    private long lastWriteToLogTimestamp;
    private long totalEventsProcessed;
    private int[] columnUpdateCounts = new int[0];

    private KeyedColumnTableModel sourceModel;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private TableModelEvent causeOfLatestSourceModelChange;

    static {
        filteredModelIndexTrimmer.startIndexTrimming();
    }

    public ChainableFilteredTableModel() {
        super(new DefaultTableModel(), 1);
        filteredModelIndexTrimmer.addModel(this);
    }

    public int getColumnIndex(Object columnKey) {
        return sourceModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int index) {
        return sourceModel.getColumnKey(index);
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceModel;
    }

    public Object[] getBeansForLocation(int rowIndex, int columnIndex) {
        Object[] result = EMPTY_OBJECT_ARRAY;   //based on the implementation of SortedTableModel
        if ( sourceModel instanceof TabularBeanAssociation) {
            result = ((TabularBeanAssociation)sourceModel).getBeansForLocation(getRowInUnderlyingModel(rowIndex), columnIndex);
        }
        return result;
    }

    public boolean isSingleBean(int rowIndex, int columnIndex) {
        boolean result = false;   //based on the implementation of SortedTableModel
         if ( sourceModel instanceof TabularBeanAssociation) {
            result = ((TabularBeanAssociation)sourceModel).isSingleBean(getRowInUnderlyingModel(rowIndex), columnIndex);
        }
        return result;
    }

    public void setSourceModel(KeyedColumnTableModel sourceModel) {
        if ( sourceModel == null ) {
            this.sourceModel = null;
            setTableModel(new DefaultTableModel());
        } else {
            this.sourceModel = sourceModel;
            resetColumnUpdateCounts();
            initializeColumnExclusions();
            setTableModel(sourceModel);
        }
    }

    public static Set<String> getDynamicallyExcludedColumnKeys() {
        return dynamicallyExcludedColumnKeys;
    }

    //if source model is changing or structure / col change event occurs, start counts afresh
    private void resetColumnUpdateCounts() {
        this.columnUpdateCounts = new int[sourceModel.getColumnCount()];
    }

    /**
     * Override the listener creation to interpose our own table model listener
     *
     * This checks for structure changes in the source model, and allows us to check for columns to
     * exclude, before processing the event in the normal way and delegating to the superclass listener
     *
     * also logs the total time taken by the filter table event processing
     */
    protected TableModelListener createTableModelListener() {
        return new TableModelListener() {

            private TableModelListener eventParser = ChainableFilteredTableModel.super.createTableModelListener();

            public void tableChanged(TableModelEvent e) {
                long startTime = System.currentTimeMillis();
                causeOfLatestSourceModelChange = e;
                setupColExclusionsAndUpdateCountsIfColumnChangeEvent(e);
                updateColumnUpdateCounts(e);
                if ( e instanceof MultipleColumnChangeEvent) {
                    unrollAndProcess(eventParser, (MultipleColumnChangeEvent)e);
                } else if ( e instanceof CellsInColumnUpdatedEvent ) {
                    unrollAndProcess(eventParser, (CellsInColumnUpdatedEvent)e);
                } else {
                    eventParser.tableChanged(e);
                }
                logEventProcessingTime(startTime);
            }
        };
    }

    private void updateColumnUpdateCounts(TableModelEvent e) {
        int col = e.getColumn();
        if ( col != -1) {
            if ( col < columnUpdateCounts.length) {
                columnUpdateCounts[col]++;
            }
        }
    }

    private void unrollAndProcess(TableModelListener l, MultipleColumnChangeEvent e) {
        for ( CellsInColumnUpdatedEvent c : e.getColumnChanges()) {
            unrollAndProcess(l, c);
        }
    }

    private void unrollAndProcess(TableModelListener l, CellsInColumnUpdatedEvent e) {
        if ( e.getRowCount() == 1) {
            l.tableChanged(e);
        } else {
            int[] rows = e.getRows();
            for ( int row : rows ) {
                l.tableChanged(new TableModelEvent((TableModel)e.getSource(), row, row, e.getColumn(), TableModelEvent.UPDATE));
            }
        }
    }

    protected TableModelEvent createTableModelEvent(TableModel source, int startRow, int endRow, int col, int eventType) {
        if ( startRow != TableModelEvent.HEADER_ROW &&
             eventType == TableModelEvent.UPDATE &&
             col != TableModelEvent.ALL_COLUMNS &&
             causeOfLatestSourceModelChange instanceof CellsInColumnUpdatedEvent ) {
             int[] rows = getAffectedRows(startRow, endRow);
             return new CellsInColumnUpdatedEvent(source, rows, col, (CellsInColumnUpdatedEvent)causeOfLatestSourceModelChange);
        } else {
            return new TableModelEvent(source, startRow, endRow, col, eventType);
        }
    }

    private int[] getAffectedRows(int startRow, int endRow) {
        int[] rows = new int[(endRow - startRow) + 1];
        for ( int loop=0; loop < rows.length; loop++ ) {
            rows[loop] = startRow + loop;
        }
        return rows;
    }

    private void setupColExclusionsAndUpdateCountsIfColumnChangeEvent(TableModelEvent e) {
        if ( e.getFirstRow() == TableModelEvent.HEADER_ROW ) {
            initializeColumnExclusions();
            resetColumnUpdateCounts();
        }
    }

    private void logEventProcessingTime(long startTime) {
        long currentTime = System.currentTimeMillis();
        totalEventsProcessed++;
        totalEventProcessingTime += (currentTime - startTime);
        if ( currentTime - lastWriteToLogTimestamp > CHECK_AND_REPORT_PERIOD_MILLIS) {
            if (isFilteringTakingMoreThanPointOnePercentOfThreadTime()) {
                log.info("Filter table model " + this + " event processing time " + totalEventProcessingTime +
                        " millis, " + totalEventsProcessed + " table events processed in this " + CHECK_AND_REPORT_PERIOD_MILLIS + "ms period ");
                excludeVolatileColumns();
                resetColumnUpdateCounts();
            }
            lastWriteToLogTimestamp = currentTime;
            totalEventProcessingTime = 0;
            totalEventsProcessed = 0;
        }
    }

    private boolean isFilteringTakingMoreThanPointOnePercentOfThreadTime() {
        return totalEventProcessingTime > CHECK_AND_REPORT_PERIOD_MILLIS / 1000;
    }

    //count the updates for each column.
    //If any column has more than MAX_COL_UPDATE_PER_SECOND update per second, treat this as a volatile column and exclude it from the filtering
    private void excludeVolatileColumns() {
        List<String> volatileColNames = findVolatileColumns();
        if ( volatileColNames.size() > 0) {
            excludeColumns(volatileColNames);
            currentlyExcludedColumnNames.addAll(volatileColNames);
        }
    }

    //parse the update count array, looking for columns which have been updated too often and are not already excluded
    private List<String> findVolatileColumns() {
        List<String> colNames = new ArrayList<String>();
        int secondsInPeriod = CHECK_AND_REPORT_PERIOD_MILLIS / 1000;
        int colUpdatesPerSecond;

        //create a set just for performance here to avoid walking the list for every col
        HashSet<String> alreadyExcluded = new HashSet<String>(currentlyExcludedColumnNames);

        for ( int loop=0 ; loop < columnUpdateCounts.length; loop++) {
            colUpdatesPerSecond = (columnUpdateCounts[loop] / secondsInPeriod);
            log.info(getColumnName(loop)+", "+colUpdatesPerSecond);
            if (colUpdatesPerSecond > MAX_COL_UPDATE_PER_SECOND) {
                String colName = getColumnName(loop);
                if ( ! alreadyExcluded.contains(colName)) {
                    Object columnKey = sourceModel.getColumnKey(loop);
                    log.info("Column with name " + colName + " and path " + columnKey +
                            " had " + colUpdatesPerSecond + " updates per second during the last period." +
                            " This column will be excluded from filtering from now on." +
                            " Consider adding an exclusion rule to the config for this column");
                    alreadyExcluded.add(colName);
                    colNames.add(colName);
                    dynamicallyExcludedColumnKeys.add(columnKey.toString());
                }
            }
        }
        return colNames;
    }

    private void initializeColumnExclusions() {
        List<String> columnNamesToExclude = getColNamesToExclude();
        if ( ! columnNamesToExclude.equals(currentlyExcludedColumnNames)) {
            clearFormatters();
            excludeColumns(columnNamesToExclude);
            currentlyExcludedColumnNames = columnNamesToExclude;
        }
    }

    private void excludeColumns(List<String> columnNamesToExclude) {
        setFormatter(FilterFormatter.EXCLUDE_FROM_FILTER_INDEX, columnNamesToExclude.toArray(new String[columnNamesToExclude.size()]));
    }

    /**
     * Calculate the names of the columns to exclude, from the column keys
     */
    private List<String> getColNamesToExclude() {
        return exclusionColumnCalculator.getColumnNamesToExclude(sourceModel);
    }

}
