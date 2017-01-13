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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.ui.series.Series;
import org.bhavaya.util.Quantity;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This class used to be nice, but now it is really nasty. All of that weird split buckets stuff has been hacked in horribly.
 * Sorry.
 * <p/>
 * What is it supposed to do? Ummm, it used to be a straight value mapper. I.e. it mapped cell values to other cell values
 * the context we used it in was to map date values into a date interval.  e.g. (11/8/03) maps to interval (01/01/03->01/01/04)
 * Then things got a bit polluted.
 * <p/>
 * Now it uses a "Series", this series maps a single value to one, or many output values.
 * This means that a single row of the input model may end up as multiple output rows. Now, if we are totalling some numerical column, our totals are
 * going to screw up due to this duplication of rows. Therefore the series mapping can also provide coefficients that will be applied to
 * any column that passes the "isSplittable" test.
 * Yes, it's all rather sucky and probably has no use outside of a specific business context. Here is an example:
 * <p/>
 * Our input row is:
 * 7.5,   2.0
 * Imagine a series defined as {0->10, 5->15, 10->20}. The value 7.5 belongs both in 0->10 and 5->15.
 * After the transform, without splitting, we would end up with 2 rows:
 * 0->10,   2.0
 * 5->15,   2.0
 * if we were totalling the numerical column, we would have a total of 4.0 - wrong!
 * <p/>
 * Now imagine we want to say that the 2.0 should be distributed between the two rows (according to some business logic),
 * our series will return both mapped values and coefficients: e.g. for an input value of 7.5, output is {(0->10, 0.75) , (5->15, 0.25)}
 * these coefficients would be applied to all numerical fields
 * 0->10,   1.5
 * 5->15,   0.5
 * now our total row would correctly read 2.0 - hurrah
 * <p/>
 * TODO: dan, use columnKeys to decide splitable columns (not column class) then in the contect of analytics table, we can prevent coefficients being applied to non-grouping columns)
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.11 $
 */
public class SeriesTableModel extends AbstractTableModel implements TabularBeanAssociation, ChainableTableModel {
    private static final Series.SeriesValue[] DATA_NOT_READY_SERIES_VALUE = new Series.SeriesValue[]{new Series.SeriesValue(CachedObjectGraph.DATA_NOT_READY, 1.0)};
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private KeyedColumnTableModel sourceModel;

    private boolean uncalculated = true;

    private List categoryValues = new ArrayList();
    private HashSet missingCategories = new HashSet();

    private boolean useFullSeries = false;
    private TableModelEventHandler eventHandler;

    private Object seriesColumnKey = null;
    private int seriesColumnIndex = -1;

    private Series series = null;

    private TIntObjectHashMap splitCellColumns = new TIntObjectHashMap();   //Column number to (List of cell values, one per row)
    private TDoubleArrayList splitCoefficients = new TDoubleArrayList();

    private int generatedRowsStartIndex = -1;

    private TIntArrayList modelRowToUnderlyingRow = new TIntArrayList();
    private TIntArrayList underlyingRowToFirstModelRow = new TIntArrayList();
    private boolean splittingValues = false;

    public SeriesTableModel() {
        eventHandler = new TableModelEventHandler();
    }

    public SeriesTableModel(KeyedColumnTableModel tableModel) {
        this();
        setSourceModel(tableModel);
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceModel;
    }

    public void setSourceModel(KeyedColumnTableModel sourceModel) {
        if (this.sourceModel != null) {
            this.sourceModel.removeTableModelListener(eventHandler);
        }
        this.sourceModel = sourceModel;
        if (sourceModel != null) {
            sourceModel.addTableModelListener(eventHandler);
        }
        invalidateSeriesColumnIndex();
        setUncalculated();
        fireTableStructureChanged();
    }

    public Object getValueAt(int row, int column) {
        if (hasValidSeriesColumn()) {
            List seriesValues = getSeriesValues();
            if (column == getSeriesColumnIndex()) {
                return seriesValues.get(row);
            } else if (row >= getGeneratedRowsStartIndex()) {
                return null;
            } else if (isSplitColumn(column)) {
                List columnValues = (List) splitCellColumns.get(column);
                return columnValues.get(row);
            }
        }
        TableModel sourceModel = getSourceModel();
        int underlyingRow = hasValidSeriesColumn() ? getUnderlyingRowForModel(row) : row;
        return sourceModel.getValueAt(underlyingRow, column);
    }

    private boolean isSplitColumn(int column) {
        return isSplittingValues() && splitCellColumns.containsKey(column);
    }

    private int getGeneratedRowsStartIndex() {
        return generatedRowsStartIndex;
    }

    private boolean isUncalculated() {
        return uncalculated;
    }

    private void setUncalculated() {
        this.uncalculated = true;
    }

    private List getSeriesValues() {
        if (isUncalculated()) {
            recalculate();
        }
        return categoryValues;
    }

    private void recalculate() {
        uncalculated = false;
        categoryValues.clear();
        splitCoefficients.clear();
        splitCellColumns.clear();

        underlyingRowToFirstModelRow.clear();
        modelRowToUnderlyingRow.clear();
        missingCategories.clear();
        TableModel sourceModel = getSourceModel();

        int rowCount = sourceModel.getRowCount();
        if (hasValidSeriesColumn()) {
            missingCategories.addAll(getSeries().getFullSeriesValues());
            if (isSplittingValues()) {
                for (int column = 0; column < sourceModel.getColumnCount(); column++) {
                    if (isSplittable(sourceModel.getColumnClass(column))) {
                        List columnValues = new ArrayList();
                        splitCellColumns.put(column, columnValues);
                    }
                }
            }

            for (int row = 0; row < rowCount; row++) {
                int seriesColumn = getSeriesColumnIndex();
                Object value;
                underlyingRowToFirstModelRow.add(categoryValues.size());
                value = sourceModel.getValueAt(row, seriesColumn);
                Series.SeriesValue[] values;
                if (value == CachedObjectGraph.DATA_NOT_READY) {
                    values = DATA_NOT_READY_SERIES_VALUE;
                } else {
                    values = getSeries().getSeriesValues(value, isSplittingValues());
                }
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        Series.SeriesValue seriesValue = values[i];
                        Object series = seriesValue.getSeries();

                        modelRowToUnderlyingRow.add(row);

                        if (isSplittingValues()) {
                            double coefficient = seriesValue.getCoefficient();
                            splitCoefficients.add(coefficient);
                            applyCoefficientToRow(categoryValues.size());
                        }

                        categoryValues.add(series);
                        missingCategories.remove(series);
                    }
                }
            }
            generatedRowsStartIndex = categoryValues.size();
            if (isUseFullSeries()) {
                for (Iterator iterator = missingCategories.iterator(); iterator.hasNext();) {
                    Object value = iterator.next();
                    categoryValues.add(value);
                }
            }
        }
    }

    public boolean isSplittable(Class columnClass) {
        return columnClass == Quantity.class || columnClass == Double.class;
    }

    public boolean isSplittingValues() {
        return splittingValues;
    }

    public void setSplittingValues(boolean splittingValues) {
        this.splittingValues = splittingValues;
        invalidateAndFireTableDataChanged();
    }

    private void applyCoefficientToRow(final int row) {
        splitCellColumns.forEachKey(new TIntProcedure() {
            public boolean execute(int i) {
                updateSplitCell(row, i);
                return true;
            }
        });
    }

    private Object updateSplitCell(int row, int column) {
        double coefficient = splitCoefficients.get(row);
        int sourceRow = getUnderlyingRowForModel(row);
        Object value = getSourceModel().getValueAt(sourceRow, column);
        Object newValue = applyCoefficient(coefficient, value);

        List columnValues = (List) splitCellColumns.get(column);
        if (row == columnValues.size()) {
            columnValues.add(newValue);
        } else {
            columnValues.set(row, newValue);
        }

        return newValue;
    }

    private Object applyCoefficient(double coefficient, Object value) {
        if (value == CachedObjectGraph.DATA_NOT_READY) return CachedObjectGraph.DATA_NOT_READY;

        if (value instanceof Quantity) {
            return ((Quantity) value).product(coefficient);
        } else if (value instanceof Double) {
            return new Double(((Double) value).doubleValue() * coefficient);
        }
        return null;
    }

    private int getUnderlyingRowForModel(int row) {
        return modelRowToUnderlyingRow.get(row);
    }

    private IntRange getRowsForUnderlying(int underlyingRow) {
        if (!hasValidSeriesColumn()) {
            return new IntRange(underlyingRow, underlyingRow + 1);
        }

        final int firstRowIndex = underlyingRowToFirstModelRow.get(underlyingRow);

        int stopIndex = generatedRowsStartIndex;
        if ((underlyingRow + 1) < underlyingRowToFirstModelRow.size()) {
            stopIndex = underlyingRowToFirstModelRow.get(underlyingRow + 1);
        }

        return new IntRange(firstRowIndex, stopIndex);
    }

    private static class IntRange {
        public int start;
        public int end;

        public IntRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
        invalidateAndFireTableDataChanged();
    }

    /**
     * override this to find a suitable series column if the previous one is deleted
     * //todo: don't like this approach, but it will do for now
     */
    public Object findNewSeriesColumnKey() {
        return null;
    }

    public Object getSeriesColumnKey() {
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel != null) {
            int index = sourceModel.getColumnIndex(seriesColumnKey);
            if (index < 0) {
                seriesColumnKey = findNewSeriesColumnKey();
            }
        }
        return seriesColumnKey;
    }

    public void setSeriesColumnKey(Object seriesColumnKey) {
        setSeriesColumnKey(seriesColumnKey, true);
    }

    public void setSeriesColumnKey(Object seriesColumnKey, boolean fireEvents) {
        this.seriesColumnKey = seriesColumnKey;
        seriesColumnIndex = getSourceModel().getColumnIndex(seriesColumnKey);
        if (fireEvents) {
            invalidateSeriesColumnIndex();
            setUncalculated();
            fireTableStructureChanged();
        }
    }

    private boolean hasValidSeriesColumn() {
        return getSeriesColumnIndex() >= 0;
    }

    private int getSeriesColumnIndex() {
        if (seriesColumnIndex < 0) {
            seriesColumnIndex = getSourceModel().getColumnIndex(getSeriesColumnKey());
        }
        return seriesColumnIndex;
    }

    private void invalidateSeriesColumnIndex() {
        seriesColumnIndex = -1;
    }

    public boolean isUseFullSeries() {
        return useFullSeries;
    }

    public void setUseFullSeries(boolean useFullSeries) {
        this.useFullSeries = useFullSeries;
        invalidateAndFireTableDataChanged();
    }

    public int getColumnCount() {
        return getSourceModel().getColumnCount();
    }

    public int getRowCount() {
        if (hasValidSeriesColumn()) return getSeriesValues().size();
        return getSourceModel().getRowCount();
    }

    public int getColumnIndex(Object columnKey) {
        return getSourceModel().getColumnIndex(columnKey);
    }

    public Object getColumnKey(int column) {
        return getSourceModel().getColumnKey(column);
    }

    public Object[] getBeansForLocation(int row, int column) {
        TableModel tableModel = getSourceModel();
        if (tableModel instanceof TabularBeanAssociation) {
            int underlyingRow = row;
            if (hasValidSeriesColumn()) {
                if (row < getGeneratedRowsStartIndex()) {
                    underlyingRow = getUnderlyingRowForModel(row);
                } else {
                    return EMPTY_OBJECT_ARRAY;
                }
            }

            return ((TabularBeanAssociation) tableModel).getBeansForLocation(underlyingRow, column);
        }
        return EMPTY_OBJECT_ARRAY;
    }

    public boolean isSingleBean(int row, int column) {
        if (sourceModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) sourceModel;
            return tabularBeanAssociation.isSingleBean(row, column);
        }
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == getSeriesColumnIndex()) {
            return getSeries().getSeriesValueClass();
        } else {
            return getSourceModel().getColumnClass(columnIndex);
        }
    }

    public String getColumnName(int column) {
        return getSourceModel().getColumnName(column);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == getSeriesColumnIndex()) {
            return false;
        } else if (isSplitColumn(columnIndex)){
            return false;
        } else {
            int underlyingRow = hasValidSeriesColumn() ? getUnderlyingRowForModel(rowIndex) : rowIndex;
            return getSourceModel().isCellEditable(underlyingRow, columnIndex);
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getSourceModel().setValueAt(aValue, rowIndex, columnIndex);
    }

    public void invalidateAndFireTableDataChanged() {
        setUncalculated();
        fireTableDataChanged();
    }

    private class TableModelEventHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            if (!hasValidSeriesColumn()) {
                fireTableChanged(e);
                return;
            }

            int eventRow = e.getFirstRow();

            // If table columns changed...
            if (eventRow == TableModelEvent.HEADER_ROW) {
                //todo: could handle special case column insertion and deletion?
                invalidateSeriesColumnIndex();
                setUncalculated();
                fireTableStructureChanged();    //both the data changed and columns changed = structure change
            } else { //not the header row
                if (isUncalculated()) {
                    invalidateAndFireTableDataChanged();
                } else {
                    if (e.getType() != TableModelEvent.UPDATE) {
                        invalidateAndFireTableDataChanged();
                    } else {
                        //handle the update
                        if (!(e instanceof CellsInColumnUpdatedEvent) && !(e instanceof MultipleColumnChangeEvent)) {
                            invalidateAndFireTableDataChanged();
                            return;
                        } else {
                            boolean sharedRows = true;
                            CellsInColumnUpdatedEvent[] changes;
                            if (e instanceof MultipleColumnChangeEvent) {
                                MultipleColumnChangeEvent multiEvent = (MultipleColumnChangeEvent) e;
                                sharedRows = multiEvent.isChangedRowsSameForEachColumn();
                                changes = multiEvent.getColumnChanges();
                            } else {
                                changes = new CellsInColumnUpdatedEvent[]{(CellsInColumnUpdatedEvent) e};
                            }
                            handleValueChanges(changes, sharedRows);
                        }
                    }
                }
            }
        }

        private void handleValueChanges(CellsInColumnUpdatedEvent[] changes, boolean sharedRows) {
            CellsInColumnUpdatedEvent[] remappedChanges = new CellsInColumnUpdatedEvent[changes.length];
            int remappedCount = 0;
            int[] remappedRows = null;
            TIntObjectHashMap splitChanges = null;
            CellsInColumnUpdatedEvent seriesChange = null;

            //first we need to fire the changes for cells that are retrieved from the parent model (i.e. not the series column or a split value column
            for (int i = 0; i < changes.length; i++) {
                CellsInColumnUpdatedEvent change = changes[i];

                int column = change.getColumn();
                if (isSplittingValues()) {
                    if (column == getSeriesColumnIndex()) {
                        seriesChange = change;
                        change = null;
                    } else if (isSplitColumn(column)) {
                        if (splitChanges == null) splitChanges = new TIntObjectHashMap(changes.length);
                        Object obj = splitChanges.put(column, change);
                        ApplicationDiagnostics.getInstance().productionAssert(obj == null, "Dan, you have a bug in the SeriesModel. Multiple changes for a given column not handled correctly.");
                        change = null;
                    } else {
                        if (remappedRows == null || !sharedRows) {
                            remappedRows = remapRows(change.getRows());
                        }
                        change = new CellsInColumnUpdatedEvent(SeriesTableModel.this, remappedRows, column, change);
                    }
                } else {
                    if (change.getColumn() == getSeriesColumnIndex()) {
                        Object oldValue = updateCategoryValues(change);
                        Object newValue = categoryValues.get(change.getRowIndex(0));
                        change = new CellsInColumnUpdatedEvent(SeriesTableModel.this, change.getRows(), column, oldValue, newValue);
                    }
                }

                if (change != null) {
                    remappedChanges[remappedCount++] = change;
                }
            }
            //send those remapped changes
            if (remappedCount != 0) {
                if (remappedCount == 1) {
                    fireTableChanged(remappedChanges[0]);
                } else {
                    if (remappedCount != remappedChanges.length) {
                        //drop null elements
                        CellsInColumnUpdatedEvent[] tmp = new CellsInColumnUpdatedEvent[remappedCount];
                        System.arraycopy(remappedChanges, 0, tmp, 0, remappedCount);
                        remappedChanges = tmp;
                    }
                    MultipleColumnChangeEvent newEvent = new MultipleColumnChangeEvent(SeriesTableModel.this, remappedChanges, sharedRows);
                    fireTableChanged(newEvent);
                }
            }

            //if some changes got deferred for later...
            if (remappedCount != remappedChanges.length) {
                //now do the series column
                int[] updatedSplitColumnIndexes;
                if (seriesChange != null) {
                    updateAndFireSeriesColumn(seriesChange);
                    //if that invalidated things, then abort
                    if (isUncalculated()) {
                        return;
                    }
                    updatedSplitColumnIndexes = splitCellColumns.keys();
                } else {
                    updatedSplitColumnIndexes = splitChanges.keys();
                }

                //finally update the splitColumns
                if (sharedRows) {
                    updateAndFireForSplitColumns(updatedSplitColumnIndexes, changes[0].getRows());
                } else {
                    //each column has different rows updated, we need to handle each updated column individually
                    for (int i = 0; i < updatedSplitColumnIndexes.length; i++) {
                        int updatedSplitColumnIndex = updatedSplitColumnIndexes[i];

                        //some rows may have been updated by a change to the series column
                        if (seriesChange != null) {
                            updateAndFireForSplitColumns(new int[]{updatedSplitColumnIndex}, seriesChange.getRows());
                        }
                        //and others may have been updated directly
                        CellsInColumnUpdatedEvent columnChange = ((CellsInColumnUpdatedEvent) splitChanges.get(updatedSplitColumnIndex));
                        if (columnChange != null) {
                            updateAndFireForSplitColumns(new int[]{updatedSplitColumnIndex}, columnChange.getRows());
                        }
                    }
                }
            }
        }

        private int[] remapRows(int[] rows) {
            TIntArrayList mappedRows = new TIntArrayList(rows.length * 2);
            for (int i = 0; i < rows.length; i++) {
                int row = rows[i];
                IntRange rowsForUnderlying = getRowsForUnderlying(row);
                for (int mappedRow = rowsForUnderlying.start; mappedRow < rowsForUnderlying.end; mappedRow++) {
                    mappedRows.add(mappedRow);
                }
            }
            return mappedRows.toNativeArray();
        }

        private void updateAndFireForSplitColumns(int[] splitColumnIndexes, int[] rows) {
            assert (isSplittingValues()) : "Should be splitting values if we are here";
            for (int i = 0; i < rows.length; i++) {
                int sourceRow = rows[i];
                IntRange rowsForUnderlying = getRowsForUnderlying(sourceRow);
                double coefficientSanityCheck = 0;

                for (int mappedRow = rowsForUnderlying.start; mappedRow < rowsForUnderlying.end; mappedRow++) {
                    double coefficient = splitCoefficients.get(mappedRow);
                    coefficientSanityCheck += coefficient;

                    CellsInColumnUpdatedEvent[] newEvents = new CellsInColumnUpdatedEvent[splitColumnIndexes.length];
                    int newEventIndex = 0;
                    for (int j = 0; j < splitColumnIndexes.length; j++) {
                        int splitColumnIndex = splitColumnIndexes[j];
                        List columnValues = (List) splitCellColumns.get(splitColumnIndex);
                        Object oldValue = columnValues.get(mappedRow);

                        Object value = getSourceModel().getValueAt(sourceRow, splitColumnIndex);
                        Object newValue = applyCoefficient(coefficient, value);
                        columnValues.set(mappedRow, newValue);

                        CellsInColumnUpdatedEvent newEvent = new CellsInColumnUpdatedEvent(SeriesTableModel.this, mappedRow, splitColumnIndex, oldValue, newValue);
                        newEvents[newEventIndex++] = newEvent;
                    }
                    if (newEvents.length == 1) {
                        fireTableChanged(newEvents[0]);
                    } else {
                        fireTableChanged(new MultipleColumnChangeEvent(SeriesTableModel.this, newEvents, true));
                    }
                }
                ApplicationDiagnostics.getInstance().productionAssert(coefficientSanityCheck == 1.0, "Major issue: Coefficients of split rows do not add up to 1.0!");
            }
        }

        private void updateAndFireSeriesColumn(CellsInColumnUpdatedEvent seriesChange) {
            Series.SeriesValue[] seriesValues;
            Series.SeriesValue[] oldSeriesValues;
            if (seriesChange.getNewValue() == CachedObjectGraph.DATA_NOT_READY) {
                seriesValues = DATA_NOT_READY_SERIES_VALUE;
            } else {
                seriesValues = getSeries().getSeriesValues(seriesChange.getNewValue(), true);
            }
            if (seriesChange.getOldValue() == CachedObjectGraph.DATA_NOT_READY) {
                oldSeriesValues = DATA_NOT_READY_SERIES_VALUE;
            } else {
                oldSeriesValues = getSeries().getSeriesValues(seriesChange.getOldValue(), true);
            }

            if (oldSeriesValues.length != seriesValues.length) {
                //rows will be added or removed. for simplicity, invalidate and abort. Will make this better if needed
                invalidateAndFireTableDataChanged();
                return;
            }
            for (int seriesIndex = 0; seriesIndex < seriesValues.length; seriesIndex++) {
                Object newSeriesValue = seriesValues[seriesIndex].getSeries();
                Object oldSeriesValue = oldSeriesValues[seriesIndex].getSeries();
                int[] mappedRows = new int[seriesChange.getRowCount()];
                for (int i = 0; i < seriesChange.getRowCount(); i++) {
                    int mappedRow = underlyingRowToFirstModelRow.get(seriesChange.getRowIndex(i)) + seriesIndex;
                    mappedRows[i] = mappedRow;
                    categoryValues.set(mappedRow, newSeriesValue);
                }
                CellsInColumnUpdatedEvent newEvent = new CellsInColumnUpdatedEvent(SeriesTableModel.this, mappedRows, getSeriesColumnIndex(), oldSeriesValue, newSeriesValue);
                fireTableChanged(newEvent);
            }
            return;
        }

        /**
         * @return the old series value
         */
        private Object updateCategoryValues(CellsInColumnUpdatedEvent change) {
            Object newValue = change.getNewValue();
            if (newValue != CachedObjectGraph.DATA_NOT_READY) {
                Series.SeriesValue[] seriesValues = getSeries().getSeriesValues(newValue, isSplittingValues());
                newValue = seriesValues[0].getSeries();
            }

            Object oldValue = null;
            for (int i = 0; i < change.getRowCount(); i++) {
                int row = change.getRowIndex(i);
                oldValue = categoryValues.set(row, newValue);
            }
            return oldValue;
        }

        private boolean isEventAffectsSplitValues(TableModelEvent e) {
            if (!isSplittingValues()) return false;

            int column = e.getColumn();
            if (column == TableModelEvent.ALL_COLUMNS) {
                if (e instanceof MultipleColumnChangeEvent) {
                    CellsInColumnUpdatedEvent[] columnChanges = ((MultipleColumnChangeEvent) e).getColumnChanges();
                    for (int i = 0; i < columnChanges.length; i++) {
                        CellsInColumnUpdatedEvent change = columnChanges[i];
                        column = change.getColumn();
                        if (isSplitColumn(column)) return true;
                        if (getSeriesColumnIndex() == column) return true;
                    }
                    return false;
                } else {
                    return true;
                }
            }

            if (isSplitColumn(column)) return true;
            if (getSeriesColumnIndex() == column) return true;

            return false;
        }
    }
}
