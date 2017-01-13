package org.bhavaya.ui.table.formula;

import org.bhavaya.util.Numeric;
import org.bhavaya.util.Utilities;

/**
 * Holder for formula evaluation result
 * Just supports String, double and boolean values
 * User: Jon Moore
 * Date: 27/01/11
 * Time: 13:46
 */
public final class FormulaResult implements Numeric, Comparable<FormulaResult> {
    private String stringValue;
    private Double doubleValue;
    private Boolean booleanValue;

    public static final FormulaResult EMPTY = new FormulaResult();

    private FormulaResult() {
    }

    public FormulaResult(Object o) {
        if (o instanceof String) {
            this.stringValue = (String) o;
        } else if (o instanceof Double) {
            this.doubleValue = (Double) o;
        } else if (o instanceof Boolean) {
            this.booleanValue = (Boolean) o;
        } else if (o instanceof FormulaResult) {
            stringValue = ((FormulaResult) o).stringValue;
            doubleValue = ((FormulaResult) o).doubleValue;
            booleanValue = ((FormulaResult) o).booleanValue;
        } else {
            throw new RuntimeException("Illegal variant value : " + o);
        }
    }

    public FormulaResult(String value) {
        this.stringValue = value;
    }

    public FormulaResult(Double value) {
        this.doubleValue = value;
    }

    public FormulaResult(Boolean value) {
        this.booleanValue = value;
    }

    public boolean isDouble() {
        return doubleValue != null;
    }

    public boolean isString() {
        return stringValue != null;
    }

    public boolean isBoolean() {
        return booleanValue != null;
    }

    public boolean booleanValue() {
        if (isBoolean()) {
            return booleanValue.booleanValue();
        }
        return false;
    }

    public double doubleValue() {
        if (isDouble()) {
            return doubleValue.doubleValue();
        }
        return 0;
    }

    public String stringValue() {
        if (isString()) {
            return stringValue;
        }
        if (isDouble()) {
            return "" + doubleValue;
        }
        if (isBoolean()) {
            return "" + booleanValue;
        }
        return null;
    }

    public String toString() {
        return stringValue();
    }

    public Object getObjectResult() {
        if (isString()) {
            return stringValue();
        }
        if (isBoolean()) {
            return booleanValue;
        }
        return doubleValue;
    }

    public boolean equals(Object o) {
        if (o instanceof FormulaResult) {
            FormulaResult v2 = (FormulaResult) o;
            return Utilities.equals(v2.getObjectResult(), getObjectResult());
        }
        return false;
    }

    @Override
    public int compareTo(FormulaResult o) {
        if(o == null) {
            return 1;
        }
        if(isDouble()) {
            return Utilities.compare(doubleValue, o.doubleValue);
        }
        if(isBoolean()) {
            return Utilities.compare(booleanValue, o.booleanValue);
        }
        return Utilities.compare(stringValue, o.stringValue);
    }
}
