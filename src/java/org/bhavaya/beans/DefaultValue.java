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

package org.bhavaya.beans;

import org.bhavaya.util.*;

import java.util.Calendar;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class DefaultValue {
    private static final String NULL_TYPE = "NULL";
    private String typeName;
    private Class type;
    private String valueString;
    private Object value;

    public DefaultValue(String typeName, String valueString) {
        this.typeName = typeName;
        this.valueString = valueString;
    }

    public Class getType() {
        if (type == null) {
            if (typeName.equals(NULL_TYPE)) {
                type = Object.class;
            } else {
                // prevent any class load order effects while loading schemas
                type = ClassUtilities.getClass(typeName, true, false);
            }
        }
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public Object getValue() {
        if (typeName.equals(NULL_TYPE)) return null;
        if (value == null) {
            if (typeName.equals("java.util.Calendar")) {
                DateFunction dateFunction = SymbolicDateFunction.getInstance(valueString);
                Calendar calendar = DateUtilities.newGmtCalendar();
                calendar.setTime(dateFunction.getDate());
                value = calendar;
            } else if (typeName.equals("java.sql.Date")) {
                DateFunction dateFunction = SymbolicDateFunction.getInstance(valueString);
                value = dateFunction.getDate();
            } else if (typeName.equals("java.util.Date")) {
                DateFunction dateFunction = SymbolicDateFunction.getInstance(valueString);
                value = new java.util.Date(dateFunction.getDate().getTime());
            } else {
                value = Utilities.changeType(getType(), valueString);
            }
        }
        return value;
    }

    public String getValueString() {
        return valueString;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultValue)) return false;

        final DefaultValue defaultValue = (DefaultValue) o;

        if (!typeName.equals(defaultValue.typeName)) return false;
        if (valueString != null ? !valueString.equals(defaultValue.valueString) : defaultValue.valueString != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = typeName.hashCode();
        result = 29 * result + (valueString != null ? valueString.hashCode() : 0);
        return result;
    }
}
