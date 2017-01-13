package org.bhavaya.ui.table;

import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.BeanActionFactory;
import org.bhavaya.ui.AcceleratorAction;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.view.ActionGroup;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This creates a dynamic matrix using the pivoting functionality.
 *
 * @author <a href="mailto:Sabine.Haas@drkw.com">Sabine Haas, Dresdner Kleinwort Wasserstein</a>
 * @version $Revision: 1.1 $
 */
public class MatrixTableTest extends JFrame {
    public JLabel unfixedActionBeanCount = new JLabel(" ");
    public JLabel fixedActionBeanCount = new JLabel(" ");

    private final static String[] portfolioNames = {"Germany", "England", "Ireland", "South Africa"};
    private final static String[] marketNames = {"Market1", "Market2", "Market3", "Market4"};

    public static void main(String[] args) {
        MatrixTableTest matrixTableTest = new MatrixTableTest();
        matrixTableTest.setTitle("Matrix Table Test");
        matrixTableTest.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = matrixTableTest.getContentPane();
        contentPane.setLayout(new GridLayout(2, 1));

        JPanel unfixedTablePanel = new JPanel(new BorderLayout());
        unfixedTablePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JComponent quoteManagementTable = matrixTableTest.createQuoteManagementTable(false);
        unfixedTablePanel.add(new JLabel("Table without Fixed Columns"),BorderLayout.NORTH);
        unfixedTablePanel.add(quoteManagementTable, BorderLayout.CENTER);
        unfixedTablePanel.add(matrixTableTest.unfixedActionBeanCount, BorderLayout.SOUTH);
        unfixedTablePanel.setPreferredSize(new Dimension(600, 130));

        JPanel fixedTablePanel = new JPanel(new BorderLayout());
        fixedTablePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JComponent fixedQuoteManagementTable = matrixTableTest.createQuoteManagementTable(true);
        fixedTablePanel.add(new JLabel("Table with Fixed Columns"),BorderLayout.NORTH);
        fixedTablePanel.add(fixedQuoteManagementTable, BorderLayout.CENTER);
        fixedTablePanel.add(matrixTableTest.fixedActionBeanCount, BorderLayout.SOUTH);
        fixedTablePanel.setPreferredSize(new Dimension(600, 130));

        contentPane.add(unfixedTablePanel);
        contentPane.add(fixedTablePanel);

        matrixTableTest.pack();
        matrixTableTest.show();
    }


    public JComponent createQuoteManagementTable(final boolean useFixedColumns) {
        final DefaultBeanCollection beanCollection = new DefaultBeanCollection(Foo.class);
        for (int i = 0; i < portfolioNames.length; i++) {
            String portfolioName = portfolioNames[i];
            for (int j = 0; j < marketNames.length; j++) {
                String marketName = marketNames[j];
                beanCollection.add(new Foo(marketName, portfolioName, Foo.OFF));
            }
        }

        final BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(Foo.class, false);
        final AnalyticsTable analyticsTable = new AnalyticsTable(beanCollectionTableModel, true);

        analyticsTable.setBeanActionFactory(new BeanActionFactory() {
            public ActionGroup getActions(final Object[] beans) {
                ActionGroup actions = new ActionGroup("");
                if (useFixedColumns) {
                    fixedActionBeanCount.setText("BeanActionFactory.getActions bean count: " + beans.length);
                } else {
                    unfixedActionBeanCount.setText("BeanActionFactory.getActions bean count: " + beans.length);
                }
                if (beans.length > 0) {
                    actions.addAction(new MyAction(beans, true));
                    actions.addAction(new MyAction(beans, false));
                }

                return actions;
            }


            public Action getAction(Object bean, String beanPath) {
                return null;
            }

            public AcceleratorAction[] getAcceleratorActions() {
                return null;
            }
        });

        beanCollectionTableModel.removeAllColumnLocators();
        beanCollectionTableModel.setBeanCollection(beanCollection);

        beanCollectionTableModel.addColumnLocator("portfolio");
        beanCollectionTableModel.addColumnLocator("market");
        beanCollectionTableModel.addColumnLocator("status");

        AnalyticsTableModel analyticsTableModel = analyticsTable.getAnalyticsTableModel();
        analyticsTableModel.addSortingColumn(beanCollectionTableModel.getColumnKey(0), false);

        analyticsTableModel.setGrouped(true);
        analyticsTableModel.setPivoted(true);

        analyticsTableModel.setPivotColumn("market");
        analyticsTableModel.setPivotDataColumn("status");

        TableColumnModel columnModel = analyticsTable.getScrollableTable().getColumnModel();
        columnModel.getColumn(0).setMinWidth(180);
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setMinWidth(100);
        }

        if (useFixedColumns) {
            analyticsTable.setColumnFixed(0, true);
        }
        analyticsTable.setFading(false);
        analyticsTable.setAnimated(false);

        analyticsTable.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value == null) return label;
                Integer status = (Integer) value;
                label.setText(status.intValue() == Foo.ON ? "On" : "Off");

                return label;
            }
        });

        return analyticsTable;
    }

    private class MyAction extends AbstractAction {
        private final Object[] beans;
        private boolean on;

        public MyAction(Object[] beans, boolean on) {
            super("Switch " + (on ? "On" : "Off"));
            this.on = on;
            this.beans = beans;
        }

        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < beans.length; i++) {
                Foo bean = (Foo) beans[i];
                bean.setStatus(on ? Foo.ON : Foo.OFF);
            }
        }
    }

    public class Foo extends org.bhavaya.beans.Bean {
        public static final int OFF = 0;
        public static final int ON = 1;
        private String portfolio;
        private String market;
        private int status = OFF;

        public Foo(String market, String portfolio, int status) {
            this.market = market;
            this.portfolio = portfolio;
            this.status = status;
        }

        public String getMarket() {
            return market;
        }

        public void setMarket(String market) {
            String oldValue = this.market;
            this.market = market;
            firePropertyChange("market", oldValue, this.market);
        }

        public String getPortfolio() {
            return portfolio;
        }

        public void setPortfolio(String portfolio) {
            String oldValue = this.portfolio;
            this.portfolio = portfolio;
            firePropertyChange("portfolio", oldValue, this.portfolio);
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            int oldValue = this.status;
            this.status = status;
            firePropertyChange("status", oldValue, this.status);
        }
    }

}
