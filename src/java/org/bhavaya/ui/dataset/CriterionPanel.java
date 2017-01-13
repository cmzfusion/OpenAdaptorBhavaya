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

import org.bhavaya.beans.criterion.*;
import org.bhavaya.ui.*;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Description
 *
 * @author ?
 * @version $Revision: 1.17 $
 */
public class CriterionPanel extends JPanel {
    protected static final String NOTHING_KEY = "Nothing";
    protected static final String BASIC_STRING_KEY = "BasicString";
    protected static final String BASIC_NUMBER_KEY = "BasicNumber";
    protected static final String BASIC_DATE_KEY = "BasicDate";
    protected static final String BASIC_BOOLEAN_KEY = "BasicBoolean";
    protected static final String BASIC_LIST_KEY = "BasicList";
    protected static final String ENUM_KEY = "Enum";
    protected static final String TREE_KEY = "Tree";

    private static final int PREFERRED_WIDTH = 400;
    private static final int DEFAULT_VGAP = 5;
    private static final int DEFAULT_HGAP = 10;

    private Criterion activeCriterion = null;

    protected JTree criteriaTree;
    protected CriterionView nothingSelectedView;
    protected CriterionView basicStringSelectedView;
    protected CriterionView basicListSelectedView;
    protected CriterionView basicNumberSelectedView;
    protected CriterionView basicDateSelectedView;
    protected CriterionView basicBooleanSelectedView;
    protected CriterionView enumSelectedView;
    protected CriterionView treeSelectedView;
    protected CriterionView activeView;
    protected JPanel contextPanel;

    public CriterionPanel(Criterion initialCriterion, String criteriaKey, Class beanType) {
        criteriaTree = new JTree(CriterionFactory.getCriterionTree(criteriaKey, "Criteria", beanType));
        criteriaTree.setBorder(BorderFactory.createEtchedBorder());
        criteriaTree.setVisibleRowCount(6);
        criteriaTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        criteriaTree.addTreeSelectionListener(new TreeHandler());

        JScrollPane treeScrollPane = new JScrollPane(criteriaTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        nothingSelectedView = new NothingSelectedPanel();
        basicStringSelectedView = new BasicPanel();
        basicNumberSelectedView = new BasicNumberPanel();
        basicDateSelectedView = new BasicDatePanel();
        basicBooleanSelectedView = new BasicBooleanPanel();
        enumSelectedView = new EnumerationCriterionEditor();
        treeSelectedView = new TreePanel();
        basicListSelectedView = new BasicListPanel();
        activeView = nothingSelectedView;

        contextPanel = new JPanel(new CardLayout());
        contextPanel.add((Component) nothingSelectedView, NOTHING_KEY);
        contextPanel.add((Component) basicStringSelectedView, BASIC_STRING_KEY);
        contextPanel.add((Component) basicListSelectedView, BASIC_LIST_KEY);
        contextPanel.add((Component) basicNumberSelectedView, BASIC_NUMBER_KEY);
        contextPanel.add((Component) basicDateSelectedView, BASIC_DATE_KEY);
        contextPanel.add((Component) basicBooleanSelectedView, BASIC_BOOLEAN_KEY);
        contextPanel.add((Component) enumSelectedView, ENUM_KEY);
        contextPanel.add((Component) treeSelectedView, TREE_KEY);
        contextPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout());
        add(treeScrollPane, BorderLayout.WEST);
        add(contextPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createEtchedBorder());

        viewCriterion(initialCriterion);
    }

    public void setActiveCriterion() throws ValidationException {
        this.activeCriterion = activeView.createCriterion();
    }

    public Criterion getCriterion() {
        return activeCriterion;
    }

    protected void viewCriterion(final Criterion item) {
        if (item instanceof TreeCriterion) {
            activeView = treeSelectedView;
            treeSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, TREE_KEY);
        } else if (item instanceof EnumerationCriterion) {
            activeView = enumSelectedView;
            enumSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, ENUM_KEY);
        } else if (item instanceof BasicCriterion && java.util.Date.class.isAssignableFrom(((BasicCriterion) item).getToBeanType())) {
            activeView = basicDateSelectedView;
            basicDateSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, BASIC_DATE_KEY);
        } else if (item instanceof BasicCriterion && Number.class.isAssignableFrom(ClassUtilities.typeToClass(((BasicCriterion) item).getToBeanType()))) {
            activeView = basicNumberSelectedView;
            basicNumberSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, BASIC_NUMBER_KEY);
        } else if (item instanceof BasicCriterion && String.class.isAssignableFrom(((BasicCriterion) item).getToBeanType())) {
            activeView = basicStringSelectedView;
            basicStringSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, BASIC_STRING_KEY);
        } else if (item instanceof BasicCriterion && Boolean.class.isAssignableFrom(ClassUtilities.typeToClass(((BasicCriterion) item).getToBeanType()))) {
            activeView = basicBooleanSelectedView;
            basicBooleanSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, BASIC_BOOLEAN_KEY);
        } else if (item == null) {
            activeView = nothingSelectedView;
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, NOTHING_KEY);
        } else if (item instanceof OrCriterion) {
            activeView = basicListSelectedView;
            basicListSelectedView.setCriterion(item);
            ((CardLayout) contextPanel.getLayout()).show(contextPanel, BASIC_LIST_KEY);
        }
    }

    protected static class NothingSelectedPanel extends JPanel implements CriterionView {
        public NothingSelectedPanel() {
            super(new RowLayout(PREFERRED_WIDTH, DEFAULT_VGAP));

            JLabel messageLabel = new JLabel("Please select a criterion from the left");

            RowLayout rowLayout = (RowLayout) getLayout();
            RowLayout.Row row = new RowLayout.Row(DEFAULT_HGAP, RowLayout.LEFT, RowLayout.TOP, false);
            add(row.addComponent(messageLabel));
            rowLayout.addRow(row);
        }

        public Criterion createCriterion() throws ValidationException {
            throw new ValidationException(new String[]{"Please select a criterion type from the left panel"});
        }

        public void setCriterion(Criterion criterion) {
        }
    }

    /**
     * criterion view for instances of BasicCriterion
     */
    protected class BasicPanel extends JPanel implements CriterionView {
        private JLabel attributeLabel;
        protected JComboBox operators;
        protected Component valueField;

        protected BasicCriterion templateCriterion;

        public BasicPanel() {
            super(new RowLayout(PREFERRED_WIDTH, DEFAULT_VGAP));

            attributeLabel = new JLabel();
            operators = createOperatorsComboBox();
            valueField = createValueField();

            RowLayout rowLayout = (RowLayout) getLayout();
            RowLayout.Row row = new RowLayout.Row(DEFAULT_HGAP, RowLayout.LEFT, RowLayout.MIDDLE, false);
            add(row.addComponent(attributeLabel, new RowLayout.PreferredWidthConstraint(attributeLabel)));
            if (operators != null) add(row.addComponent(operators));
            rowLayout.addRow(row);
            if (!(valueField instanceof JTextField)) {
                row = new RowLayout.Row(DEFAULT_HGAP, RowLayout.LEFT, RowLayout.MIDDLE, false);
            }
            add(row.addComponent(valueField));
            rowLayout.addRow(row);
        }

        protected JComboBox createOperatorsComboBox() {
            return new JComboBox(new String[]{"=", "<", ">", "<=", ">=", "<>"});
        }

        protected Component createValueField() {
            return new JTextField(15);
        }

        public void setCriterion(Criterion criterion) {
            this.templateCriterion = (BasicCriterion) criterion;
            attributeLabel.setText(templateCriterion.getName());
            if (operators != null) {
                operators.setSelectedItem(templateCriterion.getOperator());
            }
            setValueField(templateCriterion.getRightOperand());
        }

        protected Object getValueField() {
            return ((JTextField) valueField).getText();
        }

        protected void setValueField(Object value) {
            String text = value != null ? value.toString() : null;
            ((JTextField) valueField).setText(text);
        }

        public Criterion createCriterion() throws ValidationException {
            String operator;
            if (operators != null) {
                operator = (String) operators.getSelectedItem();
            } else {
                operator = Criterion.OPERATOR_EQUALS;
            }

            Object rightOperand = getValueField();

            // Validate all fields and throw an error if required.
            ArrayList invalidFields = new ArrayList();
            if (operator == null) invalidFields.add("A valid operator must be chosen");
            if (rightOperand == null) invalidFields.add("A valid value must be entered after the operator");
            if (invalidFields.size() > 0)
                throw new ValidationException((String[]) invalidFields.toArray(new String[invalidFields.size()]));
            if (valueField instanceof JTextField) {
                valueField.requestFocus();
                ((JTextField) valueField).setText("");
            }
            return CriterionFactory.newCriterion(templateCriterion.getCriterionType(), templateCriterion.getId(), operator, rightOperand);
        }
    }

    protected class BasicListPanel extends JPanel implements CriterionView {

        private final Log log = Log.getCategory(BasicListPanel.class);
        private BasicPanel selectionPanel;
        private JList addedCriterion;
        private DefaultListModel model;
        JButton addButton = new JButton(ImageIconCache.getImageIcon("add.gif"));
        JButton removeButton = new JButton(ImageIconCache.getImageIcon("remove.gif"));

        protected OrCriterion templateCriterion;

        public BasicListPanel() {
            super(new RowLayout(PREFERRED_WIDTH, DEFAULT_VGAP));
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            selectionPanel = new BasicPanel();
            selectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            Dimension buttonDimension = new Dimension(25, 25);
            addButton.setMaximumSize(buttonDimension);
            addButton.setMinimumSize(buttonDimension);
            addButton.setPreferredSize(buttonDimension);
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        BasicCriterion criterion = (BasicCriterion)selectionPanel.createCriterion();
                        Object rightOperand = criterion.getRightOperand();
                        if (rightOperand instanceof String){
                            String operand = (String)rightOperand;
                            if (operand.length()> 0){
                                 addCriterion(criterion);
                            }
                        }
                    } catch (ValidationException e1) {
                        log.warn("Unable to add Criterion ", e1);
                    }
                }
            });
            removeButton.setMaximumSize(buttonDimension);
            removeButton.setMinimumSize(buttonDimension);
            removeButton.setPreferredSize(buttonDimension);
            removeButton.setEnabled(false);
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int index = addedCriterion.getSelectedIndex();
                    if (index >= 0) {
                        model.remove(index);
                        addedCriterion.setSelectedIndex(index);
                    }
                }
            });
            addToGridBagLayout(mainPanel, selectionPanel, gbc, 0, 0, 3, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER, 0, 0, 0, 0);
            addToGridBagLayout(mainPanel, addButton, gbc, 3, 0, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0, 0, 0, 0);
            addToGridBagLayout(mainPanel, removeButton, gbc, 4, 0, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0, 0, 0, 0);

            model = new DefaultListModel();
            addedCriterion = new JList(model);
            JScrollPane scrollableList = new JScrollPane(addedCriterion);
            addedCriterion.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            Dimension listDimension =  new Dimension(300, 150);
            scrollableList.setPreferredSize(listDimension);
            scrollableList.setMaximumSize(listDimension);
            scrollableList.setMaximumSize(listDimension);


            gbc.insets = new Insets(10, 0, 0, 0);
            addToGridBagLayout(mainPanel, scrollableList, gbc, 0, 4, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH, 0, 0, 0, 150);
            addedCriterion.setCellRenderer(new ListCellRenderer() {

                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    BasicCriterion criterion = (BasicCriterion) value;
                    JLabel label = new JLabel(" " + criterion.getDescription());
                    label.setOpaque(true);
                    if (isSelected) {
                        label.setBackground(list.getSelectionBackground());
                        label.setForeground(list.getSelectionForeground());
                    } else {
                        label.setBackground(list.getBackground());
                        label.setForeground(list.getForeground());
                    }
                    return label;
                }
            });

            addedCriterion.addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int selectedIdex = addedCriterion.getSelectedIndex();
                        if (selectedIdex >= 0) {
                            removeButton.setEnabled(true);
                        } else {
                            removeButton.setEnabled(false);
                        }
                    }
                }
            });

            RowLayout rowLayout = (RowLayout) getLayout();
            RowLayout.Row row = new RowLayout.Row(0, RowLayout.LEFT, RowLayout.TOP, true);
            add(row.addComponent(mainPanel, new RowLayout.PreferredWidthConstraint(mainPanel)));
            rowLayout.addRow(row);
        }

        public void setCriterion(Criterion criterion) {
            String id = "";
            this.templateCriterion = (OrCriterion) criterion;
            BasicCriterion[] criterias = templateCriterion.getCriteria();
            for (BasicCriterion basicCriterion : criterias) {
                id = basicCriterion.getId();
                if (basicCriterion.getRightOperand() != null) {
                    addCriterion(basicCriterion);
                }
            }
            selectionPanel.setCriterion(CriterionFactory.newCriterion(BasicCriterion.BASIC,
                    id, Criterion.OPERATOR_EQUALS, null));
        }

        protected void addCriterion(Criterion value) {
            if (!model.contains(value)) {
                model.addElement(value);
            }
        }

        public Criterion createCriterion() throws ValidationException {
            int size = model.getSize();
            BasicCriterion[] criterions = new BasicCriterion[size];
            Enumeration enums = model.elements();
            BasicCriterion criterion = null;
            int count = 0;
            while (enums.hasMoreElements()) {
                criterion = (BasicCriterion) enums.nextElement();
                criterions[count] = criterion;
                count++;
            }
            return CriterionFactory.newCriterion(BasicCriterion.LIST, null, null, criterions);
        }

        private void addToGridBagLayout(JPanel panel, Component component, GridBagConstraints gbc, int gridx, int gridy,
                                        int gridwidth, int fill, int anchor, int weightx, int weighty,
                                        int ipadx, int ipady) {
            gbc.gridx = gridx;
            gbc.gridy = gridy;
            gbc.gridwidth = gridwidth;
            gbc.fill = fill;
            gbc.anchor = anchor;
            gbc.weightx = weightx;
            gbc.weighty = weighty;
            gbc.ipadx = ipadx;
            gbc.ipady = ipady;
            panel.add(component, gbc);
        }
    }

    protected class BasicNumberPanel extends BasicPanel implements CriterionView {
        protected Component createValueField() {
            return new DecimalTextField("###,###.###");
        }

        protected Object getValueField() {
            return ((DecimalTextField) valueField).getValue();
        }

        protected void setValueField(Object value) {
            ((DecimalTextField) valueField).setValue((Number) value);
        }
    }

    protected class BasicDatePanel extends BasicPanel implements CriterionView {
        protected Component createValueField() {
            return new DateFunctionPanel();
        }

        protected Object getValueField() {
            return ((DateFunctionPanel) valueField).getValue();
        }

        protected void setValueField(Object value) {
            ((DateFunctionPanel) valueField).setValue(value);
        }
    }

    protected class BasicBooleanPanel extends BasicPanel implements CriterionView {
        protected Component createValueField() {
            return new JCheckBox();
        }

        protected Object getValueField() {
            return Boolean.valueOf(((JCheckBox) valueField).isSelected());
        }

        protected void setValueField(Object value) {
            if (value == null) value = Boolean.FALSE;
            boolean b = ((Boolean) value).booleanValue();
            ((JCheckBox) valueField).setSelected(b);
        }

        protected JComboBox createOperatorsComboBox() {
            return null;
        }
    }

    protected class TreePanel extends JPanel implements CriterionView {
        private JLabel attributeLabel;
        private CheckTree tree;
        private TreeCriterionCheckModel treeModel;

        private TreeCriterion templateCriterion;

        public TreePanel() {
            super(new BorderLayout());

            attributeLabel = new JLabel();
            attributeLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

            tree = new CheckTree();
            JScrollPane scrollPane = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tree.setPopupOwner(scrollPane, scrollPane.getInsets().right, scrollPane.getInsets().right);

            add(attributeLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void setCriterion(Criterion criterion) {
            this.templateCriterion = (TreeCriterion) criterion;
            this.treeModel = new TreeCriterionCheckModel(templateCriterion);
            tree.setModel(treeModel);
            attributeLabel.setText(criterion.getName() + " is one in:");
        }

        public Criterion createCriterion() throws ValidationException {
            TreeCriterion criterion = new TreeCriterion(templateCriterion.getId(), treeModel.getRightOperand());
            criterion.getRightOperand();
            return criterion;
        }
    }

    protected class TreeHandler implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = ((DefaultMutableTreeNode) criteriaTree.getLastSelectedPathComponent());
            Criterion item = node == null ? null : (Criterion) node.getUserObject();
            viewCriterion(item);
        }
    }

    public static interface CriterionView {
        public void setCriterion(Criterion criterion);

        public Criterion createCriterion() throws ValidationException;
    }


    public static class ValidationException extends Exception {
        private String[] invalidFields;

        public ValidationException(String[] invalidFields) {
            super("Criterion validation exception");
            this.invalidFields = invalidFields;
        }

        public String[] getInvalidFields() {
            return invalidFields;
        }

        public String createDetailString() {
            StringBuffer messageBuffer = new StringBuffer("<html>");
            String[] reasons = this.getInvalidFields();
            for (int i = 0; i < reasons.length; i++) {
                messageBuffer.append(reasons[i]).append("<br>");
            }
            return messageBuffer.append("</ul></html>").toString();
        }
    }
}
