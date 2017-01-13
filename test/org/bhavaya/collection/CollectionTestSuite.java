package org.bhavaya.collection;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class CollectionTestSuite extends TestSuite {

    public CollectionTestSuite() {
        addTestSuite(SynchronizedTransformerBeanCollectionTest.class);
        addTestSuite(IndexedSetTest.class);
    }

    public static Test suite() {
        return new CollectionTestSuite();
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(CollectionTestSuite.class);
//        junit.textui.TestRunner.run(new UtilTestSuite());
    }
}
