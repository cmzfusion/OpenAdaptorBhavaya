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
 * @version $Revision: 1.3 $
 */
public final class KeyValuePair extends DefaultObservable implements Comparable {
    private Object key;
    private Object value;

    public KeyValuePair(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        Object oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyValuePair)) return false;
        if (!super.equals(o)) return false;

        final KeyValuePair keyValuePair = (KeyValuePair) o;

        if (key != null ? !key.equals(keyValuePair.key) : keyValuePair.key != null) return false;
        if (value != null ? !value.equals(keyValuePair.value) : keyValuePair.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (key != null ? key.hashCode() : 0);
        result = 29 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public int compareTo(Object o) {
        if (o == null) return -1;
        if (!(o instanceof KeyValuePair)) return -1;
        KeyValuePair other = (KeyValuePair) o;
        Object o1 = getKey();
        Object o2 = other.getKey();
        return Utilities.compare(o1, o2);
    }

    public String toString() {
        return key + " = " + value;
    }
}
