package org.bhavaya.util;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * subclass this for different String rendering rules. We use this for a "bloomberg" output of tables with columns truncated
 * to certain char widths.
 * I have provided html and CSV models as public inner classes
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public abstract class TableToStringRenderer {
    int[] rows = new int[0];
    int[] cols = new int[0];
    private JTable table = null;
    private TableModel tableModel = null;

    public TableToStringRenderer(JTable table) {
        this( table.getModel() );
        this.table = table;
    }

    public TableToStringRenderer(TableModel tableModel) {
        this.tableModel = tableModel;
    }

    public String render(boolean includeHeader) {
        StringBuffer buf = new StringBuffer();

        initSourceRowsArray();
        initSourceColumnsArray();

        if (getColumnCount() > 0) {
            int y = includeHeader ? -1 : 0;
            for (; y < getRowCount(); y++) {
                StringBuffer rowBuffer = new StringBuffer();
                for (int x = 0; x < getColumnCount(); x++) {
                    String cellText = getCellText(x, y);
                    rowBuffer.append( wrapCell(x, y, cellText) );
                }
                buf.append( wrapRow(y, rowBuffer.toString()) );
            }
        }
        return buf.toString();
    }

    private int getUnderlyingRows(int y) {
        return rows[y];
    }

    private int getUnderlyingColumn(int x) {
        return cols[x];
    }

    private void initSourceColumnsArray() {
        if (table != null) {
            cols = table.getSelectedColumns();
        }
        if (cols.length == 0) {
            int colCount = tableModel.getColumnCount();

            cols = new int[colCount];
            for (int counter = 0; counter < colCount; counter++) {
                cols[counter] = counter;
            }
        }
    }

    protected void initSourceRowsArray() {
        if (table != null) {
            rows = table.getSelectedRows();
        }
        if (rows.length == 0) {
            int rowCount = tableModel.getRowCount();

            rows = new int[rowCount];
            for (int counter = 0; counter < rowCount; counter++) {
                rows[counter] = counter;
            }
        }
    }

    public abstract String wrapRow(int y, String rowText);

    public abstract String wrapCell(int x, int y, String cellText);


    protected String getCellText(int x, int y) {
        if (y == TableModelEvent.HEADER_ROW) {
            return tableModel.getColumnName(x);
        } else {
            int underlyingCol = getUnderlyingColumn(x);
            int underlyingRow = getUnderlyingRows(y);
            Object cellValue = tableModel.getValueAt(underlyingRow, underlyingCol);
            String cellText = "";
            if (table != null && cellValue != null) {
                TableCellRenderer tableCellRenderer = table.getCellRenderer(underlyingRow, x);
                Component cellComponent = tableCellRenderer.getTableCellRendererComponent(table, cellValue, false, false, underlyingRow, underlyingCol);
                if (cellComponent instanceof JLabel) {
                    JLabel label = (JLabel) cellComponent;
                    cellText = label.getText();
                } else {
                    cellText = cellValue.toString();
                }
            }
            return cellText;
        }
    }

    protected int getRowCount() {
        return rows.length;
    }

    protected int getColumnCount() {
        return cols.length;
    }

    public static class Html extends TableToStringRenderer {
        public Html(JTable table) {
            super(table);
        }

        public Html(TableModel tableModel) {
            super(tableModel);
        }

        public String wrapRow(int y, String rowText) {
            StringBuffer buf = new StringBuffer(rowText.length() + 10);
            buf.append("<tr>").append(rowText).append("</tr>\n");
            return buf.toString();
        }

        public String wrapCell(int x, int y, String cellText) {
            return "<td>"+Utilities.escapeHtmlCharacters(cellText)+"</td>";
        }
    }

    public static class Csv extends TableToStringRenderer {
        public java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"");

        public Csv(JTable table) {
            super(table);
        }

        public Csv(TableModel tableModel) {
            super(tableModel);
        }

        public String wrapRow(int y, String rowText) {
            return rowText+"\n";
        }

        public String wrapCell(int x, int y, String cellText) {
            cellText = pattern.matcher(cellText).replaceAll("\"\"");
            if (x < getColumnCount()-1) {
                return cellText+",";
            } else {
                return cellText;
            }
        }
    }

}
