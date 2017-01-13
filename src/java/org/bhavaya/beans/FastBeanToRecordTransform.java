package org.bhavaya.beans;

import org.bhavaya.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class supercedes Transform.BEAN_TO_RECORD as a much more efficient way of converting bean back into
 * db-equivalent records.  It does this without touching the database.  This class demands a bit more work but in
 * return eliminates a trip to the database.
 *
 */
public class FastBeanToRecordTransform implements Transform {
    private Class<?> beanType;
    private PropertyExtractor[] propertyExtractors;
    private Attribute[] attributes;

    public static interface PropertyExtractor {
        Attribute createAttribute(Class<?> type);
        void extractAndApplyProperty(Object record, Object bean);
    }

    public static class DefaultPropertyExtractor implements PropertyExtractor {
        private String propertyName;
        private String columnName;

        public DefaultPropertyExtractor(String propertyName, String columnName) {
            this.propertyName = propertyName;
            this.columnName = columnName;
        }

        public Attribute createAttribute(Class<?> type) {
            Attribute attributeFromProperty = Generic.getAttribute(type, Generic.beanPathStringToArray(propertyName));
            return new DefaultAttribute(columnName, attributeFromProperty.getType());
        }

        public void extractAndApplyProperty(Object record, Object bean) {
            Generic.set(record, getColumnName(), Generic.get(bean, Generic.beanPathStringToArray(getPropertyName())));
        }

        public String getColumnName() {
            return columnName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

    public FastBeanToRecordTransform(Class<?> beanType, PropertyExtractor[] propertyColumnRelations) {
        this.beanType = beanType;
        this.propertyExtractors = propertyColumnRelations;
        this.attributes = createAttributes();
    }

    private Attribute[] createAttributes() {
        List<Attribute> attributeList = new ArrayList<Attribute>(propertyExtractors.length);
        for (PropertyExtractor defaultPropertyExtractor : propertyExtractors) {
            attributeList.add(defaultPropertyExtractor.createAttribute(beanType));
        }
        return attributeList.toArray(new Attribute[attributeList.size()]);
    }

    public Object execute(Object sourceData) {
        if (sourceData == null) return null;
        Object record = Generic.getType(attributes).newInstance();
        for (PropertyExtractor defaultPropertyExtractor : propertyExtractors) {
            defaultPropertyExtractor.extractAndApplyProperty(record, sourceData);
        }
        return record;
    }
}
