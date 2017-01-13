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

package org.bhavaya.ui.dataset;

import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionFactory;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.ui.OwnerModalDialog;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.SimpleObjectFormat;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.12 $
 */

public class CriteriaBeanCollectionEditor extends OwnerModalDialog implements BeanCollectionEditor {
    private int WIDTH = 600;
    private int HEIGHT = 300;

    private static final String ADD_ICON = "add.gif";
    private static final String REMOVE_ICON = "remove.gif";
    private static final String EDIT_ICON = "edit.gif";
    private static final String CRITERIA_KEY = "dataset";

    private static final SimpleObjectFormat FILTER_CONDITION = new SimpleObjectFormat("<html><font color=\"BLACK\">%description%</font></html>");
    private static final SimpleObjectFormat READONLY_FILTER_CONDITION = new SimpleObjectFormat("<html><font color=\"GRAY\">%description%</font></html>");

    private Class beanClass;
    private ArrayList criterionList;

    private JTable criterionTable;
    private CriterionTableModel criterionTableModel;
    private JTextField nameTextField;

    private boolean ok = false;
    private JButton okButton;


    public CriteriaBeanCollectionEditor(Class beanCollectionType, org.bhavaya.beans.criterion.CriteriaBeanCollection criteriaBeanCollection, Frame owner, String title) {
        super(owner, title);
        init(beanCollectionType, criteriaBeanCollection);
    }

    public CriteriaBeanCollectionEditor(Class beanCollectionType, org.bhavaya.beans.criterion.CriteriaBeanCollection criteriaBeanCollection, Dialog owner, String title) {
        super(owner, title);
        init(beanCollectionType, criteriaBeanCollection);
    }

    private void init(Class beanCollectionType, org.bhavaya.beans.criterion.CriteriaBeanCollection criteriaBeanCollection) {
        ArrayList criteriaList;
        if (criteriaBeanCollection != null) {
            criteriaList = new ArrayList(Arrays.asList(criteriaBeanCollection.getPrimaryCriteria().getCriteria()));
        } else {
            criteriaList = new ArrayList(CriterionFactory.getDefaultCriterion(beanCollectionType));
        }
        String name = criteriaBeanCollection == null ? "" : criteriaBeanCollection.getPrimaryCriteria().getName();
        init(beanCollectionType, criteriaList, name);
    }

    private void init(Class beanCollectionType, ArrayList criteriaList, String name) {
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.beanClass = beanCollectionType;
        this.criterionList = criteriaList;

        criterionTableModel = new CriterionTableModel();
        criterionTable = new JTable(criterionTableModel);
        ToolTipManager.sharedInstance().registerComponent(criterionTable);
        criterionTable.getColumnModel().getColumn(0).setCellRenderer(new CriterionTableCellRenderer(beanClass));
        criterionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        criterionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        criterionTable.setRowSelectionAllowed(true);
        criterionTable.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(criterionTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        criterionTable.doLayout();

        JButton addButton = new JButton(new AddAction());
        addButton.setFocusable(false);
        addButton.setToolTipText("Add new criterion");

        JButton removeButton = new JButton(new RemoveAction());
        removeButton.setFocusable(false);
        removeButton.setToolTipText("Remove selected criterion");

        JButton editButton = new JButton(new EditAction());
        editButton.setFocusable(false);
        editButton.setToolTipText("Edit selected criterion");

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.add(addButton);
        toolBar.add(removeButton);
        toolBar.add(editButton);
        toolBar.setFocusable(false);

        nameTextField = new JTextField(20);
        nameTextField.setText(name);
        nameTextField.getDocument().addDocumentListener(UIUtilities.triggerMethodOnEvent(DocumentListener.class, null, this, "validateConfirmationPossible"));

        okButton = new JButton(new OkAction());
        JButton cancelButton = new JButton(new CancelAction());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(okButton);
        southPanel.add(cancelButton);

        JPanel criterionPanel = new JPanel(new BorderLayout());
        criterionPanel.add(toolBar, BorderLayout.NORTH);
        criterionPanel.add(scrollPane, BorderLayout.CENTER);
        criterionPanel.setBorder(BorderFactory.createRaisedBevelBorder());

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));
        namePanel.add(new JLabel(BeanCollectionGroup.getDefaultInstance(beanClass).getPluralDisplayName() + " Collection Name"));
        namePanel.add(nameTextField);

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(namePanel, BorderLayout.NORTH);
        containerPanel.add(criterionPanel, BorderLayout.CENTER);
        containerPanel.add(southPanel, BorderLayout.SOUTH);

        setContentPane(containerPanel);
        setSize(WIDTH, HEIGHT);
        UIUtilities.centreInScreen(this, 0, 0);

        validateConfirmationPossible();
    }

    public BeanCollection editBeanCollection() {
        setVisible(true);
        if (!ok) return null;

        String criteriaGroupName = nameTextField.getText();
        Criterion[] criteria = (Criterion[]) criterionList.toArray(new Criterion[criterionList.size()]);
        if (criteriaGroupName.length() == 0 || criteria.length == 0) return null;
        return createBeanCollection(criteriaGroupName, criteria);
    }

    protected BeanCollection createBeanCollection(String criteriaGroupName, Criterion[] criteria) {
        return new CriteriaBeanCollection(beanClass, new CriterionGroup(criteriaGroupName, criteria));
    }

    public void validateConfirmationPossible() {
        boolean confirmationPossible = nameTextField.getText().length() > 0;
        confirmationPossible &= criterionList.size() > 0;
        okButton.setEnabled(confirmationPossible);
    }
    
    protected CriterionDialog createCriterionDialog(Criterion initialCriterion, Class beanClass) {
        return new CriterionDialog(initialCriterion, beanClass, CriteriaBeanCollectionEditor.this, CRITERIA_KEY);
    }

    private class CriterionTableModel extends AbstractTableModel {
        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int column) {
            return "Filter Condition";
        }

        public int getRowCount() {
            return criterionList.size() + CriterionFactory.getCompulsoryCriterion(beanClass).size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Criterion criterion;

            boolean readOnly = false;
            if (rowIndex >= criterionList.size()) {
                readOnly = true;
            }

            if (readOnly) {
                criterion = (Criterion) CriterionFactory.getCompulsoryCriterion(beanClass).get(rowIndex - criterionList.size());
            } else {
                criterion = (Criterion) criterionList.get(rowIndex);
            }

            return criterion;
        }

        public void rowDeleted(int index) {
            fireTableRowsDeleted(index, index);
        }

        public void rowAdded(int index) {
            fireTableRowsInserted(index, index);
        }

        public void rowChanged(int index) {
            fireTableRowsUpdated(index, index);
        }
    }

    private static class CriterionTableCellRenderer extends DefaultTableCellRenderer {
        private Class beanClass;

        public CriterionTableCellRenderer(Class beanClass) {
            super();
            this.beanClass = beanClass;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            label.setToolTipText(null);

            Criterion criterion = (Criterion) value;
            String tooltipText = criterion.getDescription();
            if (tooltipText != null) {
                tooltipText = Utilities.wrapWithSplitOnNewLine(tooltipText, 80);
                label.setToolTipText("<HTML>" + tooltipText.replaceAll("\n", "<BR>") + "</HTML>");
            }

            boolean readOnly = CriterionFactory.isCompulsoryCriterion(beanClass, criterion);
            if (readOnly) {
                label.setText(READONLY_FILTER_CONDITION.formatObject(criterion));
            } else {
                label.setText(FILTER_CONDITION.formatObject(criterion));
            }
            return label;
        }
    }


    private class AddAction extends AuditedAbstractAction {
        public AddAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(ADD_ICON));
        }

        public void auditedActionPerformed(ActionEvent e) {
            CriterionDialog criterionDialog = createCriterionDialog(null, beanClass);
            UIUtilities.centreInContainer(CriteriaBeanCollectionEditor.this, criterionDialog, 0, 0);
            criterionDialog.setVisible(true);
            Criterion newCriterion = criterionDialog.getCriterion();

            if (newCriterion != null) {
                criterionList.add(newCriterion);
                criterionTableModel.rowAdded(criterionList.size());
                validateConfirmationPossible();
            }
        }
    }

    private class RemoveAction extends AuditedAbstractAction {
        public RemoveAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(REMOVE_ICON));
            setEnabled(isEnabled());
            criterionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            int selectedRowIndex = criterionTable.getSelectedRow();
            if (selectedRowIndex >= criterionList.size()) return;

            criterionList.remove(selectedRowIndex);
            criterionTableModel.rowDeleted(selectedRowIndex);
            validateConfirmationPossible();
        }

        public boolean isEnabled() {
            return criterionTable.getSelectedRow() != -1;
        }
    }

    private class EditAction extends AuditedAbstractAction {
        public EditAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(EDIT_ICON));
            setEnabled(isEnabled());
            criterionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    setEnabled(isEnabled());
                }
            });
        }

        public void auditedActionPerformed(ActionEvent e) {
            int selectedRowIndex = criterionTable.getSelectedRow();
            if (selectedRowIndex >= criterionList.size()) return;

            CriterionDialog criterionDialog = createCriterionDialog((Criterion) criterionList.get(selectedRowIndex), beanClass);
            UIUtilities.centreInContainer(CriteriaBeanCollectionEditor.this, criterionDialog, 0, 0);
            criterionDialog.setVisible(true);
            Criterion newCriterion = criterionDialog.getCriterion();

            if (newCriterion != null) {
                criterionList.set(selectedRowIndex, newCriterion);
                criterionTableModel.rowChanged(selectedRowIndex);
                validateConfirmationPossible();
            }
        }

        public boolean isEnabled() {
            return criterionTable.getSelectedRow() != -1;
        }
    }

    private class OkAction extends AuditedAbstractAction {
        public OkAction() {
            putValue(Action.NAME, "Ok");
        }

        public void auditedActionPerformed(ActionEvent e) {
            ok = true;
            CriteriaBeanCollectionEditor.this.dispose();
        }
    }

    private class CancelAction extends AuditedAbstractAction {
        public CancelAction() {
            putValue(Action.NAME, "Cancel");
        }

        public void auditedActionPerformed(ActionEvent e) {
            CriteriaBeanCollectionEditor.this.dispose();
        }
    }
}
