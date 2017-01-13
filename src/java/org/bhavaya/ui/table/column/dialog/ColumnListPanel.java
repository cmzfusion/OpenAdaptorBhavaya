package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.table.ColumnPathRenderer;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.ui.table.formula.FormulaException;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlightDialog;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;
import org.bhavaya.ui.table.formula.conditionalhighlight.HighlightConditionSet;
import org.bhavaya.ui.view.TableView;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Sep-2009
 * Time: 16:18:37
 *
 * A panel containing a table of columns presented as a list, in which the user can reorder the columns
 * using buttons provided, or allocate columns to groups and change column attributes using a right click menu
 */
public class ColumnListPanel extends JPanel {

    private static final int ICON_SIZE = 16;
    private static final int BUTTON_ICON_SIZE = 24;

    private JTable columnListTable;
    private FixedColumnTableColumns fixedColumnTableColumns;
    private ColumnManagementDialogModel columnManagementDialogModel;
    private TableView tableView;

    public ColumnMoveListener columnMoveListener = new ColumnMoveListener(){
        public void columnMoved(int columnId) {}
    };

    public static final String ADD_TO_COL_GROUP_PATH = "addToColumnGroup.png";
    private static final String UP = "column_up.png";
    private static final String DOWN = "column_down.png";
    private static final String REMOVE = "column_delete.png";
    private static final String FIXED_COLUMN_IMAGE_PATH = "fixedColumn.png";
    private static final String SCROLLABLE_COLUMN_IMAGE_PATH = "scrollableColumn.png";
    private static final String NEW_GROUP_PATH = "newColumnGroup.png";
    private static final String REMOVE_FROM_GROUP_PATH = "removeFromColumnGroup.png";
    private static final String SELECT_ALL_PATH = "selectAll.png";
    private static final String COLUMN_SIZE_PATH = "columnSize.png";
    private static final String HIGHLIGHT_IMAGE_PATH = "marker.png";

    public static ImageIcon addToGroupIcon = createIcon(ADD_TO_COL_GROUP_PATH, ICON_SIZE);
    private static ImageIcon newGroupIcon = createIcon(NEW_GROUP_PATH, ICON_SIZE);
    private static ImageIcon removeFromGroupIcon = createIcon(REMOVE_FROM_GROUP_PATH, ICON_SIZE);
    private static ImageIcon selectAllInGroupIcon = createIcon(SELECT_ALL_PATH, ICON_SIZE);
    private static ImageIcon columnSizeIcon = createIcon(COLUMN_SIZE_PATH, ICON_SIZE);
    private static ImageIcon fixedColIcon = createIcon(FIXED_COLUMN_IMAGE_PATH, ICON_SIZE);
    private static ImageIcon scrollableColIcon = createIcon(SCROLLABLE_COLUMN_IMAGE_PATH, ICON_SIZE);
    private static ImageIcon highlightColIcon = createIcon(HIGHLIGHT_IMAGE_PATH, ICON_SIZE);

    public ColumnListPanel(){}
    public ColumnListPanel(TableView tableView, ColumnManagementDialogModel columnManagementDialogModel) {
        this.tableView = tableView;
        this.columnManagementDialogModel = columnManagementDialogModel;
        this.fixedColumnTableColumns = columnManagementDialogModel.getFixedColumnTableColumns();
        columnListTable = new ColumnRenderingJTable(columnManagementDialogModel.getColumnTableModel());

        columnListTable.setSelectionModel(columnManagementDialogModel.getColumnListSelectionModel());
        JScrollPane scrollPane = new JScrollPane(columnListTable);

        Box b = new Box(BoxLayout.Y_AXIS);
        b.add(Box.createVerticalStrut(5));
        b.add(createUpButton());
        b.add(createDownButton());
        b.add(createRemoveButton());
        b.add(Box.createVerticalGlue());

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(b, BorderLayout.EAST);
        setBorder(new TitledBorder("Column Order"));
    }

    public void setColumnMoveListener(ColumnMoveListener columnMoveListener) {
        this.columnMoveListener = columnMoveListener;
    }

    private class ColumnRenderingJTable extends JTable {

        private BooleanIconRenderer fixedOrScrollableRenderer = new BooleanIconRenderer(fixedColIcon);
        private BooleanIconRenderer conditionalHighlightRenderer = new BooleanIconRenderer(highlightColIcon);
        private FixedBorderTableCellRenderer fixedBorderRenderer = new FixedBorderTableCellRenderer();
        private ColumnPathRenderer columnPathRenderer = new ColumnPathRenderer(fixedBorderRenderer);

        public ColumnRenderingJTable(TableModel columnTableModel) {
            super(columnTableModel);
            setShowGrid(false);
            setRowMargin(0);
            getColumnModel().setColumnMargin(0);
            setColumnWidths(ColumnDialogTableModel.FIXED_OR_SCROLLABLE_COLUMN_INDEX, 30);
            setColumnWidths(ColumnDialogTableModel.WIDTH_COLUMN_INDEX, 45);
            setColumnWidths(ColumnDialogTableModel.COLUMN_NAME_COLUMN_INDEX, 115, 400);
            setColumnWidths(ColumnDialogTableModel.GROUP_COLUMN_INDEX, 115, 400);
            setColumnWidths(ColumnDialogTableModel.COLUMN_PATH_COLUMN_INDEX, 400, 800);
            setColumnWidths(ColumnDialogTableModel.HIGHLIGHT_COLUMN_INDEX, 30);
            addMouseListener(new PopupListener());
        }

        private void setColumnWidths(int index, int width) {
            setColumnWidths(index, width, width);
        }

        private void setColumnWidths(int index, int width, int maxWidth) {
            TableColumn c = getColumnModel().getColumn(index);
            c.setWidth(width);
            c.setMaxWidth(maxWidth);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            int modelColIndex = getColumnModel().getColumn(column).getModelIndex();
            switch(modelColIndex) {
                case ColumnDialogTableModel.FIXED_OR_SCROLLABLE_COLUMN_INDEX :
                    return fixedOrScrollableRenderer;
                case ColumnDialogTableModel.COLUMN_PATH_COLUMN_INDEX :
                    return columnPathRenderer;
                case ColumnDialogTableModel.HIGHLIGHT_COLUMN_INDEX :
                    return conditionalHighlightRenderer;
                default :
                    return fixedBorderRenderer;
            }
        }

        class PopupListener extends MouseAdapter {

            public void mousePressed(MouseEvent e) {
              showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
              showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    new ColumnEditingPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        // see bug ID 4127936
        // when the viewport shrinks below the preferred size, stop tracking the viewport width
        public boolean getScrollableTracksViewportWidth() {
            if (autoResizeMode != AUTO_RESIZE_OFF) {
                if (getParent() instanceof JViewport) {
                    return (((JViewport)getParent()).getWidth() > getPreferredSize().width);
                }
            }
            return false;
        }

        // see bug ID 4127936
        // when the viewport shrinks below the preferred size, return the minimum size
        // so that scrollbars will be shown
        public Dimension getPreferredSize() {
            if (getParent() instanceof JViewport) {
                if (((JViewport)getParent()).getWidth() < super.getPreferredSize().width) {
                    return getMinimumSize();
                }
            }
            return super.getPreferredSize();
        }
    }

    private JButton createUpButton() {
        final JButton upButton = new JButton(createIcon(UP, BUTTON_ICON_SIZE));
        upButton.setToolTipText("Move selected columns up/left");
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                columnManagementDialogModel.moveSelectionsUp();
                int selectionIndex = columnListTable.getSelectionModel().getMinSelectionIndex();
                scrollToSelection(selectionIndex);
                columnMoveListener.columnMoved(selectionIndex);
            }
        }
        );
        return upButton;
    }

    private void scrollToSelection(int index) {
        UIUtilities.scrollToCenter(columnListTable, index, 0);
    }

    private JButton createDownButton() {
        final JButton downButton = new JButton(createIcon(DOWN, BUTTON_ICON_SIZE));
        downButton.setToolTipText("Move selected columns down/right");
        downButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                columnManagementDialogModel.moveSelectionsDown();
                int selectionIndex = columnListTable.getSelectionModel().getMaxSelectionIndex();
                scrollToSelection(selectionIndex);
                columnMoveListener.columnMoved(selectionIndex);                
            }
        }
        );
        return downButton;
    }

    private JButton createRemoveButton() {
        final JButton removeButton = new JButton(createIcon(REMOVE, BUTTON_ICON_SIZE));
        removeButton.setToolTipText("Remove selected columns");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                columnManagementDialogModel.removeSelections();
            }
        }
        );
        return removeButton;
    }

    public static ImageIcon createIcon(String url, int iconSize) {
        ImageIcon i = ImageIconCache.getImageIcon(url);
        Image resizedImage = i.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_DEFAULT);
        return new ImageIcon(resizedImage);
    }

    private class BooleanIconRenderer extends FixedBorderTableCellRenderer {
        private ImageIcon icon;

        public BooleanIconRenderer(ImageIcon icon) {
            this.icon = icon;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setIcon(toBoolean(value) ? icon : null);
            setText("");
            return this;
        }

        private boolean toBoolean(Object value) {
            return value != null && value instanceof Boolean && (Boolean)value;
        }
    }

    private class FixedBorderTableCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //add a border at the bottom of the fixed cols
            setBorder(row == fixedColumnTableColumns.getFixedColumnCount() - 1 ? new BaseBorder() : null);
            return this;
        }
    }

    private class BaseBorder extends EmptyBorder {
        public BaseBorder() {
            super(0, 0, 2, 0);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color oldColor = g.getColor();
            g.setColor(Color.gray);
            g.drawLine(x, y + height - 2, x + width, y + height - 2);
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
            g.setColor(oldColor);
        }
    }

    private class ColumnEditingPopupMenu extends JPopupMenu {

        public ColumnEditingPopupMenu() {
            java.util.List<HidableTableColumn> selectedCols = columnManagementDialogModel.getSelectedColumns();
            add(new AddToNewGroupAction(selectedCols));

            JMenu addToExistingGroupMenu = new JMenu("Add to group");
            addToExistingGroupMenu.setIcon(addToGroupIcon);

            SortedSet<ColumnGroup> groupNames = fixedColumnTableColumns.getColumnGroups();
            for ( ColumnGroup group : groupNames) {
                addToExistingGroupMenu.add( new AddToExistingGroupAction(group, selectedCols));
            }
            add(addToExistingGroupMenu);
            if ( groupNames.size() == 0) {
                addToExistingGroupMenu.setEnabled(false);
            }

            add(new RemoveFromGroupAction(selectedCols));
            add(new SelectAllInGroupAction(selectedCols));
            add(new JSeparator());
            add(new SetColumnSizeAction(selectedCols));
            add(new FixOrUnfixColumnAction(selectedCols));
            add(new EditConditionalColumnHighlightAction(selectedCols));
        }
    }

    private class AddToNewGroupAction extends AbstractAddToGroupAction {

        public AddToNewGroupAction(java.util.List<HidableTableColumn> columns) {
            super("New group", newGroupIcon, columns);
        }

        public void actionPerformed(ActionEvent e) {
            String newGroupName = JOptionPane.showInputDialog(
                ColumnListPanel.this, "Name for new Group?", "New Group"
            );

            //prevent adding a duplicate group
            ColumnGroup newGroup = fixedColumnTableColumns.containsColumnGroupWithName(newGroupName) ?
                fixedColumnTableColumns.getColumnGroup(newGroupName) :
                new ColumnGroup(newGroupName);
            addSelectedColumnsToGroup(newGroup);
        }
    }

    private class FixOrUnfixColumnAction extends AbstractAction {

        private final int FIXED_COLS = 1;
        private final int SCROLLABLE_COLS = 2;
        private java.util.List<HidableTableColumn> columns;
        public int columnTypes;

        public FixOrUnfixColumnAction(java.util.List<HidableTableColumn> columns) {
            this.columns = columns;
            calculateColumnTypes(columns);
            switch ( columnTypes ) {
                case 1 :
                    putValue(Action.NAME, "Unfix Columns");  //just fixed selected
                    putValue(Action.SMALL_ICON, scrollableColIcon);
                    break;
                case 2 :
                    putValue(Action.NAME, "Fix Columns"); // just scrollable selected
                    putValue(Action.SMALL_ICON, fixedColIcon);
                    break;
                default :
                    putValue(Action.NAME, "Fix Columns");  //mix selected, here we show the action disabled
                    putValue(Action.SMALL_ICON, fixedColIcon);
                    setEnabled(false);
            }
        }

        private void calculateColumnTypes(java.util.List<HidableTableColumn> columns) {
            for ( HidableTableColumn c : columns) {
                columnTypes |= fixedColumnTableColumns.isTableModelColumnInFixedTable(c.getModelIndex())
                        ? FIXED_COLS : SCROLLABLE_COLS;
            }
        }

        public void actionPerformed(ActionEvent e) {
            for ( HidableTableColumn column : columns) {
                fixedColumnTableColumns.setColumnFixed(
                    column.getModelIndex(),
                    columnTypes == SCROLLABLE_COLS
                );
            }
        }
    }

    private class AddToExistingGroupAction extends AbstractAddToGroupAction {

        private ColumnGroup groupName;

        public AddToExistingGroupAction(ColumnGroup group, java.util.List<HidableTableColumn> columns) {
            super(group.getGroupName(), null, columns);
            this.groupName = group;
        }

        public void actionPerformed(ActionEvent e) {
            addSelectedColumnsToGroup(groupName);
        }
    }

    private class SelectAllInGroupAction extends AbstractAction {

        private ColumnGroup group;

        public SelectAllInGroupAction(java.util.List<HidableTableColumn> columns) {
            super("Select all in group", selectAllInGroupIcon);
            Set<ColumnGroup> g = new HashSet<ColumnGroup>();
            for (HidableTableColumn c : columns) {
                if ( c.getColumnGroup() != null) {
                    g.add(c.getColumnGroup());
                    group = c.getColumnGroup();
                }
            }
            if ( g.size() != 1) {
                setEnabled(false);
            }
        }

        public void actionPerformed(ActionEvent e) {
            columnManagementDialogModel.selectRowsWithGroup(group);
        }
    }

    private class RemoveFromGroupAction extends AbstractAddToGroupAction {

        public RemoveFromGroupAction(java.util.List<HidableTableColumn> columns) {
            super("Remove from group", removeFromGroupIcon, columns);
            boolean containsGroups = false;
            for ( HidableTableColumn c : columns) {
                if ( c.getColumnGroup() != null) {
                    containsGroups = true;
                    break;
                }
            }
            setEnabled(containsGroups);
        }

        public void actionPerformed(ActionEvent e) {
            addSelectedColumnsToGroup(null);
        }
    }

    private abstract class AbstractAddToGroupAction extends AbstractAction {

        private java.util.List<HidableTableColumn> columns;

        public AbstractAddToGroupAction(String name, Icon i, java.util.List<HidableTableColumn> columns) {
            super(name, i);
            this.columns = columns;
        }

        protected void addSelectedColumnsToGroup(ColumnGroup group) {
            for ( HidableTableColumn column : columns ) {
                column.setColumnGroup(group);
            }
            fixedColumnTableColumns.refreshColumnGroups();
            columnListTable.repaint();
        }
    }

    public class SetColumnSizeAction extends AbstractAction {

        private java.util.List<HidableTableColumn> columns;

        public SetColumnSizeAction(java.util.List<HidableTableColumn> columns) {
            super("Set column size", columnSizeIcon);
            this.columns = columns;
        }

        public void actionPerformed(ActionEvent e) {
            String newSizeString = JOptionPane.showInputDialog(columnListTable, "New Size (px)?", columns.get(0).getWidth());

            if ( newSizeString != null) {
                int newSize = Integer.parseInt(newSizeString);
                for ( HidableTableColumn column : columns ) {
                    column.setPreferredWidth(newSize);
                }
                columnListTable.repaint();
            }
        }
    }

    private class EditConditionalColumnHighlightAction extends AbstractAction {
        private HidableTableColumn column;
        private ConditionalHighlighter conditionalHighlighter = tableView.getConditionalHighlighter();

        public EditConditionalColumnHighlightAction(java.util.List<HidableTableColumn> columns) {
            if(columns != null && columns.size() == 1) {
                column = columns.get(0);
            }
            putValue(Action.NAME, "Edit Conditional Column Colour");
            putValue(Action.SMALL_ICON, highlightColIcon);
        }

        @Override
        public boolean isEnabled() {
            return column != null && conditionalHighlighter.isConditionalHighlightingPermitted(getColumnIdentifier());
        }

        private String getColumnIdentifier() {
            return column.getIdentifier().toString();
        }

        public void actionPerformed(ActionEvent e) {
            final String columnIdentifier = getColumnIdentifier();
            HighlightConditionSet highlightConditionSet = conditionalHighlighter.getColumnCondition(columnIdentifier);
            try {
                new ConditionalHighlightDialog(UIUtilities.getFrameParent(ColumnListPanel.this), tableView, columnIdentifier, highlightConditionSet);
                columnListTable.tableChanged(new TableModelEvent(columnListTable.getModel(), columnListTable.getSelectedRow()));
            } catch (FormulaException e1) {
                AlwaysOnTopJOptionPane.showMessageDialog(ColumnListPanel.this, "Unable to open conditional highlight editor:\n" + e1.getMessage(),
                        "Error Opening Conditional HighlightEditor", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public static interface ColumnMoveListener {
        void columnMoved(int columnId);
    }
}
