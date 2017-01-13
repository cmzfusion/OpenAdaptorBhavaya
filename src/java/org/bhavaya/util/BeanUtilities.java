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

import org.bhavaya.beans.Schema;
import sun.util.calendar.ZoneInfo;

import java.beans.*;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.21.4.1 $
 */

public class BeanUtilities {
    private static final Log log = Log.getCategory(BeanUtilities.class);

    private static Map<Class<?>, PersistenceDelegate> classPersistenceDelegates = new HashMap<Class<?>, PersistenceDelegate>();
    private static HashSetThreadLocal threadLocalAttributeValuesChecked = new HashSetThreadLocal();

    static {
        addPersistenceDelegate(java.sql.Date.class, new BhavayaPersistenceDelegate(new String[]{"time"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            }
        });

        addPersistenceDelegate(Locale.class, new BhavayaPersistenceDelegate(new String[]{"language", "country", "variant"}));

        addPersistenceDelegate(SimpleTimeZone.class, new TimeZonePersistenceDelegate());
        addPersistenceDelegate(ZoneInfo.class, new TimeZonePersistenceDelegate());

        // Set up and exceptional persistence delegates
        addPersistenceDelegate(BigDecimal.class, new PersistenceDelegate() {
            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }

            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[]{oldInstance.toString()});
            }
        });

        addPersistenceDelegate(BigInteger.class, new PersistenceDelegate() {
            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }

            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[]{oldInstance.toString()});
            }
        });
    }

    private static class TimeZonePersistenceDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, TimeZone.class, "getTimeZone", new Object[]{((TimeZone) oldInstance).getID()});
        }

        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }
    }

    public static void addPersistenceDelegate(Class clazz, PersistenceDelegate persistenceDelegate) {
        synchronized (classPersistenceDelegates) {
            if (classPersistenceDelegates.get(clazz) != null) log.warn("Replacing existing persistence delegate for: " + clazz);
            classPersistenceDelegates.put(clazz, persistenceDelegate);
        }
    }

    public static boolean hasPersistenceDelegate(Class clazz) {
        synchronized (classPersistenceDelegates) {
            return classPersistenceDelegates.containsKey(clazz);
        }
    }

    public static void addBhavayaBeanPersistenceDelegate(Class beanType) {
        Class generatedClass = Schema.getInstance(beanType).getGeneratedClass();
        addPersistenceDelegate(generatedClass, new BhavayaBeanPersistenceDelegate(beanType));
    }

    public static void writeObjectToStream(Object o, OutputStream out) {
        XMLEncoder encoder = getDefaultEncoder(out);
        writeObjectToStream(o, encoder);
        encoder.close();
    }

    public static void writeObjectToStream(Object o, XMLEncoder encoder) {
        final Exception[] firstException = new Exception[] { null };
        encoder.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                if (firstException[0] == null) firstException[0] = e;
                log.warn(e.getMessage());
            }
        });

        synchronized (classPersistenceDelegates) {
            for (Class<?> clazz : classPersistenceDelegates.keySet()) {
                encoder.setPersistenceDelegate(clazz, classPersistenceDelegates.get(clazz));
            }
        }

        try {
            encoder.writeObject(o);
            if (firstException[0] != null) {
                log.error("Abandoning Save", firstException[0]);
                throw firstException[0];
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static XMLEncoder getDefaultEncoder(OutputStream out) {
        Thread.currentThread().setContextClassLoader(ClassUtilities.getApplicationClassLoader());

        return new XMLEncoder(out) {
            public PersistenceDelegate getPersistenceDelegate(Class type) {
                PersistenceDelegate defaultType = super.getPersistenceDelegate(type);
                PersistenceDelegate localType;
                synchronized (classPersistenceDelegates) {
                    localType = (PersistenceDelegate) classPersistenceDelegates.get(type);
                }

                return localType == null ? defaultType : localType;
            }
        };
    }

    public static Object readObjectFromStream(InputStream in) {
        XMLDecoder decoder = getDefaultDecoder(in, true);
        Object o = decoder.readObject();
        decoder.close();
        return o;
    }

    public static XMLDecoder getDefaultDecoder(final InputStream in, boolean strict) {
        Thread.currentThread().setContextClassLoader(ClassUtilities.getApplicationClassLoader());

        /*
        if (strict) {
            return ElementDetailsLoggingXMLDecoder.createDecoder(in);
        } else {
        */
            return new XMLDecoder(in, null, new ExceptionListener() {
                public void exceptionThrown(Exception e) {
                }
            });
        //}
    }

    public static Object verySlowDeepCopy(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeObjectToStream(o, out);
        return readObjectFromStream(new ByteArrayInputStream(out.toByteArray()));
    }

    public static byte[] getXMLByteArray(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeObjectToStream(o, out);
        return out.toByteArray();
    }

    public static void copy(Object source, Object destination) {
        Type genericType = Generic.getType(source.getClass());
        int numberOfAttributes = genericType.getAttributes().length;

        for (int i = 0; i < numberOfAttributes; i++) {
            Object sourceValue = genericType.get(source, i);
            if (sourceValue != null && sourceValue.getClass().isArray()) {
                sourceValue = Utilities.copyArray((Object[]) sourceValue);
            } else {
                sourceValue = safeClone(sourceValue);
            }
            if (genericType.getAttribute(i).isWritable()) {
                genericType.set(destination, i, sourceValue);
            }
        }
    }

    /**
     * attempt to call the clone method. If it fails, just return the given instance
     *
     * @param o
     * @return
     */
    public static Object safeClone(Object o) {
        if (o != null && o instanceof Cloneable) {
            try {
                Method cloneMethod = o.getClass().getMethod("clone");
                o = cloneMethod.invoke(o);
            } catch (Exception e) {
                log.error("Could not clone instance of: " + o.getClass().getName(), e);
            }
        }
        return o;
    }

    public static String toString(Object object) {
        StringBuffer stringBuffer = new StringBuffer();
        Set attributeValuesChecked = threadLocalAttributeValuesChecked.getHashSet();

        if (object != null) {
            if (!attributeValuesChecked.contains(object)) {
                attributeValuesChecked.add(object);
                Class objectClass = object.getClass();
                stringBuffer.append(objectClass.getName());
                try {
                    Attribute[] attributes = Generic.getType(object).getAttributes();
                    for (int i = 0; i < attributes.length; i++) {
                        stringBuffer.append(", ");
                        String attributeName = attributes[i].getName();
                        try {
                            final Object attributeValue = Generic.get(object, attributeName);
                            if (!attributeValuesChecked.contains(attributeValue)) {
                                attributeValuesChecked.add(attributeValue);
                                stringBuffer.append(attributeName + " = " + attributeValue);
                                attributeValuesChecked.remove(attributeValue);
                            } else {
                                stringBuffer.append(attributeName + " = (INTROSPECTION RECURSED)");
                            }
                        } catch (Exception e) {
                            stringBuffer.append(attributeName + " = (INTROSPECTION FAILED)");
                        }
                    }
                } catch (Exception e) {
                    stringBuffer.append("(INTROSPECTION FAILED)");
                }
            } else {
                stringBuffer.append("(INTROSPECTION RECURSED)");
            }
        } else {
            stringBuffer.append("NULL");
        }

        attributeValuesChecked.clear();
        return stringBuffer.toString();
    }

    public static final String deltaToString(Object bean, Object comparisonBean) {
        if (bean == null) return "NULL";

        assert (comparisonBean != null) : "The bean you want to check against cannot be null";
        assert (bean.getClass() == comparisonBean.getClass()) : "Cannot get a deltaToString from unlike bean types";

        StringBuffer stringBuffer = new StringBuffer();
        Attribute[] attributes = Generic.getType(bean).getAttributes();
        stringBuffer.append(bean.getClass().getName());
        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            String attributeName = attribute.getName();
            Object beanValue = Generic.get(bean, attributeName);
            Object defaultValue = Generic.get(comparisonBean, attributeName);
            if (!Utilities.equals(beanValue, defaultValue)) {
                stringBuffer.append(", ");
                if (log.isDebug()) log.debug("Setting " + attributeName + " to " + defaultValue);
                stringBuffer.append(attributeName + " = " + beanValue);
            }
        }
        return stringBuffer.toString();
    }

    public static final Object getDeltaBean(Object bean1, Object bean2) {
        if (bean1 == null || bean2 == null) throw new RuntimeException("Beans cannot be null");
        if (bean1.getClass() != bean2.getClass()) throw new RuntimeException("Cannot get a delta beans from unlike bean types");

        Object deltaBean = null;
        try {
            deltaBean = bean1.getClass().newInstance();
            Attribute[] attributes = Generic.getType(bean1).getAttributes();
            for (int i = 0; i < attributes.length; i++) {
                Attribute attribute = attributes[i];
                Object oldValue = Generic.get(bean1, attribute.getName());
                Object newValue = Generic.get(bean2, attribute.getName());
                if (!Utilities.equals(oldValue, newValue)) {
                    if (log.isDebug()) log.debug("Setting " + attribute.getName() + " to " + newValue);
                    Generic.set(deltaBean, attribute.getName(), newValue);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not generate a delta bean", e);
        }
        return deltaBean;
    }

    public static final void applyBean(Object bean1, Object bean2) {
        applyBean(bean1, bean2, new ArrayList(0));
    }

    public static final void applyBean(Object bean1, Object bean2, List attributesToSkip) {
        if (bean1 == null || bean2 == null) throw new RuntimeException("Beans cannot be null");
        if (bean1.getClass() != bean2.getClass()) throw new RuntimeException("Beans must be same type");

        try {
            Attribute[] attributes = Generic.getType(bean1).getAttributes();
            for (int i = 0; i < attributes.length; i++) {
                Attribute attribute = attributes[i];
                if (attributesToSkip.contains(attribute.getName())) continue;
                Object oldValue = Generic.get(bean1, attribute.getName());
                Object newValue = Generic.get(bean2, attribute.getName());
                if (!Utilities.equals(oldValue, newValue)) {
                    if (log.isDebug()) log.debug("Setting " + attribute.getName() + " to " + newValue);
                    Generic.set(bean2, attribute.getName(), oldValue);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not apply bean changes", e);
        }
    }

    private static class HashSetThreadLocal extends ThreadLocal {
        protected Object initialValue() {
            return new HashSet();
        }

        public HashSet getHashSet() {
            return (HashSet) get();
        }
    }
}
