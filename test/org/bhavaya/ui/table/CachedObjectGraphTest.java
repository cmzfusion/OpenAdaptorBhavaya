/**
 * Description
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
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
import org.bhavaya.collection.SingleItemSet;
import org.bhavaya.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class CachedObjectGraphTest extends TestCase {
    private static final Log log;

    static {
        log = Log.getCategory(CachedObjectGraphTest.class);
//        log.setDebugOverride(true);
    }

    public CachedObjectGraphTest(String s) {
        super(s);
    }

    /**
     * The simple test case: We have an object graph of A->B->C.
     */
    public void testSingleRootSinglePath() {
        BeanA a = new BeanA();
        BeanB b = a.getB();
        BeanC c = b.getC();

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addRootObject(a);

        PropertyChangeObserver bListener = new PropertyChangeObserver("b", "b");
        PropertyChangeObserver bCListener = new PropertyChangeObserver("b.c", "b.c");
        PropertyChangeObserver bCSomeStringListener = new PropertyChangeObserver("b.c.someString", "b.c.someString");
        graph.addPathListener("b", bListener);
        graph.addPathListener("b.c", bCListener);
        graph.addPathListener("b.c.someString", bCSomeStringListener);

        // Try the simplest case first: Change the leaf.
        final String testStringOne = "Test1";
        c.setSomeString(testStringOne);
        assertEquals("Should only receive a single event for change in leaf.", 1, bCSomeStringListener.history.size());
        assertEquals("Event was not of single type", false, bCSomeStringListener.wasMulti);
        assertEquals("Event contains incorrect value.", testStringOne, bCSomeStringListener.observedValue);
        assertEquals("Should not receive event", 0, bCListener.history.size());
        assertEquals("Should not receive event", 0, bListener.history.size());

        // Next, change a higher part of the tree.
        bCSomeStringListener.reset();
        BeanC newC = new BeanC();
        b.setC(newC);
        assertEquals("Event count incorrect", 1, bCSomeStringListener.history.size());
        assertEquals("Event value incorrect", newC.getSomeString(), bCSomeStringListener.observedValue);
        assertEquals("Event count incorrect", 1, bCListener.history.size());
        assertEquals("Event value incorrect", newC, bCListener.observedValue);
        assertEquals("Event count incorrect", 0, bListener.history.size());

        // Next, invoke a setter but set the same value.  Expect no events.
        bCSomeStringListener.reset();
        bCListener.reset();
        b.setC(newC);
        assertEquals("Event count incorrect", 0, bCSomeStringListener.history.size());
        assertEquals("Event count incorrect", 0, bCListener.history.size());
        assertEquals("Event count incorrect", 0, bListener.history.size());

        // Removed by Brendon for reason: Not a pure black box test.  I think we want to keep the first few tests
        // as simple as possible.

//        graph.removePathListener("b.c.someString", bCSomeStringListener);
//        graph.removeRootObject(a);
//
//        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
//        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    /**
     * Test handling of nulls in a chain.
     */
    public void testNulls() {
        BeanA a = new BeanA();
        a.setB(null);

        BeanB newB = new BeanB();
        newB.getC().getD().setI(0);

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addRootObject(a);

        PropertyChangeObserver bCDIListener = new PropertyChangeObserver("bCDIListener");
        graph.addPathListener("b.c.d.i", bCDIListener);

        // First see that COG handles null in a path correctly.
        assertEquals("COG value returned should be null", null, graph.get(a, "b.c.d.i"));

        // Now check how it handles having the nulls replaced
        a.setB(newB);
        assertEquals("Listener got wrong value", 0, ((Integer) bCDIListener.observedValue).intValue());

        // Now check setting to null again
        a.setB(null);
        assertEquals("Listener got wrong value", null, bCDIListener.observedValue);
    }

    public void testAsynchronous() {
        BeanA a = new BeanA();
        BeanB b = a.getB();

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class, true);
        graph.addRootObject(a);

        PropertyChangeObserver bCSomeStringListener = new PropertyChangeObserver("b.c.someString", "b.c.someString");
        PropertyChangeObserver bCListener = new PropertyChangeObserver("b.c", "b.c");
        PropertyChangeObserver bListener = new PropertyChangeObserver("b", "b");
        graph.addPathListener("b.c.someString", bCSomeStringListener);
        graph.addPathListener("b.c", bCListener);
        graph.addPathListener("b", bListener);

        // Call a getter, should get "DATA NOT READY"
        assertEquals("Should have received DATA_NOT_READY", CachedObjectGraph.DATA_NOT_READY, graph.get(a, "b"));
        assertEquals("Should have received DATA_NOT_READY", CachedObjectGraph.DATA_NOT_READY, graph.get(a, "b.c"));
        assertEquals("Should have received DATA_NOT_READY", CachedObjectGraph.DATA_NOT_READY, graph.get(a, "b.c.someString"));

        // Wait for the event and check data
        try {
            bCSomeStringListener.waitForSingleEvent(50);
            assertEquals("Data value incorrect", a.getB().getC().getSomeString(), graph.get(a, "b.c.someString"));
            assertEquals("Listener got wrong value", a.getB().getC().getSomeString(), bCSomeStringListener.observedValue);
        } catch (InterruptedException e) {
            fail("Timed out waiting for singleEvent");
        }

        try {
            bCListener.waitForSingleEvent(20);
            assertEquals("Data value incorrect", a.getB().getC(), graph.get(a, "b.c"));
            assertEquals("Listener got wrong value", a.getB().getC(), bCListener.observedValue);
        } catch (InterruptedException e) {
            fail("Timed out waiting for singleEvent");
        }

        try {
            bListener.waitForSingleEvent(20);
            assertEquals("Data value incorrect", a.getB(), graph.get(a, "b"));
            assertEquals("Listener got wrong value", a.getB(), bListener.observedValue);
        } catch (InterruptedException e) {
            fail("Timed out waiting for singleEvent");
        }
        bListener.reset();
        bCListener.reset();
        bCSomeStringListener.reset();

        // Test asynchronous event handling
        try {
            b.setC(null);
            bCListener.waitForSingleEvent(20);
            assertEquals("COG value incorrect", null, graph.get(a, "b.c"));
            assertEquals("Listener value incorrect", null, bCListener.observedValue);
            bCSomeStringListener.waitForSingleEvent(10);
            assertEquals("COG value incorrect", null, graph.get(a, "b.c.someString"));
            assertEquals("Listener value incorrect", null, bCSomeStringListener.observedValue);
        } catch (InterruptedException e) {
            fail("Timed out waiting for event");
        }
        bCListener.reset();
        bCSomeStringListener.reset();
    }

//    public void testSomething() {
//        A a = new A();
//        a.b.c.setSomeString("initial");
//
//        B newB = new B();
//        newB.c.setSomeString("Second string");
//
//        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");
//        PropertyChangeObserver listener2 = new PropertyChangeObserver("listener2");
//
//        CachedObjectGraph graph = new CachedObjectGraph(A.class, false);
//        graph.addPathListener("b.c.someString", listener1);
//        graph.addPathListener("b.c.d.i", listener2);
//        graph.addRootObject(a);
//        graph.get(a, "b.c.d.i");
//
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
//        }
//
//        log.debug("b.c.someString received " + listener1.history);
//        log.debug("b.c.d.i received " + listener2.history);
//
////        a.setB(null);
//        log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));
//
//        //cause cause some listeners to be automatically rehooked to different objects
//        a.setB(newB);
//        log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));
//
//        graph.removePathListener("b.c.someString", listener1);
//        graph.removePathListener("b.c.d.i", listener2);
//
//        graph.removeRootObject(a);
//
//        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
//        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
//    }

    public void testMultipleRootSinglePath() {
        String newString = "first string";
        String secondString = "second string";

        BeanA a1 = new BeanA();
        BeanA a2 = new BeanA();
        BeanB b1 = a1.getB();
        BeanB b2 = a2.getB();

        BeanC sharedC = new BeanC();
        sharedC.setSomeString(newString);

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addRootObject(a1);
        graph.addRootObject(a2);

        PropertyChangeObserver bCSomethingListener = new PropertyChangeObserver("bCSomethingListener");
        graph.addPathListener("b.c.someString", bCSomethingListener);

        b1.setC(sharedC);
        assertEquals("Listener has incorrect root", a1, bCSomethingListener.root);
        assertEquals("Listener has wrong value", newString, bCSomethingListener.observedValue);

        b2.setC(sharedC);
        assertEquals("Listener has incorrect root", a2, bCSomethingListener.root);
        assertEquals("Listener has incorrect root", newString, bCSomethingListener.observedValue);
        bCSomethingListener.reset();

        sharedC.setSomeString(secondString);
        assertEquals("Listener should have events with 2 roots", 2, ((PathPropertyChangeEvent) bCSomethingListener.history.get(0)).getRoots().size());

        graph.removeRootObject(a2);
        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testMultipleRootMultiplePath() {
        String newString = "Shared string";
        String secondString = "second string";
        String sharedDString = "Shared d string";

        BeanA a1 = new BeanA();
        BeanA a2 = new BeanA();
        BeanB b1 = a1.getB();
        BeanB b2 = a2.getB();
        BeanD d1 = a1.getB().getC().getD();
        BeanD d2 = a2.getB().getC().getD();

        BeanC sharedC = new BeanC();
        sharedC.setSomeString(newString);
        sharedC.getD().setSomeString(sharedDString);

        //add listeners on an immutable object
        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC", "b.c.someString");
        PropertyChangeObserver listenerD = new PropertyChangeObserver("listenerD", "b.c.d.someString");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addRootObject(a1);

        graph.addPathListener("b.c", new PropertyChangeObserver("test", "b.c"));
        graph.addPathListener("b.c.someString", listenerC);
        graph.addPathListener("b.c.d.someString", listenerD);

        graph.addRootObject(a2);

        d1.setSomeString("d1 string");
        assertEquals(a1, listenerD.root);
        assertEquals("d1 string", listenerD.observedValue);

        d2.setSomeString("d2 string");
        assertEquals(a2, listenerD.root);
        assertEquals("d2 string", listenerD.observedValue);

        b1.setC(sharedC);
        assertEquals(a1, listenerC.root);
        assertEquals(newString, listenerC.observedValue);
        assertEquals(listenerC.name + " expected multi change, but got a single change", true, listenerC.wasMulti);

        assertEquals(a1, listenerD.root);
        assertEquals(sharedDString, listenerD.observedValue);
        assertEquals(listenerD.name + " expected multi change, but got a single change", true, listenerD.wasMulti);

        b2.setC(sharedC);
        assertEquals(a2, listenerC.root);
        assertEquals(newString, listenerC.observedValue);
        assertEquals(listenerC.name + " expected multi change, but got a single change", true, listenerC.wasMulti);
        assertEquals(a2, listenerD.root);
        assertEquals(sharedDString, listenerD.observedValue);
        assertEquals(listenerD.name + " expected multi change, but got a single change", true, listenerD.wasMulti);

        sharedC.setSomeString(secondString);
        assertEquals(secondString, listenerC.observedValue);
        assertEquals(listenerC.name + " expected single change, but got a multi change", false, listenerC.wasMulti);

        sharedC.getD().setSomeString("new d string");
        assertEquals("new d string", listenerD.observedValue);
        assertEquals(listenerD.name + " expected single change, but got a multi change", false, listenerD.wasMulti);

        graph.removePathListener("b.c.someString", listenerC);
        graph.removePathListener("b.c.d.someString", listenerD);
        graph.removeRootObject(a1);
        graph.removeRootObject(a2);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }


    public void testSharedPropertyPath() {
        BeanA a = new BeanA();

        //add listeners on an immutable object
        PropertyChangeObserver listenerB = new PropertyChangeObserver("listenerB", "b");
        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC", "b.c");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addPathListener("b", listenerB);
        graph.addPathListener("b.c", listenerC);

        graph.addRootObject(a);
        a.setB(new BeanB());

        assertEquals("listener B fired too many times", 1, listenerB.history.size());
        assertEquals("listener B expected multi change, but got a single change", true, listenerB.wasMulti);
        assertEquals("listener C fired too many times", 1, listenerC.history.size());
        assertEquals("listener C expected multi change, but got a single change", true, listenerB.wasMulti);

        //also be a bit nasty in cleaning up the path by adding and removing a few times
        graph.removePathListener("b", listenerB);
        graph.addPathListener("b", listenerB);
        graph.removePathListener("b.c", listenerC);
        graph.removePathListener("b", listenerB);

        graph.removeRootObject(a);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }


    public void testSharedProperty() {
        String C1_INITIAL_STR = "C1's initial string";
        String C2_INITIAL_STR = "C2's initial string";
        String C2_FINAL_STR = "C2's final string";
        String FINAL_STR = "Final string";

        BeanA a1 = new BeanA();
        BeanC c1 = a1.getB().getC();
        c1.setSomeString(C1_INITIAL_STR);

        BeanA a2 = new BeanA();
        BeanC c2 = a2.getB().getC();
        c2.setSomeString(C2_INITIAL_STR);

        BeanA a3 = new BeanA();

        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC", "b.c.someString");
        PropertyChangeObserver bcbcListener = new PropertyChangeObserver("bcbcListener", "b.c.b.c.someString");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addPathListener("b.c.someString", listenerC);
        graph.addPathListener("b.c.b.c.someString", bcbcListener);

        graph.addRootObject(a1);
        graph.addRootObject(a2);
        graph.addRootObject(a3);
        a1.setB(a2.getB());
        {
            assertEquals("listener C fired wrong number of times", 1, listenerC.history.size());
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) listenerC.history.get(0);
            assertEquals("listener C got wrong roots", new SingleItemSet(a1), event.getRoots());
            assertEquals("listener C got wrong oldValue", C1_INITIAL_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C2_INITIAL_STR, event.getNewValue());
        }

        c2.setSomeString(C2_FINAL_STR);
        {
            assertEquals("listener C fired wrong number of times", 2, listenerC.history.size());
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) listenerC.history.get(1);
            HashSet expected = new HashSet();
            expected.add(a1);
            expected.add(a2);
            assertEquals("listener C got wrong roots", expected, event.getRoots());
            assertEquals("listener C got wrong oldValue", C2_INITIAL_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C2_FINAL_STR, event.getNewValue());
        }

        a3.getB().getC().setB(a2.getB());
        {
            assertEquals("bcbcListener fired wrong number of times", 1, bcbcListener.history.size());
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) bcbcListener.history.get(0);
            assertEquals("bcbcListener got wrong roots", new SingleItemSet(a3), event.getRoots());
            assertEquals("bcbcListener got wrong oldValue", null, event.getOldValue());
            assertEquals("bcbcListener got wrong newValue", C2_FINAL_STR, event.getNewValue());
        }

        c2.setSomeString(FINAL_STR);
        {
            assertEquals("bcbcListener fired wrong number of times", 2, bcbcListener.history.size());
            assertEquals("bcbcListener expected multi change, but got a single change", true, bcbcListener.wasMulti);
            assertFalse("bcbcListener should not have thought all changes were on same roots", bcbcListener.allAffectSameRoots);
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) bcbcListener.history.get(1);
            assertEquals("bcbcListener got wrong roots", new SingleItemSet(a3), event.getRoots());
            assertEquals("bcbcListener got wrong oldValue", C2_FINAL_STR, event.getOldValue());
            assertEquals("bcbcListener got wrong newValue", FINAL_STR, event.getNewValue());

            assertEquals("listenerC fired wrong number of times", 3, listenerC.history.size());
            assertEquals("listenerC expected multi change, but got a single change", true, listenerC.wasMulti);
            assertFalse("listenerC should not have thought all changes were on same roots", listenerC.allAffectSameRoots);
            PathPropertyChangeEvent event2 = (PathPropertyChangeEvent) listenerC.history.get(2);
            HashSet expected = new HashSet();
            expected.add(a1);
            expected.add(a2);
            assertEquals("listenerC got wrong roots", expected, event2.getRoots());
            assertEquals("listenerC got wrong oldValue", C2_FINAL_STR, event2.getOldValue());
            assertEquals("listenerC got wrong newValue", FINAL_STR, event2.getNewValue());
        }

        // Also be a bit nasty in cleaning up the path by adding and removing a few times
        PropertyChangeObserver listenerB = new PropertyChangeObserver("listenerB");
        graph.addPathListener("b", listenerB);
        graph.removePathListener("b.c.someString", listenerC);
        graph.removePathListener("b.c.b.c.someString", listenerC);
        graph.removeRootObject(a3);
        graph.removePathListener("b", listenerB);

        graph.removeRootObject(a2);
        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testCyclicGraph() {
        String C1_INITIAL_STR = "C1's initial string";
        String C1_MID_STR = "C1's mid string";
        String C1_FINAL_STR = "C1's final string";
        String C2_INITIAL_STR = "C2's initial string";
        String C2_FINAL_STR = "C2's final string";

        BeanA a1 = new BeanA();
        BeanB b1 = a1.getB();
        BeanC c1 = a1.getB().getC();
        b1.setB(b1);
        c1.setSomeString(C1_INITIAL_STR);

        BeanA a2 = new BeanA();
        BeanB b2 = a2.getB();
        BeanC c2 = a2.getB().getC();
        b2.setB(b1);    //path a2.b.b.c == a1.b.b.c == c1_initial_str
        c2.setSomeString(C2_INITIAL_STR);

        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addPathListener("b.b.c.someString", listenerC);

        graph.addRootObject(a1);
        graph.addRootObject(a2);

        c1.setSomeString(C1_MID_STR);
        {
            assertEquals("listener C fired wrong number of times", 1, listenerC.history.size());
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) listenerC.history.get(0);
            HashSet expectedRoots = new HashSet();
            expectedRoots.add(a1);
            expectedRoots.add(a2);
            assertEquals("listener C got wrong roots", expectedRoots, event.getRoots());
            assertEquals("listener C got wrong oldValue", C1_INITIAL_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C1_MID_STR, event.getNewValue());
        }

        b1.setB(b2);
        {
            assertEquals("listener C fired wrong number of times", 2, listenerC.history.size());
            assertEquals("listener C expected single change, but got a multi change", false, listenerC.wasMulti);
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) listenerC.history.get(1);
            assertEquals("listener C got wrong roots", new SingleItemSet(a1), event.getRoots());
            assertEquals("listener C got wrong oldValue", C1_MID_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C2_INITIAL_STR, event.getNewValue());
        }

        c1.setSomeString(C1_FINAL_STR);
        {
            assertEquals("listener C fired wrong number of times", 3, listenerC.history.size());
            assertEquals("listener C expected single change, but got a multi change", false, listenerC.wasMulti);
            PathPropertyChangeEvent event = (PathPropertyChangeEvent) listenerC.history.get(2);
            assertEquals("listener C got wrong roots", new SingleItemSet(a2), event.getRoots());
            assertEquals("listener C got wrong oldValue", C1_MID_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C1_FINAL_STR, event.getNewValue());
        }

        //we had a problem with cleaning up cyclic references, test that.
        PropertyChangeObserver listener = new PropertyChangeObserver("a listener");
        a2.getB().getC().setB( a2.getB() );
        graph.addPathListener("b.c.b.someBoolean", listener);
        graph.removeRootObject(a2);

        c1.setSomeString("foo");
        {
            assertEquals("listener C should not have received an event", 3, listenerC.history.size());
        }

        graph.addPathListener("b.someBoolean", listener);
        graph.removePathListener("b.b.c.someString", listenerC);
        graph.removePathListener("b.someBoolean", listener);
        graph.removePathListener("b.c.b.someBoolean", listener);

        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testComplexCyclicGraph() {
        String INITIAL_A2_STR = "initial A2.b.b.b.c.string";
        String INITIAL_A1_STR = "initial A1.b.b.b.c.string";
        String FINAL_A2_STR = "final A2.b.b.b.c.string";
        String FINAL_A1_STR = "final A1.b.b.b.c.string";

        BeanA a1 = new BeanA();
        BeanB b1 = a1.getB();

        BeanA a2 = new BeanA();
        BeanB b2 = a2.getB();
        b2.setB(b1);

        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addPathListener("b.b.b.c.someString", listenerC);

        graph.addRootObject(a1);
        graph.addRootObject(a2);

        BeanB b3 = new BeanB();
        BeanC c3 = b3.getC();
        c3.setSomeString(INITIAL_A2_STR);

        BeanB b4 = new BeanB();
        BeanC c4 = b4.getC();
        c4.setSomeString(INITIAL_A1_STR);
        b3.setB(b4);

        b1.setB(b3);
        {
            assertTrue("listener C should have received multi change", listenerC.wasMulti);
            assertEquals("listener C fired wrong number of times", 2, listenerC.history.size());
            assertTrue("listener C should have thought all changes were on same roots", listenerC.allAffectSameRoots);

            PathPropertyChangeEvent event1 = (PathPropertyChangeEvent) listenerC.history.get(0);
            assertEquals("listener C got wrong roots", new SingleItemSet(a2), event1.getRoots());
            assertEquals("listener C got wrong oldValue", BeanC.DEFAULT_STRING, event1.getOldValue());
            assertEquals("listener C got wrong newValue", INITIAL_A2_STR, event1.getNewValue());

            PathPropertyChangeEvent event2 = (PathPropertyChangeEvent) listenerC.history.get(1);
            assertEquals("listener C got wrong roots", new SingleItemSet(a1), event2.getRoots());
            assertEquals("listener C got wrong oldValue", BeanC.DEFAULT_STRING, event2.getOldValue());
            assertEquals("listener C got wrong newValue", INITIAL_A1_STR, event2.getNewValue());
        }


        BeanB b5 = new BeanB();
        BeanC c5 = b5.getC();
        c5.setSomeString(FINAL_A2_STR);

        BeanB b6 = new BeanB();
        BeanC c6 = b6.getC();
        c6.setSomeString(FINAL_A1_STR);
        b5.setB(b6);

        b1.setB(b5);
        {
            assertTrue("listener C should have received multi change", listenerC.wasMulti);
            assertEquals("listener C fired wrong number of times", 4, listenerC.history.size());
            assertFalse("listener C should not have thought all changes were on same roots", listenerC.allAffectSameRoots);

            PathPropertyChangeEvent event1 = (PathPropertyChangeEvent) listenerC.history.get(2);
            assertEquals("listener C got wrong roots", new SingleItemSet(a2), event1.getRoots());
            assertEquals("listener C got wrong oldValue", INITIAL_A2_STR, event1.getOldValue());
            assertEquals("listener C got wrong newValue", FINAL_A2_STR, event1.getNewValue());

            PathPropertyChangeEvent event2 = (PathPropertyChangeEvent) listenerC.history.get(3);
            assertEquals("listener C got wrong roots", new SingleItemSet(a1), event2.getRoots());
            assertEquals("listener C got wrong oldValue", INITIAL_A1_STR, event2.getOldValue());
            assertEquals("listener C got wrong newValue", FINAL_A1_STR, event2.getNewValue());
        }

        graph.removeRootObject(a2);

        graph.removePathListener("b.b.b.c.someString", listenerC);

        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testComplexPropertyCleanup(){
        BeanA a = new BeanA();
        BeanB b = a.getB();
        b.getC().setB( b );
        PropertyChangeObserver listener = new PropertyChangeObserver("listenerC");

        CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addPathListener("b.c.b.c", listener);
        graph.addPathListener("b.c", listener);
        graph.addPathListener("b.b", listener);
        graph.addPathListener("b.b.c", listener);

        graph.addRootObject(a);
        graph.removeRootObject(a);

        graph.removePathListener("b.c.b.c", listener);
        graph.removePathListener("b.c", listener);
        graph.removePathListener("b.b", listener);
        graph.removePathListener("b.b.c", listener);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }


    public void testRowReadyListener() throws Exception {
        PropertyChangeObserver listener = new PropertyChangeObserver("listener");
        RowListener rowListener = new RowListener();

        CachedObjectGraph graph = new CachedObjectGraph(DelayBean.class, true);
        graph.addRowReadyListener(rowListener);
        graph.addPathListener("someString", listener);
        graph.addPathListener("someInteger", listener);

        DelayBean delayBean = new DelayBean();
        graph.addRootObject(delayBean);

        // Allow the COG to process.
        Thread.sleep(30);
        assertEquals("Have received an event before row is ready ", 0, rowListener.history.size());

        delayBean.unblock();

        // Allow the COG to process again
        Thread.sleep(20);
        assertEquals("Should have received a row ready event", 1, rowListener.history.size());
        assertEquals("Row event incorrect", delayBean, rowListener.history.get(0));
        assertEquals("Should have received two property events", 2, listener.history.size());
    }

    /**
     * Simulates the effect of blocking getters
     */
    public class DelayBean extends TestBean {
        private int someInteger = 3;
        private String someString = "someString";
        private Object lock = new Object();

        public int getSomeInteger() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return someInteger;
        }

        public void unblock() {
            synchronized (lock) {
                lock.notify();
            }
        }

        public String getSomeString() {
            return someString;
        }
    }


    private static Object getPrivateField(Object source, String fieldName) {
        try {
            Class aClass = source.getClass();
            Field field = aClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(source);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private static class RowListener implements CachedObjectGraph.RowReadyListener {
        private ArrayList history = new ArrayList();

        public void rowReady(Object root) {
            history.add(root);
        }
    }
}
