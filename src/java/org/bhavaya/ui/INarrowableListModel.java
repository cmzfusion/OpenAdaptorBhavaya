package org.bhavaya.ui;

import org.bhavaya.util.StringRenderer;

import javax.swing.*;
import java.util.Collection;

/**
 * ListModel that accompanies the NarrowableComboBox
 * User: ga2mop0
 * Date: 07/10/13
 * Time: 10:53
 */
public interface INarrowableListModel extends ListModel {
    void setRenderer(StringRenderer objectRenderer);
    void narrow(String narrowText);
    void narrow(String narrowText, boolean scheduleLoad);
    Object getFirstMatchingObject(String str);
    void clear();
    void addData(Collection data);
    boolean isLoadingData();
}
