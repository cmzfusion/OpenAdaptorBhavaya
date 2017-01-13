package test.bhavaya.beans;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import junit.framework.TestCase;
import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Schema;
import org.bhavaya.beans.DefaultBeanFactory;
import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.*;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.coms.NotificationSubscriber;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.Persister;
import org.bhavaya.db.SQL;
import org.bhavaya.db.SqlBeanFactory;
import org.bhavaya.util.DateUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Quantity;
import org.bhavayademo.beans.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Description
 *
 * @author DrKW
 * @version $Revision: 1.12 $
 */
public class BeanFactoryTest extends TestCase {
    private static final Log log = Log.getCategory(BeanFactoryTest.class);
    private NotificationPublisher publisher;
    private NotificationSubscriber subscriber;

    public BeanFactoryTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        DBUtilities.executeUpdateScript("demoDatabase", "destroyDemo.sql", false);
        DBUtilities.executeUpdateScript("demoDatabase", "createDemo.sql", true);
        DBUtilities.executeUpdateScript("demoDatabase", "createDemoStaticData.sql", true);
        DBUtilities.executeUpdateScript("demoDatabase", "createDemoTestData.sql", true);

        publisher = NotificationPublisher.getInstance("sql");
        if (publisher != null) {
            publisher.connect();
        }

        subscriber = NotificationSubscriber.getInstance("sql");
        if (subscriber != null) {
            subscriber.connect();
            subscriber.startProcessing();
        }
    }

    protected void tearDown() throws Exception {
        if (subscriber != null) subscriber.close();
        if (publisher != null) publisher.close();
    }

    public void testGet() {
        System.out.println("BeanFactoryTest.testGet");
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(2));
        assertTrue("Correct instrument", instrument.getDescription().equals("3M EIB 2.75 01/01/2005"));
        assertTrue("Correct instrument currency", instrument.getCurrency().equals(Currency.EUR));
        assertTrue("Correct instrument subclass", instrument instanceof org.bhavayademo.beans.BondFuture);
    }

    public void testCreateKey() {
        System.out.println("BeanFactoryTest.testCreateKey");
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Object existingInstrumentKey = new Integer(2);
        Instrument instrument = (Instrument) instrumentFactory.get(existingInstrumentKey);
        Object newInstrumentKey = instrumentFactory.getKeyForValue(instrument);
        assertTrue("Correct simple key", existingInstrumentKey.equals(newInstrumentKey));
    }

    public void testGetCompoundKey() {
        BeanFactory tradeFactory = BeanFactory.getInstance(Trade.class);
        List existingTradeKey = new ArrayList(2);
        existingTradeKey.add(new Integer(1));
        existingTradeKey.add(new Integer(0));
        Trade trade = (Trade) tradeFactory.get(existingTradeKey);
        Object newTradeKey = tradeFactory.getKeyForValue(trade);
        assertTrue("Correct trade", trade.getPrice() == 102.34);
        assertTrue("Correct compound key", existingTradeKey.equals(newTradeKey));
    }

    // TODO: dont just print out values, use an assert
    public void testCriterionGet() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);

        CriterionGroup criterionGroup = new CriterionGroup("Selected Instruments", new Criterion[]{new BasicCriterion("dataset.Currency", "=", "GBP")});
        BeanCollection gbpInstruments = instrumentFactory.getBeanCollection(criterionGroup);

        for (Iterator iterator = gbpInstruments.iterator(); iterator.hasNext();) {
            Instrument instrument = (Instrument) iterator.next();
            log.info("GBP instrument = " + instrument.getDescription());
        }
    }

    // TODO: dont just print out values, use an assert
    public void testSqlGet() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        SQL instrumentSql = Schema.getInstance(Instrument.class).getSql();
        SQL euroInstrumentsSql = instrumentSql.joinWhereClause("INSTRUMENT.CURRENCY = 'EUR'");
        Instrument[] euroInstruments = (Instrument[]) ((SqlBeanFactory) instrumentFactory).getObjects(euroInstrumentsSql);

        for (int i = 0; i < euroInstruments.length; i++) {
            Instrument instrument = euroInstruments[i];
            log.info("EUR instrument = " + instrument.getDescription());
        }
    }

    public void testInsert() throws Exception {
        System.out.println("BeanFactoryTest.testInsert");
        Persister persister = new Persister("demoDatabaseTx", publisher);

        BeanFactory tradeTxFactory = BeanFactory.getInstance(Trade.class, "demoDatabaseTx");
        BeanFactory tradeFactory = BeanFactory.getInstance(Trade.class, "demoDatabase");

        List tradeKey = new ArrayList(2);
        tradeKey.add(new Integer(2));
        tradeKey.add(new Integer(1));

        TestInsertMapListener beanFactoryMapListener = new TestInsertMapListener(tradeKey);
        tradeFactory.addMapListener(beanFactoryMapListener);

        BeanCollection allTrades = tradeFactory.getAllBeanCollection();
        TestInsertCollectionListener beanCollectionListener = new TestInsertCollectionListener(tradeKey);
        allTrades.addCollectionListener(beanCollectionListener);

        try {
            TradeType tradeType = (TradeType) BeanFactory.getInstance(TradeType.class).get(new Integer(0));
            Instrument instrument = (Instrument) BeanFactory.getInstance(Instrument.class).get(new Integer(2));
            VersionStatus versionStatus = (VersionStatus) BeanFactory.getInstance(VersionStatus.class).get(new Integer(0));

            Trade newTrade = (Trade) BeanFactory.newBeanInstance(Trade.class);
            newTrade.setVersion(1);
            newTrade.setVersionStatus(versionStatus);
            newTrade.setTradeType(tradeType);
            newTrade.setInstrument(instrument);
            newTrade.setQuantity(new Quantity(10001, instrument.getCurrency().getCode()));
            newTrade.setPrice(103.54);
            newTrade.setTradeDate(DateUtilities.newDateTime());
            newTrade.setComments("t+3 delivery");
            persister.insertObject(newTrade);
            persister.commit();

            waitForNotification();

            Trade loadedTrade = (Trade) tradeTxFactory.get(tradeKey);
            assertTrue("Trade from different datasource", loadedTrade != newTrade);
            assertTrue("Trade now exists", loadedTrade != null);
            assertTrue(BeanFactory.equalsBean(loadedTrade.getInstrument(), newTrade.getInstrument()));
            assertTrue(BeanFactory.equalsBean(loadedTrade.getVersionStatus(), newTrade.getVersionStatus()));
            assertTrue(BeanFactory.equalsBean(loadedTrade.getTradeType(), newTrade.getTradeType()));
            assertTrue(loadedTrade.getPrice() == newTrade.getPrice());
            assertTrue(loadedTrade.getQuantity() == newTrade.getQuantity());
//            assertTrue(loadedTrade.getTradeDate().equals(newTrade.getTradeDate())); // TODO fix for millis
            assertTrue(loadedTrade.getComments().equals(newTrade.getComments()));
            assertTrue(loadedTrade.getCashDelta() == newTrade.getCashDelta());

            Thread.sleep(500);
            assertTrue("Collection events arrived", beanCollectionListener.eventIndex == 3);
            assertTrue("Map events arrived", beanFactoryMapListener.eventIndex == 3);
        } finally {
            allTrades.removeCollectionListener(beanCollectionListener);
            tradeFactory.removeMapListener(beanFactoryMapListener);
            persister.rollback();
        }
    }

    public void testUpdate() throws Exception {
        System.out.println("BeanFactoryTest.testUpdate");
        Persister persister = new Persister("demoDatabaseTx", publisher);

        BeanFactory tradeTxFactory = BeanFactory.getInstance(Trade.class, "demoDatabaseTx");
        BeanFactory tradeFactory = BeanFactory.getInstance(Trade.class, "demoDatabase");

        List existingTradeKey = new ArrayList(2);
        existingTradeKey.add(new Integer(1));
        existingTradeKey.add(new Integer(0));

        TestUpdateMapListener beanFactoryMapListener = new TestUpdateMapListener(existingTradeKey);
        tradeFactory.addMapListener(beanFactoryMapListener);

        BeanCollection allTrades = tradeFactory.getAllBeanCollection();
        TestUpdateCollectionListener beanCollectionListener = new TestUpdateCollectionListener(existingTradeKey);
        allTrades.addCollectionListener(beanCollectionListener);

        try {
            Instrument instrument = (Instrument) BeanFactory.getInstance(Instrument.class).get(new Integer(0));
            VersionStatus versionStatus = (VersionStatus) BeanFactory.getInstance(VersionStatus.class).get(new Integer(0));
            Trade trade = (Trade) tradeTxFactory.get(existingTradeKey);
            assertTrue(BeanFactory.equalsBean(trade.getInstrument(), instrument));
            assertTrue(BeanFactory.equalsBean(trade.getVersionStatus(), versionStatus));

            Instrument instrument2 = (Instrument) BeanFactory.getInstance(Instrument.class).get(new Integer(3));
            VersionStatus versionStatus2 = (VersionStatus) BeanFactory.getInstance(VersionStatus.class).get(new Integer(1));
            assertTrue(!instrument2.equals(instrument));
            assertTrue(!versionStatus2.equals(versionStatus));

            trade.setInstrument(instrument2);
            trade.setVersionStatus(versionStatus2);
            persister.updateObject(trade, new String[]{"instrument", "versionStatus"});
            persister.commit();

            waitForNotification();

            Trade loadedTrade = (Trade) tradeFactory.get(existingTradeKey);
            assertTrue("Trade from different datasource", loadedTrade != trade);
            assertTrue(BeanFactory.equalsBean(loadedTrade.getInstrument(),instrument2));
            assertTrue(BeanFactory.equalsBean(loadedTrade.getVersionStatus(),versionStatus2));
            assertTrue(BeanFactory.equalsBean(loadedTrade.getTradeType(),trade.getTradeType()));
            assertTrue(loadedTrade.getPrice() == trade.getPrice());
            assertTrue(loadedTrade.getQuantity() == trade.getQuantity());
            assertTrue(loadedTrade.getTradeDate().equals(trade.getTradeDate()));
            assertTrue(loadedTrade.getComments().equals(trade.getComments()));
            assertTrue(loadedTrade.getCashDelta() == trade.getCashDelta());

            Thread.sleep(500);
            assertTrue("Collection events arrived", beanCollectionListener.eventIndex == 3);
            assertTrue("Map events arrived", beanFactoryMapListener.eventIndex == 3);
        } finally {
            allTrades.removeCollectionListener(beanCollectionListener);
            tradeFactory.removeMapListener(beanFactoryMapListener);
            persister.rollback();
        }
    }

    private void waitForNotification() throws InterruptedException {
        Thread.sleep(1000);
        log.info("Notification should have been processed");
    }

    public void testDelete() throws Exception {
        System.out.println("BeanFactoryTest.testDelete");
        Persister persister = new Persister("demoDatabaseTx", publisher);

        BeanFactory tradeTxFactory = BeanFactory.getInstance(Trade.class, "demoDatabaseTx");
        BeanFactory tradeFactory = BeanFactory.getInstance(Trade.class, "demoDatabase");

        List existingTradeKey = new ArrayList(2);
        existingTradeKey.add(new Integer(1));
        existingTradeKey.add(new Integer(0));

        TestDeleteMapListener beanFactoryMapListener = new TestDeleteMapListener(existingTradeKey);
        tradeFactory.addMapListener(beanFactoryMapListener);

        BeanCollection allTrades = tradeFactory.getAllBeanCollection();
        TestDeleteCollectionListener beanCollectionListener = new TestDeleteCollectionListener(existingTradeKey);
        allTrades.addCollectionListener(beanCollectionListener);

        try {
            Trade existingTrade = (Trade) tradeFactory.get(existingTradeKey);
            existingTrade = (Trade) tradeFactory.get(existingTradeKey);
            assertTrue("Trade exists", tradeFactory.containsKey(existingTradeKey));

            persister.deleteObject(existingTrade);
            persister.commit();

            waitForNotification();

            Trade deletedTrade = (Trade) tradeTxFactory.get(existingTradeKey);
            assertTrue("Deleted trade nolonger exists in database", deletedTrade == null);

            assertTrue("Deleted trade nolonger exists in BeanFactory", !tradeFactory.containsKey(existingTradeKey));

            Thread.sleep(500);
            assertTrue("Collection events arrived", beanCollectionListener.eventIndex == 3);
            assertTrue("Map events arrived", beanFactoryMapListener.eventIndex == 3);
        } finally {
            allTrades.removeCollectionListener(beanCollectionListener);
            tradeFactory.removeMapListener(beanFactoryMapListener);
            persister.rollback();
        }
    }

    public void testLocking() {
        final BeanFactory beanFactory = new DefaultBeanFactory(String.class, null);

        final CyclicBarrier testBarrier = new CyclicBarrier(3);
        Thread toArrayThread = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 500; i++) {
                    Object[] o;
                    synchronized(beanFactory.getLock()) {
                        o = beanFactory.values().toArray();
                    }
                    for (int j = 0; j < o.length; j++) {
                        Object o1 = o[j];
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }

                log.info("toArray Thread finished");
                try {
                    testBarrier.barrier();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        });

        Thread creatorThread = new Thread(new Runnable() {
            public void run() {
                for (int i =0; i < 500; i++) {
                    Bond b = (Bond) beanFactory.newBeanInstance();
                    b.setInstrumentType(InstrumentType.BOND);
                    b.setZSpread(i);
//                    beanFactory.putValue(b);
                    beanFactory.putValue("" + i);

                    beanFactory.get(new Integer((int) (Math.random() * 100)));

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }

                log.info("Creator thread finished");
                try {
                    testBarrier.barrier();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        });

        toArrayThread.start();
        creatorThread.start();

        try {
            testBarrier.barrier();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        log.info("Test complete.");
    }


//    public void testGeneratedValues() throws Exception {
//        Id id = (Id) BeanFactory.newBeanInstance(Id.class);
//        id.setName("Test");
//
//        Connection connection = DataSourceFactory.getInstance("demoDatabaseTx").getConnection();
//        NotificationPublisher publisher = NotificationPublisher.getInstance();
//        if (publisher != null) {
//            publisher.connect();
//        }
//        NotificationSubscriber subscriber = NotificationSubscriber.getInstance();
//        if (subscriber != null) {
//            subscriber.connect();
//            subscriber.pause(false);
//        }
//        Persister persister = new Persister("demoDatabaseTx", publisher);
//        try {
//            persister.insertObject(id);
//            persister.commit();
//        } finally {
//            persister.rollback();
//        }
//    }

    private class TestInsertMapListener implements MapListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestInsertMapListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void mapChanged(MapEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == MapEvent.INSERT && e.getKey().equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == MapEvent.COMMIT && e.getKey() == null && e.getValue() == null);
                    break;
                default:
                    fail();
            }
            eventIndex++;
        }
    }

    private class TestUpdateMapListener implements MapListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestUpdateMapListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void mapChanged(MapEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == MapEvent.UPDATE && e.getKey().equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == MapEvent.COMMIT && e.getKey() == null && e.getValue() == null);
                    break;
                default:
                    fail();
            }
            eventIndex++;
        }
    }

    private class TestDeleteMapListener implements MapListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestDeleteMapListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void mapChanged(MapEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == MapEvent.DELETE && e.getKey().equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == MapEvent.COMMIT && e.getKey() == null && e.getValue() == null);
                    break;
                default:
                    fail();
            }
            eventIndex++;
        }
    }


    private class TestInsertCollectionListener implements CollectionListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestInsertCollectionListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void collectionChanged(ListEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == ListEvent.INSERT && BeanFactory.getKeyForBean(e.getValue()).equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == ListEvent.COMMIT && e.getValue() == null);
                    break;
                default:
                    fail();
            }
            eventIndex++;
        }
    }

    private class TestUpdateCollectionListener implements CollectionListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestUpdateCollectionListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void collectionChanged(ListEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == ListEvent.UPDATE && BeanFactory.getKeyForBean(e.getValue()).equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == ListEvent.COMMIT && e.getValue() == null);
                    break;
                 default:
                    fail();
            }
            eventIndex++;
        }
    }

    private class TestDeleteCollectionListener implements CollectionListener {
        private int eventIndex = 1;
        private final Object existingTradeKey;

        public TestDeleteCollectionListener(Object existingTradeKey) {
            this.existingTradeKey = existingTradeKey;
        }

        public void collectionChanged(ListEvent e) {
            System.out.println("e = " + e);
            switch (eventIndex) {
                case 1:
                    assertTrue("Correct event 1", e.getType() == ListEvent.DELETE && BeanFactory.getKeyForBean(e.getValue()).equals(existingTradeKey));
                    break;
                case 2:
                    assertTrue("Correct event 2", e.getType() == ListEvent.COMMIT && e.getValue() == null);
                    break;
                default:
                    fail();
            }
            eventIndex++;
        }
    }

}
