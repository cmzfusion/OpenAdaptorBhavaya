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

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Type;
import org.bhavaya.util.Utilities;

import java.util.*;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public abstract class DerivedProperty extends org.bhavaya.beans.Property {
    /**
     * Maintains a list of the parameters that form the Property.
     * The order is important, as that is the order the arguments are passed into the constructor when
     * creating an instance of objectType.
     */
    protected List parameterList = new ArrayList();
    protected Map columnParametersByColumn = new LinkedHashMap();
    private Column[] columns;
    private boolean lazy;

    public abstract void initialiseSetPropertyState(Map state);

    public abstract void setPropertyValue(Object bean, Object cachedColumnValuesForBean, Map state);

    public DerivedProperty(String parentTypeName, String name, String typeName, String cardinality, boolean lazy) {
        super(parentTypeName, name, typeName, cardinality);
        this.lazy = lazy;
    }

    public Column[] getColumns() {
        if (columns == null) {
            columns = new Column[columnParametersByColumn.size()];
            int i = 0;
            for (Iterator iterator = columnParametersByColumn.values().iterator(); iterator.hasNext();) {
                ColumnParameter columnParameter = (ColumnParameter) iterator.next();
                columns[i] = columnParameter.getColumn();
                i++;
            }
        }
        return columns;
    }

    public void addParameter(Parameter parameter) {
        if (parameter instanceof ColumnParameter) {
            ColumnParameter columnParameter = (ColumnParameter) parameter;
            columnParametersByColumn.put(columnParameter.getColumn(), columnParameter);
        }
        parameterList.add(parameter);
    }

    protected List getParameterList() {
        return parameterList;
    }

    public Object getColumnValue(Object bean, Column column) {
        String[] beanPath = getBeanPathForColumn(column);
        if (beanPath != null) {
            return Generic.get(bean, beanPath);
        } else {
            return null;
        }
    }

    protected Class getColumnType(Column column) {
        return null;
    }

    private String[] getBeanPathForColumn(Column column) {
        ColumnParameter columnParameter = (ColumnParameter) columnParametersByColumn.get(column);
        if (columnParameter == null) return null;
        return columnParameter.getBeanPath();
    }

    protected Object getArgument(Parameter parameter, Object bean, Object cachedColumnValuesForBean) {
        Object argument;

        if (parameter instanceof PropertyParameter) {
            PropertyParameter propertyParameter = (PropertyParameter) parameter;
            //if (log.isDebug()) log.debug("Find argument from propertyParameter: " + propertyParameter.getName());
            argument = propertyParameter.getValue(bean);
        } else if (parameter instanceof ColumnParameter) {
            ColumnParameter columnParameter = (ColumnParameter) parameter;
            Type columnsType = Schema.getInstance(bean.getClass()).getDerivedPropertyColumnsType();
            argument = columnParameter.getValue(columnsType, cachedColumnValuesForBean);
        } else if (parameter instanceof DefaultValueParameter) {
            DefaultValueParameter defaultValueParameter = (DefaultValueParameter) parameter;
            argument = defaultValueParameter.getValue();
        } else {
            throw new RuntimeException("Invalid parameterType: " + parameter.getClass());
        }

        return argument;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DerivedProperty)) return false;
        DerivedProperty other = (DerivedProperty) obj;
        return super.equals(obj) && Utilities.equals(parameterList, other.parameterList);
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + (parameterList == null ? 0 : parameterList.hashCode());
        return hashCode;
    }

    public boolean isLazy() {
        return lazy;
    }

    protected static interface Parameter {
        public Class getType();
    }

    protected static class PropertyParameter implements Parameter {
        private String name;
        private String typeName;
        private Class type;
        private String[] beanPath;

        public PropertyParameter(String name, String typeName) {
            this.name = name;
            this.typeName = typeName;
            this.beanPath = name.split("\\.");
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            if (type == null) {
                // prevent any class load order effects while loading schemas
                type = ClassUtilities.getClass(typeName, true, false);
            }
            return type;
        }

        public String getTypeName() {
            return typeName;
        }

        public String[] getBeanPath() {
            return beanPath;
        }

        public Object getValue(Object bean) {
            Object argument;
            if (beanPath.length == 1) {
                argument = Generic.get(bean, beanPath[0]);
            } else {
                argument = Generic.get(bean, beanPath);
            }
            if (argument != null && getType() != null) {
                argument = Utilities.changeType(getType(), argument);
            }
            return argument;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PropertyParameter)) return false;
            PropertyParameter other = (PropertyParameter) obj;
            return Utilities.equals(name, other.name) && Utilities.equals(typeName, other.typeName);
        }

        public int hashCode() {
            return (name + typeName).hashCode();
        }
    }

    public static class ColumnParameter implements Parameter, Comparable {
        private Column column;
        private String typeName;
        private Class type;
        private String beanPathString;
        private String[] beanPath;

        public ColumnParameter(Column column) {
            this.column = column;
        }

        public ColumnParameter(Column column, String source, String typeName) {
            this.column = column;
            this.typeName = typeName;
            this.beanPathString = source;
            this.beanPath = source.split("\\.");
        }

        public Column getColumn() {
            return column;
        }

        public String getTypeName() {
            return typeName;
        }

        public Class getType() {
            if (type == null && typeName != null) {
                // prevent any class load order effects while loading schemas
                type = ClassUtilities.getClass(typeName, true, false);
            }
            return type;
        }

        public String[] getBeanPath() {
            return beanPath;
        }

        public String getBeanPathString() {
            return beanPathString;
        }

        public Object getValue(Type columnsType, Object cachedColumnValuesForBean) {
            //if (log.isDebug()) log.debug("Find argument from column: " + columnName);
            Object argument = columnsType.get(cachedColumnValuesForBean, column.getName());
            if (argument != null && getType() != null) {
                argument = Utilities.changeType(getType(), argument);
            }
            return argument;
        }

        public int compareTo(Object o) {
            ColumnParameter other = (ColumnParameter) o;
            return column.compareTo(other.column);
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ColumnParameter)) return false;

            final ColumnParameter columnParameter = (ColumnParameter) o;

            if (column != null ? !column.equals(columnParameter.column) : columnParameter.column != null) return false;
            if (typeName != null ? !typeName.equals(columnParameter.typeName) : columnParameter.typeName != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (column != null ? column.hashCode() : 0);
            result = 29 * result + (typeName != null ? typeName.hashCode() : 0);
            return result;
        }
    }

    protected static class DefaultValueParameter extends DefaultValue implements Parameter {
        public DefaultValueParameter(String typeName, String valueString) {
            super(typeName, valueString);
        }
    }
}