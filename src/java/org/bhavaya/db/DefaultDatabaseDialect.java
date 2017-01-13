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

import org.bhavaya.util.SimpleDateFormatThreadLocal;
import org.bhavaya.beans.Column;

import javax.naming.OperationNotSupportedException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class DefaultDatabaseDialect implements DatabaseDialect {
    private static final SimpleDateFormatThreadLocal SQL_DATE_FORMAT = new SimpleDateFormatThreadLocal("yyyy-MM-dd", TimeZone.getTimeZone("GMT"));
    private static final SimpleDateFormatThreadLocal SQL_DATETIME_FORMAT = new SimpleDateFormatThreadLocal("yyyy-MM-dd HH:mm:ss:SSS", TimeZone.getTimeZone("GMT"));
    protected static int localTempTableCount = 0;

    private Map gmtDateFormatsByPattern;

    public DefaultDatabaseDialect() {
    }

    protected void initPatterns() {
        addPattern("yyyy-MM-dd HH:mm:ss:SSS");
        addPattern("yyyy-MM-dd HH:mm:ss.SSS");
        addPattern("yyyy-MM-dd HH:mm:ss.SS");
        addPattern("yyyy-MM-dd HH:mm:ss.S");
        addPattern("yyyy-MM-dd HH:mm:ss");
        addPattern("yyyyMMdd HH:mm:ss:SSS");
        addPattern("yyyyMMdd HH:mm:ss.SSS");
        addPattern("yyyyMMdd HH:mm:ss.SS");
        addPattern("yyyyMMdd HH:mm:ss.S");
        addPattern("yyyyMMdd HH:mm:ss");
        addPattern("yyyy-MM-dd");
        addPattern("yyyyMMdd");
        addPattern("dd MMM yyyy HH:mm:ss:SSS");
        addPattern("dd MMM yyyy HH:mm:ss.SSS");
        addPattern("dd MMM yyyy HH:mm:ss.SS");
        addPattern("dd MMM yyyy HH:mm:ss.S");
        addPattern("dd MMM yyyy HH:mm:ss");
    }

    protected void addPattern(String datePattern) {
        Pattern pattern = toRegExPattern(datePattern);
        gmtDateFormatsByPattern.put(pattern, new SimpleDateFormatThreadLocal(datePattern, TimeZone.getTimeZone("GMT")));
    }

    protected void addPattern(String datePattern, String realDatePattern) {
        Pattern pattern = toRegExPattern(datePattern);
        gmtDateFormatsByPattern.put(pattern, new SimpleDateFormatThreadLocal(realDatePattern, TimeZone.getTimeZone("GMT")));
    }

    protected static Pattern toRegExPattern(String datePattern) {
        String regExPattern = datePattern;
        regExPattern = regExPattern.replaceAll("d", "\\\\w"); // this one must be first
        regExPattern = regExPattern.replaceAll("y", "\\\\d");
        regExPattern = regExPattern.replaceAll("M", "\\\\w");
        regExPattern = regExPattern.replaceAll("m", "\\\\d");
        regExPattern = regExPattern.replaceAll("H", "\\\\d");
        regExPattern = regExPattern.replaceAll("s", "\\\\d");
        regExPattern = regExPattern.replaceAll("S", "\\\\d");
        regExPattern = regExPattern.replaceAll("\\-", "\\\\-");
        regExPattern = regExPattern.replaceAll("\\:", "\\\\:");
        regExPattern = regExPattern.replaceAll("\\.", "\\\\.");
        Pattern pattern = Pattern.compile(regExPattern);
        return pattern;
    }

    public void configure(Connection connection) throws SQLException {
    }

    public DateFormat getGmtDateFormat() {
        return (DateFormat) SQL_DATE_FORMAT.get();
    }

    public DateFormat getGmtDateTimeFormat() {
        return (DateFormat) SQL_DATETIME_FORMAT.get();
    }

    public DateFormat getGmtDateFormat(String dateString) {
        synchronized (this) {
            if (gmtDateFormatsByPattern == null) {
                gmtDateFormatsByPattern = new LinkedHashMap();
                initPatterns();
            }
        }

        for (Iterator iterator = gmtDateFormatsByPattern.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Pattern pattern = (Pattern) entry.getKey();
            if (pattern.matcher(dateString).matches()) {
                SimpleDateFormatThreadLocal dateFormatThreadLocal = (SimpleDateFormatThreadLocal) entry.getValue();
                return (DateFormat) dateFormatThreadLocal.get();
            }
        }
        return null;
    }

    public String getConnectionId(Connection connection) {
        return connection.toString();
    }

    public String transformSelectSql(String sql) {
        return sql;
    }

    public String createTempTable(Connection connection, Column[] columns, boolean createWithPrimaryKey) throws Exception {
        throw new OperationNotSupportedException("Temporary tables not supported in dialect: " + this);
    }

    public boolean containsPrivateTempTable(SQL sqlToExecute) {
        return false;
    }
}
