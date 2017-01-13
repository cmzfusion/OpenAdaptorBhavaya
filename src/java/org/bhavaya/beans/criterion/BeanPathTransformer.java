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

package org.bhavaya.beans.criterion;

import gnu.trove.THashSet;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.ForeignKeyProperty;
import org.bhavaya.beans.Property;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.SQL;
import org.bhavaya.db.SqlForeignKeyProperty;
import org.bhavaya.util.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.16 $
 */
public class BeanPathTransformer {
    private Class fromBeanType;
    private String index;
    private boolean calculatedIndex;
    private String[] beanPaths;
    private Set columnGroups;
    private Column[] directColumns;

    protected BeanPathTransformer(Class fromBeanType, String[] beanPaths) {
        this.fromBeanType = fromBeanType;
        this.beanPaths = beanPaths;
        if (beanPaths == null || beanPaths.length == 0) throw new RuntimeException("Error instantiating transformer: " + this + " beanPath is null");
        assert (isBeanPathsValid()) : "Beans paths not valid in criterion";
    }

    public Class getFromBeanType() {
        return fromBeanType;
    }

    public String getIndex() {
        calculate();
        return index;
    }

    private synchronized void calculate() {
        if (!calculatedIndex) {
            String beanPath = beanPaths[0];
            String[] beanPathArray = Generic.beanPathStringToArray(beanPath);

            if (beanPathArray.length == 0) {
                index = null;
                Schema schema = Schema.getInstance(fromBeanType);
                directColumns = schema.getPrimaryKey();
            } else {
                Class parentClass = fromBeanType;
                String propertyName = beanPathArray[0];
                parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(propertyName);

                if (parentClass == null) throw new RuntimeException("Cannot calculate information for transformer: " + this);
                Schema schema = Schema.getInstance(parentClass);
                Property property = schema.getProperty(propertyName);
                directColumns = property.getColumns();

                for (int j = 0; j < (beanPathArray.length); j++) {
                    schema = Schema.getInstance(parentClass);
                    property = schema.getProperty(beanPathArray[j]);
                    parentClass = property.getType();

                    // this will find the appropriate subclass of parentClass if neccessary and join add sql for subclass
                    if (j < beanPathArray.length - 1) parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(beanPathArray[j + 1]);

                    if (j == (beanPathArray.length - 1) && property instanceof ForeignKeyProperty) {
                        ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
                        index = foreignKeyProperty.getForeignIndex();
                    }
                }
            }
            calculatedIndex = true;
        }
    }

    public String[] getBeanPaths() {
        return beanPaths;
    }

    public Set tranform(Object bean) {
        if (bean == null) return null;

        Set values = new THashSet(4);

        for (int i = 0; i < beanPaths.length; i++) {
            String beanPath = beanPaths[i];
            if (beanPath.length() == 0) {
                transform(bean, values);
            } else {
                String[] path = Generic.beanPathStringToArray(beanPath);
                transform(bean, values, path);
            }
        }
        return values;
    }

    private static void transform(Object beanValue, Set values, String[] path) {
        if (beanValue == null) {
            values.add(null);
            return;
        }

        for (int j = 0; j < path.length; j++) {
            String pathComponent = path[j];
            if (!Generic.getType(beanValue).attributeExists(pathComponent)) return;

            beanValue = Generic.get(beanValue, pathComponent);

            if (beanValue == null) {
                values.add(null);
                return;
            }

            int nextPosition = j + 1;
            if (nextPosition < path.length && beanValue.getClass().isArray()) {
                Object[] array = (Object[]) beanValue;
                for (int i = 0; i < array.length; i++) {
                    Object o = array[i];
                    transform(o, values, (String[]) Utilities.subSection(path, nextPosition, path.length - nextPosition));
                }
                return;
            }
        }

        transform(beanValue, values);
    }

    private static void transform(Object beanValue, Set values) {
        if (beanValue == null) {
            values.add(null);
        } else if (beanValue.getClass().isArray()) {
            Object[] array = (Object[]) beanValue;
            for (int i = 0; i < array.length; i++) {
                Object o = array[i];
                values.add(o);
            }
        } else {
            values.add(beanValue);
        }
    }

    public Set getColumnGroups() {
        if (columnGroups == null) {
            columnGroups = new LinkedHashSet();

            for (int i = 0; i < beanPaths.length; i++) {
                String beanPath = beanPaths[i];
                String[] beanPathArray = Generic.beanPathStringToArray(beanPath);

                if (beanPathArray.length == 0) {
                    Schema schema = Schema.getInstance(fromBeanType);
                    Column[] primaryKey = schema.getPrimaryKey();
                    columnGroups.add(primaryKey);
                } else {
                    Property property = getParentProperty(beanPathArray);

                    Column[] columns;
                    if (getIndex() != null) {
                        columns = Schema.getInstance(property.getType()).getPrimaryKey(); // do not use the indexed columns
                    } else {
                        columns = property.getColumns();
                    }

                    columnGroups.add(columns);
                }
            }
        }
        return columnGroups;
    }

    private Property getParentProperty(String[] beanPathArray) {
        Class parentClass;
        if (beanPathArray.length == 1) {
            parentClass = fromBeanType;
        } else {
            String[] parentBeanPathArray = (String[]) Utilities.subSection(beanPathArray, 0, beanPathArray.length - 1);
            Attribute parentAttribute = PropertyModel.getInstance(fromBeanType).getAttribute(parentBeanPathArray);
            parentClass = parentAttribute.getType();
        }

        String propertyName = beanPathArray[beanPathArray.length - 1];
        if (parentClass.isArray()) parentClass = parentClass.getComponentType();
        parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(propertyName);
        if (parentClass == null) {
            throw new RuntimeException("Cannot determine column groups for transformer: " + this);
        }

        Property property = Schema.getInstance(parentClass).getProperty(propertyName);
        return property;
    }

    public Column[] getDirectColumns() {
        calculate();
        return directColumns;
    }

    public SQL joinBeanPaths(SQL sql) {
        // assumes multiple beanPaths will go through the same tables, but the last columns can still be different for each beanPath
        // therefore, it does not need to iterate over all the beanPaths, it can just use the first
        // this has changed, it is assumed that multiple beanPaths will go through the same "ROOT" tables, but the longest beanPath may
        // go through some extra ones
        String beanPath = null;
        for (int i = 0; i < beanPaths.length; i++) {
            if (beanPath == null || beanPath.length() < beanPaths[i].length()) beanPath = beanPaths[i];
        }
        String[] beanPathArray = Generic.beanPathStringToArray(beanPath);

        if (beanPathArray.length > 0) {
            Class parentClass = PropertyModel.getInstance(fromBeanType).findMatchingSubclass(beanPathArray[0]);
            if (parentClass != PropertyModel.getInstance(fromBeanType).getRealClass()) {
                SQL sqlForSubclass = Schema.getInstance(parentClass).getSql();
                sql = sql.joinStatement(sqlForSubclass, null, null, null, false); // join the subclass sql
            }

            for (int j = 0; j < beanPathArray.length; j++) {
                Property property = Schema.getInstance(parentClass).getProperty(beanPathArray[j]);
                if (property instanceof SqlForeignKeyProperty) {
                    SqlForeignKeyProperty sqlForeignKeyProperty = (SqlForeignKeyProperty) property;
                    parentClass = property.getType();

                    // this will find the appropriate subclass of parentClass if neccessary
                    if (j < (beanPathArray.length - 1)) {
                        final String nextPropertyName = beanPathArray[j + 1];
                        Class oldParentClass = parentClass;
                        parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(nextPropertyName);
                        if (oldParentClass != null && parentClass == null) throw new RuntimeException("Cannot find property: " + oldParentClass.getName() + "." + nextPropertyName);
                    }

                    if (j < (beanPathArray.length - 1) || getIndex() != null) {
                        SQL propertySql = Schema.getInstance(parentClass).getSql();
                        sql = sql.joinStatement(propertySql, sqlForeignKeyProperty.getReverseJoins(), null, null, true);
                    }
                }
            }
        }
        return sql;
    }

    public String toString() {
        return fromBeanType.getName() + "/" + Utilities.asString(beanPaths, "/");
    }

    private boolean isBeanPathsValid() {
        for (int i = 0; i < beanPaths.length; i++) {
            String beanPath = beanPaths[i];
            if (beanPath.length() > 0) {
                Class ownerClass = fromBeanType;
                String[] propertyPath = Generic.beanPathStringToArray(beanPath);
                Attribute attribute = PropertyModel.getInstance(fromBeanType).getAttribute(propertyPath);
                if (attribute == null) {
                    //test to see if one of the properties was an array
                    Class testClass = fromBeanType;
                    for (int j = 0; j < propertyPath.length; j++) {
                        String property = propertyPath[j];
                        attribute = PropertyModel.getInstance(testClass).getAttribute(new String[]{property});
                        if (attribute == null) {
                            Log.getCategory(BeanPathTransformer.class).error("Invalid bean path:"+beanPath+" for class: "+fromBeanType.getName()+" specified in criterion.xml no such property: "+property+" on class: "+testClass);
                            break;
                        }
                        testClass = attribute.getType();
                        if (testClass.isArray()) {
                            testClass = testClass.getComponentType();
                        }
                    }

                }
//                assert (attribute != null) : "Invalid bean path:"+beanPath+" for class: "+ownerClass.getName()+" specified in criterion.xml";
            }
        }
        return true;
    }
}
