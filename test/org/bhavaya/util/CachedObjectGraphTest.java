/**
 * Description
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
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

package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.collection.SingleItemSet;
import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.GraphChangeListener;
import org.bhavaya.ui.table.PathPropertyChangeEvent;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.*;

public class CachedObjectGraphTest extends TestCase {
    private static final Log log = Log.getCategory(CachedObjectGraphTest.class);

    public CachedObjectGraphTest(String s) {
        super(s);
    }


    public void testSingleRootSinglePath() {
        if (log.isDebug())log.debug("hello");
        A a = new A();
        B b = a.b;
        C c = b.c;

        C newC = new C();
        newC.setSomeString("Second string");

        //add listeners on an immutable object
        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b.c.someString", listener1);
        graph.addRootObject(a);

        //cause cause some listeners to be automatically rehooked to different objects
        b.setC(newC);

        assertEquals("Second string", listener1.observedValue);

        graph.removePathListener("b.c.someString", listener1);
        graph.removeRootObject(a);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void checkAsynchronous() {
        A a = new A();
        a.b.c.setSomeString("initial");

        B newB = new B();
        newB.c.setSomeString("Second string");

        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");

        CachedObjectGraph graph = new CachedObjectGraph(A.class, true);
        graph.addPathListener("b.c.someString", listener1);
        graph.addRootObject(a);

//        a.setB(null);
        try {
            if (log.isDebug())log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));
        } catch (Exception e) {
            if (log.isDebug())log.debug("got data not ready exception");
        }

        //cause cause some listeners to be automatically rehooked to different objects
        a.setB(newB);
        assertEquals("listener got wrong event value", "Second string", listener1.observedValue);
        if (log.isDebug())log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));

        a.setB(null);
        assertEquals("listener got wrong event value", null, listener1.observedValue);

        graph.removePathListener("b.c.someString", listener1);
//        graph.removeRootObject(a);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void checkSomething() {
        A a = new A();
        a.b.c.setSomeString("initial");

        B newB = new B();
        newB.c.setSomeString("Second string");

        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");
        PropertyChangeObserver listener2 = new PropertyChangeObserver("listener2");

        CachedObjectGraph graph = new CachedObjectGraph(A.class, false);
        graph.addPathListener("b.c.someString", listener1);
        graph.addPathListener("b.c.d.i", listener2);
        graph.addRootObject(a);
        graph.get(a, "b.c.d.i");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }

        if (log.isDebug())log.debug("b.c.someString received " + listener1.history);
        if (log.isDebug())log.debug("b.c.d.i received " + listener2.history);

//        a.setB(null);
        if (log.isDebug())log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));

        //cause cause some listeners to be automatically rehooked to different objects
        a.setB(newB);
        if (log.isDebug())log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));

        graph.removePathListener("b.c.someString", listener1);
        graph.removePathListener("b.c.d.i", listener2);

        graph.removeRootObject(a);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testNulls() {
        A a = new A();
        a.setB(null);
        B newB = new B();
        newB.c.setSomeString("Second string");

        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b.c.someString", listener1);
        graph.addRootObject(a);

        if (log.isDebug())log.debug("value of b.c.someString is: " + graph.get(a, "b.c.someString"));

        //cause cause some listeners to be automatically rehooked to different objects
        a.setB(newB);
        assertEquals("listener got wrong event value", "Second string", listener1.observedValue);

        a.setB(null);
        assertEquals("listener got wrong event value", null, listener1.observedValue);

        graph.removeRootObject(a);
        graph.removePathListener("b.c.someString", listener1);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testMultipleRootSinglePath() {
        String newString = "Shared string";
        String secondString = "second string";

        A a1 = new A();
        A a2 = new A();
        B b1 = a1.b;
        B b2 = a2.b;

        C sharedC = new C();
        sharedC.setSomeString(newString);

        //add listeners on an immutable object
        PropertyChangeObserver listener1 = new PropertyChangeObserver("listener1");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b.c.someString", listener1);
        graph.addRootObject(a1);
        graph.addRootObject(a2);

        //cause cause some listeners to be automatically rehooked to different objects
        b1.setC(sharedC);
        assertEquals(a1, listener1.root);
        assertEquals(newString, listener1.observedValue);

        b2.setC(sharedC);
        assertEquals(a2, listener1.root);
        assertEquals(newString, listener1.observedValue);

        sharedC.setSomeString(secondString);
        assertEquals(secondString, listener1.observedValue);

        graph.removeRootObject(a2);
        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testMultipleRootMultiplePath() {
        String newString = "Shared string";
        String secondString = "second string";
        String sharedDString = "Shared d string";

        A a1 = new A();
        A a2 = new A();
        B b1 = a1.b;
        B b2 = a2.b;
        D d1 = a1.b.c.d;
        D d2 = a2.b.c.d;

        C sharedC = new C();
        sharedC.setSomeString(newString);
        sharedC.d.setSomeString(sharedDString);

        //add listeners on an immutable object
        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC", "b.c.someString");
        PropertyChangeObserver listenerD = new PropertyChangeObserver("listenerD", "b.c.d.someString");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b.c.someString", listenerC);
        graph.addPathListener("b.c.d.someString", listenerD);

        graph.addRootObject(a1);
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
        assertEquals(listenerC.name+" expected multi change, but got a single change", true, listenerC.wasMulti);

        assertEquals(a1, listenerD.root);
        assertEquals(sharedDString, listenerD.observedValue);
        assertEquals(listenerD.name+" expected multi change, but got a single change", true, listenerD.wasMulti);

        b2.setC(sharedC);
        assertEquals(a2, listenerC.root);
        assertEquals(newString, listenerC.observedValue);
        assertEquals(listenerC.name+" expected multi change, but got a single change", true, listenerC.wasMulti);
        assertEquals(a2, listenerD.root);
        assertEquals(sharedDString, listenerD.observedValue);
        assertEquals(listenerD.name+" expected multi change, but got a single change", true, listenerD.wasMulti);

        sharedC.setSomeString(secondString);
        assertEquals(secondString, listenerC.observedValue);
        assertEquals(listenerC.name+" expected single change, but got a multi change", false, listenerC.wasMulti);

        sharedC.d.setSomeString("new d string");
        assertEquals("new d string", listenerD.observedValue);
        assertEquals(listenerD.name+" expected single change, but got a multi change", false, listenerD.wasMulti);

        graph.removePathListener("b.c.someString", listenerC);
        graph.removePathListener("b.c.d.someString", listenerD);
        graph.removeRootObject(a1);
        graph.removeRootObject(a2);

        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }

    public void testSharedPropertyPath() {
        A a = new A();

        //add listeners on an immutable object
        PropertyChangeObserver listenerB = new PropertyChangeObserver("listenerB", "b");
        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC", "b.c");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b", listenerB);
        graph.addPathListener("b.c", listenerC);

        graph.addRootObject(a);
        a.setB(new B());

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


    public void testCyclicPath() {
        String C1_INITIAL_STR = "C1's initial string";
        String C2_INITIAL_STR = "C2's initial string";
        String C2_FINAL_STR = "C2's final string";

        A a1 = new A();
        C c1 = a1.b.c;
        c1.setSomeString(C1_INITIAL_STR);

        A a2 = new A();
        C c2 = a2.b.c;
        c2.setSomeString(C2_INITIAL_STR);

        PropertyChangeObserver listenerC = new PropertyChangeObserver("listenerC");

        CachedObjectGraph graph = new CachedObjectGraph(A.class);
        graph.addPathListener("b.c.someString", listenerC);

        graph.addRootObject(a1);
        graph.addRootObject(a2);

        a1.setB(a2.b);

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
            HashSet expected = new HashSet();   expected.add(a1);   expected.add(a2);
            assertEquals("listener C got wrong roots", expected, event.getRoots());
            assertEquals("listener C got wrong oldValue", C2_INITIAL_STR, event.getOldValue());
            assertEquals("listener C got wrong newValue", C2_FINAL_STR, event.getNewValue());
        }

        //also be a bit nasty in cleaning up the path by adding and removing a few times
        PropertyChangeObserver listenerB = new PropertyChangeObserver("listenerB");
        graph.addPathListener("b", listenerB);
        graph.removePathListener("b.c.someString", listenerC);
        graph.removePathListener("b", listenerB);

        graph.removeRootObject(a2);
        graph.removeRootObject(a1);
        Map referenceGraph = (Map) getPrivateField(graph, "referenceGraph");
        assertEquals("Reference graph not cleaned up properly", 0, referenceGraph.size());
    }


    public class A extends TestBean {
        private B b = new B();

        public B getB() {
            return b;
        }

        public void setB(B b) {
            Object oldValue = this.b;
            this.b = b;
            firePropertyChange("b", oldValue, b);
        }
    }

    public class B extends TestBean {
        public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }

        private C c = new C();
        private B b = this;

        public C getC() {
            return c;
        }

        public void setC(C c) {
            Object oldValue = this.c;
            this.c = c;
            firePropertyChange("c", oldValue, c);
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            Object oldValue = this.b;
            this.b = b;
            firePropertyChange("b", oldValue, b);
        }
    }

    public class C extends TestBean {
        private D d = new D();
        private String someString = "default C string";

        public D getD() {
            return d;
        }

        public void setD(D d) {
            Object oldValue = this.d;
            this.d = d;
            firePropertyChange("d", oldValue, d);
        }

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            Object oldValue = this.someString;
            this.someString = someString;
            firePropertyChange("someString", oldValue, someString);
        }
    }

    public class D extends TestBean {
        private Class someClass = D.class;
        private String someString = "default D String";
        private int i = 0;

        public Class getSomeClass() {
            return someClass;
        }

        public void setSomeClass(Class someClass) {
            Object oldValue = this.someClass;
            this.someClass = someClass;
            firePropertyChange("someClass", oldValue, someClass);
        }

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            Object oldValue = this.someString;
            this.someString = someString;
            firePropertyChange("someString", oldValue, someString);
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            int oldValue = this.i;
            this.i = i;
            firePropertyChange("i", oldValue, i);
        }
    }

    private static class PropertyChangeObserver implements GraphChangeListener {
        public Object observedValue = "nothing received yet";
        public Object root;
        public Object path;
        public ArrayList history = new ArrayList();
        private String name;
        private boolean wasMulti = false;
        private String filterPath;

        public PropertyChangeObserver(String name) {
            this(name, null);
        }

        public PropertyChangeObserver(String name, String filterPath) {
            this.name = name;
            this.filterPath = filterPath;
        }

        public void multipleChange(Collection changes, boolean allAffectSameRoots) {
            if (filterPath == null) {
                log.warn("Got a multi change on listener "+name+" but it has no filter set");
            }

            if (log.isDebug())log.debug(name + " got Multi change:");
            for (Iterator iterator = changes.iterator(); iterator.hasNext();) {
                PathPropertyChangeEvent event = (PathPropertyChangeEvent) iterator.next();
                if (filterPath == null || Generic.beanPathArrayToString(event.getPathFromRoot()).equals(filterPath)){
                    graphChanged(event);
                }
            }
            wasMulti = true;
        }

        public void graphChanged(PathPropertyChangeEvent event) {
            wasMulti = false;
            observedValue = event.getNewValue();
            root = event.getRoots().iterator().next();

            history.add(event);

            if (log.isDebug())log.debug(name + " got change event on roots: " + event.getRoots() + " old: " + event.getOldValue() + " new: " + observedValue);
        }
    }

    private class TestBean extends org.bhavaya.util.DefaultObservable {
        Map propertyToListeners = new HashMap();

        public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
            List listeners = (List) propertyToListeners.get(propertyName);
            if (listeners == null) {
                listeners = new ArrayList();
                propertyToListeners.put(propertyName, listeners);
            }
            listeners.add(propertyChangeListener);

            super.addPropertyChangeListener(propertyName, propertyChangeListener);
        }

        public void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
            List listeners = (List) propertyToListeners.get(propertyName);
            assertNotNull("asking to remove a listener that has not been added. Property: " + propertyName + " listener: " + propertyChangeListener, listeners);
            boolean containedItem = listeners.remove(propertyChangeListener);
            assertTrue("asking to remove a listener that has not been added. Property: " + propertyName + " listener: " + propertyChangeListener, containedItem);

            if (log.isDebug())log.debug("Removing listener: " + propertyChangeListener + " from property: " + propertyName + " of bean:" + this);
            super.removePropertyChangeListener(propertyName, propertyChangeListener);
        }

        public List getListeners(String propertyName) {
            return (List) propertyToListeners.get(propertyName);
        }

        public int getAllListenersCount() {
            int count = 0;
            Iterator iter = propertyToListeners.values().iterator();
            while (iter.hasNext()) {
                List list = (List) iter.next();
                count += list.size();
            }
            return count;
        }

    }

    public A createNewA() {
        return new A();
    }

    public B createNewB() {
        return new B();
    }

    public C createNewC() {
        return new C();
    }

    public D createNewD() {
        return new D();
    }

    public static void main(String[] args) {
        CachedObjectGraphTest cachedObjectGraphTest = new CachedObjectGraphTest("");
        cachedObjectGraphTest.testMultipleRootMultiplePath();
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

}
