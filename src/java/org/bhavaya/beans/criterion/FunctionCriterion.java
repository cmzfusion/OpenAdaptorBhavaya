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

import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.util.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;

/**
 * Description
 *
 * @author Dan Van Enckevort
 * @version $Revision: 1.14 $
 */
public class FunctionCriterion extends BasicCriterion {
    private static final Log log = Log.getCategory(FunctionCriterion.class);

    protected static Map functionMap = new HashMap();
    private static final String FUNCTION_NAME_PROPERTY = "function";

    public static final String ABS = "abs";
    public static final String CONTAINS = "contains";
    public static final String STARTS_WITH = "startsWith";
    public static final String IS_NULL = "isNull";
    public static final String IS_NOT_NULL = "isNotNull";
    public static final String BETWEEN = "between";
    public static final String NOT_BETWEEN = "notBetween";

    static {
        BeanUtilities.addPersistenceDelegate(FunctionCriterion.class, new BhavayaPersistenceDelegate(new String[]{"id", "operator", "rightOperand", "functionName"}));
        functionMap.put(ABS, new AbsFunction());
        functionMap.put(IS_NULL, new IsNullFunction());
        functionMap.put(IS_NOT_NULL, new IsNotNullFunction());
        functionMap.put(CONTAINS, new ContainsFunction());
        functionMap.put(STARTS_WITH, new StartWithFunction());
        functionMap.put(BETWEEN, new BetweenFunction());
        functionMap.put(NOT_BETWEEN, new NotBetweenFunction());
    }

    private Function function;

    public FunctionCriterion(String id, String operator, Object rightOperand) {
        super(id, operator, rightOperand);
    }

    public FunctionCriterion(String id, String operator, Object rightOperand, String functionName) {
        super(id, operator, rightOperand);
        setFunctionName(functionName);
    }

    protected void loadProperties(PropertyGroup properties) {
        super.loadProperties(properties);
        String functionName = properties.getProperty(FUNCTION_NAME_PROPERTY);
        setFunctionName(functionName);
    }

    private void setFunctionName(String functionName) {
        this.function = (Function) functionMap.get(functionName);
        if (functionName != null && function == null) throw new RuntimeException("Invalid function name: " + functionName);
    }

    /**
     * For persistence delegate.
     *
     * @return
     */
    public String getFunctionName() {
        checkFail();
        return function.getFunctionName();
    }

    public Function getFunction() {
        return function;
    }

    public boolean evaluate(Object bean) {
        checkFail();

        Set leftOperandValues = getLeftOperandValues(bean);

        for (Iterator iterator = leftOperandValues.iterator(); iterator.hasNext();) {
            Object leftOperandValue = iterator.next();
            if (function.evaluate(this, leftOperandValue)) return true;
        }

        return false;
    }

    protected void joinLeftOperandColumn(Class beanType, Column column, Object rightOperandForColumn, FastStringBuffer buffer) {
        buffer.append(function.getPrimarySqlClause(beanType, column.getRepresentation(), getOperator(), rightOperandForColumn));
    }

    public Class getToBeanType() {
        return function.getLeftOperandType(super.getToBeanType());
    }

    public String getCriterionType() {
        return BasicCriterion.FUNCTION;
    }

    protected static abstract class Function {
        private String functionName;

        public Function(String functionName) {
            this.functionName = functionName;
        }

        public abstract boolean evaluate(FunctionCriterion criterion, Object leftOperandValue);

        public abstract String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand);

        public Class getLeftOperandType(Class defaultType) {
            return defaultType;
        }

        public String getFunctionName() {
            return functionName;
        }
    }

    private static class AbsFunction extends Function {
        public AbsFunction() {
            super(ABS);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            leftOperandValue = applyFunctionToValue(leftOperandValue);
            return criterion.evaluate(leftOperandValue, criterion.getRightOperand());
        }

        private Object applyFunctionToValue(Object value) {
            double doubleValue;
            if (value instanceof Number) {
                doubleValue = ((Number) value).doubleValue();
            } else if (value instanceof Numeric) {
                doubleValue = ((Numeric) value).doubleValue();
            } else {
                throw new RuntimeException("Could not apply abs to value of type: " + value.getClass());
            }
            if (log.isDebug()) log.debug("Applying abs to value: " + doubleValue);
            return new Double(Math.abs(doubleValue));
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            SQLFormatter sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
            return "ABS(" + leftOperand + ')' + operator + sqlFormatter.format(rightOperand);
        }
    }

    private static class IsNullFunction extends Function {
        public IsNullFunction() {
            super(IS_NULL);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            boolean on = ((Boolean) criterion.getRightOperand()).booleanValue();
            if (on) {
                return leftOperandValue == null;
            } else {
                return leftOperandValue != null;
            }
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            boolean on = ((Boolean) rightOperand).booleanValue();
            if (on) {
                return leftOperand + " IS NULL";
            } else {
                return leftOperand + " IS NOT NULL";
            }
        }

        public Class getLeftOperandType(Class defaultType) {
            return Boolean.class;
        }
    }

    private static class IsNotNullFunction extends Function {
        public IsNotNullFunction() {
            super(IS_NOT_NULL);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            boolean on = ((Boolean) criterion.getRightOperand()).booleanValue();
            if (on) {
                return leftOperandValue != null;
            } else {
                return leftOperandValue == null;
            }
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            boolean on = ((Boolean) rightOperand).booleanValue();
            if (on) {
                return leftOperand + " IS NOT NULL";
            } else {
                return leftOperand + " IS NULL";
            }
        }

        public Class getLeftOperandType(Class defaultType) {
            return Boolean.class;
        }
    }

    private static class ContainsFunction extends Function {
        public ContainsFunction() {
            super(CONTAINS);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            Object rightOperand = criterion.getRightOperand();
            if (leftOperandValue == null) {
                return rightOperand == null;
            } else if (rightOperand == null) {
                return false;
            }
            String rightOperandString = rightOperand.toString();
            String leftOperandString = leftOperandValue.toString();
            int index = leftOperandString.indexOf(rightOperandString);
            return index != -1;
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            SQLFormatter sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
            return leftOperand + " LIKE " + sqlFormatter.format("%" + rightOperand + "%");
        }
    }

    private static class StartWithFunction extends Function {
        public StartWithFunction() {
            super(STARTS_WITH);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            Object rightOperand = criterion.getRightOperand();
            if (leftOperandValue == null) {
                return rightOperand == null;
            } else if (rightOperand == null) {
                return false;
            }
            String rightOperandString = rightOperand.toString();
            String leftOperandString = leftOperandValue.toString();
            return leftOperandString.startsWith(rightOperandString);
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            SQLFormatter sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
            return leftOperand + " LIKE " + sqlFormatter.format(rightOperand + "%");
        }
    }

    private static class BetweenFunction extends Function {
        public BetweenFunction() {
            super(BETWEEN);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            Interval interval = (Interval) criterion.getRightOperand();
            return BasicCriterion.evaluate(leftOperandValue, ">=", interval.getStart()) && BasicCriterion.evaluate(leftOperandValue, "<=", interval.getEnd());
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            Interval interval = (Interval) rightOperand;
            SQLFormatter sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
            return leftOperand + " BETWEEN " + sqlFormatter.format(interval.getStart()) + " AND " + sqlFormatter.format(interval.getEnd());
        }
    }

    private static class NotBetweenFunction extends Function {
        public NotBetweenFunction() {
            super(NOT_BETWEEN);
        }

        public boolean evaluate(FunctionCriterion criterion, Object leftOperandValue) {
            Interval interval = (Interval) criterion.getRightOperand();
            return !(BasicCriterion.evaluate(leftOperandValue, ">=", interval.getStart()) && BasicCriterion.evaluate(leftOperandValue, "<=", interval.getEnd()));
        }

        public String getPrimarySqlClause(Class beanType, String leftOperand, String operator, Object rightOperand) {
            Interval interval = (Interval) rightOperand;
            SQLFormatter sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
            return leftOperand + " NOT BETWEEN " + sqlFormatter.format(interval.getStart()) + " AND " + sqlFormatter.format(interval.getEnd());
        }
    }

}

