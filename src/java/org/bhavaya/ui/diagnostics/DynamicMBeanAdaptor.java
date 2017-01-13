package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.*;

import javax.management.*;
import javax.management.Attribute;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Uses reflection to expose all the object's attributes and methods through the JMX interface.
 * Implements DynamicMBean interface.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public class DynamicMBeanAdaptor implements DynamicMBean {

    private static final Log log = Log.getCategory(DynamicMBeanAdaptor.class);

    private Object instance;
    private MBeanInfo mBeanInfo;

    public DynamicMBeanAdaptor(Object instance) {
        this.instance = instance;
        Class clazz = instance.getClass();
        Type type = Generic.getType(clazz);

        MBeanAttributeInfo[] attributesInfo = null;
        MBeanConstructorInfo[] constructorsInfo = null;
        MBeanOperationInfo[] operationsInfo = null;
        MBeanNotificationInfo[] notificationsInfo = null;

        org.bhavaya.util.Attribute[] attributes = type.getAttributes();
        if (attributes != null && attributes.length > 0) {
            attributesInfo = new MBeanAttributeInfo[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                org.bhavaya.util.Attribute attribute = attributes[i];
                attributesInfo[i] = new MBeanAttributeInfo(attribute.getName(), attribute.getType().getName(), attribute.getName(), attribute.isReadable(), attribute.isWritable(), attribute.getType().equals(Boolean.class));
            }
        }

        Method[] methods = clazz.getMethods();
        if (methods != null && methods.length > 0) {
            ArrayList methodInfoList = new ArrayList(methods.length);
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getDeclaringClass() != Object.class) {
                    methodInfoList.add(new MBeanOperationInfo(method.getName(), method));
                }
            }
            operationsInfo = (MBeanOperationInfo[]) methodInfoList.toArray(new MBeanOperationInfo[methodInfoList.size()]);
        }

        mBeanInfo = new MBeanInfo(clazz.getName(), ClassUtilities.getUnqualifiedClassName(clazz.getName()), attributesInfo, constructorsInfo, operationsInfo, notificationsInfo);
    }

    public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return Generic.get(instance, name);
    }

    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Generic.set(instance, attribute.getName(), attribute.getValue());
    }

    public AttributeList getAttributes(String[] names) {
        AttributeList result = new AttributeList();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            try {
                result.add(new Attribute(name, Generic.get(instance, name)));
            } catch (Exception ex) {
                log.error("Unexpected exception", ex);
            }
        }
        return result;
    }

    public AttributeList setAttributes(AttributeList attributeList) {
        Iterator iterator = attributeList.iterator();
        while (iterator.hasNext()) {
            Attribute attribute = (Attribute) iterator.next();
            try {
                Generic.set(instance, attribute.getName(), attribute.getValue());
            } catch (Exception ex) {
                log.error("Unexpected exception", ex);
            }
        }
        return attributeList;
    }

    public Object invoke(String name, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        try {
            Class clazz = instance.getClass();
            Class[] parameterTypes = new Class[signature.length];
            for (int i = 0; i < signature.length; i++) {
                String s = signature[i];
                parameterTypes[i] = ClassUtilities.getClass(s);
            }
            Method method = clazz.getMethod(name, parameterTypes);
            if (method != null) {
                return method.invoke(instance, params);
            }
        } catch (Exception ex) {
            log.error("Unexpected exception", ex);
            if (ex.getCause() != null) {
                return ex.getCause();
            } else {
                return ex;
            }
        }
        return null;
    }

    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }
}
