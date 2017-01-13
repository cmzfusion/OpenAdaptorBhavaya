package org.bhavayademo.beans;

import org.bhavaya.beans.BeanFactory;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class InstrumentType extends org.bhavaya.util.LookupValue {
    public static final InstrumentType BOND = (InstrumentType)org.bhavaya.beans.BeanFactory.getInstance(InstrumentType.class).get(new Integer(0));
    public static final InstrumentType BOND_FUTURE = (InstrumentType) org.bhavaya.beans.BeanFactory.getInstance(InstrumentType.class).get(new Integer(1));
}
