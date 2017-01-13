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

import java.util.Comparator;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class ToStringComparator implements Comparator<Object> {
    private static final Comparator COMPARABLE_COMPARATOR = new ComparableComparator();
    public static final Comparator CASE_INSENSITIVE_COMPARATOR = new ToStringComparator(true);
    public static final Comparator CASE_SENSITIVE_COMPARATOR = new ToStringComparator(false);

    private boolean ignoreCase;

    private ToStringComparator(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            // Define null less than everything.
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        if (o1.toString() == null && o2.toString() == null) {
            return 0;
        } else if (o1.toString() == null) {
            // Define null less than everything.
            return -1;
        } else if (o2.toString() == null) {
            return 1;
        }

        // We assume values aren't comparable, so convert to strings which are.
        // Note: this defines false < true.
        if (ignoreCase) {
            return COMPARABLE_COMPARATOR.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase());
        } else {
            return COMPARABLE_COMPARATOR.compare(o1.toString(), o2.toString());
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToStringComparator)) return false;

        final ToStringComparator toStringComparator = (ToStringComparator) o;

        if (ignoreCase != toStringComparator.ignoreCase) return false;

        return true;
    }

    public int hashCode() {
        return (ignoreCase ? 1 : 0);
    }
}
