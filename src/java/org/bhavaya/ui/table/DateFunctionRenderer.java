package org.bhavaya.ui.table;

import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.util.TimeZone;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class DateFunctionRenderer extends DefaultTableCellRenderer {
    private SimpleDateFormatThreadLocal format;
    private String formatString;
    private TimeZone timezone;

    static {
        BeanUtilities.addPersistenceDelegate(DateFunctionRenderer.class, new BhavayaPersistenceDelegate(new String[]{"formatString", "timezone"}));
    }

    public DateFunctionRenderer(String formatString, TimeZone timezone) {
        format = new SimpleDateFormatThreadLocal(formatString, timezone);
        this.formatString = formatString;
        this.timezone = timezone;
    }

    public DateFunctionRenderer(String formatString, String timezoneString) {
        this(formatString, TimeZone.getTimeZone(timezoneString));
    }

    public DateFunctionRenderer(String formatString) {
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
        if (value instanceof FixedDateFunction) {
            component.setText(getDateFormat().format(((DateFunction) value).getDate()));
        }
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateFunctionRenderer)) return false;

        final DateFunctionRenderer dateTimeRenderer = (DateFunctionRenderer) o;

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
