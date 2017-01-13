package org.bhavaya.ui;

import org.bhavaya.beans.criterion.CriterionGroup;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Arrays;

/**
 * User: Jonathan Moore
 * Date: 18-Nov-2010
 * Time: 11:47:16
 */
public class CriterionCheckBox extends JCheckBox implements CriteriaSelector{

    private CriterionGroup criterionGroup;
    private CriterionGroup onCriterionGroup;
    private CriterionGroup offCriterionGroup;

    public CriterionCheckBox(String text, boolean selected, CriterionGroup onCriterionGroup, CriterionGroup offCriterionGroup) {
        super(text, selected);
        this.onCriterionGroup = onCriterionGroup;
        this.offCriterionGroup = offCriterionGroup;
        this.criterionGroup = new CriterionGroup(text, selected ? onCriterionGroup : offCriterionGroup);
        addActionListener(new CriteriaChangeActionListener());
    }

    private void setCriterionGroup() {
        getCriterionGroup().setCriteria(isSelected() ? onCriterionGroup.getCriteria() : offCriterionGroup.getCriteria());
    }

    public CriterionGroup getCriterionGroup() {
        return criterionGroup;
    }

    public void selectMatchingCriteria(CriterionGroup criterionGroup) {
        this.criterionGroup.setCriteria(criterionGroup.getCriteria());
        setSelected(Arrays.equals(onCriterionGroup.getCriteria(), criterionGroup.getCriteria()));
    }

    private static class CriteriaChangeActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            final CriterionCheckBox checkBox = (CriterionCheckBox) e.getSource();
            checkBox.setCriterionGroup();
        }
    }
}
