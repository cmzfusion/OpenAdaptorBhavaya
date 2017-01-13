/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import com.sun.org.apache.xml.internal.utils.FastStringBuffer;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The basic idea of the Generic class is to provide static methods
 * for setting and getting property values on objects that work
 * both with Java objects that follow the Bean conventions
 * (setters and getters) and with instances of a special (private)
 * Record class (which uses arrays to store the data in a generic way).
 * <p>
 * The implementation of the private Record class is much more efficient than
 * the Hashtable based approaches that are often used to provide this
 * kind of feature. Each instance of the Record class, rather
 * than being a hashtable which knows how to map its keys to values is
 * instead a simple array of property values by index. The first
 * property is always "class" and the value of this property is
 * a hashtable which maps from property names to indexes. This way a
 * table of 10000 rows from a database can be instantiated without creating
 * 10000 hash tables. In fact, one would normally instantiate just one hash
 * table and then 10000 arrays which contain a reference to it. This is an
 * analogue of the way data would be stored in a Java VM - with the
 * getClass() method returning a shared class instance which describes
 * all its instances.
 * <p>
 * We want to add this functionality to all obejcts - but cannot
 * add it to the Object.class (because Java does not have categories).
 * As the next best thing, we implement this functionality as a
 * set of static methods which take the relevant instance as
 * an argument. [Placing the instances in a wrapper class of some
 * kind is another possible implementation approach - though this
 * causes deep problems for the type system as such a scheme
 * creates an ambiguity over whether the type of a method should
 * be the real type or the type of the wrapper class.]
 * <p>
 * @author Philip Milne
 * @version $Revision: 1.17.4.1 $
 */
public class Generic {
    private static final Log log = Log.getCategory(Generic.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final java.util.regex.Pattern beanPathSeperatarPattern = java.util.regex.Pattern.compile("\\.");
    private static Map classToObjectType = new IdentityHashMap();
    private static Map classToClassType = new IdentityHashMap();
    private static Map beanNameStringToArray = new ConcurrentReaderHashMap();
    private static Map beanNameArrayToString = new ConcurrentReaderHashMap();

    private static abstract class AbstractType implements Type {
        private Map nameToIndex;
        protected Attribute[] attributes;

        protected AbstractType() {
        }

        public Attribute getAttribute(int attributeIndex) {
            return attributes[attributeIndex];
        }

        public Attribute getAttribute(String attributeName) {
            int index = getAttributeIndex(attributeName);
            return getAttribute(index);
        }

        public boolean attributeExists(String attributeName) {
            populateNameToIndexMap();
            Integer index = (Integer) nameToIndex.get(attributeName);
            return index != null;
        }

        public Attribute[] getAttributes() {
            return attributes;
        }

        public int getAttributeIndex(String attributeName) {
            populateNameToIndexMap();
            Integer index = (Integer) nameToIndex.get(attributeName);
            if (index == null) {
                String error = "Undefined attribute: " + attributeName + " on type " + this;
                if (attributeName.indexOf('.') >= 0) {
                    error += " . This looks like a path, not an attribute. Try using 'Generic.beanPathStringToArray' to convert'";
                }
                throw new RuntimeException(error);
            }
            return index;
        }

        public Object get(Object instance, String attributeName) {
            if (attributeName == null || attributeName.length() == 0) return instance;
            instance = get(instance, getAttributeIndex(attributeName));
            return instance;
        }

        public void set(Object instance, String attributeName, Object value) {
            set(instance, getAttributeIndex(attributeName), value);
        }

        public int size() {
            return attributes.length;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractType)) return false;
            final AbstractType abstractType = (AbstractType) o;
            if (!Arrays.equals(attributes, abstractType.attributes)) return false;
            return true;
        }

        public int hashCode() {
            int result = 1;
            for (Attribute attribute : this.attributes) {
                result = 31 * result + attribute.hashCode();
            }
            return result;
        }

        private void populateNameToIndexMap() {
            if (nameToIndex == null) {
                Map tmpNameToIndex = new HashMap(size());
                for (int i = 0; i < size(); i++) {
                    String name = getAttribute(i).getName();
                    // If there is already an entry for this name, leave the first one.
                    if (tmpNameToIndex.get(name) == null) {
                        tmpNameToIndex.put(name, new Integer(i));
                    }
                }
                this.nameToIndex = tmpNameToIndex;
            }
        }
    }

    private static final class RecordType extends AbstractType {
        private static Attribute[] OBJECT_ATTRIBUTES = new Attribute[]{new DefaultAttribute("class", Class.class, true, false)};
        private static Map<List, RecordType> instances = new ConcurrentHashMap<List, RecordType>();

        public static RecordType getInstance(Type superType, Attribute[] attributes) {
            Object[] keyArray = new Object[attributes.length+1];
            keyArray[0] = superType;
            System.arraycopy(attributes, 0, keyArray, 1, attributes.length);
            List key = Arrays.asList(keyArray);
            RecordType type = (RecordType) instances.get(key);
            if (type == null) {
                type = new RecordType(superType.getAttributes(), attributes);
                instances.put(key, type);
            }
            return type;
        }

        public static RecordType getInstance(Attribute[] attributes) {
            List key = Arrays.asList(attributes);
            RecordType type = (RecordType) instances.get(key);
            if (type == null) {
                type = new RecordType(OBJECT_ATTRIBUTES, attributes);
                instances.put(key, type);
            }
            return type;
        }


        private RecordType(Attribute[] superAttributes, Attribute[] attributes) {
            this.attributes = Utilities.unionArrays(superAttributes, attributes);
        }

        public Object newInstance() {
            Record instance = new Record(size());
            instance.setType(this);
            return instance;
        }

        public Object get(Object instance, int attributeIndex) {
            return ((Record) instance).get(attributeIndex);
        }

        public void set(Object instance, int attributeIndex, Object newValue) {
            if (attributeIndex == 0) throw new IllegalArgumentException("Attempt to change Type of existing object");
            Record record = (Record) instance;
            record.set(attributeIndex, newValue);
        }
    }

    public static class ObjectType extends AbstractType {
        protected Class c;

        public ObjectType(Class c) {
            this.c = c;
            this.attributes = createAttributes(c);
        }

        protected GenericAttribute[] createAttributes(Class c) {
            List attributesList = new ArrayList();

            try {
                PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(c).getPropertyDescriptors();

                for (PropertyDescriptor pd : propertyDescriptors) {
                    if (pd.getPropertyType() != null) { // The type of indexed properties is null, ignore them.
                        attributesList.add(new PropertyDescriptorAttribute(pd));
                    }
                }
            } catch (Throwable e) {
                log.error("Error while finding attributes for: " + c.getName(), e);
            }

            return (GenericAttribute[]) attributesList.toArray(new GenericAttribute[attributesList.size()]);
        }

        public Object newInstance() {
            try {
                return c.newInstance();
            } catch (Exception e) {
                log.error(e);
            }
            return null;
        }


        public Object get(Object instance, int attributeIndex) {
            GenericAttribute getter = ((GenericAttribute) attributes[attributeIndex]);
            return getter.get(instance);
        }

        public void set(Object instance, int attributeIndex, Object value) {
            GenericAttribute setter = ((GenericAttribute) attributes[attributeIndex]);
            setter.set(instance, value);
        }

        public String toString() {
            return c.toString();
        }
    }

    static interface GenericAttribute extends Attribute {
        public Object get(Object instance);
        public void set(Object instance, Object value);
    }

    private static class PropertyDescriptorAttribute implements GenericAttribute {
        private PropertyDescriptor propertyDescriptor;
        private boolean primitive;

        public PropertyDescriptorAttribute(PropertyDescriptor pd) {
            propertyDescriptor = pd;
            // cache primitive as each invocation of getParameterTypes() creates a Class[], allow set to be called many times without creating much garbage
            primitive = propertyDescriptor.getWriteMethod() != null && propertyDescriptor.getWriteMethod().getParameterTypes()[0].isPrimitive();
        }

        public String getName() {
            return propertyDescriptor.getName();
        }

        public Class getType() {
            return propertyDescriptor.getPropertyType();
        }

        public boolean isReadable() {
            return propertyDescriptor.getReadMethod() != null;
        }

        public boolean isWritable() {
            return propertyDescriptor.getWriteMethod() != null;
        }

        public int compareTo(Object o) {
            return getName().compareTo(((Attribute) o).getName());
        }

        public final Object get(Object instance) {
            return read(propertyDescriptor.getReadMethod(), instance);
        }

        public final void set(Object instance, Object value) {
            if (value == null && primitive) {
                if (getType() == double.class) {
                    // instead of null we have to set a NaN here
                    value = Double.NaN;
                } else if (getType() == float.class) {
                    // instead of null we have to set a NaN here
                    value = Float.NaN;
                } else {
                    // do not set a null primitive - well not all
                    return;
                }
            }
            write(propertyDescriptor.getWriteMethod(), instance, value);
        }
    }

    private static Object read(Method readMethod, Object instance) {
        if (readMethod != null) {
            try {
                return readMethod.invoke(instance, null);
            } catch (Exception e) {
                log.error("Calling: " + readMethod.getDeclaringClass().getName() + "." + readMethod.getName(), e);
            }
        }
        return null;
    }

    private static void write(Method writeMethod, Object instance, Object value) {
        if (writeMethod != null) {
            try {
                writeMethod.invoke(instance, value);
            } catch (Exception e) {
                log.error("Calling: " + writeMethod.getDeclaringClass().getName() + "." + writeMethod.getName() + " on instance of: " + instance.getClass().getName() + " with parameter of type: " + (value == null ? "null" : value.getClass().getName()), e);
            }
        }
    }

    private static class DefaultGenericAttribute extends DefaultAttribute implements GenericAttribute {
        private Method readMethod;
        private Method writeMethod;
        private boolean primitive;

        public DefaultGenericAttribute(String name, Class<?> type, Method readMethod, Method writeMethod) {
            super(name, type, readMethod != null, writeMethod != null);
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            // cache primitive as each invocation of getParameterTypes() creates a Class[], allow set to be called many times without creating much garbage
            primitive = writeMethod != null && writeMethod.getParameterTypes()[0].isPrimitive();
        }

        public final Object get(Object instance) {
            return read(readMethod, instance);
        }

        public final void set(Object instance, Object value) {
            // do not set a null primitive
            if (value == null && primitive) return;
            write(writeMethod, instance, value);
        }
    }


    /**
     * Make's static get/set methods on a class appear as properties of that class.
     */
    private static class ClassType extends ObjectType {

        private ClassType(Class c) {
            super(c);
        }

        protected GenericAttribute[] createAttributes(Class c) {
            List attributesList = new ArrayList();
            Method[] methods = c.getMethods();


            for (Method method : methods) {
                Method readMethod;
                Method writeMethod = null;

                String methodName = method.getName();
                Class returnType = method.getReturnType();

                if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && returnType != null)
                {
                    String propertyName = getPropertyName(methodName);

                    if (propertyName != null) {
                        readMethod = method;

                        try {
                            writeMethod = c.getMethod("set" + Utilities.capitalise(propertyName), returnType);
                        } catch (Exception e) {
                        }
                        attributesList.add(new DefaultGenericAttribute(propertyName, returnType, readMethod, writeMethod));
                    }
                }
            }

            Attribute[] classInstanceAttributes = super.createAttributes(Class.class);
            for (Attribute classInstanceAttribute : classInstanceAttributes) {
                attributesList.add(classInstanceAttribute);
            }

            return (GenericAttribute[]) attributesList.toArray(new GenericAttribute[attributesList.size()]);
        }

        private static String getPropertyName(String methodName) {
            String propertyName = null;
            if (methodName.startsWith("get")) {
                propertyName = Utilities.decapitalise(methodName.substring("get".length(), methodName.length()));
            } else if (methodName.startsWith("is")) {
                propertyName = Utilities.decapitalise(methodName.substring("is".length(), methodName.length()));
            }
            return propertyName;
        }

        public Object newInstance() {
            return c;
        }
    }

    private static class Record {
        private Object[] data;

        protected Record(int length) {
            this.data = new Object[length];
        }

        public Object get(int attributeIndex) {
            try {
                return data[attributeIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("Failed to retrieve value for index " + attributeIndex);
                throw e;
            }
        }

        public void set(int attributeIndex, Object value) {
            try {
                data[attributeIndex] = value;
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("Failed to set value for index " + attributeIndex);
                throw e;
            }
        }

        public Type getType() {
            return (Type) data[0];
        }

        public void setType(Type value) {
            data[0] = value;
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Record)) return false;

            final Record record = (Record) o;
            if (!Arrays.equals(data, record.data)) return false;

            return true;
        }

        public int hashCode() {
            int hashCode = 1;
            for (int i = 0; i < data.length; i++) {
                Object obj = data[i];
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
            return hashCode;
        }
    }

    public static Type getType(Object instance) {
        if (instance.getClass() == Record.class) { // would fail if you could subclass Record
            return ((Record) instance).getType();
        } else if (instance.getClass() == Class.class) {
            return getClassType((Class) instance);
        } else {
            Class c = instance.getClass();
            return getType(c);
        }
    }

    public static Type getType(Class c) {
        if (c == null) return null;
        Type result = (Type) classToObjectType.get(c);
        if (result == null) {
            if (DynamicObservable.class.isAssignableFrom(c)) {
                result = new DynamicObjectType(c);
            } else {
                result = new ObjectType(c);
            }
            classToObjectType.put(c, result);
        }
        return result;
    }

    public static Type getClassType(Class c) {
        if (c == null) return null;
        Type result = (Type) classToClassType.get(c);
        if (result == null) {
            result = new ClassType(c);
            classToClassType.put(c, result);
        }
        return result;
    }

    public static Type getType(Attribute[] attributes) {
        return RecordType.getInstance(attributes);
    }

    public static Type getType(Type superType, Attribute[] attributes) {
        return RecordType.getInstance(superType, attributes);
    }

    public static Object get(Object instance, String attributeName) {
        return getType(instance).get(instance, attributeName);
    }

    public static Object get(Object instance, int attributeIndex) {
        return getType(instance).get(instance, attributeIndex);
    }

    /**
     * get a value for the given locator path under bean
     * @throws NullPointerException if the locator path is not valid
     * @return value under locator path
     */
    public static Object getBeanValue(Object bean, String locator) {
        return get(bean, beanPathStringToArray(locator));
    }

    /**
     * get a value for the given locator path under bean
     * @return value under locator path, or null if the locator path is not valid
     */
    public static Object getBeanValueIfExists(Object bean, String locator) {
        return get(bean, beanPathStringToArray(locator), 0, true);
    }

    public static Object get(Object bean, String[] beanPath) {
        return get(bean, beanPath, 0);
    }

    public static Object get(Object bean, String[] beanPath, int startIndex) {
        return get(bean, beanPath, startIndex, false);
    }

    /**
     * @param allowInvalidPath if set, then this will return null rather than throwing a RuntimeException if there is no
     * such property for the given bean
     */
    public static Object get(Object bean, String[] beanPath, int startIndex, boolean allowInvalidPath) {
        return get(bean, beanPath, startIndex, beanPath.length, allowInvalidPath);
    }

    public static Object get(Object bean, String[] beanPath, int startIndex, int endIndex, boolean allowInvalidPath) {
        Object beanValue = bean;
        for (int i = startIndex; i < endIndex; i++) {
            String attrName = beanPath[i];
            if (allowInvalidPath &&
                    (bean == null || !getType(beanValue).attributeExists(attrName))) {
                beanValue = null;
            } else {
                beanValue = get(beanValue, attrName);
            }
            if (beanValue == null) return null;
        }
        return beanValue;
    }

    public static void set(Object instance, String attributeName, Object value) {
        getType(instance).set(instance, attributeName, value);
    }

    public static void set(Object instance, int attributeIndex, Object value) {
        getType(instance).set(instance, attributeIndex, value);
    }

    /**
     * @param allowInvalidPath if set, then this will not set the value rather than throwing a RuntimeException if there is no
     * such property for the given bean
     */
    public static void set(Object bean, String[] beanPath, Object value, boolean allowInvalidPath) {
        Object beanParent = bean;
        for (int i = 0; i < beanPath.length - 1; i++) {
            String attrName = beanPath[i];
            if (allowInvalidPath && !getType(beanParent).attributeExists(attrName)) {
                beanParent = null;
            } else {
                beanParent = get(beanParent, attrName);
            }
            if (beanParent == null) return;
        }
        if(!allowInvalidPath || getType(beanParent).attributeExists(beanPath[beanPath.length - 1])) {
            Generic.set(beanParent, beanPath[beanPath.length - 1], value);
        }
    }

    public static Attribute getAttribute(Class rootClass, String[] beanPath) {
        return getAttribute(rootClass, beanPath, false);
    }

    public static Attribute getAttribute(Class rootClass, String[] beanPath, boolean checkSubClasses) {
        Type type = getType(rootClass);
        return getAttribute(type, beanPath, checkSubClasses);
    }

    public static Attribute getAttribute(Object instance, String[] beanPath) {
        return getAttribute(instance, beanPath, false);
    }

    public static Attribute getAttribute(Object instance, String[] beanPath, boolean checkSubClasses) {
        Type type = getType(instance); //may get object/class/record type
        return getAttribute(type, beanPath, checkSubClasses);
    }

    private static Attribute getAttribute(Type type, String[] beanPath) {
        return getAttribute(type, beanPath, false);
    }

    private static Attribute getAttribute(Type type, String[] beanPath, boolean checkSubClasses) {
        if(checkSubClasses) {
            return getAttributeCheckingSubclasses(type, beanPath);
        }
        Attribute attribute = null;
        for (String property : beanPath) {
            attribute = type.getAttribute(property);
            Class returnClass = attribute.getType();
            type = getType(returnClass);
        }
        return attribute;
    }

    private static Attribute getAttributeCheckingSubclasses(Type type, String[] beanPath) {
        Attribute attribute = null;
        for (String property : beanPath) {
            if(!type.attributeExists(property) && attribute != null) {
                type = findSubClassTypeWithAttribute(attribute.getType(), property);
            }
            if(type != null && type.attributeExists(property)) {
                attribute = type.getAttribute(property);
                Class returnClass = attribute.getType();
                type = getType(returnClass);
            } else {
                attribute = null;
                break;
            }
        }
        return attribute;
    }

    private static Type findSubClassTypeWithAttribute(Class clazz, String property) {
        Class[] subClasses = PropertyModel.getInstance(clazz).getSubClasses();
        for(Class subClass : subClasses) {
            Type subType = getType(subClass);
            if(subType.attributeExists(property)) {
                return subType;
            }
        }
        return null;
    }

    public static String[] beanPathStringToArray(String locator) {
        if (locator != null && locator.length() > 0) {
            String[] nameArray = (String[]) beanNameStringToArray.get(locator);
            if (nameArray == null) {
                nameArray = beanPathSeperatarPattern.split(locator);
                Utilities.internStringsInArray(nameArray);
                locator = locator.intern();
                beanNameStringToArray.put(locator, nameArray);
            }
            return nameArray;
        } else {
            return EMPTY_STRING_ARRAY;
        }
    }

    public static String beanPathArrayToString(String[] path) {
        return beanPathArrayToString(path, true);
    }

    public static String beanPathArrayToString(String[] path, boolean cacheArray) {
        if (path.length == 0) return "";

        String beanName = (String) beanNameArrayToString.get(path);
        if (beanName == null) {
            FastStringBuffer buf = new FastStringBuffer();
            for (String element : path) {
                buf.append(element);
                buf.append(".");
            }
            buf.setLength(buf.length() - 1);
            beanName = buf.toString().intern();

            if (cacheArray) {
                if (!beanNameArrayToString.containsValue(beanName)) {
                    beanNameArrayToString.put(path, beanName);
                }
            }
        }
        return beanName;
    }
}