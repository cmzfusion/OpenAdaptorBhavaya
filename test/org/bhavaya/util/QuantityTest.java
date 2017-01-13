package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.util.Quantity;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class QuantityTest extends TestCase {
    private static Quantity a = new Quantity(1000, "GBP");
    private static Quantity b = new Quantity(-1000, "GBP");
    private static Quantity nan = new Quantity(Double.NaN, "GBP");
    private static final double DELTA = 0.00001d;

    public QuantityTest(String name) {
        super(name);
    }

    public void testSum() {
        Quantity aPlusB = a.sum(b);
        assertEquals("Basic addition broken", 0, aPlusB.getAmount(), DELTA);

        Quantity aPlusNaN = a.sum(nan);
        assertEquals("Addition of NaN broken (right operand)", 1000d, aPlusNaN.getAmount(), DELTA);
        assertEquals("Addition after NaN should be tainted", true, aPlusNaN.isTainted());

        Quantity nanPlusA = nan.sum(a);
        assertEquals("Addition of NaN broken (left operand)", a.getAmount(), nanPlusA.getAmount(), DELTA);
        assertEquals("Addition after NaN should be tainted", true, nanPlusA.isTainted());
    }

    public void testDifference() {
        Quantity aMinusB = a.difference(b);
        assertEquals("Basic difference broken", 2000d, aMinusB.getAmount(), DELTA);

        Quantity aMinusNaN = a.difference(nan);
        assertEquals("Difference with NaN broken (right operand)", a.getAmount(), aMinusNaN.getAmount(), DELTA);
        assertEquals("Difference after NaN should be tainted", true, aMinusNaN.isTainted());

        Quantity nanMinusA = nan.difference(a);
        assertEquals("Difference with NaN broken (left operand)", -1 * a.getAmount(), nanMinusA.getAmount(), DELTA);
        assertEquals("Difference after NaN should be tainted", true, nanMinusA.isTainted());
    }
}
