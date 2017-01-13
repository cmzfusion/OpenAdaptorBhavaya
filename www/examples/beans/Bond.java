package beans;

import java.util.Date;

/**
 * Date: 26-Mar-2004
 * Time: 12:42:01
 */
public abstract class Bond extends Instrument {
    public abstract double getParAmount();
    public abstract void setParAmount(double parAmount);

    public abstract void setMaturityDate(Date date);
    public abstract Date getMaturityDate();

    public abstract void setCoupon(double coupon);

    public String toString() {
        return "BOND Par=" + getParAmount() + " " + "Maturity="
        + getMaturityDate() + " instrument=" + super.toString();
    }

}
