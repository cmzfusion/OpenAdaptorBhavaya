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

package org.bhavaya.ui.view;

import org.bhavaya.beans.Schema;
import org.bhavaya.ui.*;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.util.PropertyModel;
import org.bhavaya.util.SetStatement;
import org.bhavaya.util.Transform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.14 $
 */
public class DefaultViewContext implements ViewContext {
    private static final SetStatement[] EMPTY_SET_STATEMENTS = new SetStatement[0];

    protected View view;
    private ImageIcon imageIcon;
    private KeyForBeanTransform beanToKeyTransform;
    private java.util.List<MenuGroup> menuGroupsToAdd;
    private java.util.List<ToolBarGroup.Element> toolbarElementsToAdd;

    public DefaultViewContext(View view) {
        this(view, null);
    }

    public DefaultViewContext(View view, ImageIcon imageIcon) {
        this.view = view;
        this.imageIcon = imageIcon;
        beanToKeyTransform = new KeyForBeanTransform();
        menuGroupsToAdd = new ArrayList<MenuGroup>();
        toolbarElementsToAdd = new ArrayList<ToolBarGroup.Element>();
    }

    public void dispose() {
        menuGroupsToAdd.clear();
        toolbarElementsToAdd.clear();
    }

    protected void addToMenuGroups(MenuGroup menuGroup) {
        menuGroupsToAdd.add(menuGroup);
    }

    public MenuGroup[] createMenuGroups() {
       return menuGroupsToAdd.toArray(new MenuGroup[menuGroupsToAdd.size()]);
    }

    protected void addToToolBarGroup(ToolBarGroup.Element element) {
        toolbarElementsToAdd.add(element);
    }

    public ToolBarGroup createToolBarGroup() {
        ToolBarGroup toolBarGroup = new ToolBarGroup("DefaultViewContext");
        for (ToolBarGroup.Element aToolbarElementsToAdd : toolbarElementsToAdd) {
            toolBarGroup.addElement(aToolbarElementsToAdd);
        }
        return toolBarGroup;
    }

    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    public Action getActionForBeanPath(Object bean, String beanPath) {
        return null;
    }

    public AcceleratorAction[] getAcceleratorActions() {
        return null;
    }

    public View getView() {
        return view;
    }

    public ActionGroup getActionsForBeanArray(Object[] beanArray) {
        ActionGroup actions = new ActionGroup("");
        if (view instanceof TableView) {
            TableView tableView = (TableView) view;

            if (beanArray.length > 0 && tableView.getAnalyticsTableModel().isGrouped()) {
                actions.addAction(new TableView.DrillDownAction(beanArray, tableView));
            }

            ActionGroup viewActions = new ActionGroup("View Related");

            ViewAssociatedBeanCollectionAction.addActions(beanArray, viewActions, tableView.getRecordType());
            ViewAssociatedBeansAction.addActions(beanArray, viewActions, getViewAssociatedBeansActionFilter(), beanToKeyTransform);

            if (viewActions.size() > 0) {
                actions.addActionGroup(viewActions);
            }

            if (PropertyModel.getInstance(tableView.getRecordType()).isKeyable()){
                addCustomisationOptionActions(beanArray, actions);
            }
        }
        return actions;
    }

    protected ViewAssociatedBeansAction.Filter getViewAssociatedBeansActionFilter() {
        return null;
    }

    public SetStatement[] getSetStatements() {
        return EMPTY_SET_STATEMENTS;
    }

    protected void addCustomisationOptionActions(final Object[] beanArray, ActionGroup actions) {
        int highlightedCount = 0;
        for (Object bean : beanArray) {
            if (Schema.hasInstance(bean.getClass())) {
                Object keyForBean = getKeyForBean(bean);
                if (((TableView) view).getAnalyticsTable().isBeanHighlighted(keyForBean)) highlightedCount++;
            }
        }

        AnalyticsTable analyticTable = ((TableView) view).getAnalyticsTable();
        LinkedList usedColours = analyticTable.getUsedColours();

        ActionGroup highlightActions = new ActionGroup("Highlight Options");
        Iterator it = usedColours.iterator();
        while (it.hasNext()) {
            Color aColor = (Color) it.next();
            highlightActions.addAction(new SetColourAction(aColor, beanArray));
        }
        highlightActions.appendSeperator();
        highlightActions.addAction(new EditHighlightRowColour(beanArray));
        actions.addActionGroup(highlightActions);

        if (highlightedCount > 0) {
            actions.addAction(new RemoveHighlight(highlightedCount, beanArray));
        }

        Object[] selectedBeans = analyticTable.getSelectedBeans();
        if(selectedBeans.length > 0){
            ActionGroup cellFormat = new ActionGroup("Cell Format");
            Object beanPath = analyticTable.getSelectedColumn().getIdentifier();
            cellFormat.addAction(new EditCellColourAction(selectedBeans, beanPath));
            cellFormat.addAction(new EditCellFontAction(selectedBeans, beanPath));
            cellFormat.addAction(new ClearCellFormatAction(selectedBeans, beanPath));
            actions.addActionGroup(cellFormat);
        }
    }

    protected Object getKeyForBean(Object bean) {
        return PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
    }

    private class EditHighlightRowColour extends AuditedAbstractAction {
        private final Object[] beanArray;

        public EditHighlightRowColour(Object[] beanArray) {
            super("Choose...");
            this.beanArray = beanArray;
        }

        public void auditedActionPerformed(ActionEvent e) {
            if (beanArray.length > 0) {
                AnalyticsTable analyticTable = ((TableView) view).getAnalyticsTable();
                Color currentHightLightColor = analyticTable.getRowHighlightColor(beanArray[0]);
                Color newHightLightColor = JColorChooser.showDialog(analyticTable, "Choose Row Colour", currentHightLightColor);
                if (newHightLightColor != null) {
                    for (Object bean : beanArray) {
                        analyticTable.setRowHighlightColor(bean, newHightLightColor);
                    }
                }
                analyticTable.repaint();
            }
        }
    }

    private class RemoveHighlight extends AuditedAbstractAction {
        private final Object[] beanArray;

        public RemoveHighlight(int highlightCount, Object[] beanArray) {
            super("Remove Highlight" + (highlightCount > 1 ? "s" : ""));
            this.beanArray = beanArray;
        }

        public void auditedActionPerformed(ActionEvent e) {
            for (Object bean : beanArray) {
                ((TableView) view).getAnalyticsTable().removeBeanForRowHighlight(bean);
            }
            ((TableView) view).getAnalyticsTable().repaint();
        }
    }

    private class KeyForBeanTransform implements Transform {
        public Object execute(Object sourceData) {
            return getKeyForBean(sourceData);
        }
    }

    private class SetColourAction extends AuditedAbstractAction {

        private final Color color;
        private final Object[] beanArray;

        public SetColourAction(Color color, Object[] beanArray) {
            putValue(Action.SMALL_ICON, new ColorIcon(color));
            this.color = color;
            this.beanArray = beanArray;
        }

        public void auditedActionPerformed(ActionEvent e) {
            for (Object bean : beanArray) {
                ((TableView) view).getAnalyticsTable().setRowHighlightColor(bean, color);
            }
            ((TableView) view).getAnalyticsTable().repaint();
        }
    }

    private class EditCellColourAction extends AuditedAbstractAction {
        private final Object[] beanArray;
        private final Object beanPath;

        public EditCellColourAction(Object[] beanArray, Object beanPath) {
            super("Edit cell colour...");
            this.beanArray = beanArray;
            this.beanPath = beanPath;
        }

        public void auditedActionPerformed(ActionEvent e) {
            AnalyticsTable analyticTable = ((TableView) view).getAnalyticsTable();
            Object key = PropertyModel.getInstance(beanArray[0].getClass()).getKeyForBean(beanArray[0]);
            Color currentCellColour = analyticTable.getCellHighlightColor(key, beanPath);
            Color newCellColor = JColorChooser.showDialog(analyticTable, "Choose cell Colour", currentCellColour);
            if (newCellColor != null) {
                for (Object bean : beanArray) {
                    key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
                    analyticTable.setCellHighlightColor(key, beanPath, newCellColor);
                }
            }
            analyticTable.repaint();
        }
    }

    private class EditCellFontAction extends AuditedAbstractAction {
        private final Object[] beanArray;
        private final Object beanPath;

        public EditCellFontAction(Object[] beanArray, Object beanPath) {
            super("Edit cell font...");
            this.beanArray = beanArray;
            this.beanPath = beanPath;
        }

        public void auditedActionPerformed(ActionEvent e) {
            AnalyticsTable analyticTable = ((TableView) view).getAnalyticsTable();
            Object key = PropertyModel.getInstance(beanArray[0].getClass()).getKeyForBean(beanArray[0]);
            Font currentCellFont = analyticTable.getCellHighlightFont(key, beanPath);
            Font newCellFont = JFontChooser.showDialog(analyticTable, "Choose cell font", currentCellFont);
            if (newCellFont != null) {
                for (Object bean : beanArray) {
                    key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
                    analyticTable.setCellHighlightFont(key, beanPath, newCellFont);
                }
            }
            analyticTable.repaint();
        }
    }

    private class ClearCellFormatAction extends AuditedAbstractAction {
        private final Object[] beanArray;
        private final Object beanPath;

        public ClearCellFormatAction(Object[] beanArray, Object beanPath) {
            super("Clear cell format");
            this.beanArray = beanArray;
            this.beanPath = beanPath;
        }

        public void auditedActionPerformed(ActionEvent e) {
            AnalyticsTable analyticTable = ((TableView) view).getAnalyticsTable();
            for (Object bean : beanArray) {
                Object key = PropertyModel.getInstance(bean.getClass()).getKeyForBean(bean);
                analyticTable.removeCellFormat(key, beanPath);
            }
            analyticTable.repaint();
        }
    }

    public JToolBar getToolbar() {
        return null;
    }
}
