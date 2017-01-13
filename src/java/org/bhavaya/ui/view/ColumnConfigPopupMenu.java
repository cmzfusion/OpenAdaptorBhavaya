package org.bhavaya.ui.view;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.*;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.DecimalTextField;
import org.bhavaya.ui.series.DateSeriesNew;
import org.bhavaya.ui.series.DateSeriesDialog;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.table.formula.FormulaException;
import org.bhavaya.ui.table.formula.SymbolMappings;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlightDialog;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;
import org.bhavaya.ui.table.formula.FormulaUtils;
import org.bhavaya.ui.table.formula.conditionalhighlight.HighlightConditionSet;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 16-Oct-2008
 * Time: 15:39:58
 *
 * This class contains column popup menu logic extracted from its previous location in TableView,
 * (thereby reducing TableView to a mere 45 screens worth on a 1024 pixel monitor. Phew)
 *
 * I added extra menu items to enable 'locking' of pivot columns
 */
public class ColumnConfigPopupMenu extends JPopupMenu {

    private static final Log log = Log.getCategory(ColumnConfigPopupMenu.class);

    private Class columnClass;
    private int modelIndex;
    private Object columnKey;
    private boolean pivotGeneratedCol;
    private AnalyticsTableModel analyticsTableModel;
    private AnalyticsTable analyticsTable;
    private BeanCollection beanCollection;
    private BeanCollectionTableModel beanCollectionTableModel;
    private Class recordType;
    private TableView tableView;

    public ColumnConfigPopupMenu(Class columnClass, int modelIndex, Object columnKey, boolean pivotGeneratedCol, AnalyticsTableModel analyticsTableModel, AnalyticsTable analyticsTable, BeanCollection beanCollection, Class recordType, TableView tableView) {
        this.columnClass = columnClass;
        this.modelIndex = modelIndex;
        this.columnKey = columnKey;
        this.pivotGeneratedCol = pivotGeneratedCol;
        this.analyticsTableModel = analyticsTableModel;
        this.analyticsTable = analyticsTable;
        this.beanCollection = beanCollection;
        this.beanCollectionTableModel = tableView.getBeanCollectionTableModel();
        this.recordType = recordType;
        this.tableView = tableView;
        addMenuItems();
    }

    private void addMenuItems() {
        addRemoveColumnMenuItem();
        addFixMenuItem();
        addLockPivotColumnMenuItems();

        addChangeDisplayNameMenuItem();

        ColumnConfig columnConfigGroup = addRenderFactoryMenuItems();
        addGroupingTypeMenuItem(columnConfigGroup);
        addConversionsMenuItems(columnConfigGroup);

        addNumericMenuItems();
        addSortingMenuItems();
        addDateSeriesMenuItems();
        addSeparator();

        addColumnNameMenuItems();
        addFontMenuItems();
        addColumnColourMenuItems();
        addFormulaMenuItems();
    }

    protected void addLockPivotColumnMenuItems() {
        if ( pivotGeneratedCol) {
            PivotTableModel pivoter = analyticsTableModel.getTablePivoter();
            PivotTableModel.GeneratedColumnKey key = (PivotTableModel.GeneratedColumnKey)columnKey;
            if (  pivoter.isLockablePivotColumn(key)) {
                boolean locked = pivoter.isLockedPivotColumn(key);
                Action lockAction = locked ?
                        new UnlockPivotColumnAction(key) :
                        new LockPivotColumnAction(key);

                JCheckBoxMenuItem lockPivotColumnItem = new JCheckBoxMenuItem(lockAction);
                lockPivotColumnItem.setSelected(pivoter.isLockedPivotColumn(key));
                add(lockPivotColumnItem);
            }
        }
    }

    private class LockPivotColumnAction extends AuditedAbstractAction {
        private PivotTableModel.GeneratedColumnKey pivotColumnValue;

        protected LockPivotColumnAction(PivotTableModel.GeneratedColumnKey pivotColumnValue) {
            super("Lock Pivot Column", "Lock Pivot Column " + pivotColumnValue);
            this.pivotColumnValue = pivotColumnValue;
        }

        protected void auditedActionPerformed(ActionEvent e) {
            analyticsTableModel.getTablePivoter().addLockedPivotColumn(pivotColumnValue);
        }
    }

    private class UnlockPivotColumnAction extends AuditedAbstractAction {
        private PivotTableModel.GeneratedColumnKey pivotColumnValue;

        protected UnlockPivotColumnAction(PivotTableModel.GeneratedColumnKey pivotColumnValue) {
            super("Lock Pivot Column", "UnLock Pivot Column " + pivotColumnValue);
            this.pivotColumnValue = pivotColumnValue;
        }

        protected void auditedActionPerformed(ActionEvent e) {
            analyticsTableModel.getTablePivoter().removeLockedPivotColumn(pivotColumnValue);
        }
    }

    protected void addColumnColourMenuItems() {
        JMenuItem editColumnColour = new JMenuItem("Edit Column Colour...");
        editColumnColour.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color currentColour = analyticsTable.getColumnColor(columnKey);
                Color color = JColorChooser.showDialog(analyticsTable, "Column Colour", currentColour);
                if (color != null) {
                    analyticsTable.setColumnColor(columnKey, color);
                }
            }
        });
        add(editColumnColour);

        if (analyticsTable.getColumnColor(columnKey) != null) {
            JMenuItem clearColumnColor = new JMenuItem(new AuditedAbstractAction("Clear column colour") {
                public void auditedActionPerformed(ActionEvent e) {
                    analyticsTable.setColumnColor(columnKey, null);
                }
            });
            add(clearColumnColor);
        }

        final ConditionalHighlighter conditionalHighlighter = tableView.getConditionalHighlighter();
        final String columnIdentifier = columnKey.toString();
        if(conditionalHighlighter.isConditionalHighlightingPermitted(columnIdentifier)) {
            JMenuItem editConditionalColumnColour = new JMenuItem("Edit Conditional Column Colour...");
            editConditionalColumnColour.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    HighlightConditionSet highlightConditionSet = conditionalHighlighter.getColumnCondition(columnIdentifier);
                    try {
                        new ConditionalHighlightDialog(UIUtilities.getFrameParent(analyticsTable), tableView, columnIdentifier, highlightConditionSet);
                    } catch (FormulaException e1) {
                        AlwaysOnTopJOptionPane.showMessageDialog(getComponent(), "Unable to open conditional highlight editor:\n" + e1.getMessage(),
                                "Error Opening Conditional HighlightEditor", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            add(editConditionalColumnColour);
        }
    }

    protected void addFontMenuItems() {
        Integer columnFontStyle = analyticsTable.getColumnFont(columnKey);
        int style = (columnFontStyle == null) ? analyticsTable.getFont().getStyle() : columnFontStyle.intValue();
        JMenu fontSizeMenu = new JMenu("Column Font Style");
        JCheckBoxMenuItem boldMenuItem = new JCheckBoxMenuItem(new ToggleColumnFontStyleAction("Bold", Font.BOLD, columnKey));
        boldMenuItem.setSelected((style & Font.BOLD) != 0);
        JCheckBoxMenuItem italicsMenuItem = new JCheckBoxMenuItem(new ToggleColumnFontStyleAction("Italic", Font.ITALIC, columnKey));
        italicsMenuItem.setSelected((style & Font.ITALIC) != 0);
        fontSizeMenu.add(boldMenuItem);
        fontSizeMenu.add(italicsMenuItem);

        add(fontSizeMenu);
    }

    protected void addFormulaMenuItems() {
        final String beanPath = columnKey.toString();
        final SymbolMappings symbolMappings = beanCollectionTableModel.getFormulaManager().getSymbolMappings();
        Set<String> symbols = symbolMappings.getSymbolForBeanPath(beanPath);
        if(symbols == null || symbols.isEmpty()) {
            JMenuItem addColumnToFormula = new JMenuItem("Add Column To Formula Symbols");
            addColumnToFormula.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    symbolMappings.addSymbolMapping(beanPath);
                }
            });
            add(addColumnToFormula);
        }
    }

    private class ToggleColumnFontStyleAction extends ToggleFontStyleAction {
        Object columnKey;

        public ToggleColumnFontStyleAction(String name, int style, Object columnKey) {
            super(name, style);
            this.columnKey = columnKey;
        }

        public void auditedActionPerformed(ActionEvent e) {
            Integer fontStyle = analyticsTable.getColumnFont(columnKey);
            int style = (fontStyle == null) ? analyticsTable.getFont().getStyle() : fontStyle.intValue();
            analyticsTable.setColumnFont(columnKey, new Integer(style ^ getStyle()));
        }
    }

    protected void addColumnNameMenuItems() {
        if (columnKey instanceof String && !pivotGeneratedCol) {
            JMenuItem editColumnName = new JMenuItem("Edit Column Name...");
            editColumnName.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TableColumn colConfig = analyticsTable.getColumn(columnKey);
                    String originalName = analyticsTableModel.getColumnName(colConfig.getModelIndex());
                    String currentColumnName = beanCollectionTableModel.getCustomColumnName(columnKey);
                    if (currentColumnName == null) {
                        currentColumnName = originalName;
                    }
                    int index = currentColumnName.lastIndexOf("\n");
                    if (index > -1) {
                        currentColumnName = currentColumnName.substring(index, currentColumnName.length());
                    }
                    currentColumnName = currentColumnName.trim();
                    String originalNameNoNewLine = originalName.replaceAll("\n", " - ");
                    String newColumnName = JOptionPane.showInputDialog(analyticsTable,
                            "Enter a new Column Name or leave empty to restore original name \n \t \t --> " + originalNameNoNewLine, currentColumnName);

                    if (newColumnName != null) {
                        if (newColumnName.length() == 0) {
                            // user is trying to remove his custom name
                            beanCollectionTableModel.removeCustomColumnName(columnKey);
                            colConfig.setHeaderValue(originalName);
                            analyticsTable.repaintHeaders();
                        } else if (!currentColumnName.equals(newColumnName)) {
                            newColumnName = newColumnName.trim();
                            beanCollectionTableModel.addCustomColumnName(columnKey, newColumnName);
                            colConfig.setHeaderValue(beanCollectionTableModel.getCustomColumnName(colConfig.getModelIndex()));
                            analyticsTable.repaintHeaders();
                        }
                    }
                }
            });
            add(editColumnName);
        }
    }

    protected void addDateSeriesMenuItems() {
        if (analyticsTableModel.isUsingDateSeries()) {
            JMenuItem editDateSeries = new JMenuItem("Edit DateSeries...");
            editDateSeries.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DateSeriesNew dateSeries = analyticsTableModel.getDateSeries();
                    DateSeriesDialog dateSeriesDialog = new DateSeriesDialog(UIUtilities.getFrameParent(analyticsTable), (DateSeriesNew) BeanUtilities.verySlowDeepCopy(dateSeries));
                    UIUtilities.centreInContainer(analyticsTable, dateSeriesDialog, 0, 0);
                    dateSeriesDialog.setVisible(true);
                    if (dateSeriesDialog.getDateSeries() != null) {
                        analyticsTableModel.setDateSeries(dateSeriesDialog.getDateSeries());
                        tableView.fireViewChanged();
                    }
                }
            });
            add(editDateSeries);
        }
    }

    protected void addSortingMenuItems() {
        if (analyticsTableModel.canSort(columnKey)) {
            if (!analyticsTableModel.isSortingColumn(columnKey) || analyticsTableModel.isSortDescendingColumn(columnKey)) {
                JMenuItem sortAscending = new JMenuItem("Sort ascending");
                sortAscending.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        analyticsTableModel.addSortingColumn(columnKey, false);
                        analyticsTable.repaintHeaders();
                    }
                });
                add(sortAscending);
            }

            if (!analyticsTableModel.isSortingColumn(columnKey) || !analyticsTableModel.isSortDescendingColumn(columnKey)) {
                JMenuItem sortDescending = new JMenuItem("Sort descending");
                sortDescending.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        analyticsTableModel.addSortingColumn(columnKey, true);
                        analyticsTable.repaintHeaders();
                    }
                });
                add(sortDescending);
            }

            if (analyticsTableModel.isSortingColumn(columnKey)) {
                JMenuItem removeSort = new JMenuItem("Remove sorting");
                removeSort.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        analyticsTableModel.removeSortingColumn(columnKey);
                        analyticsTable.repaintHeaders();
                    }
                });
                add(removeSort);
            }
        }

        if (analyticsTableModel.isSorting()) {
            JMenuItem cancelSort = new JMenuItem("Cancel all sorting");
            cancelSort.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    analyticsTableModel.cancelSorting();
                    analyticsTable.repaintHeaders();
                }
            });
            add(cancelSort);
        }
    }

    protected void addNumericMenuItems() {
        if (Number.class.isAssignableFrom(columnClass) || Numeric.class.isAssignableFrom(columnClass)) {
            JCheckBoxMenuItem blankZero = new JCheckBoxMenuItem(new AuditedAbstractAction("Show zero as blank") {
                public void auditedActionPerformed(ActionEvent e) {
                    JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
                    analyticsTable.setShowZeroAsBlank(columnKey, checkBoxMenuItem.isSelected());
                }
            });
            blankZero.setSelected(analyticsTable.isShowZeroAsBlank(columnKey));
            add(blankZero);

            JCheckBoxMenuItem blankNaN = new JCheckBoxMenuItem(new AuditedAbstractAction("Show ? as blank") {
                public void auditedActionPerformed(ActionEvent e) {
                    JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
                    analyticsTable.setShowNaNAsBlank(columnKey, checkBoxMenuItem.isSelected());
                }
            });
            blankNaN.setSelected(analyticsTable.isShowNaNAsBlank(columnKey));
            add(blankNaN);

            if (beanCollectionTableModel.isColumnSettable(columnKey)) {

                JCheckBoxMenuItem selectAfterDecimal = new JCheckBoxMenuItem(new AuditedAbstractAction("Select After Decimal") {
                    public void auditedActionPerformed(ActionEvent e) {
                        JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
                        analyticsTable.setSelectAfterDecimal(columnKey, checkBoxMenuItem.isSelected());
                    }
                });
                selectAfterDecimal.setSelected(analyticsTable.isSelectAfterDecimal(columnKey));
                add(selectAfterDecimal);

                JMenuItem setConstraint = new JMenuItem(new DecimalThresholdSetConstraintAction(columnKey));
                add(setConstraint);
            }
        }
    }

    private class DecimalThresholdSetConstraintAction extends AuditedAbstractAction {
        private final Object columnKey;

        public DecimalThresholdSetConstraintAction(Object columnKey) {
            super("Column Constraint...");
            this.columnKey = columnKey;
        }

        protected void auditedActionPerformed(ActionEvent e) {
            SetConstraintHandler setConstraintHandler = SetConstraintHandler.getInstance(beanCollection.getType());
            SetConstraint currentContraint = setConstraintHandler.getColumnSetConstraint((String) columnKey);

            if (currentContraint == null || currentContraint instanceof ThresholdSetConstraint) {

                String message = "<html>Enter a new Column Constraint or <br>leave empty to restore original behaviour</html>";
                JLabel label = new JLabel(message, JLabel.LEFT);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                label.setFocusable(false);

                DecimalTextField thresholdTextField = new DecimalTextField("#,##0.00######", 20);
                if (currentContraint != null) {
                    thresholdTextField.setValue(((ThresholdSetConstraint) currentContraint).getThreshold());
                }
                thresholdTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
                label.setPreferredSize(new Dimension((int) thresholdTextField.getPreferredSize().getWidth(), (int) label.getPreferredSize().getHeight()));

                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(label);
                box.add(Box.createVerticalStrut(5));
                box.add(thresholdTextField);

                JOptionPane pane = new JOptionPane(box, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                JDialog dialog = pane.createDialog(analyticsTable, "Conlumn Constraint");
                dialog.setVisible(true);
                dialog.dispose();
                Object selectedValue = pane.getValue();
                boolean change = (selectedValue != null && selectedValue instanceof Integer && ((Integer) selectedValue).intValue() == JOptionPane.OK_OPTION);

                if (change) {
                    Number value = thresholdTextField.getValue();
                    if (value == null || Double.isNaN(value.doubleValue())) {
                        setConstraintHandler.removeColumnSetConstraint((String) columnKey);
                    } else {
                        if (currentContraint == null) {
                            currentContraint = new ThresholdSetConstraint(value.doubleValue());
                            setConstraintHandler.addColumnSetConstraint((String) columnKey, currentContraint);
                        } else {
                            ((ThresholdSetConstraint) currentContraint).setThreshold(value.doubleValue());
                        }

                    }

                }
            }
        }
    }

    protected void addConversionsMenuItems(ColumnConfig columnConfigGroup) {
        if (columnConfigGroup != null && columnConfigGroup.getCellValueTransforms() != null && columnConfigGroup.getCellValueTransforms().length > 0) {
            JMenu transformConfigMenu = new JMenu("Conversions");
            Class userTransformerType = analyticsTable.getColumnTransformClass(columnKey);

            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(new SetTransformAction(columnKey, null));
            transformConfigMenu.add(menuItem);
            menuItem.setSelected(userTransformerType == null);

            for (int i = 0; i < columnConfigGroup.getCellValueTransforms().length; i++) {
                Class transformerType = columnConfigGroup.getCellValueTransforms()[i];
                menuItem = new JRadioButtonMenuItem(new SetTransformAction(columnKey, transformerType));
                transformConfigMenu.add(menuItem);

                if (Utilities.equals(userTransformerType, transformerType)) {
                    menuItem.setSelected(true);
                }
            }

            add(transformConfigMenu);
        }
    }

    protected void addGroupingTypeMenuItem(ColumnConfig columnConfigGroup) {
        if (columnConfigGroup != null && columnConfigGroup.getBucketTypes() != null && columnConfigGroup.getBucketTypes().length > 1) {
            JMenu bucketConfigMenu = new JMenu("Grouping type");
            Class userBucketType = analyticsTable.getColumnBucketClass(columnKey);
            for (int i = 0; i < columnConfigGroup.getBucketTypes().length; i++) {
                Class bucketType = columnConfigGroup.getBucketTypes()[i];
                JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(new SetBucketAction(columnKey, bucketType));
                bucketConfigMenu.add(menuItem);

                if (bucketType == null) {
                    if (Utilities.equals(columnConfigGroup.getDefaultBucketType(), bucketType))
                        menuItem.setSelected(true);
                } else if (Utilities.equals(userBucketType, bucketType)) {
                    menuItem.setSelected(true);
                }
            }

            add(bucketConfigMenu);
        }
    }


    private class SetBucketAction extends AuditedAbstractAction {
        private Object columnKey;
        private Class bucketClass;

        public SetBucketAction(Object columnKey, Class bucketClass) {
            this.columnKey = columnKey;
            this.bucketClass = bucketClass;

            String bucketName;
            try {
                bucketName = bucketClass.newInstance().toString();
            } catch (Exception e) {
                log.error("Could not instantiate " + bucketClass, e);
                bucketName = bucketClass.toString();
            }

            putValue(Action.NAME, bucketName);
        }

        public void auditedActionPerformed(ActionEvent e) {
            analyticsTable.setColumnBucketClass(columnKey, bucketClass);
        }
    }

    protected ColumnConfig addRenderFactoryMenuItems() {
        ColumnConfig columnConfigGroup = analyticsTable.getColumnConfigForColumn(columnKey);
        if (columnConfigGroup != null) {
            TableCellRendererFactory rendererFactory = columnConfigGroup.getRendererFactory();
            if (rendererFactory != null) {
                String[] parameterNames = rendererFactory.getParameterNames();

                String userColumnRendererId = analyticsTable.getColumnRendererId(columnKey);
                Map currentParameterMap = rendererFactory.convertIdStringToParameterMap(userColumnRendererId);

                for (int nameIdx = 0; nameIdx < parameterNames.length; nameIdx++) {
                    String parameterName = parameterNames[nameIdx];
                    JMenu columnConfigMenu = new JMenu(parameterName);

                    Object[] values = rendererFactory.getAllowedValuesForParameter(parameterName);
                    Object oldValue = currentParameterMap.get(parameterName);
                    for (int valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        Object value = values[valueIdx];
                        currentParameterMap.put(parameterName, value);
                        String newRendererId = rendererFactory.convertParameterMapToIdString(currentParameterMap);
                        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(new SetRendererAction(columnKey, value.toString(), newRendererId));
                        menuItem.setSelected(Utilities.equals(oldValue, value));
                        columnConfigMenu.add(menuItem);
                        //put the old value back again
                        currentParameterMap.put(parameterName, oldValue);
                    }

                    add(columnConfigMenu);
                }
            }
        }
        return columnConfigGroup;
    }

    private class SetRendererAction extends AuditedAbstractAction {
        private Object columnKey;
        private String columnRendererId;

        public SetRendererAction(Object columnKey, String name, String columnRendererId) {
            this.columnKey = columnKey;
            this.columnRendererId = columnRendererId;
            putValue(Action.NAME, name);
        }

        public void auditedActionPerformed(ActionEvent e) {
            //if column is a pivot column, then set all of them to the same precision
            if (columnKey instanceof PivotTableModel.GeneratedColumnKey) {

                for (Iterator iterator = analyticsTable.getColumns(); iterator.hasNext();) {
                    TableColumn modelColumn = (TableColumn) iterator.next();
                    Object testColumnKey = analyticsTableModel.getColumnKey(modelColumn.getModelIndex());

                    if (testColumnKey instanceof PivotTableModel.GeneratedColumnKey) {
                        if ( analyticsTable.getColumnConfigForColumn(testColumnKey) == analyticsTable.getColumnConfigForColumn(columnKey)) {
                            analyticsTable.setColumnRendererId(testColumnKey, columnRendererId);
                        }
                    }
                }
            } else {
                analyticsTable.setColumnRendererId(columnKey, columnRendererId);
            }
        }
    }

    private class SetTransformAction extends AuditedAbstractAction {
        private Object columnKey;
        private Class transformClass;

        public SetTransformAction(Object columnKey, Class transformClass) {
            this.columnKey = columnKey;
            this.transformClass = transformClass;

            String transformName;
            try {
                transformName = transformClass == null ? "None" : transformClass.newInstance().toString();
            } catch (Exception e) {
                log.error("Could not instantiate " + transformClass, e);
                transformName = transformClass.toString();
            }

            putValue(Action.NAME, transformName);
        }

        public void auditedActionPerformed(ActionEvent e) {
            analyticsTable.setColumnTransformClass(columnKey, transformClass);
        }
    }

    protected void addChangeDisplayNameMenuItem() {
        if (columnKey instanceof String && !pivotGeneratedCol && !FormulaUtils.isFormulaPath((String) columnKey)) {
            String locator = (String) columnKey;
            String[] path = Generic.beanPathStringToArray(locator);
            String customColumnName = beanCollectionTableModel.getFormatedCustomColumnName(columnKey);
            if (path.length > 1) {
                JMenu displayNameMenu = new JMenu("Display Name");
                for (int i = 0; i < path.length; i++) {
                    JMenuItem changeNameItem = new JMenuItem(new SetColumnNameDepth(locator, i + 1, customColumnName));
                    displayNameMenu.add(changeNameItem);
                }
                add(displayNameMenu);
            }
        }
    }

    private class SetColumnNameDepth extends AuditedAbstractAction {
        private final String locator;
        private final int depth;
        private final String customName;

        public SetColumnNameDepth(String locator, int depth, String customName) {
            this.locator = locator;
            this.depth = depth;
            this.customName = customName;
            putValue(Action.NAME, createMenuDisplayName());
        }

        public void auditedActionPerformed(ActionEvent e) {
            beanCollectionTableModel.setColumnNameDepth(locator, depth);
        }

        private String createMenuDisplayName() {
            String[] locatorPath = Generic.beanPathStringToArray(locator);
            StringBuffer menuDisplayNameBuffer = new StringBuffer();
            for (int i = 0; i < depth; i++) {
                if (i > 0) {
                    menuDisplayNameBuffer.insert(0, " - ");
                }
                Attribute attribute = PropertyModel.getInstance(recordType).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - i));

                Class parentClass;
                if (locatorPath.length - i > 1) {
                    Attribute parentAttribute = PropertyModel.getInstance(recordType).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - i - 1));
                    parentClass = parentAttribute.getType();
                } else {
                    parentClass = recordType;
                }

                if (parentClass != null) {
                    parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(attribute.getName());
                }
                menuDisplayNameBuffer.insert(0, PropertyModel.getInstance(parentClass).getDisplayName(attribute.getName()));
            }
            if (customName != null) {
                menuDisplayNameBuffer.append(" ");
                menuDisplayNameBuffer.append(customName);
            }
            return menuDisplayNameBuffer.toString();
        }
    }

    protected void addFixMenuItem() {
        if (!pivotGeneratedCol) {
            JCheckBoxMenuItem fixColumnMenuItem = new JCheckBoxMenuItem("Fix Column");
            final boolean columnFixed = analyticsTable.isColumnFixed(modelIndex);
            fixColumnMenuItem.setSelected(columnFixed);
            fixColumnMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    analyticsTable.setColumnFixed(modelIndex, !columnFixed);
                }
            });
            add(fixColumnMenuItem);
        }
    }

    protected void addRemoveColumnMenuItem() {
        if (!pivotGeneratedCol) {
            JMenuItem hideColumnMenuItem = new JMenuItem("Remove Column");
            hideColumnMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String locator = analyticsTableModel.getBeanPathForColumn(columnKey);
                    if (locator != null) {
                        beanCollectionTableModel.setColumnVisibleByLocator(locator, false);
                    }
					//removed column may have been the only one in a group
                    analyticsTable.refreshColumnGroups();
                    setVisible(false);
                }
            });
            add(hideColumnMenuItem);
        }
    }

    public static abstract class ToggleFontStyleAction extends AuditedAbstractAction {
        private int style;

        public ToggleFontStyleAction(String name, int style) {
            super(name, "Toggle Font Style");
            this.style = style;
        }

        public int getStyle() {
            return style;
        }

        public abstract void auditedActionPerformed(ActionEvent e);

    }

    public Class getColumnClass() {
        return columnClass;
    }

    public Object getColumnKey() {
        return columnKey;
    }
}
