package beans;

import org.bhavaya.util.DefaultObservable;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 02-Apr-2004
 * Time: 14:29:59
 * To change this template use File | Settings | File Templates.
 */
public abstract class TradeType extends DefaultObservable{
    public abstract String getTypeName();
    public abstract void setTypeName(String type);
}
