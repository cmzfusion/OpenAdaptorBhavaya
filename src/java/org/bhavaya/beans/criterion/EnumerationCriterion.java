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
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.db.*;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.util.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.18.6.1 $
 */
public class EnumerationCriterion extends BasicCriterion {
    public static final String ENUM_IN_OPERATION = "IN";
    public static final String ENUM_NOT_IN_OPERATION = "NOT IN";

    protected static final String ENUM_SQL = "enumSql";
    private static final String ENUM_KEY_COLUMN = "keyColumn";
    private static final String ENUM_DESCRIPTION_COLUMN = "descriptionColumn";
    private static final String ENUM_DATASOURCE = "datasource";

    private static final Map enumResultMapsBySql = new HashMap();

    static {
        BeanUtilities.addPersistenceDelegate(EnumerationCriterion.class, new BhavayaPersistenceDelegate(new String[]{"id", "operator", "rightOperand"}));
    }

    private String enumSql;
    private Column[] enumKeyColumns;
    private Column enumDescriptionColumn;
    private String datasource;
    private String tempTable;

    public EnumerationCriterion(String id) {
        this(id, ENUM_IN_OPERATION, null);
    }

    public EnumerationCriterion(String id, EnumElement[] rightOperand) {
        this(id, ENUM_IN_OPERATION, rightOperand);
    }

    public EnumerationCriterion(String id, String operator, EnumElement[] rightOperand) {
        super(id, (operator == null ? ENUM_IN_OPERATION : operator), rightOperand);
        assertEnumOperation(operator);
    }

    protected void assertEnumOperation(String operator) {
        assert ENUM_IN_OPERATION.equals(getOperator()) || ENUM_NOT_IN_OPERATION.equals(getOperator()) : "Invalid operation: " + operator;
    }

    protected void loadProperties(PropertyGroup properties) {
        super.loadProperties(properties);

        Schema schema = null;
        if (Schema.hasInstance(getToBeanType())) {
            schema = Schema.getInstance(getToBeanType());
        }

        this.datasource = properties.getProperty(ENUM_DATASOURCE);
        if (this.datasource == null || this.datasource.length() == 0) {
            if (schema != null) {
                datasource = schema.getDefaultDataSourceName();
            } else {
                datasource = DataSourceFactory.getDefaultDataSourceName();
            }
        }

        enumSql = getEnumSql(properties, schema);

        if (enumSql == null) {
            enumDescriptionColumn = new Column(properties.getMandatoryProperty(ENUM_DESCRIPTION_COLUMN));

            String enumKeyColumnString = properties.getProperty(ENUM_KEY_COLUMN);
            if (enumKeyColumnString != null && schema == null) {
                enumKeyColumns = new Column[]{new Column(enumKeyColumnString)};
            } else {
                assert schema != null : "[" + this.getId() + "]: If there is no enumSql or keyColumn, must have Schema for toBeanType, as instances of toBeanType will be loaded from a BeanFactory";
                enumKeyColumns = schema.getPrimaryKey();
            }
        } else {
            String compoundEnumKeyColumns = properties.getProperty(ENUM_KEY_COLUMN);

            if (compoundEnumKeyColumns == null || compoundEnumKeyColumns.length() == 0) {
                if (schema != null) {
                    enumKeyColumns = schema.getPrimaryKey();
                }
            } else {
                String columnName = Utilities.getUnqualifiedName(compoundEnumKeyColumns, '.');
                if (compoundEnumKeyColumns.indexOf('.') != -1) {
                    String tableName = Utilities.getQualifier(compoundEnumKeyColumns, '.');
                    CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(tableName, datasource);
                    enumKeyColumns = new Column[]{createTableColumn(catalogSchemaTable, columnName)};
                } else {
                    enumKeyColumns = new Column[]{createTableColumn(null, columnName)};
                }
            }

            String compoundEnumDescriptionColumn = properties.getMandatoryProperty(ENUM_DESCRIPTION_COLUMN);
            String descriptionColumn = Utilities.getUnqualifiedName(compoundEnumDescriptionColumn, '.');
            CatalogSchemaTable descriptionTable = null;
            descriptionTable = initDescriptionTable(compoundEnumDescriptionColumn, getToBeanType(), descriptionColumn, descriptionTable);
            this.enumDescriptionColumn = createTableColumn(descriptionTable, descriptionColumn);
        }
    }

    protected String getEnumSql(PropertyGroup properties, Schema schema) {
        String enumSql = properties.getProperty(ENUM_SQL);
        if ((enumSql == null || enumSql.length() == 0) && schema != null) {
            if (SqlBeanFactory.class.isAssignableFrom(schema.getBeanFactoryType())) {
                enumSql = schema.getSql().toString();
            } 
        }
        return enumSql;
    }

    protected CatalogSchemaTable initDescriptionTable(String compoundEnumDescriptionColumn, Class beanType, String descriptionColumn, CatalogSchemaTable descriptionTable) {
            if (compoundEnumDescriptionColumn.indexOf('.') == -1) {
            Schema schema = Schema.getInstance(beanType);
                SQL sql = null;
                if (schema != null) {
                    sql = schema.getSql();
                } else if (enumSql != null) {
                    try {
                        sql = new SQL(enumSql, datasource);
                    } catch (Exception e) {
                    }
                }
                if (sql != null) {
                    TableColumn tableColumn = sql.getColumn(descriptionColumn, datasource);
                    if (tableColumn != null) {
                        descriptionTable = tableColumn.getCatalogSchemaTable();
                    }
                }
            } else {
                String tableName = Utilities.getQualifier(compoundEnumDescriptionColumn, '.');
                descriptionTable = CatalogSchemaTable.getInstance(tableName, datasource);
            }
        return descriptionTable;
    }

    private Column createTableColumn(CatalogSchemaTable catalogSchemaTable, String columnName) {
        if (catalogSchemaTable == null) {
            return new Column(columnName);
        } else {
            return TableColumn.getInstance(columnName, catalogSchemaTable, datasource);
        }
    }

    public String getCriterionType() {
        return BasicCriterion.ENUMERATION;
    }

    public String getEnumSql() {
        return enumSql;
    }

    public Column[] getEnumKeyColumns() {
        return enumKeyColumns;
    }

    public Column getEnumDescriptionColumn() {
        return enumDescriptionColumn;
    }

    public String getDatasource() {
        return datasource;
    }

    public Collection getAllBeans() {
        checkFail();
        return BeanFactory.getInstance(getToBeanType(), datasource).getAllBeanCollection();
    }

    protected Collection getEnumResultMap() {
        checkFail();
        return getEnumResultMap(enumSql, datasource);
    }

    private static Collection getEnumResultMap(String enumSql, String datasource) {
        Collection result = (Collection) enumResultMapsBySql.get(enumSql);
        if (result == null) {
            result = DBUtilities.execute(datasource, enumSql);
            enumResultMapsBySql.put(enumSql, result);
        }
        return result;
    }

    private String getTemporaryTable() {
        Connection connection = null;
        try {
            BhavayaDataSource bhavayaDataSource = DataSourceFactory.getInstance(datasource);
            connection = bhavayaDataSource.getConnection();
            connection.isClosed(); // test connection okay

            if (tempTable == null || !isTempTableAvailable(connection, tempTable)) {
                Object[] sourceArray = (Object[]) getRightOperand();
                Object[] keys = new Object[sourceArray.length];
                Utilities.getSubPropertyArray(sourceArray, keys, "id");
                tempTable = DBUtilities.createTemporaryTable(datasource, enumKeyColumns, keys, true);
            }
            return tempTable;
        } catch (Exception e) {
            tempTable = null;
            throw new RuntimeException(e);
        } finally {
            DBUtilities.close(connection);
        }

    }

    private boolean isTempTableAvailable(Connection connection, String tempTable) {
        boolean tableExists = false;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();

            String sql = "select * from " + tempTable + " where 1=2";

            Profiler.Task task = Profiler.taskStarted(Profiler.SQL, sql);
            try {
                resultSet = statement.executeQuery(sql);
            } finally {
                Profiler.taskStopped(task);
            }

            resultSet.next();
            tableExists = true;
        } catch (SQLException e) {
        } finally {
            DBUtilities.closeResultSetAndStatement(resultSet, statement);
        }
        return tableExists;
    }

    protected boolean useTempTable() {
        return getEnumKeyColumns().length > 1 || ((EnumElement[]) getRightOperand()).length > DBUtilities.MAX_ELEMENTS_FOR_IN_STATEMENT;
    }

    protected WhereClauseAndAdditionalTables getWhereClauseAndAdditionalTables(Class beanType) {
        EnumElement[] selectedElements = (EnumElement[]) getRightOperand();

        WhereClauseAndAdditionalTables whereClauseAndAdditionalTables = null;
        if (selectedElements.length == 0) {
            if (ENUM_IN_OPERATION.equals(getOperator())) {
                whereClauseAndAdditionalTables = new WhereClauseAndAdditionalTables("1=2", null);
            } else if (ENUM_NOT_IN_OPERATION.equals(getOperator())) {
                // do nothing, this will select all
                whereClauseAndAdditionalTables = null;
            }
        } else {
            whereClauseAndAdditionalTables = super.getWhereClauseAndAdditionalTables(beanType);
            if (useTempTable()) {
                String temporaryTable = getTemporaryTable();
                whereClauseAndAdditionalTables.setTables(new CatalogSchemaTable[]{new CatalogSchemaTable(null, temporaryTable, temporaryTable)});
            }
        }

        return whereClauseAndAdditionalTables;
    }

    protected void joinLeftOperandColumnGroup(Class beanType, Column[] columnGroup, final Object rightOperand, FastStringBuffer buffer) {
        if (useTempTable()) {
            joinLeftOperandColumnGroupUsingTempTable(columnGroup, buffer);
        } else {
            joinLeftOperandColumnGroupUsingIN(beanType, columnGroup, buffer);
        }
    }

    private void joinLeftOperandColumnGroupUsingTempTable(Column[] columnGroup, FastStringBuffer buffer) {
        String temporaryTable = getTemporaryTable();
        String operator = ENUM_IN_OPERATION.equalsIgnoreCase(getOperator()) ? "=" : "<>";

        for (int i = 0; i < columnGroup.length; i++) {
            if (i > 0) buffer.append(getAndOperator());
            Column column = columnGroup[i];
            buffer.append(column.getRepresentation());
            buffer.append(" ");
            buffer.append(operator);
            buffer.append(" ");
            buffer.append(temporaryTable);
            buffer.append(".");
            buffer.append(enumKeyColumns[i].getName());
        }
    }

    private void joinLeftOperandColumnGroupUsingIN(Class beanType, Column[] columnGroup, FastStringBuffer buffer) {
        EnumElement[] selectedElements = (EnumElement[]) getRightOperand();

        for (int i = 0; i < columnGroup.length; i++) {
            if (i > 0) buffer.append(getAndOperator());
            Column column = columnGroup[i];
            Set enumElementIdsForColumn = new HashSet(selectedElements.length);

            buffer.append(column.getRepresentation());
            buffer.append(" ");
            buffer.append(getOperator());
            buffer.append(" (");

            for (int j = 0; j < selectedElements.length; j++) {
                final Object enumElementId = selectedElements[j].getId();
                Object enumElementIdForColumn = enumElementId;

                if (columnGroup.length > 1 && enumElementId instanceof List) {
                    List compoundKey = (List) enumElementId;
                    enumElementIdForColumn = compoundKey.get(i);
                }

                if (!enumElementIdsForColumn.contains(enumElementIdForColumn)) {
                    if (enumElementIdsForColumn.size() > 0) buffer.append(",");
                    enumElementIdsForColumn.add(enumElementIdForColumn);
                    buffer.append(formatRightOperantForColumn(beanType, enumElementIdForColumn));
                }
            }

            buffer.append(")");
        }
    }

    public String getDescription() {
        FastStringBuffer displayString = new FastStringBuffer();
        displayString.append(getName());
        displayString.append(" ");
        displayString.append(getOperator());
        displayString.append(" (");
        EnumElement[] selectedElements = (EnumElement[]) getRightOperand();

        if (selectedElements.length == 0) {
            // nothing
        } else {
            displayString.append(selectedElements[0].toString());
            for (int i = 1; i < selectedElements.length; i++) {
                displayString.append(", ");
                displayString.append(selectedElements[i].toString());
            }
        }
        displayString.append(")");

        return displayString.toString();
    }

    public boolean evaluate(Object bean) {
        checkFail();

        EnumElement[] rightOperands = (EnumElement[]) getRightOperand();

        final boolean inOperator = ENUM_IN_OPERATION.equals(getOperator());
        final boolean notInOperator = ENUM_NOT_IN_OPERATION.equals(getOperator());

        if (rightOperands.length > 0) {
            Set leftOperandValues = getLeftOperandValues(bean);

            for (Iterator iterator = leftOperandValues.iterator(); iterator.hasNext();) {
                Object leftOperandValue = iterator.next();

                for (int j = 0; j < rightOperands.length; j++) {
                    EnumElement rightOperand = rightOperands[j];
                    boolean evaluate = evaluate(leftOperandValue, rightOperand.getId());

                    if (evaluate && inOperator) { // only one needs to evaluate to true
                        return true;
                    } else if (!evaluate && notInOperator) { // all must evaluate to true
                        return false;
                    }
                }
            }
        }

        // no selected items or none where in selected items
        if (inOperator) {
            return false;
        } else if (notInOperator) {
            return true;
        }

        throw new RuntimeException("invalid operator: " + getOperator());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnumerationCriterion)) return false;

        final EnumerationCriterion enumerationCriterion = (EnumerationCriterion) o;

        if (super.equals(o)) return false;

        if (!getId().equals(enumerationCriterion.getId())) return false;
        if (getOperator() != null ? !getOperator().equals(enumerationCriterion.getOperator()) : enumerationCriterion.getOperator() != null) return false;
        if (!Arrays.equals((Object[]) getRightOperand(), (Object[]) enumerationCriterion.getRightOperand())) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getId().hashCode();
        result = 29 * result + (getOperator() != null ? getOperator().hashCode() : 0);
        Object[] enumElements = (Object[]) getRightOperand();
        if (enumElements != null) {
            for (int i = 0; i < enumElements.length; i++) {
                Object enumElement = enumElements[i];
                result = 29 * result + (enumElement != null ? enumElement.hashCode() : 0);
            }
        }
        return result;
    }

    public static EnumerationCriterion convertBeanCollectionToCriterion(String criterionId, BeanCollection collection) {
        ArrayList enumElements = new ArrayList(collection.size());
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Object bean = iterator.next();
            enumElements.add(new EnumElement(BeanFactory.getKeyForBean(bean), ""));
        }

        return new EnumerationCriterion(criterionId, (EnumElement[]) enumElements.toArray(new EnumElement[enumElements.size()]));
    }

    public static EnumerationCriterion convertCollectionToCriterion(String criterionId, Collection collection, String[] keyColumns, String descriptionColumn) {
        EnumElement[] enumElements = convertCollectionToEnumElements(collection, keyColumns, descriptionColumn);
        return new EnumerationCriterion(criterionId, enumElements);
    }

    public static EnumElement[] convertCollectionToEnumElements(Collection collection, String[] keyColumns, String descriptionColumn) {
        ArrayList enumElements = new ArrayList(collection.size());
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            Object id = object != null ? Utilities.createKey(keyColumns, object) : null;
            Object description = object != null ? Generic.get(object, descriptionColumn) : "NULL";
            enumElements.add(new EnumElement(id, description));
        }

        return (EnumElement[]) enumElements.toArray(new EnumElement[enumElements.size()]);
    }

    public static class EnumElement implements Comparable {
        private Object id;
        private Object description;

        public EnumElement(Object id, Object description) {
            this.id = id;
            this.description = description;
        }

        public EnumElement() {
        }


        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Object getDescription() {
            return description;
        }

        public void setDescription(Object description) {
            this.description = description;
        }

        public String toString() {
            if (description == null) return "";
            return description.toString();
        }

        public int compareTo(Object o) {
            Object description2 = ((EnumElement) o).getDescription();

            if (description == null && description2 == null) {
                return 0;
            } else if (description == null) {
                return 1;
            } else if (description2 == null) {
                return -1;
            } else {
                return String.CASE_INSENSITIVE_ORDER.compare(description.toString(), description2.toString());
            }
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnumElement)) return false;

            final EnumElement enumElement = (EnumElement) o;

            if (id != null ? !id.equals(enumElement.id) : enumElement.id != null) return false;

            return true;
        }

        public int hashCode() {
            return (id != null ? id.hashCode() : 0);
        }
    }
}
