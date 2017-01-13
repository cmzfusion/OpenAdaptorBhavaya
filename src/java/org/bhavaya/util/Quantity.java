/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO: subclass Money from Quantity, removing dependencies on currency.
 *
 * @author Philip Milne
 * @version $Revision: 1.14.8.2 $
 */
public class Quantity implements Comparable, Numeric {
    public static Quantity UNKNOWN = new Quantity(Double.NaN, "N/A", null);

    private static final Set<String> LEGACY_EUR_CURRENCIES = new HashSet<>();

    static {
        LEGACY_EUR_CURRENCIES.addAll(Arrays.asList("ATS", "BEF", "DEM", "ESP", "FIM", "FRF", "GRD", "IEP", "ITL", "LUF", "NLG", "PTE"));
    }

    private final double amount;
    private final Date rateDate;
    private volatile boolean tainted = false;
    private final Unit unit;

    public Quantity(double amount, String unitName) {
        this(amount, Unit.getInstance(unitName));
    }

    public Quantity(double amount, String unitName, Date rateDate) {
        this(amount, Unit.getInstance(unitName), rateDate);
    }

    public Quantity(double amount, Unit unit) {
        this(amount, unit, null);
    }

    public Quantity(double amount, Unit unit, Date rateDate) {
        this.amount = amount;
        this.unit = unit;
        this.rateDate = rateDate;
        this.tainted = excludeFromCalculations(amount);
    }

    public double convert(String toUnit) {
        return amount * unit.conversionRate(toUnit, rateDate);
    }

    public double convert(Unit toUnit) {
        return convert(toUnit.getName());
    }

    public double getAmount() {
        return amount;
    }

    public Unit getUnit() {
        return unit;
    }

    public Date getRateDate() {
        return rateDate;
    }

    public double getAmountInGBP() {
        return convert("GBP");
    }

    public double getAmountInUSD() {
        return convert("USD");
    }

    public double getAmountInJPY() {
        return convert("JPY");
    }

    public double getAmountInEUR() {
        return convert("EUR");
    }

    public double getAmountWithLegacyEURConversion() {
        if (isLegacyEURCurrency()) {
            return convert("EUR");
        } else {
            return amount;
        }
    }

    /**
     * A tainted quantity is any quantity that has been involved in an operation with a filthy NaN!
     */
    public boolean isTainted() {
        return tainted;
    }

    private boolean isLegacyEURCurrency() {
        return LEGACY_EUR_CURRENCIES.contains(unit.getName());
    }

    public Quantity sum(Quantity quantity) {
        if (quantity == null) return null;
        if (!Utilities.equals(unit, quantity.unit)) {
            if (unit.getDefaultConversionUnit() == null) return UNKNOWN;
            double thisAmount = excludeFromCalculations(this.amount) ? 0d : convert(unit.getDefaultConversionUnit());
            double thatAmount = excludeFromCalculations(quantity.amount) ? 0d : quantity.convert(unit.getDefaultConversionUnit());
            Quantity returnValue = new Quantity(thisAmount + thatAmount, unit.getDefaultConversionUnit(), rateDate);
            returnValue.tainted = this.isTainted() || quantity.isTainted();
            return returnValue;
        } else {
            double thisAmount = excludeFromCalculations(this.amount) ? 0d : amount;
            double thatAmount = excludeFromCalculations(quantity.amount) ? 0d : quantity.amount;
            Quantity returnValue = new Quantity(thisAmount + thatAmount, unit, rateDate);
            returnValue.tainted = this.isTainted() || quantity.isTainted();
            return returnValue;
        }
    }

    public Quantity difference(Quantity quantity) {
        if (quantity == null) return null;
        if (!Utilities.equals(unit, quantity.unit)) {
            if (unit.getDefaultConversionUnit() == null) return UNKNOWN;
            double thisAmount = excludeFromCalculations(this.amount) ? 0d : convert(unit.getDefaultConversionUnit());
            double thatAmount = excludeFromCalculations(quantity.amount) ? 0d : quantity.convert(unit.getDefaultConversionUnit());
            Quantity returnValue = new Quantity(thisAmount - thatAmount, unit.getDefaultConversionUnit(), rateDate);
            if (this.isTainted() || quantity.isTainted()) returnValue.tainted = true;
            return returnValue;
        } else {
            double thisAmount = excludeFromCalculations(this.amount) ? 0d : amount;
            double thatAmount = excludeFromCalculations(quantity.amount) ? 0d : quantity.amount;
            Quantity returnValue = new Quantity(thisAmount - thatAmount, unit, rateDate);
            if (this.isTainted() || quantity.isTainted()) returnValue.tainted = true;
            return returnValue;
        }
    }

    public Quantity product(double otherAmount) {
        Quantity quantity = new Quantity(amount * otherAmount, unit, rateDate);
        quantity.tainted = isTainted();
        return quantity;
    }

    public Quantity negative() {
        Quantity quantity = new Quantity(-amount, unit, rateDate);
        quantity.tainted = isTainted();
        return quantity;
    }

    /**
     * Returns true if the value should be ignored in calculations - NaN and infinites.
     */
    private boolean excludeFromCalculations(double amount) {
        return Double.isNaN(amount) || Double.isInfinite(amount);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity)) return false;

        final Quantity quantity = (Quantity) o;

        if (amount != quantity.amount) return false;
        // WARNING: this is not the default equals, do not change, a null rateDate is equals to any other rateDate value
        if (rateDate != null && quantity.rateDate != null && !rateDate.equals(quantity.rateDate)) return false;
        if (unit != null ? !unit.equals(quantity.unit) : quantity.unit != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = amount != +0.0d ? Double.doubleToLongBits(amount) : 0l;
        result = (int) (temp ^ (temp >>> 32));
        // WARNING: this is not the default hashCode, do not change, it doesnt include rateDate as a null rateDate is equals to any other rateDate value
        result = 29 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }

    public int compareTo(Object other) {
        Quantity otherQuantity = (Quantity) other;
        return Double.compare(amount, otherQuantity.amount);
    }

    public String toString() {
        return amount + ((isTainted() && !excludeFromCalculations(amount)) ? "?" : "") + " (" + unit + "/" + rateDate + ")";
    }

    public double doubleValue() {
        return getAmount();
    }
}
