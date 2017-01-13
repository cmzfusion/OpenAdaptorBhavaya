package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.util.Utilities;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class RoundingTest extends TestCase {

    public RoundingTest(String string) {
        super(string);
    }

    /**
     * Test numbers - valueToRound, tickSize, expectedResult
     */
    private static double[][] testNumbers = {
        {1.431568, 0.01, 1.43},
        {1.435568, 0.01, 1.44},
        {1.437568, 0.01, 1.44},

        {1.431, 0.05, 1.45},
        {1.425, 0.05, 1.40},
        {1.423, 0.05, 1.40},
        {1.451, 0.05, 1.45},
        {1.586, 0.05, 1.6},

        {879541.431568, 0.01, 879541.43},
        {879541.435568, 0.01, 879541.44},
        {879541.437568, 0.01, 879541.44},

        {879541.431, 0.05, 879541.45},
        {879541.425, 0.05, 879541.40},
        {879541.423, 0.05, 879541.40},
        {879541.451, 0.05, 879541.45},
        {879541.586, 0.05, 879541.6},
    };

    public void testRoundToTickSize() {
        for (int i = 0; i < testNumbers.length; i++) {
            double valueToRound = testNumbers[i][0];
            double tickSize = testNumbers[i][1];
            double expectedValue = testNumbers[i][2];
            double result = Utilities.roundToNearestStep(valueToRound, tickSize);
            String message = "Rounding error: ValueToRound: " + valueToRound + ", TickSize: " + tickSize + ", Expected: " + expectedValue + ", Result: " + result;
            assertEquals(message, expectedValue, result, 0.0);
        }
    }

    public void testRoundToTickSize2() {
        double start = 1.4;
        double end = 1.5;
        double step = 0.0001;
        double tick = 0.05;

        for (double v = start; v < end; v += step) {
            double expected;
            if (v < 1.425) {
                expected = 1.4;
            } else if (v < 1.475) {
                expected = 1.45;
            } else {
                expected = 1.5;
            }
            double result = Utilities.roundToNearestStep(v, tick);
            String message = "Rounding error: ValueToRound: " + v + ", TickSize: " + tick + ", Expected: " + expected + ", Result: " + result;
            assertEquals(message, expected, result, 0.0);
        }
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        new RoundingTest("Rounding").testRoundToTickSize();
        new RoundingTest("Rounding").testRoundToTickSize2();
        System.out.println("Test took: " + (System.currentTimeMillis() - start));
    }
}
