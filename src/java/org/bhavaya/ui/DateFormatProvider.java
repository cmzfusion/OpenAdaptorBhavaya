package org.bhavaya.ui;

import org.bhavaya.util.Configuration;
import org.bhavaya.util.DefaultObservable;
import org.bhavaya.util.Task;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Calendar;

/**
 * @author <a href="mailto:Sabine.Haas@dkib.com">Sabine Haas, Dresdner Kleinwort</a>
 */
public class DateFormatProvider extends DefaultObservable {
    private static final String DATE_FORMAT = "DateFormat";
    public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";

    public static final DateFormatHelper[] DATE_FORMATS = {new DateFormatHelper("DD/MM/YY", "dd/MM/yy"),
            new DateFormatHelper("DD/MM/YYYY", "dd/MM/yyyy"),
            new DateFormatHelper("MM/DD/YY", "MM/dd/yy"),
            new DateFormatHelper("MM/DD/YYYY", "MM/dd/yyyy"),
            new DateFormatHelper("DD-MMM-YY", "dd-MMM-yy"),
            new DateFormatHelper("DD-MMM-YYYY", "dd-MMM-yyyy"),
            new DateFormatHelper("DD-MARCH-YYYY", "dd-MMMM-yyyy")};

    static {
        Configuration.addSaveTask(new Task("Saving DateFormat") {
            public void run() throws AbortTaskException, Throwable {
                Configuration.getRoot().putObject(DATE_FORMAT, getInstance().getDateFormat());
            }
        });
    }

    private static DateFormatProvider instance;
    private String dateFormat = DEFAULT_DATE_FORMAT;

    public static DateFormatProvider getInstance() {
        if (instance == null) {
            instance = new DateFormatProvider();
            instance.setDateFormat((String)Configuration.getRoot().getObject(DATE_FORMAT, instance.getDateFormat(), String.class));
            //todo drop this in the future; After the first migration value will be null and therefore allways the new value is used
            migrateOldDateFormatFromTableView();
        }
        return instance;
    }

    private static void migrateOldDateFormatFromTableView() {
        instance.setDateFormat((String)Configuration.getRoot().getConfiguration("GlobalTableSettings").getObject(DATE_FORMAT, instance.getDateFormat(), String.class));
    }

    public String getDateFormatString(int initialCalendarField) {
        String dateFormat = null;
        if (initialCalendarField == Calendar.YEAR) {
            dateFormat = "yyyy";
        } else if (initialCalendarField == Calendar.MONTH) {
            dateFormat = "MM/yyyy";
        } else if (initialCalendarField == Calendar.DAY_OF_MONTH ||
                initialCalendarField == Calendar.DAY_OF_WEEK ||
                initialCalendarField == Calendar.DAY_OF_WEEK_IN_MONTH ||
                initialCalendarField == Calendar.DAY_OF_YEAR) {
            dateFormat = this.dateFormat;
        } else if (initialCalendarField == Calendar.MINUTE) {
            dateFormat = this.dateFormat + " HH:mm";
        } else {
            dateFormat = this.dateFormat + " HH:mm:ss";
        }
        return dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        String oldValue = this.dateFormat;
        this.dateFormat = dateFormat;
        firePropertyChange("dateFormat", oldValue, this.dateFormat);
    }

    public static final class DateFormatHelper {
        private String displayName;
        private String dateFormat;

        public DateFormatHelper(String displayName, String dateFormat) {
            this.displayName = displayName;
            this.dateFormat = dateFormat;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public String toString() {
            return displayName;
        }
    }

    public static final class SetDefaultDateFormatAction extends AbstractAction {
        private DateFormatHelper dateFormat;

        public SetDefaultDateFormatAction(DateFormatHelper dateFormatHelper) {
            super(dateFormatHelper.getDisplayName());
            this.dateFormat = dateFormatHelper;
        }

        public void actionPerformed(ActionEvent e) {
            DateFormatProvider.getInstance().setDateFormat(this.dateFormat.getDateFormat());
        }
    }
}
