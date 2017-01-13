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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.16 $
 */
public class DateUtilities {
    private static final Log log = Log.getCategory(DateUtilities.class);
    public static final int MILLIS_PER_DAY = 86400000;
    private static final int days_in_month[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final TimeZone localTimeZone = TimeZone.getDefault();
    private static final java.sql.Date MIN_DATE = new java.sql.Date(Long.MIN_VALUE);
    private static final CalendarThreadLocal GMT_CALENDAR_THREAD_LOCAL = new CalendarThreadLocal(TimeZone.getTimeZone("GMT"));

    public static DateFormat newGmtDateFormat(String format) {
        DateFormat gmtDateFormat = new SimpleDateFormat(format);
        gmtDateFormat.setCalendar(newGmtCalendar());
        return gmtDateFormat;
    }

    public static Calendar newGmtCalendar() {
        return new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    }

    public static Calendar newGmtCalendarForToday() {
        Calendar gmtCalendar = newGmtCalendar();
        gmtCalendar.set(Calendar.HOUR_OF_DAY, 0); //24hr
        gmtCalendar.set(Calendar.MINUTE, 0);
        gmtCalendar.set(Calendar.SECOND, 0);
        gmtCalendar.set(Calendar.MILLISECOND, 0);
        return gmtCalendar;
    }

    public static java.sql.Date newDate() {
        return newDate(System.currentTimeMillis());
    }

    public static java.sql.Date newDate(java.util.Date date) {
        if (date == null) return null;
        if (date.getTime() == Long.MIN_VALUE) return MIN_DATE;
        long millis = date.getTime();
        return newDate(millis);
    }

    public static java.sql.Date newDate(long millis) {
        Calendar gmtCalendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        return newDate(millis, gmtCalendar);
    }

    private static java.sql.Date newDate(long millis, Calendar calendar) {
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0); //24hr
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new java.sql.Date(calendar.getTimeInMillis());
    }

    public static java.sql.Date newDate(int year, int month, int day) {
        Calendar gmtCalendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        gmtCalendar.set(year, month - 1, day, 0, 0, 0);
        gmtCalendar.set(Calendar.MILLISECOND, 0);
        return new java.sql.Date(gmtCalendar.getTimeInMillis());
    }

    public static java.util.Date newDateTime() {
        return new java.util.Date();
    }

    public static java.util.Date newDateTime(int year, int month, int day, int hour, int minutes, int seconds, int milliseconds) {
        Calendar calendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        calendar.set(year, month - 1, day, hour, minutes, seconds);
        calendar.set(Calendar.MILLISECOND, milliseconds);
        return calendar.getTime();
    }

    public static java.util.Date newTime(int hour, int minutes, int seconds, int milliseconds) {
        Calendar calendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour); //24hr
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, milliseconds);
        return calendar.getTime();
    }

    public static java.util.Date newLocalTime(int hour, int minutes, int seconds, int milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour); //24hr
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, milliseconds);
        return calendar.getTime();
    }

    public static java.util.Date newNextTime(int hour, int minutes, int seconds, int milliseconds) {
        Calendar calendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        return nextTime(calendar, hour, minutes, seconds, milliseconds);
    }

    public static java.util.Date newLocalNextTime(int hour, int minutes, int seconds, int milliseconds) {
        Calendar calendar = Calendar.getInstance();
        return nextTime(calendar, hour, minutes, seconds, milliseconds);
    }

    private static java.util.Date nextTime(Calendar calendar, int hour, int minutes, int seconds, int milliseconds) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour); //24hr
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, milliseconds);

        // already past that time today so same time tommorrow
        if ((calendar.getTimeInMillis()) < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1);
        }

        return calendar.getTime();
    }

    public static long addGmtOffset(long millis) {
        return millis + localTimeZone.getOffset(millis);
    }

    public static long addGmtOffset(long millis, TimeZone fromTimeZone) {
        return millis + fromTimeZone.getOffset(millis);
    }

    public static java.util.Date relativeToDate(java.util.Date referenceDate, int calendarField, int offset) {
        Calendar calendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        calendar.setTime(referenceDate);
        calendar.add(calendarField, offset);
        return calendar.getTime();
    }

    public static java.sql.Date relativeToDate(java.sql.Date referenceDate, int calendarField, int offset) {
        Calendar calendar = (Calendar) GMT_CALENDAR_THREAD_LOCAL.get();
        calendar.setTime(referenceDate);
        calendar.add(calendarField, offset);
        return new java.sql.Date(calendar.getTime().getTime());
    }

    public static int dateToYYYYMMDD(java.util.Date date) {
        if (date == null) {
            log.warn("dateToYYYMMDD give null date. Todo: find out how GAL should be passed a null date");
            date = newDate();
        }
        // to speed up this method, see code in previous versions of this class
        Calendar calendar = Calendar.getInstance(); // in local date
        calendar.setTime(date);
        return ((calendar.get(Calendar.YEAR) * 10000) +
                ((calendar.get(Calendar.MONTH) + 1) * 100) +
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static boolean isSameDay(java.sql.Date dateA, java.sql.Date dateB) {
        return stripTime(dateA.getTime()) == stripTime(dateB.getTime());
    }

    public static long stripTime(long millis) {
        return (((millis / DateUtilities.MILLIS_PER_DAY)) * DateUtilities.MILLIS_PER_DAY);
    }

    public static java.sql.Date addDays(java.sql.Date date, int numDaysToAdd) {
        return new java.sql.Date(date.getTime() + (numDaysToAdd * DateUtilities.MILLIS_PER_DAY));
    }

    public static int calcDaysDifference(java.util.Date a, java.util.Date b) {
        int tempDifference;
        int difference = 0;
        Calendar earlier = Calendar.getInstance();
        Calendar later = Calendar.getInstance();

        if (a.compareTo(b) < 0) {
            earlier.setTime(a);
            later.setTime(b);
        } else {
            earlier.setTime(b);
            later.setTime(a);
        }

        while (earlier.get(Calendar.YEAR) != later.get(Calendar.YEAR)) {
            tempDifference = 365 * (later.get(Calendar.YEAR) - earlier.get(Calendar.YEAR));
            difference += tempDifference;
            earlier.add(Calendar.DAY_OF_YEAR, tempDifference);
        }

        if (earlier.get(Calendar.DAY_OF_YEAR) != later.get(Calendar.DAY_OF_YEAR)) {
            tempDifference = later.get(Calendar.DAY_OF_YEAR) - earlier.get(Calendar.DAY_OF_YEAR);
            difference += tempDifference;
        }

        return difference;
    }

    public static java.sql.Date yyyymmddToDate(int yyyyMMdd_date) {
        int day = yyyyMMdd_date % 100;
        yyyyMMdd_date /= 100;
        int mth = yyyyMMdd_date % 100;
        yyyyMMdd_date /= 100;
        int yr = yyyyMMdd_date;
        return newDate(yr, mth, day);
    }


    // return true if leap year
    private static boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }

        if (year % 400 == 0) {
            return true;
        }

        return (year % 100 != 0);
    }

    // get the number of days in a given month
    public static int getDaysInMonth(int month, int year) {
        int m = days_in_month[month - 1];

        if (month == 2 && isLeapYear(year)) {
            m++;
        }

        return m;
    }

    public static java.util.Date getRandomDate(int minYear, int maxYear) {
        // generate year, month, day
        int year = Utilities.getRandomInt(minYear, maxYear);
        int month = Utilities.getRandomInt(1, 12);
        int day = Utilities.getRandomInt(1, getDaysInMonth(month, year));
        int secs = Utilities.getRandomInt(1, 60);
        int millisecs = Utilities.getRandomInt(1, 1000);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.SECOND, secs);
        calendar.set(Calendar.MILLISECOND, millisecs);

        return calendar.getTime();
    }
}
