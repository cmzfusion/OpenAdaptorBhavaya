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

import java.util.ArrayList;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public abstract class Property {
    public static final String ONE = "ONE";
    public static final String MANY = "MANY";

    private String name;
    private Class type;
    private String typeName;
    private Class generatedClass;
    private Class parentType;
    private String parentTypeName;
    private Class parentGeneratedClass;
    private String cardinality = ONE;
    private List propertyValueTransforms;

    public abstract Column[] getColumns();

    public abstract Object getColumnValue(Object bean, Column column);

    public Property(String parentTypeName, String name, String typeName, String cardinality) {
        this.name = name;
        this.typeName = typeName;
        this.parentTypeName = parentTypeName;
        if (cardinality == null || (!cardinality.equalsIgnoreCase(ONE) && !cardinality.equalsIgnoreCase(MANY))) throw new IllegalArgumentException("Invalid cardinality: " + cardinality + " for property: " + parentTypeName + "." + name);
        this.cardinality = cardinality.toUpperCase();
    }

    public boolean isValid() {
        return true;
    }


    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public Class getType() {
        if (type == null) {
            // prevent any class load order effects while loading schemas
            type = ClassUtilities.getClass(typeName, true, false);
        }
        return type;
    }

    public Class getGeneratedClass() {
        if (generatedClass == null) {
            this.generatedClass = Schema.mapTypeToGeneratedClass(getType());
        }
        return generatedClass;
    }

    public String getParentTypeName() {
        return parentTypeName;
    }

    public Class getParentType() {
        if (parentType == null) {
            // prevent any class load order effects while loading schemas
            parentType = ClassUtilities.getClass(parentTypeName, true, false);
        }
        return parentType;
    }

    public Class getParentGeneratedClass() {
        if (parentGeneratedClass == null) {
            this.parentGeneratedClass = Schema.getInstance(getParentType()).getGeneratedClass();
        }
        return parentGeneratedClass;
    }

    public String getCardinality() {
        return cardinality;
    }

    public Object getValue(Object bean) {
        if (bean == null) return null;
        return Generic.get(bean, name);
    }

    public boolean isLazy() {
        return false;
    }

    public void addPropertyValueTransform(PropertyValueTransform transform) {
        if (propertyValueTransforms == null) propertyValueTransforms = new ArrayList();
        propertyValueTransforms.add(transform);
    }

    public void removePropertyValueTransform(PropertyValueTransform transform) {
        if (propertyValueTransforms == null) propertyValueTransforms = new ArrayList();
        propertyValueTransforms.add(transform);
    }

    public Object applyPropertyValueTransform(Object parent, Object propertyValue) {
        if (propertyValueTransforms == null) return propertyValue;

        for (int i = 0; i < propertyValueTransforms.size(); i++) {
            PropertyValueTransform transform = (PropertyValueTransform) propertyValueTransforms.get(i);
            propertyValue = transform.execute(this, parent, propertyValue);
        }

        return propertyValue;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Property)) return false;

        final Property property = (Property) o;

        if (!cardinality.equals(property.cardinality)) return false;
        if (!name.equals(property.name)) return false;
        if (!parentTypeName.equals(property.parentTypeName)) return false;
        if (!typeName.equals(property.typeName)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 29 * result + typeName.hashCode();
        result = 29 * result + parentTypeName.hashCode();
        result = 29 * result + cardinality.hashCode();
        return result;
    }

    public String toString() {
        return getParentTypeName() + "." + getName();
    }

    public interface PropertyValueTransform {
        public Object execute(Property property, Object parent, Object propertyValue);
    }
}
