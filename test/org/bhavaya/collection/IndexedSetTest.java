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

import junit.framework.TestCase;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.Utilities;

import java.util.Iterator;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.1 $
 */
public class IndexedSetTest extends TestCase {
    public IndexedSetTest(String s) {
        super(s);
    }

    public void testNull() throws Exception {
        IndexedSet beans = new IndexedSet();

        beans.add("A");
        beans.add(null);

        assertTrue(beans.indexOf("A") == 0);
        assertTrue(beans.indexOf(null) == 1);

        assertTrue(beans.get(0) == "A");
        assertTrue(beans.get(1) == null);
    }

    public void testOrder() throws Exception {
        IndexedSet beans = new IndexedSet();

        Object[] array = new String[]{"B", "C", "E", "D"};

        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            beans.add(o);
        }

        int i = 0;
        for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            switch (i) {
                case 0:
                    assertTrue(o.equals("B"));
                    break;
                case 1:
                    assertTrue(o.equals("C"));
                    break;
                case 2:
                    assertTrue(o.equals("E"));
                    break;
                case 3:
                    assertTrue(o.equals("D"));
                    break;
                default:
                    fail();
            }
            i++;
        }

        assertTrue(beans.indexOf("B") == 0);
        assertTrue(beans.indexOf("C") == 1);
        assertTrue(beans.indexOf("E") == 2);
        assertTrue(beans.indexOf("D") == 3);

        assertTrue(beans.size() == 4);
    }

    public void testOrderGet() throws Exception {
        IndexedSet beans = new IndexedSet();

        Object[] array = new String[]{"B", "C", "E", "D"};

        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            beans.add(o);
        }

        for (int i = 0; i < beans.size(); i++) {
            Object o = beans.get(i);
            switch (i) {
                case 0:
                    assertTrue(o.equals("B"));
                    break;
                case 1:
                    assertTrue(o.equals("C"));
                    break;
                case 2:
                    assertTrue(o.equals("E"));
                    break;
                case 3:
                    assertTrue(o.equals("D"));
                    break;
                default:
                    fail();
            }
        }

        assertTrue(beans.size() == 4);
    }

    public void testNoDuplicate() throws Exception {
        IndexedSet beans = new IndexedSet();

        Object[] array = new String[]{"B", "C", "B", "E", "D"};

        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            beans.add(o);
        }

        int i = 0;
        for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            switch (i) {
                case 0:
                    assertTrue(o.equals("B"));
                    break;
                case 1:
                    assertTrue(o.equals("C"));
                    break;
                case 2:
                    assertTrue(o.equals("E"));
                    break;
                case 3:
                    assertTrue(o.equals("D"));
                    break;
                default:
                    fail();
            }
            i++;
        }


        assertTrue(beans.indexOf("B") == 0);
        assertTrue(beans.indexOf("C") == 1);
        assertTrue(beans.indexOf("E") == 2);
        assertTrue(beans.indexOf("D") == 3);

        assertTrue(beans.size() == 4);
    }

    public void testSort() throws Exception {
        IndexedSet beans = new IndexedSet();

        Object[] array = new String[]{"C", "B", "E", "D"};

        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            beans.add(o);
        }

        Utilities.sort(beans);

        int i = 0;
        for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            switch (i) {
                case 0:
                    assertTrue(o.equals("B"));
                    break;
                case 1:
                    assertTrue(o.equals("C"));
                    break;
                case 2:
                    assertTrue(o.equals("D"));
                    break;
                case 3:
                    assertTrue(o.equals("E"));
                    break;
                default:
                    fail();
            }
            i++;
        }


        assertTrue(beans.indexOf("B") == 0);
        assertTrue(beans.indexOf("C") == 1);
        assertTrue(beans.indexOf("D") == 2);
        assertTrue(beans.indexOf("E") == 3);

        assertTrue(beans.size() == 4);
    }


//    public void testIteratorAdd() throws Exception {
//        IndexedSet beans = new IndexedSet();
//
//        Object[] array = new String[]{"C", "B", "E", "D"};
//
//        for (int i = 0; i < array.length; i++) {
//            Object o = array[i];
//            beans.add(o);
//        }
//
//        int i = 0;
//        for (ListIterator iterator = beans.listIterator(); iterator.hasNext();) {
//            iterator.next();
//            iterator.add(new Integer(i));
//            i++;
//        }
//
//        i = 0;
//        for (ListIterator iterator = beans.listIterator(); iterator.hasNext();) {
//            Object o = iterator.next();
//            switch (i) {
//                case 0:
//                    assertTrue(o.equals("C"));
//                    break;
//                case 1:
//                    assertTrue(o.equals(new Integer(0)));
//                    break;
//                case 2:
//                    assertTrue(o.equals("B"));
//                    break;
//                case 3:
//                    assertTrue(o.equals(new Integer(1)));
//                    break;
//                case 4:
//                    assertTrue(o.equals("E"));
//                    break;
//                case 5:
//                    assertTrue(o.equals(new Integer(2)));
//                    break;
//                case 6:
//                    assertTrue(o.equals("D"));
//                    break;
//                case 7:
//                    assertTrue(o.equals(new Integer(3)));
//                    break;
//                default:
//                    fail();
//            }
//            i++;
//        }
//
//
//        assertTrue(beans.indexOf("C") == 0);
//        assertTrue(beans.indexOf(new Integer(0)) == 1);
//        assertTrue(beans.indexOf("B") == 2);
//        assertTrue(beans.indexOf(new Integer(1)) == 3);
//        assertTrue(beans.indexOf("E") == 4);
//        assertTrue(beans.indexOf(new Integer(2)) == 5);
//        assertTrue(beans.indexOf("D") == 6);
//        assertTrue(beans.indexOf(new Integer(3)) == 7);
//
//        assertTrue(beans.size() == 8);
//    }

//    public void testIteratorRemove() throws Exception {
//        IndexedSet beans = new IndexedSet();
//
//        Object[] array = new String[]{"C", "B", "E", "D"};
//
//        for (int i = 0; i < array.length; i++) {
//            Object o = array[i];
//            beans.add(o);
//        }
//
//        ListIterator iterator = beans.listIterator();
//        assertTrue(iterator.next().equals("C"));
//        assertTrue(iterator.next().equals("B"));
//        iterator.remove();
//        assertTrue(iterator.next().equals("E"));
//        assertTrue(iterator.next().equals("D"));
//        iterator.remove();
//
//        int i = 0;
//        for (ListIterator iterator2 = beans.listIterator(); iterator2.hasNext();) {
//            Object o = iterator2.next();
//            switch (i) {
//                case 0:
//                    assertTrue(o.equals("C"));
//                    break;
//                case 1:
//                    assertTrue(o.equals("E"));
//                    break;
//                default:
//                    fail();
//            }
//            i++;
//        }
//
//
//        assertTrue(beans.indexOf("C") == 0);
//        assertTrue(beans.indexOf("E") == 1);
//
//        assertTrue(beans.size() == 2);
//    }
}
