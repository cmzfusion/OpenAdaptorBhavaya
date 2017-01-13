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

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.SQL;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.util.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Description
 *
 * @author Brendon McLean
 * @author Parwinder Sekhon
 * @version $Revision: 1.21 $
 */
public class BasicCriterion implements SqlCriterion {
    private static final Log log = Log.getCategory(BasicCriterion.class);

    public static final String BASIC = "basic";
    public static final String ENUMERATION = "enumeration";
    public static final String FUNCTION = "function";
    public static final String TREE = "tree";
    public static final String SUBTREE = "subtree";
    public static final String LIST = "list";

    private String operator;
    private Object rightOperand;
    protected BeanPathTransformerGroup transformerGroup;
    protected String id;
    private Object sqlFormatter;

    static {
        BeanUtilities.addPersistenceDelegate(BasicCriterion.class, new BhavayaPersistenceDelegate(new String[]{"id", "operator", "rightOperand"}));
    }

    public BasicCriterion(String id) {
        this.id = id;
        transformerGroup = BeanPathTransformerGroup.getInstance(id);
        PropertyGroup group = CriterionFactory.getCriterionPropertyGroup().getGroup(id);
        if (group == null) {
            log.error("Cannot find properties for id: " + id);
        } else {
            loadProperties(group);
        }
    }

    public BasicCriterion(String id, String operator, Object rightOperand) {
        this(id);
        this.operator = operator;
        this.rightOperand = rightOperand;
    }

    public String getCriterionType() {
        return BasicCriterion.BASIC;
    }

    protected void loadProperties(PropertyGroup properties) {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        checkFail();
        return transformerGroup.getName();
    }

    public Class getToBeanType() {
        checkFail();
        return transformerGroup.getToBeanType();
    }

    public String getOperator() {
        return operator;
    }

    public Object getRightOperand() {
        return rightOperand;
    }

    protected void setRightOperand(Object rightOperand) {
        this.rightOperand = rightOperand;
    }

    public String getDescription() {
        return getName() + " " + getOperator() + " (" + getRightOperand().toString() + ")";
    }

    public boolean isValidForBeanType(Class beanType) {
        checkFail();
        return transformerGroup.isValidForBeanType(beanType);
    }

    public boolean evaluate(Object bean) {
        checkFail();

        Set leftOperandValues = getLeftOperandValues(bean);
        Object rightOperand = getRightOperand();

        for (Iterator iterator = leftOperandValues.iterator(); iterator.hasNext();) {
            Object leftOperandValue = iterator.next();
            if (evaluate(leftOperandValue, rightOperand)) return true;
        }
        return false;
    }

    protected void checkFail() {
        if (transformerGroup == null) throw new IllegalStateException("Unknown id: " + id);
    }

    protected Set getLeftOperandValues(Object bean) {
        checkFail();
        if (bean == null) return null;
        return transformerGroup.getTransformer(bean.getClass()).tranform(bean);
    }

    protected boolean evaluate(Object leftOperandValue, Object rightOperandValue) {
        return evaluate(leftOperandValue, getOperator(), rightOperandValue);
    }

    protected static boolean evaluate(Object leftOperandValue, String operator, Object rightOperandValue) {
        if (leftOperandValue == null) {
            if (operator.equals("=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_IN_OPERATION)) return rightOperandValue == null;
            if (operator.equals("<>") || operator.equals("!=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_NOT_IN_OPERATION)) return rightOperandValue != null;
            return false;

        } else if (Schema.hasInstance(leftOperandValue.getClass())) {
            // RightOperand must be a key
            rightOperandValue = Schema.getInstance(leftOperandValue.getClass()).changeKeyType(rightOperandValue, null);
            Object leftOperandKey = BeanFactory.getKeyForBean(leftOperandValue); // convert bean to key

            if (operator.equals("=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_IN_OPERATION)) return Utilities.equals(leftOperandKey, rightOperandValue);
            if (operator.equals("<>") || operator.equals("!=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_NOT_IN_OPERATION)) return !Utilities.equals(leftOperandKey, rightOperandValue);

            if (leftOperandValue instanceof Comparable) {
                rightOperandValue = BeanFactory.getInstance(leftOperandValue.getClass()).get(rightOperandValue); // convert key to bean
                return evaluate((Comparable) leftOperandValue, operator, (Comparable) rightOperandValue);
            }

            log.error("Cannot evaluate operands of type Bean with operator: " + operator + " the bean may need to implement Comparable");
            return false;

        } else if (rightOperandValue instanceof Number) {
            Double rightOperandNumber = ((Number) rightOperandValue).doubleValue();
            Double leftOperandNumber = getLeftOperandNumber(leftOperandValue);
            return evaluate(leftOperandNumber, operator, rightOperandNumber);
        } else if (leftOperandValue instanceof java.lang.Boolean) {
            Boolean leftOperandBoolean = (Boolean) leftOperandValue;
            Boolean rightOperandBooean = (Boolean) rightOperandValue;
            if (!operator.equals("=")) {
                log.error("Boolean data type only supports '=', not " + operator);
                return false;
            }
            return leftOperandBoolean.booleanValue() == rightOperandBooean.booleanValue();
        } else if (leftOperandValue instanceof java.util.Date) {
            java.util.Date rightOperandDate = getRightOperandDate(rightOperandValue);
            java.util.Date leftOperandDate = (java.util.Date) leftOperandValue;
            return evaluate(leftOperandDate, operator, rightOperandDate);

        } else if (leftOperandValue instanceof Enum) {
            return evaluate((Enum) leftOperandValue, operator, (Enum) rightOperandValue);
        } else {
            String leftOperandString = String.valueOf(leftOperandValue);
            String rightOperandString = String.valueOf(rightOperandValue);
            return evaluate(leftOperandString, operator, rightOperandString);
        }
    }

    private static boolean evaluate(Comparable leftOperand, String operator, Comparable rightOperand) {
        if (operator.equals("=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_IN_OPERATION)) return leftOperand.equals(rightOperand);
        if (operator.equals(">")) return leftOperand.compareTo(rightOperand) > 0;
        if (operator.equals("<")) return leftOperand.compareTo(rightOperand) < 0;
        if (operator.equals("<>") || operator.equals("!=") || operator.equalsIgnoreCase(EnumerationCriterion.ENUM_NOT_IN_OPERATION)) return !leftOperand.equals(rightOperand);
        if (operator.equals(">=")) return leftOperand.equals(rightOperand) || leftOperand.compareTo(rightOperand) > 0;
        if (operator.equals("<=")) return leftOperand.equals(rightOperand) || leftOperand.compareTo(rightOperand) < 0;
        log.error("Cannot evaluate operands of type Comparable with operator: " + operator);
        return false;
    }

    private static java.util.Date getRightOperandDate(Object rightOperandValue) {
        java.util.Date rightOperandDate;
        if (rightOperandValue instanceof DateFunction) {
            rightOperandDate = ((DateFunction) rightOperandValue).getDate();
        } else {
            rightOperandDate = (java.util.Date) rightOperandValue;
        }
        return rightOperandDate;
    }

    private static Double getLeftOperandNumber(Object leftOperandValue) {
        Double leftOperandNumber;
        if (leftOperandValue instanceof Double) {
            leftOperandNumber = (Double) leftOperandValue;
            // leave this case in, as it uses the plain amount not the scaled number
        } else if (leftOperandValue instanceof ScalableNumber) { 
            leftOperandNumber = new Double(((ScalableNumber) leftOperandValue).getAmount());
        } else if (leftOperandValue instanceof Numeric) {
            leftOperandNumber = new Double(((Numeric) leftOperandValue).doubleValue());
        } else {
            leftOperandNumber = new Double(((Number) leftOperandValue).doubleValue());
        }
        return leftOperandNumber;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicCriterion)) return false;

        final BasicCriterion basicCriterion = (BasicCriterion) o;

        if (!getId().equals(basicCriterion.getId())) return false;
        if (operator != null ? !operator.equals(basicCriterion.operator) : basicCriterion.operator != null) return false;
        if (getRightOperand() != null ? !getRightOperand().equals(basicCriterion.getRightOperand()) : basicCriterion.getRightOperand() != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getId().hashCode();
        result = 29 * result + (operator != null ? operator.hashCode() : 0);
        result = 29 * result + (getRightOperand() != null ? getRightOperand().hashCode() : 0);
        return result;
    }

    public String toString() {
        return getName();
    }

    public SQL joinSql(Class beanType, SQL sql) {
        checkFail();

        sql = joinBeanPaths(beanType, sql);
        sql = joinLeftOperandColumnGroups(beanType, sql);
        return sql;
    }

    protected SQL joinBeanPaths(Class beanType, SQL sql) {
        sql = transformerGroup.getTransformer(beanType).joinBeanPaths(sql);
        return sql;
    }

    private SQL joinLeftOperandColumnGroups(Class beanType, SQL sql) {
        WhereClauseAndAdditionalTables whereClauseAndAdditionalTables = getWhereClauseAndAdditionalTables(beanType);
        if (whereClauseAndAdditionalTables != null) {
            sql = sql.joinWhereClause(whereClauseAndAdditionalTables.getWhereClause(), whereClauseAndAdditionalTables.getTables());
        }
        return sql;
    }

    protected WhereClauseAndAdditionalTables getWhereClauseAndAdditionalTables(Class beanType) {
        String whereClause = getWhereClause(beanType);
        return new WhereClauseAndAdditionalTables(whereClause, null);
    }

    public String getWhereClause(Class beanType) {
        Object rightOperand = this.getRightOperand();
        if (rightOperand instanceof DateFunction) rightOperand = ((DateFunction) rightOperand).getDate();
        rightOperand = Utilities.changeType(getToBeanType(), rightOperand);

        FastStringBuffer buffer = new FastStringBuffer();

        Set leftOperandColumns = transformerGroup.getTransformer(beanType).getColumnGroups();
        int x = 0;
        if (leftOperandColumns.size() > 1) buffer.append("(");
        for (Iterator iterator = leftOperandColumns.iterator(); iterator.hasNext();) {
            Column[] columnGroup = (Column[]) iterator.next();
            if (x > 0) buffer.append(" OR ");
            if (columnGroup.length > 1) buffer.append("(");
            joinLeftOperandColumnGroup(beanType, columnGroup, rightOperand, buffer);
            if (columnGroup.length > 1) buffer.append(")");
            x++;
        }
        if (leftOperandColumns.size() > 1) buffer.append(")");
        return buffer.toString();
    }

    protected void joinLeftOperandColumnGroup(Class beanType, Column[] columnGroup, Object rightOperand, FastStringBuffer buffer) {
        for (int i = 0; i < columnGroup.length; i++) {
            if (i > 0) buffer.append(getAndOperator());
            Column column = columnGroup[i];
            Object rightOperandForColumn = rightOperand;
            if (columnGroup.length > 1 && rightOperand instanceof List) {
                List compoundKey = (List) rightOperand;
                rightOperandForColumn = compoundKey.get(i);
            }
            joinLeftOperandColumn(beanType, column, rightOperandForColumn, buffer);
        }
    }

    protected String getAndOperator() {
        return " AND ";
    }

    protected void joinLeftOperandColumn(Class beanType, Column column, Object rightOperandForColumn, FastStringBuffer buffer) {
        if (rightOperandForColumn == null && operator.equals("=")) {
            buffer.append(column.getRepresentation());
            buffer.append(" IS NULL");
        } else if (rightOperandForColumn == null && (operator.equals("<>") || operator.equals("!="))) {
            buffer.append(column.getRepresentation());
            buffer.append(" IS NOT NULL");
        } else {
            buffer.append(column.getRepresentation());
            buffer.append(operator);
            buffer.append(formatRightOperantForColumn(beanType, rightOperandForColumn));
        }
    }

    protected String formatRightOperantForColumn(Class beanType, Object rightOperandForColumn) {
        try {
            Object sqlFormatter = getSQLFormater(beanType);
            Method method = sqlFormatter.getClass().getMethod("format", Object.class);
            return (String) method.invoke(sqlFormatter, rightOperandForColumn);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getSQLFormater(Class beanType) {
        if (sqlFormatter == null) {
            sqlFormatter = SQLFormatter.getInstance(Schema.getInstance(beanType).getDefaultDataSourceName());
        }
        return sqlFormatter;
    }

    public void setSqlFormatter(Object sqlFormatter) {
        this.sqlFormatter = sqlFormatter;
    }

    public Column[] getDirectLeftOperandColumns(Class beanType) {
        checkFail();
        return transformerGroup.getTransformer(beanType).getDirectColumns();
    }

    protected static class WhereClauseAndAdditionalTables {
        private String whereClause;
        private CatalogSchemaTable[] tables;

        public WhereClauseAndAdditionalTables(String whereClause, CatalogSchemaTable[] tables) {
            this.whereClause = whereClause;
            this.tables = tables;
        }

        public String getWhereClause() {
            return whereClause;
        }

        public CatalogSchemaTable[] getTables() {
            return tables;
        }

        public void setWhereClause(String whereClause) {
            this.whereClause = whereClause;
        }

        public void setTables(CatalogSchemaTable[] tables) {
            this.tables = tables;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("" + evaluate(new Double(10), ">", new Double(5)));
            System.out.println("" + evaluate("B", ">", "A"));
            java.util.Date now = new java.util.Date();
            Thread.sleep(10);
            java.util.Date later = new java.util.Date();
            System.out.println("" + evaluate(later, ">", now));
        } catch (InterruptedException e) {
            log.error(e);
        }
    }
}
