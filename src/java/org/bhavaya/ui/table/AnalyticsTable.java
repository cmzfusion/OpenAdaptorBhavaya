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

import com.od.filtertable.TableCellFinder;
import gnu.trove.TIntHashSet;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.*;
import org.bhavaya.ui.table.column.model.ObservableWidthColumnModel;
import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;
import org.bhavaya.ui.view.ActionGroup;
import org.bhavaya.ui.view.FilterTablePanelModel;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.text.Format;
import java.util.*;
import java.util.List;


/**
 * A table that displays an AnalyticsTableModel.
 * It makes use of KeyedColumnTableModel to improve the "createDefaultColumnsFromModel" method.
 * It links in a mouse handler to handle actions on beans, setting column precisions, etc
 * It replaces the header renderer with one more suited to a sorted table (i.e. show the sort columns as little arrows)
 * It allows persistance of table model and view configurations.
 * <p/>
 * A little note about column renderers:
 * in order to persist view configuration
 * <p/>
 * 14-10-03 Oh yuk, this class is dirty. Time for a spring clean I think. Configs are the main cause of mess.
 *
 * @author Brendon McLean
 * @author Daniel van Enckevort
 * @version $Revision: 1.58.4.4 $
 */
public class AnalyticsTable extends FixedColumnTable implements TabularBeanAssociation {
    private static final Log log = Log.getCategory(AnalyticsTable.class);

    private static final String COMPACT_TABLE_ACTION_KEY = "CompactTable";

    //a default column config factory
    private static ColumnConfigFactory NULL_COL_CONFIG_FACTORY = new ColumnConfigFactory() {
        public ColumnConfig getColumnConfigForColumn(Object columnKey) {
            return null;
        }
    };

    //give the column updaters a way of adding and configuring columns
    private AbstractAnalyticsTableColumnModelUpdater.ColumnCreator columnCreator = new AbstractAnalyticsTableColumnModelUpdater.ColumnCreator() {
        public void configureColumnAndAddToModel(TableModel tableModel, TableColumnModel tableColumnModel, TableColumn column) {
            addAndConfigureColumn(tableModel, tableColumnModel, column);
        }

        public TableColumn createTableColumn(int modelIndex) {
            return AnalyticsTable.this.createTableColumn(modelIndex);
        }
    };

    private List<TableCellEditListener> tableCellEditListeners = new ArrayList<TableCellEditListener>();

    public void addTableCellEditorListener(TableCellEditListener listener){
        tableCellEditListeners.add(listener);
    }
    public void removeTableCellEditorListener(TableCellEditListener listener){
        tableCellEditListeners.remove(listener);
    }
    private class TableCellEditEvent{
        JTable table;
        int row;
        int column;
        Object oldValue;
        int selectedRows[];

        TableCellEditEvent(JTable table,
                           int row,
                           int column,
                           int selectedRows[],
                           Object oldValue) {
            this.table = table;
            this.row = row;
            this.column = column;
            this.selectedRows = selectedRows;
            this.oldValue = oldValue;
        }
    }


    private TableCellEditEvent tableCellEditEvent;
    private CopyPasteRowHighlighter scrollableCopyPasteRowHighlighter;
    private CopyPasteRowHighlighter fixedCopyPasteRowHighlighter;


    private static Map renderers = new HashMap();
    private boolean constructed = false;

    private CustomRowHighlighter scrollableRowHighlighter;
    private CustomRowHighlighter fixedRowHighlighter;
    private HashSet showZeroAsBlankColumnKeys = new HashSet();
    private HashSet showNaNAsBlankColumnKeys = new HashSet();
    private HashSet selectAfterDecimalColumnKeys = new HashSet();
    private HashMap columnFonts;
    private GrouperBucketFactory grouperBucketFactory;

    private ColumnConfigFactory columnConfigFactory = NULL_COL_CONFIG_FACTORY;
    private HashMap<Object, String> columnRendererIds;

    private BeanActionFactory beanActionFactory;

    protected CustomHighlightedTable fixedTable;
    protected CustomHighlightedTable scrollableTable;

    private ConditionalHighlighter conditionalHighlighter;

    private TableCellFinder tableCellFinder;
    private FilterTablePanelModel filterModel = new FilterTablePanelModel();

    private int defaultRowHeight;
    private Set rowsToRecalHeight = new HashSet();

    private boolean columnColourHasPriority = Workspace.getInstance().isColumnColourHasPriority();
    private AnalyticsTableSelection selection;

    /**
     * provides a default mechanism to back instance calls of getColumnRendererForId and setColumnRendererForId
     *
     * @param rendererId
     * @param renderer
     * @deprecated Is this used at all?  Brendon9x
     */
    public static void addRenderer(String rendererId, TableCellRenderer renderer) {
        renderers.put(rendererId, renderer);
    }

    /**
     * provides a default mechanism to back instance calls of getColumnRendererForId and setColumnRendererForId
     *
     * @param rendererId
     * @deprecated I'm deprecating this because I'm not sure its needed any more.  Let me know if it is (brendon9x)
     */
    public static TableCellRenderer getRenderer(String rendererId) {
        return (TableCellRenderer) renderers.get(rendererId);
    }


    public AnalyticsTable(KeyedColumnTableModel tableModel, boolean animated) {
        super(new AnalyticsTableModel(tableModel));

        fixedTable = (CustomHighlightedTable) super.getFixedTable();
        scrollableTable = (CustomHighlightedTable) super.getScrollableTable();

        FilterFindCellHighlighter scrollableFilterFindHighlighter = new FilterFindCellHighlighter(scrollableTable.getCurrentHighlighter());
        FilterFindCellHighlighter fixedFilterFindHighlighter = new FilterFindCellHighlighter(fixedTable.getCurrentHighlighter());
        scrollableTable.setCurrentHighlighter(scrollableFilterFindHighlighter);
        fixedTable.setCurrentHighlighter(fixedFilterFindHighlighter);

        scrollableRowHighlighter = new CustomRowHighlighter(scrollableTable.getCustomHighlighter());
        fixedRowHighlighter = new CustomRowHighlighter(fixedTable.getCustomHighlighter());

        scrollableTable.setCustomHighlighter(scrollableRowHighlighter);
        fixedTable.setCustomHighlighter(fixedRowHighlighter);

        AnalyticsTableModel analyticsModel = (AnalyticsTableModel) getModel();
        setupHeaderRenderers(analyticsModel);

        tableCellFinder = new TableCellFinder(new AnalyticsColumnSource(), analyticsModel);
        selection = new AnalyticsTableSelection(this, analyticsModel);

        scrollableTable.getTableHeader().addMouseListener(getScrollToRowTableSortListener());
        fixedTable.getTableHeader().addMouseListener(getScrollToRowTableSortListener());

        GroupedRowManager.BucketFactory defaultGrouperBucketFactory = analyticsModel.getTableGrouper().getBucketFactory();
        grouperBucketFactory = new GrouperBucketFactory(defaultGrouperBucketFactory);
        analyticsModel.getTableGrouper().setBucketFactory(grouperBucketFactory);

        analyticsModel.getTableGrouper().setGroupedKeyDefinition(grouperBucketFactory);
        analyticsModel.getTablePivoter().setSuitableDataColumnFilter(new GrouperAwareDataColumnFilter());

        fixedTable.setTableHeaderToolTipFactory(new AnalyticsTablePropertyToolTipFactory(fixedTable));
        scrollableTable.setTableHeaderToolTipFactory(new AnalyticsTablePropertyToolTipFactory(scrollableTable));

        scrollableTable.setAnimated(animated);
        fixedTable.setAnimated(animated);

        installKeyBindings();
        defaultRowHeight = fixedTable.getRowHeight();

        scrollableCopyPasteRowHighlighter = new CopyPasteRowHighlighter(scrollableFilterFindHighlighter);
        scrollableTable.setCurrentHighlighter(scrollableCopyPasteRowHighlighter);

        fixedCopyPasteRowHighlighter = new CopyPasteRowHighlighter(fixedFilterFindHighlighter);
        fixedTable.setCurrentHighlighter(fixedCopyPasteRowHighlighter);

        constructed = true;
    }

    //as well as changing the sort on the sortable model, we want to scroll back to the user's current selections
    private MouseListener getScrollToRowTableSortListener() {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                getAnalyticsTableModel().getClickSortMouseHandler().mouseClicked(e);
                scrollToFirstSelectedCell();
            }
        };
    }

    public void showHighlighterChainEditorForFixedTable() {
        new HighlighterChainEditor(fixedTable, fixedTable.getCurrentHighlighter()).setVisible(true);
    }

    public void showHighlighterChainEditorForScrollableTable() {
        new HighlighterChainEditor(scrollableTable, scrollableTable.getCurrentHighlighter()).setVisible(true);
    }

    private void setupHeaderRenderers(AnalyticsTableModel analyticsModel) {
        scrollableTable.getTableHeader().setDefaultRenderer(new PivotAwareTableCellRenderer(
                    analyticsModel, analyticsModel.getTableHeaderRenderer(scrollableTable.getTableHeader()
            )
        ));
        fixedTable.getTableHeader().setDefaultRenderer(new FixedTableHeaderCellRenderer(
                                          analyticsModel.getTableHeaderRenderer(fixedTable.getTableHeader())));
    }

    protected void installKeyBindings() {
        Action compactTableAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TableUtilities.initColumnSizes(fixedTable, 0, fixedTable.getRowCount());
                TableUtilities.initColumnSizes(scrollableTable, 0, scrollableTable.getRowCount());
                adjustFixedTableSize();
            }
        };
        scrollableTable.getActionMap().put(COMPACT_TABLE_ACTION_KEY, compactTableAction);
        scrollableTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), COMPACT_TABLE_ACTION_KEY);
        fixedTable.getActionMap().put(COMPACT_TABLE_ACTION_KEY, compactTableAction);
        fixedTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), COMPACT_TABLE_ACTION_KEY);
    }

    protected FixedForCustomRowHeightsJTable createFixedTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        CustomHighlightedTable table = new CustomHighlightedTable(tableModel);
        table.setColumnModel(tableColumnModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateColumnsOnInsert(false);
        return table;
    }

    protected FixedForCustomRowHeightsJTable createScrollableTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        CustomHighlightedTable table = new CustomHighlightedTable(tableModel);
        table.setColumnModel(tableColumnModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateColumnsOnInsert(true);
        return table;
    }

    public void setBeanActionFactory(BeanActionFactory beanActionFactory) {
        if (this.beanActionFactory != null) {
            unregisterAcceleratorActions(this.beanActionFactory);
        }
        this.beanActionFactory = beanActionFactory;

        if (beanActionFactory != null) {
            registerAcceleratorActions(beanActionFactory);
        }

        fixedTable.setBeanActionFactory(beanActionFactory);
        scrollableTable.setBeanActionFactory(beanActionFactory);
    }

    private void unregisterAcceleratorActions(BeanActionFactory beanActionFactory) {
        AcceleratorAction[] acceleratorActions = beanActionFactory.getAcceleratorActions();
        if (acceleratorActions != null) {
            ActionMap actionMap = getActionMap();
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            for (int i = 0; i < acceleratorActions.length; i++) {
                AcceleratorAction acceleratorAction = acceleratorActions[i];
                actionMap.remove(acceleratorAction.getActionName());
                inputMap.remove(acceleratorAction.getKeyStroke());
            }
        }
    }

    private void registerAcceleratorActions(BeanActionFactory beanActionFactory) {
        AcceleratorAction[] acceleratorActions = beanActionFactory.getAcceleratorActions();
        if (acceleratorActions != null) {
            ActionMap actionMap = getActionMap();
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            for (int i = 0; i < acceleratorActions.length; i++) {
                AcceleratorAction acceleratorAction = acceleratorActions[i];
                AnalyticsTableAcceleratorAction action = new AnalyticsTableAcceleratorAction(acceleratorAction);
                actionMap.put(acceleratorAction.getActionName(), action);
                inputMap.put(acceleratorAction.getKeyStroke(), acceleratorAction.getActionName());
            }
        }
    }

    public Color getRowHighlightColor(Object bean) {
        return scrollableRowHighlighter.getColorForBean(bean);
    }

    public void setRowHighlightColor(Object bean, Color highlighColor) {
        scrollableRowHighlighter.addBean(bean, highlighColor);
        fixedRowHighlighter.addBean(bean, highlighColor);
    }

     public void setCustomRowHeight(int row, int newHeight) {
        int currentRowHeight = fixedTable.getRowHeight(row);
        if (currentRowHeight != newHeight) {
            fixedTable.setRowHeight(row, newHeight);
            scrollableTable.setRowHeight(row, newHeight);
        }
    }

    public Color getCellHighlightColor(Object beanKey, Object beanPath) {
        Color customCellColour = fixedRowHighlighter.getCustomCellColour(beanKey, beanPath);
        if (customCellColour == null) {
            customCellColour = scrollableRowHighlighter.getCustomCellColour(beanKey, beanPath);
        }
        return customCellColour;
    }

    public void setCellHighlightColor(Object beanKey, Object beanPath, Color color) {
        scrollableRowHighlighter.addCustomCellColour(beanKey, beanPath, color);
        fixedRowHighlighter.addCustomCellColour(beanKey, beanPath, color);
    }

    public Font getCellHighlightFont(Object beanKey, Object beanPath) {
        Font customCellFont = fixedRowHighlighter.getCustomCellFont(beanKey, beanPath);
        if (customCellFont == null) {
            customCellFont = scrollableRowHighlighter.getCustomCellFont(beanKey, beanPath);
        }
        return customCellFont;
    }

    public void setCellHighlightFont(Object beanKey, Object beanPath, Font font) {
        scrollableRowHighlighter.addCustomCellFont(beanKey, beanPath, font);
        fixedRowHighlighter.addCustomCellFont(beanKey, beanPath, font);
    }

    public void removeCellFormat(Object beanKey, Object beanPath) {
        scrollableRowHighlighter.removeCellFormat(beanKey, beanPath);
        fixedRowHighlighter.removeCellFormat(beanKey, beanPath);
    }

    public int getMaxRowHeightForBean(Object beanKey) {
        int maxFixedRowHeight = fixedRowHighlighter.getMaxRowheight(beanKey);
        int maxScrollableRowHeight = fixedRowHighlighter.getMaxRowheight(beanKey);
        return Math.max(maxFixedRowHeight, maxScrollableRowHeight);
    }

    public LinkedList getUsedColours() {
        return scrollableRowHighlighter.getUsedColours();
    }

    public void setDragEnabled(boolean isEnabled) {
        fixedTable.setDragEnabled(isEnabled);
        scrollableTable.setDragEnabled(isEnabled);
    }

    public class AnalyticsTableAcceleratorAction extends AbstractAction {
        private AcceleratorAction acceleratorAction;
        private AnalyticsTable analyticsTable;

        public AnalyticsTableAcceleratorAction(AcceleratorAction acceleratorAction) {
            super(acceleratorAction.getActionName());
            this.acceleratorAction = acceleratorAction;
            this.analyticsTable = AnalyticsTable.this;
        }

        public void actionPerformed(ActionEvent e) {
            // stop editing before performing accelerator action - pressing an accelerator key can start the editing
            if (fixedTable.isEditing()) {
                fixedTable.removeEditor();
            }
            if (scrollableTable.isEditing()) {
                scrollableTable.removeEditor();
            }
            Object[] beanArray = analyticsTable.getSelectedBeans();
            AcceleratorAction.AcceleratorActionEvent event = new AcceleratorAction.AcceleratorActionEvent(analyticsTable, ActionEvent.ACTION_PERFORMED, null, beanArray);
            acceleratorAction.actionPerformed(event);
        }
    }

    /**
     * set the given config to match the current view settings
     * the config does not hold references to the table column model because creating a new config
     * from the current view would mean that different configs shared the same tableColumnModel object
     * and would tread on each others toes.
     */
    public TableViewConfiguration getViewConfiguration() {
        AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
        if (analyticsTableModel == null) return null;

        TableViewConfiguration configSnapshot = analyticsTableModel.getConfiguration();

        TableColumnModel tableColumnModel = (TableColumnModel) BeanUtilities.verySlowDeepCopy(scrollableTable.getColumnModel());
        TableColumnModel fixedColumnModel = (TableColumnModel) BeanUtilities.verySlowDeepCopy(fixedTable.getColumnModel());
        configSnapshot.setTableColumnModel(tableColumnModel);
        configSnapshot.setFixedColumnModel(fixedColumnModel);

        configSnapshot.setColumnColorMap(new HashMap(scrollableTable.getColumnHighlighter().getColorMap()));
        configSnapshot.setHighlightedRowKeys(new HashSet(scrollableRowHighlighter.getHighlightedRowKeys()));
        configSnapshot.setHighlightedRowColours(new HashMap(scrollableRowHighlighter.getHighlightedRowColours()));
        scrollableRowHighlighter.removeUnusedCustomCellFormat();
        fixedRowHighlighter.removeUnusedCustomCellFormat();
        configSnapshot.setCustomCellFormats(new HashMap(scrollableRowHighlighter.getCustomCellFormats()));

        configSnapshot.setShowZeroAsBlankKeys(new HashSet(showZeroAsBlankColumnKeys));
        configSnapshot.setShowNaNAsBlankKeys(new HashSet(showNaNAsBlankColumnKeys));
        configSnapshot.setSelectAfterDecimalKeys(new HashSet(selectAfterDecimalColumnKeys));
        configSnapshot.setColumnRendererIds(new HashMap(columnRendererIds));
        configSnapshot.setColumnBucketTypes(new HashMap(grouperBucketFactory.columnToBucket));
        configSnapshot.setColumnFont(new HashMap(columnFonts));
        configSnapshot.setHighlightChanges(isAnimated());
        configSnapshot.setFading(isFading());
        configSnapshot.setHighlightNewRows(isHighlightNewRows());
        configSnapshot.setColumnColourHasPriority(isColumnColourHasPriority());
        configSnapshot.setUsedColours(scrollableRowHighlighter.getUsedColours());

        configSnapshot.setTableFont(getFont());
        configSnapshot.setIncludeSubstringsInFind(filterModel.isIncludeSubstringsInFind());
        configSnapshot.setShowFilterFindPanel(filterModel.isShowFilterFindPanel());
        configSnapshot.setFilterWithFind(filterModel.isFilterWithFind());
        configSnapshot.setHighlightConditionalSetMap(getConditionalHighlighter().getHighlightConditionSetMap());
        return configSnapshot;
    }

    public boolean isHighlightNewRows() {
        return scrollableTable.isHighlightNewRows();
    }

    public void setHighlightNewRows(boolean highlightNewRows) {
        scrollableTable.setHighlightNewRows(highlightNewRows);
        fixedTable.setHighlightNewRows(highlightNewRows);
    }

    public boolean isColumnColourHasPriority() {
       return columnColourHasPriority;
    }

    public void setColumnColourHasPriority(boolean columnColourHasPriority) {
        this.columnColourHasPriority = columnColourHasPriority;
        repaint();
    }

    public boolean isFading() {
        return scrollableTable.isFading();
    }

    public void setFading(boolean fading) {
        scrollableTable.setFading(fading);
        fixedTable.setFading(fading);
    }

    public boolean isAnimated() {
        return scrollableTable.isAnimated();
    }

    public void setAnimated(boolean animated) {
        scrollableTable.setAnimated(animated);
        fixedTable.setAnimated(animated);
    }

    public void setViewConfiguration(TableViewConfiguration config) {
        if (log.isDebug()) log.debug("Setting new TableViewConfiguration");
        columnRendererIds = new HashMap(config.getColumnRendererIds());
        grouperBucketFactory.columnToBucket = new HashMap(config.getColumnBucketTypes());
        columnFonts = new HashMap(config.getColumnFont());
        showZeroAsBlankColumnKeys = new HashSet(config.getShowZeroAsBlankKeys());
        showNaNAsBlankColumnKeys = new HashSet(config.getShowNaNAsBlankKeys());
        selectAfterDecimalColumnKeys = new HashSet(config.getSelectAfterDecimalKeys());

        clearColumnModel(fixedTable.getColumnModel());
        clearColumnModel(scrollableTable.getColumnModel());

        getAnalyticsTableModel().setConfiguration(config);

        // Now we can look at the persisted column prefs and update the current column model to match
        if (config.getTableColumnModel() == null) {
            // We cannot use the config bean to give us table column info. use a default
            log.warn("Could not use stored tableColumnModel to configure column ordering and widths: " + config.getTableColumnModel());
        } else {
            // Use the persisted config to configure our table columns
            TableColumnModel persistedScrollableColModel = config.getTableColumnModel();
            ViewConfigurationColumnUpdater columnUpdater = new ViewConfigurationColumnUpdater(getAnalyticsTableModel(), persistedScrollableColModel, (ColumnHidingColumnModel)scrollableTable.getColumnModel(), columnCreator );
            columnUpdater.updateColumns();

            TableColumnModel persistedFixedColModel = config.getFixedColumnModel();
            if (persistedFixedColModel == null) persistedFixedColModel = new ObservableWidthColumnModel();
            columnUpdater = new ViewConfigurationColumnUpdater(getAnalyticsTableModel(), persistedFixedColModel, (ColumnHidingColumnModel)fixedTable.getColumnModel(), columnCreator );
            columnUpdater.updateColumns();
        }
        reconfigureAllColumns();
        refreshColumnGroups();

        scrollableTable.getColumnHighlighter().setColorMap(new HashMap(config.getColumnColorMap()));
        fixedTable.getColumnHighlighter().setColorMap(new HashMap(config.getColumnColorMap()));

        scrollableRowHighlighter.setHighlightedRowKeys(new HashSet(config.getHighlightedRowKeys()));
        fixedRowHighlighter.setHighlightedRowKeys(new HashSet(config.getHighlightedRowKeys()));

        scrollableRowHighlighter.setHighlightedRowColours(new HashMap(config.getHighlightedRowColours()));
        fixedRowHighlighter.setHighlightedRowColours(new HashMap(config.getHighlightedRowColours()));

        scrollableRowHighlighter.setCustomCellFormats(new HashMap(config.getCustomCellFormats()));
        fixedRowHighlighter.setCustomCellFormats(new HashMap(config.getCustomCellFormats()));

        scrollableRowHighlighter.setUsedColours(config.getUsedColours());
        fixedRowHighlighter.setUsedColours(config.getUsedColours());

        columnColourHasPriority = config.isColumnColourHasPriority();

        setAnimated(config.isHighlightChanges());
        setFading(config.isFading());
        setHighlightNewRows(config.isHighlightNewRows());

        filterModel.setIncludeSubstringsInFind(config.isIncludeSubstringsInFind());
        filterModel.setShowFilterFindPanel(config.isShowFilterFindPanel());
        filterModel.setFilterWithFind(config.isFilterWithFind());

        setConditionalHighlighter(new ConditionalHighlighter(getAnalyticsTableModel(), config.getHighlightConditionalSetMap()));

        Font tableFont = config.getTableFont();
        if (tableFont != null)
            setFont(tableFont);
        else {
            setFontSize(12);
            config.setTableFont(getFont());
        }
    }

    private void clearColumnModel(TableColumnModel columnModel) {
        while (columnModel.getColumnCount() > 0) {
            TableColumn tableColumn = columnModel.getColumn(0);
            columnModel.removeColumn(tableColumn);
        }
    }

    public Font getFont() {
        if (!constructed) return super.getFont();
        return scrollableTable.getFont();
    }

    public void setFont(Font font) {
        if (!constructed) {
            super.setFont(font);
            return;
        }
        scrollableTable.setFont(font);
        fixedTable.setFont(font);
    }

    public void setFontSize(int fontSize) {
        scrollableTable.setFontSize(fontSize);
        fixedTable.setFontSize(fontSize);

    }

    public int getFontSize() {
        return scrollableTable.getFontSize();
    }


    public Object[] getBeansForLocation(int row, int column) {
        return getAnalyticsTableModel().getBeansForLocation(row, column);
    }

    public boolean isSingleBean(int row, int column) {
        return getAnalyticsTableModel().isSingleBean(row, column);
    }

    public ColumnConfigFactory getColumnConfigFactory() {
        return columnConfigFactory;
    }

    public void setColumnConfigFactory(ColumnConfigFactory columnConfigFactory) {
        this.columnConfigFactory = columnConfigFactory;
        reconfigureAllColumns();
    }

    private void reconfigureAllColumns() {
        // Reconfigure all columns to effect change.
        for (Iterator iter = getColumns(); iter.hasNext();) {
            TableColumn tableColumn = (TableColumn) iter.next();
            configureColumn(tableColumn);
        }
    }

    private void addAndConfigureColumn(TableModel tableModel, TableColumnModel tableColumnModel, TableColumn column) {
        setupHeaderValue(tableModel, column);

        column.setIdentifier(
                getAnalyticsTableModel().getColumnKey(column.getModelIndex())
        );

        tableColumnModel.addColumn(column);
        configureColumn(column);
    }

    private void setupHeaderValue(TableModel tableModel, TableColumn column) {
        if (column.getHeaderValue() == null) {
            int modelColumn = column.getModelIndex();
            String columnName = tableModel.getColumnName(modelColumn);
            column.setHeaderValue(columnName);
        }
    }

    private void configureColumn(TableColumn configColumn) {
        if ( columnConfigFactory != null ) {
            ColumnConfig columnConfig = columnConfigFactory.getColumnConfigForColumn(configColumn.getIdentifier());
            if (columnConfig != null) {
                String persistedRendererId = columnRendererIds.get(configColumn.getIdentifier());
                TableCellRenderer cellRenderer = columnConfig.getRendererForId(persistedRendererId);
                configColumn.setCellRenderer(cellRenderer);
            }
        }
    }

    public final ColumnConfig getColumnConfigForColumn(Object columnKey) {
        return columnConfigFactory.getColumnConfigForColumn(columnKey);
    }

    public AnalyticsTableModel getAnalyticsTableModel() {
        TableModel model = getModel();
        AnalyticsTableModel analyticsTableModel = null;
        if (model instanceof AnalyticsTableModel) {
            analyticsTableModel = (AnalyticsTableModel) model;
        } else {
            if (log.isDebug())
                log.debug("called get analytics model but model was not instance of analyticsModel: " + model);
        }
        return analyticsTableModel;
    }

    /**
     * nb. there is a gotcha here -
     * this method does not currently support the case where there is a row selection but
     * no column selection, or vice versa. I think this can happen if the table selection is made programatically.
     * In this case null will be returned
     *
     * If in doubt, use getBeansForLocation instead
     * TODO - fix so that it works if row or columm only is selected
     *
     * @return
     */
    public Object[] getSelectedBeans() {
        int[] selectedRows = scrollableTable.getSelectedRows();

        // Get the array of beans
        ArrayList beanList = new ArrayList();
        for (int i = 0; i < selectedRows.length; i++) {
            int row = selectedRows[i];

            // Make sure we convert this to the model index.
            TableColumn selectedColumn = getSelectedColumn();
            if (selectedColumn != null) {
                beanList.addAll(Arrays.asList(getBeansForLocation(row, selectedColumn.getModelIndex())));
            }
        }

        KeyedColumnTableModel sourceModel = getAnalyticsTableModel().getSourceModel();
        Object[] array;
        if (sourceModel instanceof BeanCollectionTableModel) {
            // create an array using the beanType if possible, as clients can then cast the array
            BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) sourceModel;
            array = (Object[]) Array.newInstance(beanCollectionTableModel.getBeanType(), beanList.size());
        } else {
            array = new Object[beanList.size()];
        }
        return beanList.toArray(array);
    }

    /**
     * I hope someone feels very guilty for making me do this.
     * A horrible hack, for a horrible requirement, put in a horrible place in the code. Sorry
     *
     * @param columnKey
     * @param selected
     */
    public void setShowZeroAsBlank(Object columnKey, boolean selected) {
        //if column is a pivot column, then set all of them to the same precision
        AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
        if (columnKey instanceof PivotTableModel.GeneratedColumnKey) {

            for (Iterator iterator = getColumns(); iterator.hasNext();) {
                TableColumn modelColumn = (TableColumn) iterator.next();
                Object testColumnKey = analyticsTableModel.getColumnKey(modelColumn.getModelIndex());

                if (testColumnKey instanceof PivotTableModel.GeneratedColumnKey) {
                    if (getColumnConfigForColumn(testColumnKey) == getColumnConfigForColumn(columnKey)) {
                        if (selected) {
                            showZeroAsBlankColumnKeys.add(testColumnKey);
                        } else {
                            showZeroAsBlankColumnKeys.remove(testColumnKey);
                        }
                    }
                }
            }
        } else {
            if (selected) {
                showZeroAsBlankColumnKeys.add(columnKey);
            } else {
                showZeroAsBlankColumnKeys.remove(columnKey);
            }
        }

        repaint();
    }

    public boolean isShowZeroAsBlank(Object columnKey) {
        return showZeroAsBlankColumnKeys.contains(columnKey);
    }

    /**
     * I hope someone feels very guilty for making me do this.
     * A horrible hack, for a horrible requirement, put in a horrible place in the code. Sorry
     *
     * @param columnKey
     * @param selected
     */
    public void setShowNaNAsBlank(Object columnKey, boolean selected) {
        if (selected) {
            showNaNAsBlankColumnKeys.add(columnKey);
        } else {
            showNaNAsBlankColumnKeys.remove(columnKey);
        }
        repaint();
    }

    public boolean isShowNaNAsBlank(Object columnKey) {
        return showNaNAsBlankColumnKeys.contains(columnKey);
    }

    /**
     * Used for decimal editors, where they want to select the decimal places only
     * instead of all
     *
     * @param columnKey
     * @param selected
     */
    public void setSelectAfterDecimal(Object columnKey, boolean selected) {
        if (selected) {
            selectAfterDecimalColumnKeys.add(columnKey);
        } else {
            selectAfterDecimalColumnKeys.remove(columnKey);
        }
    }

    public boolean isSelectAfterDecimal(Object columnKey) {
        return selectAfterDecimalColumnKeys.contains(columnKey);
    }

    public Integer getColumnFont(Object columnKey) {
        if (columnFonts == null) return null;
        return (Integer) columnFonts.get(columnKey);
    }

    // later adding font instead of style, but I'm not sure that this will look good, if you have to
    // many font style per column...
    public void setColumnFont(Object columnKey, Integer fontStyle) {
        this.columnFonts.put(columnKey, fontStyle);
        fixedTable.resizeAndRepaint();
        scrollableTable.resizeAndRepaint();
    }

    public void setColumnRendererId(Object columnKey, String columnRendererId) {
        TableColumn column = getColumn(columnKey);
        ColumnConfig columnConfig = getColumnConfigForColumn(columnKey);
        column.setCellRenderer(columnConfig.getRendererForId(columnRendererId));
        repaint();
        columnRendererIds.put(columnKey, columnRendererId);
    }

    public String getColumnRendererId(Object columnKey) {
        return columnRendererIds.get(columnKey);
    }

    public void setColumnBucketClass(Object columnKey, Class bucketClass) {
        grouperBucketFactory.setBucketClassForColumn(columnKey, bucketClass);
    }

    public Class getColumnBucketClass(Object columnKey) {
        return grouperBucketFactory.getBucketClassForColumn(columnKey);
    }

    public Class getColumnTransformClass(Object columnKey) {
        return getAnalyticsTableModel().getTransformClassForColumn(columnKey);
    }

    public void setColumnTransformClass(Object columnKey, Class transformClass) {
        getAnalyticsTableModel().setTransformClassForColumn(columnKey, transformClass);
    }

    public void setDefaultRenderer(Class clazz, TableCellRenderer quantityRenderer) {
        fixedTable.setDefaultRenderer(clazz, quantityRenderer);
        scrollableTable.setDefaultRenderer(clazz, quantityRenderer);
    }

    public void setDefaultFilterFormatter(Class clazz, Format format) {
        getAnalyticsTableModel().setFilterFormatter(clazz, format);
    }

    public void setDefaultEditor(Class clazz, TableCellEditor cellEditor) {
        fixedTable.setDefaultEditor(clazz, cellEditor);
        scrollableTable.setDefaultEditor(clazz, cellEditor);
    }

    public void addTableHeaderMouseListener(MouseListener mouseListener) {
        fixedTable.getTableHeader().addMouseListener(mouseListener);
        scrollableTable.getTableHeader().addMouseListener(mouseListener);
    }

    public void removeTableHeaderMouseListener(MouseListener mouseListener) {
        fixedTable.getTableHeader().removeMouseListener(mouseListener);
        scrollableTable.getTableHeader().removeMouseListener(mouseListener);
    }

    public void addTableMouseListener(MouseListener mouseListener) {
        fixedTable.addMouseListener(mouseListener);
        scrollableTable.addMouseListener(mouseListener);
    }

    public void removeTableMouseListener(MouseListener mouseListener) {
        fixedTable.removeMouseListener(mouseListener);
        scrollableTable.removeMouseListener(mouseListener);
    }

    public void addUserChangedSelectionListener(UserChangedSelectionListener l) {
        fixedTable.addMouseListener(l);
        scrollableTable.addMouseListener(l);
        fixedTable.addKeyListener(l);
        scrollableTable.addKeyListener(l);
    }

    public void removeUserChangedSelectionListener(UserChangedSelectionListener l) {
        fixedTable.removeMouseListener(l);
        scrollableTable.removeMouseListener(l);
        fixedTable.removeKeyListener(l);
        scrollableTable.removeKeyListener(l);
    }

    public void dispose() {
        fixedTable.dispose();
        scrollableTable.dispose();
    }

    /**
     * Causes a repaint of all table headers.  Used to update rendering of visual cues about table sorting.
     */
    public void repaintHeaders() {
        fixedTable.getTableHeader().repaint();
        scrollableTable.getTableHeader().repaint();
    }

    public int rowAtPoint(Point p) {
        if (inFixedTable(p)) {
            return fixedTable.rowAtPoint(p);
        } else {
            return scrollableTable.rowAtPoint(p);
        }
    }

    private boolean inFixedTable(Point p) {
        return p.x < fixedTable.getWidth();
    }

    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        if (column < fixedTable.getColumnCount()) {
            return fixedTable.getCellRect(row, column, includeSpacing);
        } else {
            return scrollableTable.getCellRect(row, column, includeSpacing);
        }
    }

    public int[] getSelectedRows() {
        return scrollableTable.getSelectedRows();
    }

    public int getSelectedRow() {
        return scrollableTable.getSelectedRow();
    }

    public Color getColumnColor(Object columnKey) {
        return scrollableTable.getColumnHighlighter().getColumnColor(columnKey);
    }

    public void setColumnColor(Object columnKey, Color color) {
        scrollableTable.getColumnHighlighter().setColumnColor(columnKey, color);
        fixedTable.getColumnHighlighter().setColumnColor(columnKey, color);
    }

    public int getRowCount() {
        return scrollableTable.getRowCount();
    }

    public String getColumnName(int column) {
        int fixedColumnCount = fixedTable.getColumnCount();
        if (column < fixedColumnCount) {
            return fixedTable.getColumnName(column);
        } else {
            return scrollableTable.getColumnName(column - fixedColumnCount);
        }
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
        int fixedColumnCount = fixedTable.getColumnCount();
        if (column < fixedColumnCount) {
            return fixedTable.getCellRenderer(row, column);
        } else {
            return scrollableTable.getCellRenderer(row, column - fixedColumnCount);
        }
    }

    public Object getValueAt(int row, int col) {
        int fixedColumnCount = fixedTable.getColumnCount();
        if (col < fixedColumnCount) {
            return fixedTable.getValueAt(row, col);
        } else {
            return scrollableTable.getValueAt(row, col - fixedColumnCount);
        }
    }

    public void setIntercellSpacing(Dimension dimension) {
        fixedTable.setIntercellSpacing(dimension);
        scrollableTable.setIntercellSpacing(dimension);
    }

    public void setShowGrid(boolean b) {
        fixedTable.setShowGrid(b);
        scrollableTable.setShowGrid(b);
    }

    public void setRowHeight(int height) {
        fixedTable.setRowHeight(height);
        scrollableTable.setRowHeight(height);
    }

    private void setConditionalHighlighter(final ConditionalHighlighter conditionalHighlighter) {
        this.conditionalHighlighter = conditionalHighlighter;
        scrollableTable.setCurrentHighlighter(new MultiplexingCellHighlighter(conditionalHighlighter,
                scrollableTable.getCurrentHighlighter()));
        fixedTable.setCurrentHighlighter(new MultiplexingCellHighlighter(conditionalHighlighter,
                fixedTable.getCurrentHighlighter()));
    }

    public ConditionalHighlighter getConditionalHighlighter() {
        return conditionalHighlighter;
    }

    public void setCustomHighlighter(final HighlightedTable.CellHighlighter cellHighlighter) {
        scrollableTable.setCustomHighlighter(new MultiplexingCellHighlighter(cellHighlighter,
                scrollableTable.getCustomHighlighter()));
        fixedTable.setCustomHighlighter(new MultiplexingCellHighlighter(cellHighlighter,
                fixedTable.getCustomHighlighter()));
    }

    public HighlightedTable.CellHighlighter getCustomHighlighter() {
        return  scrollableTable.getCustomHighlighter();
    }

    public void setSelectionMode(int selection) {
        scrollableTable.setSelectionMode(selection);
        fixedTable.setSelectionMode(selection);
    }

    public boolean isBeanHighlighted(Object keyForBean) {
        return scrollableRowHighlighter.getHighlightedRowColours().containsKey(keyForBean);
    }

    public void addBeanForRowHighlight(Object keyForBean) {
        scrollableRowHighlighter.addBean(keyForBean);
        fixedRowHighlighter.addBean(keyForBean);
    }

    public void removeBeanForRowHighlight(Object keyForBean) {
        scrollableRowHighlighter.removeBean(keyForBean);
        fixedRowHighlighter.removeBean(keyForBean);
    }

    public void setAutoResizeMode(int autoResizeAllColumns) {
        scrollableTable.setAutoResizeMode(autoResizeAllColumns);
        fixedTable.setAutoResizeMode(autoResizeAllColumns);
    }

    public void cancelCellEditing() {
        cancelEditor(scrollableTable.getCellEditor());
        cancelEditor(fixedTable.getCellEditor());
    }

    private void cancelEditor(TableCellEditor cellEditor) {
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
    }

    public void setPreferredScrollableViewportSize(Dimension dimension) {
        scrollableTable.setPreferredScrollableViewportSize(dimension);
        // Ignore the fixed table.  Not needed in this case.
    }

    public TableCellFinder getMatchFinder() {
        return tableCellFinder;
    }

    public FilterTablePanelModel getFilterPanelModel() {
        return filterModel;
    }

    public boolean isShowFilterFindPanel() {
        return filterModel.isShowFilterFindPanel();
    }

    /**
     * Restore the user selections where possible when the user changes the sort
     * (or a data change event occurs)
     */
    private class SelectionRestoringHighlightedTable extends HighlightedTable {

        public SelectionRestoringHighlightedTable(TableModel tableModel) {
            super(tableModel);
        }

        public void tableChanged(TableModelEvent e) {

            //when the super.tableChanged() processes the event this will clear the selections,
            //which is why we need to save the current selections and restore them afterwards
             if ( e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE ) {
                Set<Object> selectionSnapshot = selection.getSelectionSnapshot();
                int selectedColumn = selection.getSelectedColumn();
                super.tableChanged(e);
                selection.restoreSelections(selectionSnapshot, selectedColumn);
             } else {
                 super.tableChanged(e);
             }
         }
    }

    protected class CustomHighlightedTable extends SelectionRestoringHighlightedTable {
        private TableMouseEventHandler tableMouseEventHandler;
        private Map<Integer,Font> derivedFonts = new HashMap<Integer,Font>();

        public CustomHighlightedTable(TableModel tableModel) {
            super(tableModel);
        }

        @Override
        public boolean editCellAt(int row, int column,  EventObject e) {

            boolean editOk =  super.editCellAt(row, column, e);

            // check prev event and send cancel...
            if(tableCellEditEvent != null){
                for(TableCellEditListener l : tableCellEditListeners){
                    l.editCancelled(this);
                }
            }
            if(!editOk){
                tableCellEditEvent = null;
            }
            else{
                tableCellEditEvent = new TableCellEditEvent(this, row, column,
                        scrollableCopyPasteRowHighlighter.getRows(), getValueAt(row, column));
                for(TableCellEditListener l : tableCellEditListeners){
                    l.editCellStarted(this, row, column,
                            tableCellEditEvent.selectedRows,
                            tableCellEditEvent.oldValue);
                }
            }

            return editOk;
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
            super.editingCanceled(e);
            tableCellEditEvent = null;
            for(TableCellEditListener l : tableCellEditListeners){
                l.editCancelled(this);
            }
        }

        @Override
        public void editingStopped(ChangeEvent e) {

            int row = getEditingRow();
            int col = getEditingColumn();

            TableCellEditor editor = getCellEditor();
            Object newValue = null;
            if (editor != null) {
                newValue = editor.getCellEditorValue();
            }

            if(getCopyPasteSelectedRows() != null && getCopyPasteSelectedRows().length > 1){
                super.editingCanceled(e);
            }
            else{
                super.editingStopped(e);
            }

            if(tableCellEditEvent != null){

                for(TableCellEditListener l : tableCellEditListeners){
                    l.editCellCompleted(this, row, col,
                            tableCellEditEvent.selectedRows,
                            tableCellEditEvent.oldValue,
                            newValue);
                }
            }


            tableCellEditEvent = null;
        }

        public void resizeAndRepaint() {
            super.resizeAndRepaint();
        }

        public void setBeanActionFactory(BeanActionFactory beanActionFactory) {
            if (this.tableMouseEventHandler != null) {
                removeMouseListener(tableMouseEventHandler);
            }

            if (beanActionFactory != null) {
                tableMouseEventHandler = new TableMouseEventHandler(beanActionFactory);
                addMouseListener(tableMouseEventHandler);
            }
        }

        /**
         * I hope someone feels very guilty for making me do this.
         * A horrible hack, for a horrible requirement, put in a horrible place in the code. Sorry
         * Blank zero values *shudder*
         * todo: remove this when Dan finishes custom renderers per bean path
         */
        public Component prepareRenderer(TableCellRenderer renderer, int row, int colIndex) {

            RenderingComponentWithCellValue c = super.doPrepareRenderer(renderer, row, colIndex);
            Component component = c.getRendererComponent();
            Object cellValue = c.getCellValue();

            Object columnKey = getAnalyticsTableModel().getColumnKey(convertColumnIndexToModel(colIndex));

            if (isShowZeroAsBlank(columnKey)) {
                if ((cellValue instanceof Number && ((Number) cellValue).doubleValue() == 0.0) ||
                        (cellValue instanceof Numeric && ((Numeric) cellValue).doubleValue() == 0.0)) {
                    if (component instanceof DefaultTableCellRenderer) {
                        ((DefaultTableCellRenderer) component).setText("");
                    }
                }
            }

            if (isShowNaNAsBlank(columnKey)) {
                if ((cellValue instanceof Number && Double.isNaN(((Number) cellValue).doubleValue())) ||
                        (cellValue instanceof Numeric && Double.isNaN(((Numeric) cellValue).doubleValue()))) {
                    if (component instanceof DefaultTableCellRenderer) {
                        ((DefaultTableCellRenderer) component).setText("");
                    }
                }
            }

            Object[] beans = getAnalyticsTableModel().getBeansForLocation(row, 0);
            Font customFont = null;
            Object key = null;
            if (beans.length > 0) {
                key = PropertyModel.getInstance(beans[0].getClass()).getKeyForBean(beans[0]);
                Object beanPath = getColumnModel().getColumn(colIndex).getIdentifier();
                customFont = getCellHighlightFont(key, beanPath);
            }

            if (customFont != null) {
                component.setFont(customFont);
            } else {
                Integer fontStyle = getColumnFont(columnKey);
                if (fontStyle != null) {
                    if (component instanceof DefaultTableCellRenderer) {
                        JComponent jComponent = (JComponent) component;
                        jComponent.setFont(getDerivedFont(fontStyle));
                    }
                }
            }

            if (rowsToRecalHeight.contains(key)){
                int height = getMaxRowHeightForBean(key);
                setCustomRowHeight(row, height);
                rowsToRecalHeight.remove(key);
            }
            return component;
        }

        //maintaing a map of derived fonts saves a lot of cpu cycles - deriveFont was using 3%
        private Font getDerivedFont(Integer fontStyle) {
            Font f = derivedFonts.get(fontStyle);
            if ( f == null ) {
                f = getFont().deriveFont(getFont().getStyle() ^ fontStyle.intValue());
                derivedFonts.put(fontStyle, f);
            }
            return f;
        }

        public void setFont(Font f) {
            super.setFont(f);
            //setFont is called by superclass constructor - so derivedFonts will be null first time
            //there's a moral there, somewhere.
            if ( derivedFonts != null ) {
                derivedFonts.clear();
            }
        }

        public void setModel(TableModel dataModel) {
            if (!(dataModel instanceof AnalyticsTableModel)) {
                if (log.isDebug()) log.debug("Setting to a non-analytics model: " + dataModel);
            }
            super.setModel(dataModel);
        }

        public Component prepareEditor(TableCellEditor editor, int row, int column) {
            Component component = super.prepareEditor(editor, row, column);
            Object[] beans = getAnalyticsTableModel().getBeansForLocation(row, 0);
            Object columnKey = getAnalyticsTableModel().getColumnKey(convertColumnIndexToModel(column));
            Font customFont = null;
            if (beans.length > 0) {
                Object key = PropertyModel.getInstance(beans[0].getClass()).getKeyForBean(beans[0]);
                String beanPath = (String) getColumnModel().getColumn(column).getIdentifier();
                customFont = getCellHighlightFont(key, beanPath);
            }
            Integer fontStyle = getColumnFont(columnKey);
            if (component instanceof JTextField || component instanceof JComboBox) {
                JComponent jComponent = (JComponent) component;
                if (customFont != null) {
                    component.setFont(customFont);
                } else {
                    jComponent.setFont((fontStyle != null) ? getDerivedFont(fontStyle) : getFont());
                }
                if (component instanceof DecimalTextField) {
                    if (isSelectAfterDecimal(columnKey)) {
                        ((DecimalTextField) component).setSelectAfterDecimal(true);
                    } else {
                        ((DecimalTextField) component).setSelectAfterDecimal(false);
                    }
                }
            }
            return component;
        }

        public boolean getColumnSelectionAllowed() {
            return getAnalyticsTableModel().isColumnSelectionAllowed();
        }

        /**
         * This gets called during the construction and after table structure changed events.
         */
        public void createDefaultColumnsFromModel() {

            if (!constructed) {
                super.createDefaultColumnsFromModel();
                return;
            }

            boolean isFixedTable = (this == fixedTable);
            ColumnHidingColumnModel fixedColumnModel = (ColumnHidingColumnModel) fixedTable.columnModel;
            ColumnHidingColumnModel scrollableColumnModel = (ColumnHidingColumnModel) scrollableTable.columnModel;
            ColumnHidingColumnModel oppositeColumnModel = isFixedTable ? scrollableColumnModel : fixedColumnModel;

            setupColumns((ColumnHidingColumnModel)getColumnModel(), oppositeColumnModel, isFixedTable);

            repaintHeaders();
        }

        private void repaintHeaders() {
            JTableHeader tableHeader = getTableHeader();
            if (tableHeader != null) {
                tableHeader.resizeAndRepaint();
            }
            resizeAndRepaint();
        }

        /**
         * Setup the table columns in response to a structure change in the source table model
         */
        private void setupColumns(ColumnHidingColumnModel tableColumnModel, ColumnHidingColumnModel oppositeColumnModel, boolean isFixedTable) {

            StructureChangeColumnUpdater columnUpdater = new StructureChangeColumnUpdater(
                    getAnalyticsTableModel(),
                    tableColumnModel,
                    oppositeColumnModel,
                    columnCreator,
                    isFixedTable
            );

            columnUpdater.updateColumns();
        }

        public void addColumn(TableColumn column) {
            addAndConfigureColumn(getModel(), getColumnModel(), column);
        }

        private class TableMouseEventHandler extends MouseAdapter {
            private AnalyticsTable analyticsTable;
            private BeanActionFactory beanActionFactory;

            public TableMouseEventHandler(BeanActionFactory beanActionFactory) {
                this.analyticsTable = AnalyticsTable.this;
                this.beanActionFactory = beanActionFactory;
            }

            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    defaultActionForPath();
                }
            }

            private void defaultActionForPath() {
                Action action = createAction();

                if (action != null && action.isEnabled()) {
                    action.actionPerformed(new ActionEvent(analyticsTable, ActionEvent.ACTION_PERFORMED, null));
                }
            }

            private void checkPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    ActionGroup actions = createActions();

                    if (actions.size() > 0) {
                        showContextMenu(actions, (Component) e.getSource(), e.getX(), e.getY());
                    }
                }
            }

            private void showContextMenu(ActionGroup actions, Component source, int x, int y) {
                JMenu popupMenu = createMenuForActionGroup(actions);
                popupMenu.getPopupMenu().show(source, x, y);
            }

            private JMenu createMenuForActionGroup(ActionGroup actionGroup) {
                JMenu menu = new JMenu(actionGroup.getName());
                Iterator iterator = actionGroup.iterator();
                while (iterator.hasNext()) {
                    Object element = iterator.next();
                    if (element == ActionGroup.SEPERATOR_ACTION) {
                        menu.addSeparator();
                    } else if (element instanceof Action) {
                        menu.add((Action) element);
                    } else if (element instanceof ActionGroup) {
                        JMenu subMenu = createMenuForActionGroup((ActionGroup) element);
                        menu.add(subMenu);
                    }
                }
                return menu;
            }

            private ActionGroup createActions() {
                Object[] beanArray = analyticsTable.getSelectedBeans();
                return beanActionFactory.getActions(beanArray);
            }

            /**
             * @return default action (invoked on double click).
             */
            private Action createAction() {
                Object[] beanArray = analyticsTable.getSelectedBeans();

                if (beanArray.length == 1) {
                    // Get the bean path inferred by the column index
                    TableColumn selectedColumn = getColumnModel().getColumn(getSelectedColumn());
                    int selectedColumnModelIndex = selectedColumn.getModelIndex();
                    Object columnKey = analyticsTable.getAnalyticsTableModel().getColumnKey(selectedColumnModelIndex);
                    String beanPath = analyticsTable.getAnalyticsTableModel().getBeanPathForColumn(columnKey);
                    if (beanPath == null) beanPath = ""; // avoid NullPointerException in existing code
                    return beanActionFactory.getAction(beanArray[0], beanPath);
                } else {
                    return null;
                }
            }
        }
    }



    public class FilterFindCellHighlighter extends HighlightedTable.CellHighlighter {

        public FilterFindCellHighlighter(HighlightedTable.CellHighlighter underlyingCellHighlighter) {
            super(underlyingCellHighlighter);
        }

        public Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
            int tableCol = table.getColumnModel().getColumn(viewCol).getModelIndex();
            boolean isMatchingFind = analyticsTableModel.isCellMatchingSearch(row, tableCol);

            //the tableCellFinder has a ColumnSource which combines the columns from fixed + scrollable
            int colIndexInCombinedTable = table == getFixedTable() ? viewCol : viewCol + getFixedTable().getColumnCount();
            return isMatchingFind ? tableCellFinder.getLastFindResult().isCellAt(row, colIndexInCombinedTable) ?
                    Color.red : Color.yellow :
                    super.getHighlightForCell(row, viewCol, table);
        }
    }

    public class CopyPasteRowHighlighter extends HighlightedTable.CellHighlighter {
        private final Color highlight;
        private final float[] cpClr;
        private int rows[];
        private final float alpha = 0.5f;
        private final float alphaS = 0.999f - alpha;

        public CopyPasteRowHighlighter(HighlightedTable.CellHighlighter underlying) {
            super(underlying);
            highlight = new Color(255, 0, 255);
            cpClr = highlight.getRGBColorComponents(null);
        }

        public void setRows(int[] rows) {
            this.rows = rows;
        }

        public int[] getRows() {
            return rows;
        }

        public Color getHighlightForCell(int row, int col, HighlightedTable table) {
            if (rows != null && rows.length > 0) {
                for (int r : rows){
                    if(r == row){
                        Color superClr = super.getHighlightForCell(row, col, table);
                        if(superClr == null){
                            return highlight;
                        }
                        float[] superC = superClr.getRGBColorComponents(null);
                        Color color = new Color(
                                alphaS * superC[0] + cpClr[0] * alpha,
                                alphaS * superC[1] + cpClr[1] * alpha,
                                alphaS * superC[2] + cpClr[2] * alpha);
                        return color;
                    }
                }
            }
            return super.getHighlightForCell(row, col, table);
        }
    }

    //provides a combined view of the columns from both fixed and scrollable models to the TableCellFinder
    private class AnalyticsColumnSource implements TableCellFinder.ColumnSource {

        public int getTableModelColumnIndex(int columnModelIndex) {
            return getColumn(columnModelIndex).getModelIndex();
        }

        public int getColumnCount() {
            return AnalyticsTable.this.getColumnCount();
        }
    }



    public class CustomRowHighlighter extends HighlightedTable.CellHighlighter {

        private HashSet highlightedRowKeys = new HashSet();
        private HashMap highlightedRowColours = new HashMap();
        private HashMap<Object, Map> customCellFormats = new HashMap<Object, Map>();
        private TIntHashSet highlightedRows = new TIntHashSet(highlightedRowKeys.size());
        private boolean needsRecalc = true;
        private LinkedList usedColours = new LinkedList();
        private final int noOfColoursToStore = 20;

        private Color defaultRowHighlightColor = new Color(180, 255, 240);

        public CustomRowHighlighter(HighlightedTable.CellHighlighter underlying) {
            super(underlying);
        }

        public Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            recalc();
            Color highlightcolor = super.getHighlightForCell(row, viewCol, table);
            Color customColor = null;
            Object key = null;
            Object[] beans = getAnalyticsTableModel().getBeansForLocation(row, viewCol);
            if (beans != null && beans.length > 0) {
                key = PropertyModel.getInstance(beans[0].getClass()).getKeyForBean(beans[0]);
                Object beanPath = table.getColumnModel().getColumn(viewCol).getIdentifier();
                customColor = getCustomCellColour(key, beanPath);
            }
            if (customColor != null) {
                highlightcolor = customColor;
            } else {
                if (highlightedRows.contains(row)) {
                    Color configuredColor = (Color) highlightedRowColours.get(key);
                    if (configuredColor != null) {
                        if (columnColourHasPriority) {
                            if (highlightcolor == null) {
                                highlightcolor = configuredColor;
                            } else if (highlightcolor.equals(ALTERNATE_ROW_HIGHLIGHT_COLOR)) {
                                highlightcolor = UIUtilities.multiply(configuredColor, highlightcolor);
                            }
                        } else {
                            highlightcolor = UIUtilities.multiply(configuredColor, highlightcolor);
                        }
                    }
                }
            }
            return highlightcolor;
        }

        // This method is only used for the colour chooser to show the highlight color.
        public Color getColorForBean(Object bean) {
            Color highlightColor = (Color) highlightedRowColours.get(bean);
            if (highlightColor == null) {
                highlightColor = defaultRowHighlightColor;
            }
            return highlightColor;
        }

        public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            if (needsRecalc) return;

            if (e.getColumn() == TableModelEvent.ALL_COLUMNS && !(e instanceof MultipleColumnChangeEvent)) {
                if (e.getType() == TableModelEvent.INSERT) {
                    if (e.getFirstRow() == e.getLastRow()) {
                        //one row insert
                        int insertRow = e.getFirstRow();

                        int[] rows = highlightedRows.toArray();
                        for (int i = 0; i < rows.length; i++) {
                            int row = rows[i];
                            if (row >= insertRow) {
                                highlightedRows.remove(row);
                                highlightedRows.add(row + 1);
                            }
                        }

                        if (isHighlighted(insertRow)) {
                            highlightedRows.add(insertRow);
                        }

                        // Fix for JTable bug.  Insert when row zero selected causes more and more rows to be selected.
                        if (getSelectionModel().getMinSelectionIndex() == 0 && e.getFirstRow() == 0) {
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    getSelectionModel().removeSelectionInterval(0, 0);
                                }
                            });
                        }
                    } else {
                        needsRecalc = true;
                    }
                } else if (e.getType() == TableModelEvent.DELETE) {
                    if (e.getFirstRow() == e.getLastRow()) {
                        //one row delete
                        int deleteRow = e.getFirstRow();

                        highlightedRows.remove(deleteRow);
                        int[] rows = highlightedRows.toArray();
                        for (int i = 0; i < rows.length; i++) {
                            int row = rows[i];
                            if (row >= deleteRow) {
                                highlightedRows.remove(row);
                                highlightedRows.add(row - 1);
                            }
                        }
                    } else {
                        needsRecalc = true;
                    }
                } else {
                    //some large update, check if it is structure or data change
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW || (e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE)) {
                        needsRecalc = true;
                    }
                }
            }
        }

        private void recalc() {
            if (needsRecalc) {
                highlightedRows.clear();
                if (getAnalyticsTableModel().getColumnCount() > 0) {
                    int rowCount = getAnalyticsTableModel().getRowCount();
                    for (int row = 0; row < rowCount; row++) {
                        if (isHighlighted(row)) {
                            highlightedRows.add(row);
                        }
                    }
                }
                needsRecalc = false;
            }
        }

        private boolean isHighlighted(int row) {
            Object[] beans = getAnalyticsTableModel().getBeansForLocation(row, 0);
            if (beans.length == 1) {
                if (highlightedRowColours.size() > 0) {
                    Object key = PropertyModel.getInstance(beans[0].getClass()).getKeyForBean(beans[0]);
                    return highlightedRowColours.containsKey(key);
                }
            }
            return false;
        }

        public void addUsedColor(Color aColor) {
            if (!usedColours.contains(aColor)) {
                if (usedColours.size() == noOfColoursToStore) {
                    usedColours.removeFirst();
                }
                usedColours.addLast(aColor);
            }
        }

        public void addBean(Object bean) {
            Object key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
            highlightedRowColours.put(key, defaultRowHighlightColor);
            highlightedRowKeys.add(key);
            addUsedColor(defaultRowHighlightColor);
            needsRecalc = true;
        }

        public void addBean(Object bean, Color newHighlightColor) {
            Object key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
            highlightedRowColours.put(key, newHighlightColor);
            highlightedRowKeys.add(key);
            addUsedColor(newHighlightColor);
            needsRecalc = true;
        }

        public void removeBean(Object bean) {
            Object key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
            highlightedRowColours.remove(key);
            highlightedRowKeys.remove(key);
            needsRecalc = true;
        }

        public HashMap getHighlightedRowColours() {
            return highlightedRowColours;
        }

        public void setHighlightedRowColours(HashMap highlightedRowColours) {
            this.highlightedRowColours = highlightedRowColours;
            // For backward compability add all the highlight rows to the colour map
            Iterator it = highlightedRowKeys.iterator();
            while (it.hasNext()) {
                Object key = it.next();
                if (!highlightedRowColours.containsKey(key)) {
                    highlightedRowColours.put(key, defaultRowHighlightColor);
                }
            }
        }

        public HashSet getHighlightedRowKeys() {
            return highlightedRowKeys;
        }

        public void setHighlightedRowKeys(HashSet highlightedRowKeys) {
            this.highlightedRowKeys = highlightedRowKeys;
        }

        public LinkedList getUsedColours() {
            return usedColours;
        }

        public void setUsedColours(LinkedList usedColours) {
            this.usedColours = usedColours;
            addUsedColor(defaultRowHighlightColor);
        }

        public HashMap getCustomCellFormats() {
            return customCellFormats;
        }

        public void setCustomCellFormats(HashMap customCellFormats) {
            this.customCellFormats = customCellFormats;
            rowsToRecalHeight.addAll(customCellFormats.keySet());
        }

        public Color getCustomCellColour(Object beanKey, Object beanPath) {
            Color customColor = null;
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
                if (customCellFormat != null) {
                    customColor = customCellFormat.getColor();
                }
            }
            return customColor;
        }

        public void removeCustomCellColour(Object beanKey, Object beanPath) {
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
                if (customCellFormat != null) {
                    customCellFormat.setColor(null);
                    if (customCellFormat.getFont() == null) {
                        beanConfig.remove(beanPath);
                        if (beanConfig.size() == 0) {
                            customCellFormats.remove(beanKey);
                        }
                    }
                }
            }
        }

        public void addCustomCellColour(Object beanKey, Object beanPath, Color color) {
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig == null) {
                beanConfig = new HashMap();
                customCellFormats.put(beanKey, beanConfig);
            }
            CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
            if (customCellFormat == null) {
                customCellFormat = new CustomCellFormat();
                beanConfig.put(beanPath, customCellFormat);
            }
            customCellFormat.setColor(color);
        }

        public Font getCustomCellFont(Object beanKey, Object beanPath) {
            Font customeFont = null;
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
                if (customCellFormat != null) {
                    customeFont = customCellFormat.getFont();
                }
            }
            return customeFont;
        }

        public void removeCustomCellFont(Object beanKey, String beanPath) {
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
                if (customCellFormat != null) {
                    customCellFormat.setFont(null);
                    if (customCellFormat.getColor() == null) {
                        beanConfig.remove(beanPath);
                        if (beanConfig.size() == 0) {
                            customCellFormats.remove(beanKey);
                        }
                    }
                }
            }
            rowsToRecalHeight.add(beanKey);
        }

        public void addCustomCellFont(Object beanKey, Object beanPath, Font font) {
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig == null) {
                beanConfig = new HashMap();
                customCellFormats.put(beanKey, beanConfig);
            }
            CustomCellFormat customCellFormat = (CustomCellFormat) beanConfig.get(beanPath);
            if (customCellFormat == null) {
                customCellFormat = new CustomCellFormat();
                beanConfig.put(beanPath, customCellFormat);
            }
            customCellFormat.setFont(font);
            rowsToRecalHeight.add(beanKey);
        }

        public void removeCellFormat(Object beanKey, Object beanPath) {
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                beanConfig.remove(beanPath);
            }
            rowsToRecalHeight.add(beanKey);
        }

        public void removeUnusedCustomCellFormat() {
            Set currentBeanKeys = getCurrentBeanKeys();
            if (currentBeanKeys.size() > 0) {
                Set beansToRemove = new HashSet();
                Iterator<Object> customCellFormatKeys = customCellFormats.keySet().iterator();
                while (customCellFormatKeys.hasNext()) {
                    Object beanKey = customCellFormatKeys.next();
                    if (!currentBeanKeys.contains(beanKey)) {
                        beansToRemove.add(beanKey);
                    }
                }
                Iterator iterator = beansToRemove.iterator();
                while (iterator.hasNext()) {
                    customCellFormats.remove(iterator.next());
                }
            }
        }

        public int getMaxRowheight(Object beanKey) {
            int currentMaxHeight = defaultRowHeight;
            Map beanConfig = customCellFormats.get(beanKey);
            if (beanConfig != null) {
                Iterator customsCellFormats = beanConfig.values().iterator();
                while (customsCellFormats.hasNext()) {
                    CustomCellFormat cellFormat = (CustomCellFormat) customsCellFormats.next();
                    Font customFont = cellFormat.getFont();
                    if (customFont != null) {
                        int fontHeight = getFontMetrics(customFont).getHeight();
                        if (fontHeight > currentMaxHeight) {
                            currentMaxHeight = fontHeight;
                        }
                    }
                }
            }
            return currentMaxHeight;
        }

        private Set getCurrentBeanKeys() {
            Set beanKeys = new HashSet();
            KeyedColumnTableModel tableModel = getAnalyticsTableModel().getSourceModel();
            if (tableModel instanceof BeanCollectionTableModel) {
                BeanCollectionTableModel model = (BeanCollectionTableModel) tableModel;
                BeanCollection beanCollecton = model.getBeanCollecton();
                if (beanCollecton != null) {
                    Iterator beans = beanCollecton.iterator();
                    while (beans.hasNext()) {
                        Object bean = beans.next();
                        Object key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
                        beanKeys.add(key);
                    }
                }
            }
            return beanKeys;
        }

    }

    public class GrouperBucketFactory implements GroupedRowManager.BucketFactory, GroupedTableModel.GroupedKeyDefinition {
        private GroupedRowManager.BucketFactory defaultFactory;
        private HashMap<String, Class> columnToBucket = new HashMap<String, Class>();

        public GrouperBucketFactory(GroupedRowManager.BucketFactory defaultFactory) {
            this.defaultFactory = defaultFactory;
        }

        public GroupedRowManager.ValueBucket createBucketForKey(Object columnKey) {
            String beanPath = getBeanPath(columnKey);
            Class bucketClass = getBucketClassForColumn(beanPath, true);
            GroupedRowManager.ValueBucket newBucket;
            try {
                newBucket = (GroupedRowManager.ValueBucket) bucketClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not create bucket of type: " + bucketClass, e);
            }
            return newBucket;
        }

        public void clear() {
            columnToBucket.clear();
        }

        public void setBucketClassForColumn(Object columnKey, Class bucketClass) {
            String path = getBeanPath(columnKey);
            columnToBucket.put(path, bucketClass);
            getAnalyticsTableModel().getTableGrouper().recalcBuckets();
        }

        public Class getBucketClassForColumn(Object columnKey) {
            String beanPath = getBeanPath(columnKey);
            return getBucketClassForColumn(beanPath, false);
        }

        private Class getBucketClassForColumn(String columnKey, boolean cache) {
            Class bucketClass = columnToBucket.get(columnKey);
            if (bucketClass == null) {
                //try from config
                ColumnConfig columnConfigForColumn = getColumnConfigForColumn(columnKey);
                if (columnConfigForColumn != null) {
                    bucketClass = columnConfigForColumn.getDefaultBucketType();
                }
                if (bucketClass == null) {
                    GroupedRowManager.ValueBucket bucketForKey = defaultFactory.createBucketForKey(columnKey);
                    if (bucketForKey != null) bucketClass = bucketForKey.getClass();
                }
                if (bucketClass == null) {
                    bucketClass = GroupedRowManager.UnmergeableDataBucket.class;
                }
                if (cache) columnToBucket.put(columnKey, bucketClass);
            }
            return bucketClass;
        }

        /**
         * Handle pivot generated columns, which should have a bucket type based
         * on the underlying bean path for the pivot data column
         */
        private String getBeanPath(Object columnKey) {
            if ( columnKey instanceof PivotTableModel.GeneratedColumnKey) {
                columnKey = getAnalyticsTableModel().getTablePivoter().getDataColumnKey();
            }
            return columnKey.toString();
        }

        /**
         * @return true if this column contains data items that should not be aggregated
         */
        public boolean isGroupKeyColumn(Object columnKey) {
            Class bucketClass = getBucketClassForColumn(columnKey);
            return bucketClass == null || GroupedRowManager.UnmergeableDataBucket.class.isAssignableFrom(bucketClass);
        }
    }

    private class GrouperAwareDataColumnFilter implements PivotTableModel.DataColumnFilter {
        // It can only be a data column if it is not part of the grouped key columns
        public boolean isValidDataColumn(Object obj) {
            return !grouperBucketFactory.isGroupKeyColumn(obj);
        }
    }

    /**
     * At first sight it's a bit confusing why this class exists even though the CellHighlighter superclass supports
     * chaining of highlighters - after some pondering I think it's done this way to allow the fixed and scrollable table
     * to maintain their own separate chain of highligher instances, while allowing the user to set a shared highlighter instance
     * on the combined table. When this happens, MultiplexingCellHighligher instances are created and used to wrap the shared instance,
     * changing its underlyingHighligher according to whether it's the fixed or scrollable table being rendered.
     */
    public class MultiplexingCellHighlighter extends HighlightedTable.CellHighlighter {
        private final HighlightedTable.CellHighlighter cellHighlighter;

        public MultiplexingCellHighlighter(HighlightedTable.CellHighlighter cellHighlighter, HighlightedTable.CellHighlighter underlyingHighlighter) {
            super(underlyingHighlighter);
            this.cellHighlighter = cellHighlighter;
        }

        public Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
            cellHighlighter.setUnderlyingCellHighlighter(getUnderlyingHighlighter());
            return cellHighlighter.realGetHighlightForCell(row, viewCol, table);
        }

        public void doTableChanged(TableModelEvent e) {
            cellHighlighter.setUnderlyingCellHighlighter(getUnderlyingHighlighter());
            cellHighlighter.tableChanged(e);
        }

        public HighlightedTable.CellHighlighter getMultiplexedHighlighter() {
            return cellHighlighter;
        }
    }

    private class AnalyticsTablePropertyToolTipFactory extends PropertyToolTipFactory {

        private HighlightedTable table;

        public AnalyticsTablePropertyToolTipFactory(HighlightedTable table) {
            this.table = table;
        }

        public String getToolTipText(MouseEvent event) {
            Point point = event.getPoint();
            int columnIndex = table.columnAtPoint(point);
            if (columnIndex != -1) {
                columnIndex = table.convertColumnIndexToModel(columnIndex);
                AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
                Object columnKey = analyticsTableModel.getColumnKey(columnIndex);
                if (columnKey instanceof String) {
                    KeyedColumnTableModel sourceModel = analyticsTableModel.getSourceModel();
                    if (sourceModel instanceof BeanCollectionTableModel) {
                        BeanCollectionTableModel beanCollectionModel = ((BeanCollectionTableModel) sourceModel);
                        Class type = beanCollectionModel.getBeanType();
                        return getToolTipText(type, (String) columnKey, beanCollectionModel.getFormatedCustomColumnName(columnKey));
                    }
                }
            }
            return null;
        }
    }

    public static class CustomCellFormat {
        private Color color;
        private Font font;

        static {
            BeanUtilities.addPersistenceDelegate(CustomCellFormat.class, new BhavayaPersistenceDelegate(new String[]{"color", "font"}));
        }

        public CustomCellFormat() {
        }

        public CustomCellFormat(Color color, Font font) {
            this.color = color;
            this.font = font;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public Color getColor() {
            return color;
        }

        public Font getFont() {
            return font;
        }
    }

    public int[] getCopyPasteSelectedRows() {
        // we favour scrollable one cf fixed but they are always in sync
        return scrollableCopyPasteRowHighlighter.getRows();
    }

    public void setCopyPasteSelectedRows(int[] copyPasteSelectedRows) {

        boolean change = false;
        int oldRows[] = scrollableCopyPasteRowHighlighter.getRows();
        if(copyPasteSelectedRows == null ^ oldRows == null){
            change = true;
        }
        else{
            if(copyPasteSelectedRows != null){
                 if(copyPasteSelectedRows.length != oldRows.length){
                     change = true;
                 }
                else{
                     for(int i=0; i< copyPasteSelectedRows.length; i++){
                        if(copyPasteSelectedRows[i] != oldRows[i]){
                            change = true;
                            break;
                        }
                     }
                 }
            }
        }
        scrollableCopyPasteRowHighlighter.setRows(copyPasteSelectedRows);
        fixedCopyPasteRowHighlighter.setRows(copyPasteSelectedRows);

        if(change){
            TableModelEvent ev = new TableModelEvent(scrollableTable.getModel());
            scrollableTable.tableChanged(ev);
            fixedTable.tableChanged(ev);
        }
    }
    public void clearCopyPasteHighlight() {
        boolean change = scrollableCopyPasteRowHighlighter.getRows() != null;
        scrollableCopyPasteRowHighlighter.setRows(null);
        fixedCopyPasteRowHighlighter.setRows(null);
        if(change){
            repaint();
        }
    }

}