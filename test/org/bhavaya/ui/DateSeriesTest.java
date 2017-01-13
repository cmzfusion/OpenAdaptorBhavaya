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

package org.bhavaya.ui;

import junit.framework.TestCase;
import org.bhavaya.ui.table.DateSeries;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.beans.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * A now irrelevant unit test that probably doesn't work.
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */

public class DateSeriesTest extends TestCase {
    private final Date refA = new Date(System.currentTimeMillis());
    private final Date refB = new Date(System.currentTimeMillis() + 1000);

    public DateSeriesTest(String s) {
        super(s);
    }

    public void testFixedDateFunction() {
        DateSeries.FixedDateFunction fdfA = new DateSeries.FixedDateFunction(refA);
        DateSeries.FixedDateFunction fdfB = (DateSeries.FixedDateFunction) fdfA.copy();
        assertEquals(fdfA, fdfB);
    }

    public void testRelativeDateFunction() {
        DateSeries.RelativeDateFunction rdfA = new DateSeries.RelativeDateFunction(
                new DateSeries.FixedDateFunction(refA), DateSeries.RelativeDateFunction.DAYS, DateSeries.RelativeDateFunction.AFTER, 3);
        DateSeries.RelativeDateFunction rdfB = (DateSeries.RelativeDateFunction) rdfA.copy();
        assertEquals(rdfA, rdfB);
    }

    public void testSymbolicDateFunction() {
        DateSeries.SymbolicDateFunction sdfA = DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.TODAY);
        DateSeries.SymbolicDateFunction sdfB = (DateSeries.SymbolicDateFunction) sdfA.copy();
        assertEquals(sdfA, sdfB);

        sdfA = DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.THIS_WEEK);
        sdfB = (DateSeries.SymbolicDateFunction) sdfA.copy();
        assertEquals(sdfA, sdfB);
    }

    private DateSeries createSampleDateSeries() {
        DateSeries dateSeriesA = new DateSeries();
        dateSeriesA.setName("Test");
        final DateSeries.FixedDateFunction function = new DateSeries.FixedDateFunction(refA);
        dateSeriesA.addFunction(function);
        dateSeriesA.addFunction(new DateSeries.RelativeDateFunction(function, DateSeries.RelativeDateFunction.DAYS, DateSeries.RelativeDateFunction.AFTER, 3));
        dateSeriesA.addFunction(DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.THIS_MONTH));
        dateSeriesA.addFunction(DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.TODAY));
        return dateSeriesA;
    }

    public void testDateSeries() {
        DateSeries dateSeriesA = new DateSeries();
        dateSeriesA.setName("Test");
        final DateSeries.FixedDateFunction function = new DateSeries.FixedDateFunction(refA);
        dateSeriesA.addFunction(function);
        dateSeriesA.addFunction(new DateSeries.RelativeDateFunction(function, DateSeries.RelativeDateFunction.DAYS, DateSeries.RelativeDateFunction.AFTER, 3));
        dateSeriesA.addFunction(DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.THIS_MONTH));
        dateSeriesA.addFunction(DateSeries.SymbolicDateFunction.getInstance(DateSeries.SymbolicDateFunction.TODAY));

        DateSeries dateSeriesB = dateSeriesA.copy();
        assertEquals(dateSeriesA, dateSeriesB);
    }

    public void testPersistence() throws Exception {
        DateSeries outSeries = createSampleDateSeries();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(out));
        encoder.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                e.printStackTrace();
            }
        });

        encoder.setPersistenceDelegate(DateSeries.class, new BhavayaPersistenceDelegate() {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                DateSeries dateSeries = (DateSeries) oldInstance;
                for (int i = 0; i < dateSeries.getSize(); i++) {
                    DateSeries.DateFunction dateFunction = dateSeries.getFunction(i);
                    out.writeStatement(new Statement(dateSeries, "addFunction", new Object[]{dateFunction}));
                }
            }
        });

        encoder.writeObject(outSeries);
        encoder.close();

        XMLDecoder xmlDecoder = new XMLDecoder(new ByteArrayInputStream(out.toByteArray()));
        DateSeries inSeries = (DateSeries) xmlDecoder.readObject();
        assertEquals(inSeries, outSeries);
    }
}
