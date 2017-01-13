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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.9 $
 */
public class SQLFormatter {
    private static final Pattern singleQuotePattern = Pattern.compile("'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern replacePattern = Pattern.compile("\\?");

    private static final Map instances = new HashMap();

    private DatabaseDialect dialect;

    public static synchronized SQLFormatter getInstance(String dataSourceName) {
        if (dataSourceName == null) dataSourceName = DataSourceFactory.getDefaultDataSourceName();

        SQLFormatter instance = (SQLFormatter) instances.get(dataSourceName);
        if (instance == null) {
            instance = new SQLFormatter(dataSourceName);
            instances.put(dataSourceName, instance);
        }
        return instance;
    }

    private SQLFormatter(String dataSourceName) {
        this.dialect = (DataSourceFactory.getInstance(dataSourceName)).getDialect();
    }

    public String replace(CharSequence sql, Object value) {
        return replace(sql, value, false);
    }

    /**
     * Take care if you use this method in a loop. ? in the input sql are replaced by value,
     * but if value contains a ? then on the next iteration through the loop, value's inserted ?
     * will be replaced next, rather than the next ? in sql.
     *
     * @param sql
     * @param value
     * @param all
     * @return
     */
    public String replace(CharSequence sql, Object value, boolean all) {
        String str;
        String replacement = escapePatternMatchingChars(format(value));
        if (all) {
            str = replacePattern.matcher(sql).replaceAll(replacement);
        } else {
            str = replacePattern.matcher(sql).replaceFirst(replacement);
        }
        return str;
    }

    public StringBuffer replaceAll(CharSequence sql, Object... values) {
        return replace(sql, values);
    }

    public StringBuffer replace(CharSequence sql, Object[] values) {
        Matcher m = replacePattern.matcher(sql);
        StringBuffer replacedBuffer = new StringBuffer(sql.length());

        int i = 0;
        boolean result = m.find();
        while (result && i < values.length) {
            m.appendReplacement(replacedBuffer, escapePatternMatchingChars(format(values[i])));
            result = m.find();
            i++;
        }
        m.appendTail(replacedBuffer);
        return replacedBuffer;
    }

    public String replace(CharSequence sql, int value) {
        return replacePattern.matcher(sql).replaceFirst(format(value));
    }

    public String replace(CharSequence sql, long value) {
        return replacePattern.matcher(sql).replaceFirst(format(value));
    }

    public String replace(CharSequence sql, float value) {
        return replacePattern.matcher(sql).replaceFirst(format(value));
    }

    public String replace(CharSequence sql, double value) {
        return replacePattern.matcher(sql).replaceFirst(format(value));
    }

    public String format(boolean value) {
        return value ? "1" : "0";
    }

    public String format(int value) {
        return Integer.toString(value);
    }

    public String format(long value) {
        return Long.toString(value);
    }

    public String format(float value) {
        if (Float.isNaN(value)) return "null";
        return Float.toString(value);
    }

    public String format(double value) {
        if (Double.isNaN(value)) return "null";
        return Double.toString(value);
    }

    public String format(String value) {
        if (value == null) return "null";

        if (value.equalsIgnoreCase("null")) {
            return value;
        } else {
            // escape a single quote with another single quote
            Matcher singleQuotePatternMatcher = singleQuotePattern.matcher(value);
            value = singleQuotePatternMatcher.replaceAll("''");
            return "'" + value + "'";
        }
    }

    public String format(java.sql.Date value) {
        if (value == null) return "null";
        DateFormat dateFormat = dialect.getGmtDateFormat();
        return "\'" + dateFormat.format(value) + "\'";
    }

    public String format(java.util.Date value) {
        if (value == null) return "null";
        DateFormat dateFormat = dialect.getGmtDateTimeFormat();
        return "\'" + dateFormat.format(value) + "\'";
    }

    public String formatDate(java.util.Date value) {
        if (value == null) return "null";
        DateFormat dateFormat = dialect.getGmtDateFormat();
        return "\'" + dateFormat.format(value) + "\'";
    }

    public String format(Boolean value) {
        if (value == null) return "null";
        return value.booleanValue() ? "1" : "0";
    }

    public String format(Character value) {
        if (value == null) return "null";
        return "\'" + value.toString() + "\'";
    }

    public String format(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return format((String) value);
        if (value instanceof java.sql.Date) return format((java.sql.Date) value);
        if (value instanceof java.util.Date) return format((java.util.Date) value);
        if (value instanceof Boolean) return format((Boolean) value);
        if (value instanceof Character) return format((Character) value);
        if (value instanceof Double) return format(((Number) value).doubleValue());
        if (value instanceof Float) return format(((Number) value).floatValue());
        if (value instanceof Long) return format(((Number) value).longValue());
        if (value instanceof Integer) return format(((Number) value).intValue());
        return value.toString();
    }

    public java.util.Date getGmtDateFromString(String dateString) {
        if (dateString == null || dateString.equalsIgnoreCase("null")) return null;
        dateString = dateString.substring(1, dateString.length() - 1);
        DateFormat dateFormat = dialect.getGmtDateFormat(dateString);

        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Quick hack to allow the processing of $ in replacements Strings in Matcher
     *
     * @param input
     * @return
     */
    private static String escapePatternMatchingChars(String input) {
        return replaceStringWithString(input, "$", "\\$");
    }

    public static String replaceStringWithString(String input, String toReplace, String replacement) {
        StringBuffer output = new StringBuffer();
        int index = 0;
        int lastIndex = 0;
        while ((index = input.indexOf(toReplace, lastIndex)) != -1) {
            output.append(input.substring(lastIndex, index));
            output.append(replacement);
            lastIndex = index + toReplace.length();
        }
        output.append(input.substring(lastIndex));
        return output.toString();
    }
}
