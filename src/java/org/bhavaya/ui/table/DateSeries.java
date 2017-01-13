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
import org.bhavaya.util.Utilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.beans.Encoder;
import java.beans.Statement;
import java.sql.Date;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */

public class DateSeries {
    static {
        BeanUtilities.addPersistenceDelegate(DateSeries.class, new BhavayaPersistenceDelegate() {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                DateSeries dateSeries = (DateSeries) oldInstance;
                for (int i = 0; i < dateSeries.getSize(); i++) {
                    DateFunction dateFunction = dateSeries.getFunction(i);
                    out.writeStatement(new Statement(dateSeries, "addFunction", new Object[]{dateFunction}));
                }
            }
        });
    }

    public interface Listener {
        public void seriesChanged();
    }

    public static abstract class DateFunction implements Comparable {
        public interface Listener {
            public void functionChanged();
        }

        // Used by the TableGrouper -  a maximal DateFunction called total.
        // We need this in the case where a user sorts a column of DateFunctions
        // with totaling enabled in the grouper.
        public static DateFunction TOTAL = new DateFunction() {
            public DateFunction copy() {
                return this;
            }

            public Date getDate() {
                return new Date(Long.MAX_VALUE);
            }

            public String toString() {
                return "Total";
            }
        };

        private String userDefinedAlias;
        private DateSeries dateSeries;
        private ArrayList listeners = new ArrayList();

        public abstract Date getDate();

        public int compareTo(Object o) {
            return getDate().compareTo(((DateFunction) o).getDate());
        }

        public String toString() {
            return getNaturalName();
        }

        public String getNaturalName() {
            return getDate() == null ? null : getDate().toString();
        }

        public void addListener(Listener listener) {
            listeners.add(listener);
        }

        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        public String getBucketStartString() {
            if (dateSeries != null) {
                int indexOfThis = dateSeries.indexOf(this);
                if (indexOfThis == 0) {
                    return "All prior dates";
                } else {
                    return dateSeries.getFunction(indexOfThis - 1).getAlias();
                }
            } else {
                return null;
            }
        }

        public String getBucketEndString() {
            return getAlias();
        }

        public void setDateSeries(DateSeries dateSeries) {
            this.dateSeries = dateSeries;
            fireUpdate();
        }

        public String getAlias() {
            return userDefinedAlias == null ? getNaturalName() : userDefinedAlias;
        }

        public void setAlias(String userDefinedAlias) {
            this.userDefinedAlias = userDefinedAlias == null ? null : userDefinedAlias.equals(getNaturalName()) ? null : userDefinedAlias;
            fireUpdate();
        }

        /**
         * A shocking departure from traditional OOP practices and a dark plunge back to the
         * days of procedural code.  This takes a bean of the same type and copies the
         * necessary properties.  What is really needed is and introspecting bean-cloner that
         * will make a new bean from an existing one using the public accessors.
         */
        public DateFunction inflateCopy(DateFunction dateFunction) {
            dateFunction.userDefinedAlias = userDefinedAlias;
            return dateFunction;
        }

        public abstract DateFunction copy();

        protected void fireUpdate() {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                Listener listener = (Listener) iterator.next();
                listener.functionChanged();
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof DateFunction)) {
                return false;
            }
            DateFunction df = (DateFunction) obj;
            return getAlias().equals(df.getAlias());
        }
    }

    public static class FixedDateFunction extends DateFunction {
        private Date fixedDate;

        public FixedDateFunction() {
        }

        public FixedDateFunction(java.sql.Date absoluteDate) {
            this.fixedDate = absoluteDate;
        }

        public FixedDateFunction(java.util.Date absoluteDate) {
            this.fixedDate = new Date(absoluteDate.getTime());
        }

        public Date getDate() {
            return fixedDate;
        }

        public void setDate(java.sql.Date fixedDate) {
            this.fixedDate = fixedDate;
            fireUpdate();
        }

        public void setDate(java.util.Date fixedDate) {
            this.fixedDate = new Date(fixedDate.getTime());
            fireUpdate();
        }

        public DateFunction inflateCopy(DateFunction dateFunction) {
            super.inflateCopy(dateFunction);
            ((FixedDateFunction) dateFunction).fixedDate = fixedDate;
            return dateFunction;
        }

        public DateFunction copy() {
            return inflateCopy(new FixedDateFunction());
        }

        public boolean equals(Object obj) {
            boolean equals = super.equals(obj);

            if (obj instanceof FixedDateFunction) {
                FixedDateFunction fdf = (FixedDateFunction) obj;
                equals &= fixedDate == null ? fdf.getDate() == null : fixedDate.equals(fdf.fixedDate);
            } else {
                equals = false;
            }

            return equals;
        }
    }

    public static class RelativeDateFunction extends DateFunction {
        public static final String BEFORE = "before";
        public static final String AFTER = "after";

        public static final String DAYS = "Days";
        public static final String WEEKS = "Weeks";
        public static final String MONTHS = "Months";
        public static final String YEARS = "Years";

        private final static Map OFFSET_TYPES = new HashMap();
        private final static Set OFFSET_TYPES_ORDERED = new LinkedHashSet();

        static {
            OFFSET_TYPES.put(DAYS, new Integer(Calendar.DAY_OF_YEAR));
            OFFSET_TYPES.put(WEEKS, new Integer(Calendar.WEEK_OF_YEAR));
            OFFSET_TYPES.put(MONTHS, new Integer(Calendar.MONTH));
            OFFSET_TYPES.put(YEARS, new Integer(Calendar.YEAR));
            OFFSET_TYPES_ORDERED.add(DAYS);
            OFFSET_TYPES_ORDERED.add(WEEKS);
            OFFSET_TYPES_ORDERED.add(MONTHS);
            OFFSET_TYPES_ORDERED.add(YEARS);
        }

        public static final Set getOffsetTypes() {
            return OFFSET_TYPES_ORDERED;
        }

        private DateFunction referenceDate = SymbolicDateFunction.getInstance(SymbolicDateFunction.TODAY);
        private String calendarOffsetType = DAYS;
        private int offset;
        private String preposition = AFTER;

        public RelativeDateFunction() {
        }

        public RelativeDateFunction(DateFunction referenceDate, String calendarOffsetType, String preposition, int offset) {
            this.referenceDate = referenceDate;
            this.calendarOffsetType = calendarOffsetType;
            this.preposition = preposition;
            this.offset = offset;
        }

        public Date getDate() {
            int multiplier = preposition.equals(AFTER) ? 1 : -1;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(referenceDate.getDate());
            cal.add(((Integer) OFFSET_TYPES.get(calendarOffsetType)).intValue(), multiplier * Math.abs(offset));
            return new Date(cal.getTime().getTime());
        }

        public int getOffset() {
            return Math.abs(offset);
        }

        public void setOffset(int offset) {
            this.offset = offset;
            fireUpdate();
        }

        public String getCalendarOffsetType() {
            return calendarOffsetType;
        }

        public void setCalendarOffsetType(String calendarOffsetType) {
            this.calendarOffsetType = calendarOffsetType;
            fireUpdate();
        }

        public DateFunction getReferenceDate() {
            return referenceDate;
        }

        public void setReferenceDate(DateFunction referenceDate) {
            this.referenceDate = referenceDate;
            fireUpdate();
        }

        public void setPreposition(String prepositionType) {
            this.preposition = prepositionType;
            fireUpdate();
        }

        public String getNaturalName() {
            return new StringBuffer(Integer.toString(Math.abs(offset))).append(" ").append(calendarOffsetType.toLowerCase()).
                    append(" ").append(getPreposition().toLowerCase()).append(" '").append(referenceDate.getAlias()).append("'").toString();
        }

        public String getPreposition() {
            return preposition;
        }

        public DateFunction inflateCopy(DateFunction dateFunction) {
            super.inflateCopy(dateFunction);
            RelativeDateFunction relativeDateFunction = (RelativeDateFunction) dateFunction;
            relativeDateFunction.referenceDate = referenceDate;
            relativeDateFunction.preposition = preposition;
            relativeDateFunction.offset = offset;
            relativeDateFunction.calendarOffsetType = calendarOffsetType;
            return dateFunction;
        }

        public DateFunction copy() {
            return inflateCopy(new RelativeDateFunction());
        }

        public boolean equals(Object obj) {
            boolean equals = super.equals(obj);

            if (obj instanceof RelativeDateFunction) {
                RelativeDateFunction rdf = (RelativeDateFunction) obj;
                equals &= referenceDate == null ? rdf.referenceDate == null : referenceDate.equals(rdf.referenceDate);
                equals &= calendarOffsetType == null ? rdf.referenceDate == null : calendarOffsetType.equals(rdf.calendarOffsetType);
                equals &= preposition == null ? rdf.preposition == null : preposition.equals(rdf.preposition);
                equals &= offset == rdf.offset;
            } else {
                equals = false;
            }

            return equals;
        }
    }

    public static class SymbolicDateFunction extends DateFunction {
        public static final String TODAY = "Today";
        public static final String THIS_WEEK = "Week-begin";
        public static final String THIS_MONTH = "Month-begin";
        public static final String THIS_YEAR = "Year-begin";

        private static DateFunction TODAY_DATEFUNCTION = new TodayDateFunction();
        private static DateFunction THIS_WEEK_DATEFUNCTION = new SymbolicDateFunction(THIS_WEEK, Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        private static DateFunction THIS_MONTH_DATEFUNCTION = new SymbolicDateFunction(THIS_MONTH, Calendar.DAY_OF_MONTH, 1);
        private static DateFunction THIS_YEAR_DATEFUNCTION = new SymbolicDateFunction(THIS_YEAR, Calendar.DAY_OF_YEAR, 1);

        private static final Map SYMBOLS = new HashMap();
        private static final Set ORDERED_SYMBOL_LIST = new LinkedHashSet();

        static {
            SYMBOLS.put(TODAY, TODAY_DATEFUNCTION);
            SYMBOLS.put(THIS_WEEK, THIS_WEEK_DATEFUNCTION);
            SYMBOLS.put(THIS_MONTH, THIS_MONTH_DATEFUNCTION);
            SYMBOLS.put(THIS_YEAR, THIS_YEAR_DATEFUNCTION);
            ORDERED_SYMBOL_LIST.add(TODAY);
            ORDERED_SYMBOL_LIST.add(THIS_WEEK);
            ORDERED_SYMBOL_LIST.add(THIS_MONTH);
            ORDERED_SYMBOL_LIST.add(THIS_YEAR);
        }

        public static final Set getAvailableSymbols() {
            return ORDERED_SYMBOL_LIST;
        }

        public static final SymbolicDateFunction getInstance(String symbolKey) {
            return (SymbolicDateFunction) SYMBOLS.get(symbolKey);
        }

        public static final SymbolicDateFunction getDefaultInstance() {
            return (SymbolicDateFunction) TODAY_DATEFUNCTION;
        }

        private String symbolName = "";
        private int calendarFieldToSet;
        private int fieldValue;

        public SymbolicDateFunction() {
        }

        private SymbolicDateFunction(String symbolName) {
            this.symbolName = symbolName;
        }

        private SymbolicDateFunction(String symbolName, int calendarFieldToSet, int fieldValue) {
            this.symbolName = symbolName;
            this.calendarFieldToSet = calendarFieldToSet;
            this.fieldValue = fieldValue;
        }

        public Date getDate() {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(calendarFieldToSet, fieldValue);
            return new Date(calendar.getTimeInMillis());
        }

        public String getSymbolName() {
            return symbolName;
        }

        public int getCalendarFieldToSet() {
            return calendarFieldToSet;
        }

        public void setCalendarFieldToSet(int calendarFieldToSet) {
            this.calendarFieldToSet = calendarFieldToSet;
        }

        public int getFieldValue() {
            return fieldValue;
        }

        public void setFieldValue(int fieldValue) {
            this.fieldValue = fieldValue;
        }

        public void setSymbolName(String symbolName) {
            this.symbolName = symbolName;
        }

        public String getNaturalName() {
            return Utilities.capitalise(symbolName.toLowerCase());
        }

        public DateFunction inflateCopy(DateFunction dateFunction) {
            super.inflateCopy(dateFunction);
            SymbolicDateFunction symbolicDateFunction = (SymbolicDateFunction) dateFunction;
            symbolicDateFunction.symbolName = symbolName;
            symbolicDateFunction.calendarFieldToSet = calendarFieldToSet;
            symbolicDateFunction.fieldValue = fieldValue;
            return dateFunction;
        }

        public DateFunction copy() {
            return inflateCopy(new SymbolicDateFunction());
        }

        public boolean equals(Object obj) {
            boolean equals = super.equals(obj);

            if (obj instanceof SymbolicDateFunction) {
                SymbolicDateFunction sdf = (SymbolicDateFunction) obj;
                equals &= symbolName == null ? sdf.symbolName == null : symbolName.equals(sdf.symbolName);
                equals &= calendarFieldToSet == sdf.calendarFieldToSet;
                equals &= fieldValue == sdf.fieldValue;
            } else {
                equals = false;
            }

            return equals;
        }

        public static class TodayDateFunction extends SymbolicDateFunction {
            public TodayDateFunction() {
                super(TODAY);
            }

            public Date getDate() {
                return new Date(System.currentTimeMillis());
            }

            public boolean equals(Object obj) {
                return obj instanceof TodayDateFunction;
            }

            public DateFunction copy() {
                return new TodayDateFunction();
            }
        }
    }

    public static DateSeries getDefaultInstance() {
        DateFunction today = SymbolicDateFunction.TODAY_DATEFUNCTION;

        DateSeries dateSeries = new DateSeries();
        dateSeries.setName("Default Date Series");
        dateSeries.addFunction(today);
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.DAYS, RelativeDateFunction.AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.WEEKS, RelativeDateFunction.AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.WEEKS, RelativeDateFunction.AFTER, 2));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.MONTHS, RelativeDateFunction.AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.MONTHS, RelativeDateFunction.AFTER, 3));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.MONTHS, RelativeDateFunction.AFTER, 6));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.MONTHS, RelativeDateFunction.AFTER, 9));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 2));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 5));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 10));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 20));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 50));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.YEARS, RelativeDateFunction.AFTER, 100));

        return dateSeries;
    }

    private String name;
    private List dateFunctions = new ArrayList();
    private ArrayList listeners = new ArrayList();

    public DateSeries() {
    }

    public void addFunction(DateFunction dateFunction) {
        dateFunction.setDateSeries(this);
        dateFunctions.add(dateFunction);
        Utilities.sort(dateFunctions);
        fireUpdate();
    }

    public void removeFunction(DateFunction dateFunction) {
        dateFunction.setDateSeries(null);
        dateFunctions.remove(dateFunction);
        fireUpdate();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireUpdate() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            Listener listener = (Listener) iterator.next();
            listener.seriesChanged();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return dateFunctions.size();
    }

    public DateFunction getFunction(int index) {
        return (DateFunction) dateFunctions.get(index);
    }

    public List getFunctions() {
        return dateFunctions;
    }

    public int indexOf(DateFunction dateFunction) {
        return dateFunctions.indexOf(dateFunction);
    }

    public DateFunction getDateCategory(java.util.Date date) {
        if (date == null) {
            return null;
        }

        for (Iterator iterator = dateFunctions.iterator(); iterator.hasNext();) {
            DateFunction function = (DateFunction) iterator.next();
            if (function.getDate().after(date)) {
                return function;
            }
        }

        return null;
    }

    public DateSeries inflateClone(DateSeries dateSeries) {
        dateSeries.name = name;
        dateSeries.dateFunctions = new ArrayList(dateFunctions.size());
        for (Iterator iterator = dateFunctions.iterator(); iterator.hasNext();) {
            DateFunction function = ((DateFunction) iterator.next()).copy();
            function.setDateSeries(dateSeries);
            dateSeries.dateFunctions.add(function);
        }
        return dateSeries;
    }

    public DateSeries copy() {
        return inflateClone(new DateSeries());
    }

    public boolean equals(Object obj) {

        DateSeries ds = (DateSeries) obj;
        boolean equals = ds.getName() == null ? name == null : ds.getName().equals(name);

        if (dateFunctions.size() == ds.getSize()) {
            for (int i = 0; i < getSize(); i++) {
                equals &= dateFunctions.get(i).equals(ds.dateFunctions.get(i));
            }
        } else {
            equals = false;
        }

        return equals;
    }
}
