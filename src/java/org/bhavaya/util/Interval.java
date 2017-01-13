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

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class Interval implements Comparable {
    protected Object start;
    protected Object end;

    static {
        BeanUtilities.addPersistenceDelegate(Interval.class, new BhavayaPersistenceDelegate(new String[]{"start", "end"}));
    }

    public Interval(Object start, Object end) {
        this.start = start;
        this.end = end;
    }

    public int compareTo(Object o) {
        return Utilities.compare(this.end, ((Interval) o).end);
    }

    public boolean contains(Object o) {
        return Utilities.compare(start, o) <= 0 && Utilities.compare(end, o) >= 0;
    }

    public Object getStart() {
        return start;
    }

    public Object getEnd() {
        return end;
    }

    public String toString() {
        return start.toString() + "->" + end.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval)) return false;

        final Interval interval = (Interval) o;

        if (!start.equals(interval.start)) return false;
        if (!end.equals(interval.end)) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = start.hashCode();
        result = 29 * result + end.hashCode();
        return result;
    }

    public static void main(String[] args) {
        Interval interval = new Interval(new Double(-10), new Double(10));
        System.out.println("interval.contains(new Double(5)) = " + interval.contains(new Double(5)));
        System.out.println("interval.contains(new Double(11)) = " + interval.contains(new Double(11)));
        System.out.println("interval.contains(new Double(-11)) = " + interval.contains(new Double(-11)));
        System.out.println("interval.contains(new Double(10)) = " + interval.contains(new Double(10)));
        System.out.println("interval.contains(new Double(-10)) = " + interval.contains(new Double(-10)));
        System.out.println("interval.contains(new Double(0)) = " + interval.contains(new Double(0)));
    }
}
