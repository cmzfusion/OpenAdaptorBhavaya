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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.bhavaya.util.DefaultObservable;
import org.bhavaya.util.Utilities;
import org.bhavaya.util.BeanPropertyChangeSupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class DefaultObserverableTest extends TestCase {
    protected void setUp() throws Exception {
        super.setUp();
        BeanPropertyChangeSupport.safe = true;
    }

    public void testSingleNamedPropertyChange() {
        DummyObservable dummyObservable = new DummyObservable();
        dummyObservable.addPropertyChangeListener("property1", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property1", new Short((short) 0), new Short((short) 1))));
        dummyObservable.addPropertyChangeListener("property2", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property2", new Integer(0), new Integer(1))));
        dummyObservable.addPropertyChangeListener("property3", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property3", new Long(0), new Long(1))));
        dummyObservable.addPropertyChangeListener("property4", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property4", new Float(0), new Float(1))));
        dummyObservable.addPropertyChangeListener("property5", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property5", new Double(0), new Double(1))));
        dummyObservable.addPropertyChangeListener("property6", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property6", new Boolean(false), new Boolean(true))));
        dummyObservable.addPropertyChangeListener("property7", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property7", new Character('\0'), new Character('\n'))));
        dummyObservable.addPropertyChangeListener("property8", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property8", new Byte((byte) 0), new Byte((byte) 1))));
        dummyObservable.addPropertyChangeListener("property9", new SinglePropertyChangeListener(new PropertyChangeEvent(dummyObservable, "property9", null, "test")));

        dummyObservable.setProperty1((short) 1);
        dummyObservable.setProperty2(1);
        dummyObservable.setProperty3(1);
        dummyObservable.setProperty4(1);
        dummyObservable.setProperty5(1);
        dummyObservable.setProperty6(true);
        dummyObservable.setProperty7('\n');
        dummyObservable.setProperty8((byte) 1);
        dummyObservable.setProperty9("test");
    }

    public void testUnnamedPropertyChange() {
        final DummyObservable dummyObservable = new DummyObservable();

        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
                    int index = -1;

                    public void propertyChange(PropertyChangeEvent evt) {
                        index++;

                        switch (index) {
                            case 0:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property1", new Short((short) 0), new Short(Short.MAX_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 1:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property2", new Integer(0), new Integer(Integer.MIN_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 2:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property3", new Long(0), new Long(Long.MAX_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 3:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property4", new Float(0), new Float(Float.NaN)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 4:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property5", new Double(0), new Double(Double.NaN)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 5:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property6", new Boolean(false), new Boolean(true)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 6:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property7", new Character('\0'), new Character('z')))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 7:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property8", new Byte((byte) 0), new Byte((byte) 1)))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 8:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property9", null, "test"))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            case 9:
                                if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property9", "test", null))) throw new RuntimeException("Received wrong event: " + evt);
                                break;
                            default:
                                throw new RuntimeException("Received wrong event: " + evt);
                        }
                    }
                };

        dummyObservable.addPropertyChangeListener(propertyChangeListener);

        dummyObservable.setProperty1(Short.MAX_VALUE);
        dummyObservable.setProperty2(Integer.MIN_VALUE);
        dummyObservable.setProperty3(Long.MAX_VALUE);
        dummyObservable.setProperty4(Float.NaN);
        dummyObservable.setProperty5(Double.NaN);
        dummyObservable.setProperty6(true);
        dummyObservable.setProperty7('z');
        dummyObservable.setProperty8((byte) 1);
        dummyObservable.setProperty9("test");
        dummyObservable.setProperty9(null);

        // these shouldnt fire as they dont change the value
        dummyObservable.setProperty1(Short.MAX_VALUE);
        dummyObservable.setProperty2(Integer.MIN_VALUE);
        dummyObservable.setProperty3(Long.MAX_VALUE);
        dummyObservable.setProperty4(Float.NaN);
        dummyObservable.setProperty5(Double.NaN);
        dummyObservable.setProperty6(true);
        dummyObservable.setProperty7('z');
        dummyObservable.setProperty8((byte) 1);
        dummyObservable.setProperty9(null);
    }

    public void testMultipleListeners() {
        final DummyObservable dummyObservable = new DummyObservable();

        TestMultipleListenersPropertyChangeListener propertyChangeListener = new TestMultipleListenersPropertyChangeListener(dummyObservable);

        dummyObservable.addPropertyChangeListener(propertyChangeListener);
        dummyObservable.addPropertyChangeListener("property1", propertyChangeListener);
        dummyObservable.addPropertyChangeListener("property1", propertyChangeListener);
        dummyObservable.addPropertyChangeListener("property2", propertyChangeListener); // this should get an event

        dummyObservable.setProperty1(Short.MAX_VALUE); // this should trigger the listener 3 times

        Assert.assertTrue(propertyChangeListener.index == 2);
    }

    public void testAddRemoveUnnamedListener() {
        final DummyObservable dummyObservable = new DummyObservable();

        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Assert.fail();
            }
        };

        PropertyChangeListener propertyChangeListener2 = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Assert.fail();
            }
        };

        try {
            dummyObservable.removePropertyChangeListener(propertyChangeListener);
            Assert.fail("cannot remove listener before added");
        } catch (Exception e) {
        }

        dummyObservable.addPropertyChangeListener(propertyChangeListener);
        dummyObservable.addPropertyChangeListener(propertyChangeListener);
        dummyObservable.addPropertyChangeListener(propertyChangeListener2);

        try {
            dummyObservable.removePropertyChangeListener("property1", propertyChangeListener);
            Assert.fail("cannot remove an unnamed listener using named listener ");
        } catch (Exception e) {
        }

        dummyObservable.removePropertyChangeListener(propertyChangeListener);
        dummyObservable.removePropertyChangeListener(propertyChangeListener);
        dummyObservable.removePropertyChangeListener(propertyChangeListener2);

        try {
            dummyObservable.removePropertyChangeListener(propertyChangeListener);
            Assert.fail("cannot remove listener twice");
        } catch (Exception e) {
        }

        try {
            dummyObservable.removePropertyChangeListener(propertyChangeListener2);
            Assert.fail("cannot remove listener twice");
        } catch (Exception e) {
        }

        // these shouldnt fire as listeners removed
        dummyObservable.setProperty1(Short.MAX_VALUE);
        dummyObservable.setProperty2(Integer.MIN_VALUE);
        dummyObservable.setProperty3(Long.MAX_VALUE);
        dummyObservable.setProperty4(Float.NaN);
        dummyObservable.setProperty5(Double.NaN);
        dummyObservable.setProperty6(true);
        dummyObservable.setProperty7('z');
        dummyObservable.setProperty8((byte) 1);
        dummyObservable.setProperty9(null);

    }

    private static class SinglePropertyChangeListener implements PropertyChangeListener {
        private boolean receivedEvent;
        private PropertyChangeEvent event;

        public SinglePropertyChangeListener(PropertyChangeEvent event) {
            this.event = event;
            receivedEvent = false;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (receivedEvent) throw new RuntimeException("Already received an event");
            if (!equalsEvent(evt, this.event)) throw new RuntimeException("Received wrong event: " + evt);
            receivedEvent = true;
        }
    }

    private static boolean equalsEvent(PropertyChangeEvent event1, PropertyChangeEvent event2) {
        boolean equal = Utilities.equals(event1.getSource(), event2.getSource()) &&
                Utilities.equals(event1.getPropertyName(), event2.getPropertyName()) &&
                Utilities.equals(event1.getOldValue(), event2.getOldValue()) &&
                Utilities.equals(event1.getNewValue(), event2.getNewValue()) &&
                Utilities.equals(event1.getPropagationId(), event2.getPropagationId());
        return equal;
    }

    private static class DummyObservable extends DefaultObservable {
        private short property1;
        private int property2;
        private long property3;
        private float property4;
        private double property5;
        private boolean property6;
        private char property7;
        private byte property8;
        private String property9;

        public short getProperty1() {
            return property1;
        }

        public void setProperty1(short property1) {
            short oldValue = this.property1;
            this.property1 = property1;
            firePropertyChange("property1", oldValue, property1);
        }

        public int getProperty2() {
            return property2;
        }

        public void setProperty2(int property2) {
            int oldValue = this.property2;
            this.property2 = property2;
            firePropertyChange("property2", oldValue, property2);
        }

        public long getProperty3() {
            return property3;
        }

        public void setProperty3(long property3) {
            long oldValue = this.property3;
            this.property3 = property3;
            firePropertyChange("property3", oldValue, property3);
        }

        public float getProperty4() {
            return property4;
        }

        public void setProperty4(float property4) {
            float oldValue = this.property4;
            this.property4 = property4;
            firePropertyChange("property4", oldValue, property4);
        }

        public double getProperty5() {
            return property5;
        }

        public void setProperty5(double property5) {
            double oldValue = this.property5;
            this.property5 = property5;
            firePropertyChange("property5", oldValue, property5);
        }

        public boolean isProperty6() {
            return property6;
        }

        public void setProperty6(boolean property6) {
            boolean oldValue = this.property6;
            this.property6 = property6;
            firePropertyChange("property6", oldValue, property6);
        }

        public char getProperty7() {
            return property7;
        }

        public void setProperty7(char property7) {
            char oldValue = this.property7;
            this.property7 = property7;
            firePropertyChange("property7", oldValue, property7);
        }

        public byte getProperty8() {
            return property8;
        }

        public void setProperty8(byte property8) {
            byte oldValue = this.property8;
            this.property8 = property8;
            firePropertyChange("property8", oldValue, property8);
        }

        public String getProperty9() {
            return property9;
        }

        public void setProperty9(String property9) {
            String oldValue = this.property9;
            this.property9 = property9;
            firePropertyChange("property9", oldValue, property9);
        }


    }

    private static class TestMultipleListenersPropertyChangeListener implements PropertyChangeListener {
        public int index = -1;
        private final DummyObservable dummyObservable;

        public TestMultipleListenersPropertyChangeListener(DummyObservable dummyObservable) {
            this.dummyObservable = dummyObservable;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            index++;

            switch (index) {
                case 0:
                    if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property1", new Short((short) 0), new Short(Short.MAX_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                    break;
                case 1:
                    if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property1", new Short((short) 0), new Short(Short.MAX_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                    break;
                case 2:
                    if (!equalsEvent(evt, new PropertyChangeEvent(dummyObservable, "property1", new Short((short) 0), new Short(Short.MAX_VALUE)))) throw new RuntimeException("Received wrong event: " + evt);
                    break;
                default:
                    throw new RuntimeException("Received wrong event: " + evt);
            }
        }
    }
}
