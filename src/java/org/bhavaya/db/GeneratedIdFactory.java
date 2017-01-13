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

import org.bhavaya.util.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class GeneratedIdFactory implements IdFactory {
    private static final Log log = Log.getCategory(GeneratedIdFactory.class);
    private static final Log sqlLog = Log.getCategory("sql");

    public boolean isGeneratedPreInsert() {
        return false;
    }

    public boolean isGeneratedPostInsert() {
        return true;
    }

    public Object getInsertValue(Object bean, TableColumn column, Persister persister) throws SQLException {
        String generatedValueSql = getPreparedSelectPostGeneratedValuesSql(column);
        log.info("Executing sql: " + generatedValueSql);
        sqlLog.info(generatedValueSql);

        Statement generatedValuesStatement = null;
        ResultSet generatedValuesResultSet = null;
        try {
            generatedValuesStatement = persister.getConnection().createStatement();
            generatedValuesResultSet = generatedValuesStatement.executeQuery(generatedValueSql);
            if (generatedValuesResultSet.next()) {
                Object columnValue = DBUtilities.getObjectFromResultSet(1, generatedValuesResultSet);
                return columnValue;
            }
        } finally {
            DBUtilities.closeResultSetAndStatement(generatedValuesResultSet, generatedValuesStatement);
        }
        return null;
    }

    private static String getPreparedSelectPostGeneratedValuesSql(TableColumn column) {
        return "SELECT MAX(" + column.getName() + ") FROM " + column.getCatalogSchemaTable().getTableRepresentation();
    }
}
