package org.bhavaya.ui.table;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.HighlightedTable;

import javax.swing.*;
import java.awt.*;

/**
 * Test GUI for TableCellToolTip class.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class TableToolTipTest {

    public void testTableToolTips() {
        BeanCollection beanCollection = prepareData();
        BeanCollectionTableModel tableModel = new BeanCollectionTableModel(beanCollection, true);
        tableModel.addColumnLocator("b.b.date");
        tableModel.addColumnLocator("longString");
        tableModel.addColumnLocator("b.b.c.someDouble");
        tableModel.addColumnLocator("b.b.c.someString");

        AnalyticsTable table1 = new AnalyticsTable(tableModel, true);
        AnalyticsTable table2 = new AnalyticsTable(tableModel, true);
        table2.setIntercellSpacing(new Dimension(6, 8));
        AnalyticsTable table3 = new AnalyticsTable(tableModel, true);
        table3.setIntercellSpacing(new Dimension(0, 0));

        AnalyticsTable table4 = new AnalyticsTable(tableModel, true);
        table4.setShowGrid(false);
        AnalyticsTable table5 = new AnalyticsTable(tableModel, true);
        table5.setShowGrid(false);
        table5.setIntercellSpacing(new Dimension(21, 20));
        table5.setRowHeight(50);
        AnalyticsTable table6 = new AnalyticsTable(tableModel, true);
        table6.setShowGrid(false);
        table6.setIntercellSpacing(new Dimension(0, 0));

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();

        contentPane.setLayout(new GridLayout(3, 2));
        contentPane.add(new JScrollPane(table1));
        contentPane.add(new JScrollPane(table2));
        contentPane.add(new JScrollPane(table3));
        contentPane.add(new JScrollPane(table4));
        contentPane.add(new JScrollPane(table5));
        contentPane.add(new JScrollPane(table6));

        HighlightedTable.setDisplayFullCellValueOnHover(true);

        frame.pack();
        frame.setVisible(true);
    }

    private BeanCollection prepareData() {
        BeanCollection beanCollection = new DefaultBeanCollection(BeanA.class);
        RandomDataFeed dataFeed = new RandomDataFeed(beanCollection);
        dataFeed.run();
        return beanCollection;
    }

    public static void main(String[] args) {
        new TableToolTipTest().testTableToolTips();
    }
}
