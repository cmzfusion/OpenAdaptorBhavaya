package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class TreeTableCellRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row, int column) {
        TreeTableModel tableModel = (TreeTableModel) table.getModel();
        JTree underlyingTree = tableModel.getUnderlyingTree();
        TreeModel treeModel = underlyingTree.getModel();

        TreePath pathForRow = underlyingTree.getPathForRow(row);

        Component component = underlyingTree.getCellRenderer().getTreeCellRendererComponent(underlyingTree, value, isSelected, true, treeModel.isLeaf(value), row, hasFocus);
        Rectangle bounds = component.getBounds();
        int newWidth = bounds.width + pathForRow.getPathCount() * 20;
        component.setBounds(bounds.x, bounds.y, newWidth, bounds.height);
        return component;
    }


}
