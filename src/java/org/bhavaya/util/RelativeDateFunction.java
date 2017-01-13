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
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */
public class RelativeDateFunction extends DateFunction {
    public static final Preposition PREPOSITION_BEFORE = new Preposition("-", "before");
    public static final Preposition PREPOSITION_AFTER = new Preposition("+", "after");

    public static final OffsetType OFFSET_TYPE_DAYS = new OffsetType("d", "days", Calendar.DAY_OF_YEAR);
    public static final OffsetType OFFSET_TYPE_WEEKS = new OffsetType("w", "weeks", Calendar.WEEK_OF_YEAR);
    public static final OffsetType OFFSET_TYPE_MONTHS = new OffsetType("m", "months", Calendar.MONTH);
    public static final OffsetType OFFSET_TYPE_YEARS = new OffsetType("y", "years", Calendar.YEAR);

    private final static Map OFFSET_TYPES = new LinkedHashMap();
    private final static Map PREPOSITIONS = new LinkedHashMap();

    static {
        OFFSET_TYPES.put(OFFSET_TYPE_DAYS.getCode(), OFFSET_TYPE_DAYS);
        OFFSET_TYPES.put(OFFSET_TYPE_WEEKS.getCode(), OFFSET_TYPE_WEEKS);
        OFFSET_TYPES.put(OFFSET_TYPE_MONTHS.getCode(), OFFSET_TYPE_MONTHS);
        OFFSET_TYPES.put(OFFSET_TYPE_YEARS.getCode(), OFFSET_TYPE_YEARS);

        PREPOSITIONS.put(PREPOSITION_BEFORE.getCode(), PREPOSITION_BEFORE);
        PREPOSITIONS.put(PREPOSITION_AFTER.getCode(), PREPOSITION_AFTER);

        BeanUtilities.addPersistenceDelegate(RelativeDateFunction.class, new BhavayaPersistenceDelegate(new String[]{"referenceDate", "calendarOffsetType", "preposition", "offset"}));
        BeanUtilities.addPersistenceDelegate(OffsetType.class, new BhavayaPersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, RelativeDateFunction.class, "getCalendarOffsetType", new Object[]{((OffsetType) oldInstance).getCode()});
            }
        });
        BeanUtilities.addPersistenceDelegate(Preposition.class, new BhavayaPersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, RelativeDateFunction.class, "getPreposition", new Object[]{((Preposition) oldInstance).getCode()});
            }
        });
    }

    private DateFunction referenceDate;
    private OffsetType calendarOffsetType;
    private int offset;
    private Preposition preposition;
    private java.sql.Date oldDate;

    public static OffsetType[] getCalendarOffsetTypes() {
        return (OffsetType[]) OFFSET_TYPES.values().toArray(new OffsetType[OFFSET_TYPES.values().size()]);
    }

    public static OffsetType getCalendarOffsetType(String calendarOffsetType) {
        return (OffsetType) OFFSET_TYPES.get(calendarOffsetType);
    }

    public static Preposition[] getPrepositions() {
        return (Preposition[]) PREPOSITIONS.values().toArray(new Preposition[PREPOSITIONS.values().size()]);
    }

    public static Preposition getPreposition(String preposition) {
        return (Preposition) PREPOSITIONS.get(preposition);
    }

    public RelativeDateFunction() {
        this(SymbolicDateFunction.TODAY_DATEFUNCTION, OFFSET_TYPE_DAYS, PREPOSITION_AFTER, 0);
    }

    public RelativeDateFunction(DateFunction referenceDate, String calendarOffsetType, String preposition, int offset) {
        this(referenceDate, getCalendarOffsetType(calendarOffsetType), getPreposition(preposition), offset);
    }

    public RelativeDateFunction(DateFunction referenceDate, OffsetType calendarOffsetType, Preposition preposition, int offset) {
        assert (referenceDate != null) : "Null referenceDate";
        assert (calendarOffsetType != null) : "Null calendarOffsetType";
        assert (preposition != null) : "Null preposition";
        this.referenceDate = referenceDate;
        this.calendarOffsetType = calendarOffsetType;
        this.preposition = preposition;
        this.offset = Math.abs(offset);
        updateDescription();
    }

    public java.sql.Date getDate() {
        java.sql.Date currentReferenceDate = referenceDate.getDate();
        long currentReferenceMillis = currentReferenceDate.getTime();

        if (currentReferenceMillis == Long.MAX_VALUE && preposition.equals(PREPOSITION_AFTER)) {
            return currentReferenceDate;
        } else if (currentReferenceMillis == Long.MIN_VALUE && preposition.equals(PREPOSITION_BEFORE)) {
            return currentReferenceDate;
        } else {
            int multiplier = preposition.equals(PREPOSITION_AFTER) ? 1 : -1;
            Calendar cal = DateUtilities.newGmtCalendar();
            cal.setTime(currentReferenceDate);
            cal.add(calendarOffsetType.getCalendarType(), multiplier * offset);
            return DateUtilities.newDate(cal.getTime().getTime());
        }
    }

    private void update() {
        updateDate();
        updateDescription();
    }

    private void updateDate() {
        java.sql.Date newDate = getDate();
        firePropertyChange("date", oldDate, newDate);
        oldDate = newDate;
    }

    private void updateDescription() {
        setDescription((preposition.equals(PREPOSITION_BEFORE) ? preposition.getCode() : "") + Integer.toString(offset) + calendarOffsetType.getCode());
        setVerboseDescription(referenceDate.getDescription() + preposition.getCode() + Integer.toString(offset) + calendarOffsetType.getCode());
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        int oldValue = this.offset;
        int newValue = Math.abs(offset);
        this.offset = newValue;
        firePropertyChange("offset", oldValue, newValue);
        update();
    }

    public OffsetType getCalendarOffsetType() {
        return calendarOffsetType;
    }

    public void setCalendarOffsetType(OffsetType calendarOffsetType) {
        OffsetType oldValue = this.calendarOffsetType;
        this.calendarOffsetType = calendarOffsetType;
        firePropertyChange("calendarOffsetType", oldValue, calendarOffsetType);
        update();
    }

    // TODO: left in for config migration
    public void setCalendarOffsetType(String calendarOffsetType) {
        setCalendarOffsetType(getCalendarOffsetType(calendarOffsetType));
    }

    public DateFunction getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(DateFunction referenceDate) {
        DateFunction oldValue = this.referenceDate;
        this.referenceDate = referenceDate;
        firePropertyChange("referenceDate", oldValue, referenceDate);
        update();
    }

    public Preposition getPreposition() {
        return preposition;
    }

    public void setPreposition(Preposition preposition) {
        assert PREPOSITION_AFTER.equals(preposition) || PREPOSITION_BEFORE.equals(preposition);
        Preposition oldValue = this.preposition;
        this.preposition = preposition;
        firePropertyChange("preposition", oldValue, preposition);
        update();
    }

    // TODO: left in for config migration
    public void setPreposition(String preposition) {
        setPreposition(getPreposition(preposition));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelativeDateFunction)) return false;

        final RelativeDateFunction relativeDateFunction = (RelativeDateFunction) o;

        if (!referenceDate.equals(relativeDateFunction.referenceDate)) return false;
        if (!calendarOffsetType.equals(relativeDateFunction.calendarOffsetType)) return false;
        if (offset != relativeDateFunction.offset) return false;
        if (!preposition.equals(relativeDateFunction.preposition)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = referenceDate.hashCode();
        result = 29 * result + calendarOffsetType.hashCode();
        result = 29 * result + offset;
        result = 29 * result + preposition.hashCode();
        return result;
    }

    public static class OffsetType {
        private String code;
        private String description;
        private int calendarType;

        private OffsetType(String code, String description, int calendarType) {
            this.code = code;
            this.description = description;
            this.calendarType = calendarType;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public int getCalendarType() {
            return calendarType;
        }

        public String toString() {
            return description;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OffsetType)) return false;

            final OffsetType offsetType = (OffsetType) o;

            if (!code.equals(offsetType.code)) return false;

            return true;
        }

        public int hashCode() {
            return code.hashCode();
        }
    }

    public static class Preposition {
        private String code;
        private String description;

        private Preposition(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            return description;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Preposition)) return false;

            final Preposition preposition = (Preposition) o;

            if (!code.equals(preposition.code)) return false;

            return true;
        }

        public int hashCode() {
            return code.hashCode();
        }
    }
}
