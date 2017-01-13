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
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
@ClassMetaData(selectable = true)
public abstract class DateFunction extends DefaultObservable implements Comparable, LoadClosure, Describeable {
    private String description;
    private String verboseDescription;

    public abstract java.sql.Date getDate();

    public Object load() {
        return getDate();
    }

    public String getDescription() {
        return description;
    }

    public String getVerboseDescription() {
        return verboseDescription;
    }

    protected void setDescription(String description) {
        String oldValue = this.description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    protected void setVerboseDescription(String verboseDescription) {
        String oldValue = this.verboseDescription;
        this.verboseDescription = verboseDescription;
        firePropertyChange("verboseDescription", oldValue, verboseDescription);
    }

    public int compareTo(Object o) {
        return getDate().compareTo(((DateFunction) o).getDate());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateFunction)) return false;

        final DateFunction dateFunction = (DateFunction) o;

        if (!getDate().equals(dateFunction.getDate())) return false;
        if (!getDescription().equals(dateFunction.getDescription())) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getDate().hashCode();
        result = 29 * result + getDescription().hashCode();
        return result;
    }

    public String toString() {
        return getDescription();
    }
}