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

package org.bhavaya.ui.freechart;

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.ui.table.TabularBeanAssociation;
import org.bhavaya.util.Log;
import org.bhavaya.util.Numeric;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.AbstractDataset;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps a TableModel as a CategoryDataset, for graphing.
 *
 * @author Philip Milne
 * @version $Revision: 1.7 $
 */

public class TableModelDataSet extends AbstractDataset implements CategoryDataset {
    private static final Log log = Log.getCategory(TableModelDataSet.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

    private TableModel tableModel;
    private IndexedSet categories = new IndexedSet();
    private IndexedSet seriesKeys = new IndexedSet();

    private TableModelListener tableListener;

    private String domainName = "";
    private String overridenRangeName = null;

    private ArrayList columnIndexesOfDomain = new ArrayList();

    private TableModelDataSet() {
        this.tableListener = new TableModelUpdateHandler();
    }

    public TableModelDataSet(TableModel tableModel) {
        this();
        setTableModel(tableModel);
    }

    public void setTableModel(TableModel tableModel) {
        if (this.tableModel != null) {
            this.tableModel.removeTableModelListener(tableListener);
        }
        this.tableModel = tableModel;
        if (this.tableModel != null) {
            this.tableModel.addTableModelListener(tableListener);
            init();
        }
    }

    //todo: TableModelDataSet should probably assume KeyedColumnTableModel, in which case, use column keys rather than indexes
    /**
     * the indexes of the columns that are used as the Domain for the graph
     *
     * @return
     */
    public List getColumnIndexesOfDomain() {
        return columnIndexesOfDomain;
    }

    public List getSeriesKeys() {
        return seriesKeys;
    }


    private void init() {
        columnIndexesOfDomain.clear();
        seriesKeys.clear();

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            Class c = tableModel.getColumnClass(i);
            if (!isNumeric(c)) {
                columnIndexesOfDomain.add(new Integer(i));
            } else {
                String seriesName = tableModel.getColumnName(i);
                seriesKeys.add(new SeriesKey(seriesName, i));
            }
        }

        StringBuffer domainNameBuf = new StringBuffer();
        for (int i = 0; i < columnIndexesOfDomain.size(); i++) {
            Integer nameColumn = (Integer) columnIndexesOfDomain.get(i);
            domainNameBuf.append(tableModel.getColumnName(nameColumn.intValue()));
            if (i != columnIndexesOfDomain.size() - 1) {
                domainNameBuf.append(", ");
            }
        }

        domainName = domainNameBuf.toString();

        initCategories();
    }

    private void initCategories() {
        categories.clear();

        for (int categoryIdx = 0; categoryIdx < getColumnCount(); categoryIdx++) {
            StringBuffer categoryNameBuf = new StringBuffer();

            for (int i = 0; i < columnIndexesOfDomain.size(); i++) {
                int nameColumn = ((Integer) columnIndexesOfDomain.get(i)).intValue();
                if (nameColumn >= 0) {
                    Object value = tableModel.getValueAt(categoryIdx, nameColumn);
                    if (value != null) {
                        if (categoryNameBuf.length() > 0) {
                            categoryNameBuf.append(", ");
                        }
                        categoryNameBuf.append(value.toString());
                    }
                }
            }
            CategoryKey category = new CategoryKey(categoryIdx, categoryNameBuf.toString());
            categories.add(category);
        }
    }

    public CategoryKey getCategory(int category) {
        return (CategoryKey) categories.get(category);
    }

    public Number numberFromObject(Object value) {
        double amount;
        if (value instanceof Number) {
            amount = ((Number) value).doubleValue();
        } else if (value instanceof Numeric) {
            amount = ((Numeric) value).doubleValue();
        } else {
            return null;
        }
        return new Double(check(amount));
    }

    public boolean isNumeric(Class c) {
        return Number.class.isAssignableFrom(c) || Numeric.class.isAssignableFrom(c);
    }

    private double check(double amount) {
        if (Double.isInfinite(amount) || Double.isNaN(amount)) {
            amount = 0.0d;
        }
        return amount;
    }

    public int getSeriesCount() {
        return seriesKeys.size();
    }

    public SeriesKey getSeriesKey(int series) {
        return (SeriesKey) seriesKeys.get(series);
    }

    public String getDomainName() {
        return domainName;
    }

    public String getRangeName() {
        if (getSeriesCount() == 0) {
            return "";
        } else {
            return getSeriesKey(0).getSeriesName();
        }
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    public Object[] getBeansForLocation(int series, Object category) {
        if (tableModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation beanAssociation = (TabularBeanAssociation) tableModel;
            int row = ((CategoryKey) category).getTableRow();
            int col = getSeriesKey(series).getColumnIndex();
            return beanAssociation.getBeansForLocation(row, col);
        }
        return EMPTY_OBJECT_ARRAY;
    }


//------------------ CategoryDataset implementation -------------------------
    //CategoryDataset "Rows" are equivalent to the concept of a having multiple data "series" sharing the same domain. One row per series

    public int getRowCount() {
        return getSeriesCount();
    }

    public Comparable getRowKey(int row) {
        return getSeriesKey(row);
    }

    public List getRowKeys() {
        return seriesKeys;
    }

    public int getRowIndex(Comparable key) {
        return seriesKeys.indexOf(key);
    }


    //CategoryDataset "Columns" are equivalent to the categories that belong to the Domain of the graph.

    public int getColumnIndex(Comparable key) {
        return categories.indexOf(key);
    }

    public Comparable getColumnKey(int column) {
        return getCategory(column);
    }

    public List getColumnKeys() {
        return categories;
    }

    public int getColumnCount() {
        return getTableModel().getRowCount();
    }


    // mapping series and category combinations to actual values in the Range

    public Number getValue(Comparable seriesKey, Comparable categoryKey) {
        SeriesKey series = ((SeriesKey) seriesKey);
        CategoryKey category = (CategoryKey) categoryKey;

        Object value = tableModel.getValueAt(category.getTableRow(), series.getColumnIndex());
        if (value == null) {
            return null;
        } else {
            return numberFromObject(value);
        }
    }

    public Number getValue(int series, int dataPoint) {
        SeriesKey seriesKey = getSeriesKey(series);
        CategoryKey categoryKey = getCategory(dataPoint);

        return getValue(seriesKey, categoryKey);
    }

    //---- inner classes ---------

    private static class CategoryKey implements Comparable {
        private int tableRow;
        private String category;

        public CategoryKey(int row, String category) {
            this.tableRow = row;
            this.category = category;
        }

        public int getTableRow() {
            return tableRow;
        }

        public int compareTo(Object o) {
            return 0;
        }

        public String toString() {
            return category;
        }
    }

    public static class SeriesKey implements Comparable {
        int columnIndex;
        String seriesName;

        public SeriesKey(String seriesName, int columnIndex) {
            if (seriesName == null) seriesName = "";
            this.seriesName = seriesName;
            this.columnIndex = columnIndex;
        }

        public int compareTo(Object o) {
            return seriesName.compareTo((String) o);
        }

        public String toString() {
            return seriesName;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public String getSeriesName() {
            return seriesName;
        }
    }

    private class TableModelUpdateHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            int eventRow = e.getFirstRow();
            int eventColumn = e.getColumn();
            int type = e.getType();

            //try to do work out how the data has changed to minimise cpu overhead
            boolean cellValuesChanged = (type == TableModelEvent.UPDATE
                    && e.getLastRow() != Integer.MAX_VALUE
                    && eventColumn != TableModelEvent.ALL_COLUMNS);

            if (cellValuesChanged) {
                fireDatasetChanged();
            } else {
                //rows or columns inserted or deleted. work out the domain and range again.
                init();
                fireDatasetChanged();
            }
        }
    }
}
