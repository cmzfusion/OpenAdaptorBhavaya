package org.bhavayademo.beans;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class BondFuture extends org.bhavayademo.beans.Instrument  {
    public abstract double getContractSize();
    public abstract void setContractSize(double contractSize);

    public abstract java.util.Date getFirstDeliveryDate();
    public abstract void setFirstDeliveryDate(java.util.Date firstDeliveryDate);

    public abstract java.util.Date getLastDeliveryDate();
    public abstract void setLastDeliveryDate(java.util.Date lastDeliveryDate);
}
