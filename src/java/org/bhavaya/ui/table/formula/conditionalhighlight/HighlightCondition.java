package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.formula.SymbolMappings;
import org.bhavaya.ui.table.formula.*;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;

import java.awt.*;
import java.util.List;

/**
 * Encapsulation of a formula (with a boolean result) and an associated highlight colour.
 * User: ga2mhana
 * Date: 10/03/11
 * Time: 15:42
 */
public class HighlightCondition {
    private static final Log log = Log.getCategory(HighlightCondition.class);
    public static final String CURRENT_COL = "value";

    private boolean enabled;
    private Color color;
    private Formula conditionFormula;

    public HighlightCondition() {
        this(true, Color.white);
    }

    public HighlightCondition(Color color) {
        this(true, color);
    }

    public HighlightCondition(Color color, String expression) throws FormulaException {
        this(true, color, expression);
    }

    public HighlightCondition(boolean enabled, Color color) {
        FormulaFactory factory = FormulaUtils.getFactoryInstance();
        conditionFormula = factory.createFormula();
        this.enabled = enabled;
        this.color = color;
    }

    public HighlightCondition(boolean enabled, Color color, String expression) throws FormulaException {
        this(enabled, color);
        if(expression != null) {
            conditionFormula.setExpression(expression);
        }
    }

    public boolean isSatisfied(Object o, String columnIdentifier, SymbolMappings symbolMappings) {
        if(!conditionFormula.hasExpression()) {
            return true;
        }
        List<String> symbols = conditionFormula.getSymbols();
        for(String symbol : symbols) {
            String beanPath = CURRENT_COL.equals(symbol) ? columnIdentifier : symbolMappings.getBeanPathForSymbol(symbol);
            Object symbolValue = Generic.get(o, Generic.beanPathStringToArray(beanPath));
            conditionFormula.setSymbolValue(symbol, symbolValue);
                }
        FormulaResult result = null;
        try {
            result = conditionFormula.evaluate();
        } catch (FormulaException e) {
            log.debug("Exception during condition evaluation", e);
        }
        return result != null && result.isBoolean() && result.booleanValue();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Formula getConditionFormula() {
        return conditionFormula;
    }

    public void setConditionFormula(Formula conditionFormula) {
        this.conditionFormula = conditionFormula;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public HighlightCondition copy() throws FormulaException {
        return new HighlightCondition(isEnabled(), getColor(), getConditionFormula().getExpression());
    }
}
