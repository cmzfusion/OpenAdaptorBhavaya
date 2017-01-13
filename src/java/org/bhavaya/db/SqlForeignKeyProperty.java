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

package org.bhavaya.db;

import org.bhavaya.beans.*;
import org.bhavaya.util.LoadClosure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.13 $
 */
public class SqlForeignKeyProperty extends ForeignKeyProperty {
    public static final String SELECT_STATEMENT_PROPERTY = "selectStatementProperty";
    private Join[] joins;
    private Join[] reverseJoins;
    private boolean calculated;
    private int maximumJoinTableCount;

    public SqlForeignKeyProperty(String parentTypeName, String name, String typeName, String cardinality, String foreignIndex, boolean lazy, int maximumJoinTableCount) {
        super(parentTypeName, name, typeName, cardinality, foreignIndex, lazy);
        this.maximumJoinTableCount = maximumJoinTableCount;
    }

    public void initialiseSetPropertyState(Map state) {
        super.initialiseSetPropertyState(state);
        BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");
        if (beanFactory instanceof SqlBeanFactory) {
            SQL propertySql = Schema.getInstance(getGeneratedClass()).getSql();
            SQL specificSql = (SQL) state.get(SELECT_STATEMENT_PROPERTY);
            if (specificSql != null) {
                SQL selectStatementForRun = propertySql.joinStatement(specificSql, getJoins(), null, null, true);
                state.put("selectStatementForRun", selectStatementForRun);
                state.put("parentSQL", specificSql);
                state.put("joinsToParent", getJoins());
            }
        }
    }

    protected Object getPropertyValue(Object propertyKey, Map state) {
        BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");
        if (!(beanFactory instanceof SqlBeanFactory)) return super.getPropertyValue(propertyKey, state);

        SQL selectStatementForRun = (SQL) state.get("selectStatementForRun");
        SQL parentSQL = (SQL) state.get("parentSQL");
        Join[] joinsToParent = (Join[]) state.get("joinsToParent");
        return ((SqlBeanFactory) beanFactory).get(propertyKey, getForeignIndex(), selectStatementForRun, parentSQL, joinsToParent);
    }

    protected LoadClosure createLoad(Object propertyKey, Map state) {
        BeanFactory beanFactory = (BeanFactory) state.get("beanFactoryForRun");
        if (!(beanFactory instanceof SqlBeanFactory)) return super.createLoad(propertyKey, state);

        LoadGroup loadGroup = (LoadGroup) state.get("loadGroupForRun");
        SQL selectStatementForRun = (SQL) state.get("selectStatementForRun");
        SQL parentSQL = (SQL) state.get("parentSQL");
        Join[] joinsToParent = (Join[]) state.get("joinsToParent");
        return new SqlBeanFactoryLoad((SqlBeanFactory) beanFactory, propertyKey, getForeignIndex(), loadGroup, selectStatementForRun, parentSQL, joinsToParent, this);
    }

    public Join[] getJoins() {
        calculate();
        // copying is essential as the caller may mutate the joins, e.g. by aliasing them
        Join[] copy = (Join[]) joins.clone();
        return copy;
    }

    public Join[] getReverseJoins() {
        calculate();
        // copying is essential as the caller may mutate the joins, e.g. by aliasing them
        Join[] copy = (Join[]) reverseJoins.clone();
        return copy;
    }

    private synchronized void calculate() {
        if (calculated) return;
        calculated = true;

        Schema foreignSchema = Schema.getInstance(getTypeName());

        Column[] foreignColumns;
        if (getForeignIndex() == null) {
            foreignColumns = foreignSchema.getPrimaryKey();
        } else {
            foreignColumns = foreignSchema.getIndex(getForeignIndex()).getColumns();
        }

        List joinsList = new ArrayList(foreignColumns.length);
        List reverseJoinsList = new ArrayList(foreignColumns.length);

        for (int i = 0; i < foreignColumns.length; i++) {
            TableColumn foreignColumn = (TableColumn) foreignColumns[i];
            CatalogSchemaTable foreignTable = foreignColumn.getCatalogSchemaTable();

            DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) parameterList.get(i);

            if (parameter instanceof DerivedProperty.ColumnParameter) {
                DerivedProperty.ColumnParameter columnParameter = (DerivedProperty.ColumnParameter) parameter;
                TableColumn column = (TableColumn) columnParameter.getColumn();
                CatalogSchemaTable table = column.getCatalogSchemaTable();
                joinsList.add(new TableToTableJoin(table, column.getName(), foreignTable, foreignColumn.getName()));
                reverseJoinsList.add(new ReverseTableToTableJoin(table, column.getName(), foreignTable, foreignColumn.getName()));
            } else if (parameter instanceof DerivedProperty.DefaultValueParameter) {
                DerivedProperty.DefaultValueParameter defaultValueParameter = (DerivedProperty.DefaultValueParameter) parameter;
                joinsList.add(new DefaultValueToTableJoin(defaultValueParameter, foreignTable, foreignColumn.getName(), foreignSchema.getDefaultDataSourceName()));
                reverseJoinsList.add(new ReverseDefaultValueToTableJoin(defaultValueParameter, foreignTable, foreignColumn.getName(), foreignSchema.getDefaultDataSourceName()));
            }
        }

        joins = (Join[]) joinsList.toArray(new Join[joinsList.size()]);
        reverseJoins = (Join[]) reverseJoinsList.toArray(new Join[reverseJoinsList.size()]);
    }


    private static class SqlBeanFactoryLoad extends WrapperBeanFactoryLoad {
        private SQL selectStatement;
        private SQL parentSQL;
        private Join[] joinsToParent;


        SqlBeanFactoryLoad(SqlBeanFactory beanFactory, Object propertyKey, String index, LoadGroup loadGroup, SQL selectStatement, SQL parentSQL, Join[] joinsToParent, SqlForeignKeyProperty foreignKeyProperty) {
            super(beanFactory, propertyKey, index, loadGroup, foreignKeyProperty);
            if (loadGroup == null) throw new IllegalArgumentException("loadGroup cannot be null");
            this.selectStatement = selectStatement;
            this.parentSQL = parentSQL;
            this.joinsToParent = joinsToParent;
        }

        protected Object get() {
            Object propertyValue;
            int maximumJoinTableCount = ((SqlForeignKeyProperty)foreignKeyProperty).maximumJoinTableCount;
            if (selectStatement != null && (maximumJoinTableCount < 0 || selectStatement.getTables().length <= maximumJoinTableCount)) {
                propertyValue = ((SqlBeanFactory) beanFactory).get(key, index, selectStatement, parentSQL, joinsToParent);
            } else {
                propertyValue = beanFactory.get(key, index);
            }
            propertyValue = foreignKeyProperty.applyPropertyValueTransform(null, propertyValue);
            return propertyValue;
        }

        protected void reset() {
            super.reset();
            selectStatement = null;
            parentSQL = null;
            joinsToParent = null;
        }
    }
}