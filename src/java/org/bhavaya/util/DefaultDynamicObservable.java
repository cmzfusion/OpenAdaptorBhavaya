package org.bhavaya.util;

import java.util.HashMap;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class DefaultDynamicObservable extends DefaultObservable implements DynamicObservable {

    private final HashMap valueMap = new HashMap();

    public void set(String propertyName, Object value) {
        Object oldValue = valueMap.put(propertyName, value);
        firePropertyChange(propertyName, oldValue, value);
    }

    public Object get(String property) {
        return valueMap.get(property);
    }
}
