package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.HighlightedTable;
import org.bhavaya.ui.table.formula.FormulaUtils;

import java.awt.*;
import java.util.*;

/**
 * Cell highlighter for conditional highlighting. Contains a map of column identifiers to ConditionSets
 * User: ga2mhana
 * Date: 10/03/11
 * Time: 13:45
 */
public class ConditionalHighlighter extends HighlightedTable.CellHighlighter {
    private final Map<String, HighlightConditionSet> highlightConditionSetMap;
    private final AnalyticsTableModel analyticsTableModel;
    private ConditionalHighlightColumnExclusions exclusions;

    public ConditionalHighlighter(AnalyticsTableModel analyticsTableModel, Map<String, HighlightConditionSet> highlightConditionSetMap) {
        this.analyticsTableModel = analyticsTableModel;
        this.highlightConditionSetMap = highlightConditionSetMap;
    }

    public void setColumnCondition(String columnIdentifier, HighlightConditionSet highlightConditionSet) {
        if(highlightConditionSet == null) {
            highlightConditionSetMap.remove(columnIdentifier);
        } else if (isConditionalHighlightingPermitted(columnIdentifier)) {
            highlightConditionSetMap.put(columnIdentifier, highlightConditionSet);
        }
    }

    public HighlightConditionSet getColumnCondition(String columnIdentifier) {
        return highlightConditionSetMap.get(columnIdentifier);
    }

    @Override
    protected Color getHighlightForCell(int row, int viewCol, HighlightedTable table) {
        Color superColor = super.getHighlightForCell(row, viewCol, table);
        Color color = null;
        if(conditionalHighlightingEnabled() && !highlightConditionSetMap.isEmpty()) {
            int modelIndex = table.getColumnModel().getColumn(viewCol).getModelIndex();
            String columnIdentifier = analyticsTableModel.getColumnKey(modelIndex).toString();
            if(isConditionalHighlightingPermitted(columnIdentifier)) {
                HighlightConditionSet highlightConditionSet = highlightConditionSetMap.get(columnIdentifier);
                if(highlightConditionSet != null) {
                    Object[] beans = analyticsTableModel.getBeansForLocation(row, modelIndex);
                    if (beans.length == 1) {
                        color = highlightConditionSet.getColor(beans[0], columnIdentifier);
                    }
                }
            }
        }

        return color == null ? superColor : UIUtilities.multiply(color, superColor);
    }

    public Map<String, HighlightConditionSet> getHighlightConditionSetMap() {
        return highlightConditionSetMap;
    }

    public boolean isConditionalHighlightingPermitted(String columnIdentifier) {
        return conditionalHighlightingEnabled() &&
                (exclusions == null || !exclusions.isExcluded(columnIdentifier));
    }

    public void setExclusions(ConditionalHighlightColumnExclusions exclusions) {
        this.exclusions = exclusions;
    }

    public static boolean conditionalHighlightingEnabled() {
        return FormulaUtils.formulasEnabled();
    }

    public boolean hasEnabledHighlightConditionsForColumn(String columnIdentifier) {
        if(isConditionalHighlightingPermitted(columnIdentifier) && !highlightConditionSetMap.isEmpty()) {
            HighlightConditionSet highlightConditionSet = highlightConditionSetMap.get(columnIdentifier);
            if(highlightConditionSet != null) {
                return highlightConditionSet.hasEnabledHighlightConditions();
            }
        }
        return false;
    }
}
