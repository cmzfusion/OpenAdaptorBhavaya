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

import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.Table;
import org.bhavaya.db.TableColumn;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.openadaptor.dataobjects.*;
import org.openadaptor.util.DateHolder;
import org.openadaptor.util.DateTimeHolder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 1) Is a factory for empty DataObjects, uses reflection to define
 * the schema for the objects that are constructed using <code>getEmptyDataObject</code>.
 * 2) Converts between DataObjects and Objects and vice versa, using <code>asObject</code> and
 * <code>asDataObject</code>.  This currently does not handle arrays of primitives or recursive
 * structures.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class DataObjectMapper {
    private static final Log log = Log.getCategory(DataObjectMapper.class);

    private static final DataObjectMapper instance = new DataObjectMapper();

    /**
     * Used by getDateTimeHolderForDate and getDateForDateTimeHolder to set
     * timeZone.  Default to GMT.
     */
    private static final String TIMEZONE_NAME = "GMT";


    /**
     * OpenAdapter can have problems if the DOType name contains ".".
     * Therefore replace them with this character.
     * This can be dangerous where the class name really does contain this character
     */
    private static final char PACKAGE_DELIMITER = '_';

    private Map doTypeCache = new HashMap();
    private final Set simpleTypes = new HashSet();
    private Map typeToDOTypeMappings = new HashMap();
    private Map classToDOTypeMappings = new HashMap();
    private Map dOTypeToClassMappings = new HashMap();


    public static DataObjectMapper getInstance() {
        return instance;
    }

    private DataObjectMapper() {
        simpleTypes.add(Boolean.class);
        simpleTypes.add(Boolean.TYPE);
        simpleTypes.add(Character.class);
        simpleTypes.add(Character.TYPE);
        simpleTypes.add(Byte.class);
        simpleTypes.add(Byte.TYPE);
        simpleTypes.add(Short.class);
        simpleTypes.add(Short.TYPE);
        simpleTypes.add(Integer.class);
        simpleTypes.add(Integer.TYPE);
        simpleTypes.add(Long.class);
        simpleTypes.add(Long.TYPE);
        simpleTypes.add(Float.class);
        simpleTypes.add(Float.TYPE);
        simpleTypes.add(Double.class);
        simpleTypes.add(Double.TYPE);
        simpleTypes.add(BigDecimal.class);
        simpleTypes.add(BigInteger.class);
        simpleTypes.add(String.class);

        classToDOTypeMappings.put(Boolean.class, SDOType.BOOLEAN);
        classToDOTypeMappings.put(Boolean.TYPE, SDOType.BOOLEAN);
        classToDOTypeMappings.put(Character.class, SDOType.STRING);
        classToDOTypeMappings.put(Character.TYPE, SDOType.STRING);
        classToDOTypeMappings.put(Byte.class, SDOType.INT32);
        classToDOTypeMappings.put(Byte.TYPE, SDOType.INT32);
        classToDOTypeMappings.put(Short.class, SDOType.INT32);
        classToDOTypeMappings.put(Short.TYPE, SDOType.INT32);
        classToDOTypeMappings.put(Integer.class, SDOType.INT32);
        classToDOTypeMappings.put(Integer.TYPE, SDOType.INT32);
        classToDOTypeMappings.put(Long.class, SDOType.INT64);
        classToDOTypeMappings.put(Long.TYPE, SDOType.INT64);
        classToDOTypeMappings.put(Float.class, SDOType.FLOAT);
        classToDOTypeMappings.put(Float.TYPE, SDOType.FLOAT);
        classToDOTypeMappings.put(Double.class, SDOType.DOUBLE);
        classToDOTypeMappings.put(Double.TYPE, SDOType.DOUBLE);
        classToDOTypeMappings.put(BigDecimal.class, SDOType.DOUBLE);
        classToDOTypeMappings.put(BigInteger.class, SDOType.INT64);
        classToDOTypeMappings.put(String.class, SDOType.STRING);

        typeToDOTypeMappings.put(java.sql.Date.class, SDOType.DATE);
        typeToDOTypeMappings.put(java.util.Date.class, SDOType.DATETIME);

        dOTypeToClassMappings.put(SDOType.BOOLEAN.getName(), Boolean.class);
        dOTypeToClassMappings.put(SDOType.INT16.getName(), Integer.class); // rather than a short
        dOTypeToClassMappings.put(SDOType.INT32.getName(), Integer.class);
        dOTypeToClassMappings.put(SDOType.INT64.getName(), Long.class);
        dOTypeToClassMappings.put(SDOType.FLOAT.getName(), Float.class);
        dOTypeToClassMappings.put(SDOType.DOUBLE.getName(), Double.class);
        dOTypeToClassMappings.put(SDOType.STRING.getName(), String.class);
        dOTypeToClassMappings.put(SDOType.DATE.getName(), java.sql.Date.class);
        dOTypeToClassMappings.put(SDOType.DATETIME.getName(), java.util.Date.class);
    }

    public Object asObject(DataObject dataObject, Set excludeProperties) {
        Class beanClass = mapDOTypeToJavaClass(dataObject.getType());
        TabularData tabularData = new DataObjectTabularData(new DataObject[]{dataObject});
        TabularDataToBeanTransformer tabularDataToBeanTransformer = new TabularDataToBeanTransformer(beanClass, tabularData, excludeProperties);
        try {
            tabularDataToBeanTransformer.process();
            List beans = tabularDataToBeanTransformer.getBeans();
            if (beans.size() == 1) return beans.get(0);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    /**
     * Builds a DataObject instance from an Object instance, by introspecting the objects properties.
     */
    public DataObject asDataObject(Object object) {
        try {
            return asDataObject(object, new HashMap(), null);
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException("Cannot create data object", e);
        }
    }

    public DataObject asDataObject(Object object, Map additionalAttributeValues) {
        try {
            return asDataObject(object, new HashMap(), additionalAttributeValues);
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException("Cannot create data object", e);
        }
    }

    private DataObject asDataObject(Object bean, Map createdDataObjects, Map additionalAttributeValues) throws Exception {
        if (bean == null) return null;

        // need to allow circular relationships
        DataObject dataObject = (DataObject) createdDataObjects.get(bean);
        if (dataObject != null) {
            // dataObject has already been constructed
            return dataObject;
        }
        dataObject = getEmptyDataObject(bean.getClass(), additionalAttributeValues);
        createdDataObjects.put(bean, dataObject);

        DOType dataObjectType = dataObject.getType();

        Class beanType = mapDOTypeToJavaClass(dataObjectType);

        Schema schema = Schema.getInstance(beanType);
        CatalogSchemaTable[] tables = schema.getSql().getTables();
        Set attributesSet = new HashSet();
        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            TableColumn[] tableColumns = Table.getInstance(table, schema.getDefaultDataSourceName()).getColumns(beanType);
            Arrays.sort(tableColumns);

            for (int j = 0; j < tableColumns.length; j++) {
                TableColumn tableColumn = tableColumns[j];
                String doAttributeName = tableColumn.getName();
                if (!attributesSet.contains(doAttributeName)) {
                    attributesSet.add(doAttributeName);
                    Object doAttributeValue = TabularDataBeanFactory.getColumnValueForBean(bean, tableColumn);
                    setDataObjectAttributeValue(dataObject, doAttributeName, doAttributeValue);
                }
            }
        }

        return dataObject;
    }

    private DataObject getEmptyDataObject(Class type, Map additionalAttributeValues) throws DataObjectException {
        SimpleDataObject dataObject = new SimpleDataObject(getDoType(type));

        if (additionalAttributeValues != null) {
            for (Iterator iterator = additionalAttributeValues.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String attributeName = (String) entry.getKey();
                Object attributeValue = entry.getValue();
                dataObject.addAttributeValue(attributeName, attributeValue);
            }
        }

        return dataObject;
    }

    private SDOType getDoType(Class beanType) throws DataObjectException {
        SDOType doType = (SDOType) doTypeCache.get(beanType);

        if (doType == null) {
            String doTypeName = beanType.getName().replace('.', PACKAGE_DELIMITER);
            log.info("Creating DOType: " + doTypeName + " for: " + beanType.getName());
            doType = new SDOType(doTypeName);
            doTypeCache.put(beanType, doType);

            Schema schema = Schema.getInstance(beanType);
            CatalogSchemaTable[] tables = schema.getSql().getTables();
            for (int i = 0; i < tables.length; i++) {
                CatalogSchemaTable table = tables[i];
                Column[] columns = Table.getInstance(table, schema.getDefaultDataSourceName()).getColumns(beanType);
                Arrays.sort(columns);

                for (int j = 0; j < columns.length; j++) {
                    Column column = columns[j];
                    String doAttributeName = column.getName();
                    SDOType doAttributeType = mapJavaClassToDOType(column);

                    if (log.isDebug()) log.debug("Adding attribute of type: " + doAttributeType.getName() + " for: " + column.getRepresentation());

                    if (!doType.isAttributeDefined(doAttributeName)) {
                        doType.addAttribute(doAttributeName, doAttributeType);
                    } else {
                        if (log.isDebug()) log.debug("Ignoring attribute of type: " + doAttributeType.getName() + " for: " + column.getRepresentation() + " as it already exists");
                    }
                }
            }
        }

        return doType;
    }

    private SDOType mapJavaClassToDOType(Column column) throws DataObjectException {
        Class javaClass = column.getType();
        return mapJavaClassToDOType(javaClass);
    }

    public SDOType mapJavaClassToDOType(Class javaClass) throws DataObjectException {
        SDOType doType = (SDOType) classToDOTypeMappings.get(javaClass);

        if (doType == null) {
            for (Iterator iterator = typeToDOTypeMappings.entrySet().iterator(); iterator.hasNext() && doType == null;) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Class mappedClass = (Class) entry.getKey();
                if (mappedClass.isAssignableFrom(javaClass)) {
                    doType = (SDOType) entry.getValue();
                }
            }
        }

        if (doType == null) {
            doType = getDoType(javaClass);
        }

        return doType;
    }

    public Class mapDOTypeToJavaClass(DOType doType) {
        Class javaClass = (Class) dOTypeToClassMappings.get(doType.getName());
        if (javaClass == null) {
            String className = doType.getName().replace(PACKAGE_DELIMITER, '.');
            javaClass = ClassUtilities.getClass(className, false, false);
        }
        return javaClass;
    }

    private void setDataObjectAttributeValue(DataObject dataObject, String attributeName, Object attributeValue) throws Exception {
        if (attributeValue == null) return;

        Class attributeValueType = attributeValue.getClass();
        DOType attributeType = dataObject.getType().getAttribute(attributeName).getType();

        if (attributeValue instanceof Double && ((Double) attributeValue).isNaN()) {
            attributeValue = null;

        } else if (attributeValue instanceof Float && ((Float) attributeValue).isNaN()) {
            attributeValue = null;

        } else if (simpleTypes.contains(attributeValueType)) {
            // no modifications to attributeValue

        } else if (attributeType.equals(SDOType.DATE)) {
            attributeValue = getDateHolderForDate((java.util.Date) attributeValue);

        } else if (attributeType.equals(SDOType.DATETIME)) {
            attributeValue = getDateTimeHolderForDate((java.util.Date) attributeValue);

        } else {
            throw new RuntimeException("Invalid attributeValueType:" + attributeValue.getClass().getName());
        }

        dataObject.setAttributeValue(attributeName, attributeValue);
    }


    /**
     * Gets an OpenAdapter DateTimeHolder object for a given Date and TimeZone
     * uses TIMEZONE_NAME as the timeZone.  Therefore all incoming dates are assumed to
     * be in the local timeZone and are converted to the timeZone in <code>TIMEZONE_NAME</code>.
     */
    private static DateTimeHolder getDateTimeHolderForDate(java.util.Date date) throws Exception {
        if (date == null) return null;
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NAME));
        calendar.setTime(date);
        DateTimeHolder dateTimeHolder = new DateTimeHolder(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND),
                TIMEZONE_NAME);
        return dateTimeHolder;
    }

    /**
     * Gets an OpenAdapter DateHolder object for a given Date and TimeZone
     * uses TIMEZONE_NAME as the timeZone.  Therefore all incoming dates are assumed to
     * be in the local timeZone and are converted to the timeZone in <code>TIMEZONE_NAME</code>.
     */
    private static DateHolder getDateHolderForDate(java.util.Date date) throws Exception {
        if (date == null) return null;
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NAME));
        calendar.setTime(date);
        DateHolder dateHolder = new DateHolder(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                TIMEZONE_NAME);

        return dateHolder;
    }

    protected static java.sql.Date getDate(DateHolder dateHolder) {
        if (dateHolder == null) return null;
        Calendar calendar = Calendar.getInstance(dateHolder.getTimeZone());
        calendar.set(dateHolder.getTrueYear(),
                dateHolder.getMonth(),
                dateHolder.getDate(),
                0,
                0,
                0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new java.sql.Date(calendar.getTime().getTime());
    }

    protected static java.util.Date getDateTime(DateTimeHolder dateTimeHolder) {
        if (dateTimeHolder == null) return null;
        Calendar calendar = Calendar.getInstance(dateTimeHolder.getTimeZone());
        calendar.set(dateTimeHolder.getTrueYear(),
                dateTimeHolder.getMonth(),
                dateTimeHolder.getDate(),
                dateTimeHolder.getHours(),
                dateTimeHolder.getMinutes(),
                dateTimeHolder.getSeconds());
        calendar.set(Calendar.MILLISECOND, dateTimeHolder.getMilliseconds());
        return calendar.getTime();
    }
}