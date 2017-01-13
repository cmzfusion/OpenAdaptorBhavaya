package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.ColumnPathRenderer;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Abstract dialog for editing formula symbols
 * User: ga2mhana
 * Date: 18/03/11
 * Time: 10:15
 */
public abstract class AbstractFormulaEditorDialog extends JDialog {

    private static final Border BORDER = BorderFactory.createEmptyBorder(5,5,5,5);
    protected static final ImageIcon ADD_ICON = ImageIconCache.getImageIcon("add.gif");
    protected static final ImageIcon REMOVE_ICON = ImageIconCache.getImageIcon("remove.gif");

    private SymbolMappingsEditorModel symbolMappingsEditorModel;
    private BeanCollectionTableModel beanCollectionTableModel;

    private ColumnWidthTable symbolTable;
    private String editorType;

    protected AbstractFormulaEditorDialog(Frame owner, String title, BeanCollectionTableModel beanCollectionTableModel, String editorType) {
        super(owner, title, true);
        this.beanCollectionTableModel = beanCollectionTableModel;
        this.editorType = editorType;
    }

    protected void initComponents(SymbolMappingsEditorModel symbolMappingsEditorModel) {
        this.symbolMappingsEditorModel = symbolMappingsEditorModel;
        JPanel mainPanel = addBorder(new JPanel(new BorderLayout()));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, addBorder(createSymbolPanel()), addBorder(createCentralPanel()));
        splitPane.setDividerLocation(120);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setSize(500, 350);
        setVisible(true);
    }

    private JPanel addBorder(JPanel panel) {
        panel.setBorder(BORDER);
        return panel;
    }

    protected abstract JPanel createCentralPanel();

    private JPanel createButtonPanel() {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private void cancelPressed() {
        dispose();
    }

    protected void okPressed() {
        //I don't like this but a quick fix is required...
        //If in the process of editing a formula and ok is pressed, stopCellEditing() is called on the editor which will setValue()
        //If the formula is invalid a popup is shown, but this will happen again when validate is called
        //This flag can be used to avoid showing the first popup.
        symbolMappingsEditorModel.setOkBeingPressed(true);
        try {
            ensureEditingFinished();
        } finally {
            symbolMappingsEditorModel.setOkBeingPressed(false);
        }
    }

    protected void ensureEditingFinished() {
        ensureEditingFinished(symbolTable);
        ensureEditingFinished(getTable());
    }

    private void ensureEditingFinished(JTable table) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    protected abstract JTable getTable();

    private JPanel createSymbolPanel() {
        JPanel symbolPanel = new JPanel(new BorderLayout());
        symbolPanel.add(new BoldLabel("Columns to use in "+editorType), BorderLayout.NORTH);
        symbolTable = new ColumnWidthTable(symbolMappingsEditorModel.getSymbolTableModel(), new int[] {70, 250} );
        symbolPanel.add(new JScrollPane(symbolTable), BorderLayout.CENTER);
        symbolTable.getColumnModel().getColumn(1).setCellEditor(new PopupColumnPathTableCellEditor(this, beanCollectionTableModel.getBeanType()));
        symbolTable.getColumnModel().getColumn(1).setCellRenderer(new ColumnPathRenderer());

        AbstractAction addAction = new AbstractAction(null, ADD_ICON) {
            public void actionPerformed(ActionEvent e) {
                symbolMappingsEditorModel.addSymbol();
            }
        };
        AbstractAction removeAction = new AbstractAction(null, REMOVE_ICON) {
            public void actionPerformed(ActionEvent e) {
                int[] selected = symbolTable.getSelectedRows();
                if(selected.length > 0) {
                    symbolMappingsEditorModel.removeSymbols(selected);
                    symbolTable.selectIndexes(selected);
                }
            }
        };
        symbolPanel.add(new VerticalButtonPanel(addAction, removeAction), BorderLayout.EAST);
        symbolPanel.setPreferredSize(new Dimension(450, 100));
        return symbolPanel;
    }

    protected class BoldLabel extends JLabel {
        public BoldLabel(String text) {
            super(text);
            setFont(getFont().deriveFont(Font.BOLD));
        }
    }

    protected class ColumnWidthTable extends JTable {
        public ColumnWidthTable(TableModel dm, int[] columnWidths) {
            super(dm);
            setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
            TableColumnModel columnModel = getColumnModel();
            for(int i=0; i<columnWidths.length && i<columnModel.getColumnCount(); i++) {
                TableColumn col = columnModel.getColumn(i);
                col.setPreferredWidth(columnWidths[i]);
            }
        }

        public void selectIndexes(int[] selected) {
            for(int i=0; i<selected.length; i++) {
                if(selected[i] < getRowCount()) {
                    changeSelection(selected[i], 0, i > 0, false);
                }
            }
        }
    }

    protected class VerticalButtonPanel extends JPanel {
        public VerticalButtonPanel(Action... actions) {
            super(new GridBagLayout());
            setBorder(new EmptyBorder(0,5,0,5));
            Box box = new Box(BoxLayout.Y_AXIS);
            for(Action action : actions) {
                box.add(new JButton(action));
            }
            add(box);
        }
    }
}
