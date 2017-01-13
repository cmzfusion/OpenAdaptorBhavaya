package org.bhavayademo.beans;

import org.bhavaya.beans.BeanFactory;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class TradeType extends org.bhavaya.util.LookupValue   {
    public static final TradeType BUY = (TradeType) BeanFactory.getInstance(TradeType.class).get(new Integer(0));
    public static final TradeType SELL = (TradeType) BeanFactory.getInstance(TradeType.class).get(new Integer(1));
}
