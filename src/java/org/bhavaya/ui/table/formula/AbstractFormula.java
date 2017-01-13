package org.bhavaya.ui.table.formula;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract implementation of Formula
 * User: Jon Moore
 * Date: 17/01/11
 * Time: 16:25
 */
public abstract class AbstractFormula implements Formula {
    private final long id; //unique identifier - this is used so we can update name, expression and symbol without affecting it's use as a key in a map
    private String expression;
    private String name;
    private String symbol;
    private boolean enabled = true;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");

    private static int nextId = 0;

    public AbstractFormula() {
        this.id = getNextId();
    }

    private synchronized long getNextId() {
        long limit = 1000;
        if(++nextId == limit) {
            nextId = 0;
        }
        return (Long.parseLong(DATE_FORMAT.format(new Date())) * limit) + nextId;
    }

    public AbstractFormula(long id, String name, String expression, String symbol, boolean enabled) throws FormulaException {
        this.id = id;
        this.name = name;
        this.expression = expression;
        this.symbol = symbol;
        this.enabled = enabled;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) throws FormulaException {
        this.expression = expression;
        parseExpression();
    }

    public boolean hasExpression() {
        return expression != null && expression.trim().length() > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractFormula that = (AbstractFormula) o;
        return that.id == id;
    }

    @Override
    public int hashCode() {
        return (int)id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
