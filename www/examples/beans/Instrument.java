package beans;

import org.bhavaya.util.DefaultObservable;

/**
 * Date: 23-Mar-2004
 * Time: 13:31:51
 */
public abstract class Instrument extends DefaultObservable {

    public abstract int getInstrumentId();
    public abstract Currency getCurrency();
    
    public abstract String getDescription();
    public abstract void setDescription(String description);

    public abstract void setValid(String valid);

    public abstract double getPrice();
    public abstract void setPrice(double price);

    public abstract Quantity getInstrumentQuantity();
    public abstract void setInstrumentQuantity(Quantity quantity);

    private static final double VAT=17.5;

    public double getVAT(){
        return VAT;
    }

    public double getVatPrice(){
        return getPrice()*(1+(getVAT()/100));
    }

    public String toString() {
        return getDescription();
    }

}
