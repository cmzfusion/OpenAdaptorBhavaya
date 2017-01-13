package org.bhavaya.ui.table.formula;

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.*;

/**
 * Class for managing symbols and formula.
 * User: Jon Moore
 * Date: 21/01/11
 * Time: 10:03
 */
public class FormulaManager {
    private SymbolMappings symbolMappings = new SymbolMappings();
    private List<Formula> formulaList = new ArrayList<Formula>();

    static {
        BeanUtilities.addPersistenceDelegate(FormulaManager.class, new BhavayaPersistenceDelegate(new String[]{"symbolToBeanPathMap", "formulaList"}));
    }

    public FormulaManager() {
    }

    public FormulaManager(Map<String, String> symbolToBeanPath, List<Formula> formulaList) {
        this.symbolMappings = new SymbolMappings(symbolToBeanPath);
        this.formulaList = formulaList;
    }

    public FormulaManager(FormulaManager manager) throws FormulaException {
        initialiseFrom(manager);
    }

    public SymbolMappings getSymbolMappings() {
        return symbolMappings;
    }

    public void addFormula(Formula formula) {
        formulaList.add(formula);
    }

    public String getBeanPathForSymbol(String symbol) {
        return symbolMappings.getBeanPathForSymbol(symbol);
    }

    public Set<String> getSymbolForBeanPath(String beanPath) {
        return symbolMappings.getSymbolForBeanPath(beanPath);
    }

    public Formula getFormulaByName(String formulaName) {
        for(Formula formula : formulaList) {
            if(formulaName.equals(formula.getName())) {
                return formula;
            }
        }
        return null;
    }

    /* only used by the persistence delegate */
    public Map<String, String> getSymbolToBeanPathMap() {
        return symbolMappings.getSymbolToBeanPathMap();
    }

    /* only used by the persistence delegate */
    public List<Formula> getFormulaList() {
        return new ArrayList<Formula>(formulaList);
    }

    public List<Formula> getAllFormulas() {
        return Collections.unmodifiableList(formulaList);
    }

    public void removeFormulaAt(int index) {
        formulaList.remove(index);
    }

    public void initialiseFrom(FormulaManager other) throws FormulaException {
        clearAll();
        symbolMappings.initialiseFrom(other.symbolMappings);
        for(Formula formula : other.formulaList) {
            addFormula(formula.copy());
        }
    }

        // Check the manager is in a valid state
    public String[] validate() {
        List<String> errors = new ArrayList<String>();
        //check all symbols are valid
        errors.addAll(Arrays.asList(symbolMappings.validate()));
        List<String> allFormulaSymbols = new LinkedList<String>();
        for(Formula formula : formulaList) {
            allFormulaSymbols.add(formula.getSymbol());
        }
        for(Formula formula : formulaList) {
            String name = formula.getName();
            if(name == null || name.trim().length() == 0) {
                errors.add("A formula has been added with no name");
            }
            List<String> symbols = formula.getSymbols();
            try {
                formula.parseExpression();
            } catch (FormulaException e) {
                errors.add("Formula \""+formula.getExpression()+"\" is invalid");
            }
            if(symbols == null || symbols.isEmpty()) {
                errors.add("Formula \""+formula.getName()+"\" has no expression");
            } else  {
                for(String symbol : formula.getSymbols()) {
                    if(allFormulaSymbols.contains(symbol)) {
                        errors.add("Formula \""+formula.getName()+"\" depends on another formula with symbol \""+symbol+"\"");
                    }
                    if(!symbolMappings.containsSymbol(symbol)) {
                        errors.add("Formula \""+formula.getName()+"\" depends on an invalid symbol \""+symbol+"\"");
                    }
                }
            }
        }

        return errors.isEmpty() ? null : errors.toArray(new String[errors.size()]);
    }
    public void clearAll() {
        symbolMappings.clear();
        formulaList.clear();
    }

    public boolean hasFormulas() {
        return formulaList.size() > 0;
    }
}
