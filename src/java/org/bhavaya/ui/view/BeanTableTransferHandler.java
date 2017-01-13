package org.bhavaya.ui.view;

import org.bhavaya.ui.table.TabularBeanAssociation;
import org.bhavaya.ui.BeanTableTransferable;
import org.bhavaya.ui.BeanClipboardContents;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.*;
import java.util.HashSet;

/**
 * This class is a modified version of BasicTableUI.BeanTableTransferHandler
 * It uses the cell renderer to render the values, rather than getValueAt().toString
 *
 * Now supports TabularBeanAssociation, so bean instances can be used in copy paste operations within local jvm
 */
public class BeanTableTransferHandler extends TransferHandler {


    private boolean isDragToDeleteSupported;

     /**
     * Whether components using this transfer handler can have their contents dragged to a garbage bin/shredder
     */
    public void setDragToDeleteSupported(boolean dragToDeleteSupported) {
        isDragToDeleteSupported = dragToDeleteSupported;
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return false;
    }

    /**
     * Create a Transferable to use as the source for a data transfer.
     *
     * @param c The component holding the data to be transfered.  This
     *          argument is provided to enable sharing of TransferHandlers by
     *          multiple components.
     * @return The representation of the data to be transfered.
     */
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            int[] rows;
            int[] cols;

            if (!table.getRowSelectionAllowed() && !table.getColumnSelectionAllowed()) {
                return null;
            }

            if (!table.getRowSelectionAllowed()) {
                int rowCount = table.getRowCount();

                rows = new int[rowCount];
                for (int counter = 0; counter < rowCount; counter++) {
                    rows[counter] = counter;
                }
            } else {
                rows = table.getSelectedRows();
            }

            if (!table.getColumnSelectionAllowed()) {
                int colCount = table.getColumnCount();

                cols = new int[colCount];
                for (int counter = 0; counter < colCount; counter++) {
                    cols[counter] = counter;
                }
            } else {
                cols = table.getSelectedColumns();
            }

            if (rows == null || cols == null || rows.length == 0 || cols.length == 0) {
                return null;
            }

            StringBuffer plainBuf = new StringBuffer();
            StringBuffer htmlBuf = new StringBuffer();

            //use a set for the bean instances - multiple cells may derive from the same underlying bean and not just in each row - the table may be pivoted
            //I think we want unique bean instances in the copy transferable
            //but this will mean that ordering is not significant in the copy/paste currently
            HashSet beanInstances = new HashSet();
            TabularBeanAssociation tabularBeanAssociation = getTabularBeanAssociation(table);

            htmlBuf.append("<html>\n<body>\n<table>\n");

            for (int row = 0; row < rows.length; row++) {
                htmlBuf.append("<tr>\n");
                for (int col = 0; col < cols.length; col++) {

                    addSelectedBeans(beanInstances, tabularBeanAssociation, rows[row], cols[col]);

                    Object cellValue = table.getValueAt(rows[row], cols[col]);
                    String cellText = "";
                    if (cellValue != null) {
                        TableCellRenderer tableCellRenderer = table.getCellRenderer(rows[row], cols[col]);
                        Component cellComponent = tableCellRenderer.getTableCellRendererComponent(table, cellValue, false, false, rows[row], cols[col]);
                        if (cellComponent instanceof JLabel) {
                            JLabel label = (JLabel) cellComponent;
                            cellText = label.getText();
                        } else {
                            cellText = cellValue.toString();
                        }
                    }

                    plainBuf.append(cellText).append("\t");
                    htmlBuf.append("  <td>").append(cellText).append("</td>\n");
                }
                // we want a newline at the end of each line and not a tab
                plainBuf.deleteCharAt(plainBuf.length() - 1).append("\n");
                htmlBuf.append("</tr>\n");
            }

            // remove the last newline
            plainBuf.deleteCharAt(plainBuf.length() - 1);
            htmlBuf.append("</table>\n</body>\n</html>");

            Object[] beans = beanInstances.toArray(new Object[beanInstances.size()]);
            BeanTableTransferable transferable = new BeanTableTransferable(plainBuf.toString(), htmlBuf.toString(), beans, isDragToDeleteSupported);
            return transferable;
        }

        return null;
    }

    /**
    * @return  TabularBeanAssoication backing this table, or null
    */
    private TabularBeanAssociation getTabularBeanAssociation(JTable table) {
        TabularBeanAssociation result = null;
        if ( table.getModel() instanceof TabularBeanAssociation)
        {
            result = (TabularBeanAssociation)table.getModel();
        }
        return result;
    }

    private void addSelectedBeans(HashSet beanInstances, TabularBeanAssociation tabularBeanAssociation, int row, int col) {
        if ( tabularBeanAssociation != null ) {
            Object[] beans = tabularBeanAssociation.getBeansForLocation(row, col);
            for ( Object bean : beans ) {
                beanInstances.add(bean);
            }
        }
    }

    public int getSourceActions(JComponent c) {
        return COPY;
    }

    /**
     * @param transferFlavors
     * @param beanClasses, class which bean data must be assignable to
     * @return true, if transferFlavors contains a flavor for bean transfer, and the beans are of a class type which is assignable to one of the classes in beanClasses array
     */
    protected boolean containsBeanDataFlavor(DataFlavor[] transferFlavors, Class... beanClasses) {
        boolean result = false;

        dance:
        for ( DataFlavor flavor : transferFlavors ) {
            if ( flavor instanceof BeanClipboardContents.BeanClipboardContentsDataFlavor )  {

                //In bhavaya the actual bean instance will probably be of a generated class type which subclasses beanClass
                Class transferrableBeanClass = ((BeanClipboardContents.BeanClipboardContentsDataFlavor)flavor).getBeanClass();

                for ( Class beanClass : beanClasses) {
                    if ( beanClass.isAssignableFrom(transferrableBeanClass)) {
                        result = true;
                        break dance;
                    }
                }
            }
        }
        return result;
    }

    public Class findTransferableBeanClass(Transferable t) {
        Class retVal = null;
        DataFlavor[] flavors = t.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (flavor instanceof BeanClipboardContents.BeanClipboardContentsDataFlavor) {
                retVal = ((BeanClipboardContents.BeanClipboardContentsDataFlavor) flavor).getBeanClass();
                break;
            }
        }
        return retVal;
    }
}
