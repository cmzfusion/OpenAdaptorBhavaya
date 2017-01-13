package org.bhavaya.ui.table;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.HighlightedTable;
import org.bhavaya.util.PropertyModel;

import javax.swing.*;
import java.awt.*;

/**
 * Test GUI for TableCellToolTip class.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class TableHeaderToolTipTest {

    public void testTableToolTips() {
        BeanCollection beanCollection = prepareData();
        BeanCollectionTableModel tableModel = new BeanCollectionTableModel(beanCollection, true);
        tableModel.addColumnLocator("b.b.date");
        tableModel.addColumnLocator("longString");
        tableModel.addColumnLocator("b.b.c.someDouble");
        tableModel.addColumnLocator("b.b.c.someString");


        PropertyModel propertyModel = PropertyModel.getInstance(BeanA.class);
        propertyModel.setDescription("longString", "<p> Applicable only to Spread pricing method. Different formula is used depending on whether only one or two buckets are selected.</p> <pre>  spread = (midYield - bucket1.inhousePrice.midYield) * 100</pre> or <pre>  spread = (2*midYield - bucket1.inhousePrice.midYield - bucket2.inhousePrice.midYield) * 100</pre>");
        propertyModel = PropertyModel.getInstance(BeanC.class);
        propertyModel.setDescription("someString", "test description test description test description test description test description test description test description test description test description test description test description test description test description test description test description test description ");

        AnalyticsTable table = new AnalyticsTable(tableModel, true);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();

        contentPane.setLayout(new BorderLayout(3, 3));
        contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

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
        new TableHeaderToolTipTest().testTableToolTips();
    }

}
