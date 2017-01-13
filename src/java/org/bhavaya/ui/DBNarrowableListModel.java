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

package org.bhavaya.ui;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.util.Log;

import java.util.Collection;

/**
 * Provides a database backend the NarrowableComboBox to allow the narrowing on a certain column on a certain table.
 *
 * @author Brendon McLean
 * @author DanVan
 * @version $Revision: 1.5 $
 */
public class DBNarrowableListModel extends CachedDataSourceNarrowableListModel {
    private static final Log log = Log.getCategory(DBNarrowableListModel.class);

    public DBNarrowableListModel(String dataSourceName, String selectStatement) {
        this(dataSourceName, selectStatement, false);
    }

    public DBNarrowableListModel(String dataSourceName, String selectStatement, boolean emptyStringIsAllData) {
        super(new DBDataQuery(dataSourceName, selectStatement), emptyStringIsAllData);
    }

    public DBNarrowableListModel(String dataSourceName, String selectStatement, String favouriteStatement) {
        super(new DBDataQuery(dataSourceName, selectStatement), new DBDataQuery(dataSourceName, favouriteStatement));
    }

    private static class DBDataQuery implements CachedDataSourceNarrowableListModel.DataQuery {
        private String dataSourceName;
        private String selectStatement;

        public DBDataQuery(String dataSourceName, String selectStatement) {
            this.dataSourceName = dataSourceName;
            this.selectStatement = selectStatement;
        }

        public Collection execAndGetCollection(String searchString) {
            String query = SQLFormatter.getInstance(dataSourceName).replace(selectStatement, searchString, true);
            if (log.isDebug()) log.debug("Firing query: " + query);
            long time = System.currentTimeMillis();
            Collection results = DBUtilities.execute(dataSourceName, query);
            long endTime = System.currentTimeMillis();
            if (log.isDebug()) log.debug("Got updated data for combo (" + results.size() + " rows) in: " + (endTime - time) + "millis after requesting it. SQL=" + query);
            return results;
        }

        public Object getQueryKey() {
            return selectStatement;
        }
    }
}
