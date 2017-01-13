package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.formula.FormulaException;
import org.bhavaya.ui.table.formula.SymbolMappings;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An ordered set of highlight conditions, which basically forms an if...then...else set of highlight conditions
 * User: ga2mhana
 * Date: 11/03/11
 * Time: 11:13
 */
public class HighlightConditionSet {
    private List<HighlightCondition> highlightConditions = new ArrayList<HighlightCondition>();
    private SymbolMappings symbolMappings = new SymbolMappings();

    static {
        BeanUtilities.addPersistenceDelegate(HighlightConditionSet.class, new BhavayaPersistenceDelegate(new String[]{"symbolToBeanPathMap", "highlightConditions"}));
    }

    public HighlightConditionSet() {
    }

    public HighlightConditionSet(Map<String, String> symbolToBeanPathMap, List<HighlightCondition> highlightConditions) {
        this.symbolMappings = new SymbolMappings(symbolToBeanPathMap);
        this.highlightConditions = highlightConditions;
    }

    public HighlightConditionSet(HighlightConditionSet highlightConditionSet) throws FormulaException {
        initialiseFrom(highlightConditionSet);
    }

    private void initialiseFrom(HighlightConditionSet other) throws FormulaException {
        clearAll();
        if(other == null) {
            other = new HighlightConditionSet();
        }
        symbolMappings.initialiseFrom(other.symbolMappings);
        for(HighlightCondition highlightCondition : other.highlightConditions) {
            addHighlightCondition(highlightCondition.copy());
        }
    }

    private void clearAll() {
        symbolMappings.clear();
        highlightConditions.clear();
    }

    public Color getColor(Object o, String columnIdentifier) {
        for(HighlightCondition condition : highlightConditions) {
            if(condition.isEnabled() && condition.isSatisfied(o, columnIdentifier, symbolMappings)) {
                return condition.getColor();
            }
        }
        return null;
    }

    public HighlightCondition addHighlightCondition() {
        return addHighlightCondition(new HighlightCondition());
    }

    public HighlightCondition addHighlightCondition(Color color, String expression) throws FormulaException {
        return addHighlightCondition(new HighlightCondition(color, expression));
    }

    public HighlightCondition addHighlightCondition(Color color) throws FormulaException {
        return addHighlightCondition(new HighlightCondition(color));
    }

    private HighlightCondition addHighlightCondition(HighlightCondition highlightCondition) {
        highlightConditions.add(highlightCondition);
        return highlightCondition;
    }

    public List<HighlightCondition> getHighlightConditions() {
        return highlightConditions;
    }

    public SymbolMappings getSymbolMappings() {
        return symbolMappings;
    }

    /* only used by the persistence delegate */
    public Map<String, String> getSymbolToBeanPathMap() {
        return symbolMappings.getSymbolToBeanPathMap();
    }

    public boolean hasEnabledHighlightConditions() {
        for(HighlightCondition highlightCondition : highlightConditions) {
            if(highlightCondition.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
