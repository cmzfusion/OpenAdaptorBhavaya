package org.bhavayademo.beans;

import org.bhavaya.beans.BeanFactory;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class Currency extends org.bhavaya.beans.Bean {
    public static final Currency GBP = (Currency)org.bhavaya.beans.BeanFactory.getInstance(Currency.class).get("GBP");
    public static final Currency USD = (Currency) org.bhavaya.beans.BeanFactory.getInstance(Currency.class).get("USD");
    public static final Currency EUR = (Currency) org.bhavaya.beans.BeanFactory.getInstance(Currency.class).get("EUR");

    public abstract String getCode();

    public String toString() {
        return getCode();
    }
}
