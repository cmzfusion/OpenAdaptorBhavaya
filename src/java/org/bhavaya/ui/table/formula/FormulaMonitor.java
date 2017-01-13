package org.bhavaya.ui.table.formula;

/**
 * Implement this interface to provide monitoring on number of formulas recalculated and the time it took.
 * Implementations of this interface should be defined in application properties formula.formulaMonitorClass,
 * and instantiated by FormulaUtils
 * User: ga2mhana
 * Date: 24/02/11
 * Time: 13:36
 */
public interface FormulaMonitor {
    void monitorFormulas(long formulaRecalcCount, long updateTime);
}
