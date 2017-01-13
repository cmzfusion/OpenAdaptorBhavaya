package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.beans.Schema;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.Generic;

import javax.swing.*;
import java.util.Date;

/**
 * Provides unit tests for spliced table.
 *
 * @author vanencd
 * @author Vladimir Hrmo
 */
public class SplicedTableModelTest extends TestCase {

    public void testReordering() {
        Schema.setGenerationMode(true);

        BeanCollection leftCollection = new DefaultBeanCollection(BeanD.class);
        BeanCollection rightCollection = new DefaultBeanCollection(BeanD.class);

        for (int i = 0; i < 50; i++) {
            BeanD beanD = new BeanD("BeanD " + i, i);
            leftCollection.add(beanD);
            rightCollection.add(0, beanD);
        }

        BeanCollectionTableModel leftBeanCollectionTableModel = new BeanCollectionTableModel(leftCollection, false);
        BeanCollectionTableModel rightBeanCollectionTableModel = new BeanCollectionTableModel(rightCollection, false);

        leftBeanCollectionTableModel.addColumnLocator("someString");
        leftBeanCollectionTableModel.addColumnLocator("i");
        rightBeanCollectionTableModel.addColumnLocator("i");
        rightBeanCollectionTableModel.addColumnLocator("someString");

        SplicedTableModel splicedTableModel = new SplicedTableModel(leftBeanCollectionTableModel, "i", rightBeanCollectionTableModel, "i");
        for (int row = 0; row < splicedTableModel.getRowCount(); row++) {
            Object leftValue = splicedTableModel.getValueAt(row, 0);
            Object rightValue = splicedTableModel.getValueAt(row, 1);
            assertEquals("the splicer has incorrectly spliced on row " + row + " leftValue = " + leftValue + " rightValue= " + rightValue, leftValue, rightValue);
        }
        JFrame f = new JFrame();
        f.getContentPane().add(new JScrollPane(new JTable(splicedTableModel)));
        f.pack();
        f.show();
    }

    public void testTableAsynchronous() {
        testTable(true);
    }

    public void testTableSynchronous() {
        testTable(false);
    }

    public void testTable(boolean asynchronous) {
        BeanCollection leftCollection = new DefaultBeanCollection(BeanA.class);
        BeanCollection rightCollection = new DefaultBeanCollection(BeanC.class);

        BeanCollectionTableModel leftBeanCollectionTableModel = new BeanCollectionTableModel(leftCollection, asynchronous);
        BeanCollectionTableModel rightBeanCollectionTableModel = new BeanCollectionTableModel(rightCollection, asynchronous);

        leftBeanCollectionTableModel.addColumnLocator("someDouble");
        leftBeanCollectionTableModel.addColumnLocator("b.date");
        leftBeanCollectionTableModel.addColumnLocator("b.c.d.index");
        leftBeanCollectionTableModel.addColumnLocator("b.c.d.i");
        rightBeanCollectionTableModel.addColumnLocator("d.i");
        rightBeanCollectionTableModel.addColumnLocator("d.index");
        rightBeanCollectionTableModel.addColumnLocator("someString");
        rightBeanCollectionTableModel.addColumnLocator("b.c.someString");

        SplicedTableModel splicedTableModel = new SplicedTableModel(leftBeanCollectionTableModel, "b.c.d.i", rightBeanCollectionTableModel, "d.i");

        TableTransformsTest.AnalyticsTableTest test =
                new TableTransformsTest.AnalyticsTableTest("Spliced table test", splicedTableModel, new SplicedTableModelTestRandomDataFeed(leftCollection, rightCollection));
        test.runTest();
        System.out.println("All done.");
    }

    private class SplicedTableModelTestRandomDataFeed extends RandomDataFeed {

        private static final int ADD_REMOVE_DATA_LOOPS = 500;
        private static final int UPDATE_DATA_LOOPS = 2000;

        protected BeanCollection leftCollection;
        protected BeanCollection rightCollection;

        protected int leftIdCounter = 1;
        protected int rightIdCounter = 1;

        public SplicedTableModelTestRandomDataFeed(BeanCollection leftCollection, BeanCollection rightCollection) {
            super(null);
            this.leftCollection = leftCollection;
            this.rightCollection = rightCollection;
        }

        public void run() {
            addRemoveRows(true);
//            sleep(500);
            updateData();
        }

        protected void addRemoveRows(boolean removeRows) {
            int added = 0;
            int removed = 0;
            for (int i = 0; i < ADD_REMOVE_DATA_LOOPS; i++) {
                BeanCollection collection = (random.nextBoolean()) ? leftCollection : rightCollection;

                double rnd = random.nextDouble();
                if (removeRows && rnd < 0.20) { //20% delete
                    if (collection.size() > 0) {
                        int row = random.nextInt(collection.size());
                        collection.remove(row);
                        removed++;
                    }
                } else {//80% insert
                    Object newBean = getRandom(collection.getType());
                    // set the Id
                    if (collection == leftCollection) {
                        if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                            ((BeanA) newBean).setB(sharedBean);
                        } else {
                            ((BeanA) newBean).getB().getC().getD().setI(leftIdCounter++);
                        }
                    } else {
                        ((BeanC) newBean).getD().setI(rightIdCounter++);
                        if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                            ((BeanC) newBean).setB(sharedBean);
                        }
                    }
                    collection.add(newBean);
                    added++;
                }
            }
            System.out.println("Added " + added + " Removed " + removed + " rows.");
        }

        protected void updateData() {
            System.out.println("Updating data.");
            for (int i = 0; i < UPDATE_DATA_LOOPS; i++) {
//                sleep(50);

                //decide what to update:
                Object b;
                BeanCollection beanCollection;
                int choice = random.nextInt(10);
                switch (choice) {
                    case 0: // update shared bean
                        sharedBean.setDate(new Date());
                        sharedBean.setC((BeanC) getRandom(BeanC.class));
                        break;

                    case 1: // update field in left collection to shared bean
                        beanCollection = leftCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("b"), sharedBean, true);
                        break;
                    case 2: // update left collection field
                        beanCollection = leftCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("b.date"), new Date(), true);
                        break;
                    case 3: // update key in left collection to non unique
                        beanCollection = leftCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("b.c.d.i"), new Integer(5), true);
                        break;
                    case 4: // update key in left collection to unique
                        beanCollection = leftCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("b.c.d.i"), new Integer(leftIdCounter++), true);
                        break;

                    case 5: // update right collection field to shared bean
                        beanCollection = rightCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("b"), sharedBean, true);
                        break;
                    case 6: // update right collection field
                        beanCollection = rightCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("someString"), getRandom(String.class), true);
                        break;
                    case 7: // update key in right collection
                        beanCollection = rightCollection;
                        b = beanCollection.get(random.nextInt(beanCollection.size()));
                        Generic.set(b, Generic.beanPathStringToArray("d.i"), new Integer(rightIdCounter++), true);
                        break;

                    case 8: // add or remove bean from left collection
                        if (random.nextDouble() < 0.3) { // 30% deletes
                            beanCollection = leftCollection;
                            if (beanCollection.size() > 0) {
                                int row = random.nextInt(beanCollection.size());
                                beanCollection.remove(row);
                            }
                        } else { // 70% inserts
                            // add bean to left collection
                            BeanA newBeanA = (BeanA) getRandom(BeanA.class);
                            newBeanA.getB().getC().getD().setI(leftIdCounter++);
                            if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                                newBeanA.setB(sharedBean);
                            }
                            leftCollection.add(newBeanA);
                        }
                        break;

                    case 9: // add or remove bean from right collection
                        if (random.nextDouble() < 0.3) { // 30% deletes
                            beanCollection = rightCollection;
                            if (beanCollection.size() > 0) {
                                int row = random.nextInt(beanCollection.size());
                                beanCollection.remove(row);
                            }
                        } else { // 70% inserts
                            BeanC newBean = (BeanC) getRandom(BeanC.class);
                            newBean.getD().setI(rightIdCounter++);
                            if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                                newBean.setB(sharedBean);
                            }
                            rightCollection.add(newBean);
                        }
                        break;
                }
            }
        }

        void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) {
        SplicedTableModelTest test = new SplicedTableModelTest();
        test.testReordering();
        test.testTableAsynchronous();
    }
}
