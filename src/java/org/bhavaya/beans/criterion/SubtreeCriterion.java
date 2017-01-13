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

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.Property;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.AliasedCatalogSchemaTable;
import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.SQL;
import org.bhavaya.db.SqlForeignKeyProperty;
import org.bhavaya.util.*;

import java.util.*;


/**
 * @author Daniel van Enckevort
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class SubtreeCriterion extends BasicCriterion {
    private Object parentNode;
    private String parentNodeProperty;
    private String childCollectionProperty;
    private String nodeDescriptionProperty;

    static {
        BeanUtilities.addPersistenceDelegate(SubtreeCriterion.class, new BhavayaPersistenceDelegate(new String[]{"id", "rightOperand"}));
    }

    public SubtreeCriterion(String id, Object parentNodeKey) {
        super(id, "child of", parentNodeKey);
        loadProperties(CriterionFactory.getCriterionPropertyGroup().getGroup(id));
    }

    public String getCriterionType() {
        return BasicCriterion.SUBTREE;
    }

    public boolean evaluate(Object bean) {
        checkFail();

        Set leftOperandValues = getLeftOperandValues(bean);

        for (Iterator iterator = leftOperandValues.iterator(); iterator.hasNext();) {
            Object leftOperandValue = iterator.next();

            do {
                if (Utilities.equals(leftOperandValue, getParentNode())) return true;
                if (leftOperandValue != null) leftOperandValue = Generic.get(leftOperandValue, parentNodeProperty);
            } while (leftOperandValue != null);
        }

        return false;
    }

    protected int getMaxLeafDepth() {
        return getMaxLeafDepth(getParentNode());
    }

    private int getMaxLeafDepth(Object node) {
        Collection children = (Collection) Generic.get(node, childCollectionProperty);
        if (children == null || children.size() == 0) {
            //this is a leaf node
            return 0;
        } else {
            int maxDepth = 0;
            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
                Object childNode = iterator.next();
                int childDepth = getMaxLeafDepth(childNode);
                maxDepth = Math.max(maxDepth, childDepth);
            }
            return maxDepth + 1;
        }
    }

    protected SQL joinBeanPaths(Class beanType, SQL sql) {
        sql = super.joinBeanPaths(beanType, sql);

        // also this joins the last property in the beanPath to the nodeTable

        String beanPath = transformerGroup.getTransformer(beanType).getBeanPaths()[0];
        String[] beanPathArray = Generic.beanPathStringToArray(beanPath);

        if (beanPathArray.length > 0) {
            Class parentClass = PropertyModel.getInstance(beanType).findMatchingSubclass(beanPathArray[0]);

            for (int j = 0; j < beanPathArray.length; j++) {
                Property property = Schema.getInstance(parentClass).getProperty(beanPathArray[j]);

                if (property instanceof SqlForeignKeyProperty) {
                    SqlForeignKeyProperty sqlForeignKeyProperty = (SqlForeignKeyProperty) property;
                    parentClass = property.getType();

                    if (j == beanPathArray.length - 1) {
                        SQL propertySql = Schema.getInstance(parentClass).getSql();
                        sql = sql.joinStatement(propertySql, sqlForeignKeyProperty.getReverseJoins(), null, null, true);
                    }
                }
            }
        }
        return sql;
    }

    protected WhereClauseAndAdditionalTables getWhereClauseAndAdditionalTables(Class beanType) {
        StringBuffer buffer = new StringBuffer(2000);
        Schema schema = Schema.getInstance(getToBeanType());
        CatalogSchemaTable nodeTable = schema.getSql().getTables()[0]; // todo: allow multi-table node tables
        Column nodeKeyColumn = schema.getPrimaryKey()[0]; // todo: allows compound keys

        int childDepth = getMaxLeafDepth();
        List joinTables = new ArrayList(childDepth);

        if (childDepth == 0) {
            buffer.append(nodeKeyColumn.getRepresentation()).append("=");
        } else {
            Column parentKeyColumn = schema.getProperty(parentNodeProperty).getColumns()[0]; // todo: allows compound keys

            buffer.append(parentKeyColumn.getRepresentation()).append("=");
            for (int levelAboveParent = 1; levelAboveParent < childDepth; levelAboveParent++) {
                AliasedCatalogSchemaTable alias = new AliasedCatalogSchemaTable(nodeTable, "leafParent" + levelAboveParent);
                joinTables.add(alias);

                buffer.append(alias.getRepresentation()).append(".").append(nodeKeyColumn.getName()).append(" and ").append(alias.getRepresentation()).append(".").append(parentKeyColumn.getName()).append("=");
                // e.g. after 2 iterations the buffer might be:
                // "nodeTable.parent=leafParent1.key and leafParent1.parent=leafParent2.key and leafParent2.parent="
            }
        }
        buffer.append(getRightOperand());

        CatalogSchemaTable[] extraJoinTables = joinTables.size() > 0 ? (CatalogSchemaTable[]) joinTables.toArray(new CatalogSchemaTable[joinTables.size()]) : null;
        String joinClause = buffer.toString();

        return new WhereClauseAndAdditionalTables(joinClause, extraJoinTables);
    }

    public String getDescription() {
        StringBuffer displayString = new StringBuffer("");
        displayString.append(getName()).append(" ").append(getOperator()).append(" (");

        if (nodeDescriptionProperty == null) {
            displayString.append(getParentNode().toString());
        } else {
            String[] pathArray = Generic.beanPathStringToArray(nodeDescriptionProperty);
            displayString.append((String) Generic.get(getParentNode(), pathArray, 0, true));
        }
        displayString.append(")");
        return displayString.toString();
    }

    protected void loadProperties(PropertyGroup propertyGroup) {
        super.loadProperties(propertyGroup);
        nodeDescriptionProperty = propertyGroup.getProperty("nodeDescriptionProperty");
        parentNodeProperty = propertyGroup.getProperty("parentNodeProperty");
        childCollectionProperty = propertyGroup.getProperty("childCollectionProperty");
    }

    private Object getParentNode() {
        if (parentNode == null) parentNode = BeanFactory.getInstance(getToBeanType()).get(getRightOperand());
        return parentNode;
    }
}
