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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.BeanPathSelector;
import org.bhavaya.ui.FilteredTreeModel;
import org.bhavaya.ui.SearchableBeanPathSelector;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class BeanPathSelectorTest {
    private static final Log log = Log.getCategory(BeanPathSelectorTest.class);

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel p = new JPanel(new GridLayout(1, 1));

        BeanCollection beanCollection = new DefaultBeanCollection(Root.class);
        BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, false);
        SearchableBeanPathSelector selector = new SearchableBeanPathSelector(
                beanCollectionTableModel,
                FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER,
                new FilteredTreeModel.Filter() {
                    public boolean isValidProperty(Class beanPathRootClass, Class parentClass, Class propertyClass, String propertyName, String propertyBeanPath) {
                        return true;
                    }
                },
                new BeanPathSelector.SelectionModel(true) {
                    public void locatorSelected(String columnLocator) {
                    }

                    public boolean isSelected(String columnLocator) {
                        return false;
                    }
                });
        p.add( selector );
        frame.setContentPane(p);
        frame.pack();
        frame.show();
    }

    public static class Root {
        private int number;
        private String name;
        private A a;
        private B b;
        private C c;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }

        public C getC() {
            return c;
        }
    }

    public static class A extends C{
        private String foo;
        private int numero;

        public int getNumero() {
            return numero;
        }

        public void setNumero(int numero) {
            this.numero = numero;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;

        }
    }

    public static class B extends C{
        private String bar;
        private String nombre;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    public static class C {
        private String cString;

        public String getcString() {
            return cString;
        }
    }
}
