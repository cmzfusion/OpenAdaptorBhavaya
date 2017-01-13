package org.bhavaya.util;

import java.beans.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

/**
 * Subclass of DefaultPersistenceDelegate for backward compatibility between Java 6 and Java 5.
 * The Java 5 the instantiate() method looks for fields or properties, but in Java 6 this has been changed to
 * just use properties, breaking some of the legacy code. This class combines the two, looking for properties first,
 * then fields if no property can be found.  
 * User: Jonathan Moore
 * Date: 21-Jun-2010
 * Time: 17:27:02
 */
public class BhavayaPersistenceDelegate extends DefaultPersistenceDelegate {
    private String[] constructor;

    public BhavayaPersistenceDelegate() {
        this(new String[0]);
    }

    public BhavayaPersistenceDelegate(String[] constructorPropertyNames) {
        super(constructorPropertyNames);
        //Need to keep a local copy of the constructor args for use in the overridden instantiate() method.
        //Unfortunately this is private in the superclass, but is not modified
        this.constructor = constructorPropertyNames;
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        int nArgs = constructor.length;
        Object[] constructorArgs = new Object[nArgs];
        for(int i = 0; i < nArgs; i++) {

            try {
                Field field = findField(oldInstance.getClass(), constructor[i]);
                if(field != null && !Modifier.isStatic(field.getModifiers())) {
                    constructorArgs[i] = field.get(oldInstance);
                } else {
                    Method method = findMethod(oldInstance.getClass(), constructor[i]);
                    if(method != null) {
                        constructorArgs[i] = method.invoke(oldInstance);
                    } else {
                        throw new IllegalStateException("Could not find field or property for class "+ oldInstance.getClass() +
                                " by the name " + constructor[i]);
                    }
                }
            }
            catch (Exception e) {
                out.getExceptionListener().exceptionThrown(e);
            }
        }
        return new Expression(oldInstance, oldInstance.getClass(), "new", constructorArgs);
    }

    private Field findField(Class clazz, String property) throws IllegalAccessException {
        Field f = null;
        try {
            f = clazz.getDeclaredField(property);
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            //ignore
        }
        return f;
    }

    private Method findMethod(Class type, String property) throws IntrospectionException {
        if (property == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        BeanInfo info = Introspector.getBeanInfo(type);
        for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
            if (property.equals(pd.getName())) {
                Method method = pd.getReadMethod();
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }
}
