package org.bhavaya.beans;

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import java.lang.reflect.Method;

/**
 * Rather than using constructor of a class in some cases its easier to have a factory to decide which derived
 * class to instantiate or what params to pass into the property class' constructor.
 * <p>
 * Factory method has to provide empty constructor and get method with parameters of type defined in schema. 
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class FactoryProperty extends ConstructorProperty {
    private static final Log log = Log.getCategory(FactoryProperty.class);

    private String factoryClassName;
    private Object factory;

    public FactoryProperty(String parentTypeName, String name, String typeName, String className, String factoryClassName, boolean lazy) {
        super(parentTypeName, name, typeName, className, lazy);
        this.factoryClassName = factoryClassName;
    }

    private synchronized Object getFactory() {
        if (factory == null) {
            try {
                Class aClass = ClassUtilities.getClass(factoryClassName, true, true);
                factory = aClass.newInstance();
            } catch (Throwable t) {
                log.error("Unable to instantiate the factory: " + factoryClassName,t);
            }
        }
        return factory;
    }

    protected Object newPropertyValueInternal(Object bean, Object cachedColumnValuesForBean) {
        Object propertyValue = null;

        try {
            Object factory = getFactory();
            Method factoryMethod = factory.getClass().getMethod("get", getParameterTypes());
            Object[] arguments = (Object[]) getArguments(bean, cachedColumnValuesForBean);
            if (arguments != null) {
                propertyValue = factoryMethod.invoke(factory, arguments);
                propertyValue = applyPropertyValueTransform(bean, propertyValue);
            }
        } catch (Throwable t) {
            log.error(t);
        }

        return propertyValue;
    }
}
