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

package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.Log;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class BeanCollectionTableModelTest extends TestCase {
    private static final Log log;

    static {
        log = Log.getCategory(CachedObjectGraphPerformance.class);
//        log.setDebugOverride(true);
    }

    public BeanCollectionTableModelTest(String s) {
        super(s);
    }

    public void testSynchronous() {
        BeanA a1 = new BeanA();
        BeanA a2 = new BeanA(new BeanB(new BeanC(new BeanD("BeanD2String", 1), "BeanC2String"), true), 3.0d);
        BeanCollection beanACollection = new DefaultBeanCollection(BeanA.class);
        beanACollection.add(a1);
        beanACollection.add(a2);

        TableModelObserver listener = new TableModelObserver();

        BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanACollection, false);
        beanCollectionTableModel.addTableModelListener(listener);

        // Test column events.
        beanCollectionTableModel.addColumnLocator("someDouble");
        assertEquals("Event should be structure changed for first column", TableModelEvent.ALL_COLUMNS, listener.getLastEvent().getColumn());
        beanCollectionTableModel.addColumnLocator("b.someBoolean");
        assertEquals("Event should be column insert", TableModelEvent.INSERT, listener.getLastEvent().getType());
        assertEquals("Event should be column 1", 1, listener.getLastEvent().getColumn());
        assertEquals("Event should be all rows", -1, listener.getLastEvent().getFirstRow());
        assertEquals("Event should be all rows", -1, listener.getLastEvent().getLastRow());
        beanCollectionTableModel.addColumnLocator("b.c.someString");
        beanCollectionTableModel.addColumnLocator("b.c.d.i");
    }

    private static class TableModelObserver implements TableModelListener {
        private ArrayList history = new ArrayList();
        private TableModelEvent lastEvent;

        public void tableChanged(TableModelEvent e) {
            history.add(e);
            lastEvent = e;
        }

        public void clear() {
            history.clear();
            lastEvent = null;
        }

        public ArrayList getHistory() {
            return history;
        }

        public TableModelEvent getLastEvent() {
            return lastEvent;
        }
    }
}
