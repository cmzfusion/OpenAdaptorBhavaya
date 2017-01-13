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

import org.bhavaya.util.*;

import java.beans.PropertyChangeListener;

/**
 * utility class for when you have a form that has multiple quantities with the same unit.
 * If you use thsi model to bind gui components with quantities and use the default pipes (i.e. any of the bind methods
 * except Adapter.bind(String beanPropertyName, JComponent guiComponent, Pipe beanToGuiPipe, Pipe guiToBeanPipe)) then
 * all quantities constructed from the gui will query the given unitPropertyPath in order to obtain a unit for the new quantity.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.8 $
 */
public class QuantityAdapter extends Adapter {
    private static final Log log = Log.getCategory(QuantityAdapter.class);
    private String unitPropertyBeanPath;
    private String rateDatePropertyBeanPath;

    public QuantityAdapter(Observable bean, String unitPropertyBeanPath, String rateDatePropertyBeanPath) {
        super(bean);
        this.unitPropertyBeanPath = unitPropertyBeanPath;
        this.rateDatePropertyBeanPath = rateDatePropertyBeanPath;
    }

    protected Pipe createBeanToGuiPipe(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform) {
        Class propertyType = PropertyModel.getInstance(getBean().getClass()).getAttribute(Generic.beanPathStringToArray(propertyName)).getType();

        if (Quantity.class.isAssignableFrom(propertyType)) {
            return createQuantityToGuiPipe(propertyName, guiComponent, guiSetterProperty, guiSetterTransform);
        } else {
            return super.createBeanToGuiPipe(propertyName, guiComponent, guiSetterProperty, guiSetterTransform);
        }
    }

    protected Pipe createGuiToBeanPipe(Object guiComponent, String guiGetterProperty, String propertyName, Transform guiGetterTransform) {
        Class propertyType = PropertyModel.getInstance(getBean().getClass()).getAttribute(Generic.beanPathStringToArray(propertyName)).getType();

        if (Quantity.class.isAssignableFrom(propertyType)) {
            return createGuiToQuantityPipe(guiComponent, guiGetterProperty, propertyName, guiGetterTransform);
        } else {
            return super.createGuiToBeanPipe(guiComponent, guiGetterProperty, propertyName, guiGetterTransform);
        }
    }


    protected PropertyPipe createQuantityToGuiPipe(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform) {
        Transform quantityToGuiTransform = guiSetterTransform;
        if (quantityToGuiTransform == null) quantityToGuiTransform = Transform.QUANTITY_TO_DOUBLE;

        return new AWTPropertyPipe(getBean(), propertyName, guiComponent, guiSetterProperty, quantityToGuiTransform);
    }

    protected Pipe createGuiToQuantityPipe(Object guiComponent, String guiGetterProperty, String propertyName, Transform guiGetterTransform) {
        Pipe guiToBean;
        Transform guiToDoubleTransform = guiGetterTransform;

        Source unitSource;
        if (unitPropertyBeanPath == null) {
            log.warn("Binding a quantity (source obj = " + getBean() + " property = " + propertyName + ") but no unitPropertyBeanPath set");
            unitSource = new ValueSource(null);
        } else {
            unitSource = new PropertySource(getBean(), unitPropertyBeanPath);
        }

        Source rateDateSource;
        if (rateDatePropertyBeanPath == null) {
            log.warn("Binding a quantity (source obj = " + getBean() + " property = " + propertyName + ") but no rateDatePropertyBeanPath set");
            rateDateSource = new ValueSource(null);
        } else {
            rateDateSource = new PropertySource(getBean(), rateDatePropertyBeanPath);
        }

        guiToBean = new ValueToQuantityPipe(guiComponent, guiGetterProperty, getBean(), propertyName, guiToDoubleTransform, unitSource, rateDateSource);
        //also execute from gui to bean whenever the unit changes
        if (unitPropertyBeanPath != null) {
            addPropertyChangeListener(unitPropertyBeanPath, (PropertyChangeListener) guiToBean.getListenerInterface(PropertyChangeListener.class));
        }

        //also execute from gui to bean whenever the rateDate changes
        if (rateDatePropertyBeanPath != null) {
            addPropertyChangeListener(rateDatePropertyBeanPath, (PropertyChangeListener) guiToBean.getListenerInterface(PropertyChangeListener.class));
        }

        return guiToBean;
    }
}
