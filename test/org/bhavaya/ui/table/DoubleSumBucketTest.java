package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.ui.table.DoubleSumBucket;
import org.bhavaya.ui.table.PartialBucketValue;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Jan 29, 2004
 * Time: 12:58:39 PM
 * To change this template use Options | File Templates.
 */
public class DoubleSumBucketTest extends TestCase {
    public void testBucket() {
        DoubleSumBucket bucket = new DoubleSumBucket();
        bucket.insert(null);
        bucket.insert(null);
        bucket.insert(null);
        bucket.update(null, new Double(3));
        bucket.update(null, new Double(9));
        assertEquals(new Double(12), bucket.getOutput());
        bucket.delete(new Double(3));
        assertEquals(new Double(9), bucket.getOutput());
        bucket.delete(null);
        assertEquals(new Double(9), bucket.getOutput());
    }

    public void testPartialBucket() {
        DoubleSumBucket bucket = new DoubleSumBucket();
        bucket.insert(new Double(3));
        bucket.insert(null);
        bucket.insert(null);
        bucket.update(null, new Double(3));
        assertEquals(new Double(6), bucket.getOutput());
        bucket.update(new Double(3), new Double(9));
        assertEquals(new Double(12), bucket.getOutput());
        bucket.delete(new Double(3));
        assertEquals(new Double(9), bucket.getOutput());
        bucket.delete(null);
        assertEquals(new Double(9), bucket.getOutput());

        bucket.insert(new PartialBucketValue(new Double(3), new Double(Double.POSITIVE_INFINITY), 2));
        assertEquals(new PartialBucketValue(new Double(12), new Double(Double.POSITIVE_INFINITY), 2), bucket.getOutput());

        bucket.update(new Double(3), new PartialBucketValue(new Double(3), new Double(Double.NaN), 2));
        assertEquals(new PartialBucketValue(new Double(12), new Double(Double.NaN), 2), bucket.getOutput());

        bucket.delete(new PartialBucketValue(new Double(3), new Double(Double.NaN), 1));
        assertEquals(new PartialBucketValue(new Double(9), new Double(Double.NaN), 1), bucket.getOutput());

        bucket.update(new PartialBucketValue(new Double(3), new Double(Double.NaN), 2), new Double(5));
        assertEquals(new Double(11), bucket.getOutput());
    }
}
