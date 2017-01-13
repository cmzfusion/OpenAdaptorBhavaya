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

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.ui.adaptor.Source;
import org.bhavaya.ui.adaptor.ValueSource;

import java.util.Date;
import java.util.List;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.9 $
 */

public interface Transform<S, T> {

    public T execute(S sourceData);

    public static final Transform<Object, Object> IDENTITY_TRANSFORM = new Transform() {
        public Object execute(Object sourceData) {
            return sourceData;
        }
    };
    public static final Transform<Quantity, Double> QUANTITY_TO_DOUBLE = new Transform<Quantity, Double>() {
        public Double execute(Quantity sourceData) {
            if (sourceData == null) return Utilities.DOUBLE_NAN;
            return sourceData.getAmount();
        }
    };
    public static final Transform<Date, java.sql.Date> DATE_TO_SQL_DATE = new Transform<Date, java.sql.Date>() {
        public java.sql.Date execute(Date sourceData) {
            if (sourceData == null) return null;
            return DateUtilities.newDate(sourceData);
        }
    };

    public static final Transform<ScalableNumber, Double> SCALABLE_NUMBER_TO_DOUBLE = new Transform<ScalableNumber, Double>() {
        public Double execute(ScalableNumber sourceData) {
            if (sourceData == null) return Utilities.DOUBLE_NAN;
            return sourceData.getScaledAmount();
        }
    };

    public static final Transform<Number, ScalableNumber> DOUBLE_TO_PERCENTAGE_SCALABLE_NUMBER = new Transform<Number, ScalableNumber>() {
        public ScalableNumber execute(Number sourceData) {
            if (sourceData == null) return null;
            double doubleValue = sourceData.doubleValue();
            if (Double.isNaN(doubleValue)) return null;
            return new ScalableNumber(doubleValue / ScalableNumber.PERCENTAGE, ScalableNumber.PERCENTAGE);
        }
    };

    public static final Transform<Number, ScalableNumber> DOUBLE_TO_INVERSE_PERCENTAGE_SCALABLE_NUMBER = new Transform<Number, ScalableNumber>() {
        public ScalableNumber execute(Number sourceData) {
            if (sourceData == null) return null;
            double doubleValue = sourceData.doubleValue();
            if (Double.isNaN(doubleValue)) return null;
            return new ScalableNumber(doubleValue / ScalableNumber.INVERSE_PERCENTAGE, ScalableNumber.INVERSE_PERCENTAGE);
        }
    };

    public static final Transform<Number, Float> NUMBER_TO_FLOAT = new Transform<Number, Float>() {
        public Float execute(Number sourceData) {
            if (sourceData == null) return null;
            return sourceData.floatValue();
        }
    };

    public static class ScaledDoubleToUnscaledDoubleTransform implements Transform<Double, Double> {
        private double scale;

        public ScaledDoubleToUnscaledDoubleTransform(double scale) {
            this.scale = scale;
        }

        public Double execute(Double sourceData) {
            if (sourceData == null) return Utilities.DOUBLE_NAN;
            return sourceData / scale;
        }
    }

    public static class UnscaledDoubleToScaledDoubleTransform implements Transform<Double, Double> {
        private double scale;

        public UnscaledDoubleToScaledDoubleTransform(double scale) {
            this.scale = scale;
        }

        public Double execute(Double sourceData) {
            if (sourceData == null) return Utilities.DOUBLE_NAN;
            return sourceData * scale;
        }
    }

    public static final Transform<Number, Integer> NUMBER_TO_INTEGER = new Transform<Number, Integer>() {
        public Integer execute(Number sourceData) {
            if (sourceData == null) return null;
            return sourceData.intValue();
        }
    };

    public static final Transform<Number, Double> NUMBER_TO_DOUBLE = new Transform<Number, Double>() {
        public Double execute(Number sourceData) {
            if (sourceData == null) return null;
            return sourceData.doubleValue();
        }
    };

    public static final Transform<Object, String> OBJECT_TO_STRING = new Transform<Object, String>() {
        public String execute(Object sourceData) {
            if (sourceData == null) return null;
            return sourceData.toString();
        }
    };

    public static final Transform<Object, String> EMPTY_STRING_TO_NULL = new Transform<Object, String>() {
        public String execute(Object sourceData) {
            if (sourceData == null) return null;

            if (sourceData instanceof String) {
                String s = (String) sourceData;
                if (s.equals("")) return null;
            }

            return sourceData.toString();
        }
    };

    public static final Transform<Boolean, Boolean> BOOLEAN_NOT = new Transform<Boolean, Boolean>() {
        public Boolean execute(Boolean sourceData) {
            return !sourceData;
        }
    };

    public static class RecordToBean implements Transform {
        private Class<?> type;

        public RecordToBean(Class<?> type) {
            this.type = type;
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            Schema schema = Schema.getInstance(type);
            Column[] primaryKey = schema.getPrimaryKey();
            String[] primaryKeyNames = Column.columnsToNames(primaryKey);
            Object key = Utilities.createKey(primaryKeyNames, sourceData);
            key = schema.changeKeyType(key, null);
            return BeanFactory.getInstance(type).get(key);
        }
    }

    public static class BeanToRecord implements Transform {
        private String sql;
        private String dataSourceName;

        public BeanToRecord(String dataSourceName, String sql) {
            this.dataSourceName = dataSourceName;
            this.sql = ApplicationProperties.substituteApplicationProperties(sql);
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            Object key = BeanFactory.getKeyForBean(sourceData);
            String recordSql = SQLFormatter.getInstance(dataSourceName).replace(sql, key);
            List<?> records = DBUtilities.execute(dataSourceName, recordSql);
            if (records.size() == 0) return null;
            return records.get(0); // if multiple records returned ignore them
        }
    }

    public static class KeyToBean implements Transform {
        private Class<?> beanType;
        private String dataSourceName;

        public KeyToBean(Class<?> beanType, String dataSourceName) {
            this.beanType = beanType;
            this.dataSourceName = dataSourceName;
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            return BeanFactory.getInstance(beanType, dataSourceName).get(sourceData);
        }
    }

    public static class BeanToKey implements Transform {
        private Class<?> beanType;
        private String dataSourceName;

        public BeanToKey(Class<?> beanType, String dataSourceName) {
            this.beanType = beanType;
            this.dataSourceName = dataSourceName;
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            return BeanFactory.getInstance(beanType, dataSourceName).getKeyForValue(sourceData);
        }
    }

    public static class KeyToRecord implements Transform {
        private String sql;
        private String dataSourceName;

        public KeyToRecord(String dataSourceName, String sql) {
            this.dataSourceName = dataSourceName;
            this.sql = ApplicationProperties.substituteApplicationProperties(sql);
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;

            String recordSql;
            if (sourceData instanceof List) {
                List<?> compoundKey = (List<?>) sourceData;
                Object[] components = compoundKey.toArray();
                recordSql = SQLFormatter.getInstance(dataSourceName).replace(sql, components).toString();
            } else {
                recordSql = SQLFormatter.getInstance(dataSourceName).replace(sql, sourceData);
            }
            List<?> records = DBUtilities.execute(dataSourceName, recordSql);
            if (records.size() == 0) return null;
            return records.get(0); // if multiple records returned ignore them
        }
    }

    public static class RecordToKey implements Transform {
        private String[] keyColumns;

        public RecordToKey(Column[] keyColumns) {
            this.keyColumns = Column.columnsToNames(keyColumns);
        }

        public RecordToKey(String[] keyColumns) {
            this.keyColumns = keyColumns;
        }

        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            return Utilities.createKey(keyColumns, sourceData);
        }
    }

    public static class QuantityTransform implements Transform<Number, Quantity> {
        private Source unitSource;
        private Source rateDateSource;
        private Transform<Number, Double> toDoubleTransform;

        public QuantityTransform(String unit, Date rateDate) {
            this(NUMBER_TO_DOUBLE, new ValueSource(unit), new ValueSource(rateDate));
        }

        public QuantityTransform(Transform<Number, Double> toDoubleTransform, Source unitSource, Source rateDateSource) {
            if (toDoubleTransform == null) {
                this.toDoubleTransform = NUMBER_TO_DOUBLE;
            }else{
                this.toDoubleTransform = toDoubleTransform;
            }
            this.unitSource = unitSource;
            this.rateDateSource = rateDateSource;
        }

        public Quantity execute(Number sourceData) {
            Double value = toDoubleTransform.execute(sourceData);
            if (value == null) return null;

            Object unit = unitSource.getData();
            String unitString = unit != null ? unit.toString() : null;

            return new Quantity(value, unitString, (Date) rateDateSource.getData());
        }
    }

    public static class AbsDoubleTransform implements Transform<Number, Double> {
        public Double execute(Number sourceData) {
            if (sourceData == null) return null;
            return Math.abs(sourceData.doubleValue());
        }

        public String toString() {
            return "ABS";
        }
    }

    public static class AbsQuantityTransform implements Transform {
        public Object execute(Object sourceData) {
            if (sourceData instanceof Quantity) {
                Quantity quantity = (Quantity) sourceData;
                if (quantity.getAmount() < 0) {
                    return new Quantity(quantity.getAmount() * -1, quantity.getUnit(), quantity.getRateDate());
                } else {
                    return quantity;
                }
            } else {
                return sourceData;
            }
        }

        public String toString() {
            return "ABS";
        }
    }

    public static class NegateDoubleTransform implements Transform<Number, Double> {
        public Double execute(Number sourceData) {
            if (sourceData == null) return null;
            return -1.0 * sourceData.doubleValue();
        }

        public String toString() {
            return "Negate";
        }
    }

    public static class NegateQuantityTransform implements Transform<Quantity, Quantity> {
        public Quantity execute(Quantity sourceData) {
            if (sourceData == null) return null;
            return new Quantity(sourceData.getAmount() * -1, sourceData.getUnit(), sourceData.getRateDate());
        }

        public String toString() {
            return "Negate";
        }
    }

}
