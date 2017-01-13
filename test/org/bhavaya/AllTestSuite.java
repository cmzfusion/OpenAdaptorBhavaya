package org.bhavaya;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;
import org.bhavaya.ui.UITestSuite;
import org.bhavaya.ui.table.TableTestSuite;
import org.bhavaya.db.DBTestSuite;
import org.bhavaya.util.UtilTestSuite;
import org.bhavaya.collection.CollectionTestSuite;

import java.util.Enumeration;

/**
 * Runs all the bhavaya test suites.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class AllTestSuite extends TestSuite {

    public AllTestSuite() {
        addAll(new DBTestSuite());
        addAll(new UtilTestSuite());
        addAll(new UITestSuite());
        addAll(new CollectionTestSuite());
        addAll(new TableTestSuite());
    }

    /**
     * Adds all tests from the test suite to this test suite.
     */
    private void addAll(TestSuite suite) {
        Enumeration tests = suite.tests();
        while (tests.hasMoreElements()) {
            Test test = (Test) tests.nextElement();
            addTest(test);
        }
    }

    public static Test suite() {
        return new AllTestSuite();
    }

    public static void main(String[] args) {
        TestRunner testRunner = new TestRunner();
        testRunner.setLoading(false); // without this the test run with ANT fails with class loading exception
        testRunner.start(new String[]{AllTestSuite.class.getName()});
    }
}
