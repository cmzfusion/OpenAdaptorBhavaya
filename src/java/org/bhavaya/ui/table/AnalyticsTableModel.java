/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui.table;

import org.bhavaya.ui.TableViewConfiguration;
import org.bhavaya.ui.series.DateSeriesNew;
import org.bhavaya.ui.series.Series;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;

import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.event.MouseListener;
import java.util.*;
import java.text.Format;

import com.od.filtertable.FilterFormatter;
import com.od.filtertable.TimedIndexTrimmer;
import com.od.filtertable.TableCell;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.19 $
 */
public class AnalyticsTableModel implements FilterFindTableModel, TabularBeanAssociation, CustomColumnNameModel {
    private static final Log log = Log.getCategory(AnalyticsTableModel.class);

    private static final int SPLITTER = 0;
    private static final int TRANSFORMER = 1;
    private static final int PIVOTER = 2;
    private static final int GROUPER = 3;
    private static final int ROWFILTER = 4;
    private static final int SORTER = 5;
    private static final int MAX_MODELS = SORTER + 1;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private ChainableTableModel[] transformationChain = new ChainableTableModel[MAX_MODELS];

    private KeyedColumnTableModel sourceTableModel;
    private DateSeriesTableModel tableDateSeriesSpliter;
    private TransformTableModel tableCellValueTransformer;
    private PivotTableModel tablePivoter;
    private GroupedTableModel tableGrouper;
    private ChainableFilteredTableModel tableRowFilter;
    private SortedTableModel tableSorter;

    public AnalyticsTableModel() {
        tableDateSeriesSpliter = new DateSeriesTableModel();
        tableCellValueTransformer = new TransformTableModel();
        tablePivoter = new PivotTableModel();
        tableGrouper = new GroupedTableModel();
        tableRowFilter = new ChainableFilteredTableModel();
        tableSorter = new SortedTableModel();
        initTransformChain();
    }

    public AnalyticsTableModel(KeyedColumnTableModel tableModel) {
        this();
        setSourceModel(tableModel);
    }

    private void initTransformChain() {
        for (int transformId = GROUPER; transformId >= 0; transformId--) {
            removeTransform(transformId);
        }
        if (transformationChain[SORTER] == null) {
            insertTransform(SORTER);
        }
    }

    /**
     * this is the top table model in the chain. Yes, not complete api, you'd expect a "setDelegateTableModel"
     */
    private FilterFindTableModel getDelegateTableModel() {
        return getTableSorter();
    }

    public void setSourceModel(KeyedColumnTableModel sourceTableModel) {
        ChainableTableModel currentSourceListener = getFirstDataTransform();
        currentSourceListener.setSourceModel(sourceTableModel);
        this.sourceTableModel = sourceTableModel;
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceTableModel;
    }

    private void insertTransform(int transformId) {
        ChainableTableModel listenerOfTransform = getListenerOfTransform(transformId);
        KeyedColumnTableModel inputModel = getInputModelOfTransform(transformId);
        ChainableTableModel transform = getTransform(transformId);
        transformationChain[transformId] = transform;
        transform.setSourceModel(inputModel);
        if (listenerOfTransform != null) listenerOfTransform.setSourceModel(transform);
    }

    private void removeTransform(int transformId) {
        ChainableTableModel listenerOfTransform = getListenerOfTransform(transformId);
        KeyedColumnTableModel inputModel = getInputModelOfTransform(transformId);

        ChainableTableModel transform = getTransform(transformId);
        transformationChain[transformId] = null;

        if (listenerOfTransform != null) listenerOfTransform.setSourceModel(inputModel);
        transform.setSourceModel(null);
    }

    private ChainableTableModel getFirstDataTransform() {
        for (int i = 0; i < transformationChain.length; i++) {
            ChainableTableModel chainableTableModel = transformationChain[i];
            if (chainableTableModel != null) return chainableTableModel;
        }
        return null;
    }

    private KeyedColumnTableModel getInputModelOfTransform(int transformId) {
        for (int i = transformId - 1; i >= 0; i--) {
            ChainableTableModel chainableTableModel = transformationChain[i];
            if (chainableTableModel != null) return chainableTableModel;
        }
        return getSourceModel();
    }

    private ChainableTableModel getListenerOfTransform(int transformId) {
        for (int i = transformId + 1; i < transformationChain.length; i++) {
            ChainableTableModel chainableTableModel = transformationChain[i];
            if (chainableTableModel != null) return chainableTableModel;
        }
        return null;
    }

    public ChainableTableModel getTransform(int transformId) {
        switch (transformId) {
            case SPLITTER:
                return getTableDateSeriesSpliter();
            case TRANSFORMER:
                return getTableCellValueTransformer();
            case PIVOTER:
                return getTablePivoter();
            case GROUPER:
                return getTableGrouper();
            case ROWFILTER:
                return getTableRowFilter();
            case SORTER:
                return getTableSorter();
        }
        throw new IllegalArgumentException("Invalid transform id");
    }

    public boolean isGrouped() {
        return transformationChain[GROUPER] != null;
    }

    public void setGrouped(boolean grouped) {
        if (isGrouped() == grouped) return;

        if (grouped) {
            insertTransform(GROUPER);
        } else {
            removeTransform(GROUPER);
        }
    }

    public boolean isPivoted() {
        return transformationChain[PIVOTER] != null;
    }

    public void setPivoted(boolean pivoted) {
        if (isPivoted() == pivoted) return;

        if (pivoted) {
            insertTransform(PIVOTER);
        } else {
            removeTransform(PIVOTER);
        }
    }

    public boolean isRowFiltered() {
        return transformationChain[ROWFILTER] != null;
    }

    public void setRowFiltered(boolean rowFiltered) {
        if ( isRowFiltered() == rowFiltered ) return;

        if (rowFiltered) {
            insertTransform(ROWFILTER);    
        } else {
            removeTransform(ROWFILTER);
        }
    }

    public void setIncludeSubstringsInFilterFind(boolean include) {
        tableRowFilter.setIncludeSubstringsInSearch(include);
    }

    public boolean isIncludeSubstringsInFilterFind() {
        return tableRowFilter.isIncludeSubstringsInSearch();
    }

    public boolean isUsingDateSeries() {
        return transformationChain[SPLITTER] != null;
    }

    public void setUsingDateSeries(boolean usingDateSeries) {
        if (isUsingDateSeries() == usingDateSeries) return;

        if (usingDateSeries) {
            insertTransform(SPLITTER);
        } else {
            removeTransform(SPLITTER);
        }
    }

    public boolean isVerboseDateSeries() {
        return getTableDateSeriesSpliter().isVerboseDateSeries();
    }

    public void setVerboseDateSeries(boolean verbose) {
        getTableDateSeriesSpliter().setVerboseDateSeries(verbose);
    }

    public GroupedTableModel getTableGrouper() {
        return tableGrouper;
    }

    public PivotTableModel getTablePivoter() {
        return tablePivoter;
    }

    public TransformTableModel getTableCellValueTransformer() {
        return tableCellValueTransformer;
    }

    public DateSeriesTableModel getTableDateSeriesSpliter() {
        return tableDateSeriesSpliter;
    }

    public ChainableFilteredTableModel getTableRowFilter() {
        return tableRowFilter;
    }

    public SortedTableModel getTableSorter() {
        return tableSorter;
    }

    public ChainableFilteredTableModel getRowFilteredTableModel() {
        return tableRowFilter;
    }

    public TableCellRenderer getTableHeaderRenderer(JTableHeader tableHeader) {
        return getTableSorter().getTableHeaderRenderer(tableHeader);
    }

    public boolean isColumnSelectionAllowed() {
        return isPivoted();
    }

//----------- transformer specific methods -------

    public Class getTransformClassForColumn(Object columnKey) {
        if (transformationChain[TRANSFORMER] == null) {
            return null;
        } else {
            return getTableCellValueTransformer().getTransformClassForColumn(columnKey);
        }
    }

    public void setTransformClassForColumn(Object columnKey, Class transformClass) {
        getTableCellValueTransformer().setTransformClassForColumn(columnKey, transformClass);
        if (getTableCellValueTransformer().getTransformationCount() == 0) {
            removeTransform(TRANSFORMER);
        } else if (transformationChain[TRANSFORMER] == null) {
            insertTransform(TRANSFORMER);
        }
    }

//----------- series specific methods -------

    public void setDateSeries(DateSeriesNew dateSeries) {
        getTableDateSeriesSpliter().setSeries(dateSeries);
    }

    public DateSeriesNew getDateSeries() {
        return (DateSeriesNew) getTableDateSeriesSpliter().getSeries();
    }

    public boolean isDateBucketSpreading() {
        return getTableDateSeriesSpliter().isSplittingValues();
    }

    public void setDateBucketSpreading(boolean datebucketSpreading) {
        getTableDateSeriesSpliter().setSplittingValues(datebucketSpreading);
    }

    public void setUseFullSeries(boolean useCompleteSeries) {
        getTableDateSeriesSpliter().setUseFullSeries(useCompleteSeries);
    }

    public boolean isUseFullSeries() {
        return getTableDateSeriesSpliter().isUseFullSeries();
    }

//----------- pivoter specific methods -------------

    public String getPivotColumn() {
        if (!isPivoted()) return null;

        return (String) getTablePivoter().getPivotColumnKey();
    }

    public void setPivotColumn(String columnLocator) {
        getTablePivoter().setPivotColumnKey(columnLocator);
    }

    public String getPivotDataColumn() {
        if (!isPivoted()) return null;
        return (String) getTablePivoter().getDataColumnKey();
    }

    public void setPivotDataColumn(String columnLocator) {
        getTablePivoter().setDataColumnKey(columnLocator);
    }

    public boolean isPivotGeneratedColumn(Object columnKey) {
        //todo: little bit hacky. Really we should traverse down the chain, asking each transform to map the given column to their sourceModel column space
        //todo: until we get to the tablePivoter. Then we can ask the pivoter about the column in the correct columnspace
        return (getTablePivoter().isGeneratedColumn(columnKey));
    }

    public boolean isSuitablePivotDataType(Object columnKey) {
        //only non-keys can be in the pivot data
        return !getTableGrouper().getGroupedKeyDefinition().isGroupKeyColumn(columnKey);
    }

    public boolean isSuitablePivotColumnType(Object columnKey) {
        //only allow key values to become column headings
        return getTableGrouper().getGroupedKeyDefinition().isGroupKeyColumn(columnKey);
    }

    public void invertPivot() {
        if (isPivoted()) getTablePivoter().invertPivot();
    }

    public void setColumnTotallingEnabled(boolean columnTotalling) {
        getTablePivoter().setColumnTotallingEnabled(columnTotalling);
    }

    public boolean isColumnTotallingEnabled() {
        return isPivoted() && getTablePivoter().isColumnTotallingEnabled();
    }

//----------- Grouper specific methods --------------

//----------- Sorter specific methods --------------

    public MouseListener getClickSortMouseHandler() {
        return getTableSorter().getClickSortMouseHandler();
    }

    public void addSortingColumn(Object columnKey, boolean descending) {
        getTableSorter().addSortingColumn(columnKey, descending);
    }

    public void removeSortingColumn(Object columnKey) {
        getTableSorter().removeSortingColumn(columnKey);
    }

    public boolean isSorting() {
        return getTableSorter().isSorting();
    }

    public boolean isSortingColumn(Object columnKey) {
        return getTableSorter().isSortingColumn(columnKey);
    }

    public void cancelSorting() {
        getTableSorter().cancelSorting();
    }

    public void toggleSortingDirection(Object columnKey) {
        getTableSorter().toggleSortingDirection(columnKey);
    }

    public boolean isSortDescendingColumn(Object columnKey) {
        return getTableSorter().isSortDescendingColumn(columnKey);
    }

    public boolean isRowTotallingEnabled() {
        return getTableSorter().isRowTotallingEnabled();
    }

    public void setRowTotallingEnabled(boolean rowTotalling) {
        getTableSorter().setRowTotallingEnabled(rowTotalling);
    }

//---------- Configuration bean handling ----------------

    public void setConfiguration(TableViewConfiguration configuration) {
        if (log.isDebug()) log.debug("Setting new TableViewConfiguration");

        //reset chain
        initTransformChain();

        //----beanCollectionTableModel configuration
        TableModel sourceModel = getSourceModel();
        if (sourceModel instanceof BeanCollectionTableModel) {
            BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) sourceModel;
            beanCollectionTableModel.setFormulaManager(configuration.getFormulaManager());
            beanCollectionTableModel.setColumnLocators(configuration.getTableColumnList());
            beanCollectionTableModel.setLocatorToDepthMap(configuration.getLocatorToDepthMap());
            beanCollectionTableModel.setCustomColumnNames(configuration.getCustomColumnNames());
        }

        //---- Series configuration
        if (configuration.isUsingDateSeries()) {
            DateSeriesTableModel tableDateSeriesSpliter = getTableDateSeriesSpliter();
            KeyedColumnTableModel seriesSourceModel = getInputModelOfTransform(SPLITTER);
            tableDateSeriesSpliter.setSourceModel(seriesSourceModel);

            DateSeriesNew series = configuration.getDateSeriesNew().getSize() == 2 ? DateSeriesNew.getDefaultInstance() : configuration.getDateSeriesNew();
            tableDateSeriesSpliter.setSeries(series);
            tableDateSeriesSpliter.setUseFullSeries(configuration.isUseFullSeries());
            tableDateSeriesSpliter.setSplittingValues(configuration.isSplittingValues());
            tableDateSeriesSpliter.setVerboseDateSeries(configuration.isVerboseDateSeries());

        }
        setUsingDateSeries(configuration.isUsingDateSeries());

        getTableCellValueTransformer().setColumnTransformMap(new HashMap(configuration.getColumnTransformMap()));
        if (getTableCellValueTransformer().getTransformationCount() > 0) {
            insertTransform(TRANSFORMER);
        }

        //---- Pivoter configuration
        if (configuration.isPivoted()) {
            PivotTableModel tablePivoter = getTablePivoter();
            KeyedColumnTableModel pivoterSourceModel = getInputModelOfTransform(PIVOTER);
            tablePivoter.setSourceModel(pivoterSourceModel);

            tablePivoter.setPivotColumnKey(configuration.getPivotColumnLocator());
            tablePivoter.setDataColumnKey(configuration.getPivotDataColumnLocator());
            tablePivoter.setLockedPivotColumnValues(configuration.getLockedPivotColumns());

            tablePivoter.setColumnTotallingEnabled(configuration.isColumnTotallingEnabled());
        }
        setPivoted(configuration.isPivoted());

        //---- Grouped configuration
        setGrouped(configuration.isGrouped());

        //----sorter configuration
        SortedTableModel tableSorter = getTableSorter();
        tableSorter.setSortingColumns(configuration.getSortingColumns());
        tableSorter.setRowTotallingEnabled(configuration.isRowTotallingEnabled());
    }

    public TableViewConfiguration getConfiguration() {
        TableViewConfiguration configSnapshot = new TableViewConfiguration();

        //----sorter configuration
        SortedTableModel tableSorter = getTableSorter();
        configSnapshot.setSortingColumns(tableSorter.getSortingColumns());
        configSnapshot.setRowTotallingEnabled(tableSorter.isRowTotallingEnabled());

        //---- Grouped configuration
        configSnapshot.setGrouped(isGrouped());

        //---- Pivoter configuration
        configSnapshot.setPivoted(isPivoted());
        PivotTableModel tablePivoter = getTablePivoter();
        configSnapshot.setPivotColumnLocator((String) tablePivoter.getPivotColumnKey());
        configSnapshot.setPivotDataColumnLocator((String) tablePivoter.getDataColumnKey());
        configSnapshot.setLockedPivotColumns(tablePivoter.getLockedPivotColumnValues());
        configSnapshot.setColumnTotallingEnabled(isColumnTotallingEnabled());

        configSnapshot.setColumnTransformMap(new HashMap(getTableCellValueTransformer().getColumnTransformMap()));

        //---- Series configuration
        configSnapshot.setUsingDateSeries(isUsingDateSeries());
        DateSeriesTableModel tableDateSeriesSpliter = getTableDateSeriesSpliter();
        configSnapshot.setDateSeriesNew((DateSeriesNew) tableDateSeriesSpliter.getSeries());
        configSnapshot.setSplittingValues(tableDateSeriesSpliter.isSplittingValues());
        configSnapshot.setUseFullSeries(tableDateSeriesSpliter.isUseFullSeries());
        configSnapshot.setVerboseDateSeries(tableDateSeriesSpliter.isVerboseDateSeries());

        //----beanCollectionTableModel configuration
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel instanceof BeanCollectionTableModel) {
            BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) sourceModel;
            configSnapshot.setTableColumnList(beanCollectionTableModel.getColumnLocators());
            configSnapshot.setLocatorToDepthMap((Map) ((HashMap) beanCollectionTableModel.getLocatorToDepthMap()).clone());
            configSnapshot.setCustomColumnNames(new HashMap(beanCollectionTableModel.getCustomColumnNames()));
            configSnapshot.setFormulaManager(beanCollectionTableModel.getFormulaManager());
        } else {
            configSnapshot.setTableColumnList(new ArrayList(1));
            configSnapshot.setLocatorToDepthMap(new HashMap(1));
        }


        return configSnapshot;
    }

//----------TableModel delegation--------------

    public String getColumnName(int column) {
        return getDelegateTableModel().getColumnName(column);
    }

    public String getCustomColumnName(int column) {
        KeyedColumnTableModel tableModel = getSourceModel();
        if (tableModel instanceof BeanCollectionTableModel) {
            Object columnKey = getColumnKey(column);
            if ( isPivotGeneratedColumn(columnKey)) {
                return ((PivotTableModel.GeneratedColumnKey)columnKey).getPivotValue();
            } else {
                BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) tableModel;
                return beanCollectionTableModel.getCustomColumnName(column);
            }
        } else { // not a BeanCollectionTableModel just use column Name
            return getDelegateTableModel().getColumnName(column);
        }
    }

    public Class getColumnClass(int columnIndex) {
        return getDelegateTableModel().getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getDelegateTableModel().isCellEditable(rowIndex, columnIndex);
    }

    public void addTableModelListener(TableModelListener l) {
        getDelegateTableModel().addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        getDelegateTableModel().removeTableModelListener(l);
    }

    public Object[] getBeansForLocation(int row, int column) {
        TableModel delegateTableModel = getDelegateTableModel();
        if (delegateTableModel instanceof TabularBeanAssociation) {
            return ((TabularBeanAssociation) delegateTableModel).getBeansForLocation(row, column);
        }
        return EMPTY_OBJECT_ARRAY;
    }

    public boolean isSingleBean(int row, int column) {
        TableModel delegateTableModel = getDelegateTableModel();
        if (delegateTableModel instanceof TabularBeanAssociation) {
            return ((TabularBeanAssociation) delegateTableModel).isSingleBean(row, column);
        }
        return false;
    }

    /**
     * @return a bean path for a columnKey which already exists in the model
     */
    public String getBeanPathForColumn(Object columnKey) {
        if (columnKey instanceof PivotTableModel.GeneratedColumnKey) {
            //all pivot columns should share the data column key, which is a bean path
            return getTablePivoter().getDataColumnKey().toString();
        }
        return columnKey.toString();
    }

    public Object getColumnKey(int column) {
        return getDelegateTableModel().getColumnKey(column);
    }

    public int getColumnIndex(Object columnKey) {
        return getDelegateTableModel().getColumnIndex(columnKey);
    }

    public int getRowCount() {
        return getDelegateTableModel().getRowCount();
    }

    public int getColumnCount() {
        return getDelegateTableModel().getColumnCount();
    }

    public Object getValueAt(int row, int column) {
        return getDelegateTableModel().getValueAt(row, column);
    }

    public void setValueAt(Object aValue, int row, int column) {
        getDelegateTableModel().setValueAt(aValue, row, column);
    }

    public boolean canSort(Object columnKey) {
        boolean result = true;
        if ( columnKey instanceof String) {
            result = ! getTableSorter().isExclusionColumnKey(columnKey);
        }
        return result;
    }

    public void setFilterFormatter(final Class clazz, final Format filterFormat) {
        //some bhavaya models break the contract of TableModel.getColumnClass() by returning partial bucket values etc.
        //so we have to wrap the Format with a formatter which does class checking
        FilterFormatter classCheckingFormatter = new FilterFormatter() {
            public CharSequence format(Object o) {
                return  clazz.isInstance(o) ? filterFormat.format(o) : null;
            }
        };
        getTableRowFilter().setFormatter(classCheckingFormatter, clazz);
    }

    public boolean isCellMatchingSearch(int rowIndex, int colIndex) {
        return isRowFiltered() && getDelegateTableModel().isCellMatchingSearch(rowIndex, colIndex);
    }

    public static class DateSeriesTableModel extends SeriesTableModel {
        private DateSeriesNew.Listener seriesChangedListener = new DateSeriesNew.Listener() {
            public void seriesChanged() {
                invalidateAndFireTableDataChanged();
            }
        };

        public DateSeriesTableModel() {
            setSeries(DateSeriesNew.getDefaultInstance());
        }

        public void setSeries(Series series) {
            Series oldSeries = getSeries();
            if (oldSeries instanceof DateSeriesNew) {
                ((DateSeriesNew) oldSeries).removeListener(seriesChangedListener);
            }
            ((DateSeriesNew) series).addListener(seriesChangedListener);
            super.setSeries(series);
        }

        public Object findNewSeriesColumnKey() {
            for (int column = 0; column < getSourceModel().getColumnCount(); column++) {
                Class columnClass = getSourceModel().getColumnClass(column);
                if (Date.class.isAssignableFrom(columnClass)) return getSourceModel().getColumnKey(column);
            }
            return super.findNewSeriesColumnKey();
        }


        public void setVerboseDateSeries(boolean verbose) {
            ((DateSeriesNew) getSeries()).setVerbose(verbose);
            invalidateAndFireTableDataChanged();
        }

        public boolean isVerboseDateSeries() {
            return ((DateSeriesNew) getSeries()).isVerbose();
        }
    }
}
