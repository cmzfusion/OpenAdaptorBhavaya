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

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.FastStringBufferThreadLocal;
import org.bhavaya.util.Log;
import org.bhavaya.util.Tokenizer2;
import org.bhavaya.util.Utilities;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.16 $
 */
public class SQL {
    private static final Log log = Log.getCategory(SQL.class);

    public static final int DROP_TABLE = -3;
    public static final int TRUNCATE = -2;
    public static final int DELETE = -1;
    public static final int UPDATE = 0;
    public static final int INSERT = 1;
    public static final int SELECT = 2;

    private static final String[] keywords =
            {"SELECT", "UPDATE", "INSERT", "DELETE", "WHERE", "FROM", "INTO",
             "UNION", "INTERSECT", "EXCEPT",
             "ORDER", "GROUP", "BY", "HAVING",
             "VALUES", "SET",
             "JOIN", "INNER", "OUTER", "CROSS", "LEFT", "RIGHT", "FULL", "NATURAL", "DROP"};
    private static final Set keywordsSet;
    private static final String[] functions = {"MAX", "MIN", "ABS", "AVG", "SUM", "COUNT", "UPPER", "LOWER", "TRIM", "SUBSTRING"};
    private static final Set functionsSet;
    private static final Map tableFindingPatterns = new HashMap();
    private static int aliasSuffix = 0;

    private static final FastStringBufferThreadLocal lineBufferThreadLocal = new FastStringBufferThreadLocal();
    private static final ListThreadLocal statementsThreadLocal = new ListThreadLocal();
    private static final ListThreadLocal tokensForStatementThreadLocal = new ListThreadLocal();

    private String dataSourceName;
    private String statement;
    private boolean distinct;
    private String statementWithoutWhereClause;
    private String operation;
    private int operationType;
    private IndexedSet columns;    //need an indexed view of the columns fo SQLResultSetFascade
    private Set modifiedColumns;    //need an indexed view of the columns fo SQLResultSetFascade
    private Map columnValueMap;
    private Map whereClauseColumnValueMap;
    private CatalogSchemaTable[] tables;
    private String body;
    private String whereClause;
    private String whereClauseForJoinToSelect;
    private String groupByClause;
    private String havingClause;
    private String orderByClause;
    private Map aliasedTables;
    private boolean valid;

    private int numberOfTokens = 0;
    private int position = 0;
    private String[] tokens = null;
    private SQLFormatter formatter;

    static {
        keywordsSet = new HashSet();
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            keywordsSet.add(keyword.toUpperCase());
        }

        functionsSet = new HashSet();
        for (int i = 0; i < functions.length; i++) {
            String function = functions[i];
            functionsSet.add(function.toUpperCase());
        }
    }

    public static SQL[] parseTransaction(String sqlTransactionString) {
        return parseTransaction(sqlTransactionString, DataSourceFactory.getDefaultDataSourceName());
    }

    public static SQL[] parseTransaction(String sqlTransactionString, String dataSourceName) {
        List statements = (List) statementsThreadLocal.get();
        List tokensForStatement = (List) tokensForStatementThreadLocal.get();
        FastStringBuffer line = (FastStringBuffer) lineBufferThreadLocal.get();

        Tokenizer2 tokenizer = new Tokenizer2(sqlTransactionString);

        for (String token = tokenizer.readToken(); token.length() != 0; token = tokenizer.readToken()) {
            char firstChar = token.charAt(0);
            if (firstChar == '\n') {
                addTokensToStatements(statements, tokensForStatement, line, sqlTransactionString, dataSourceName);
            } else if (Character.isWhitespace(firstChar)) {
                line.append(token);
            } else {
                line.append(token);
                tokensForStatement.add(token);
            }
        }

        addTokensToStatements(statements, tokensForStatement, line, sqlTransactionString, dataSourceName);
        SQL[] sqlStatements = (SQL[]) statements.toArray(new SQL[statements.size()]);
        statements.clear(); // reduce accumalated memory
        return sqlStatements;
    }

    private static void addTokensToStatements(List statements, List tokensForStatement, FastStringBuffer line, String sqlTransactionString, String dataSourceName) {
        try {
            if (tokensForStatement.size() == 0) return; // will still clear in finally block
            String[] tokens = (String[]) tokensForStatement.toArray(new String[tokensForStatement.size()]);
            SQL statement = new SQL(tokens, line.toString(), dataSourceName);
            if (statement.valid) statements.add(statement);
        } catch (Exception e) {
            log.error("Error parsing line: " + line.toString());
            log.error("Error parsing transaction: " + sqlTransactionString);
        } finally {
            line.setLength(0);
            tokensForStatement.clear();
        }
    }

    protected static String[] tokenizeStatement(String statementString) {
        //if (log.isDebug()) log.debug("Tokenizing statement: " + statementString);
        List tokensForStatement = (List) tokensForStatementThreadLocal.get();
        Tokenizer2 tokenizer = new Tokenizer2(statementString);

        for (String token = tokenizer.readToken(); token.length() != 0; token = tokenizer.readToken()) {
            if (!(Character.isWhitespace(token.charAt(0)))) {
                //if (log.isDebug()) log.debug(token + "|");
                tokensForStatement.add(token);
            }
        }

        return (String[]) tokensForStatement.toArray(new String[tokensForStatement.size()]);
    }

    private SQL() {
    }

    public SQL(String statement) {
        this(statement, DataSourceFactory.getDefaultDataSourceName());
    }

    public SQL(String statement, String dataSourceName) {
        this(tokenizeStatement(statement), statement, dataSourceName);
        if (!valid) fail();
    }

    private SQL(String[] tokens, String statement, String dataSourceName) {
        this.statement = statement;
        this.dataSourceName = dataSourceName;
        assert dataSourceName != null : "Null dataSourceName";
        this.valid = true;

        setTokens(tokens);
        String token = tokens[position];

        if (token.equalsIgnoreCase("SELECT")) {
            operation = "SELECT";
            operationType = SELECT;
            parseSelect();
        } else if (token.equalsIgnoreCase("INSERT")) {
            operation = "INSERT";
            operationType = INSERT;
            parseInsert();
        } else if (token.equalsIgnoreCase("UPDATE")) {
            operation = "UPDATE";
            operationType = UPDATE;
            parseUpdate();
        } else if (token.equalsIgnoreCase("DELETE")) {
            operation = "DELETE";
            operationType = DELETE;
            parseDelete();
        } else if (token.equalsIgnoreCase("TRUNCATE")) {
            operation = "TRUNCATE";
            operationType = TRUNCATE;
            parseTruncate();
        } else if (token.equalsIgnoreCase("DROP") && tokens[position + 1].equalsIgnoreCase("TABLE")) {
            operation = "DROP TABLE";
            operationType = DROP_TABLE;
            parseDropTable();
        } else {
            valid = false;
            if (log.isDebug()) log.debug("Not parsing: " + statement);
        }

        // allow tokens to be garbage collected early
        setTokens(null);
    }

    public Object clone() {
        SQL newSQL = new SQL();
        newSQL.valid = valid;
        newSQL.statement = statement;
        newSQL.statementWithoutWhereClause = statementWithoutWhereClause;
        newSQL.operation = operation;
        newSQL.operationType = operationType;
        newSQL.tables = (CatalogSchemaTable[]) tables.clone();
        newSQL.body = body;
        newSQL.distinct = distinct;
        newSQL.whereClause = whereClause;
        newSQL.whereClauseForJoinToSelect = whereClauseForJoinToSelect;
        newSQL.groupByClause = groupByClause;
        newSQL.havingClause = havingClause;
        newSQL.orderByClause = orderByClause;
        newSQL.dataSourceName = dataSourceName;

        if (columns != null) {
            newSQL.columns = new IndexedSet(columns.size());
            newSQL.columns.addAll(columns);
        }

        if (modifiedColumns != null) {
            newSQL.modifiedColumns = new HashSet(modifiedColumns.size());
            newSQL.modifiedColumns.addAll(modifiedColumns);
        }

        if (columnValueMap != null) newSQL.columnValueMap = (Map) ((HashMap) columnValueMap).clone();
        if (aliasedTables != null) newSQL.aliasedTables = (Map) ((HashMap) aliasedTables).clone();
        if (whereClauseColumnValueMap != null) newSQL.whereClauseColumnValueMap = (Map) ((HashMap) whereClauseColumnValueMap).clone();
        return newSQL;
    }


    private void fail() {
        log.error("Cannot parse statement: " + statement);
        throw new RuntimeException("Cannot parse statement: " + statement);
    }

    private void setTokens(String[] tokens) {
        this.tokens = tokens;
        numberOfTokens = (tokens == null) ? 0 : tokens.length;
        position = 0;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    private void parseSelect() {
        String token = tokens[++position];

        StringBuffer statementWithoutWhereClauseBuffer = new StringBuffer(statement.length());
        statementWithoutWhereClauseBuffer.append("SELECT ");

        if (token.equalsIgnoreCase("DISTINCT")) {
            statementWithoutWhereClauseBuffer.append("DISTINCT ");
            distinct = true;
            token = tokens[++position];
        }

        StringBuffer bodyBuffer = new StringBuffer(statement.length() / 2);

        Map allColumnsTokens = new LinkedHashMap();

        // Get columns into body
        if (position == numberOfTokens) fail(); // Need columns
        for (; position < numberOfTokens; position++) {
            StringBuffer columnBuffer = new StringBuffer();
            List columnTokens = new ArrayList(1);
            // second loop handles column aliases e.g. 'SELECT amount * price AS total FROM ...'
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                if (token.equalsIgnoreCase(",")) break;
                if (token.equalsIgnoreCase("FROM")) break;
                // fail if any other keyword
                if (isKeyword(token)) fail();
                insertSpace(columnBuffer, token);
                columnBuffer.append(token);
                columnTokens.add(token);
            }
            allColumnsTokens.put(columnTokens, columnBuffer.toString());
            bodyBuffer.append(columnBuffer.toString());
            if (token.equalsIgnoreCase(",")) bodyBuffer.append(", ");
            if (token.equalsIgnoreCase("FROM")) break;
        }
        statementWithoutWhereClauseBuffer.append(bodyBuffer);
        body = bodyBuffer.toString();

        // Get tables
        Map tablesByAlias = new HashMap(8);
        if (token.equalsIgnoreCase("FROM")) {
            statementWithoutWhereClauseBuffer.append(" FROM ");
            token = tokens[++position];
            if (position == numberOfTokens) fail(); // Need tables
            List tablesList = new ArrayList();
            for (; position < numberOfTokens; position++) {
                StringBuffer tableBuffer = new StringBuffer();
                // second loop handles table aliases
                for (; position < numberOfTokens; position++) {
                    token = tokens[position];
                    if (token.equalsIgnoreCase(",")) break;
                    if (token.equalsIgnoreCase("WHERE")) break;
                    if (token.equalsIgnoreCase("GROUP")) break;
                    if (token.equalsIgnoreCase("ORDER")) break;
                    // fail if any other keyword
                    if (isKeyword(token)) fail();
                    insertSpace(tableBuffer, token);
                    tableBuffer.append(token);
                }
                String table = tableBuffer.toString();
                CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(table, dataSourceName);
                tablesList.add(catalogSchemaTable);
                tablesByAlias.put(tokens[position - 1], catalogSchemaTable);
                statementWithoutWhereClauseBuffer.append(table);

                if (token.equalsIgnoreCase(",")) statementWithoutWhereClauseBuffer.append(", ");
                if (token.equalsIgnoreCase("WHERE")) break;
                if (token.equalsIgnoreCase("GROUP")) break;
                if (token.equalsIgnoreCase("ORDER")) break;
            }

            tables = (CatalogSchemaTable[]) tablesList.toArray(new CatalogSchemaTable[tablesList.size()]);
        } else {
            // must have FROM clause
            fail();
        }

        // need to determine columns from name after determining tables
        for (Iterator iterator = allColumnsTokens.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            List columnTokens = (List) entry.getKey();
            String originalColumnName = (String) entry.getValue();

            if (Schema.isGenerationMode()) {
                addColumn(new Column(originalColumnName));
            } else if (columnTokens.size() == 1) {
                String columnName = (String) columnTokens.get(0);
                if (columnName.equals("*")) {
                    // all columns in first table
                    CatalogSchemaTable catalogSchemaTable = tables[0];
                    Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
                    if (table == null) {
                        log.error("Could not add columns for: " + catalogSchemaTable + " to: " + statement);
                        valid = false;
                        return;
                    }

                    addColumnsForTable(table);

                } else if (columnName.indexOf('.') != -1) {
                    String unqualifiedColumnName = Utilities.getUnqualifiedName(columnName, '.');
                    String tableQualifier = Utilities.getQualifier(columnName, '.');
                    CatalogSchemaTable catalogSchemaTable = (CatalogSchemaTable) tablesByAlias.get(tableQualifier);
                    Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
                    if (table == null) {
                        log.error("Could not add columns for: " + catalogSchemaTable + " to: " + statement);
                        valid = false;
                        return;
                    }

                    if (unqualifiedColumnName.equals("*")) {
                        addColumnsForTable(table);
                    } else {
                        Column column = table.getColumn(columnName);
                        if (column == null) column = new Column(originalColumnName);
                        addColumn(column);
                    }
                }
            } else {
                String lastColumnToken = (String) columnTokens.get(columnTokens.size() - 1);
                if (lastColumnToken.equals(")")) {
                    // e.g. "MAX(X)" would create a column called "MAX(X)"
                    addColumn(new Column(originalColumnName));
                } else {
                    // e.g. "MAX(X) AS TOTAL" would create a column called "TOTAL"
                    addColumn(new Column((String) columnTokens.get(columnTokens.size() - 1)));
                }
            }
        }

        statementWithoutWhereClause = statementWithoutWhereClauseBuffer.toString();

        // Get where clause
        if (token.equalsIgnoreCase("WHERE")) {
            parseWhereClause(new String[]{"GROUP", "ORDER"});
            if (!valid) return;
        }
        if (position >= numberOfTokens) return;

        token = tokens[position];
        // Get group by clause
        if (token.equalsIgnoreCase("GROUP")) {
            token = tokens[++position];
            if (token.equalsIgnoreCase("BY")) {
                token = tokens[++position];
            } else {
                fail();
            }

            StringBuffer groupByClauseBuffer = new StringBuffer(25);
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                if (token.equalsIgnoreCase("HAVING")) break;
                if (token.equalsIgnoreCase("ORDER")) break;
                // fail if any other keyword
                if (isKeyword(token)) fail();
                insertSpace(groupByClauseBuffer, token);
                groupByClauseBuffer.append(token);
            }

            groupByClause = groupByClauseBuffer.toString();
        }

        // Get having clause
        if (token.equalsIgnoreCase("HAVING")) {
            token = tokens[++position];
            StringBuffer havingClauseBuffer = new StringBuffer(25);
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                if (token.equalsIgnoreCase("ORDER")) break;
                // fail if any other keyword
                if (isKeyword(token)) fail();
                insertSpace(havingClauseBuffer, token);
                havingClauseBuffer.append(token);
            }

            havingClause = havingClauseBuffer.toString();
        }

        // Get order by clause
        if (token.equalsIgnoreCase("ORDER")) {
            token = tokens[++position];
            if (token.equalsIgnoreCase("BY")) {
                ++position;
            } else {
                // 'order' must be followed by 'by'
                fail();
            }

            StringBuffer orderByClauseBuffer = new StringBuffer(25);
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                // fail if any other keyword
                if (isKeyword(token)) fail();
                insertSpace(orderByClauseBuffer, token);
                orderByClauseBuffer.append(token);
            }

            orderByClause = orderByClauseBuffer.toString();
        }
    }

    private void addColumnsForTable(Table table) {
        Column[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];
            addColumn(column);
        }
    }


    private void parseInsert() {
        String token = tokens[++position];

        if (token.equalsIgnoreCase("INTO")) {
            token = tokens[++position];
        }

        // Get table name
        if (position == numberOfTokens) fail(); // Need a table name
        if (isKeyword(token)) fail();

        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(token, dataSourceName);
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) {
            log.error("Cannot find table: " + catalogSchemaTable + " for sql: " + statement);
            valid = false;
            return;
        }
        tables = new CatalogSchemaTable[]{catalogSchemaTable};

        token = tokens[++position];

        // Get columns
        if (token.equalsIgnoreCase("(")) {
            token = tokens[++position];
            if (position == numberOfTokens) fail(); // Need columns
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                if (token.equalsIgnoreCase(")")) break;
                // fail if any other keyword
                if (isKeyword(token)) fail();
                if (token.equalsIgnoreCase(",")) continue;
                Column column = table.getColumn(token);
                if (column != null) {
                    addColumn(column);
                } else {
                    log.error("Could not find column: " + token + " in table: + " + catalogSchemaTable);
                    valid = false;
                    return;
                }
            }

            if (token.equalsIgnoreCase(")")) {
                token = tokens[++position];
            } else {
                // columns in 'INSERT INTO table (.....' must be followed by ')'
                fail();
            }
        } else {
            addColumnsForTable(table);
        }

        // Get values
        if (token.equalsIgnoreCase("VALUES")) {
            token = tokens[++position];
            if (token.equalsIgnoreCase("(")) {
                token = tokens[++position];
            } else {
                // 'VALUES' must be followed by '('
                fail();
            }

            int i = 0;
            for (; position < numberOfTokens; position++) {
                String value = null;
                // second loop to handle scenario 'INSERT INTO TRADE (..,..,..) VALUES (34 * 23, "ABB" + "BBA", 45)
                for (; position < numberOfTokens; position++) {
                    token = tokens[position];
                    if (token.equalsIgnoreCase(",")) break;
                    if (token.equalsIgnoreCase(")")) break;
                    // fail if any other keyword
                    if (isKeyword(token)) fail();
                    //if (insertSpace()) value = (value == null ? null : value + " ");
                    value = (value == null ? token : value + token);
                }

                final Column column = getColumn(i);
                if (column != null) {
                    addColumnValue(column, value);
                } else {
                    log.error("Could not find column position: " + i + " in table: + " + catalogSchemaTable);
                    valid = false;
                    return;
                }
                i++;
                if (token.equalsIgnoreCase(")")) break;
            }

            if (token.equalsIgnoreCase(")")) {
                //
            }
        }

        if (token.equalsIgnoreCase("SELECT")) {
            // parse cases like 'INSERT INTO (..,..,..) SELECT * FROM TRADE'
            CatalogSchemaTable[] tablesTemp = tables;
            IndexedSet columnsTemp = columns;
            columns = new IndexedSet();
            parseSelect();
            tables = tablesTemp;
            if (columnsTemp != null) columns = columnsTemp; // if columnsTemp is null we are relying on columns from select
        } else {
            // Create where clause from data in VALUES clause
            StringBuffer whereClauseBuffer = new StringBuffer(100);
            Column[] keyColumns = table.getPrimaryKey();
            for (int i = 0; i < keyColumns.length; i++) {
                if (i > 0) whereClauseBuffer.append(" AND ");
                Column keyColumn = keyColumns[i];
                String columnValue = getColumnValue(keyColumn);
                addWhereClauseColumnValue(keyColumn, columnValue);
                whereClauseBuffer.append(keyColumn.getRepresentation()).append("=").append(columnValue);
            }
            whereClause = whereClauseBuffer.toString();
        }
    }

    private void parseUpdate() {
        String token = tokens[++position];

        // Get table name
        if (position == numberOfTokens) fail(); // Need a table name
        if (isKeyword(token)) fail();

        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(token, dataSourceName);
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) {
            log.error("Cannot find table: " + catalogSchemaTable + " for sql: " + statement);
            valid = false;
            return;
        }
        tables = new CatalogSchemaTable[]{catalogSchemaTable};

        token = tokens[++position];

        // Get value changes
        if (token.equalsIgnoreCase("SET")) {
            token = tokens[++position];
            if (position == numberOfTokens) fail(); // Need value changes
            for (; position < numberOfTokens; position++) {
                token = tokens[position];
                String columnName = Utilities.getUnqualifiedName(token, '.');
                Column column = table.getColumn(columnName);
                if (column != null) {
                    addColumn(column);
                    addModifiedColumn(column);
                } else {
                    log.error("Could not find column: " + columnName + " in table: + " + catalogSchemaTable);
                    valid = false;
                    return;
                }
                ++position; // skip the '=' operator
                token = tokens[++position];
                String value = null;
                // second loop to handle scenarios such as 'set amount = amount * 2, '
                for (; position < numberOfTokens; position++) {
                    token = tokens[position];
                    if (token.equalsIgnoreCase(",")) break;
                    if (token.equalsIgnoreCase("WHERE")) break;
                    // fail if any other keyword
                    if (isKeyword(token)) fail();
                    //if (insertSpace()) value = (value == null ? null : value + " ");
                    value = (value == null ? token : value + token);
                }
                addColumnValue(column, value);
                if (token.equalsIgnoreCase("WHERE")) break;
            }
        } else {
            // update must set some values
//            fail();
            valid = false; // fail silently
            return;
        }

        // Get where clause
        if (token.equalsIgnoreCase("WHERE")) {
            parseWhereClause(null);
            if (!valid) return;
        }
    }

    private void parseDelete() {
        String token = tokens[++position];

        if (token.equalsIgnoreCase("FROM")) {
            token = tokens[++position];
        }

        // Get table name
        if (position == numberOfTokens) fail(); // Need a table name
        if (isKeyword(token)) fail();

        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(token, dataSourceName);
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) {
            log.error("Cannot find table: " + catalogSchemaTable + " for sql: " + statement);
            valid = false;
            return;
        }
        tables = new CatalogSchemaTable[]{catalogSchemaTable};

        if (++position < numberOfTokens) token = tokens[position];

        // Get where clause
        if (token.equalsIgnoreCase("WHERE")) {
            parseWhereClause(null);
            if (!valid) return;
        }
    }

    private void parseTruncate() {
        String token = tokens[++position];

        if (token.equalsIgnoreCase("TABLE")) {
            token = tokens[++position];
        }

        // Get table name
        if (position == numberOfTokens) fail(); // Need a table name
        if (isKeyword(token)) fail();

        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(token, dataSourceName);
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) {
            log.error("Cannot find table: " + catalogSchemaTable + " for sql: " + statement);
            valid = false;
            return;
        }
        tables = new CatalogSchemaTable[]{catalogSchemaTable};
    }

    private void parseDropTable() {
        String token = tokens[++position];

        if (token.equalsIgnoreCase("TABLE")) {
            token = tokens[++position];
        }

        // Get table name
        if (position == numberOfTokens) fail(); // Need a table name
        if (isKeyword(token)) fail();

        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(token, dataSourceName);
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) {
            log.error("Cannot find table: " + catalogSchemaTable + " for sql: " + statement);
            valid = false;
            return;
        }
        tables = new CatalogSchemaTable[]{catalogSchemaTable};
    }

    /**
     * determine the where clause string. Also determine if the where clause is a single primary key
     */
    private void parseWhereClause(String[] breakingKeywords) {
        StringBuffer whereClauseBuffer = new StringBuffer(statement.length() / 2);
        position++;

        boolean buildPrimaryKey = (getOperationType() == INSERT || getOperationType() == UPDATE || getOperationType() == DELETE);
        StringBuffer whereClauseForJoinToSelectBuffer = null;
        Map columnsValues = null;

        if (buildPrimaryKey) {
            columnsValues = new HashMap();
            whereClauseForJoinToSelectBuffer = new StringBuffer(statement.length() / 2);
        }

        for (; position < numberOfTokens; position++) {
            String token = tokens[position];
            if (breakingKeywords != null && Utilities.contains(breakingKeywords, token, true)) break;
            // fail if any other keyword
            if (isKeyword(token)) fail();
            if (buildPrimaryKey) {
                int newPosition = parseWhereClauseKeyValue(position, whereClauseBuffer, whereClauseForJoinToSelectBuffer, columnsValues);
                if (!valid) return;
                if (newPosition == -1) {
                    //this where clause does not contain a primary key definition and parseWhereClauseKeyValue did nothing
                    //parse where clause as normal, do not use parseWhereClauseKeyValue again for rest of where clause
                    buildPrimaryKey = false;
                    insertSpace(whereClauseBuffer, token);
                    whereClauseBuffer.append(token);
                } else {
                    // either:
                    // 1) added the key-value-pair from where clause
                    // or
                    // 2) ignored this key-value-pair, as it has already been defined
                    //    e.g. in an "update table set x = newValue, where x = oldValue" do not process (x = oldValue) as we already have (x = newValue)
                    position = newPosition;
                }
            } else {
                insertSpace(whereClauseBuffer, token);
                whereClauseBuffer.append(token);
            }
        }

        if (buildPrimaryKey) {
            // successfully parse the whole where clause as key-value-pairs, update the state of 'this'.
            for (Iterator iterator = columnsValues.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Column column = (Column) entry.getKey();
                String columnValue = (String) entry.getValue();
                //if (log.isDebug()) log.debug("Add column: " + column + ", columnValue: " + columnValue);
                addColumn(column);
                addColumnValue(column, columnValue);
                addWhereClauseColumnValue(column, columnValue);
            }

            whereClauseForJoinToSelect = whereClauseForJoinToSelectBuffer.toString();
        }

        whereClause = whereClauseBuffer.toString();
    }


    /**
     * parse:
     * any number of leading open brackets
     * then a column name
     * then "="
     * then a value
     * then any number of closing brackets
     * then zero or one "AND" symbols
     * <p/>
     * if any deviation is found then return "position" as it was passed to us
     * else return min( position+(length of successfully parsed statement), total number of tokens )
     * <p/>
     * it also accumulates parsed tokens into the provided string buffer
     */
    private int parseWhereClauseKeyValue(int originalPosition, StringBuffer whereClauseAccumulator, StringBuffer whereClauseForJoinToSelectAccumulator, Map columnValues) {
        int originalWhereClauseAccumulatorLength = whereClauseAccumulator.length();
        int originalWhereClauseForJoinToSelectAccumulatorLength = whereClauseForJoinToSelectAccumulator.length();
        int currentPosition = originalPosition;
        boolean foundKeyValuePair = false;
        Column column = null;
        String columnValue = null;

        String token = tokens[currentPosition];
        //ignore leading brackets
        while (token.equals("(")) {
            whereClauseAccumulator.append(token);
            whereClauseForJoinToSelectAccumulator.append(token);
            token = tokens[++currentPosition];
        }

        if ((numberOfTokens - currentPosition) > 2) {
            //append the column name
            insertSpace(whereClauseAccumulator, token);
            whereClauseAccumulator.append(token);

            String columnName = Utilities.getUnqualifiedName(token, '.');
            column = TableColumn.getInstance(columnName, tables[0], dataSourceName); //remove any table identifier from the column name
            if (column != null) {
                String qualifiedColumnName = column.getRepresentation();
                insertSpace(whereClauseForJoinToSelectAccumulator, qualifiedColumnName);
                whereClauseForJoinToSelectAccumulator.append(qualifiedColumnName);

                token = tokens[++currentPosition];

                //if next token is anything but "=" then we don't have a primary key
                if (token.equals("=") || token.equalsIgnoreCase("IS")) {
                    String operator = token;
                    token = tokens[++currentPosition];
                    columnValue = token;

                    insertSpace(whereClauseAccumulator, operator);
                    whereClauseAccumulator.append(operator);
                    insertSpace(whereClauseAccumulator, columnValue);
                    whereClauseAccumulator.append(columnValue);

                    if (hasColumnValue(column)) {
                        // e.g. in an "update table set x = newValue, where x = oldValue" use the newValue for the whereClauseForJoinToSelect
                        String newColumnValue = getColumnValue(column);

                        if (newColumnValue.equalsIgnoreCase("NULL")) {
                            insertSpace(whereClauseForJoinToSelectAccumulator, "IS");
                            whereClauseForJoinToSelectAccumulator.append("IS NULL");
                        } else {
                            insertSpace(whereClauseForJoinToSelectAccumulator, "=");
                            whereClauseForJoinToSelectAccumulator.append("=");
                            insertSpace(whereClauseForJoinToSelectAccumulator, newColumnValue);
                            whereClauseForJoinToSelectAccumulator.append(newColumnValue);
                        }
                    } else {
                        insertSpace(whereClauseForJoinToSelectAccumulator, operator);
                        whereClauseForJoinToSelectAccumulator.append(operator);
                        insertSpace(whereClauseForJoinToSelectAccumulator, columnValue);
                        whereClauseForJoinToSelectAccumulator.append(columnValue);
                    }

                    foundKeyValuePair = true;

                    if ((numberOfTokens - currentPosition) > 1) {
                        token = tokens[++currentPosition];

                        //now next token can be any number of close brackets
                        while (token.equals(")")) {
                            whereClauseAccumulator.append(token);
                            whereClauseForJoinToSelectAccumulator.append(token);
                            if ((numberOfTokens - currentPosition) <= 1) break;
                            token = tokens[++currentPosition];
                        }

                        //allow the next token to be an "AND"
                        if (token.equalsIgnoreCase("AND")) {
                            insertSpace(whereClauseAccumulator, token);
                            whereClauseAccumulator.append(token);

                            insertSpace(whereClauseForJoinToSelectAccumulator, token);
                            whereClauseForJoinToSelectAccumulator.append(token);
                        } else {
                            //we don't want to consume this token
                            currentPosition--;
                        }
                    }
                }
            } else {
                log.error("Could not find column: " + columnName + " in table: + " + tables[0]);
                valid = false;
            }
        }

        if (foundKeyValuePair) {
            // e.g. in an "update table set x = newValue, where x = oldValue" do not process (x = oldValue) as we already have (x = newValue)
            if (hasColumnValue(column)) {
                // revert where clause to original state
                whereClauseAccumulator.setLength(originalWhereClauseAccumulatorLength);
            } else {
                columnValues.put(column, columnValue);
            }
            return currentPosition;
        } else {
            // revert where clause to original state
            whereClauseAccumulator.setLength(originalWhereClauseAccumulatorLength);
            whereClauseForJoinToSelectAccumulator.setLength(originalWhereClauseForJoinToSelectAccumulatorLength);
            return -1;
        }
    }

    public String getOperation() {
        return operation;
    }

    public int getOperationType() {
        return operationType;
    }

    public String getBody() {
        return body;
    }

    private void addColumn(Column column) {
        if (column == null) throw new IllegalArgumentException("Null column added");
        getColumns().add(column);
    }

    public IndexedSet getColumns() {
        if (columns == null) columns = new IndexedSet();
        return columns;
    }

    public int getColumnCount() {
        if (columns == null) return 0;
        return columns.size();
    }

    public Column getColumn(int index) {
        return (Column) columns.get(index);
    }

    public boolean hasColumnValue(Column column) {
        return columnValueMap != null && columnValueMap.containsKey(column);
    }

    public String getColumnValue(Column column) {
        if (columnValueMap == null || !columnValueMap.containsKey(column)) throw new RuntimeException("Column: " + column + " does not exist in sql statement: " + this);
        return (String) getColumnValueMap().get(column);
    }

    private void addColumnValue(Column column, String value) {
        if (column == null) throw new IllegalArgumentException("Null column added");
        getColumnValueMap().put(column, value);
    }

    private Map getColumnValueMap() {
        if (columnValueMap == null) {
            columnValueMap = new HashMap();
        }
        return columnValueMap;
    }

    private void addWhereClauseColumnValue(Column column, String value) {
        if (column == null) throw new IllegalArgumentException("Null column added");
        getWhereClauseColumnValueMap().put(column, value);
    }

    private Map getWhereClauseColumnValueMap() {
        if (whereClauseColumnValueMap == null) {
            whereClauseColumnValueMap = new HashMap();
        }
        return whereClauseColumnValueMap;
    }

    public boolean isModified(Column column) {
        if (modifiedColumns == null) return false;
        return modifiedColumns.contains(column);
    }

    private void addModifiedColumn(Column column) {
        if (column == null) throw new IllegalArgumentException("Null column added");
        getModifiedColumns().add(column);
    }

    private Set getModifiedColumns() {
        if (modifiedColumns == null) modifiedColumns = new HashSet();
        return modifiedColumns;
    }


    public CatalogSchemaTable[] getTables() {
        return tables;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public String getWhereClauseForJoinToSelect() {
        if (whereClauseForJoinToSelect != null) {
            return whereClauseForJoinToSelect;
        } else {
            return whereClause;
        }
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public String getHavingClause() {
        return havingClause;
    }

    public String getStatementSansWhereClause() {
        return statementWithoutWhereClause;
    }

    public String getStatementString() {
        return statement;
    }

    public String joinWhereClause(String otherWhereClause, String operator) {
        if (otherWhereClause == null || otherWhereClause.length() == 0) {
            return this.getStatementString();
        } else {
            StringBuffer joinedSql = new StringBuffer(this.statement.length() + otherWhereClause.length() + 20);
            joinedSql.append(this.getStatementSansWhereClause()).append(" WHERE ").append(concatenateClauses(this.getWhereClause(), operator, otherWhereClause, true));
            return joinedSql.toString();
        }
    }

    public SQL joinWhereClause(String otherWhereClause) {
        return joinStatement(null, null, otherWhereClause, null, false);
    }

    public SQL joinWhereClause(String otherWhereClause, CatalogSchemaTable[] additionalJoinTables) {
        return joinStatement(null, null, otherWhereClause, additionalJoinTables, false);
    }

    /**
     * otherStatement's "FROM" tables must be the same as this statement's for this method to work
     */
    public SQL joinStatement(SQL otherStatement) {
        return joinStatement(otherStatement, null, null, null, false);
    }

    public SQL joinStatement(SQL otherStatement, Join[] joins, String additionalClause, CatalogSchemaTable[] additionalJoinTables, boolean aliasTables) {
        assert (getOperationType() == SELECT) : "This method does not support non SELECT statements";

        SQL thisStatement = (SQL) this.clone();

        // we may mutate the array
        Join[] joinsCopy = null;
        if (joins != null) {
            joinsCopy = (Join[]) joins.clone();
        }

        if (otherStatement != null) {
            if (aliasTables) {
                // alias matching tables between thisStatement and otherStatement
                CatalogSchemaTable[] tablesToAlias = Utilities.intersection(thisStatement.getTables(), otherStatement.getTables());

                for (int i = 0; i < tablesToAlias.length; i++) {
                    CatalogSchemaTable tableToAlias = tablesToAlias[i];
                    aliasSuffix++;
                    AliasedCatalogSchemaTable alias = new AliasedCatalogSchemaTable(tableToAlias, tableToAlias.getTableName() + 'N' + aliasSuffix);
                    thisStatement.alias(tableToAlias, alias);
                    if (additionalClause != null) additionalClause = aliasTableInString(additionalClause, tableToAlias, alias);
                    if (joinsCopy != null) {
                        for (int j = 0; j < joinsCopy.length; j++) {
                            joinsCopy[j] = joinsCopy[j].aliasForeignTable(alias);
                        }
                    }
                }
            }

            // then amend join for any historical aliases that were applied to otherStatement
            if (otherStatement.aliasedTables != null) {
                for (Iterator iterator = otherStatement.aliasedTables.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    CatalogSchemaTable tableToAlias = (CatalogSchemaTable) entry.getKey();
                    AliasedCatalogSchemaTable alias = (AliasedCatalogSchemaTable) entry.getValue();
                    if (additionalClause != null) additionalClause = aliasTableInString(additionalClause, tableToAlias, alias);
                    if (joinsCopy != null) {
                        for (int j = 0; j < joinsCopy.length; j++) {
                            joinsCopy[j] = joinsCopy[j].aliasParentTable(alias);
                        }
                    }
                }
            }
        }

        Set joinedStatementTablesSet = new LinkedHashSet(thisStatement.getTables().length + (otherStatement != null ? otherStatement.getTables().length : 0));
        addTables(joinedStatementTablesSet, thisStatement.getTables());

        String joinedWhereClause = thisStatement.whereClause;
        if (joinsCopy != null) {
            for (int j = 0; j < joinsCopy.length; j++) {
                Join join = joinsCopy[j];
                joinedWhereClause = concatenateClauses(joinedWhereClause, "AND", join.getRepresentation(), true);
            }
        }
        joinedWhereClause = concatenateClauses(joinedWhereClause, "AND", additionalClause, true);

        String joinedGroupByClause = thisStatement.getGroupByClause();
        String joinedHavingClause = thisStatement.getHavingClause();
        String joinedOrderByClause = thisStatement.getOrderByClause();

        if (otherStatement != null) {
            addTables(joinedStatementTablesSet, otherStatement.getTables());

            if (otherStatement.getOperationType() == UPDATE || otherStatement.getOperationType() == DELETE || otherStatement.getOperationType() == INSERT) {
                joinedWhereClause = concatenateClauses(joinedWhereClause, "AND", otherStatement.getWhereClauseForJoinToSelect(), true);
            } else {
                joinedWhereClause = concatenateClauses(joinedWhereClause, "AND", otherStatement.getWhereClause(), true);
            }
            joinedGroupByClause = concatenateClauses(joinedGroupByClause, ",", otherStatement.getGroupByClause(), false);
            joinedHavingClause = concatenateClauses(joinedHavingClause, "AND", otherStatement.getHavingClause(), true);
            joinedOrderByClause = concatenateClauses(joinedOrderByClause, ",", otherStatement.getOrderByClause(), false);
        }

        if (additionalJoinTables != null) {
            addTables(joinedStatementTablesSet, additionalJoinTables);
        }

        thisStatement.tables = (CatalogSchemaTable[]) joinedStatementTablesSet.toArray(new CatalogSchemaTable[joinedStatementTablesSet.size()]);
        thisStatement.whereClause = joinedWhereClause;
        thisStatement.groupByClause = joinedGroupByClause;
        thisStatement.havingClause = joinedHavingClause;
        thisStatement.orderByClause = joinedOrderByClause;

        StringBuffer joinedStatementBuffer;
        joinedStatementBuffer = new StringBuffer();
        joinedStatementBuffer.append("SELECT ");
        if (distinct) joinedStatementBuffer.append("DISTINCT ");
        joinedStatementBuffer.append(thisStatement.getBody()).append(" FROM ");
        for (int i = 0; i < thisStatement.tables.length; i++) {
            CatalogSchemaTable table = thisStatement.tables[i];
            if (i > 0) joinedStatementBuffer.append(", ");
            joinedStatementBuffer.append(table.getTableRepresentation());
        }

        thisStatement.statementWithoutWhereClause = joinedStatementBuffer.toString();

        if (joinedWhereClause != null && joinedWhereClause.length() > 0) {
            joinedStatementBuffer.append(" WHERE ").append(joinedWhereClause);
        }
        if (joinedGroupByClause != null && joinedGroupByClause.length() > 0) {
            joinedStatementBuffer.append(" GROUP BY ").append(joinedGroupByClause);
        }
        if (joinedHavingClause != null && joinedHavingClause.length() > 0) {
            joinedStatementBuffer.append(" HAVING ").append(joinedHavingClause);
        }
        if (joinedOrderByClause != null && joinedOrderByClause.length() > 0) {
            joinedStatementBuffer.append(" ORDER BY ").append(joinedOrderByClause);
        }
        thisStatement.statement = joinedStatementBuffer.toString();

        return thisStatement;
    }

    private void addTables(Set tablesSet, CatalogSchemaTable[] tables) {
        for (int i = 0; i < tables.length; i++) {
            tablesSet.add(tables[i]);
        }
    }

    private void alias(CatalogSchemaTable tableToAlias, AliasedCatalogSchemaTable alias) {
        if (aliasedTables == null) aliasedTables = new HashMap();
        aliasedTables.put(tableToAlias, alias);

        statement = aliasTableInString(statement, tableToAlias, alias);
        statementWithoutWhereClause = aliasTableInString(statementWithoutWhereClause, tableToAlias, alias);

        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            if (table.equals(tableToAlias)) tables[i] = alias;
        }

        body = aliasTableInString(body, tableToAlias, alias);
        if (whereClause != null) whereClause = aliasTableInString(whereClause, tableToAlias, alias);
        if (whereClauseForJoinToSelect != null) whereClauseForJoinToSelect = aliasTableInString(whereClauseForJoinToSelect, tableToAlias, alias);
        if (groupByClause != null) groupByClause = aliasTableInString(groupByClause, tableToAlias, alias);
        if (havingClause != null) havingClause = aliasTableInString(havingClause, tableToAlias, alias);
        if (orderByClause != null) orderByClause = aliasTableInString(orderByClause, tableToAlias, alias);
    }

    private static String aliasTableInString(String string, CatalogSchemaTable tableToAlias, AliasedCatalogSchemaTable alias) {
        Pattern pattern = getTableFindingPattern(tableToAlias);
        Matcher matcher = pattern.matcher(string);
        StringBuffer aliasedString = new StringBuffer(string.length());
        while (matcher.find()) {
            String replacement = alias.getRepresentation() + matcher.group(3);
            matcher.appendReplacement(aliasedString, replacement);
        }
        matcher.appendTail(aliasedString);
        return aliasedString.toString();
    }

    private static Pattern getTableFindingPattern(CatalogSchemaTable tableToAlias) {
        Pattern findTablePattern = (Pattern) tableFindingPatterns.get(tableToAlias);
        if (findTablePattern == null) {
            findTablePattern = Pattern.compile("((" + tableToAlias.getRepresentation() + ")([^a-zA-Z0-9_]|$))");
            tableFindingPatterns.put(tableToAlias, findTablePattern);
        }
        return findTablePattern;
    }

    public SQL getSelectKeysOnlyStatement(Column[] keyColumns) {
        StringBuffer selectKeysOnlyStatementBuffer = new StringBuffer(statement.length());
        selectKeysOnlyStatementBuffer.append("SELECT ");

        for (int i = 0; i < keyColumns.length; i++) {
            Column keyColumn = keyColumns[i];
            if (i > 0) selectKeysOnlyStatementBuffer.append(", ");
            selectKeysOnlyStatementBuffer.append(keyColumn.getRepresentation());
        }

        selectKeysOnlyStatementBuffer.append(" FROM ");

        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            if (i > 0) selectKeysOnlyStatementBuffer.append(", ");
            selectKeysOnlyStatementBuffer.append(table.getRepresentation());
        }

        if (whereClause != null) {
            selectKeysOnlyStatementBuffer.append(" WHERE ");
            selectKeysOnlyStatementBuffer.append(whereClause);
        }

        if (groupByClause != null) {
            selectKeysOnlyStatementBuffer.append(" GROUP BY ");
            selectKeysOnlyStatementBuffer.append(groupByClause);
        }

        if (havingClause != null) {
            selectKeysOnlyStatementBuffer.append(" HAVING ");
            selectKeysOnlyStatementBuffer.append(havingClause);
        }

        if (orderByClause != null) {
            selectKeysOnlyStatementBuffer.append(" ORDER BY ");
            selectKeysOnlyStatementBuffer.append(orderByClause);
        }

        return new SQL(selectKeysOnlyStatementBuffer.toString(), dataSourceName);
    }

    public SQL getSelectCountStatement(Column column) {
        StringBuffer selectCountStatementBuffer = new StringBuffer(statement.length());
        selectCountStatementBuffer.append("SELECT ");
        if (distinct) selectCountStatementBuffer.append("DISTINCT ");
        selectCountStatementBuffer.append("count(").append(column.getRepresentation()).append(") as NUMBEROFROWS FROM ");

        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            if (i > 0) selectCountStatementBuffer.append(", ");
            selectCountStatementBuffer.append(table.getTableRepresentation());
        }

        if (whereClause != null) {
            selectCountStatementBuffer.append(" WHERE ");
            selectCountStatementBuffer.append(whereClause);
        }

        if (groupByClause != null) {
            selectCountStatementBuffer.append(" GROUP BY ");
            selectCountStatementBuffer.append(groupByClause);
        }

        if (havingClause != null) {
            selectCountStatementBuffer.append(" HAVING ");
            selectCountStatementBuffer.append(havingClause);
        }

        if (orderByClause != null) {
            selectCountStatementBuffer.append(" ORDER BY ");
            selectCountStatementBuffer.append(orderByClause);
        }

        return new SQL(selectCountStatementBuffer.toString(), dataSourceName);
    }

    /**
     * Returns a String as SQL doesnt currently parse "SELECT .... INTO ...."
     *
     * @param tableName
     */
    public String getSelectIntoStatement(String tableName, Column[] columns) {
        StringBuffer selectIntoStatementBuffer = new StringBuffer(statement.length());

        selectIntoStatementBuffer.append("SELECT ");
        if (distinct) selectIntoStatementBuffer.append("DISTINCT ");

        if (columns == null) {
            columns = (Column[]) this.columns.toArray(new Column[this.columns.size()]);
        }
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];
            if (i > 0) selectIntoStatementBuffer.append(", ");
            selectIntoStatementBuffer.append(column.getRepresentation());
        }

        selectIntoStatementBuffer.append(" INTO ");
        selectIntoStatementBuffer.append(tableName);
        selectIntoStatementBuffer.append(" FROM ");

        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            if (i > 0) selectIntoStatementBuffer.append(", ");
            selectIntoStatementBuffer.append(table.getTableRepresentation());
        }

        if (whereClause != null) {
            selectIntoStatementBuffer.append(" WHERE ");
            selectIntoStatementBuffer.append(whereClause);
        }

        if (groupByClause != null) {
            selectIntoStatementBuffer.append(" GROUP BY ");
            selectIntoStatementBuffer.append(groupByClause);
        }

        if (havingClause != null) {
            selectIntoStatementBuffer.append(" HAVING ");
            selectIntoStatementBuffer.append(havingClause);
        }

        if (orderByClause != null) {
            selectIntoStatementBuffer.append(" ORDER BY ");
            selectIntoStatementBuffer.append(orderByClause);
        }

        return selectIntoStatementBuffer.toString();
    }

    public SQL getSelectStatementForKey(Column[] keyColumns, Object key) {
        if (key == null) return this;

        int numberOfKeyComponents = keyColumns.length;
        StringBuffer whereClauseBuffer = new StringBuffer(numberOfKeyComponents * 40);

        if (numberOfKeyComponents == 1) {
            whereClauseBuffer.append(keyColumns[0].getRepresentation()).append(" = ").append(getSqlFormatter().format(key));
        } else {
            List keyList = (List) key;
            for (int i = 0; i < keyColumns.length; i++) {
                Column keyColumn = keyColumns[i];
                Object keyComponent = keyList.get(i);
                if (i > 0) whereClauseBuffer.append(" AND ");
                whereClauseBuffer.append(keyColumn.getRepresentation()).append(" = ").append(getSqlFormatter().format(keyComponent));
            }
        }

        return joinWhereClause(whereClauseBuffer.toString());
    }

    private SQLFormatter getSqlFormatter() {
        if (formatter == null) formatter = SQLFormatter.getInstance(dataSourceName);
        return formatter;
    }

    public SQL getSelectStatementForKeys(Column keyColumn, Object[] keys) {
        if (keys == null || keys.length == 0) return this;

        StringBuffer whereClauseBuffer = new StringBuffer(keys.length * 10);

        whereClauseBuffer.append(keyColumn.getRepresentation()).append(" IN ( ");
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (i > 0) whereClauseBuffer.append(", ");
            whereClauseBuffer.append(getSqlFormatter().format(key));
        }
        whereClauseBuffer.append(")");

        return joinWhereClause(whereClauseBuffer.toString());
    }

    public boolean tablesMatch(SQL incoming) {
        return Utilities.contains(tables, incoming.getTables()[0]);
    }

    public TableColumn getColumn(String columnName, String datasource) {
        for (int j = 0; j < getTables().length; j++) {
            TableColumn column = TableColumn.getInstance(columnName, getTables()[j], datasource);
            if (column != null) return column;
        }
        log.warn("Cannot find column: " + columnName + " in tables: " + Utilities.asString(getTables(), ","));
        return null;
    }

    public String toString() {
        return statement;
    }

    public void debug() {
        if (log.isDebug()) log.debug("");
        if (log.isDebug()) log.debug("Statement: " + this.statement);
        if (log.isDebug()) log.debug("Operation: " + this.getOperation());

        if (columns != null) {
            for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
                if (log.isDebug()) log.debug("Column: " + iterator.next());
            }
        }

        if (modifiedColumns != null) {
            for (Iterator iterator = modifiedColumns.iterator(); iterator.hasNext();) {
                if (log.isDebug()) log.debug("Modified Column: " + iterator.next());
            }
        }

        if (columnValueMap != null) {
            Iterator iter = columnValueMap.keySet().iterator();
            while (iter.hasNext()) {
                Column column = (Column) iter.next();
                String value = getColumnValue(column);
                if (log.isDebug()) log.debug("ColumnValue: " + column + " = " + value);
            }
        }

        if (log.isDebug()) log.debug("Body: " + this.getBody());
        for (int i = 0; i < this.getTables().length; i++) {
            if (log.isDebug()) log.debug("Table: " + this.getTables()[i]);
        }
        if (log.isDebug()) log.debug("Where clause: " + this.getWhereClause());
        if (log.isDebug()) log.debug("Statement Sans Where Clause: " + this.getStatementSansWhereClause());
        if (log.isDebug()) log.debug("Group by clause: " + this.getGroupByClause());
        if (log.isDebug()) log.debug("Having clause: " + this.getHavingClause());
        if (log.isDebug()) log.debug("Order by clause: " + this.getOrderByClause());
    }


    public Map getAliasedTables() {
        return aliasedTables;
    }

    public boolean equals(Object object) {
        return statement.equals(object);
    }

    public int hashCode() {
        return statement.hashCode();
    }

    /**
     * Build a clause string from two clauses.
     * The clauses are separated by the given logical operator
     */
    private static String concatenateClauses(String firstClause, String operator, String secondClause, boolean delimitWithBrackets) {
        if (firstClause != null && secondClause != null && firstClause.length() != 0 && secondClause.length() != 0) {
            StringBuffer concatenatedClause = new StringBuffer(firstClause.length() + secondClause.length() + 10);
            if (delimitWithBrackets) concatenatedClause.append("(");
            concatenatedClause.append(firstClause);
            if (delimitWithBrackets) concatenatedClause.append(")");
            insertSpace(concatenatedClause, operator);
            concatenatedClause.append(operator);
            if (delimitWithBrackets) {
                insertSpace(concatenatedClause, "(");
                concatenatedClause.append("(");
            }
            insertSpace(concatenatedClause, secondClause);
            concatenatedClause.append(secondClause);
            if (delimitWithBrackets) concatenatedClause.append(")");
            return concatenatedClause.toString();
        } else if (firstClause == null || firstClause.length() == 0) {
            return secondClause;
        } else if (secondClause == null || secondClause.length() == 0) {
            return firstClause;
        }
        return "";
    }

    private static void insertSpace(StringBuffer buffer, String token) {
        if (buffer.length() == 0) return;
        if (token.equals("(") && isFunction(buffer)) return; // not order of if statement is useful performance optimisation, as isFunction is slow
        char token1Char = buffer.charAt(buffer.length() - 1);
        char token2Char = token.charAt(0);
        if (insertSpace(token1Char, token2Char)) buffer.append(' ');
    }

    private static boolean insertSpace(char token1Char, char token2Char) {
        if ((token1Char == ' ') || (token1Char == '(') || (token2Char == ',') || (token2Char == ')') || (token2Char == ' ')) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean isFunction(CharSequence charSequence) {
        for (int i = 0; i < functions.length; i++) {
            String function = functions[i];
            if (
                    (
                    (charSequence.length() == function.length() && charSequence.toString().toUpperCase().equals(function)) ||
                    (charSequence.length() > function.length() && charSequence.charAt(charSequence.length() - function.length() - 1) == ' ' && charSequence.subSequence(charSequence.length() - function.length(), charSequence.length()).equals(function))
                    )
            ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKeyword(String token) {
        return keywordsSet.contains(token.toUpperCase());
    }

    private static class ListThreadLocal extends ThreadLocal {
        protected Object initialValue() {
            return new ArrayList();
        }

        public Object get() {
            List o = (List) super.get();
            o.clear();
            return o;
        }
    }
}
