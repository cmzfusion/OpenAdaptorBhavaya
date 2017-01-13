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

import junit.framework.TestCase;
import org.bhavaya.beans.Column;
import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.SQL;
import org.bhavaya.db.Table;
import org.bhavaya.util.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.1 $
 */
public class SQLTest extends TestCase {
    public SQLTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        DBUtilities.executeUpdateScript("demoDatabase", "destroyDemo.sql", false);
        DBUtilities.executeUpdateScript("demoDatabase", "createDemo.sql", true);
    }

    public void testSelect() throws Exception {
        String statementString = "SELECT COUNT(I.INSTRUMENT_ID), MAX(TRADE.PRICE), MAX(TRADE.QUANTITY) AS TOTAL, I.* FROM INSTRUMENT AS I, TRADE WHERE TRADE.INSTRUMENT_ID = I.INSTRUMENT_ID AND TRADE.TRADE_DATE = '2003-03-19' GROUP BY I.INSTRUMENT_ID, TRADE.PRICE HAVING MAX(TRADE.PRICE) >= 105 AND MIN(TRADE.QUANTITY) <= 20000 ORDER BY TRADE.TRADE_ID ASC, I.INSTRUMENT_ID";
        SQL sql1 = new SQL(statementString);
        assertTrue("Statement", compareStrings(sql1.getStatementString(), statementString));
        assertTrue("Statement Sans Where Clause", compareStrings(sql1.getStatementSansWhereClause(), "SELECT COUNT(I.INSTRUMENT_ID), MAX(TRADE.PRICE), MAX(TRADE.QUANTITY) AS TOTAL, I.* FROM INSTRUMENT AS I, TRADE"));
        assertTrue("Where clause", compareStrings(sql1.getWhereClause(), "TRADE.INSTRUMENT_ID = I.INSTRUMENT_ID AND TRADE.TRADE_DATE = '2003-03-19'"));
        assertTrue("Group By", compareStrings(sql1.getGroupByClause(), "I.INSTRUMENT_ID, TRADE.PRICE"));
        assertTrue("Having", compareStrings(sql1.getHavingClause(), "MAX(TRADE.PRICE) >= 105 AND MIN(TRADE.QUANTITY) <= 20000"));
        assertTrue("Order By", compareStrings(sql1.getOrderByClause(), "TRADE.TRADE_ID ASC, I.INSTRUMENT_ID"));

        Column[] columns = (Column[]) sql1.getColumns().toArray(new Column[sql1.getColumns().size()]);
        assertTrue("Column1", columns[0].getRepresentation().equals("COUNT(I.INSTRUMENT_ID)"));
        assertTrue("Column2", columns[1].getRepresentation().equals("MAX(TRADE.PRICE)"));
        assertTrue("Column3", columns[2].getRepresentation().equals("TOTAL"));
        assertTrue("Column4", columns[3].getRepresentation().equals("INSTRUMENT.INSTRUMENT_ID"));
        assertTrue("Column5", columns[4].getRepresentation().equals("INSTRUMENT.CURRENCY"));
        assertTrue("Column6", columns[5].getRepresentation().equals("INSTRUMENT.INSTRUMENT_TYPE_ID"));
        assertTrue("Column7", columns[6].getRepresentation().equals("INSTRUMENT.DESCRIPTION"));
        assertTrue("Column8", columns[7].getRepresentation().equals("INSTRUMENT.VALID"));
    }

    public void testWhereClause() throws Exception {
        String statementString = "UPDATE TRADE SET QUANTITY = 32000000, PRICE = 108 WHERE PRICE = 107 AND COMMENTS = 'test''Value'";
        SQL sql1 = new SQL(statementString);

        Table table = Table.getInstance(CatalogSchemaTable.getInstance("TRADE", "demoDatabase"), "demoDatabase");
        assertTrue(sql1.getColumnValue(table.getColumn("PRICE")).equals("108"));
        assertTrue(sql1.getColumnValue(table.getColumn("COMMENTS")).equals("'test''Value'"));
        assertTrue(sql1.getColumnValue(table.getColumn("QUANTITY")).equals("32000000"));
    }

    public void testParseTransaction() throws Exception {
        String statementString = "begin transaction\nINSERT INTO COUNTERPARTY VALUES (0, 'ABN AMRO')\nINSERT INTO INSTRUMENT_RATING VALUES (1, 3)\nINSERT INTO TRADE VALUES (1, 0, 0, '2002-08-11', 0, 0, 'FOR DEMO', 2, 3000000, 102.34)\nend transaction\n";
        SQL[] sqls = SQL.parseTransaction(statementString);

        assertTrue(sqls.length == 3);
        assertTrue(compareStrings(sqls[0].getStatementString(), "INSERT INTO COUNTERPARTY VALUES (0, 'ABN AMRO')"));
        assertTrue(compareStrings(sqls[1].getStatementString(), "INSERT INTO INSTRUMENT_RATING VALUES (1, 3)"));
        assertTrue(compareStrings(sqls[2].getStatementString(), "INSERT INTO TRADE VALUES (1, 0, 0, '2002-08-11', 0, 0, 'FOR DEMO', 2, 3000000, 102.34)"));
    }

    public void testInsert() {
        List columns = new ArrayList();
        Table table = Table.getInstance(CatalogSchemaTable.getInstance("TRADE", "demoDatabase"), "demoDatabase");
        columns.add(table.getColumn("TRADE_ID"));
        columns.add(table.getColumn("VERSION"));
        columns.add(table.getColumn("VERSION_STATUS_ID"));
        columns.add(table.getColumn("TRADE_DATE"));
        columns.add(table.getColumn("TRADE_TYPE_ID"));
        columns.add(table.getColumn("INSTRUMENT_ID"));
        columns.add(table.getColumn("COMMENTS"));
        columns.add(table.getColumn("QUANTITY"));
        columns.add(table.getColumn("COUNTERPARTY_ID"));
        columns.add(table.getColumn("PRICE"));

        StringBuffer statementString = new StringBuffer("INSERT TRADE (");
        int i = 0;
        for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
            Column column = (Column) iterator.next();
            if (i > 0) statementString.append(", ");
            statementString.append(column.getName());
            i++;
        }

        statementString.append(") VALUES (");

        List values = new ArrayList();
        values.add("10234");
        values.add("2");
        values.add("1");
        values.add("'Thu Feb 21 11:31:28 GMT 2002'");
        values.add("4");
        values.add("123124");
        values.add("'Hello\n''You'' INSERT VALUES SELECT'"); // embedded keywords
        values.add("3100000");
        values.add("NULL");
        values.add("101.4569");

        i = 0;
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            String value = (String) iterator.next();
            if (i > 0) statementString.append(", ");
            statementString.append(value);
            i++;
        }

        statementString.append(")");

        SQL sql1 = new SQL(statementString.toString());

        Column[] parsedColumns = (Column[]) sql1.getColumns().toArray(new Column[]{});
        assertTrue("Column1", parsedColumns[0].equals(table.getColumn("TRADE_ID")));
        assertTrue("Column2", parsedColumns[1].equals(table.getColumn("VERSION")));
        assertTrue("Column3", parsedColumns[2].equals(table.getColumn("VERSION_STATUS_ID")));
        assertTrue("Column4", parsedColumns[3].equals(table.getColumn("TRADE_DATE")));
        assertTrue("Column5", parsedColumns[4].equals(table.getColumn("TRADE_TYPE_ID")));
        assertTrue("Column6", parsedColumns[5].equals(table.getColumn("INSTRUMENT_ID")));
        assertTrue("Column7", parsedColumns[6].equals(table.getColumn("COMMENTS")));
        assertTrue("Column8", parsedColumns[7].equals(table.getColumn("QUANTITY")));
        assertTrue("Column9", parsedColumns[8].equals(table.getColumn("COUNTERPARTY_ID")));
        assertTrue("Column10", parsedColumns[9].equals(table.getColumn("PRICE")));

        assertTrue("Column1", sql1.getColumnValue(parsedColumns[0]).equals("10234"));
        assertTrue("Column2", sql1.getColumnValue(parsedColumns[1]).equals("2"));
        assertTrue("Column3", sql1.getColumnValue(parsedColumns[2]).equals("1"));
        assertTrue("Column4", sql1.getColumnValue(parsedColumns[3]).equals("'Thu Feb 21 11:31:28 GMT 2002'"));
        assertTrue("Column5", sql1.getColumnValue(parsedColumns[4]).equals("4"));
        assertTrue("Column6", sql1.getColumnValue(parsedColumns[5]).equals("123124"));
        assertTrue("Column7", sql1.getColumnValue(parsedColumns[6]).equals("'Hello\n''You'' INSERT VALUES SELECT'"));
        assertTrue("Column8", sql1.getColumnValue(parsedColumns[7]).equals("3100000"));
        assertTrue("Column9", sql1.getColumnValue(parsedColumns[8]).equals("NULL"));
        assertTrue("Column10", sql1.getColumnValue(parsedColumns[9]).equals("101.4569"));

        assertTrue("Where clause", compareStrings(sql1.getWhereClause(), "TRADE.TRADE_ID=10234 AND TRADE.VERSION=2", true));
    }

    public void testInsertFromSelect() {
        StringBuffer statementString = new StringBuffer("INSERT TRADE SELECT * FROM TRADE WHERE TRADE.TRADE_ID = 34 AND TRADE.VERSION = 7");

        SQL sql1 = new SQL(statementString.toString());

        Column[] parsedColumns = (Column[]) sql1.getColumns().toArray(new Column[]{});
        Table table = Table.getInstance(CatalogSchemaTable.getInstance("TRADE", "demoDatabase"), "demoDatabase");
        assertTrue("Column1", parsedColumns[0].equals(table.getColumn("TRADE_ID")));
        assertTrue("Column2", parsedColumns[1].equals(table.getColumn("VERSION")));
        assertTrue("Column3", parsedColumns[2].equals(table.getColumn("VERSION_STATUS_ID")));
        assertTrue("Column4", parsedColumns[3].equals(table.getColumn("TRADE_DATE")));
        assertTrue("Column5", parsedColumns[4].equals(table.getColumn("TRADE_TYPE_ID")));
        assertTrue("Column6", parsedColumns[5].equals(table.getColumn("INSTRUMENT_ID")));
        assertTrue("Column6", parsedColumns[6].equals(table.getColumn("COMMENTS")));
        assertTrue("Column7", parsedColumns[7].equals(table.getColumn("COUNTERPARTY_ID")));
        assertTrue("Column8", parsedColumns[8].equals(table.getColumn("QUANTITY")));
        assertTrue("Column9", parsedColumns[9].equals(table.getColumn("PRICE")));

        assertTrue("Where clause", compareStrings(sql1.getWhereClause(), "TRADE.TRADE_ID = 34 AND TRADE.VERSION = 7"));
    }

    public void testUpdate() { //TODO
//      "UPDATE SHIVA..POSITION SET SHIVA..POSITION.current_position= -2000000.0 where ((SHIVA..POSITION.a = 3) AND ((SHIVA..POSITION.b =4) AND (SHIVA..POSITION.c=1)))"
//      "UPDATE SHIVA..POSITION SET current_position = 2000000.0, portfolio = 'WHERE \"ARE\" YOU FROM' WHERE SHIVA..POSITION.instrument_id = 2015633 AND SHIVA..POSITION.portfolio_id = 4319"
//      "UPDATE SHIVA..POSITION SET current_position= -2000000.0 WHERE SHIVA..POSITION.instrument_id = 2006197 AND SHIVA..POSITION.portfolio_id = 4532"
//      "UPDATE SHIVA..POSITION SET current_position = 'WHERE ''I'' AM COOL' WHERE SHIVA..POSITION.instrument_id = 2006197 AND SHIVA..POSITION.portfolio_id = 4532"
//      "UPDATE SHIVA..POSITION SET current_position = current_position * 2, portfolio = 3 WHERE SHIVA..POSITION.instrument_id = 2006197 AND SHIVA..POSITION.portfolio_id = 4532"

    }

    public void testDelete() { //TODO
        SQL sql1 = new SQL("DELETE FROM INSTRUMENT_RATING where INSTRUMENT_ID = 1056 AND RATING_ID = 23");
        SQL sql2 = new SQL("DELETE FROM INSTRUMENT_RATING where INSTRUMENT_ID.RATING_ID = 23");
        SQL sql3 = new SQL("DELETE FROM INSTRUMENT_RATING");
    }

    public void testJoinWithOrderBy() throws Exception {
        SQL sql1 = new SQL("SELECT * FROM BOND_FUTURE where BOND_FUTURE.LASTDELIVERYDATE = '2002-12-30' ORDER BY CONTRACTSIZE, FIRSTDELIVERYDATE");
        SQL sql2 = new SQL("SELECT * FROM BOND_FUTURE where BOND_FUTURE.INSTRUMENT_ID = 2 ORDER BY INSTRUMENT_ID");
        SQL sql3 = sql1.joinStatement(sql2);
        assertTrue("Where clause AND Order by join", compareStrings(sql3.getStatementString(), "SELECT * FROM BOND_FUTURE WHERE (BOND_FUTURE.LASTDELIVERYDATE = '2002-12-30') AND (BOND_FUTURE.INSTRUMENT_ID = 2) ORDER BY CONTRACTSIZE, FIRSTDELIVERYDATE, INSTRUMENT_ID"));
    }

    public void testJoinSelectWithUpdate() throws Exception {
        SQL sql1 = new SQL("SELECT * FROM POSITION2 where POSITION2.INSTRUMENT_ID = 1223");
        SQL sql2 = new SQL("UPDATE POSITION2 set QUANTITY = 102.45 where QUANTITY = 101 AND UPDATE_TIMESTAMP = '2002-12-30'");
        SQL sql3 = sql1.joinStatement(sql2);
        assertTrue("Select AND Update join", compareStrings(sql3.getStatementString(), "SELECT * FROM POSITION2 WHERE (POSITION2.INSTRUMENT_ID = 1223) AND (POSITION2.QUANTITY = 102.45 AND POSITION2.UPDATE_TIMESTAMP = '2002-12-30')"));
    }

    public void testJoinWithOrderByAndGroupBy() throws Exception {
        SQL sql1 = new SQL("SELECT * FROM BOND_FUTURE where BOND_FUTURE.INSTRUMENT_ID = 1 GROUP BY LASTDELIVERYDATE ORDER BY CONTRACTSIZE, FIRSTDELIVERYDATE");
        SQL sql2 = new SQL("SELECT * FROM BOND_FUTURE where BOND_FUTURE.INSTRUMENT_ID = 2 GROUP BY INSTRUMENT_ID ORDER BY INSTRUMENT_ID");
        SQL sql3 = sql1.joinStatement(sql2);
        assertTrue("Where clause AND Order by join", compareStrings(sql3.getStatementString(), "SELECT * FROM BOND_FUTURE WHERE (BOND_FUTURE.INSTRUMENT_ID = 1) AND (BOND_FUTURE.INSTRUMENT_ID = 2) GROUP BY LASTDELIVERYDATE, INSTRUMENT_ID ORDER BY CONTRACTSIZE, FIRSTDELIVERYDATE, INSTRUMENT_ID"));
    }

    public void testJoinWithGroupByAndHavingBy() throws Exception {
        SQL sql1 = new SQL("SELECT *, MAX(CONTRACTSIZE) FROM BOND_FUTURE GROUP BY FIRSTDELIVERYDATE, CONTRACTSIZE HAVING MAX(LASTDELIVERYDATE) > '2002-12-30'");
        SQL sql2 = new SQL("SELECT *, MAX(FIRSTDELIVERYDATE) FROM BOND_FUTURE GROUP BY INSTRUMENT_ID HAVING SUM(CONTRACTSIZE) > 5000");
        SQL sql3 = sql1.joinStatement(sql2);
        assertTrue("Group by AND Having", compareStrings(sql3.getStatementString(), "SELECT *, MAX(CONTRACTSIZE) FROM BOND_FUTURE GROUP BY FIRSTDELIVERYDATE, CONTRACTSIZE, INSTRUMENT_ID HAVING (MAX(LASTDELIVERYDATE) > '2002-12-30') AND (SUM(CONTRACTSIZE) > 5000)"));
    }

    public void testTruncate() {
        SQL sql1 = new SQL("truncate table TRADE");
        assertTrue(sql1.getOperationType() == SQL.TRUNCATE);
        assertTrue(sql1.getTables()[0].equals(CatalogSchemaTable.getInstance("TRADE", "demoDatabase")));
    }

    private static boolean compareStrings(String s1, String s2, boolean ignoreCase) {
        System.out.println("\ns1 = " + s1);
        System.out.println("s2 = " + s2);
        return ignoreCase ? Utilities.equalsIgnoreCase(s1, s2) : Utilities.equals(s1, s2);

    }

    private static boolean compareStrings(String s1, String s2) {
        return compareStrings(s1, s2, false);
    }
}
