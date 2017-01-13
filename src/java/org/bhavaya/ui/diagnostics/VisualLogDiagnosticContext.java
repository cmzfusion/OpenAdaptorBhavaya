package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.ArrayListModel;
import org.bhavaya.ui.ListTable;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Provides graphical view of a list of LogEvents.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class VisualLogDiagnosticContext extends DiagnosticContext {
    private ArrayListModel listModel;

    public VisualLogDiagnosticContext(String name, ImageIcon icon, ArrayListModel listModel) {
        super(name, icon);
        this.listModel = listModel;
    }

    private static JComponent createLogComponent(final AbstractListModel tableModel, String[] propertyName, String[] columnNames, double[] ratios) {
        final ListTable listTable = new ListTable(new ListTable.ListModelTableModel(tableModel, propertyName, columnNames), ratios);
        listTable.getColumn("Time").setCellRenderer(new DateRenderer());
        JScrollPane scrollPane = new JScrollPane(listTable);
        scrollPane.setPreferredSize(new Dimension(800, 250));

        final JEditorPane messageLabel = new JEditorPane();
        messageLabel.setContentType("text/html");
        messageLabel.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(messageLabel);
        listTable.getSelectionModel().addListSelectionListener((ListSelectionListener) UIUtilities.triggerOnEvent(ListSelectionListener.class, null, new Runnable() {
            public void run() {
                int selectedRow = listTable.getSelectedRow();
                if (selectedRow != -1) {
                    ApplicationDiagnostics.LoggingEventBean loggingEventBean = (ApplicationDiagnostics.LoggingEventBean) tableModel.getElementAt(selectedRow);
                    messageLabel.setText(createErrorMessage(loggingEventBean.getMessage(), loggingEventBean.getThrowableStrings()));
                }
            }
        }));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, messageScrollPane);

        return splitPane;
    }


    public Component createComponent() {
        return createLogComponent(listModel,
                new String[]{"timestamp", "threadName", "category", "message"},
                new String[]{"Time", "Thread", "Category", "Message"},
                new double[]{0.12, 0.18, 0.18});
    }


    private static String createErrorMessage(String errorTitle, String[] stringArray) {
        StringBuffer buffer = new StringBuffer("<html><font face=dialog>").append(errorTitle).append("</font><br>");
        if (stringArray != null) {
            buffer.append("<br><font face=dialog size=3>").append(stringArray[0]).append("<br>");
            for (int i = 1; i < stringArray.length; i++) {
                buffer.append(".....").append(stringArray[i] + "<br>");
            }
            buffer.append("</font>");
        }
        return buffer.append("</html>").toString();
    }

    private static class DateRenderer extends DefaultTableCellRenderer {
        private DateFormat localDatetimeFormat;

        public DateRenderer() {
            localDatetimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value == null) return component;
            component.setText(localDatetimeFormat.format(value));
            return this;
        }
    }

    public String createHTMLDescription() {
        return null;
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }
}
