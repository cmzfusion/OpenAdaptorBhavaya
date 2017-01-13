package org.bhavayademo.beans;

import org.bhavaya.beans.Bean;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class InstrumentPrice extends org.bhavaya.beans.Bean {
    public abstract double getPrice();
    public abstract void setPrice(double price);

    public abstract java.util.Date getPriceDate();
    public abstract void setPriceDate(java.util.Date priceDate);

    public abstract void setInstrument(Instrument instrument);
}
