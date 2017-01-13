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

package org.bhavaya.ui.series;

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.ui.table.DateSeries;
import org.bhavaya.util.*;

import java.beans.*;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */

public class DateSeriesNew implements Series {
    private boolean verbose = true;

    static {
        BeanUtilities.addPersistenceDelegate(DateSeriesNew.class, new BhavayaPersistenceDelegate() {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                DateSeriesNew dateSeries = (DateSeriesNew) oldInstance;
                for (int i = 1; i < dateSeries.getSize() - 1; i++) {
                    DateFunction dateFunction = dateSeries.getFunction(i);
                    out.writeStatement(new Statement(dateSeries, "addFunction", new Object[]{dateFunction}));
                }
            }
        });
    }

    public interface Listener {
        public void seriesChanged();
    }

    public static DateSeriesNew getDefaultInstance() {
        DateFunction today = SymbolicDateFunction.TODAY_DATEFUNCTION;

        DateSeriesNew dateSeries = new DateSeriesNew();
        dateSeries.addFunction(today);
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_DAYS, RelativeDateFunction.PREPOSITION_AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_WEEKS, RelativeDateFunction.PREPOSITION_AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_WEEKS, RelativeDateFunction.PREPOSITION_AFTER, 2));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_MONTHS, RelativeDateFunction.PREPOSITION_AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_MONTHS, RelativeDateFunction.PREPOSITION_AFTER, 3));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_MONTHS, RelativeDateFunction.PREPOSITION_AFTER, 6));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_MONTHS, RelativeDateFunction.PREPOSITION_AFTER, 9));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 1));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 2));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 5));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 10));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 20));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 50));
        dateSeries.addFunction(new RelativeDateFunction(today, RelativeDateFunction.OFFSET_TYPE_YEARS, RelativeDateFunction.PREPOSITION_AFTER, 100));

        return dateSeries;
    }

    public static final DateFunctionInterval ALL_TIME_INTERVAL = new DateFunctionInterval(SymbolicDateFunction.TIME_BEGIN, SymbolicDateFunction.TIME_END) {
        public String toString() {
            return Character.MAX_VALUE + "Total";
        }
    };

    private IndexedSet dateFunctions = new IndexedSet();
    private ArrayList listeners = new ArrayList();
    private PropertyChangeListener functionChangedListener;
    private boolean fullSeriesCalculated = false;
    private ArrayList allIntervals = new ArrayList();

    public DateSeriesNew() {
        functionChangedListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                fireUpdate();
            }
        };
    }

    public void addFunction(DateFunction dateFunction) {
        dateFunctions.add(dateFunction);
        Utilities.sort(dateFunctions);
        dateFunction.addPropertyChangeListener(functionChangedListener);
        fireUpdate();
    }

    public void removeFunction(DateFunction dateFunction) {
        dateFunctions.remove(dateFunction);
        dateFunction.removePropertyChangeListener(functionChangedListener);
        fireUpdate();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireUpdate() {
        fullSeriesCalculated = false;
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            Listener listener = (Listener) iterator.next();
            listener.seriesChanged();
        }
    }

    public int getSize() {
        return dateFunctions.size() + 2;
    }

    public DateFunction getFunction(int index) {
        if (index == 0) {
            return SymbolicDateFunction.TIME_BEGIN;
        } else if (index == dateFunctions.size() + 1) {
            return SymbolicDateFunction.TIME_END;
        } else {
            return (DateFunction) dateFunctions.get(index - 1);
        }
    }

    public DateFunctionInterval getInterval(java.util.Date date) {
        if (date == null) {
            return null;
        }

        Iterator iterator = dateFunctions.iterator();
        DateFunction startFunction = SymbolicDateFunction.TIME_BEGIN;
        Date rightDate = DateUtilities.newDate(date);
        while (iterator.hasNext()) {
            DateFunction endFunction = (DateFunction) iterator.next();
            Date leftDate = endFunction.getDate();
            if (rightDate.before(leftDate)) {
                return new RenderedInterval(startFunction, endFunction);
            } else {
                startFunction = endFunction;
            }
        }

        return new RenderedInterval(startFunction, SymbolicDateFunction.TIME_END);
    }


    /**
     * @param date
     * @return returns an array of intervals. This array consists of the interval containing the given date and the interval before this (if one exists)
     */
    public DateFunctionInterval[] getIntervals(java.util.Date date, boolean useSeriesSplitting) {
        if (date == null) {
            return null;
        }
        Date rightDate = DateUtilities.newDate(date);

        DateFunctionInterval[] intervals = new DateFunctionInterval[2];

        Iterator iterator = dateFunctions.iterator();
        DateFunction startFunction = SymbolicDateFunction.TIME_BEGIN;
        while (iterator.hasNext()) {
            DateFunction endFunction = (DateFunction) iterator.next();

            Date leftDate = endFunction.getDate();
            if (rightDate.before(leftDate)) {
                boolean hasOneInterval = (intervals[0] == null || !useSeriesSplitting);
                int index = hasOneInterval ? 0 : 1;
                intervals[index] = new RenderedInterval(startFunction, endFunction);
                return intervals;
            } else {
                intervals[0] = new RenderedInterval(startFunction, endFunction);
                startFunction = endFunction;
            }
        }

        //date did not fall into one of our intervals, therefore it must be in the "until end of time" interval
        RenderedInterval lastTimeBucket = new RenderedInterval(startFunction, SymbolicDateFunction.TIME_END);
        if (! useSeriesSplitting) { //just one interval please
            intervals[0] = lastTimeBucket;
        } else {
            intervals[1] = lastTimeBucket;
        }
        return intervals;
    }

    /**
     * @deprecated remove March 03.
     */
    public void setName(String name) {
    }

    /**
     * @deprecated remove March 03.
     */
    public String getName() {
        return null;
    }

    public void addFunction(DateSeries.DateFunction oldDateFunction) {
        addFunction(TemporarySeriesMigrationStrategy.convert(oldDateFunction));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateSeriesNew)) return false;

        final DateSeriesNew dateSeriesNew = (DateSeriesNew) o;

        if (verbose != dateSeriesNew.verbose) return false;
        if (!dateFunctions.equals(dateSeriesNew.dateFunctions)) return false;

        return true;
    }

    public Collection getFullSeriesValues() {
        if (!fullSeriesCalculated) recalculateFullSeries();
        return allIntervals;
    }

    private void recalculateFullSeries() {
        allIntervals.clear();

        for (int i = 0; i < DateSeriesNew.this.getSize() - 1; i++) {
            DateFunction start = DateSeriesNew.this.getFunction(i);
            DateFunction end = DateSeriesNew.this.getFunction(i + 1);
            allIntervals.add(new RenderedInterval(start, end));
            start = end;
        }
    }

    public Series.SeriesValue[] getSeriesValues(Object value, boolean useSeriesSplitting) {
        java.util.Date date = (java.util.Date) value;
        DateFunctionInterval[] intervals = getIntervals(date, useSeriesSplitting);
//todo:
        if (intervals == null) {
            return new SeriesValue[]{new SeriesValue(null, 1)};
        }

        if (intervals[1] == null) {
            return new SeriesValue[]{new SeriesValue(intervals[0], 1.0)};
        } else {
            SeriesValue[] values = new SeriesValue[2];

            DateFunctionInterval containingInterval = intervals[1];
            long intervalStart = containingInterval.getStartDateFunction().getDate().getTime();
            long dateValue = date.getTime();
            long intervalWidth = containingInterval.getEndDateFunction().getDate().getTime() - intervalStart;
            double participation = (double) (dateValue - intervalStart) / (double) intervalWidth;
            values[1] = new SeriesValue(containingInterval, participation);
            values[0] = new SeriesValue(intervals[0], 1.0 - participation);

            return values;
        }
    }

    public Class getSeriesValueClass() {
        return DateFunctionInterval.class;
    }

    public int hashCode() {
        int result;
        result = (verbose ? 1 : 0);
        result = 29 * result + dateFunctions.hashCode();
        return result;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private class RenderedInterval extends DateFunctionInterval {
        public RenderedInterval(DateFunction intervalBegin, DateFunction intervalEnd) {
            super(intervalBegin, intervalEnd);
        }

        public String toString() {
            return (verbose ? super.toString() : getEndDateFunction().getDescription());
        }
    }
}
