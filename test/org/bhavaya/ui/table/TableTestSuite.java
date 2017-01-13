package org.bhavaya.ui.table;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class TableTestSuite extends TestSuite {

    public TableTestSuite() {
        addTestSuite(CachedObjectGraphTest.class);
        addTestSuite(BeanCollectionTableModelTest.class);
        addTestSuite(CachedObjectGraphPerformance.class);
        addTestSuite(DoubleSumBucketTest.class);
        addTestSuite(QuantitySumBucketTest.class);
        addTestSuite(BeanCollectionTableModelTest.class);
        addTestSuite(SplicedTableModelTest.class);
        addTestSuite(GenericTableTest.class);
        addTestSuite(TableTransformsTest.class);
    }

    public static Test suite() {
        return new TableTestSuite();
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(TableTestSuite.class);
//        junit.textui.TestRunner.run(new UtilTestSuite());
    }
}
