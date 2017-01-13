package org.bhavayademo.beans;

import org.bhavaya.util.Quantity;

import java.util.Date;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public abstract class Trade extends org.bhavaya.beans.Bean {
    public abstract Quantity getQuantity();

    public abstract void setQuantity(Quantity quantity);

    public abstract TradeType getTradeType();

    public abstract void setTradeType(TradeType tradeType);

    public abstract double getPrice();

    public abstract void setPrice(double price);

    public abstract void setTradeId(int tradeId);

    public abstract int getTradeId();

    public abstract void setVersion(int version);

    public abstract VersionStatus getVersionStatus();

    public abstract void setVersionStatus(VersionStatus versionStatus);

    public abstract Instrument getInstrument();

    public abstract void setInstrument(Instrument instrument);

    public abstract Date getTradeDate();

    public abstract void setTradeDate(Date tradeDate);

    public double getCashDelta() {
        return getQuantity().getAmount() * (getPrice() / 100);
    }

    public abstract String getComments();

    public abstract void setComments(String comments);

    public abstract Counterparty getCounterparty();
    public abstract void setCounterparty(Counterparty counterparty);
}
