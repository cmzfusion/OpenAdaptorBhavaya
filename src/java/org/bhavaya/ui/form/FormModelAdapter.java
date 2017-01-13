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

package org.bhavaya.ui.form;

import org.bhavaya.ui.EditableComponent;
import org.bhavaya.ui.adaptor.QuantityAdapter;
import org.bhavaya.util.Transform;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * As part of the bind, it uses the formModel to set others properties of the gui component, e.g. the enabled and visible properties.
 * Dan: Its extension of QuantityAdapter is rather questionable, but I'll turn a blind eye because I have better things to do right now!
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class FormModelAdapter extends QuantityAdapter {
    FormModel formModel;

    public FormModelAdapter(FormModel formModel) {
        this(formModel, null, null);
    }

    public FormModelAdapter(FormModel formModel, String unitProperty, String rateDateProperty) {
        super(formModel.getBean(), unitProperty, rateDateProperty);
        this.formModel = formModel;
    }

    protected void bind(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform, String guiGetterProperty, Transform guiGetterTransform, boolean bindGuiToBean, boolean bindBeanToGui) {
        setComponentStateFromFormModel(formModel, propertyName, guiComponent);
        super.bind(propertyName, guiComponent, guiSetterProperty, guiSetterTransform, guiGetterProperty, guiGetterTransform, bindGuiToBean, bindBeanToGui);
    }

    /**
     * This method updates the initial state of a ui component from the values provided by a form model
     *
     * Some components are not bound to bean properties, or perform their own binding (bad idea), but these still require their state to be set when a form is
     * created. These components have ended up duplicating the logic to set their initial state based on the form settings. I have made this logic
     * static in FormModelAdapter so that I can remove this duplication.
     */
    public static void setComponentStateFromFormModel(FormModel formModel, String propertyName, Object guiComponent) {
        boolean propertyEditable = formModel.isPropertyEditable(propertyName);
        boolean propertyEnabled = formModel.isPropertyEnabled(propertyName);
        boolean propertyVisible = formModel.isPropertyVisible(propertyName);

        if (guiComponent instanceof JComponent) {
            JComponent component = (JComponent) guiComponent;
            component.setEnabled(propertyEnabled);
            component.setVisible(propertyVisible);
            if (!propertyEditable) {
                if (guiComponent instanceof EditableComponent) {
                    ((EditableComponent)guiComponent).setEditable(propertyEditable);
                } else if (guiComponent instanceof JTextComponent) {
                    ((JTextComponent)guiComponent).setEditable(propertyEditable);
                } else if (guiComponent instanceof JComboBox) {
                    /**
                     * Setting editable to false on combo box doesn't help much as one still can choose
                     * from the combo-box's options (drop down list). Rather use text component or restrict
                     * the number of options to one.
                     */
                    ((JComboBox)guiComponent).setEditable(propertyEditable);
                }
            }
            if (!formModel.isPropertyFocusable(propertyName)) component.setFocusable(false); // only ever disable focus if necessary
        }
    }


}
