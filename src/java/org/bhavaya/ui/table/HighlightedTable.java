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

import gnu.trove.TIntLongHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;
import org.bhavaya.ui.ToolTipFactory;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.VariableDelaySwingTask;
import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;
import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.TableUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Description
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.40.4.1 $
 */
public class HighlightedTable extends TrackViewportJTable {
    private static final Log log = Log.getCategory(HighlightedTable.class);

    private static final Color NEUTRAL_COLOR_CHANGE = new Color(54, 210, 255);
    private static final Color POSITIVE_COLOR_CHANGE = Color.GREEN;
    private static final Color NEGATIVE_COLOR_CHANGE = Color.RED;

    /**
     * Determines the behaviour on enter when on last row of the table (by default the selection moves on the first row)
     */
    private static boolean cycleTableOnEnter = true;

    private TIntObjectHashMap cellUpdateTimes = new TIntObjectHashMap();
    private TIntLongHashMap rowToInsertTime = new TIntLongHashMap();

    private int[] columnModelIdxToViewIdx = null;

    private CellHighlighter currentHighlighter;
    private CustomColumnHighlighter columnHighlighter;
    private CellHighlighter cellChangeHighlighter;

    private Color overrideBgColour = null;
    private static ScheduledExecutorService repaintExecutor = NamedExecutors.newSingleThreadScheduledExecutor("HighlightTable");
    private boolean loggingErrors = true;

    private boolean fading = true;
    private volatile boolean animated = true;
    private boolean highlightNewRows = false;

    private static final TableCellRenderer TEXT_RENDERER = new DefaultTableCellRenderer();

    private int maxDelay;
    private int minDelay;
    private boolean profileCellFading = false;
    private RowToInsertTimeFireEventProcedure rowToInsertTimeFireEventProcedure;

    private static boolean displayFullCellValueOnHover = false;
    private TableToolTipManager toolTipManager;
    private ToolTipFactory tableHeaderToolTipFactory;

    private boolean autoCreateColumnsOnInsert = false;

    private static final long FULL_REPAINT_DELAY = 3000;
    private long lastFullRepaint = System.currentTimeMillis();
    private VariableDelaySwingTask repaintTask = new VariableDelaySwingTask(500, repaintExecutor) {
        @Override
        protected void doRunOnEventThread() {
            performRepaint();
        }
    };

    public HighlightedTable(boolean animated) {
        this(null, animated);
    }

    public HighlightedTable(TableModel tableModel) {
        this(tableModel, true);
    }

    public HighlightedTable(TableModel tableModel, boolean animated) {
        super(tableModel);
        CellHighlighter underlyingHighlighter = new AlternateRowHighlighter();
        columnHighlighter = new CustomColumnHighlighter(underlyingHighlighter);
        CellHighlighter rowHighlighter = new NewRowHighlighter(columnHighlighter);
        cellChangeHighlighter = rowHighlighter;
        CellChangeHighlighter highlighter = new CellChangeHighlighter(rowHighlighter);
        setCurrentHighlighter(highlighter);

        maxDelay = 800;
        minDelay = 200;

        setAnimated(animated);
        repaintTask.start();

        getTableHeader().setDefaultRenderer(new MultiLineHeaderRenderer(getTableHeader().getDefaultRenderer()));
        rowToInsertTimeFireEventProcedure = new RowToInsertTimeFireEventProcedure();
        toolTipManager = new TableToolTipManager(this);
    }

    private void performRepaint() {
        try {
            if (!isShowing()) return;

            int delay = repaintTask.getDelay();

            if (delay < maxDelay && CpuLoad.getInstance().getLoad() > 0.50) {
                repaintTask.setDelay(delay + 100);
            }
            if (delay > minDelay && CpuLoad.getInstance().getLoad() > 0.30) {
                repaintTask.setDelay(delay - 100);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFullRepaint > FULL_REPAINT_DELAY) {
                // This is done so logic dependant highlights, e.g. where a cell highlight is dependant on the value
                // of another, are refreshed, i.e. decide if the highlight change
                lastFullRepaint = currentTime;
                repaint();
            } else if(isAnimated()){
                paintAllChanged();
            }
        } catch (Exception e) {
            log.error("Exception while painting updated cells", e);
        }
    }

    public void dispose() {
        setModel(new DefaultTableModel());

        repaintTask.stop(false);

        // All of this is superstituous garbage collection voodoo.  createDefaultRenderers will flush out
        // all of the assigned class->tableCellRenderer mappings.
        cellUpdateTimes.clear();
        cellUpdateTimes = null;
        rowToInsertTime.clear();
        rowToInsertTime = null;
        createDefaultRenderers();
    }

    public CustomColumnHighlighter getColumnHighlighter() {
        return columnHighlighter;
    }

    public void setOverrideBgColour(Color overrideBgColour) {
        this.overrideBgColour = overrideBgColour;
    }

    public final Color getBackground() {
        if (overrideBgColour != null) {
            return overrideBgColour;
        } else {
            return super.getBackground();
        }
    }

    /**
     * get a List of all the table columns in column model
     * If the column model is a ColumnHidingColumnModel this will include any columns currently hidden.
     */
    public List<? extends TableColumn> getAllTableColumns() {
        List<? extends TableColumn> result;
        TableColumnModel columnModel = getColumnModel();
        if ( columnModel instanceof ColumnHidingColumnModel ) {
            result = ((ColumnHidingColumnModel)getColumnModel()).getAllColumns();
        }  else {
            Enumeration<TableColumn> e = columnModel.getColumns();
            ArrayList<TableColumn> l = new ArrayList<TableColumn>();
            while(e.hasMoreElements())  {
                l.add(e.nextElement());
            }
            result = l;
        }
        return result;
    }

    /**
     * The call to getValueAt is very expensive cpu-wise due to all the reflection etc. - we don't want to
     * make that call more than once if we can help it.
     * This class is used by doPrepareRenderer to return the cell value in addition to renderer component,
     * so that subclass prepareRenderer() implementations don't have to re-invoke getValueAt()
     */
    protected static class RenderingComponentWithCellValue {

        private Object cellValue;
        private Component rendererComponent;

        public RenderingComponentWithCellValue(Object cellValue, Component rendererComponent) {
            this.cellValue = cellValue;
            this.rendererComponent = rendererComponent;
        }

        public Object getCellValue() {
            return cellValue;
        }

        public Component getRendererComponent() {
            return rendererComponent;
        }
    }

    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        return doPrepareRenderer(renderer, row, column).getRendererComponent();
    }

    protected RenderingComponentWithCellValue doPrepareRenderer(TableCellRenderer renderer, int row, int column) {
        boolean isSelected = isCellSelected(row, column);
        boolean rowIsAnchor = (selectionModel.getAnchorSelectionIndex() == row);
        boolean colIsAnchor = (columnModel.getSelectionModel().getAnchorSelectionIndex() == column);
        boolean hasFocus = (rowIsAnchor && colIsAnchor) && isFocusOwner();

        Component c;
        Object cellValue = null;
        try {
            Color overrideBgColour = getCurrentHighlighter().realGetHighlightForCell(row, column, this);
            setOverrideBgColour(overrideBgColour);

            cellValue = getValueAt(row, column);

            //some rather naughty cross-package dependent code. sorry. makes our lives a lot easier
            if (cellValue == CachedObjectGraph.DATA_NOT_READY) {
                c = TEXT_RENDERER.getTableCellRendererComponent(this, CachedObjectGraph.DATA_NOT_READY.toString(), isSelected, hasFocus, row, column);
                c.setForeground(Color.LIGHT_GRAY);
            } else if (cellValue == SortedTableModel.TotalBucket.TOTAL) {
                c = TEXT_RENDERER.getTableCellRendererComponent(this, SortedTableModel.TotalBucket.TOTAL.toString(), isSelected, hasFocus, row, column);
                c.setForeground(Color.GRAY);
            } else if (cellValue == GroupedRowManager.UnmergeableDataBucket.NA) {
                c = TEXT_RENDERER.getTableCellRendererComponent(this, GroupedRowManager.UnmergeableDataBucket.NA.toString(), isSelected, hasFocus, row, column);
                c.setForeground(Color.GRAY);
            } else {
                c = renderer.getTableCellRendererComponent(this, cellValue, isSelected, hasFocus, row, column);
            }
        } catch (Exception e) {
            // only log an error once, as it can cause too much logging
            String valueClassname = (cellValue != null ? ("instanceof " + cellValue.getClass()) : "");
            if (loggingErrors)
                log.error("Error during cell rendering. Could not render value: " + cellValue + " " + valueClassname, e);
            loggingErrors = false;
            c = TEXT_RENDERER.getTableCellRendererComponent(this, "Error loading data", isSelected, hasFocus, row, column);
            c.setForeground(Color.RED);
        }
        boolean cellEditable = false;
        if (hasFocus) {
            cellEditable = isCellEditable(row, column);
        }

        boolean editing = hasFocus && cellEditable;
        Color bgColor;
        if (editing) {
            bgColor = Color.white;
        } else {
            bgColor = getBackground();
            if (isSelected) bgColor = UIUtilities.multiply(getSelectionBackground(), bgColor);
        }
        c.setBackground(bgColor);
        setOverrideBgColour(null);
        return new RenderingComponentWithCellValue(cellValue, c);
    }

    protected void setCurrentHighlighter(CellHighlighter highlighter) {
        this.currentHighlighter = highlighter;
        //repaint all cells
        super.tableChanged(new TableModelEvent(getModel()));
    }

    /**
     * @return the topmost highlighter (i.e. the one with the highest priority) this will probably be the cell change highlighter
     */
    protected CellHighlighter getCurrentHighlighter() {
        return currentHighlighter;
    }

    public CellHighlighter getCustomHighlighter() {
        return cellChangeHighlighter.underlyingHighlighter;
    }

    public void setCustomHighlighter(CellHighlighter customHighlighter) {
        cellChangeHighlighter.setUnderlyingCellHighlighter(customHighlighter);

        //repaint all cells
        super.tableChanged(new TableModelEvent(getModel()));
    }

    public boolean isAutoCreateColumnsOnInsert() {
        return autoCreateColumnsOnInsert;
    }

    public void setAutoCreateColumnsOnInsert(boolean autoCreateColumns) {
        this.autoCreateColumnsOnInsert = autoCreateColumns;
    }

    private void paintAllChanged() {
        TableModelEvent event;

        boolean[] dirtyColumns = null;
        int[] minRows = null;
        int[] maxRows = null;
        Rectangle visibleRect;
        int firstVisibleRow = -1;
        int firstVisibleColumn = -1;
        int lastVisibleRow = -1;
        int lastVisibleColumn = -1;

        boolean useColumnStrategy = getModel() instanceof AnalyticsTableModel
                && !(((AnalyticsTableModel) getModel()).isGrouped() || ((AnalyticsTableModel) getModel()).isPivoted());

        if (useColumnStrategy && getParent() instanceof JViewport) {
            int columnCount = getColumnCount();
            dirtyColumns = new boolean[columnCount];
            minRows = new int[columnCount];
            maxRows = new int[columnCount];
            Arrays.fill(minRows, Integer.MAX_VALUE);
            JViewport parent = (JViewport) getParent();
            visibleRect = parent.getViewRect();
            Point p = visibleRect.getLocation();
            firstVisibleRow = rowAtPoint(p);
            firstVisibleColumn = columnAtPoint(p);
            p.translate(visibleRect.width, visibleRect.height);
            lastVisibleRow = rowAtPoint(p);
            lastVisibleColumn = columnAtPoint(p);

            firstVisibleRow = firstVisibleRow == -1 ? 0 : firstVisibleRow;
            firstVisibleColumn = firstVisibleColumn == -1 ? 0 : firstVisibleColumn;
            lastVisibleRow = lastVisibleRow == -1 ? getRowCount() - 1 : lastVisibleRow;
            lastVisibleColumn = lastVisibleColumn == -1 ? getColumnCount() - 1 : lastVisibleColumn;
        }

        Object[] colorCellChanges = cellUpdateTimes.getValues();
        for (int i = 0; i < colorCellChanges.length; i++) {
            ColorCellChange colorCellChange = (ColorCellChange) colorCellChanges[i];
            event = colorCellChange.getCause();
            int viewColumn = convertColumnIndexToView(event.getColumn());
            if (colorCellChange.hasExpired()) {
                cellUpdateTimes.remove(getCellId(event.getFirstRow(), viewColumn));
            }
            if (useColumnStrategy) {
                if (viewColumn >= firstVisibleColumn && viewColumn <= lastVisibleColumn) {
                    int viewRow = event.getFirstRow();
                    if (viewRow >= firstVisibleRow && viewRow <= lastVisibleRow) {
                        dirtyColumns[viewColumn] = true;
                        minRows[viewColumn] = Math.min(minRows[viewColumn], viewRow);
                        maxRows[viewColumn] = Math.max(maxRows[viewColumn], event.getFirstRow());
                        if (maxRows[viewColumn] > 8) {
                            System.currentTimeMillis();
                        }
                    }
                }
            } else {
                paintImmediately(getCellRect(event.getFirstRow(), viewColumn, false));
            }
        }

        if (useColumnStrategy) {
            for (int i = 0; i < dirtyColumns.length; i++) {
                if (dirtyColumns[i]) {
                    Rectangle firstCellRect = getCellRect(minRows[i], i, false);
                    Rectangle lastCellRect = getCellRect(maxRows[i], i, false);
                    paintImmediately(SwingUtilities.computeUnion(firstCellRect.x, firstCellRect.y, firstCellRect.width, firstCellRect.height, lastCellRect));
                }
            }
        }

        rowToInsertTime.forEachKey(rowToInsertTimeFireEventProcedure);
    }

    public int getFontSize() {
        return getFont().getSize();
    }

    public void setFontSize(int fontSize) {
        Font nextFont = getFont().deriveFont((float) fontSize);
        setFont(nextFont);
        setRowHeight(getFontMetrics(nextFont).getHeight());
        resizeAndRepaint();
    }

    public void setFont(Font font) {
        super.setFont(font);
        setRowHeight(getFontMetrics(font).getHeight());
        if (getCellEditor() != null) getCellEditor().cancelCellEditing();
    }

    public int convertColumnIndexToView(int modelColumnIndex) {
        if (columnModelIdxToViewIdx == null) {
            int tableModelColumnCount = getModel().getColumnCount();
            int columnModelColumnCount = columnModel.getColumnCount();
            columnModelIdxToViewIdx = new int[tableModelColumnCount];
            Arrays.fill(columnModelIdxToViewIdx, -1);
            for (int i = 0; i < columnModelColumnCount; i++) {
                columnModelIdxToViewIdx[columnModel.getColumn(i).getModelIndex()] = i;
            }
        }
        return columnModelIdxToViewIdx[modelColumnIndex];
    }

    public void columnMoved(TableColumnModelEvent e) {
        columnModelIdxToViewIdx = null;
        super.columnMoved(e);
    }

    public void columnAdded(TableColumnModelEvent e) {
        columnModelIdxToViewIdx = null;
        super.columnAdded(e);
    }

    public void columnRemoved(TableColumnModelEvent e) {
        columnModelIdxToViewIdx = null;
        super.columnRemoved(e);
    }

    private void registerCellChange(CellsInColumnUpdatedEvent e) {
        int modelCol = e.getColumn();
        if(modelCol <0){
           log.error("Column missing " + e.getColumn() + " for value " + e.getNewValue());
            return;
        }
        int viewCol = convertColumnIndexToView(modelCol);

        if (viewCol == -1) return;

        int rowCount = e.getRowCount();
        int row;
        for (int i = 0; i < rowCount; i++) {
            row = e.getRowIndex(i);
            int cellId = getCellId(row, viewCol);

            ColorCellChange change = (ColorCellChange) cellUpdateTimes.get(cellId);
            if (change == null) {
                if (fading) {
                    change = profileCellFading ? new ProfiledFadingCellChange(e, 2000) : new FadingCellChange(e, 2000);
                } else {
                    change = new ColorCellChange(e, 2000);
                }

                cellUpdateTimes.put(cellId, change);
            } else {
                change.reUseForEvent(e);
            }
        }
    }

    /**
     * specific handling of the extended use of TableModelEvent to notify of specific column insertions or removals
     * See BeanCollectionTableModel.removeColumnLocator
     */
    public void tableChanged(TableModelEvent e) {
        if (currentHighlighter != null) {
            currentHighlighter.tableChanged(e);
        }

        int row = e.getFirstRow();
        int modelIndex = e.getColumn();
        int type = e.getType();

// If the table changed structure (row == TableModelEvent.HEADER_ROW)
// find out how, and behave appropriately
        if (row == TableModelEvent.HEADER_ROW) {
//changes to the structure should blow away animation
            if (cellUpdateTimes != null) {
                cellUpdateTimes.clear();
            }


            if (type == TableModelEvent.INSERT) {
                updateColumnIndexesForInsert(modelIndex);

                if (autoCreateColumnsOnInsert) {
                    addColumn(createColumn(modelIndex));
                }
            } else if (type == TableModelEvent.DELETE) {
                //go through all the columns, updating their model index's appropriately
                TableColumn columnToDelete = updateColumnIndexesForDelete(modelIndex);
                if (columnToDelete != null) {
                    removeColumn(columnToDelete);
                }
            } else if (type == TableModelEvent.UPDATE && modelIndex >= 0) {
//column 'modelIndex' has been updated
                int size = getColumnModel().getColumnCount();
                if (log.isDebug()) log.debug("size = " + size);
                for (int i = 0; i < size; i++) {
                    TableColumn column = getColumnModel().getColumn(i);
                    if (column.getModelIndex() == modelIndex) {
                        String columnName = getModel().getColumnName(modelIndex);
                        if (getModel() instanceof CustomColumnNameModel) {
                            columnName = ((CustomColumnNameModel) getModel()).getCustomColumnName(modelIndex);
                        }
                        if (log.isDebug())
                            log.debug("Setting old header: " + column.getHeaderValue() + " to " + columnName);

                        column.setHeaderValue(columnName);
                        getTableHeader().resizeAndRepaint();
                        break;
                    }
                }
            } else {
//structure change
                super.tableChanged(e);
            }
//blow away cached model to view
            columnModelIdxToViewIdx = null;
        } else {
            if (!isAnimated() || type != TableModelEvent.UPDATE || CpuLoad.getInstance().getLoad() < 0.3) {
                if (e instanceof CellsInColumnUpdatedEvent) {
                    unrollColumnEvent((CellsInColumnUpdatedEvent) e);
                } else if (e instanceof MultipleColumnChangeEvent) {
                    CellsInColumnUpdatedEvent[] cellsInColumnUpdatedEvents = ((MultipleColumnChangeEvent) e).getColumnChanges();
                    for (int i = 0; i < cellsInColumnUpdatedEvents.length; i++) {
                        unrollColumnEvent(cellsInColumnUpdatedEvents[i]);
                    }
                } else {
                    super.tableChanged(e);
                }
            } else {
                //if the change is a real tableDataChanged event then allow it through since rows may have been added or removed
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS && e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE
                        && !(e instanceof MultipleColumnChangeEvent)) {
                    super.tableChanged(e);
                }
            }
            //To the observant: if the CPU load is >30% we do NOTHING for events where cells simply changed value. This is because we expect
            //periodic "paintAllChanged" method to display the changes
        }
    }

    //when a column is deleted, we need to update the index of all subsequent cols in the col model
    private TableColumn updateColumnIndexesForDelete(int modelIndex) {
        TableColumn columnToDelete = null;
        for (TableColumn column : getAllTableColumns()) {
            int columnModelIndex = column.getModelIndex();
            if (columnModelIndex > modelIndex) {
                column.setModelIndex(columnModelIndex - 1);
            } else if (columnModelIndex == modelIndex) {
                columnToDelete = column;
            }
        }
        return columnToDelete;
    }

    //when a column is inserted, we need to update the index of all subsequent cols in the col model
    private void updateColumnIndexesForInsert(int modelIndex) {
        for (TableColumn column : getAllTableColumns()) {
            int columnIndex = column.getModelIndex();
            if (columnIndex >= modelIndex) {
                column.setModelIndex(columnIndex + 1);
            }
        }
    }

    private void unrollColumnEvent(CellsInColumnUpdatedEvent cellsInColumnEvent) {
        if (cellsInColumnEvent.getRowCount() == 1) {
            super.tableChanged(cellsInColumnEvent);
        } else {
            int[] rows = cellsInColumnEvent.getRows();
            for (int i = 0; i < rows.length; i++) {
                super.tableChanged(new TableModelEvent((TableModel) cellsInColumnEvent.getSource(), rows[i], rows[i], cellsInColumnEvent.getColumn(), TableModelEvent.UPDATE));
            }
        }
    }

/* Workaround for JTableHeader bug #4292511.
   See: http://developer.java.sun.com/developer/bugParade/bugs/4292511.html */

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            private int preferredHeight = -1;

            private Component getHeaderRenderer(int columnIndex) {
                TableColumn aColumn = getColumnModel().getColumn(columnIndex);
                TableCellRenderer renderer = aColumn.getHeaderRenderer();
                if (renderer == null) {
                    renderer = getDefaultRenderer();
                }
                return renderer.getTableCellRendererComponent(getTable(),
                        aColumn.getHeaderValue(), false, false,
                        -1, columnIndex);
            }

            private int getPreferredHeight() {
                if (preferredHeight == -1) {
                    preferredHeight = 0;
                    TableColumnModel columnModel = getColumnModel();
                    for (int column = 0; column < columnModel.getColumnCount(); column++) {
                        Component comp = getHeaderRenderer(column);
                        int rendererHeight = comp.getPreferredSize().height;
                        preferredHeight = Math.max(preferredHeight, rendererHeight);
                    }
                }
                return preferredHeight;
            }

            public String getToolTipText(MouseEvent event) {
                if (tableHeaderToolTipFactory != null) {
                    return tableHeaderToolTipFactory.getToolTipText(event);
                } else {
                    return super.getToolTipText(event);
                }
            }

            public Point getToolTipLocation(MouseEvent event) {
                if (tableHeaderToolTipFactory != null) {
                    return tableHeaderToolTipFactory.getToolTipLocation(event);
                } else {
                    return super.getToolTipLocation(event);
                }
            }

            public JToolTip createToolTip() {
                if (tableHeaderToolTipFactory != null) {
                    return tableHeaderToolTipFactory.createToolTip();
                } else {
                    return super.createToolTip();
                }
            }

            public void doLayout() {
                preferredHeight = -1;
                super.doLayout();
            }

            public void resizeAndRepaint() {
                preferredHeight = -1;
                super.resizeAndRepaint();
            }

            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, getPreferredHeight());
            }
        };
    }

//-----------------------------------------------------------------

    public static abstract class CellHighlighter implements TableModelListener {
        protected static final Color ALTERNATE_ROW_HIGHLIGHT_COLOR = new Color(240, 240, 240);
        private CellHighlighter underlyingHighlighter = null;
        private boolean isEnabled = true;

        public CellHighlighter() {
        }

        public CellHighlighter(CellHighlighter underlyingCellHighlighter) {
            this.underlyingHighlighter = underlyingCellHighlighter;
        }

        public void setUnderlyingCellHighlighter(CellHighlighter cellHighlighter) {
            this.underlyingHighlighter = cellHighlighter;
        }

        public CellHighlighter getUnderlyingHighlighter() {
            return underlyingHighlighter;
        }

        //uses the enabled flag to decide whether to skip this highlighter in the chain
        //subclasses should usually override getHighlightForCell to implement actual highlighting
        public Color realGetHighlightForCell(int row, int viewCol, HighlightedTable table) {
            if ( isEnabled) {
                return getHighlightForCell(row, viewCol, table);
            }   else {
                return underlyingHighlighter != null ?
                    underlyingHighlighter.realGetHighlightForCell(row, viewCol, table) :
                    null;
            }
        }

        protected Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            if (underlyingHighlighter != null) {
                return underlyingHighlighter.realGetHighlightForCell(row, viewCol, table);
            } else {
                return null;
            }
        }

        //uses the enabled flag to decide whether to skip this highlighter in the chain
        //subclasses should instead override doTableChanged in most cases
        public void tableChanged(TableModelEvent e) {
            if ( isEnabled) {
                doTableChanged(e);
            }   else {
                if (underlyingHighlighter != null ) underlyingHighlighter.tableChanged(e);
            }
        }

        protected void doTableChanged(TableModelEvent e) {
            if (underlyingHighlighter != null) {
                underlyingHighlighter.tableChanged(e);
            }
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public void setEnabled(boolean enabled) {
            isEnabled = enabled;
        }
    }

    public static class AlternateRowHighlighter extends CellHighlighter {
        private Color highlight;

        public AlternateRowHighlighter() {
            this(ALTERNATE_ROW_HIGHLIGHT_COLOR);
        }

        public AlternateRowHighlighter(Color highlight) {
            this.highlight = highlight;
        }

        public Color getHighlightForCell(int row, int col, HighlightedTable table) {
            if ((row % 2) == 0) {
                return highlight;
            }
            return null;
        }
    }

    public class CustomColumnHighlighter extends CellHighlighter {
        private HashMap columnToColour = new HashMap();

        public CustomColumnHighlighter(CellHighlighter underlying) {
            super(underlying);
        }

        public Color getColumnColor(Object columnKey) {
            return (Color) columnToColour.get(columnKey);
        }

        public void setColumnColor(Object columnIdentifier, Color color) {
            if (color != null)
                columnToColour.put(columnIdentifier, color);
            else
                columnToColour.remove(columnIdentifier);

            int viewIndex;
            try {
                viewIndex = getColumnModel().getColumnIndex(columnIdentifier);
                int modelindex = getColumnModel().getColumn(viewIndex).getModelIndex();
                HighlightedTable.super.tableChanged(new TableModelEvent(getModel(), 0, Integer.MAX_VALUE, modelindex));
            } catch (Exception e) {
                // Using an exception in an unexceptional circumstance.  But there is no hasColumn(identifier) so we live
                // with this
                if (log.isDebug()) log.debug("Could not find column with id: " + columnIdentifier);
            }
        }

        public Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            Object columnId = getColumnModel().getColumn(viewCol).getIdentifier();
            Color color = (Color) columnToColour.get(columnId);
            Color background = super.getHighlightForCell(row, viewCol, table);
            if (color == null)
                return background;
            else {
                if (background == null) return color;
                return UIUtilities.multiply(background, color);
            }
        }

        public HashMap getColorMap() {
            return columnToColour;
        }

        public void setColorMap(HashMap map) {
            columnToColour = map;
        }
    }

    private class CellChangeHighlighter extends CellHighlighter {
        public CellChangeHighlighter(CellHighlighter underlyingHighlighter) {
            super(underlyingHighlighter);
        }

        public Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            if (isAnimated()) {
                int cellId = getCellId(row, viewCol);

                ColorCellChange change = (ColorCellChange) cellUpdateTimes.get(cellId);
                if (change != null) {
                    // Check that the view column is as expected
                    int modelCol = change.getCause().getColumn();
                    int changeViewCol = convertColumnIndexToView(modelCol);
                    if (changeViewCol == viewCol) {
                        Color originalBg = super.getHighlightForCell(row, viewCol, table);
                        if (change.hasExpired()) {
                            cellUpdateTimes.remove(cellId);
                            return originalBg;
                        }
                        if (originalBg == null) {
                            originalBg = HighlightedTable.super.getBackground();
                        }
                        return change.getColor(originalBg);
                    } else {
                        // The view column of the original change has moved (thus invalidating the CellId)
                        cellUpdateTimes.remove(cellId);
                        cellUpdateTimes.put(getCellId(row, changeViewCol), change);
                    }
                }
            }
            return super.getHighlightForCell(row, viewCol, table);
        }

        public void doTableChanged(TableModelEvent e) {
            if (isAnimated()) {
                int row = e.getFirstRow();
                int type = e.getType();


                if (row != TableModelEvent.HEADER_ROW && type == TableModelEvent.UPDATE) {
                    if (e instanceof CellsInColumnUpdatedEvent) {
                        registerCellChange((CellsInColumnUpdatedEvent) e);
                    } else if (e instanceof MultipleColumnChangeEvent) {
                        CellsInColumnUpdatedEvent[] columnChanges = ((MultipleColumnChangeEvent) e).getColumnChanges();
                        for (int i = 0; i < columnChanges.length; i++) {
                            registerCellChange(columnChanges[i]);
                        }
                    }
                }
            }
            super.doTableChanged(e);
        }
    }

    private class NewRowHighlighter extends CellHighlighter {
        private Color newRowColour = new Color(193, 183, 251);

        public NewRowHighlighter(CellHighlighter underlyingHighlighter) {
            super(underlyingHighlighter);
        }

        public void doTableChanged(TableModelEvent e) {
            if (isAnimated() && isHighlightNewRows()) {
                int firstRow = e.getFirstRow();
                int lastRow = e.getLastRow();

                // If the table changed structure (row == TableModelEvent.HEADER_ROW)
                if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == TableModelEvent.ALL_COLUMNS) { //all data changed
                    if (firstRow == TableModelEvent.HEADER_ROW || (firstRow == 0 && lastRow == Integer.MAX_VALUE)) {
                        //either all data changed, or full structure change
                        rowToInsertTime.clear();
                    }
                } else
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS && firstRow >= 0 && lastRow < Integer.MAX_VALUE) {
                    //changed a row of data

                    //update the row insert indexes and times
                    if (e.getType() == TableModelEvent.INSERT) {
                        //a single row was inserted
                        int[] rows = rowToInsertTime.keys();
                        for (int i = 0; i < rows.length; i++) {
                            int row = rows[i];
                            int distanceFromInsertPoint = (row - firstRow);
                            if (distanceFromInsertPoint >= 0) {
                                long oldTime = rowToInsertTime.remove(row);
                                rowToInsertTime.put(lastRow + 1 + distanceFromInsertPoint, oldTime);
                            }
                        }
                        for (int i = firstRow; i < lastRow + 1; i++) {
                            rowToInsertTime.put(i, System.currentTimeMillis());
                        }
                    } else if (e.getType() == TableModelEvent.DELETE) {
                        //a single row was deleted
                        int[] rows = rowToInsertTime.keys();
                        for (int i = 0; i < rows.length; i++) {
                            int row = rows[i];
                            int distanceFromLast = row - lastRow;
                            if (distanceFromLast >= 0) {
                                long oldTime = rowToInsertTime.remove(row);
                                rowToInsertTime.put(firstRow + distanceFromLast, oldTime);
                            }
                        }
                        for (int i = firstRow; i < lastRow + 1; i++) {
                            rowToInsertTime.remove(i);
                        }
                    }
                }
            }
            super.doTableChanged(e);
        }

        public Color getHighlightForCell(int row, int col, HighlightedTable table) {
            if (isAnimated() && isHighlightNewRows()) {
                boolean containsTime = rowToInsertTime.containsKey(row);
                long time = rowToInsertTime.get(row);

                if (containsTime) {
                    Color originalBg = super.getHighlightForCell(row, col, table);
                    long timeSinceInsert = System.currentTimeMillis() - time;
                    if (timeSinceInsert > 2000) {
                        rowToInsertTime.remove(row);
                        return originalBg;
                    }
                    if (originalBg == null) {
                        originalBg = HighlightedTable.super.getBackground();
                    }
                    return getColourForTime(newRowColour, originalBg, timeSinceInsert);
                }
            }

            //use default
            return super.getHighlightForCell(row, col, table);
        }

        private Color getColourForTime(Color start, Color currentBg, long time) {
            float t = (float) time / 2000.0f;

            return UIUtilities.blend(start, currentBg, 1.0f - t);
        }
    }


    private static int getCellId(int row, int viewCol) {
        return (row * 1000) + viewCol;
    }

    private static class ProfiledFadingCellChange extends FadingCellChange {
        private boolean firstCall = true;

        public ProfiledFadingCellChange(CellsInColumnUpdatedEvent e, long durationMillis) {
            super(e, durationMillis);
        }

        public Color getColor(Color currentBg) {
            long t = System.currentTimeMillis() - time;
            if (firstCall) {
                Object propertyChange = cause.getEventCause();
                if (propertyChange instanceof TimedPathPropertyChangeEvent) {
                    long causeTime = ((TimedPathPropertyChangeEvent) propertyChange).getTime();
                    if (log.isDebug())
                        log.debug("bean->screen time=" + (System.currentTimeMillis() - causeTime) + " table->screen = " + t);
                }
                firstCall = false;
            }

            return super.getColor(currentBg);
        }

        public void reUseForEvent(CellsInColumnUpdatedEvent e) {
            firstCall = true;
            super.reUseForEvent(e);
        }
    }

    private static class FadingCellChange extends ColorCellChange {
        private FadingCellChange(CellsInColumnUpdatedEvent e, long durationMillis) {
            super(e, durationMillis);
        }

        private float limit(float x, float min, float max) {
            return Math.max(min, Math.min(max, x));
        }

        private float log10(float x) {
            return (float) (Math.log(x) / Math.log(10));
        }

        public Color getColor(Color currentBg) {
            long t = System.currentTimeMillis() - time;
            if (!hasMagnitude(cause.getNewValue())) {
                return UIUtilities.blend(NEUTRAL_COLOR_CHANGE, currentBg, limit((1 - ((float) t / (float) duration)), 0f, 1f));
            }

            double nv = doubleValue(cause.getNewValue());
            double ov = doubleValue(cause.getOldValue());
            if (Double.isNaN(nv) && Double.isNaN(ov)) {
                return currentBg;
            }

            // Work out the delta as a percentage.
            double r = Math.abs(nv / ov - 1d) * 100d;

            // If NAN then just return the same colour.  Don't know if this is right.
            if (Double.isNaN(r)) {
                return UIUtilities.blend(NEUTRAL_COLOR_CHANGE, currentBg, limit((1 - ((float) t / (float) duration)), 0f, 1f));
            }

            float a = 0.0f;

            if (t <= duration) {
                a = log10((float) r); // deltas of a 10% or more will be rendered in full colour.
                a = limit(limit(a, 0.5f, 1.0f) * (1 - ((float) t / (float) duration)), 0.0f, 1.0f);
            }

            Color start = (nv < ov) ? NEGATIVE_COLOR_CHANGE : POSITIVE_COLOR_CHANGE;
            return UIUtilities.blend(start, currentBg, a);
        }
    }

    private static class ColorCellChange {
        protected long time;
        protected CellsInColumnUpdatedEvent cause;
        protected long duration;

        private ColorCellChange(CellsInColumnUpdatedEvent e, long durationMillis) {
            this.time = System.currentTimeMillis();
            this.cause = e;
            this.duration = durationMillis;
        }

        public long getTime() {
            return time;
        }

        public CellsInColumnUpdatedEvent getCause() {
            return cause;
        }

        public long getDuration() {
            return duration;
        }

        public void reUseForEvent(CellsInColumnUpdatedEvent e) {
            this.time = System.currentTimeMillis();
            this.cause = e;
        }

        public Color getColor(Color currentBg) {
            if (!hasMagnitude(cause.getNewValue())) {
                return NEUTRAL_COLOR_CHANGE;
            }

            double nv = doubleValue(cause.getNewValue());
            double ov = doubleValue(cause.getOldValue());
            if (Double.isNaN(nv) && Double.isNaN(ov)) {
                return currentBg;
            }

            // If NAN then just return the same colour.  Don't know if this is right.
            if (Double.isNaN(Math.abs(nv / ov - 1d) * 100d)) {
                return NEUTRAL_COLOR_CHANGE;
            }

            return (nv < ov) ? NEGATIVE_COLOR_CHANGE : POSITIVE_COLOR_CHANGE;
        }

        public boolean hasExpired() {
            long t = System.currentTimeMillis() - time;
            return t > duration;
        }

        protected static double doubleValue(Object value) {
            // will leave this one in as it uses amount and not scaled amount
            if (value instanceof ScalableNumber) {
                return ((ScalableNumber) value).getAmount();
            }
            if (value instanceof Numeric) {
                return ((Numeric)value).doubleValue();
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof Date) {
                return (double) ((Date) value).getTime();
            }
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue() ? 1.0 : 0.0;
            }
            if (value instanceof PartialBucketValue) {
                return ((Number) ((PartialBucketValue) value).getPartialValue()).doubleValue();
            }
            return Double.NaN;
        }

        protected static boolean hasMagnitude(Object value) {
            return value instanceof Numeric
                    || value instanceof Number
                    || value instanceof Date
                    || value instanceof Boolean
                    || value instanceof PartialBucketValue;
        }
    }

    public String getToolTipText(MouseEvent event) {
        assert super.getToolTipText(event) == null : "Tooltips are not supported in the table as they are used to display full content of the table cell.";
        if (displayFullCellValueOnHover) {
            return toolTipManager.getToolTipText(event);
        } else {
            return null;
        }
    }

    public Point getToolTipLocation(MouseEvent event) {
        if (displayFullCellValueOnHover) {
            return toolTipManager.getToolTipLocation(event);
        } else {
            return null;
        }
    }

    public JToolTip createToolTip() {
        return toolTipManager.createToolTip();
    }

    public void setTableHeaderToolTipFactory(ToolTipFactory tableHeaderToolTipFactory) {
        this.tableHeaderToolTipFactory = tableHeaderToolTipFactory;
    }

    public boolean isAnimated() {
        return animated;
    }

    public boolean isFading() {
        return fading;
    }

    public void setProfileCellFading(boolean profile) {
        this.profileCellFading = profile;
    }

    public void setFading(boolean fading) {
        this.fading = fading;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
        if (animated) {
            rowToInsertTime.clear();
            cellUpdateTimes.clear();
        }
    }

    public boolean isHighlightNewRows() {
        return highlightNewRows;
    }

    public void setHighlightNewRows(boolean highlightNewRows) {
        this.highlightNewRows = highlightNewRows;
    }

    protected TableColumn createColumn(int index) {
        return new HidableTableColumn(index, 100);
    }

    private class RowToInsertTimeFireEventProcedure implements TIntProcedure {
        public boolean execute(int row) {
            HighlightedTable.super.tableChanged(new TableModelEvent(getModel(), row));
            return true;
        }
    }

    public void setUI(TableUI ui) {
        super.setUI(ui);
        ActionMap actionMap = getActionMap();
        Action tmpAction = actionMap.get("selectNextRowCell");
        actionMap.put("selectNextRowCell", new OnEnterAction(tmpAction, 1));
        tmpAction = actionMap.get("selectPreviousRowCell");
        actionMap.put("selectPreviousRowCell", new OnEnterAction(tmpAction, -1));
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke("TAB"), "selectNextColumnCell");
        inputMap.put(KeyStroke.getKeyStroke("shift TAB"), "selectPreviousColumnCell");
    }

    public static void setDisplayFullCellValueOnHover(boolean b) {
        displayFullCellValueOnHover = b;
    }

    public static boolean isDisplayFullCellValueOnHover() {
        return displayFullCellValueOnHover;
    }

    public static boolean isCycleTableOnEnter() {
        return cycleTableOnEnter;
    }

    public static void setCycleTableOnEnter(boolean cycleTableOnEnter) {
        HighlightedTable.cycleTableOnEnter = cycleTableOnEnter;
    }

    /**
     * On enter action that changes the default JTable behaviour so that the cell selection doesn't cycle
     * arround the table/selection. (Hitting return at end of table shouldn't return to the top -
     * Same applies to Shift+Return on first row).
     *
     * @author Vladimir Hrmo
     */
    private static class OnEnterAction extends AbstractAction {
        /**
         * The original action used to preserve original behaviour
         */
        protected Action originalAction;
        protected int dy;

        protected OnEnterAction(Action originalAction, int dy) {
            this.dy = dy;
            this.originalAction = originalAction;
        }

        private int restrict(int x, int min, int max) {
            return (x < min) ? min :
                    (x >= max) ? max - 1 :
                            x;
        }

        public void actionPerformed(ActionEvent e) {
            if (cycleTableOnEnter && originalAction != null) {
                // dispatch to original action
                originalAction.actionPerformed(e);
            } else {
                performModifiedAction(e);
            }
        }

        private void performModifiedAction(ActionEvent e) {
            JTable table = (JTable) e.getSource();
            ListSelectionModel rsm = table.getSelectionModel();
            int anchorRow = rsm.getAnchorSelectionIndex();

            if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                return;
            }

            boolean canStayInSelection = (rsm.getMaxSelectionIndex() - rsm.getMinSelectionIndex() + 1) > 1;
            if (canStayInSelection) {
                // stay within current selection
                anchorRow = restrict(anchorRow + dy, rsm.getMinSelectionIndex(), rsm.getMaxSelectionIndex() + 1);
                table.changeSelection(anchorRow, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex(), true, true);
            } else {
                // stay within table boundaries
                anchorRow = restrict(anchorRow + dy, 0, table.getRowCount());
                table.changeSelection(anchorRow, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex(), false, false);
            }
        }
    }
}