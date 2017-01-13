package org.bhavaya.ui.table;

import org.bhavaya.util.Quantity;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class QuantityConversionRenderer extends QuantityRenderer {
    private String convertToUnit;

    public QuantityConversionRenderer(int precision, String convertToUnit) {
        super(precision);
        this.convertToUnit = convertToUnit;
    }

    public QuantityConversionRenderer(String precisionString, String convertToUnitString) {
        this(Integer.parseInt(precisionString), convertToUnitString);
    }

    public void setValue(Object value) {
        if (value instanceof Quantity) {
            Quantity quantity = (Quantity) value;
            super.setValue(new Quantity(quantity.convert(convertToUnit), convertToUnit, quantity.getRateDate()));
        } else {
            super.setValue(value);
        }
    }
}
