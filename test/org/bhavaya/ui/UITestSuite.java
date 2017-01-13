package org.bhavaya.ui;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class UITestSuite extends TestSuite {

    public UITestSuite() {
        addTestSuite(NarrowableComboBoxTest.class);
        addTestSuite(DateSeriesTest.class);
    }

    public static Test suite() {
        return new UITestSuite();
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(UITestSuite.class);
//        junit.textui.TestRunner.run(new UtilTestSuite());
    }
}
