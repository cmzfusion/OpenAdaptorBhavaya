package org.bhavayademo.beans;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public abstract class Instrument extends org.bhavaya.beans.Bean {
    private double price;

    public abstract int getInstrumentId();

    public abstract Currency getCurrency();
    public abstract void setCurrency(Currency currency);

    public abstract void setInstrumentType(InstrumentType instrumentType);

    public abstract String getDescription();
    public abstract void setDescription(String description);

    public abstract void setValid(String valid);

    public abstract void setIssuerCountry(String issuerCountry);
    public abstract String getIssuerCountry();

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        double oldValue = this.price;
        this.price = price;
        firePropertyChange("price", oldValue, price);
    }

    public String toString() {
        return getDescription();
    }

}
