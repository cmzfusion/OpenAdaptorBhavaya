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

import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.util.GenericStringRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */

public class CriterionComboBox extends JComboBox implements CriteriaSelector {
    private CriterionGroup criterionGroup;

    public CriterionComboBox(String groupName, CriterionGroup[] criterionGroups) {
        super(criterionGroups);
        this.criterionGroup = new CriterionGroup(groupName, criterionGroups[0].getCriteria());
        setRenderer(UIUtilities.createListCellRenderer(new GenericStringRenderer("name")));
        addItemListener(new CriteriaChangeItemListener());
    }

    public void selectMatchingCriteria(CriterionGroup criterionGroup) {
        ComboBoxModel model = getModel();

        if (criterionGroup != null) {
            int size = model.getSize();
            for (int i = 0; i < size; i++) {
                CriterionGroup existingCriterionGroup = (CriterionGroup) model.getElementAt(i);
                if (Arrays.equals(existingCriterionGroup.getCriteria(), criterionGroup.getCriteria())) {
                    this.criterionGroup.setCriteria(criterionGroup.getCriteria());
                    model.setSelectedItem(existingCriterionGroup);
                    return;
                }
            }
        }

        model.setSelectedItem(model.getElementAt(0));
    }

    public CriterionGroup getCriterionGroup() {
        return criterionGroup;
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private static class CriteriaChangeItemListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            if ( e.getStateChange() == ItemEvent.SELECTED)  {
                final CriterionComboBox comboBox = (CriterionComboBox) e.getSource();
                comboBox.getCriterionGroup().setCriteria(((CriterionGroup) comboBox.getSelectedItem()).getCriteria());
            }
        }
    }

}
