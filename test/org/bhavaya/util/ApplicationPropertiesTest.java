package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.PropertyGroup;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Jun 19, 2006
 * Time: 12:04:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationPropertiesTest extends TestCase {
    private static final String MAIN = "test/org/bhavaya/util/application_properties_test_mainfile.xml";
    private static final String SUB = "test/org/bhavaya/util/application_properties_test_subfile.xml";
    private static final String SUB_SUB = "test/org/bhavaya/util/application_properties_test_sub_subfile.xml";

    public void testSubstitution() {
        PropertyGroup root = ApplicationProperties.getInstance(new String[]{MAIN, SUB});

        assertEquals("Inline substiution didn't work in same file", "testtoken", root.getProperty("simpleInlineReferencingProperty"));
        assertEquals("Inline substiution didn't work in external file", "testtoken", root.getProperty("simpleInlineReferencingProperty"));
    }

    public void testAll() {
        PropertyGroup root = ApplicationProperties.getInstance(new String[]{MAIN, SUB, SUB_SUB});

        assertEquals("Problem getting simple property", "simpleValue", root.getProperty("simpleGroup.simpleProperty"));
        assertEquals("Problem getting encrypted property", "encryptedValue", root.getProperty("simpleGroup.encryptedProperty"));
        assertEquals("Problem getting internally referenced property", "referencedValue", root.getProperty("simpleGroup.referencingProperty"));

        String[] expected = new String[]{"propertyOne", "propertyTwo"};
        String[] result = root.getProperties("simpleGroup.referencingProperties");
        assertTrue("Problem getting simple properties from externally referenced properties ", Arrays.equals(expected, result));

        assertEquals("Problem getting external referenced property", "simpleValue", root.getProperty("simpleGroup.externalReferencingProperty"));
        assertEquals("Problem getting encrypted external referenced property", "encryptedValue", root.getProperty("simpleGroup.encryptedExternalReferencingProperty"));

        assertEquals("Property should be overridden", "overridden", root.getProperty("simpleGroup.overriddenProperty"));
        assertEquals("Property should not be overridden", "notOverridden", root.getProperty("simpleGroup.notOverriddenProperty"));

        PropertyGroup[] groups = root.getGroups("externalReferencingGroup");
        assertEquals("Should have returned two groups", 2, groups.length);
        assertEquals("Wrong value for group[0]", "one", groups[0].getProperty("test"));
        assertEquals("Wrong value for group[1]", "two", groups[1].getProperty("test"));

        assertEquals("Group should have been overridden", "overridden", root.getProperty("overrideTest.overriddenGroup.test"));
        assertEquals("Group should not have been overridden", "notOverridden", root.getProperty("overrideTest.notOverriddenGroup.test"));
        assertEquals("Group should have been overridden twice", "sub_sub", root.getProperty("doubleOverrideTest.test"));
    }
}
