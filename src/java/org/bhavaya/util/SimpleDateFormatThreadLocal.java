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
import java.util.TimeZone;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class SimpleDateFormatThreadLocal extends ThreadLocal {
    private String format;
    private TimeZone timezone;

    /**
     * Constructs a SimpleDateFormatThreadLocal using a supplied format and TimeZone
     * This is the desired constructor, since it is machine agnostic
     * @param format the format of the SimpleDateFormat @see java.text.SimpleDateFormat
     * @param timezone the TimeZone the formatter works in
     */
    public SimpleDateFormatThreadLocal(String format, TimeZone timezone) {
        this.format = format;
        this.timezone = timezone;
    }

    protected Object initialValue() {
        SimpleDateFormat initialValue = new SimpleDateFormat(format);
        if (timezone != null) initialValue.setTimeZone(timezone);
        return initialValue;
    }

    public DateFormat getDateFormat() {
        return (DateFormat) get();
    }
}
