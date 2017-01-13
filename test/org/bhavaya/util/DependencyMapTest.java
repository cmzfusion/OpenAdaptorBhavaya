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

import org.bhavaya.util.DependencyMap;
import org.bhavaya.util.Log;
import org.bhavaya.util.Observable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * Description
 *
 * @author
 * @version $Revision: 1.1 $
 */
public class DependencyMapTest implements Observable, DependencyMap.Dependant {
    private static final Log log = Log.getCategory(DependencyMapTest.class);

    private static DependencyMap dependencyMap;
    private List propertyChangeListeners = new ArrayList();

    public void addDependencies(DependencyMap dependencyMap, String name) {
        dependencyMap.addDependency("a", "updateA", new String[]{"b"});
        dependencyMap.addDependency("b", "updateB", new String[]{"c"});
        dependencyMap.addDependency("c", "updateC", new String[]{"a", "g"});
        dependencyMap.addDependency("d", "updateD", new String[]{"e"});
        dependencyMap.addDependency("e", "updateE", new String[]{"g", "f"});
        dependencyMap.addDependency("f", "updateF", new String[]{"b"});
        dependencyMap.addDependency("g", "updateG", new String[]{"d"});
        dependencyMap.addDependency("h", "updateH", new String[]{"g"});
    }


    protected Integer a;
    protected Integer b;
    protected Integer c;
    protected Integer d;
    protected Integer e;
    protected Integer f;
    protected Integer g;
    protected Integer h;

    public static void main(String[] args) {
        DependencyMapTest test = new DependencyMapTest();
        dependencyMap.touchField("e", test);
//        dependencyMap.touchField("g", test);
//        dependencyMap.touchField("d", test);
        /** Start the OptimizeIt audit system with some default options **/
//    Audit.start(1470, Audit.SYSTEM_EXIT_DISABLED | Audit.WAIT_FOR_OPTIMIZEIT | Audit.WAIT_FOR_FILTERS);
//        Audit.disableCPUProfiler();
        System.out.println("Starting audit:");
        for (int i = 0; i < 1000; i++) {
            test.setF(new Integer(2));
        }
        System.out.println("\n\n\n\nStarting cpu profile:");
//        Audit.enableCPUProfiler();
        for (int i = 0; i < 1000; i++) {
            test.setF(new Integer(2));
        }

//        Audit.disableCPUProfiler();
        System.out.println("disabled:");

        test.setF(new Integer(2));

    }


    public DependencyMapTest() {
        a = new Integer(1);
        b = new Integer(2);
        c = new Integer(4);
        d = new Integer(0);
        e = new Integer(5);
        f = new Integer(6);
        g = new Integer(-3);
        h = new Integer(-4);

        dependencyMap = DependencyMap.getInstance(this);
        dependencyMap.monitor(this);
    }


    //--------------------- Observable interface -------------------
    //-----------------------------------------------------------------------------------------
    /**
     * Observable implementation.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.add(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Observable implementation.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.remove(l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Observable implementation.
     */
    protected void firePropertyChanged(String propertyName, Object oldValue, Object newValue) {
        if (newValue == null && oldValue != null || !newValue.equals(oldValue)) {
            PropertyChangeEvent e = new PropertyChangeEvent(this, propertyName, oldValue, newValue);

            int size = propertyChangeListeners.size();
            for (int i = 0; i < size; i++) {
                ((PropertyChangeListener) propertyChangeListeners.get(i)).propertyChange(e);
            }
        }
    }

    protected void setProperty(String propertyName, Object newValue) {
        System.out.println("Setting: " + propertyName + " to: " + newValue);
        try {
            Class clazz = this.getClass();
            Field field = null;

            while (field == null) {
                try {
                    field = clazz.getDeclaredField(propertyName);
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    //sorry, no other way of doing this without catching exceptions!! yuk.
                    clazz = clazz.getSuperclass();
                    if (clazz == null) throw new RuntimeException("Cannot find field: " + propertyName + " in class: " + this.getClass().getName());
                }
            }

            Object oldValue = field.get(this);
            if (oldValue != newValue) {
                field.set(this, newValue);
                firePropertyChanged(propertyName, oldValue, newValue);
            }
        } catch (Exception e) {
            log.error("setProperty(" + propertyName + ", " + newValue + ")", e);
        }
    }

    public void setProperty(String propertyName, int newValue) {
        setProperty(propertyName, new Integer(newValue));
    }

    public void setProperty(String propertyName, boolean newValue) {
        setProperty(propertyName, new Boolean(newValue));
    }

    public void setProperty(String propertyName, double newValue) {
        setProperty(propertyName, new Double(newValue));
    }


    public void updateA() {
        System.out.println("updating A from b=" + getB());
        setA(new Integer(getB().intValue() * 2));
    }

    public void updateB() {
        System.out.println("updating B from c=" + getC());
        setB(new Integer(getC().intValue() * 2));
    }

    public void updateC() {
        System.out.println("updating C from A=" + getA());
        setC(new Integer(getA().intValue() / 4));
    }

    public void updateD() {
        System.out.println("updating D from c=" + getC() + " and e=" + getE());
        setD(new Integer(getC().intValue() * getE().intValue()));
    }

    public void updateE() {
        System.out.println("updating E from f=" + getF() + " and g=" + getG());
        setE(new Integer(getF().intValue() + getG().intValue()));
    }

    public void updateF() {
        System.out.println("updating F from b=" + getB());
        setF(new Integer(getB().intValue() + 2));
    }

    public void updateG() {
        System.out.println("updating G from d=" + getD());
        setG(new Integer(getD().intValue() - 8));
    }

    public void updateH() {
        System.out.println("updating H from g=" + getG());
        setH(new Integer(getG().intValue() - 10));
    }

    //---------------- override setters to work with observable
    public Integer getA() {
        return a;
    }

    public Integer getB() {
        return b;
    }

    public Integer getC() {
        return c;
    }

    public Integer getD() {
        return d;
    }

    public Integer getE() {
        return e;
    }

    public Integer getF() {
        return f;
    }

    public Integer getG() {
        return g;
    }

    public Integer getH() {
        return h;
    }

    public void setA(Integer a) {
        setProperty("a", a);
    }

    public void setB(Integer b) {
        setProperty("b", b);
    }

    public void setC(Integer c) {
        setProperty("c", c);
    }

    public void setD(Integer d) {
        setProperty("d", d);
    }

    public void setE(Integer e) {
        setProperty("e", e);
    }

    public void setF(Integer f) {
        setProperty("f", f);
    }

    public void setG(Integer g) {
        setProperty("g", g);
    }

    public void setH(Integer h) {
        setProperty("h", h);
    }
}
