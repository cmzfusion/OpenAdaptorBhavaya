package org.bhavaya.db;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

/**
 * These tests here require these jvm parameters:
 *
 * -DOVERRIDE_RESOURCE_DIR=../demo/resources
 * -DENVIRONMENT_PATH=application.xml,test_environment.xml
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class DBTestSuite extends TestSuite {

    public DBTestSuite() {
        addTestSuite(CatalogSchemaTableTest.class);
        addTestSuite(SQLTest.class);
    }

    public static Test suite() {
        return new DBTestSuite();
    }

    public static void main(String[] args) {
        TestRunner testRunner = new TestRunner();
        testRunner.setLoading(false); // without this the test run with ANT fails with class loading exception
        testRunner.start(new String[]{DBTestSuite.class.getName()});
    }
}
