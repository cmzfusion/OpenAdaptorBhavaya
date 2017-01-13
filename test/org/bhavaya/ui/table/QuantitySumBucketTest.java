package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.util.Quantity;
import org.bhavaya.ui.table.QuantitySumBucket;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class QuantitySumBucketTest extends TestCase {
    private static Quantity a = new Quantity(1000, "GBP");
    private static Quantity b = new Quantity(-1000, "GBP");
    private static Quantity c = new Quantity(3333, "GBP");
    private static Quantity nan = new Quantity(Double.NaN, "GBP");
    private static final double DELTA = 0.00001d;

    public QuantitySumBucketTest(String s) {
        super(s);
    }

    public void testInsert() {
        QuantitySumBucket sumBucket = new QuantitySumBucket();
        sumBucket.insert(a);

        Quantity output = (Quantity) sumBucket.getOutput();
        assertEquals("Simple insert broken", a.getAmount(), output.getAmount(), DELTA);

        sumBucket.insert(b);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Basic addition broken", 0d, output.getAmount(), DELTA);

        sumBucket.insert(nan);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Addition of NaN broken", 0d, output.getAmount(), DELTA);
        assertEquals("Addition after NaN should be tainted", true, output.isTainted());

        sumBucket.insert(c);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Addition after tainting broken", c.getAmount(), output.getAmount(), DELTA);
        assertEquals("Addition after NaN should be tainted", true, output.isTainted());
    }

    public void testDelete() {
        QuantitySumBucket sumBucket = new QuantitySumBucket();
        sumBucket.insert(a);
        sumBucket.insert(b);
        sumBucket.insert(c);

        Quantity output = (Quantity) sumBucket.getOutput();
        assertEquals("Test assumptions fail", c.getAmount(), output.getAmount(), DELTA);

        sumBucket.delete(c);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Basic delete broken", 0d, output.getAmount(), DELTA);

        sumBucket.insert(nan);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Test assumption failed", 0d, output.getAmount(),  DELTA);
        assertEquals("Test assumption failed", true, output.isTainted());

        sumBucket.delete(b);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Delete after NaN insert failed", a.getAmount(), output.getAmount(), DELTA);
        assertEquals("Value should be tainted", true, output.isTainted());

        boolean recalc = sumBucket.delete(nan);
        assertEquals("Delete after NaN removal should require full recalc", false, recalc);
    }

    public void testUpdate() {
        QuantitySumBucket sumBucket = new QuantitySumBucket();
        sumBucket.insert(a);
        sumBucket.insert(b);

        sumBucket.update(b, c);
        Quantity output = (Quantity) sumBucket.getOutput();
        assertEquals("Basic update broken", 4333d, output.getAmount(), DELTA);

        sumBucket.update(c, nan);
        output = (Quantity) sumBucket.getOutput();
        assertEquals("Update broken after value->NaN", 1000d, output.getAmount(), DELTA);
        assertEquals("Update should be tainted value->NaN", true, output.isTainted());

        boolean needRecalc = sumBucket.update(nan, c);
        assertEquals("Update after NaN removal should require full recalc", false, needRecalc);
    }
}
