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

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.4 $
 */
public class FixedDateFunction extends DateFunction {
    static {
        BeanUtilities.addPersistenceDelegate(FixedDateFunction.class, new BhavayaPersistenceDelegate(new String[]{"date", "alias", "verboseAlias"}));
    }

    private java.sql.Date date;
    private String alias;
    private String verboseAlias;
    private DateFormat dateFormat;

    // TODO: left in for config migration
    public FixedDateFunction() {
        this(DateUtilities.newDate());
    }

    public FixedDateFunction(java.util.Date absoluteDate) {
        this(DateUtilities.newDate(absoluteDate.getTime()));
    }

    public FixedDateFunction(java.util.Date absoluteDate, DateFormat dateFormat) {
        this.date = DateUtilities.newDate(absoluteDate.getTime());
        this.dateFormat = dateFormat;
        update();
    }

    public FixedDateFunction(java.sql.Date date) {
        this(date, null, null);
    }

    public FixedDateFunction(java.sql.Date date, String alias) {
        this(date, alias, null);
    }

    public FixedDateFunction(java.sql.Date date, String alias, String verboseAlias) {
        this.date = date;
        this.alias = alias;
        this.verboseAlias = verboseAlias;
        update();
    }

    public java.sql.Date getDate() {
        return date;
    }

    // TODO: left in for config migration
    public void setFixedDate(java.sql.Date date) {
        setDate(date);
    }

    public void setDate(java.sql.Date date) {
        java.sql.Date oldValue = this.date;
        this.date = date;
        firePropertyChange("date", oldValue, date);
        update();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        String oldValue = this.alias;
        this.alias = alias;
        firePropertyChange("alias", oldValue, alias);
        update();
    }

    public String getVerboseAlias() {
        return verboseAlias;
    }

    public void setVerboseAlias(String verboseAlias) {
        String oldValue = this.verboseAlias;
        this.verboseAlias = verboseAlias;
        firePropertyChange("verboseAlias", oldValue, verboseAlias);
        update();
    }

    private void update() {
        setDescription(alias != null ? alias :(dateFormat != null) ? dateFormat.format(getDate()): getDate().toString());
        setVerboseDescription(verboseAlias != null ? verboseAlias : alias != null ? alias + " (" + getDate().toString() + ")" : getDate().toString());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FixedDateFunction)) return false;
        final FixedDateFunction fixedDateFunction = (FixedDateFunction) o;

        if (!getDate().equals(fixedDateFunction.getDate())) return false;

        return true;
    }

    public int hashCode() {
        return getDate().hashCode();
    }
}
