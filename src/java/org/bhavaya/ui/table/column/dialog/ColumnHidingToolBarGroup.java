package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.ToolBarGroup;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.util.WeakPropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.SortedSet;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Sep-2009
 * Time: 09:21:20
 */
public class ColumnHidingToolBarGroup extends ToolBarGroup implements FixedColumnTableColumns.ColumnGroupListener {

    private FixedColumnTableColumns fixedTableColumns;

    /**
     * No argument constructor for config serialization/deserialization
     */
    public ColumnHidingToolBarGroup() {
        super("ColumnHidingToolBarGroup");        
    }
    
    public ColumnHidingToolBarGroup(FixedColumnTableColumns fixedTableColumns) {
        super("ColumnHidingToolBarGroup");
        this.fixedTableColumns = fixedTableColumns;
        groupsChanged(fixedTableColumns.getColumnGroups());
    }

    public void groupsChanged(SortedSet<ColumnGroup> newGroups) {
        clearElements();
        for ( ColumnGroup g : newGroups) {
            addElement(new ComponentElement(new ColumnGroupButton(g)));
        }
    }

    private class ColumnGroupButton extends JButton {

        private ColumnGroup columnGroup;

        public ColumnGroupButton(ColumnGroup columnGroup) {
            super(columnGroup.getGroupName());
            this.columnGroup = columnGroup;
            columnGroup.addPropertyChangeListener(new ColumnGroupButtonListener());
            addActionListener(new HideOrUnhideAction());
            setIcon(ColumnListPanel.addToGroupIcon);
            updateButtonState();
        }

        private void updateButtonState() {
            setForeground(columnGroup.isHidden() ? Color.RED : Color.BLACK);
        }

        private class HideOrUnhideAction extends AuditedAbstractAction {

            protected void auditedActionPerformed(ActionEvent e) {
                if ( columnGroup.isHidden() ) {
                    fixedTableColumns.showColumnsInGroup(columnGroup);
                } else {
                    fixedTableColumns.hideColumnsInGroup(columnGroup);
                }
                columnGroup.setHidden(! columnGroup.isHidden());
                updateButtonState();
            }
        }

        private class ColumnGroupButtonListener extends WeakPropertyChangeListener {
            public ColumnGroupButtonListener() {
                super(columnGroup, ColumnGroupButton.this, "hidden");
            }

            protected void propertyChange(Object listenerOwner, PropertyChangeEvent evt) {
                updateButtonState();
            }
        }
    }

}
