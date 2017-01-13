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

import org.bhavaya.beans.Column;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class StoredProcedure {
    private String statement;
    private String storedProcedure;
    private List parameters = new ArrayList();
    private SQLFormatter sqlFormatter;

    public StoredProcedure(String statement, String dataSourceName) {
        this.statement = ApplicationProperties.substituteApplicationProperties(statement);
        parseStoredProcedure(SQL.tokenizeStatement(statement));
        sqlFormatter = SQLFormatter.getInstance(dataSourceName);
    }

    private void parseStoredProcedure(String[] tokens) {
        storedProcedure = tokens[0];

        for (int position = 1; position < tokens.length; position++) {
            String parameter = null;
            for (; position < tokens.length; position++) {
                String token = tokens[position];
                if (token.equalsIgnoreCase(",")) break;
                parameter = (parameter == null ? token : parameter + token);
            }
            parameters.add(parameter);
        }
    }

    public String getStoredProcedureForKey(Column[] keyColumns, Object key) {
        if (key == null) return storedProcedure;

        int numberOfKeyComponents = keyColumns.length;
        StringBuffer storedProcedureBuffer = new StringBuffer(numberOfKeyComponents * 40);
        storedProcedureBuffer.append(storedProcedure);

        if (numberOfKeyComponents == 1) {
            storedProcedureBuffer.append(' ').append(sqlFormatter.format(key));
        } else {
            List keyList = (List) key;
            for (int j = 0; j < parameters.size(); j++) {
                String parameter = (String) parameters.get(j);

                for (int i = 0; i < keyColumns.length; i++) {
                    Column keyColumn = keyColumns[i];
                    if (Utilities.equals(parameter, keyColumn.getName())) {
                        Object keyComponent = keyList.get(i);
                        storedProcedureBuffer.append(' ').append(sqlFormatter.format(keyComponent));
                    }
                }
            }
        }

        return storedProcedureBuffer.toString();
    }

    public String toString() {
        return statement;
    }

    public boolean equals(Object object) {
        return statement.equals(object);
    }

    public int hashCode() {
        return statement.hashCode();
    }
}

