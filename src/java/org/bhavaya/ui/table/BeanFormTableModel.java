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

package org.bhavaya.ui.table;

import org.bhavaya.util.Generic;
import org.bhavaya.util.Observable;
import org.bhavaya.ui.UIUtilities;

import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class BeanFormTableModel extends AbstractTableModel {
    private Observable bean;
    private String[] beanPaths;
    private String[] beanPathDisplayNames;
    private BeanPropertyChangeListener beanPropertyChangeListener;

    public BeanFormTableModel(Observable bean, String[] beanPaths, String[] beanPathDisplayNames) {
        this.bean = bean;
        this.beanPaths = beanPaths;
        this.beanPathDisplayNames = beanPathDisplayNames;
        beanPropertyChangeListener = new BeanPropertyChangeListener();
        bean.addPropertyChangeListener(beanPropertyChangeListener);

    }

    public int getRowCount() {
        return beanPaths.length;
    }

    public int getColumnCount() {
        return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return beanPathDisplayNames[rowIndex];
        } else if (columnIndex == 1) {
            return Generic.get(bean, Generic.beanPathStringToArray(beanPaths[rowIndex]));
        }
        return null;
    }

    public String getColumnName(int column) {
        if (column == 0) {
            return "Name";
        } else if (column == 1) {
            return "Value";
        }
        return null;
    }

    public void dispose() {
        bean.removePropertyChangeListener(beanPropertyChangeListener);
    }

    private class BeanPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    BeanFormTableModel.this.fireTableDataChanged();
                }
            });
        }
    }
}
