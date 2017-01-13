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

package org.bhavaya.ui;

import org.bhavaya.ui.series.DateSeriesNew;
import org.bhavaya.ui.table.PivotTableModel;
import org.bhavaya.ui.table.SortedTableModel;
import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;
import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.formula.conditionalhighlight.HighlightConditionSet;
import org.bhavaya.ui.table.formula.FormulaManager;
import org.bhavaya.util.*;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.beans.Encoder;
import java.beans.Statement;
import java.util.*;
import java.util.List;

/**
 * Plain databean representing persistable the view
 *
 * @author
 * @version $Revision: 1.19.4.1 $
 */
public class TableViewConfiguration {
    static {
        // The persistence of a DefaultTableColumnModel is non-standard, so the delegate helps out.
        BeanUtilities.addPersistenceDelegate(ColumnHidingColumnModel.class, new ColumnHidingColumnModelPersistenceDelegate());

        BeanUtilities.addPersistenceDelegate(HidableTableColumn.class, new BhavayaPersistenceDelegate(new String[]{"modelIndex", "width"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                HidableTableColumn tc = (HidableTableColumn) oldInstance;
                out.writeStatement(new Statement(tc, "setIdentifier", new Object[]{tc.getIdentifier()}));
                if ( tc.getColumnGroup() != null) {
                    out.writeStatement(new Statement(tc, "setColumnGroup", new Object[]{tc.getColumnGroup()}));
                }
            }
        });

        Configuration.addSaveTask(new Task("Saving used Colours") {
            public void run() {
                Configuration configuration = Configuration.getRoot();
                configuration.putObject("UsedColours", usedColours);
            }
        });
    }

    private TableColumnModel tableColumnModel;
    private TableColumnModel fixedColumnModel;
    private List tableColumnList;
    private boolean grouped = false;
    private boolean usingDateSeries = false;
    private boolean rowTotallingEnabled = false;
    private boolean columnTotallingEnabled = false;

    private boolean pivoted = false;
    private String pivotColumnLocator = "";
    private String pivotDataColumnLocator = "";
    private Set<String> lockedPivotColumns = new HashSet<String>();

    private Map sortingColumns = new HashMap();
    private DateSeriesNew dateSeries = new DateSeriesNew();
    private boolean splittingValues = false;
    private boolean useFullSeries = false;
    private boolean verboseDateSeries = false;
    private Map locatorToDepthMap;
    private HashMap columnColorMap = new HashMap();
    private HashMap columnRendererIds = new HashMap();
    private HashMap columnBucketTypes = new HashMap();
    private HashMap columnFont = new HashMap();
    private HashSet showZeroAsBlankKeys = new HashSet();
    private HashSet showNaNAsBlankKeys = new HashSet();
    private HashSet selectAfterDecimalKeys = new HashSet();
    private boolean highlightChanges = true;
    private boolean highlightNewRows = false;
    private boolean columnColourHasPriority = false;
    private boolean fading = true;
    private Font tableFont;
    private HashMap columnTransformMap = new HashMap();
    private boolean showFilterFindPanel;
    private boolean includeSubstringsInFind;
    private boolean filterWithFind;
    private Map<String, HighlightConditionSet> highlightConditionalSetMap = new HashMap<String, HighlightConditionSet>();

    private static volatile boolean initialisedUsedColurs = false;
    private static final Object initLock = new Object();

    private static LinkedList usedColours;

    private HashMap customColumnNames = new HashMap();

    // This holds the keys of all the highlighted rows
    private HashSet highlightedRowKeys = new HashSet();
    // This map holds all the highlighted row keys along with the highlight colour. Here we are
    // duplicating the keys stored but this makes our migration strategy simpler. In the future
    // we can remove the above highlightedRowKeys Set.
    private HashMap highlightedRowColours = new HashMap();

    private HashMap customCellFormats = new HashMap();

    private boolean showHedging = false;    //todo: hacky hack. sorry. I want to move the view configuration thing to

    private FormulaManager formulaManager;

    private String category = "";

    //use a Configuration node rather than a dedicated bean. This allows different models
    //and business views to persist their own stuff in a nice independent manner
    public TableViewConfiguration() {
       }

    private static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialisedUsedColurs) return;
            initialisedUsedColurs = true;
            usedColours = Configuration.getRoot().getObject("UsedColours",
                    new LinkedList(), LinkedList.class);
        }
    }

    public static LinkedList getUsedColours() {
        init();
        return usedColours;
    }

    public static void setUsedColours(LinkedList list) {
        usedColours = list;
    }

    public Map getSortingColumns() {
        return sortingColumns;
    }

    public HashMap getCustomColumnNames() {
        return customColumnNames;
    }

    public void setCustomColumnNames(HashMap customColumnNames) {
        this.customColumnNames = customColumnNames;
    }

    public void setSortingColumns(Map sortingColumns) {
        this.sortingColumns = sortingColumns;
    }

    public Map getLocatorToDepthMap() {
        return locatorToDepthMap;
    }

    public void setLocatorToDepthMap(Map locatorToDepthMap) {
        this.locatorToDepthMap = locatorToDepthMap;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public void setGrouped(boolean grouped) {
        this.grouped = grouped;
    }

    public TableColumnModel getTableColumnModel() {
        return tableColumnModel;
    }

    public void setTableColumnModel(TableColumnModel tableColumnModel) {
        this.tableColumnModel = tableColumnModel;
    }

    public TableColumnModel getFixedColumnModel() {
        return fixedColumnModel;
    }

    public void setFixedColumnModel(TableColumnModel tableColumnModel) {
        this.fixedColumnModel = tableColumnModel;
    }

    public List getTableColumnList() {
        return tableColumnList;
    }

    public void setTableColumnList(List tableColumnList) {
        this.tableColumnList = tableColumnList;
    }

    public boolean isPivoted() {
        return pivoted;
    }

    public void setPivoted(boolean pivoted) {
        this.pivoted = pivoted;
    }

    public String getPivotColumnLocator() {
        return pivotColumnLocator;
    }

    public void setPivotColumnLocator(String pivotColumnId) {
        this.pivotColumnLocator = pivotColumnId;
    }

    public String getPivotDataColumnLocator() {
        return pivotDataColumnLocator;
    }

    public void setPivotDataColumnLocator(String pivotDataColumnId) {
        this.pivotDataColumnLocator = pivotDataColumnId;
    }

    public boolean isRowTotallingEnabled() {
        return rowTotallingEnabled;
    }

    public void setRowTotallingEnabled(boolean rowTotallingEnabled) {
        this.rowTotallingEnabled = rowTotallingEnabled;
    }

    public boolean isColumnColourHasPriority() {
        return columnColourHasPriority;
    }

    public void setColumnColourHasPriority(boolean columnColourHasPriority) {
        this.columnColourHasPriority = columnColourHasPriority;
    }

    public boolean isColumnTotallingEnabled() {
        return columnTotallingEnabled;
    }

    public void setColumnTotallingEnabled(boolean columnTotallingEnabled) {
        this.columnTotallingEnabled = columnTotallingEnabled;
    }

    public boolean isIncludeSubstringsInFind() {
        return includeSubstringsInFind;
    }

    public void setIncludeSubstringsInFind(boolean includeSubstringsInSearch) {
        this.includeSubstringsInFind = includeSubstringsInSearch;
    }

    public boolean isFilterWithFind() {
        return filterWithFind;
    }

    public void setFilterWithFind(boolean filterWithFind) {
        this.filterWithFind = filterWithFind;
    }

    public boolean isShowFilterFindPanel() {
        return showFilterFindPanel;
    }

    public void setShowFilterFindPanel(boolean showFilterFindPanel) {
        this.showFilterFindPanel = showFilterFindPanel;
    }

    public DateSeriesNew getDateSeriesNew() {
        return dateSeries;
    }

    public void setDateSeriesNew(DateSeriesNew dateSeries) {
        this.dateSeries = dateSeries;
    }

    public void setDateSeries(DateSeriesNew dateSeriesNew) {
        this.dateSeries = dateSeriesNew;
    }

    /**
     * Part of an elaborate deprecation strategy.  Remove March 03
     * @deprecated
     */
    public DateSeriesNew getDateSeries() {
        return dateSeries;
    }

//todo: NOTE, we must implement the hashcode!!!
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        TableViewConfiguration other = (TableViewConfiguration) obj;

        if (!tableColumnModelEqual(this.tableColumnModel, other.tableColumnModel, other.tableColumnList)) return false;
        if (!tableColumnModelEqual(this.fixedColumnModel, other.fixedColumnModel, other.tableColumnList)) return false;
        if (!Utilities.equals(tableColumnList, other.tableColumnList)) return false;
        if (!(grouped == other.grouped)) return false;
        if (!(rowTotallingEnabled == other.rowTotallingEnabled)) return false;
        if (!(pivoted == other.pivoted)) return false;
        if (pivoted) {
            if (!(columnTotallingEnabled == other.columnTotallingEnabled)) return false;
            if (!Utilities.equals(pivotColumnLocator, other.pivotColumnLocator)) return false;
            if (!Utilities.equals(pivotDataColumnLocator, other.pivotDataColumnLocator)) return false;
            if (!Utilities.equals(lockedPivotColumns, other.lockedPivotColumns)) return false;
        }

        if (!Utilities.equals(SortedTableModel.copySortingColumns(sortingColumns), SortedTableModel.copySortingColumns(other.sortingColumns))) return false;
        if (!(usingDateSeries == other.usingDateSeries)) return false;
        if (usingDateSeries) {
            if (!Utilities.equals(dateSeries, other.dateSeries)) return false;
            if (!(useFullSeries == other.useFullSeries)) return false;
            if (!(splittingValues == other.splittingValues)) return false;
        }

        if (!Utilities.equals(columnColorMap, other.columnColorMap)) return false;
        if (!columnKeyMapEqual(columnRendererIds, other.columnRendererIds)) return false;
        if (!columnKeyMapEqual(columnBucketTypes, other.columnBucketTypes)) return false;
        if (!Utilities.equals(columnFont, other.columnFont)) return false;
        if (!Utilities.equals(columnTransformMap, other.columnTransformMap)) return false;
        if (!Utilities.equals(highlightedRowKeys, other.highlightedRowKeys)) return false;
        if (!Utilities.equals(highlightedRowColours, other.highlightedRowColours)) return false;
        if (!Utilities.equals(customCellFormats, other.customCellFormats)) return false;
        if (!Utilities.equals(showZeroAsBlankKeys, other.showZeroAsBlankKeys)) return false;
        if (!Utilities.equals(showNaNAsBlankKeys, other.showNaNAsBlankKeys)) return false;
        if (!Utilities.equals(selectAfterDecimalKeys, other.selectAfterDecimalKeys)) return false;
        if (!(highlightChanges == other.highlightChanges)) return false;
        if (!(highlightNewRows == other.highlightNewRows)) return false;
        if (!(columnColourHasPriority == other.columnColourHasPriority)) return false;
        if (!(fading == other.fading)) return false;
        if (!(showHedging == other.showHedging)) return false;
        if (!Utilities.equals(tableFont, other.tableFont)) return false;
        if (!Utilities.equals(customColumnNames, other.customColumnNames)) return false;
        if (!Utilities.equals(showFilterFindPanel, other.showFilterFindPanel)) return false;
        if (!Utilities.equals(includeSubstringsInFind, other.includeSubstringsInFind)) return false;
        if (!Utilities.equals(filterWithFind, other.filterWithFind)) return false;
        if (!Utilities.equals(formulaManager, other.formulaManager)) return false;
        if (!Utilities.equals(highlightConditionalSetMap, other.highlightConditionalSetMap)) return false;
        return true;
    }

    private boolean columnKeyMapEqual(Map thisColumnMap, Map otherColumnMap) {
        HashSet otherKeys = new HashSet( otherColumnMap.keySet() );
        for (Object o : thisColumnMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            if (!isPivotColumnIdentifier(key)) {
                Object value = entry.getValue();
                Object otherValue = otherColumnMap.get(key);
                if (!Utilities.equals(value, otherValue)) return false;
                otherKeys.remove(key);
            }
        }

        for (Object key : otherKeys) {
            if (!isPivotColumnIdentifier(key)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPivotColumnIdentifier(Object identifier) {
       return identifier instanceof PivotTableModel.GeneratedColumnKey;
    }


    private boolean tableColumnModelEqual(TableColumnModel thisModel, TableColumnModel other, List otherTableColumnList) {
        if (thisModel == null && other == null) return true;
        if (thisModel == null) return false;
        if (other == null) return false;

        if (!pivoted) { //since columns depend on the data, the column count could be different when pivoted
            if (!(thisModel.getColumnCount() == other.getColumnCount())) return false;
        }


        // Now see if the column widths, etc are equal
        for (int i = 0; i < thisModel.getColumnCount(); i++) {
            TableColumn thisColumn = null;
            TableColumn otherColumn = null;
            try {
                thisColumn = thisModel.getColumn(i);
                //ignore columns that seem to have been generated (their identifier is not in the column identifier list.
                if (isPivotColumn(thisColumn)) continue; // Ingore pivot data columns
            } catch (IllegalArgumentException e) {
            }
            try {
                if (i < other.getColumnCount()) {
                    otherColumn = other.getColumn(i);
                    if (isPivotColumn(otherColumn)) continue; // Ingore pivot data columns
                }
            } catch (IllegalArgumentException e) {
            }

            if (!tableColumnEqual(thisColumn, otherColumn)) return false;
        }

        return true;
    }

    private boolean isPivotColumn(TableColumn thisColumn) {
        return thisColumn != null && isPivotColumnIdentifier(thisColumn.getIdentifier());
    }

    private boolean tableColumnEqual(TableColumn t1, TableColumn t2) {
        if (t1 == t2) return true;
        if (t1 == null && t2 != null) return false;
        if (t1 != null && t2 == null) return false;

        if (!Utilities.equals(t1.getIdentifier(), t2.getIdentifier())) return false;
        if (!(t1.getWidth() == t2.getWidth())) return false;
        if (!(t1.getPreferredWidth() == t2.getPreferredWidth())) return false;

        return ColumnGroup.groupsAreEqualForPersistence(
            ((HidableTableColumn)t1).getColumnGroup(),
            ((HidableTableColumn)t2).getColumnGroup()
        );
    }

    public boolean isUsingDateSeries() {
        return usingDateSeries;
    }

    public void setUsingDateSeries(boolean usingDateSeries) {
        this.usingDateSeries = usingDateSeries;
    }

    public boolean isSplittingValues() {
        return splittingValues;
    }

    public void setSplittingValues(boolean splittingValues) {
        this.splittingValues = splittingValues;
    }

    public boolean isUseFullSeries() {
        return useFullSeries;
    }

    public void setUseFullSeries(boolean useFullSeries) {
        this.useFullSeries = useFullSeries;
    }

    public boolean isVerboseDateSeries() {
        return verboseDateSeries;
    }

    public void setVerboseDateSeries(boolean verboseDateSeries) {
        this.verboseDateSeries = verboseDateSeries;
    }

    public void setColumnColorMap(HashMap map) {
        this.columnColorMap = map;
    }

    public HashMap getColumnColorMap() {
        return columnColorMap;
    }

    public Map<String, HighlightConditionSet> getHighlightConditionalSetMap() {
        return highlightConditionalSetMap;
    }

    public void setHighlightConditionalSetMap(Map<String, HighlightConditionSet> highlightConditionalSetMap) {
        this.highlightConditionalSetMap = highlightConditionalSetMap;
    }

    public void setHighlightedRowKeys(HashSet highlightedRowKeys) {
        this.highlightedRowKeys = highlightedRowKeys;
    }

    public HashSet getHighlightedRowKeys() {
        return highlightedRowKeys;
    }

    public void setHighlightedRowColours(HashMap highlightedRowColours) {
        this.highlightedRowColours = highlightedRowColours;
    }

    public HashMap getHighlightedRowColours() {
        return highlightedRowColours;
    }

    public HashMap getCustomCellFormats() {
        return customCellFormats;
    }

    public void setCustomCellFormats(HashMap customCellFormats) {
        this.customCellFormats = customCellFormats;
    }

    public void setShowZeroAsBlankKeys(HashSet hashSet) {
        this.showZeroAsBlankKeys = hashSet;
    }

    public HashSet getShowZeroAsBlankKeys() {
        return showZeroAsBlankKeys;
    }

    public HashSet getShowNaNAsBlankKeys() {
        return showNaNAsBlankKeys;
    }

    public void setShowNaNAsBlankKeys(HashSet showNaNAsBlankKeys) {
        this.showNaNAsBlankKeys = showNaNAsBlankKeys;
    }

    public HashSet getSelectAfterDecimalKeys() {
        return selectAfterDecimalKeys;
    }

    public void setSelectAfterDecimalKeys(HashSet selectAfterDecimalKeys) {
        this.selectAfterDecimalKeys = selectAfterDecimalKeys;
    }

    public void setHighlightChanges(boolean animated) {
        this.highlightChanges = animated;
    }

    public boolean isHighlightChanges() {
        return highlightChanges;
    }

    public void setFading(boolean fading) {
        this.fading = fading;
    }

    public boolean isFading() {
        return fading;
    }

    public boolean isHighlightNewRows() {
        return highlightNewRows;
    }

    public void setHighlightNewRows(boolean highlightNewRows) {
        this.highlightNewRows = highlightNewRows;
    }

    public HashMap getColumnRendererIds() {
        return columnRendererIds;
    }

    public void setColumnRendererIds(HashMap columnRendererIds) {
        this.columnRendererIds = columnRendererIds;
    }

    public HashMap getColumnBucketTypes() {
        return columnBucketTypes;
    }

    public void setColumnBucketTypes(HashMap columnBucketTypes) {
        this.columnBucketTypes = columnBucketTypes;
    }

    public Font getTableFont() {
        return tableFont;
    }

    public void setTableFont(Font tableFont) {
        this.tableFont = tableFont;
    }

    public HashMap getColumnFont() {
        return columnFont;
    }

    public void setColumnFont(HashMap columnFont) {
        this.columnFont = columnFont;
    }

    public boolean isShowHedging() {
        return showHedging;
    }

    public void setShowHedging(boolean showHedging) {
        this.showHedging = showHedging;
    }

    public HashMap getColumnTransformMap() {
        return columnTransformMap;
    }

    public void setColumnTransformMap(HashMap columnTransformMap) {
        this.columnTransformMap = columnTransformMap;
    }

    public Set<String> getLockedPivotColumns() {
        return lockedPivotColumns;
    }

    public void setLockedPivotColumns(Set<String> columns) {
        this.lockedPivotColumns = columns;
    }

    private static class ColumnHidingColumnModelPersistenceDelegate extends BhavayaPersistenceDelegate {
        protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            ColumnHidingColumnModel cm = (ColumnHidingColumnModel) oldInstance;
            for ( HidableTableColumn tc : cm.getAllColumns()) {
                out.writeStatement(new Statement(cm, "addColumn", new Object[]{tc}));
            }
        }
    }

    public FormulaManager getFormulaManager() {
        return formulaManager;
    }

    public void setFormulaManager(FormulaManager formulaManager) {
        this.formulaManager = formulaManager;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
