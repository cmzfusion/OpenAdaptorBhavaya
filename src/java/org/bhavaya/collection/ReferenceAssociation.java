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

package org.bhavaya.collection;

import java.util.*;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class ReferenceAssociation extends DefaultAssociation {
    private ReferenceValueMap keyToValue;

    public static ReferenceAssociation newWeakInstance() {
        return newWeakInstance(new HashMap());
    }

    public static ReferenceAssociation newWeakInstance(Map underlyingMap) {
        ReferenceValueMap keyToValue = new WeakReferenceValueMap(underlyingMap);
        return new ReferenceAssociation(keyToValue);
    }

    public static ReferenceAssociation newSoftInstance() {
        return newWeakInstance(new HashMap());
    }

    public static ReferenceAssociation newSoftInstance(Map underlyingMap) {
        ReferenceValueMap keyToValue = new SoftReferenceValueMap(underlyingMap);
        return new ReferenceAssociation(keyToValue);
    }

    private ReferenceAssociation(ReferenceValueMap keyToValue) {
        super(keyToValue, new WeakHashMap());
        this.keyToValue = keyToValue;
    }

    public void addReferenceRemovedListener(MapListener l) {
        keyToValue.addReferenceRemovedListener(l);
    }

    public void removeReferenceRemovedListener(MapListener l) {
        keyToValue.removeReferenceRemovedListener(l);
    }

    public static void main(String[] args) {
        Association m = newWeakInstance(new LinkedHashMap());
        String value = "aeker";
        m.put("foo", "bar");
        m.put("god", value);
        m.put("frabe", "normington");
        System.out.println(m.get("foo"));
        System.out.println(m.getKeyForValue(value));

        for (Iterator iterator = m.values().iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            System.out.println("o = " + o);
        }
    }
}
