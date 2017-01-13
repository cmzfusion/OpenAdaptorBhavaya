package org.bhavaya.util;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class UtilTestSuite extends TestSuite {

    public UtilTestSuite() {
        addTestSuite(QuantityTest.class);
        addTestSuite(CachedObjectGraphTest.class);
        addTestSuite(DefaultObserverableTest.class);
    }

    public static Test suite() {
        return new UtilTestSuite();
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(UtilTestSuite.class);
//        junit.textui.TestRunner.run(new UtilTestSuite());
    }
}
