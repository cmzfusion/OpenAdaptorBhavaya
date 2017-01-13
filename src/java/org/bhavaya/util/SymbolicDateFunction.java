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

package org.bhavaya.util;

import java.beans.Encoder;
import java.beans.Expression;
import java.sql.Date;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5.6.1 $
 */
public abstract class SymbolicDateFunction extends DateFunction {

    private static DateFunctionHandler dateFunctionHandler = new FuturesCTDDateFunctionHandler();

    public static SymbolicDateFunction TODAY_DATEFUNCTION = new TodayDateFunction();
    public static RelativeDateFunction YESTERDAY_DATEFUNCTION = new RelativeDateFunction(TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, RelativeDateFunction.PREPOSITION_BEFORE, 1);
    public static SymbolicDateFunction START_OF_WEEK_DATEFUNCTION = new OffSetDateFunction("W", "Start of week", Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    public static SymbolicDateFunction START_OF_MONTH_DATEFUNCTION = new OffSetDateFunction("M", "Start of month", Calendar.DAY_OF_MONTH, 1);
    public static SymbolicDateFunction START_OF_YEAR_DATEFUNCTION = new OffSetDateFunction("Y", "Start of year", Calendar.DAY_OF_YEAR, 1);
    public static SymbolicDateFunction TIME_BEGIN = new TimeBeginFunction();
    public static SymbolicDateFunction TIME_END = new TimeEndFunction();
    public static SymbolicDateFunction BOBL_CTD_MATURITY_DATEFUNCTION = new FutureCTDDateFunction("BOBL", "BOBL CTD Maturity", "BOBL CTD Maturity", dateFunctionHandler);
    public static SymbolicDateFunction BUND_CTD_MATURITY_DATEFUNCTION = new FutureCTDDateFunction("BUND", "BUND CTD Maturity", "BUND CTD Maturity", dateFunctionHandler);
    public static SymbolicDateFunction SCHATZ_CTD_MATURITY_DATEFUNCTION = new FutureCTDDateFunction("SCHATZ", "SCHATZ CTD Maturity", "SCHATZ CTD Maturity", dateFunctionHandler);

    private static final Map SYMBOLS = new LinkedHashMap();

    static {
        SYMBOLS.put(TODAY_DATEFUNCTION.getDescription(), TODAY_DATEFUNCTION);
        SYMBOLS.put("T-1d", YESTERDAY_DATEFUNCTION); // note, the description can be used in schema files for symbolic dates, so be wary when changing
        SYMBOLS.put(START_OF_WEEK_DATEFUNCTION.getDescription(), START_OF_WEEK_DATEFUNCTION);
        SYMBOLS.put(START_OF_MONTH_DATEFUNCTION.getDescription(), START_OF_MONTH_DATEFUNCTION);
        SYMBOLS.put(START_OF_YEAR_DATEFUNCTION.getDescription(), START_OF_YEAR_DATEFUNCTION);
        SYMBOLS.put(TIME_BEGIN.getDescription(), TIME_BEGIN);
        SYMBOLS.put(TIME_END.getDescription(), TIME_END);
        SYMBOLS.put(BOBL_CTD_MATURITY_DATEFUNCTION.getDescription(), BOBL_CTD_MATURITY_DATEFUNCTION);
        SYMBOLS.put(BUND_CTD_MATURITY_DATEFUNCTION.getDescription(), BUND_CTD_MATURITY_DATEFUNCTION);
        SYMBOLS.put(SCHATZ_CTD_MATURITY_DATEFUNCTION.getDescription(), SCHATZ_CTD_MATURITY_DATEFUNCTION);

        BhavayaPersistenceDelegate persistenceDelegate = new BhavayaPersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, SymbolicDateFunction.class, "getInstance", new Object[]{((SymbolicDateFunction) oldInstance).getDescription()});
            }
        };
        BeanUtilities.addPersistenceDelegate(SymbolicDateFunction.class, persistenceDelegate);
        BeanUtilities.addPersistenceDelegate(TodayDateFunction.class, persistenceDelegate);
        BeanUtilities.addPersistenceDelegate(TimeBeginFunction.class, persistenceDelegate);
        BeanUtilities.addPersistenceDelegate(TimeEndFunction.class, persistenceDelegate);
        BeanUtilities.addPersistenceDelegate(OffSetDateFunction.class, persistenceDelegate);
        BeanUtilities.addPersistenceDelegate(FutureCTDDateFunction.class, persistenceDelegate);
    }

    public static DateFunction getInstance(String description) {
        return (DateFunction) SYMBOLS.get(description);
    }

    public static DateFunction[] getInstances() {
        return (DateFunction[]) SYMBOLS.values().toArray(new DateFunction[SYMBOLS.values().size()]);
    }

    private SymbolicDateFunction(String description, String verboseDescription) {
        setDescription(description);
        setVerboseDescription(verboseDescription != null ? verboseDescription : description);
    }

    public String getSymbolName() {
        return getDescription();
    }

    public static class OffSetDateFunction extends SymbolicDateFunction {
        private int calendarFieldToSet;
        private int fieldValue;

        private OffSetDateFunction(String description, String verboseDescription, int calendarFieldToSet, int fieldValue) {
            super(description, verboseDescription);
            this.calendarFieldToSet = calendarFieldToSet;
            this.fieldValue = fieldValue;
        }

        public java.sql.Date getDate() {
            Calendar calendar = DateUtilities.newGmtCalendarForToday();
            calendar.set(calendarFieldToSet, fieldValue);
            return new java.sql.Date(calendar.getTimeInMillis());
        }

        public int getCalendarFieldToSet() {
            return calendarFieldToSet;
        }

        public int getFieldValue() {
            return fieldValue;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OffSetDateFunction)) return false;

            final OffSetDateFunction offSetDateFunction = (OffSetDateFunction) o;

            if (!getDescription().equals(offSetDateFunction.getDescription())) return false;
            if (calendarFieldToSet != offSetDateFunction.calendarFieldToSet) return false;
            if (fieldValue != offSetDateFunction.fieldValue) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = getDescription().hashCode();
            result = 29 * result + calendarFieldToSet;
            result = 29 * result + fieldValue;
            return result;
        }
    }

    public static class TodayDateFunction extends SymbolicDateFunction {
        private TodayDateFunction() {
            super("T", "Today");
        }

        public java.sql.Date getDate() {
            return DateUtilities.newDate();
        }

        public boolean equals(Object obj) {
            return obj instanceof TodayDateFunction;
        }

        public int hashCode() {
            return TodayDateFunction.class.hashCode();
        }
    }

    public static class TimeBeginFunction extends SymbolicDateFunction {
        private static final java.sql.Date date = new java.sql.Date(Long.MIN_VALUE);

        private TimeBeginFunction() {
            super("-infinity", "-infinity");
        }

        public java.sql.Date getDate() {
            return date;
        }

        public boolean equals(Object obj) {
            return obj instanceof TimeBeginFunction;
        }

        public int hashCode() {
            return TimeBeginFunction.class.hashCode();
        }
    }

    public static class TimeEndFunction extends SymbolicDateFunction {
        private static final java.sql.Date date = new java.sql.Date(Long.MAX_VALUE);

        private TimeEndFunction() {
            super("infinity", "infinity");
        }

        public java.sql.Date getDate() {
            return date;
        }

        public boolean equals(Object obj) {
            return obj instanceof TimeEndFunction;
        }

        public int hashCode() {
            return TimeEndFunction.class.hashCode();
        }
    }

    public static class FutureCTDDateFunction extends SymbolicDateFunction {
        private final String futureContract;
        private final DateFunctionHandler dateFunctionHandler;

        private FutureCTDDateFunction(String futureContract, String description, String verboseDesc, DateFunctionHandler dateHandler) {
            super(description, verboseDesc);
            this.futureContract = futureContract;
            this.dateFunctionHandler = dateHandler;
        }

        @Override
        public Date getDate() {
            java.util.Date date = dateFunctionHandler.getDate(futureContract);
            if( date != null ) {
                return new Date( date.getTime());
            }

            return DateUtilities.newDate();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FutureCTDDateFunction that = (FutureCTDDateFunction) o;

            if (!futureContract.equals(that.futureContract)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = futureContract.hashCode();
            return result;
        }
    }


    public static class FuturesCTDDateFunctionHandler implements DateFunctionHandler {
        DateFunctionHandler dateFunctionHandler;

        private FuturesCTDDateFunctionHandler() {
            initialiseDateFunctionHandler();
        }

        public java.util.Date getDate(Object arg) {
            java.util.Date retVal = null;
            if( dateFunctionHandler != null ) {
                retVal = dateFunctionHandler.getDate(arg);
            }

            if( retVal == null )
                retVal = DateUtilities.newDate();

            return retVal;
        }

        private void initialiseDateFunctionHandler() {
            String className = ApplicationProperties.getApplicationProperties().getProperty("futuresCTDDateHandler");
            if( className != null ) {
                try {
                    Class clasz = Class.forName(className);
                    dateFunctionHandler = (DateFunctionHandler)clasz.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
