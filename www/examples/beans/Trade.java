package beans;

import org.bhavaya.util.DefaultObservable;

import java.util.Date;

/**
 * Date: 02-Apr-2004
 * Time: 14:18:31
 */
public abstract class Trade extends DefaultObservable{

    public abstract double getQuantity();

    public abstract void setQuantity(double quantity);

    public abstract TradeType getTradeType();

    public abstract void setTradeType(TradeType tradeType);

    public abstract double getPrice();

    public abstract void setPrice(double price);

    public abstract void setTradeId(int tradeId);

    public abstract void setVersion(int version);

    public abstract Instrument getInstrument();

    public abstract void setInstrument(Instrument instrument);

    public abstract Date getTradeDate();

    public abstract void setTradeDate(Date tradeDate);

    public double getCashDelta() {
        return getQuantity() * (getPrice() / 100);
    }

    public abstract String getComments();

    public abstract void setComments(String comments);

}
