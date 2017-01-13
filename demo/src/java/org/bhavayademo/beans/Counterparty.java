package org.bhavayademo.beans;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public abstract class Counterparty extends org.bhavaya.beans.Bean {
    public abstract String getName();

    public String toString() {
        return getName();
    }
}
