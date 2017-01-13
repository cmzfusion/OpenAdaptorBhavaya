package org.bhavaya.ui.table.formula;

/**
 * Exception relating to Formulas
 * User: Jon Moore
 * Date: 17/01/11
 * Time: 16:01
 */
public class FormulaException extends Exception {
    public FormulaException() {
        super();
    }

    public FormulaException(String message) {
        super(message);
    }

    public FormulaException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaException(Throwable cause) {
        super(cause);
    }
}
