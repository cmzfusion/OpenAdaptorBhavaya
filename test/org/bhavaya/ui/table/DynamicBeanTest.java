package org.bhavaya.ui.table;

import org.bhavaya.beans.Bean;
import org.bhavaya.util.DynamicObservable;

import java.util.HashMap;
import java.util.Map;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Dec 18, 2006
 * Time: 9:33:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicBeanTest extends Bean implements DynamicObservable {
    private Map<String, Object> dynamicProperties = new HashMap<String, Object>();

    private BeanD d = new BeanD();
    public static int multiple = 0;

    public DynamicBeanTest() {
        d.addPropertyChangeListener("i", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                set("dynamicProperty", d.getI() * multiple);
            }
        });
    }

    public void set(String propertyName, Object value) {
        Object oldValue = dynamicProperties.put(propertyName, value);
        firePropertyChange(propertyName, oldValue, value);
    }

    public Object get(String property) {
        return dynamicProperties.get(property);
    }

    public BeanD getD() {
        return d;
    }
}
