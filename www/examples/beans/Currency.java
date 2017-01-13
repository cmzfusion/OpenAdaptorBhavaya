package beans;

import org.bhavaya.util.DefaultObservable;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 25-Mar-2004
 * Time: 09:56:53
 * To change this template use File | Settings | File Templates.
 */
public abstract class Currency extends DefaultObservable {

    public abstract String getCode();

    public String toString() {
        return getCode();
    }
}
