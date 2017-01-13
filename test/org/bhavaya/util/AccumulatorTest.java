package org.bhavaya.util;

import org.bhavaya.util.Accumulator;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Apr 26, 2006
 * Time: 9:13:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class AccumulatorTest extends MockObjectTestCase {

    public void testItemsToTriggerBatch() throws InterruptedException {
        Mock mockHandler = mock(Accumulator.Handler.class);
        Accumulator accumulator = new Accumulator("Test", 5, Integer.MAX_VALUE, (Accumulator.Handler) mockHandler.proxy());
        accumulator.start();

        mockHandler.expects(never()).method("handleBatch");
        for (int i = 0; i < 4; i++) accumulator.add(new Object());
        Thread.sleep(25);
        mockHandler.verify();

        mockHandler.reset();
        mockHandler.expects(once()).method("handleBatch");
        accumulator.add(new Object());
        Thread.sleep(25);
        mockHandler.verify();
        mockHandler.reset();
    }

    public void testIdleTimeTriggersBatch() throws InterruptedException {
        Mock mockHandler = mock(Accumulator.Handler.class);
        Accumulator accumulator = new Accumulator("Test", Integer.MAX_VALUE, 500, (Accumulator.Handler) mockHandler.proxy());
        accumulator.start();

        mockHandler.expects(never()).method("handleBatch");
        accumulator.add(new Object());
        Thread.sleep(400);
        mockHandler.verify();
        mockHandler.reset();

        mockHandler.expects(once()).method("handleBatch");
        Thread.sleep(610);
        mockHandler.verify();
        mockHandler.reset();

        mockHandler.expects(never()).method("handleBatch");
        for (int i = 0; i < 5; i++) {
            accumulator.add(new Object());
            Thread.sleep(500);
        }
        mockHandler.verify();
        mockHandler.reset();
    }

    public void testTimings() throws InterruptedException {
        Object lock = new Object();

        for (int i = 0; i < 100; i++) {
            synchronized(lock) {
                long start = System.currentTimeMillis();
                lock.wait(10L);
                long diff = System.currentTimeMillis() - start;
                assertTrue("Expected diff >= start.  Diff was " + diff, diff >= 10);
            }
        }
    }
}
