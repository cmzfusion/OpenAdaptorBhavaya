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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class DateFunctionPanel extends JPanel {

    private JComboBox defaultDatesComboBox;
    private JComboBox relativeDatePrepositionsComboBox;
    private DecimalTextField relativeDateOffset;
    private JComboBox relativeDateOffsetTypes;
    private BhavayaDateSpinner dateSpinner;
    private AbstractAction defaultDateAction;
    private AbstractAction relativeDateAction;
    private AbstractAction specificDateAction;
    private JRadioButton defaultDateItem;
    private JRadioButton relativeDateItem;
    private JRadioButton specificDateItem;
    private JLabel relativeDateLabel;
    private List defaultDateFunctions;
    private DateFunction referenceDate;

    public DateFunctionPanel() {
        this(getDefaultDateFunctions(null));
    }

    public DateFunctionPanel(RelativeDateFunction.Preposition preposition) {
        this(getDefaultDateFunctions(preposition));
    }

    public DateFunctionPanel(List defaultDateFunctions) {
        this(defaultDateFunctions, SymbolicDateFunction.TODAY_DATEFUNCTION);
    }

    public DateFunctionPanel(List defaultDateFunctions, DateFunction referenceDate) {
        //super(BoxLayout.Y_AXIS);
        this.referenceDate = referenceDate;

        this.defaultDateFunctions = defaultDateFunctions = defaultDateFunctions != null ? defaultDateFunctions : getDefaultDateFunctions(null);
        defaultDatesComboBox = new JComboBox(defaultDateFunctions.toArray(new Object[defaultDateFunctions.size()]));

        defaultDatesComboBox.setRenderer(new DefaultListCellRenderer() {
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

        defaultDateAction = new AbstractAction("Default") {
            public void actionPerformed(ActionEvent e) {
                defaultDatesComboBox.setEnabled(true);
                relativeDateOffset.setEnabled(false);
                relativeDateOffsetTypes.setEnabled(false);
                relativeDatePrepositionsComboBox.setEnabled(false);
                relativeDateLabel.setEnabled(false);
                dateSpinner.setEnabled(false);

                defaultDateItem.setSelected(true);
            }
        };
        defaultDateItem = new JRadioButton(defaultDateAction);

        relativeDateAction = new AbstractAction("Relative") {
            public void actionPerformed(ActionEvent e) {
                defaultDatesComboBox.setEnabled(false);
                relativeDateOffset.setEnabled(true);
                relativeDateOffsetTypes.setEnabled(true);
                relativeDatePrepositionsComboBox.setEnabled(true);
                relativeDateLabel.setEnabled(true);
                dateSpinner.setEnabled(false);

                relativeDateItem.setSelected(true);
            }
        };
        relativeDateItem = new JRadioButton(relativeDateAction);

        specificDateAction = new AbstractAction("Fixed") {
            public void actionPerformed(ActionEvent e) {
                defaultDatesComboBox.setEnabled(false);
                relativeDateOffset.setEnabled(false);
                relativeDateOffsetTypes.setEnabled(false);
                relativeDatePrepositionsComboBox.setEnabled(false);
                relativeDateLabel.setEnabled(false);
                dateSpinner.setEnabled(true);

                specificDateItem.setSelected(true);
            }
        };

        this.setLayout(new CompactGridLayout(3,2,15,15));
        specificDateItem = new JRadioButton(specificDateAction);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultDateItem);
        buttonGroup.add(relativeDateItem);
        buttonGroup.add(specificDateItem);

        JPanel defaultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        //defaultPanel.add(defaultDateItem);
        defaultPanel.add(defaultDatesComboBox);

        JPanel relativePanel = new JPanel(new GridLayout(1,3, 5, 0));
        relativePanel.add(relativeDateItem);
        relativePanel.add(relativeDateOffset);
        relativePanel.add(relativeDateOffsetTypes);
        relativePanel.add(relativeDatePrepositionsComboBox);
        relativePanel.add(relativeDateLabel);

        JPanel specificPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        //specificPanel.add(specificDateItem);
        specificPanel.add(dateSpinner);


        add(defaultDateItem);
        add(defaultDatesComboBox);

        add(relativeDateItem);
        add(relativePanel);

        add(specificDateItem);
        add(dateSpinner);

//        add(defaultPanel);
//        add(createVerticalStrut(5));
//        add(relativePanel);
//        add(createVerticalStrut(5));
//        add(specificPanel);

        setValue((DateFunction) null);
    }

    public DateFunction getValue() {
        if (defaultDatesComboBox.isEnabled()) {
            return (DateFunction) defaultDatesComboBox.getSelectedItem();
        } else if (relativeDatePrepositionsComboBox.isEnabled()) {
            return new RelativeDateFunction(referenceDate, (RelativeDateFunction.OffsetType) relativeDateOffsetTypes.getSelectedItem(), (RelativeDateFunction.Preposition) relativeDatePrepositionsComboBox.getSelectedItem(), relativeDateOffset.getValue().intValue());
        } else if (dateSpinner.isEnabled()) {
            return new FixedDateFunction((java.util.Date) dateSpinner.getValue(), DateUtilities.newGmtDateFormat("yyyy-MM-dd"));
        } else {
            return null;
        }
    }

    public void setValue(Object value) {
        if (value instanceof DateFunction) {
            DateFunction dateFunction = (DateFunction) value;
            setValue(dateFunction);
        } else if (value instanceof java.sql.Date) {
            java.sql.Date date = (java.sql.Date) value;
            setValue(date);
        } else if (value instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) value;
            setValue(date);
        } else {
            setValue((DateFunction) null);
        }
    }

    public void setValue(java.util.Date value) {
        setValue(new FixedDateFunction(value, DateUtilities.newGmtDateFormat("yyyy-MM-dd")));
    }

    public void setValue(java.sql.Date value) {
        setValue(new FixedDateFunction(value, DateUtilities.newGmtDateFormat("yyyy-MM-dd")));
    }

    public void setValue(DateFunction value) {
        if (defaultDateFunctions.contains(value)) {
            defaultDatesComboBox.setSelectedItem(value);
            defaultDateAction.actionPerformed(null);
        } else if (value instanceof RelativeDateFunction) {
            RelativeDateFunction relativeDateFunction = (RelativeDateFunction) value;
            relativeDateOffset.setValue(new Integer(relativeDateFunction.getOffset()));
            relativeDateOffsetTypes.setSelectedItem(relativeDateFunction.getCalendarOffsetType());
            relativeDatePrepositionsComboBox.setSelectedItem(relativeDateFunction.getPreposition());
            relativeDateAction.actionPerformed(null);
        } else if (value instanceof FixedDateFunction) {
            FixedDateFunction fixedDateFunction = (FixedDateFunction) value;
            dateSpinner.setValue(fixedDateFunction.getDate());
            specificDateAction.actionPerformed(null);
        } else {
            dateSpinner.setValue(DateUtilities.newDate());
            relativeDateOffset.setValue(new Integer(1));
            relativeDateOffsetTypes.setSelectedItem(RelativeDateFunction.OFFSET_TYPE_DAYS);
            relativeDatePrepositionsComboBox.setSelectedItem(RelativeDateFunction.PREPOSITION_BEFORE);
            defaultDatesComboBox.setSelectedItem(SymbolicDateFunction.TODAY_DATEFUNCTION);
            defaultDateAction.actionPerformed(null);
        }
    }

    public static List getDefaultDateFunctions(RelativeDateFunction.Preposition preposition) {
        IndexedSet dateFunctions = new IndexedSet(Arrays.asList(SymbolicDateFunction.getInstances()));

        if (preposition != null) {
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 1));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 2));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 3));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 4));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 5));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 6));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_DAYS, preposition, 7));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_WEEKS, preposition, 1));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_WEEKS, preposition, 2));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_WEEKS, preposition, 3));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_MONTHS, preposition, 1));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_MONTHS, preposition, 3));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_MONTHS, preposition, 6));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_MONTHS, preposition, 9));
            dateFunctions.add(new RelativeDateFunction(SymbolicDateFunction.TODAY_DATEFUNCTION, RelativeDateFunction.OFFSET_TYPE_YEARS, preposition, 1));

            java.sql.Date now = DateUtilities.newDate();

            for (Iterator iterator = dateFunctions.iterator(); iterator.hasNext();) {
                DateFunction dateFunction = (DateFunction) iterator.next();
                if ((preposition.equals(RelativeDateFunction.PREPOSITION_BEFORE) && dateFunction.getDate().after(now)) ||
                        (preposition.equals(RelativeDateFunction.PREPOSITION_AFTER) && dateFunction.getDate().before(now))) {
                    iterator.remove();
                }
            }

            Utilities.sort(dateFunctions);
            if (preposition.equals(RelativeDateFunction.PREPOSITION_BEFORE)) Utilities.reverse(dateFunctions);
        }
        return dateFunctions;
    }
}
