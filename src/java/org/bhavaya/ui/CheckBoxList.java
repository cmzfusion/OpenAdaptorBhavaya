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

import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class CheckBoxList extends JList {
    private static final Log log = Log.getCategory(CheckBoxList.class);

    public CheckBoxList() {
        super();
        setCellRenderer(new CheckBoxListCellRenderer());
    }

    public CheckBoxList(ListModel dataModel) {
        super(dataModel);
        setCellRenderer(new CheckBoxListCellRenderer());
        setSelectionModel(new CheckBoxListSelectionModel());
    }

    public CheckBoxList(Vector listData) {
        super(listData);
        setCellRenderer(new CheckBoxListCellRenderer());
    }

    public CheckBoxList(Object[] listData) {
        super(listData);
        setCellRenderer(new CheckBoxListCellRenderer());
    }

    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
    }

    private static class CheckBoxListCellRenderer extends DefaultListCellRenderer {
        private Icon checkOn = new CheckIcon(true);
        private Icon checkOff = new CheckIcon(false);

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIcon(isSelected ? checkOn : checkOff);
            label.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
            return label;
        }
    }


    /**
     * an icon that draws a check box.
     * the drawing code has been ripped from MetalIconFactory.CheckBoxIcon
     */
    private static class CheckIcon implements Icon {
        boolean selected;
        int controlSize = 13;

        public CheckIcon(boolean selected) {
            this.selected = selected;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            drawFlush3DBorder(g, x, y, controlSize, controlSize);

            if (selected) {
                g.setColor(MetalLookAndFeel.getControlInfo());
                g.fillRect(x + 3, y + 5, 2, controlSize - 8);
                g.drawLine(x + (controlSize - 4), y + 3, x + 5, y + (controlSize - 6));
                g.drawLine(x + (controlSize - 4), y + 4, x + 5, y + (controlSize - 5));
            }

        }

        public int getIconWidth() {
            return controlSize;
        }

        public int getIconHeight() {
            return controlSize;
        }

        /**
         * This draws the "Flush 3D Border" which is used throughout the Metal L&F
         */
        static void drawFlush3DBorder(Graphics g, int x, int y, int w, int h) {
            g.translate(x, y);
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(0, 0, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRect(1, 1, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControl());
            g.drawLine(0, h - 1, 1, h - 2);
            g.drawLine(w - 1, 0, w - 2, 1);
            g.translate(-x, -y);
        }
    }

    private class CheckBoxListSelectionModel implements ListSelectionModel {
        private TreeSet selectedIndices = new TreeSet();
        private TreeSet adjustingSelectedIndices = (TreeSet) selectedIndices.clone();

        private ArrayList listeners = new ArrayList();
        private boolean adjusting = false;
        private int anchor = -1;

        public void setSelectionInterval(int index0, int index1) {
            if (adjusting) {
                if (selectedIndices.contains(new Integer(index0))) {
                    adjustingSelectedIndices.remove(new Integer(index0));
                } else {
                    adjustingSelectedIndices.add(new Integer(index0));
                    anchor = index0;
                }

                fireValueChanged(index0);
            } else {
                selectedIndices.add(new Integer(index0));
            }
        }

        public void addListSelectionListener(ListSelectionListener x) {
            listeners.add(x);
        }

        public void addSelectionInterval(int index0, int index1) {
        }

        public void clearSelection() {
            adjustingSelectedIndices.clear();
            selectedIndices.clear();
        }

        public int getAnchorSelectionIndex() {
            return anchor;
        }

        public int getLeadSelectionIndex() {
            return anchor;
        }

        public int getMaxSelectionIndex() {
            return adjusting
                    ? adjustingSelectedIndices.size() == 0 ? -1 : ((Integer) adjustingSelectedIndices.last()).intValue()
                    : selectedIndices.size() == 0 ? -1 : ((Integer) selectedIndices.last()).intValue();
        }

        public int getMinSelectionIndex() {
            return adjusting
                    ? adjustingSelectedIndices.size() == 0 ? -1 : ((Integer) adjustingSelectedIndices.first()).intValue()
                    : selectedIndices.size() == 0 ? -1 : ((Integer) selectedIndices.first()).intValue();
        }

        public int getSelectionMode() {
            return 0;
        }

        public boolean getValueIsAdjusting() {
            return adjusting;
        }

        public void insertIndexInterval(int index, int length, boolean before) {
        }

        public boolean isSelectedIndex(int index) {
            return adjusting ? adjustingSelectedIndices.contains(new Integer(index)) : selectedIndices.contains(new Integer(index));
        }

        public boolean isSelectionEmpty() {
            return adjusting ? adjustingSelectedIndices.size() == 0 : selectedIndices.contains(new Integer(0));
        }

        public void removeIndexInterval(int index0, int index1) {
        }

        public void removeListSelectionListener(ListSelectionListener x) {
            listeners.remove(x);
        }

        public void removeSelectionInterval(int index0, int index1) {
        }

        public void setAnchorSelectionIndex(int index) {
        }

        public void setLeadSelectionIndex(int index) {
        }

        public void setSelectionMode(int selectionMode) {
        }

        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            if (valueIsAdjusting) {
                adjustingSelectedIndices = (TreeSet) selectedIndices.clone();
            } else {
                selectedIndices = (TreeSet) adjustingSelectedIndices.clone();
            }
            adjusting = valueIsAdjusting;
        }

        private void fireValueChanged(int index) {
            ListSelectionEvent evt = new ListSelectionEvent(this, index, index, false);
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                ListSelectionListener listener = (ListSelectionListener) iterator.next();
                listener.valueChanged(evt);
            }
        }
    }
}
