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

package org.bhavaya.ui.adaptor;

import org.bhavaya.ui.NarrowableComboBox;
import org.bhavaya.ui.RadioButtonPanel;
import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.GraphChangeListener;
import org.bhavaya.ui.table.PathPropertyChangeEvent;
import org.bhavaya.util.*;
import org.bhavaya.util.Observable;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class Adapter {
    private static final Log log = Log.getCategory(Adapter.class);

    private Observable bean;
    private CachedObjectGraph objectGraph;
    private java.util.List listenersAddedToBean = new ArrayList();
    private java.util.Map guiToBeanPipes = new HashMap();
    private java.util.Map beanToGuiPipes = new HashMap();

    public Adapter(Observable bean) {
        assert bean != null;
        this.bean = bean;
        this.objectGraph = new CachedObjectGraph(bean.getClass());
        objectGraph.addRootObject(bean);
    }

    public void bind(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform, String guiGetterProperty, Transform guiGetterTransform) {
        bind(propertyName, guiComponent, guiSetterProperty, guiSetterTransform, guiGetterProperty, guiGetterTransform, true, true);
    }

    public void bind(String propertyName, Object guiComponent, String guiSetterProperty, String guiGetterProperty) {
        bind(propertyName, guiComponent, guiSetterProperty, null, guiGetterProperty, null, true, true);
    }

    public void bindOneWayBeanToGui(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform) {
        bind(propertyName, guiComponent, guiSetterProperty, guiSetterTransform, null, null, false, true);
    }

    public void bindOneWayBeanToGui(String propertyName, Object guiComponent, String guiSetterProperty) {
        bind(propertyName, guiComponent, guiSetterProperty, null, null, null, false, true);
    }

    public void bindOneWayGuiToBean(String propertyName, Object guiComponent, String guiGetterProperty, Transform guiGetterTransform) {
        bind(propertyName, guiComponent, null, null, guiGetterProperty, guiGetterTransform, true, false);
    }

    public void bindOneWayGuiToBean(String propertyName, Object guiComponent, String guiGetterProperty) {
        bind(propertyName, guiComponent, null, null, guiGetterProperty, null, true, false);
    }

    public void bind(String propertyName, Object guiComponent, String guiProperty) {
        bind(propertyName, guiComponent, guiProperty, null, guiProperty, null, true, true);
    }

    protected void bind(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform, String guiGetterProperty, Transform guiGetterTransform, boolean bindGuiToBean, boolean bindBeanToGui) {
        Pipe guiToBean = null;
        Pipe beanToGui = null;

        if (bindBeanToGui) {
            beanToGui = createBeanToGuiPipe(propertyName, guiComponent, guiSetterProperty, guiSetterTransform);
        }

        if (bindGuiToBean) {
            guiToBean = createGuiToBeanPipe(guiComponent, guiGetterProperty, propertyName, guiGetterTransform);
        }

        bind(propertyName, guiComponent, beanToGui, guiToBean);
    }

    protected Pipe createBeanToGuiPipe(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform) {
        return new AWTPropertyPipe(bean, propertyName, guiComponent, guiSetterProperty, guiSetterTransform);
    }

    protected Pipe createGuiToBeanPipe(Object guiComponent, String guiGetterProperty, String propertyName, Transform guiGetterTransform) {
        return new PropertyPipe(guiComponent, guiGetterProperty, bean, propertyName, guiGetterTransform);
    }

    public void bind(String beanPropertyName, Object guiComponent, Pipe beanToGuiPipe, Pipe guiToBeanPipe) {
        if (guiToBeanPipe != null && guiComponent instanceof JComponent) {
            if (beanToGuiPipe != null) guiToBeanPipe.setReversePipe(beanToGuiPipe);
            Class listenerClass = getListenerClassForComponent((JComponent) guiComponent);
            addPipeAsListenerToGuiComponent((JComponent) guiComponent, guiToBeanPipe, listenerClass);
            guiToBeanPipes.put(beanPropertyName, guiToBeanPipe);
        }

        if (beanToGuiPipe != null) {
            if (guiToBeanPipe != null) beanToGuiPipe.setReversePipe(guiToBeanPipe);
            beanToGuiPipe.execute(); // set initial value on gui
            addPropertyChangeListener(beanPropertyName, (PropertyChangeListener) beanToGuiPipe.getListenerInterface(PropertyChangeListener.class));
            beanToGuiPipes.put(beanPropertyName, beanToGuiPipe);
        }
    }


    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        bean.addPropertyChangeListener(propertyChangeListener);
        listenersAddedToBean.add(propertyChangeListener);
    }

    public void addPropertyChangeListener(String propertyPath, final PropertyChangeListener propertyChangeListener) {
        GraphChangeToPropertyChangeAdapter listener = new GraphChangeToPropertyChangeAdapter(propertyPath, propertyChangeListener);
        objectGraph.addPathListener(propertyPath, listener);
    }

    public void dispose() {
        for (Iterator iterator = listenersAddedToBean.iterator(); iterator.hasNext();) {
            PropertyChangeListener propertyChangeListener = (PropertyChangeListener) iterator.next();
            bean.removePropertyChangeListener(propertyChangeListener);
        }
        listenersAddedToBean.clear();
        objectGraph.dispose();
    }

    public void flushAllBeanToGui() {
        for (Iterator iterator = beanToGuiPipes.values().iterator(); iterator.hasNext();) {
            ((Pipe) iterator.next()).execute();
        }
    }

    public void flushAllGuiToBean() {
        for (Iterator iterator = guiToBeanPipes.values().iterator(); iterator.hasNext();) {
            ((Pipe) iterator.next()).execute();
        }
    }

    /**
     * I might refactor this so that you can call "addListenerClassForComponent" instead of having to extend Adapter.
     * but not necessary yet.
     * @param guiComponent
     * @return
     */
    protected Class getListenerClassForComponent(JComponent guiComponent) {
        Class listenerClass;
        if (guiComponent instanceof JComboBox || guiComponent instanceof NarrowableComboBox
                || guiComponent instanceof AbstractButton || guiComponent instanceof RadioButtonPanel) {
            listenerClass = ActionListener.class;
        } else if (guiComponent instanceof JSpinner) {
            listenerClass = ChangeListener.class;
        } else if (guiComponent instanceof JList) {
            listenerClass = ListSelectionListener.class;
        } else {
            listenerClass = FocusListener.class;
        }
        return listenerClass;
    }

    private static void addPipeAsListenerToGuiComponent(JComponent guiComponent, Pipe guiToBeanPipe, Class listenerClass) {
        String addMethod = "add" + ClassUtilities.getUnqualifiedClassName(listenerClass);
        try {
            Class guiComponentClass = guiComponent.getClass();
            Method method = guiComponentClass.getMethod(addMethod, listenerClass);
            Object listener = guiToBeanPipe.getListenerInterface(listenerClass);
            method.invoke(guiComponent, listener);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public Object getBean() {
        return bean;
    }

    public static void linkCausality(Adapter causeAdapter, String causeProperty, Adapter effectAdapter, String effectProperty) {
        ((Pipe) causeAdapter.beanToGuiPipes.get(causeProperty)).setReversePipe((Pipe) effectAdapter.beanToGuiPipes.get(effectProperty));
    }

    /**
     * Allows you to "prime the pipe" with a certain value.  This will be considered the value that was last set for
     * a particular property.  Is used to prevent updates for a particular (usually initial) value.
     */
    public void primeOutput(String property, Object primedValue) {
        ((Pipe) beanToGuiPipes.get(property)).prime(primedValue);
    }

    private static class GraphChangeToPropertyChangeAdapter implements GraphChangeListener {
        private String[] propertyPath;
        private final PropertyChangeListener propertyChangeListener;

        public GraphChangeToPropertyChangeAdapter(String propertyPathString, PropertyChangeListener propertyChangeListener) {
            this.propertyPath = Generic.beanPathStringToArray(propertyPathString);
            this.propertyChangeListener = propertyChangeListener;
        }

        public void graphChanged(PathPropertyChangeEvent event) {
            propertyChangeListener.propertyChange(event);
        }

        public void multipleChange(Collection changes, boolean allAffectSameRoots) {
            for (Iterator iterator = changes.iterator(); iterator.hasNext();) {
                PathPropertyChangeEvent event = (PathPropertyChangeEvent) iterator.next();
                if (Arrays.equals(event.getPathFromRoot(), propertyPath)) {
                    propertyChangeListener.propertyChange(event);
                }
            }
        }
    }
}
