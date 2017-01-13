package org.bhavaya.ui.table;

import org.bhavaya.ui.FastGradientPaint;
import org.bhavaya.ui.SpringUtilities;
import org.bhavaya.util.Numeric;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Aug 21, 2006
 * Time: 5:26:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiLineHeaderRenderer extends JPanel implements TableCellRenderer, ListCellRenderer {
    private static final java.util.regex.Pattern newLineSeperatarPattern = java.util.regex.Pattern.compile("\n");
    private static final Color DEFAULT_GRADIENT_END_COLOR = new Color(222, 221, 205);

    private TableCellRenderer delegateTableCellRenderer;
    private ListCellRenderer delegateListCellRenderer;

    private JLabel iconLabel = new JLabel();
    private JList list = new JList();
    private DefaultListModel listModel = new DefaultListModel();
    private Border headerBorder;

    private Component topSpacer = Box.createGlue();
    private Component bottomSpacer = Box.createGlue();

    private Class columnClass; // Temp variable to pass info from table cell renderer to list cell renderer

    private Color gradientStartColor;
    private Color gradientEndColor;


    public MultiLineHeaderRenderer(TableCellRenderer delegateTableCellRenderer) {
        this.delegateTableCellRenderer = delegateTableCellRenderer;
        this.delegateListCellRenderer = list.getCellRenderer();
        list.setModel(listModel);
        list.setOpaque(false);
        list.setCellRenderer(this);
        setDefaultGradientColors();

        add(list);
        add(iconLabel);
        add(topSpacer);
        add(bottomSpacer);

        customiseLayout();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel delegate = (JLabel) delegateTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (headerBorder == null) headerBorder = BorderFactory.createCompoundBorder(delegate.getBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 5));

        if (value != null) {
            String valueString = value.toString();
            String[] textLines = newLineSeperatarPattern.split(valueString);

            setBorder(headerBorder);
            setToolTipText(valueString.replaceAll("\n", " - "));

            columnClass = table.getColumnClass(column);
            listModel.clear();
            for (int i = 0; i < textLines.length; i++) {
                String textLine = textLines[i];
                listModel.addElement(textLine);
            }
        }

        doLayout();

        return this;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel c = (JLabel) delegateListCellRenderer.getListCellRendererComponent(list, value, index, false, false);
        c.setOpaque(false);

        if (columnClass.isAssignableFrom(Boolean.class)) {
            c.setHorizontalAlignment(JLabel.CENTER);
        } else if (Number.class.isAssignableFrom(columnClass) || Numeric.class.isAssignableFrom(columnClass)) {
            c.setHorizontalAlignment(JLabel.RIGHT);
        } else {
            c.setHorizontalAlignment(JLabel.LEFT);
        }

        return c;
    }

    public void setIcon(Icon icon) {
        iconLabel.setIcon(icon);
    }

    private void customiseLayout() {
        SpringLayout layout = new SpringLayout();
        layout.putConstraint(SpringLayout.EAST, this, 4, SpringLayout.EAST, iconLabel);
        layout.putConstraint(SpringLayout.WEST, iconLabel, 2, SpringLayout.EAST, list);
        layout.putConstraint(SpringLayout.WEST, list, 2, SpringLayout.WEST, this);
        layout.getConstraints(list).setWidth(new SpringUtilities.WidthSpring(list));

        layout.putConstraint(SpringLayout.NORTH, topSpacer, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.NORTH, list, 0, SpringLayout.SOUTH, topSpacer);
        layout.putConstraint(SpringLayout.NORTH, bottomSpacer, 0, SpringLayout.SOUTH, list);
        layout.putConstraint(SpringLayout.SOUTH, this, 1, SpringLayout.SOUTH, bottomSpacer);

        layout.putConstraint(SpringLayout.SOUTH, iconLabel, 0, SpringLayout.SOUTH, list);
        layout.putConstraint(SpringLayout.NORTH, iconLabel, 0, SpringLayout.NORTH, list);

        layout.getConstraints(list).setHeight(new SpringUtilities.AbstractSpring() {
            public int getMinimumValue() {
                return list.getPreferredSize().height;
            }

            public int getPreferredValue() {
                return list.getPreferredSize().height;
            }

            public int getMaximumValue() {
                return list.getPreferredSize().height;
            }
        });
        layout.getConstraints(list).setWidth(new SpringUtilities.AbstractSpring() {
            public int getMinimumValue() {
                return 0;
            }

            public int getPreferredValue() {
                return list.getPreferredSize().width;
            }

            public int getMaximumValue() {
                return Integer.MAX_VALUE;
            }
        });

        setLayout(layout);
    }

    public void setDefaultGradientColors() {
        setGradientColors(Color.WHITE, DEFAULT_GRADIENT_END_COLOR);
    }

    public void setGradientColors(Color startColor, Color endColor) {
        gradientStartColor = startColor;
        gradientEndColor = endColor;
    }

    protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                Graphics2D g2d = (Graphics2D) scratchGraphics;
                Paint gradientPaint = new FastGradientPaint(gradientStartColor, gradientEndColor, true);
                g2d.setPaint(gradientPaint);
                g2d.fill(new Rectangle(0, 0, getWidth(), getHeight()));
            }
            finally {
                scratchGraphics.dispose();
            }
        }
    }

}
