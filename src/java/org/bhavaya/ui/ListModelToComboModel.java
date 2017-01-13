package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.event.ListDataListener;

/**
 * Description
 *
 * @author Brendon McLean
 */
public class ListModelToComboModel implements ComboBoxModel {
    private ListModel listModel;
    private Object selectedItem;

    public ListModelToComboModel(ListModel listModel) {
        this.listModel = listModel;
    }

    public int getSize() {
        return listModel.getSize();
    }

    public Object getElementAt(int index) {
        return listModel.getElementAt(index);
    }

    public void addListDataListener(ListDataListener l) {
        listModel.addListDataListener(l);
    }

    public void removeListDataListener(ListDataListener l) {
        listModel.removeListDataListener(l);
    }

    public void setSelectedItem(Object anItem) {
        this.selectedItem = anItem;
    }

    public Object getSelectedItem() {
        return selectedItem;
    }
}
