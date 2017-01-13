package org.bhavaya.util;

/**
 * This class is used to validating input data on cell editors, before it reaches the SetStatement.
 * Any violation of the contraint defined by this, will prompt a confirmation dialog to go ahead.
 * 
 * @author <a href="mailto:Sabine.Haas@dkib.com">Sabine Haas, Dresdner Kleinwort</a>
 */
public interface SetConstraint {
    /**
     * Validate whether this constraint is violated or not
     * @param oldValue currentValue of the underlying bean
     * @param newValue newValue for the underlying bean
     * @return whether constraint is violated
     */
    public boolean constraintViolated(Object oldValue, Object newValue);

    /**
     * Provide the error message to the user, if the constraint was violated
     * @param oldValue currentValue of the underlying bean
     * @param newValue newValue for the underlying bean
     * @return error message for rule violation
     */
    public String getErrorMessage(Object oldValue, Object newValue);
}
