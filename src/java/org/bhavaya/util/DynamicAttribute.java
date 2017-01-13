package org.bhavaya.util;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class DynamicAttribute extends DefaultAttribute implements Generic.GenericAttribute {

    public DynamicAttribute(String name, Class type) {
        super(name, type);
    }

    public DynamicAttribute(String name, Class type, boolean readable, boolean writable) {
        super(name, type, readable, writable);
    }

    public Object get(Object instance) {
        return ((DynamicObservable)instance).get(getName());
    }

    public void set(Object instance, Object value) {
        ((DynamicObservable)instance).set(getName(), value);
    }
}
