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
import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.SQL;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class OrCriterion implements SqlCriterion {
    private BasicCriterion[] criteria;

    static {
        BeanUtilities.addPersistenceDelegate(OrCriterion.class, new BhavayaPersistenceDelegate(new String[]{"criteria"}));
    }


    public OrCriterion(BasicCriterion[] criteria) {
        this.criteria = criteria;
    }

    public String getName() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < criteria.length; i++) {
            if (i > 0) buffer.append("-");
            BasicCriterion criterion = criteria[i];
            buffer.append(criterion.getName());
        }
        return buffer.toString();
    }

    public BasicCriterion[] getCriteria() {
        return criteria;
    }

    public boolean evaluate(Object bean) {
        for (int i = 0; i < criteria.length; i++) {
            Criterion criterion = criteria[i];
            if (criterion.evaluate(bean)) return true;
        }
        return false;
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < criteria.length; i++) {
            if (i > 0) buffer.append(" OR ");
            Criterion criterion = criteria[i];
            buffer.append(criterion.getDescription());
        }
        return buffer.toString();
    }

    public boolean isValidForBeanType(Class beanType) {
        for (int i = 0; i < criteria.length; i++) {
            BasicCriterion criterion = criteria[i];
            if (!criterion.isValidForBeanType(beanType)) return false;
        }
        return true;
    }


    public SQL joinSql(Class beanType, SQL sql) {
        for (int i = 0; i < criteria.length; i++) {
            BasicCriterion criterion = criteria[i];
            sql = criterion.joinBeanPaths(beanType, sql);
        }

        StringBuffer whereClauseBuffer = new StringBuffer();
        Set additionalTablesSet = new HashSet();
        int whereClausesAdded = 0;
        for (int i = 0; i < criteria.length; i++) {
            BasicCriterion criterion = criteria[i];
            BasicCriterion.WhereClauseAndAdditionalTables whereClauseAndAdditionalTables = criterion.getWhereClauseAndAdditionalTables(beanType);
            if (whereClauseAndAdditionalTables != null) {
                if (whereClausesAdded > 0) whereClauseBuffer.append(" OR ");

                whereClauseBuffer.append("(").append(whereClauseAndAdditionalTables.getWhereClause()).append(")");
                whereClausesAdded++;
                CatalogSchemaTable[] tables = whereClauseAndAdditionalTables.getTables();
                if (tables != null) {
                    for (int j = 0; j < tables.length; j++) {
                        CatalogSchemaTable table = tables[j];
                        additionalTablesSet.add(table);
                    }
                }
            }
        }

        String whereClause = whereClauseBuffer.toString();
        CatalogSchemaTable[] tables = (CatalogSchemaTable[]) additionalTablesSet.toArray(new CatalogSchemaTable[additionalTablesSet.size()]);

        sql = sql.joinWhereClause(whereClause, tables);
        return sql;
    }

    public Column[] getDirectLeftOperandColumns(Class beanType) {
        Set columns = new HashSet();
        for (int i = 0; i < criteria.length; i++) {
            SqlCriterion criterion = criteria[i];
            Column[] columnsForCriterion = criterion.getDirectLeftOperandColumns(beanType);
            if (columnsForCriterion != null) {
                for (int j = 0; j < columnsForCriterion.length; j++) {
                    Column column = columnsForCriterion[j];
                    columns.add(column);
                }
            }
        }
        return (Column[]) columns.toArray(new Column[columns.size()]);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrCriterion)) return false;

        final OrCriterion orCriterion = (OrCriterion) o;

        if (!Arrays.equals(criteria, orCriterion.criteria)) return false;

        return true;
    }

    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < criteria.length; i++) {
            hashCode = 31 * hashCode + (criteria[i] == null ? 0 : criteria[i].hashCode());
        }
        return hashCode;
    }

    public String toString() {
        String text = "";
        if (criteria.length != 0) {
            text = criteria[0].getName();
        }
        return text;
    }
}
