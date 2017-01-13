package org.bhavaya.ui.table;

import org.bhavaya.util.Log;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class QuantityRendererFactory extends DecimalTableCellRendererFactory {
    private static final Log log = Log.getCategory(QuantityRendererFactory.class);
    protected static final String CURRENCY_SPECIFIC = "Currency Specific";

    public QuantityRendererFactory(String[] params) {
        super(params);
    }

    protected DefaultTableCellRenderer getRendererForPrecision(String precision) {
        if (CURRENCY_SPECIFIC.equals(precision)) {
            return new CurrencySpecificRenderer();
        } else {
            try {
                int digits = Integer.parseInt(precision);
                return new QuantityRenderer(digits);
            } catch (NumberFormatException e) {
                log.error("could not parse renderer precision string", e);
            }
        }
        return new QuantityRenderer(0);
    }

    protected String[] getAvailablePrecisionOptions() {
        String[] oldOptions = super.getAvailablePrecisionOptions();
        String[] newOptions = new String[oldOptions.length+1];
        System.arraycopy(oldOptions, 0, newOptions, 0, oldOptions.length);
        newOptions[oldOptions.length] = CURRENCY_SPECIFIC;
        return newOptions;
    }


}
