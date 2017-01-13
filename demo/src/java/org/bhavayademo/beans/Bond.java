package org.bhavayademo.beans;

import org.bhavaya.util.PropertyMetaData;

import java.util.Date;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public abstract class Bond extends org.bhavayademo.beans.Instrument {
    public abstract double getParAmount();
    public abstract void setParAmount(double parAmount);

    public abstract void setMaturityDate(Date date);

    public abstract Date getMaturityDate();

    public abstract void setCoupon(double coupon);

    public abstract void setZSpread(double zSpread);
    public abstract double getZSpread();

    @PropertyMetaData(hidden = true)
    public int getHiddenProperty() {
        return 0;
    }

    @PropertyMetaData(displayName = "Super Renamed Propery", description = "This property was renamed using annotations")
    public String getRenamedPropery() {
        return "RP";
    }
}
