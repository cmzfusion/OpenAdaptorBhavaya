package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.ToolBarGroup;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.SortedSet;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Sep-2009
 * Time: 16:23:22
 *
 * Shows column group buttons for the set of column groups, which can be used to show or hide
 * groups of columns in the table
 */
public class GroupListPanel extends JPanel {

    private JToolBar toolBar = new JToolBar();
    private ColumnHidingToolBarGroup toolBarGroup;

    public GroupListPanel(){}
    public GroupListPanel(ColumnManagementDialogModel columnManagementDialogModel) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(toolBar);
        add(Box.createHorizontalGlue());
        toolBar.setFloatable(false);

        FixedColumnTableColumns fixedTableColumns = columnManagementDialogModel.getFixedColumnTableColumns();
        toolBarGroup = new ColumnHidingToolBarGroup(fixedTableColumns);
        rebuildToolbar();

        setBorder(new TitledBorder("Column Groups"));
        setPreferredSize(new Dimension(getPreferredSize().width, 60));
        
        fixedTableColumns.addColumnGroupListener(new FixedColumnTableColumns.ColumnGroupListener() {
            public void groupsChanged(SortedSet<ColumnGroup> newGroups) {
                toolBarGroup.groupsChanged(newGroups);
                rebuildToolbar();
            }
        });
    }

    private void rebuildToolbar() {
        toolBar.removeAll();
        for ( ToolBarGroup.Element e : toolBarGroup.getElements()) {
            e.applyAdd(toolBar);
        }

        validate();
        repaint();
    }

}
