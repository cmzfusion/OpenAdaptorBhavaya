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

import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.util.Generic;
import org.bhavaya.util.LoadClosure;
import org.bhavaya.util.Log;
import org.bhavaya.util.Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.13 $
 */
public class ForeignKeyProperty extends org.bhavaya.beans.DerivedProperty {
    private static final Log log = Log.getCategory(ForeignKeyProperty.class);

    public static final String DATA_SOURCE_NAME_PROPERTY = "dataSourceName";

    public static final String MANY_PROPERTY_SUFFIX = "Collection";

    private String foreignIndex;
    private boolean lowDataQuantityAndVolatilitySet;
    private boolean lowDataQuantityAndVolatility;
    private Boolean valid;

    public ForeignKeyProperty(String parentTypeName, String name, String typeName, String cardinality, String foreignIndex, boolean lazy) {
        super(parentTypeName, name, typeName, cardinality, lazy);
        this.foreignIndex = foreignIndex;
    }

    public String getForeignIndex() {
        return foreignIndex;
    }

    public void initialiseSetPropertyState(Map state) {
        LoadGroup loadGroup = new LoadGroup();

        String dataSourceName = (String) state.get(DATA_SOURCE_NAME_PROPERTY);
        BeanFactory beanFactory = BeanFactory.getInstance(getGeneratedClass()); // use the default datasource, can be overriden by subclass
        BeanFactory parentBeanFactory = BeanFactory.getInstance(getParentGeneratedClass(), dataSourceName);

        if (parentBeanFactory.getClass() == beanFactory.getClass()) {
            beanFactory = BeanFactory.getInstance(getGeneratedClass(), dataSourceName);
        } else {
            beanFactory = BeanFactory.getInstance(getGeneratedClass()); // use the default datasource
        }
        state.put("beanFactoryForRun", beanFactory);
        state.put("loadGroupForRun", loadGroup);
        state.put("loadCacheForRun", new HashMap());
    }

    public boolean isValid() {
        if (valid == null) {
            valid = Boolean.TRUE;

            if (!Schema.hasInstance(getType())) {
                valid = Boolean.FALSE;
            }


            if (getCardinality().equals(org.bhavaya.beans.Property.MANY)) {
                if (foreignIndex == null) {
                    log.fatal("Cardinality is MANY, and no foreignIndex has been specified for ForeignKeyProperty: " + this + ", a NON-UNIQUE foreignIndex must be specified");
                    valid = Boolean.FALSE;
                    System.exit(1);
                } else if (Schema.getInstance(getTypeName()).getIndex(foreignIndex).isUnique()) {
                    log.fatal("Cardinality is MANY, and a UNIQUE foreignIndex has been specified for ForeignKeyProperty: " + this + ", a NON-UNIQUE foreignIndex must be specified");
                    valid = Boolean.FALSE;
                    System.exit(1);
                }
            } else if (getCardinality().equals(org.bhavaya.beans.Property.ONE) && foreignIndex != null && !Schema.getInstance(getTypeName()).getIndex(foreignIndex).isUnique()) {
                log.fatal("Cardinality is ONE, and a NON-UNIQUE foreignIndex has been specified for ForeignKeyProperty: " + this + ", a UNIQUE foreignIndex must be specified");
                valid = Boolean.FALSE;
                System.exit(1);
            }

            Column[] foreignColumns;
            if (getForeignIndex() == null) {
                foreignColumns = Schema.getInstance(getTypeName()).getPrimaryKey();
            } else {
                foreignColumns = Schema.getInstance(getTypeName()).getIndex(getForeignIndex()).getColumns();
            }

            if (parameterList.size() != foreignColumns.length) {
                log.error("Specified " + parameterList.size() + " parameters but " + (getForeignIndex() == null ? "primary key" : "index '" + getForeignIndex() + "'") + " requires " + foreignColumns.length + " parameters for ForeignKeyProperty: " + this);
                valid = Boolean.FALSE;
            }
        }


        return valid.booleanValue();
    }

    protected Class getColumnType(Column column) {
        Parameter parameter = (Parameter) columnParametersByColumn.get(column);
        int parameterIndex = parameterList.indexOf(parameter);
        return Schema.getInstance(getTypeName()).getKeyType(parameterIndex, foreignIndex);
    }


    public void setPropertyValue(Object bean, Object cachedColumnValuesForBean, Map state) {
        //if (log.isDebug()) log.debug("Getting derived value: " + getParentType() + "." + getName() + ": " + getType());
        try {
            Object propertyKey = getArguments(bean, cachedColumnValuesForBean);
            if (propertyKey == null) return;

            Object propertyValue;
            BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");

            if (beanFactory.isFullyInflatedKey(propertyKey, foreignIndex) || !isLazy() || isLowDataQuantityAndVolatility()) {
                propertyValue = getPropertyValue(propertyKey, state);
                propertyValue = applyPropertyValueTransform(bean, propertyValue);
            } else {
                // do not already have property value in a cache
                if (getCardinality().equals(ONE)) {
                    propertyValue = BeanFactory.newBeanInstance(getGeneratedClass());
                } else {
                    propertyValue = new LazyBeanCollection();
                }
                setLoad((Lazy) propertyValue, propertyKey, state);
            }

            if (propertyValue != null) {
                // bean may be a subclass, therefore do not use getGenericParentType
                Type genericParentBeanType = Generic.getType(bean.getClass());

                if (getCardinality().equals(ONE)) {
                    genericParentBeanType.set(bean, getName(), propertyValue);
                } else {
                    genericParentBeanType.set(bean, getName() + MANY_PROPERTY_SUFFIX, propertyValue);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private boolean isLowDataQuantityAndVolatility() {
        if (!lowDataQuantityAndVolatilitySet) {
            Schema schema = Schema.getInstance(getTypeName());
            lowDataQuantityAndVolatility = (schema.getDataQuantity().equals(Schema.LOW) && schema.getDataVolatility().equals(Schema.LOW));
            lowDataQuantityAndVolatilitySet = true;
        }
        return lowDataQuantityAndVolatility;
    }

    protected Object getPropertyValue(Object propertyKey, Map state) {
        BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");
        return beanFactory.get(propertyKey, foreignIndex);
    }

    private void setLoad(Lazy propertyValue, Object propertyKey, Map state) {
        Map loadCache = (Map) state.get("loadCacheForRun");
        LoadClosure load = (LoadClosure) loadCache.get(propertyKey);
        if (load == null) {
            load = createLoad(propertyKey, state);
            loadCache.put(propertyKey, load);
        }
        propertyValue.setLoad(load);
    }

    protected LoadClosure createLoad(Object propertyKey, Map state) {
        BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");
        LoadGroup loadGroup = (LoadGroup) state.get("loadGroupForRun");
        return new WrapperBeanFactoryLoad(beanFactory, propertyKey, foreignIndex, loadGroup, this);
    }

    public Object getColumnValue(Object bean, Column column) {
        Object propertyBean = super.getColumnValue(bean, column);
        if (propertyBean != null) return propertyBean;

        // e.g. if getting instrument_id of Trade bean, first get Instrument...

        // Cannot use getGenericType() as propertyBean may be a subclass of type
        if (getCardinality().equals(org.bhavaya.beans.Property.ONE)) {
            propertyBean = getValue(bean);
        } else {
            propertyBean = Generic.get(bean, getName() + MANY_PROPERTY_SUFFIX);
        }
        if (propertyBean == null) return null;

        // ... then get the key for the Instrument...
        Object propertyBeanKey = BeanFactory.getKeyForBean(propertyBean, getForeignIndex());

        // ... then extract the instrument_id from the Instruments key.
        if (parameterList.size() == 1) {
            return propertyBeanKey;
        } else {
            List propertyBeanKeyList = (List) propertyBeanKey;
            int parameterIndex = 0;
            for (int i = 0; i < parameterList.size(); i++) {
                DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) parameterList.get(i);
                if (parameter instanceof DerivedProperty.ColumnParameter) {
                    DerivedProperty.ColumnParameter keyColumnParameter = (DerivedProperty.ColumnParameter) parameter;
                    if (keyColumnParameter.getColumn().equals(column)) {
                        return propertyBeanKeyList.get(parameterIndex);
                    }
                }
                parameterIndex++;
            }
        }

        return null;
    }

    private Object getArguments(Object bean, Object cachedColumnValuesForBean) {
        Object arguments = null;
        int numberOfParameters = parameterList.size();

        if (numberOfParameters > 1) {
            arguments = new EfficientArrayList(numberOfParameters);
        }

        for (int i = 0; i < numberOfParameters; i++) {
            DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) parameterList.get(i);
            Object argument = getArgument(parameter, bean, cachedColumnValuesForBean);

            if (numberOfParameters > 1) {
                ((Collection) arguments).add(argument);
            } else {
                arguments = argument;
            }
        }

        arguments = Schema.getInstance(getType()).changeKeyType(arguments, foreignIndex);

        //if (log.isDebug()) log.debug("Arguments: " + arguments);
        return arguments;
    }

    protected static class WrapperBeanFactoryLoad extends BeanFactoryLoad {
        protected ForeignKeyProperty foreignKeyProperty;

        public WrapperBeanFactoryLoad(BeanFactory beanFactory, Object key, String index, LoadGroup loadGroup, ForeignKeyProperty foreignKeyProperty) {
            super(beanFactory, key, index, loadGroup);
            this.foreignKeyProperty = foreignKeyProperty;
        }

        protected Object get() {
            Object propertyValue = super.get();
            propertyValue = foreignKeyProperty.applyPropertyValueTransform(null, propertyValue);
            return propertyValue;
        }
    }
}