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

import org.bhavaya.collection.Association;
import org.bhavaya.collection.DefaultAssociation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Description
 *
 * @author
 * @version $Revision: 1.5 $
 */
public class RadioButtonPanel extends JPanel {
    private ArrayList listeners = new ArrayList();

    // This button is only used to support the concept of nothing selected within a ButtonGroup.  see The ButtonGroup
    // Javadoc for more information.
    private final JRadioButton noneSelectedButton = new JRadioButton("SWING HACK BUTTON");

    private ButtonGroup buttonGroup = new ButtonGroup();

    private Association buttonModelToOption = new DefaultAssociation();
    private ActionListener buttonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            fireAction();
        }
    };

    public RadioButtonPanel(Object[] optionsArray) {
        this(optionsArray, "none");
    }

    public RadioButtonPanel(Object[] optionsArray, int vGap) {
        this(optionsArray, "none", null, vGap);
    }

    public RadioButtonPanel(Object[] optionsArray, String nullOptionText) {
        this(optionsArray, nullOptionText, null);
    }

    public RadioButtonPanel(Object[] optionsArray, LayoutManager layoutManager) {
        this(optionsArray, "none", layoutManager);
    }

    public RadioButtonPanel(Object[] optionsArray, String nullOptionText, LayoutManager layoutManager) {
        this(optionsArray, nullOptionText, layoutManager, 0);
    }

    public RadioButtonPanel(Object[] optionsArray, String nullOptionText, LayoutManager layoutManager, int vGap) {
        if (layoutManager == null) {
            layoutManager = new BoxLayout(this, BoxLayout.Y_AXIS);
        }

        setLayout(layoutManager);

        for (int i = 0; i < optionsArray.length; i++) {
            String text = optionsArray[i] != null ? optionsArray[i].toString() : nullOptionText;
            JRadioButton button = new JRadioButton(text);
            button.setAlignmentX(0);
            button.addActionListener(buttonListener);
            add(button);
            addVerticalStrut(layoutManager, vGap, i != optionsArray.length - 1);
            buttonGroup.add(button);
            buttonModelToOption.put(button.getModel(), optionsArray[i]);
        }

        buttonGroup.add(noneSelectedButton);
    }

    private void addVerticalStrut(LayoutManager layoutManager, int vGap, boolean notLastElement) {
        if (layoutManager instanceof BoxLayout && vGap != 0 && notLastElement) {
            add(Box.createVerticalStrut(vGap));
        }
    }

    public Object getSelectedOption() {
        ButtonModel selectedModel = buttonGroup.getSelection();
        if (selectedModel == noneSelectedButton.getModel()) {
            return null;
        } else {
            Object option = buttonModelToOption.get(selectedModel);
            return option;
        }
    }

    public void setSelectedOption(Object option) {
        if (option != null) {
            ButtonModel buttonModel = (ButtonModel) buttonModelToOption.getKeyForValue(option);
            if (buttonModel != null) {
                buttonModel.setSelected(true);
            }
        } else if (getSelectedOption() != null) {
            noneSelectedButton.setSelected(true);
        }
    }

    public void addActionListener(ActionListener actionListener) {
        listeners.add(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        listeners.remove(actionListener);
    }

    protected void fireAction() {
        ActionEvent evt = new ActionEvent(this, 0, getSelectedOption().toString());
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ActionListener actionListener = (ActionListener) iterator.next();
            actionListener.actionPerformed(evt);
        }
    }
}
