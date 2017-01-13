package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.util.PropertyGroup;

import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 26-Aug-2009
 * Time: 15:05:09
 */
public class TestExclusionColumnCalculator extends TestCase {

    private ExclusionColumnCalculator calculator;

    public void setUp() {
        PropertyGroup p = new PropertyGroup(null, "test");
        PropertyGroup patternGroup = new PropertyGroup(p, ExclusionColumnCalculator.PATTERN_GROUP_PROPERTY_PATH);
        patternGroup.addProperty(ExclusionColumnCalculator.EXCLUDE_PATTERN_PROPERTY_PATH, "instrument.price.*");
        patternGroup.addProperty(ExclusionColumnCalculator.INCLUDE_PATTERN_PROPERTY_PATH, "instrument.price.wibble");

        PropertyGroup patternGroup2 = new PropertyGroup(p, ExclusionColumnCalculator.PATTERN_GROUP_PROPERTY_PATH);
        patternGroup2.addProperty(ExclusionColumnCalculator.EXCLUDE_PATTERN_PROPERTY_PATH, "instrument.marketPrice.*");

        p.addPropertyGroup(ExclusionColumnCalculator.PATTERN_GROUP_PROPERTY_PATH, patternGroup);
        p.addPropertyGroup(ExclusionColumnCalculator.PATTERN_GROUP_PROPERTY_PATH, patternGroup2);
        p.addProperty(ExclusionColumnCalculator.LEGACY_EXCLUSION_PROPERTY_PATH, "legacy.exclude");

        calculator = ExclusionColumnCalculator.getInstance(p);
    }

    public void testExclusions() {
        assertTrue(calculator.isExcluded("instrument.price.bid"));
        assertFalse(calculator.isExcluded("instrument.price.wibble"));
        assertTrue(calculator.isExcluded("instrument.marketPrice.bid"));
        assertTrue(calculator.isExcluded("legacy.exclude"));
        assertFalse(calculator.isExcluded("legacy.wibble"));
    }

    //we also determine column names to filter for filter table model using the exclusion rules
    public void testFilterTableModelColumnExclusionCalculation() {

        class TestKeyedColumnTableModel extends DefaultTableModel implements KeyedColumnTableModel {
            public int getColumnIndex(Object columnKey) {
                return 0;
            }

            public Object getColumnKey(int index) {
                switch (index) {
                    case 0 : return "instrument.price.bid";
                    case 1 : return "instrument.price.wibble";
                    case 2 : return "instrument.marketPrice.bid";
                    case 3 : return "legacy.exclude";
                    case 4 : return "legacy.wibble";
                }
                return null;
            }

            //assume the column name is the last token
            public String getColumnName(int index) {
                String s = getColumnKey(index).toString();
                return s.substring(s.lastIndexOf(".") + 1);
            }

            public int getColumnCount() {
                return 5;
            }
        }

        List<String> columnNamesToExclude = calculator.getColumnNamesToExclude(new TestKeyedColumnTableModel());
        assertEquals(Arrays.asList("bid", "exclude"), columnNamesToExclude);
    }

}
