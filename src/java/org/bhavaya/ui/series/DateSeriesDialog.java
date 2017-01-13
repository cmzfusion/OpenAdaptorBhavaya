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

package org.bhavaya.ui.series;

import org.bhavaya.ui.*;
import org.bhavaya.ui.adaptor.Pipe;
import org.bhavaya.ui.adaptor.PropertyPipe;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.Calendar;

/**
 * @author Brendon McLean
 * @version $Revision: 1.7.4.1 $
 */

public class DateSeriesDialog extends JDialog {

    private DateSeriesNew dateSeries;
    private ListTable functionTable;
    private OkAction okAction;
    private RemoveAction removeAction;
    private EditAction editAction;
    private DateSeriesNew.Listener seriesChangedTableTrigger;
    private DateSeriesNew.Listener seriesChangedValidationTrigger;
    private AddRelativeDateAction addBeforeAction;
    private AddRelativeDateAction addAfterAction;

    public DateSeriesDialog(JFrame owner, DateSeriesNew dateSeries) {
        super(owner, "Date series editor", true);

        dateSeries = dateSeries == null ? new DateSeriesNew() : dateSeries;
        this.dateSeries = dateSeries;

        JButton addSymbolicDateButton = new JButton(new AddSymbolicDateAction());
        addSymbolicDateButton.setFocusable(false);
        addSymbolicDateButton.setHorizontalAlignment(JButton.LEFT);
        addSymbolicDateButton.setToolTipText("Add symbolic date function (Today/Week-begin/etc)");

        JButton addFixedDateButton = new JButton(new AddFixedDateAction());
        addFixedDateButton.setFocusable(false);
        addFixedDateButton.setHorizontalAlignment(JButton.LEFT);
        addFixedDateButton.setToolTipText("Add a fixed date");

        addBeforeAction = new AddRelativeDateAction("minus.png", RelativeDateFunction.PREPOSITION_BEFORE);
        JButton addBeforeSelectedFunctionButton = new JButton(addBeforeAction);
        addBeforeSelectedFunctionButton.setFocusable(false);
        addBeforeSelectedFunctionButton.setMargin(new Insets(2, 3, 2, 3));
        addBeforeSelectedFunctionButton.setToolTipText("Add relative date before selected date");

        addAfterAction = new AddRelativeDateAction("plus.png", RelativeDateFunction.PREPOSITION_AFTER);
        JButton addAfterSelectedFunctionButton = new JButton(addAfterAction);
        addAfterSelectedFunctionButton.setFocusable(false);
        addAfterSelectedFunctionButton.setMargin(new Insets(2, 3, 2, 3));
        addAfterSelectedFunctionButton.setToolTipText("Add relative date after selected date");

        RowLayout rowLayout = new RowLayout(addSymbolicDateButton.getPreferredSize().width, 1);
        RowLayout.Row row = new RowLayout.Row(0, RowLayout.LEFT, RowLayout.TOP, false);
        row.addComponent(addSymbolicDateButton, new RowLayout.RemainingWidthConstraint());
        rowLayout.addRow(row);

        row = new RowLayout.Row(0, RowLayout.LEFT, RowLayout.TOP, false);
        row.addComponent(addFixedDateButton, new RowLayout.RemainingWidthConstraint());
        rowLayout.addRow(row);

        row = new RowLayout.Row(0, RowLayout.LEFT, RowLayout.TOP, false);
        row.addComponent(addBeforeSelectedFunctionButton, new RowLayout.RelativeWidthConstraint(0.5));
        row.addComponent(addAfterSelectedFunctionButton, new RowLayout.RelativeWidthConstraint(0.5));
        rowLayout.addRow(row);

        JPanel addPanel = new JPanel(rowLayout);
        addPanel.add(addSymbolicDateButton);
        addPanel.add(addFixedDateButton);
        addPanel.add(addBeforeSelectedFunctionButton);
        addPanel.add(addAfterSelectedFunctionButton);

        JPanel titledAddPanel = new JPanel(new BorderLayout(0, 0));
        titledAddPanel.add(addPanel, BorderLayout.CENTER);
        titledAddPanel.setBorder(BorderFactory.createTitledBorder("Add"));

        editAction = new EditAction();
        JButton editDateFunctionButton = new JButton(editAction);
        editDateFunctionButton.setFocusable(false);
        editDateFunctionButton.setToolTipText("Edit the selected date range");

        removeAction = new RemoveAction();
        JButton removeDateFunctionButton = new JButton(removeAction);
        removeDateFunctionButton.setFocusable(false);
        removeDateFunctionButton.setToolTipText("Remove the selected date range");

        JPanel otherButtonsPanel = new JPanel(new BorderLayout());
        otherButtonsPanel.add(editDateFunctionButton, BorderLayout.NORTH);
        otherButtonsPanel.add(Box.createVerticalStrut(5), BorderLayout.CENTER);
        otherButtonsPanel.add(removeDateFunctionButton, BorderLayout.SOUTH);
        otherButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 3, 0, 3));

        JPanel toolBar = new JPanel(new CustomToolBarLayout(titledAddPanel.getPreferredSize().width, 1));
        toolBar.add(titledAddPanel);
        toolBar.add(otherButtonsPanel);

        ListTable.ListTableModel tableModel = new ListTable.ListTableModel(new String[]{"verboseDescription", "date"}, new String[]{"Date function", "Evaluated today"}) {
            protected Object getObjectAtRow(int rowIndex) {
                return DateSeriesDialog.this.dateSeries.getFunction(rowIndex);
            }

            public int getRowCount() {
                return DateSeriesDialog.this.dateSeries.getSize();
            }
        };
        seriesChangedTableTrigger = (DateSeriesNew.Listener) tableModel.getEventBinding(DateSeriesNew.Listener.class);
        seriesChangedValidationTrigger = (DateSeriesNew.Listener) UIUtilities.triggerMethodOnEvent(DateSeriesNew.Listener.class, null, this, "validCheck");
        dateSeries.addListener(seriesChangedTableTrigger);
        dateSeries.addListener(seriesChangedValidationTrigger);
        functionTable = new ListTable(tableModel, new double[]{0.37, 0.37});
        functionTable.setDefaultRenderer(Object.class, new SpecialCaseRenderer());
        functionTable.setColumnTooltips(new String[]{"The starting date for this date interval", "The last date of this date interval"});
        functionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                DateFunction selectedFunction = getSelectedFunction();
                editAction.setEnabled(selectedFunction != null && (selectedFunction instanceof FixedDateFunction || selectedFunction instanceof RelativeDateFunction));
                removeAction.setEnabled(selectedFunction != null && (selectedFunction != SymbolicDateFunction.TIME_BEGIN && selectedFunction != SymbolicDateFunction.TIME_END));

                boolean validRelativeSelection = selectedFunction instanceof SymbolicDateFunction || selectedFunction instanceof FixedDateFunction || selectedFunction instanceof RelativeDateFunction;
                addBeforeAction.setEnabled(validRelativeSelection);
                addAfterAction.setEnabled(validRelativeSelection);
            }
        });
        functionTable.setPreferredScrollableViewportSize(new Dimension(400, 200));
        JScrollPane scrollPane = new JScrollPane(functionTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final AbstractTableModel intervalTableModel = new IntervalTableModel();
        JTable intervalTable = new JTable(intervalTableModel);
        intervalTable.setPreferredScrollableViewportSize(new Dimension(400, 200));
        JScrollPane intervalTableScrollPane = new JScrollPane(intervalTable);

        JPanel userDatesPanel = new JPanel(new BorderLayout());
        userDatesPanel.add(scrollPane, BorderLayout.CENTER);
        userDatesPanel.add(toolBar, BorderLayout.EAST);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Edit date sequence", userDatesPanel);
        tabbedPane.addTab("View intervals", intervalTableScrollPane);

        okAction = new OkAction();
        JButton closeButton = new JButton(okAction);
        JButton cancelButton = new JButton(new CancelAction());
        JPanel closeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButtonsPanel.add(closeButton);
        closeButtonsPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(closeButtonsPanel, BorderLayout.SOUTH);

        validCheck();

        JMenuItem closeMenuItem = new JMenuItem(new AuditedAbstractAction("Close") {
            public void auditedActionPerformed(ActionEvent e) {
                cancelDialog();
            }
        });
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(closeMenuItem);

        JCheckBoxMenuItem verboseMenuItem = new JCheckBoxMenuItem(new AuditedAbstractAction("Verbose", "Set DateSeries Verbose") {
            public void auditedActionPerformed(ActionEvent e) {
                DateSeriesDialog.this.dateSeries.setVerbose(((JCheckBoxMenuItem) e.getSource()).isSelected());
                intervalTableModel.fireTableDataChanged();
            }
        });
        verboseMenuItem.setSelected(dateSeries.isVerbose());
        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.add(verboseMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancelDialog();
            }
        });

        setContentPane(contentPanel);
        pack();
    }

    private DateFunction getFunction(int row) {
        return DateSeriesDialog.this.dateSeries.getFunction(row);
    }

    private DateFunction getSelectedFunction() {
        int selectedRow = functionTable.getSelectedRow();
        return selectedRow == -1 ? null : DateSeriesDialog.this.dateSeries.getFunction(selectedRow);
    }

    public void validCheck() {
        okAction.setEnabled(dateSeries.getSize() > 0);
    }

    public DateSeriesNew getDateSeries() {
        return dateSeries;
    }

    public void dispose() {
        super.dispose();
    }

    private void cancelDialog() {
        clearListeners();
        dateSeries = null;
        dispose();
    }

    private void clearListeners() {
        dateSeries.removeListener(seriesChangedTableTrigger);
        dateSeries.removeListener(seriesChangedValidationTrigger);
    }

    private class EditAction extends AuditedAbstractAction {
        public EditAction() {
            putValue(Action.NAME, "Edit...");
            setEnabled(false);
        }

        public void auditedActionPerformed(ActionEvent e) {
            DateFunction selectedFunction = getSelectedFunction();
            DateFunction dateFunctionCopy = (DateFunction) BeanUtilities.verySlowDeepCopy(selectedFunction);
            DateFunctionEditor dateFunctionEditor = null;
            if (dateFunctionCopy instanceof FixedDateFunction) {
                dateFunctionEditor = new FixedDateDialog(DateSeriesDialog.this, (FixedDateFunction) dateFunctionCopy);
            } else if (dateFunctionCopy instanceof RelativeDateFunction) {
                dateFunctionEditor = new RelativeDateDialog(DateSeriesDialog.this, (RelativeDateFunction) dateFunctionCopy);
            }

            UIUtilities.centreInContainer(DateSeriesDialog.this, dateFunctionEditor, 0, 0);
            dateFunctionEditor.show();
            DateFunction newDateFunction = dateFunctionEditor.getDateFunction();
            if (newDateFunction != null) {
                dateSeries.removeFunction(selectedFunction);
                dateSeries.addFunction(newDateFunction);
            }

        }
    }

    private class RemoveAction extends AuditedAbstractAction {
        public RemoveAction() {
            putValue(Action.NAME, "Remove");
            setEnabled(false);
        }

        public void auditedActionPerformed(ActionEvent e) {
            dateSeries.removeFunction(dateSeries.getFunction(functionTable.getSelectedRow()));
        }
    }

    private class OkAction extends AuditedAbstractAction {
        public OkAction() {
            putValue(Action.NAME, "Ok");
            setEnabled(false);
        }

        public void auditedActionPerformed(ActionEvent e) {
            clearListeners();
            dispose();
        }
    }

    private class CancelAction extends AuditedAbstractAction {
        public CancelAction() {
            putValue(Action.NAME, "Cancel");
        }

        public void auditedActionPerformed(ActionEvent e) {
            cancelDialog();
        }
    }

    private class AddFixedDateAction extends AuditedAbstractAction {
        public AddFixedDateAction() {
            putValue(Action.NAME, "Fixed");
        }

        public void auditedActionPerformed(ActionEvent e) {
            FixedDateDialog dialog = new FixedDateDialog(DateSeriesDialog.this, new FixedDateFunction(DateUtilities.newDate()));
            UIUtilities.centreInContainer(DateSeriesDialog.this, dialog, 0, 0);
            dialog.show();
            DateFunction dateFunction = dialog.getDateFunction();
            if (dateFunction != null) {
                dateSeries.addFunction(dateFunction);
            }
        }
    }

    private class AddRelativeDateAction extends AuditedAbstractAction {
        private RelativeDateFunction.Preposition preposition;

        public AddRelativeDateAction(String iconResourceName, RelativeDateFunction.Preposition preposition) {
            this.preposition = preposition;
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(iconResourceName));
            setEnabled(false);
        }

        public void auditedActionPerformed(ActionEvent e) {
            RelativeDateFunction newDateFunction;
            if (getSelectedFunction() instanceof RelativeDateFunction) {
                newDateFunction = (RelativeDateFunction) BeanUtilities.verySlowDeepCopy(getSelectedFunction());
            } else {
                newDateFunction = new RelativeDateFunction(getSelectedFunction(), RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 1);
            }

            RelativeDateDialog dialog = new RelativeDateDialog(DateSeriesDialog.this, newDateFunction);
            UIUtilities.centreInContainer(DateSeriesDialog.this, dialog, 0, 0);
            dialog.show();
            DateFunction dateFunction = dialog.getDateFunction();
            if (dateFunction != null) {
                dateSeries.addFunction(dateFunction);
            }
        }
    }

    private class AddSymbolicDateAction extends AuditedAbstractAction {
        public AddSymbolicDateAction() {
            putValue(Action.NAME, "Symbolic");
        }

        public void auditedActionPerformed(ActionEvent e) {
            JMenuItem addTodayMenuItem = new JMenuItem(new AddSymbolicAction("T - Today", SymbolicDateFunction.TODAY_DATEFUNCTION));
            JMenuItem addWeekMenuItem = new JMenuItem(new AddSymbolicAction("W - Week-begin", SymbolicDateFunction.START_OF_WEEK_DATEFUNCTION));
            JMenuItem addMonthMenuItem = new JMenuItem(new AddSymbolicAction("M - Month-begin", SymbolicDateFunction.START_OF_MONTH_DATEFUNCTION));
            JMenuItem addYearMenuItem = new JMenuItem(new AddSymbolicAction("Y - Year-begin", SymbolicDateFunction.START_OF_YEAR_DATEFUNCTION));

            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.add(addTodayMenuItem);
            popupMenu.add(addWeekMenuItem);
            popupMenu.add(addMonthMenuItem);
            popupMenu.add(addYearMenuItem);

            String includeCTDDatesProperty = ApplicationProperties.getApplicationProperties() != null ? ApplicationProperties.getApplicationProperties().getProperty("includeFuturesCTDDates") : null;
            if( includeCTDDatesProperty != null && includeCTDDatesProperty.equals("true")) {
                JMenuItem boblCTDMenuItem = new JMenuItem(new AddSymbolicAction("BOBL - CTD Maturity", SymbolicDateFunction.BOBL_CTD_MATURITY_DATEFUNCTION));
                JMenuItem bundCTDMenuItem = new JMenuItem(new AddSymbolicAction("BUND - CTD Maturity", SymbolicDateFunction.BUND_CTD_MATURITY_DATEFUNCTION));
                JMenuItem schatzCTDMenuItem = new JMenuItem(new AddSymbolicAction("SCHATZ - CTD Maturity", SymbolicDateFunction.SCHATZ_CTD_MATURITY_DATEFUNCTION));

                popupMenu.add(boblCTDMenuItem);
                popupMenu.add(bundCTDMenuItem);
                popupMenu.add(schatzCTDMenuItem);
            }

            JButton button = (JButton) e.getSource();
            popupMenu.show(button, 0, button.getHeight());
        }

        private class AddSymbolicAction extends AuditedAbstractAction {
            private SymbolicDateFunction symbolicDateFunction;

            public AddSymbolicAction(String menuText, SymbolicDateFunction symbolicDateFunction) {
                this.symbolicDateFunction = symbolicDateFunction;
                putValue(Action.NAME, menuText);
            }

            public void auditedActionPerformed(ActionEvent e) {
                dateSeries.addFunction(symbolicDateFunction);
            }
        }
    }

    public abstract class DateFunctionEditor extends JDialog {
        private DateFunction dateFunction;

        public DateFunctionEditor(JDialog owner, String title, DateFunction dateFunction) {
            super(owner, title, true);
            this.dateFunction = dateFunction;
        }

        public DateFunction getDateFunction() {
            return dateFunction;
        }

        protected void setDateFunction(DateFunction dateFunction) {
            this.dateFunction = dateFunction;
        }

        protected JPanel createButtonPanel() {
            JButton okButton = new JButton(new AuditedAbstractAction("OK", "Set DateFunction") {
                public void auditedActionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            okButton.setDefaultCapable(true);
            JButton cancelButton = new JButton(new AuditedAbstractAction("Cancel") {
                public void auditedActionPerformed(ActionEvent e) {
                    dateFunction = null;
                    dispose();
                }
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

            getRootPane().setDefaultButton(okButton);

            return buttonPanel;
        }

        public void dispose() {
            super.dispose();
        }
    }

    public class FixedDateDialog extends DateFunctionEditor {
        public FixedDateDialog(JDialog owner, final FixedDateFunction dateFunction) {
            super(owner, "Enter fixed date", dateFunction);

            BhavayaDateSpinner dateSpinner = new BhavayaDateSpinner(Calendar.DAY_OF_MONTH, dateFunction.getDate());
            Pipe datePipe = new PropertyPipe(dateSpinner, "date", dateFunction, "fixedDate");
            dateSpinner.addChangeListener((ChangeListener) datePipe.getListenerInterface(ChangeListener.class));
            dateSpinner.addFocusListener((FocusListener) datePipe.getListenerInterface(FocusListener.class, "focusLost"));

            final JTextField aliasTextField = new JTextField();
            aliasTextField.setEnabled(dateFunction.getAlias() != null);
            aliasTextField.setText(dateFunction.getAlias() == null ? "" : dateFunction.getAlias());
            final Pipe aliasPipe = new PropertyPipe(aliasTextField, "text", dateFunction, "alias");
            aliasTextField.getDocument().addDocumentListener((DocumentListener) aliasPipe.getListenerInterface(DocumentListener.class));

            final JCheckBox useAliasCheckBox = new JCheckBox("Use alias");
            useAliasCheckBox.setSelected(dateFunction.getAlias() != null);
            useAliasCheckBox.addActionListener((ActionListener) UIUtilities.triggerOnEvent(ActionListener.class, null, new Runnable() {
                public void run() {
                    if (useAliasCheckBox.isSelected()) {
                        aliasTextField.setEnabled(true);
                        dateFunction.setAlias(aliasTextField.getText());
                    } else {
                        aliasTextField.setEnabled(false);
                        dateFunction.setAlias(null);
                    }
                }
            }));

            final int panelWidth = 285;
            JPanel dateEditorPanel = new JPanel(new RowLayout(panelWidth, 10));
            RowLayout dateEditorRowLayout = (RowLayout) dateEditorPanel.getLayout();

            RowLayout.Row row = new RowLayout.Row(5, RowLayout.LEFT, RowLayout.MIDDLE, false);
            dateEditorPanel.add(row.addComponent(new JLabel("Fixed date")));
            dateEditorPanel.add(row.addComponent(dateSpinner));
            dateEditorRowLayout.addRow(row);

            row = new RowLayout.Row(5, RowLayout.LEFT, RowLayout.MIDDLE, false);
            dateEditorPanel.add(row.addComponent(useAliasCheckBox));
            dateEditorPanel.add(row.addComponent(aliasTextField, new RowLayout.RemainingWidthConstraint()));
            dateEditorRowLayout.addRow(row);

            dateEditorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(dateEditorPanel, BorderLayout.CENTER);
            contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);

            setContentPane(contentPanel);
            pack();
        }
    }

    public class RelativeDateDialog extends DateFunctionEditor {
        private final String DATE_FORMAT_STRING = "<html>Date label: <font color=blue>%description%</font></html>";
        private final SimpleObjectFormat DATE_FORMATTER = new SimpleObjectFormat(DATE_FORMAT_STRING);

        public RelativeDateDialog(JDialog owner, final RelativeDateFunction dateFunction) {
            super(owner, "Relative date", dateFunction);

            JSpinner offsetSpinner = new JSpinner(new SpinnerNumberModel(dateFunction.getOffset(), 1, 999, 1));
            offsetSpinner.addChangeListener((ChangeListener) new PropertyPipe(offsetSpinner, "value", dateFunction, "offset", Transform.NUMBER_TO_INTEGER).getListenerInterface(ChangeListener.class));

            final JComboBox offsetType = new JComboBox(RelativeDateFunction.getCalendarOffsetTypes());
            offsetType.setSelectedItem(dateFunction.getCalendarOffsetType());
            offsetType.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dateFunction.setCalendarOffsetType((RelativeDateFunction.OffsetType) offsetType.getSelectedItem());
                }
            });

            final JComboBox offsetSign = new JComboBox(RelativeDateFunction.getPrepositions());
            offsetSign.setSelectedItem(dateFunction.getPreposition());
            offsetSign.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dateFunction.setPreposition((RelativeDateFunction.Preposition) offsetSign.getSelectedItem());
                }
            });

            JLabel referenceDateLabel = new JLabel(dateFunction.getReferenceDate().getDescription());

            JPanel dateEditorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            dateEditorPanel.add(new JLabel("This date is"));
            dateEditorPanel.add(offsetSpinner);
            dateEditorPanel.add(offsetType);
            dateEditorPanel.add(offsetSign);
            dateEditorPanel.add(referenceDateLabel);

            final JLabel datePreviewLabel = new JLabel(DATE_FORMATTER.formatObject(dateFunction));
            dateFunction.addPropertyChangeListener((PropertyChangeListener) UIUtilities.triggerOnEvent(PropertyChangeListener.class, null, new Runnable() {
                public void run() {
                    datePreviewLabel.setText(DATE_FORMATTER.formatObject(dateFunction));
                }
            }));

            JPanel datePreviewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            datePreviewPanel.add(datePreviewLabel);

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(dateEditorPanel, BorderLayout.NORTH);
            contentPanel.add(datePreviewPanel, BorderLayout.CENTER);
            contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);

            setContentPane(contentPanel);
            pack();
        }
    }

    private static class CustomToolBarLayout extends RowLayout {
        public CustomToolBarLayout(int preferredWidth, int vGap) {
            super(preferredWidth, vGap);
        }

        public void addLayoutComponent(Component comp, Object constraint) {
            RowLayout.Row row = new RowLayout.Row(0, LEFT, CENTRE, false);
            row.addComponent(comp, new RowLayout.RemainingWidthConstraint());
            ((JComponent) comp).setPreferredSize(comp.getMinimumSize());
            addRow(row);
        }

        public void addLayoutComponent(String name, Component comp) {
            addLayoutComponent(comp, null);
        }
    }

    private class SpecialCaseRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            DateFunction function = getFunction(row);
            if (function == SymbolicDateFunction.TIME_BEGIN || function == SymbolicDateFunction.TIME_END) {
                label.setForeground(Color.gray);
                if (column == 1) {
                    label.setText(function == SymbolicDateFunction.TIME_BEGIN ? "-infinity" : "infinity");
                }
            } else {
                label.setForeground(Color.black);
            }
            return label;
        }
    }

    private class IntervalTableModel extends AbstractTableModel {
        private final int INTERVAL_NAME = 0;
        private final int INTERVAL_START = 1;
        private final int INTERVAL_END = 2;
        private final String[] COLUMNS = new String[]{"DateFunctionInterval", "First date", "Last date"};

        public int getRowCount() {
            return dateSeries.getSize() - 1;
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            DateFunction intervalStart = dateSeries.getFunction(rowIndex);
            DateFunction intervalEnd = dateSeries.getFunction(rowIndex + 1);
            switch (columnIndex) {
                case INTERVAL_NAME:
                    return dateSeries.getInterval(intervalStart.getDate());
                case INTERVAL_START:
                    return intervalStart == SymbolicDateFunction.TIME_BEGIN ? (Object) "-infinity" : (Object) intervalStart.getDate();
                case INTERVAL_END:
                    return intervalEnd == SymbolicDateFunction.TIME_END ? (Object) "infinity" : DateUtilities.relativeToDate(intervalEnd.getDate(), Calendar.DAY_OF_YEAR, -1);
            }
            return null;
        }
    }
}
