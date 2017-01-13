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

import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 5/10/13
 * Time: 3:15 PM
 */
public class DateFunctionDlgPanel  extends JPanel {

    public static final ImageIcon FIXED = ImageIconCache.getImageIcon("fixedDate.png");
    public static final ImageIcon RELATIVE = ImageIconCache.getImageIcon("relativeDate.png");
    public static final ImageIcon DEFAULT = ImageIconCache.getImageIcon("defaultDate.png");

    public static final SymbolicDateFunction[] SYMBOLIC_DATES = new  SymbolicDateFunction[]{
            SymbolicDateFunction.TODAY_DATEFUNCTION,
            SymbolicDateFunction.START_OF_WEEK_DATEFUNCTION,
            SymbolicDateFunction.START_OF_MONTH_DATEFUNCTION,
            SymbolicDateFunction.START_OF_YEAR_DATEFUNCTION,
            SymbolicDateFunction.TIME_BEGIN,
            SymbolicDateFunction.TIME_END};


    private JComboBox symbolicDatesComboBox;
    private JComboBox relativeDatePrepositionsComboBox;
    private DecimalTextField relativeDateOffset;
    private JComboBox relativeDateOffsetTypes;
    private BhavayaDateSpinner dateSpinner;
    private AbstractAction symbolicDateAction;
    private AbstractAction relativeDateAction;
    private AbstractAction specificDateAction;
    private JRadioButton defaultDateBtn;
    private JRadioButton relativeDateBtn;
    private JRadioButton specificDateBtn;
    private JLabel relativeDateLabel;
    private DateFunction referenceDate;

    private JLabel defaultDateLabel;
    private JLabel relativedateLabel;
    private JLabel fixedDateLabel;

    private List defaultDateFunctions = DateFunctionPanel.getDefaultDateFunctions(null);

    public DateFunctionDlgPanel(DateFunction referenceDate) {

        this.referenceDate = referenceDate;

        FlowLayout flow = new FlowLayout(FlowLayout.LEFT, 5, 0);

        symbolicDatesComboBox = new JComboBox(SYMBOLIC_DATES);
        symbolicDatesComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(((DateFunction) value).getVerboseDescription());
                return label;
            }
        });

        relativeDateLabel = new JLabel(referenceDate.getVerboseDescription());
        relativeDatePrepositionsComboBox = new JComboBox(RelativeDateFunction.getPrepositions());
        relativeDateOffset = new DecimalTextField("###0", 4, new Integer(1));
        relativeDateOffsetTypes = new JComboBox(RelativeDateFunction.getCalendarOffsetTypes());
        dateSpinner = new BhavayaDateSpinner(Calendar.DAY_OF_MONTH, DateUtilities.newDate());
        dateSpinner.setDefaultCaretPosition(DateFormatProvider.getInstance().getDateFormat().indexOf("d"));

        symbolicDateAction = new AbstractAction("Default") {
            public void actionPerformed(ActionEvent e) {
                symbolicDatesComboBox.setEnabled(true);
                relativeDateOffset.setEnabled(false);
                relativeDateOffsetTypes.setEnabled(false);
                relativeDatePrepositionsComboBox.setEnabled(false);
                relativeDateLabel.setEnabled(false);
                dateSpinner.setEnabled(false);

                defaultDateLabel.setEnabled(true);
                fixedDateLabel.setEnabled(false);
                relativeDateLabel.setEnabled(false);

                defaultDateBtn.setSelected(true);
            }
        };
        defaultDateBtn = new JRadioButton(symbolicDateAction);

        relativeDateAction = new AbstractAction("Relative") {
            public void actionPerformed(ActionEvent e) {
                symbolicDatesComboBox.setEnabled(false);
                relativeDateOffset.setEnabled(true);
                relativeDateOffsetTypes.setEnabled(true);
                relativeDatePrepositionsComboBox.setEnabled(true);
                relativeDateLabel.setEnabled(true);
                dateSpinner.setEnabled(false);

                defaultDateLabel.setEnabled(false);
                fixedDateLabel.setEnabled(false);
                relativeDateLabel.setEnabled(true);

                relativeDateBtn.setSelected(true);
            }
        };
        relativeDateBtn = new JRadioButton(relativeDateAction);

        specificDateAction = new AbstractAction("Fixed") {
            public void actionPerformed(ActionEvent e) {
                symbolicDatesComboBox.setEnabled(false);
                relativeDateOffset.setEnabled(false);
                relativeDateOffsetTypes.setEnabled(false);
                relativeDatePrepositionsComboBox.setEnabled(false);
                relativeDateLabel.setEnabled(false);
                dateSpinner.setEnabled(true);

                defaultDateLabel.setEnabled(false);
                fixedDateLabel.setEnabled(true);
                relativeDateLabel.setEnabled(false);

                specificDateBtn.setSelected(true);
            }
        };

        this.setLayout(new CompactGridLayout(3,2,15,15));
        specificDateBtn = new JRadioButton(specificDateAction);


        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultDateBtn);
        buttonGroup.add(relativeDateBtn);
        buttonGroup.add(specificDateBtn);

        JPanel defaultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        defaultPanel.add(symbolicDatesComboBox);

        JPanel relativePanel = new JPanel(flow);
        relativePanel.add(relativeDateLabel = new JLabel(RELATIVE));
        relativePanel.add(relativeDateBtn);
        relativePanel.add(relativeDateOffset);
        relativePanel.add(relativeDateOffsetTypes);
        relativePanel.add(relativeDatePrepositionsComboBox);

        JPanel specificPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        specificPanel.add(dateSpinner);

        add(defaultDateBtn);
        JPanel defP = new JPanel(flow);
        defP.add(defaultDateLabel = new JLabel(DEFAULT));
        defP.add(symbolicDatesComboBox);
        add(defP);

        add(relativeDateBtn);
        add(relativePanel);

        add(specificDateBtn);
        JPanel fixP = new JPanel(flow);
        fixP.add(fixedDateLabel = new JLabel(FIXED));
        fixP.add(dateSpinner);
        add(fixP);

        setInitValue(referenceDate);
    }

    public DateFunction getDateFunction() {
        if (defaultDateBtn.isSelected()) {
            return (DateFunction) symbolicDatesComboBox.getSelectedItem();
        }
        else if (relativeDateBtn.isSelected()) {
            return new RelativeDateFunction(referenceDate, (RelativeDateFunction.OffsetType) relativeDateOffsetTypes.getSelectedItem(), (RelativeDateFunction.Preposition) relativeDatePrepositionsComboBox.getSelectedItem(), relativeDateOffset.getValue().intValue());
        }
        else if (specificDateBtn.isSelected()) {
            return new FixedDateFunction((java.util.Date) dateSpinner.getValue(), DateUtilities.newGmtDateFormat("yyyy-MM-dd"));
        }
        else {
            return null;
        }
    }

    private void setInitValue(DateFunction value) {
        if(value instanceof FixedDateFunction ){
            specificDateAction.actionPerformed(null);
            dateSpinner.setDate(value.getDate());
        }
        else if(value instanceof RelativeDateFunction ){
            relativeDateAction.actionPerformed(null);
            RelativeDateFunction relativeDateFunction = (RelativeDateFunction) value;
            relativeDateOffset.setValue(new Integer(relativeDateFunction.getOffset()));
            relativeDateOffsetTypes.setSelectedItem(relativeDateFunction.getCalendarOffsetType());
            relativeDatePrepositionsComboBox.setSelectedItem(relativeDateFunction.getPreposition());
        }
        else if(value instanceof SymbolicDateFunction ){
            symbolicDatesComboBox.setSelectedItem(value);
            symbolicDateAction.actionPerformed(null);
        }
    }


}
