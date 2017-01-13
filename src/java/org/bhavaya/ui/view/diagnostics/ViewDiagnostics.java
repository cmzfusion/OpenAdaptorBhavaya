package org.bhavaya.ui.view.diagnostics;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.diagnostics.DiagnosticUtilities;
import org.bhavaya.ui.table.formula.Formula;
import org.bhavaya.ui.table.formula.FormulaManager;
import org.bhavaya.ui.table.formula.conditionalhighlight.HighlightConditionSet;
import org.bhavaya.ui.view.TableView;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.Utilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ViewDiagnosticsMBean.
 * Returns a table with details of all open views.
 * User: ga2mhana
 * Date: 28/02/11
 * Time: 10:00
 */
public class ViewDiagnostics implements ViewDiagnosticsMBean {

    /**
     * {@inheritDoc}
     */
    public String getViewStats(String worthlessParameter) {
        Collection<View> views = Workspace.getInstance().getViews();
        StringBuffer result = DiagnosticUtilities.contextHeader(new StringBuffer(), "Views");
        DiagnosticUtilities.tableHeader(result);
        DiagnosticUtilities.tableHeaderRow(result, new Object[] {"View Name", "Bean Type", "Description", "Last Activated", "Configuration", "Row Count", "No. Active Formulas", "Formula Names", "No. Active Conditional Highlighters", "Highlighter Columns"});
        int formulaCount = 0;
        int highlightCount = 0;
        int rowCount = 0;
        for(View view : views) {
            Object[] basicDetails = getBasicDetails(view);
            Object[] formulaDetails = getFormulaDetails(view);
            Object[] highlightDetails = getHighlightDetails(view);

            formulaCount += (Integer)formulaDetails[0];
            highlightCount += (Integer)highlightDetails[0];
            rowCount += (Integer)basicDetails[5];

            DiagnosticUtilities.tableRow(result, appendArrays(basicDetails, formulaDetails, highlightDetails));
        }
        DiagnosticUtilities.tableRow(result, new Object[] {"Total", views.size(), " ", " ", " ", rowCount, formulaCount, " ",  highlightCount, " "});
        DiagnosticUtilities.tableFooter(result);

        return result.toString();
    }

    private Object[] appendArrays(Object[]... arrays) {
        int size = 0;
        for(Object[] array : arrays) {
            size += array.length;
        }
        Object[] result = new Object[size];
        int start = 0;
        for(Object[] array : arrays) {
            System.arraycopy(array, 0, result, start, array.length);
            start += array.length;
        }
        return result;
    }

    private Object[] getBasicDetails(View view) {
        String config = " ";
        String type = " ";
        String description = " ";
        if(view instanceof TableView) {
            TableView tableView = (TableView)view;
            config = tableView.getViewConfigurationId();
            type = tableView.getRecordType().getSimpleName();
            description = tableView.getDescription();
        }
        BeanCollection beanCollection = view.getBeanCollection();
        return new Object[] {view.getFrameTitle(), type, description, view.getLastActivated(), config, beanCollection != null ? beanCollection.size() : 0};

    }

    private Object[] getFormulaDetails(View view) {
        int formulaCount = 0;
        String formulaString = " ";
        if(view instanceof TableView) {
            TableView tableView = (TableView)view;
            FormulaManager formulaManager = tableView.getBeanCollectionTableModel().getFormulaManager();
            List<Formula> formulas = formulaManager.getAllFormulas();

            StringBuilder sb = new StringBuilder("<ul>");
            for(Formula formula : formulas) {
                if(formula.isEnabled()) {
                    formulaCount++;
                    sb.append("<li>").append(formula.getName()).append("</li>");
                }
            }
            sb.append("</ul>");
            formulaString = sb.toString();
        }

        return new Object[] {formulaCount, formulaString};
    }

    private Object[] getHighlightDetails(View view) {
        int highlightCount = 0;
        String highlightString = " ";
        if(view instanceof TableView) {
            TableView tableView = (TableView)view;
            if(tableView.getConditionalHighlighter() != null) {
                StringBuilder highlightColumns = new StringBuilder("<ul>");
                Map<String, HighlightConditionSet> conditionMap = tableView.getConditionalHighlighter().getHighlightConditionSetMap();
                for(String column : conditionMap.keySet()) {
                    HighlightConditionSet conditionSet = conditionMap.get(column);
                    if(conditionSet.hasEnabledHighlightConditions()) {
                        highlightCount++;
                        highlightColumns.append("<li>").append(Utilities.getDisplayNameForPropertyPath(column)).append("</li>");
                    }
                }
                highlightColumns.append("</ul>");
                highlightString = highlightColumns.toString();
            }
        }

        return new Object[] {highlightCount, highlightString};
    }
}
