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

package org.bhavaya.ui.table;

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.SimpleDateFormatThreadLocal;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.util.TimeZone;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class DateTimeRenderer extends DefaultTableCellRenderer {
    private SimpleDateFormatThreadLocal format;
    private String formatString;
    private TimeZone timezone;

    static {
        BeanUtilities.addPersistenceDelegate(DateTimeRenderer.class, new BhavayaPersistenceDelegate(new String[]{"formatString", "timezone"}));
    }

    public DateTimeRenderer(String formatString, TimeZone timezone) {
        format = new SimpleDateFormatThreadLocal(formatString, timezone);
        this.formatString = formatString;
        this.timezone = timezone;
    }

    public DateTimeRenderer(String formatString, String timezoneString) {
        this(formatString, TimeZone.getTimeZone(timezoneString));
    }

    public DateTimeRenderer(String formatString) {
        this(formatString, TimeZone.getDefault());
    }

    public String getFormatString() {
        return formatString;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public DateFormat getDateFormat() {
        return (DateFormat) format.get();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof java.util.Date) {
            component.setText(getDateFormat().format(value));
        }
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateTimeRenderer)) return false;

        final DateTimeRenderer dateTimeRenderer = (DateTimeRenderer) o;

        if (!formatString.equals(dateTimeRenderer.formatString)) return false;
        if (!timezone.equals(dateTimeRenderer.timezone)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = formatString.hashCode();
        result = 29 * result + timezone.hashCode();
        return result;
    }

    public String toString() {
        return formatString;
    }
}
