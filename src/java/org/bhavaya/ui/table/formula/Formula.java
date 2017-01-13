package org.bhavaya.ui.table.formula;

import java.util.List;

/**
 * Definition of a Formula. AbstractFormula provides most of the methods by default.
 * User: Jon Moore
 * Date: 17/01/11
 * Time: 15:13
 */
public interface Formula {
    String getName();

    void setName(String name);

    String getExpression();

    void setExpression(String expression) throws FormulaException;

    boolean hasExpression();

    void parseExpression() throws FormulaException;

    void setSymbolValue(String symbol, Object value);

    void setEmptySymbolValue(String symbol, Class<?> type);

    List<String> getSymbols();

    String getSymbol();

    void setSymbol(String symbol);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean dependsOnSymbol(String symbol);

    FormulaResult evaluate() throws FormulaException;

    Formula copy() throws FormulaException;
}
