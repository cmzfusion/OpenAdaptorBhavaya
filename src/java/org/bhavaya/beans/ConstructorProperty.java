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

package org.bhavaya.beans;

import org.bhavaya.util.*;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.11 $
 */
public class ConstructorProperty extends org.bhavaya.beans.DerivedProperty {
    private static final Log log = Log.getCategory(ConstructorProperty.class);

    private Class[] parameterTypes;
    private Class propertyClass;
    private String propertyClassName;

    public ConstructorProperty(String parentTypeName, String name, String typeName, String propertyClassName, boolean lazy) {
        super(parentTypeName, name, typeName, org.bhavaya.beans.Property.ONE, lazy);
        this.propertyClassName = (propertyClassName != null && propertyClassName.length() > 0) ? propertyClassName : typeName;
    }

    public void initialiseSetPropertyState(Map state) {
    }

    protected Class getColumnType(Column column) {
        Parameter parameter = (Parameter) columnParametersByColumn.get(column);
        return parameter.getType();
    }

    public Class getPropertyClass() {
        if (propertyClass == null) {
            // prevent any class load order effects while loading schemas
            propertyClass = ClassUtilities.getClass(propertyClassName, true, false);
        }
        return propertyClass;
    }

    public String getPropertyClassName() {
        return propertyClassName;
    }

    public void setPropertyValue(Object bean, Object cachedColumnValuesForBean, Map state) {
        //if (log.isDebug()) log.debug("Getting derived value: " + getParentType() + "." + getName() + ": " + getType());
        try {
            Object propertyValue = newPropertyValue(bean, cachedColumnValuesForBean);
            if (propertyValue != null) {
                // bean may be a subclass, therefore do not use getGenericParentType
                Type genericParentBeanType = Generic.getType(bean.getClass());
                genericParentBeanType.set(bean, getName(), propertyValue);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    protected Object newPropertyValue(Object bean, Object cachedColumnValuesForBean) {
        Object propertyValue = null;

        try {
            if (isLazy()) {
                Lazy lazyPropertyValue = (Lazy) getPropertyClass().newInstance();
                LoadClosure load = new ConstructorLoad(bean, cachedColumnValuesForBean, this);
                lazyPropertyValue.setLoad(load);
                propertyValue = lazyPropertyValue;
            } else {
                propertyValue = newPropertyValueInternal(bean, cachedColumnValuesForBean);
            }
        } catch (Exception e) {
            log.error(e);
        }

        return propertyValue;
    }

    protected Object newPropertyValueInternal(Object bean, Object cachedColumnValuesForBean) {
        Object propertyValue = null;

        try {
            Constructor constructor = getPropertyClass().getConstructor(getParameterTypes());
            Object[] arguments = (Object[]) getArguments(bean, cachedColumnValuesForBean);
            if (arguments != null) {
                propertyValue = constructor.newInstance(arguments);
                propertyValue = applyPropertyValueTransform(bean, propertyValue);
            }
        } catch (Exception e) {
            log.error(e);
        }

        return propertyValue;
    }

    protected Class[] getParameterTypes() {
        if (parameterTypes == null) {
            Class[] parameterTypesTemp = new Class[parameterList.size()];
            int index = 0;
            for (int i = 0; i < parameterList.size(); i++) {
                DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) parameterList.get(i);
                Class parameterType = parameter.getType();
                if (log.isDebug()) log.debug("Found constructor parameter: " + parameterType.getName() + " for type: " + getParentType().getName() + "." + getName());
                parameterTypesTemp[index] = parameterType;
                index++;
            }
            parameterTypes = parameterTypesTemp;
        }
        return parameterTypes;
    }

    protected Object getArguments(Object bean, Object cachedColumnValuesForBean) {
        int numberOfParameters = parameterList.size();
        Object arguments = new Object[numberOfParameters];

        int index = 0;
        for (int i = 0; i < numberOfParameters; i++) {
            DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) parameterList.get(i);
            Object argument = getArgument(parameter, bean, cachedColumnValuesForBean);

            if (argument == null) {
                return null; // cannot determine all arguments
            }

            java.lang.reflect.Array.set(arguments, index, argument);
            index++;
        }

        //if (log.isDebug()) log.debug("Arguments: " + arguments);
        return arguments;
    }


    private static class ConstructorLoad implements LoadClosure {
        private ConstructorProperty constructorProperty;
        private final Object bean;
        private final Object cachedColumnValuesForBean;

        public ConstructorLoad(Object bean, Object cachedColumnValuesForBean, ConstructorProperty constructorProperty) {
            this.bean = bean;
            this.cachedColumnValuesForBean = cachedColumnValuesForBean;
            this.constructorProperty = constructorProperty;
        }

        public Object load() {
            Object value = constructorProperty.newPropertyValueInternal(bean, cachedColumnValuesForBean);
            if (value == null)
                value = BeanFactory.getLazyNull(constructorProperty.getPropertyClass());
            return value;
        }
    }
}