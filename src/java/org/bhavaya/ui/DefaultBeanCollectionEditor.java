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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;

/**
 * This class has now been split with most of the functionality in DefaultBeanCollectionEditorPanel. Ideally you should use that
 * class and embed the panel in your own Dialog
 * *
 * @author Jonathan Moore
 * @version $Revision: 1.12 $
 */
public class DefaultBeanCollectionEditor extends JDialog {
    private OkAction okAction;
    private CancelAction cancelAction;
    private Runnable okRunnable;
    private boolean isCanceled = false;

    private DefaultBeanCollectionEditorPanel editorPanel;

    public static DefaultBeanCollectionEditor createBeanCollectionEditor(Component owner,
                                                                         String title,
                                                                         boolean modal,
                                                                         BeanCollection beanCollection,
                                                                         Runnable okRunnable,
                                                                         String[] searchProperties,
                                                                         String[] searchPropertyDisplayNames,
                                                                         String[] tableProperties,
                                                                         String[] tablePropertyDisplayNames,
                                                                         Class[] tablePropertyTypes,
                                                                         int width,
                                                                         double[] columnRatios,
                                                                         int[] sortingColumns,
                                                                         boolean singleRowSelection,
                                                                         String dataSourceName,
                                                                         String sql,
                                                                         String descriptionRenderText,
                                                                         Transform beanToRecordTransformer,
                                                                         Filter[] additionalFilters) {
        return createBeanCollectionEditor(owner, title, modal, beanCollection, okRunnable, searchProperties, searchPropertyDisplayNames,
                tableProperties, tablePropertyDisplayNames, tablePropertyTypes, width, columnRatios, sortingColumns, singleRowSelection,
                dataSourceName, sql, descriptionRenderText, beanToRecordTransformer, additionalFilters, false);
    }

    public static DefaultBeanCollectionEditor createBeanCollectionEditor(Component owner,
                                                                         String title,
                                                                         boolean modal,
                                                                         BeanCollection beanCollection,
                                                                         Runnable okRunnable,
                                                                         String[] searchProperties,
                                                                         String[] searchPropertyDisplayNames,
                                                                         String[] tableProperties,
                                                                         String[] tablePropertyDisplayNames,
                                                                         Class[] tablePropertyTypes,
                                                                         int width,
                                                                         double[] columnRatios,
                                                                         int[] sortingColumns,
                                                                         boolean singleRowSelection,
                                                                         String dataSourceName,
                                                                         String sql,
                                                                         String descriptionRenderText,
                                                                         Transform beanToRecordTransformer,
                                                                         Filter[] additionalFilters,
                                                                         boolean orderable) {
        Window window = UIUtilities.getWindowParent(owner);
        if (window instanceof Dialog) {
            return new DefaultBeanCollectionEditor((Dialog) window, title, modal, beanCollection, okRunnable, searchProperties,
                    searchPropertyDisplayNames, tableProperties, tablePropertyDisplayNames, tablePropertyTypes, width,
                    columnRatios, sortingColumns, singleRowSelection, dataSourceName, sql, descriptionRenderText,
                    beanToRecordTransformer, additionalFilters, orderable);
        } else {
            return new DefaultBeanCollectionEditor((Frame) window, title, modal, beanCollection, okRunnable, searchProperties,
                    searchPropertyDisplayNames, tableProperties, tablePropertyDisplayNames, tablePropertyTypes, width,
                    columnRatios, sortingColumns, singleRowSelection, dataSourceName, sql, descriptionRenderText,
                    beanToRecordTransformer, additionalFilters, orderable);
        }
    }


    private DefaultBeanCollectionEditor(Frame owner,
                                       String title,
                                       boolean modal,
                                       BeanCollection beanCollection,
                                       Runnable okRunnable,
                                       String[] searchProperties,
                                       String[] searchPropertyDisplayNames,
                                       String[] tableProperties,
                                       String[] tablePropertyDisplayNames,
                                       Class[] tablePropertyTypes,
                                       int width,
                                       double[] columnRatios,
                                       int[] sortingColumns,
                                       boolean singleRowSelection,
                                       String dataSourceName,
                                       String sql,
                                       String descriptionRenderText,
                                       Transform beanToRecordTransformer,
                                       Filter[] additionalFilters,
                                       boolean orderable) {
        super(owner, title);
        init(modal, beanCollection, okRunnable, searchPropertyDisplayNames, tableProperties, tablePropertyTypes, sortingColumns, tablePropertyDisplayNames, columnRatios, dataSourceName, sql, singleRowSelection, descriptionRenderText, width, searchProperties, beanToRecordTransformer, additionalFilters, orderable);
    }

    private DefaultBeanCollectionEditor(Dialog owner,
                                       String title,
                                       boolean modal,
                                       BeanCollection beanCollection,
                                       Runnable okRunnable,
                                       String[] searchProperties,
                                       String[] searchPropertyDisplayNames,
                                       String[] tableProperties,
                                       String[] tablePropertyDisplayNames,
                                       Class[] tablePropertyTypes,
                                       int width,
                                       double[] columnRatios,
                                       int[] sortingColumns,
                                       boolean singleRowSelection,
                                       String dataSourceName,
                                       String sql,
                                       String descriptionRenderText,
                                       Transform beanToRecordTransformer,
                                       Filter[] additionalFilters,
                                       boolean orderable) {
        super(owner, title);
        init(modal, beanCollection, okRunnable, searchPropertyDisplayNames, tableProperties, tablePropertyTypes, sortingColumns, tablePropertyDisplayNames, columnRatios, dataSourceName, sql, singleRowSelection, descriptionRenderText, width, searchProperties, beanToRecordTransformer, additionalFilters, orderable);
    }

    private void init(boolean modal,
                      final BeanCollection beanCollection,
                      Runnable okRunnable,
                      String[] searchPropertyDisplayNames,
                      String[] tableProperties,
                      Class[] tablePropertyTypes,
                      int[] sortingColumns,
                      String[] tablePropertyDisplayNames,
                      double[] columnRatios,
                      String dataSourceName,
                      String sql,
                      boolean singleRowSelection,
                      String descriptionRenderText,
                      int width,
                      String[] searchProperties,
                      final Transform beanToRecordTransformer,
                      Filter[] additionalFilters,
                      boolean orderable) {
        setModal(modal);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        editorPanel = new DefaultBeanCollectionEditorPanel(beanCollection,
                                       searchProperties,
                                       searchPropertyDisplayNames,
                                       tableProperties,
                                       tablePropertyDisplayNames,
                                       tablePropertyTypes,
                                       width,
                                       columnRatios,
                                       sortingColumns,
                                       singleRowSelection,
                                       dataSourceName,
                                       sql,
                                       descriptionRenderText,
                                       beanToRecordTransformer,
                                       additionalFilters,
                                       orderable);
        this.okRunnable = okRunnable;

        okAction = new OkAction();
        cancelAction = new CancelAction();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(editorPanel);
        panel.add(createButtonsPanel(), BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(panel);
        pack();
        UIUtilities.centreInContainer(getOwner(), this, 0, 0);
    }

    private Container createButtonsPanel() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        container.add(new JExtendedButton(okAction));
        container.add(new JExtendedButton(cancelAction));
        return container;
    }

    public void setDefaultRenderer(Class aClass, TableCellRenderer tableCellRenderer) {
        editorPanel.setDefaultRenderer(aClass, tableCellRenderer);
    }

    private class OkAction extends AuditedAbstractAction {
        public OkAction() {
            putValue(Action.NAME, "Ok");
        }

        public void auditedActionPerformed(ActionEvent e) {
            editorPanel.updateCollection();
            isCanceled = false;
            dispose();
            if (okRunnable != null) okRunnable.run();
        }
    }

    private class CancelAction extends AuditedAbstractAction {
        public CancelAction() {
            putValue(Action.NAME, "Cancel");
        }

        public void auditedActionPerformed(ActionEvent e) {
            editorPanel.clearSelection();
            isCanceled = true;
            dispose();
        }
    }

    public boolean isCanceled() {
        return isCanceled;
    }
}
