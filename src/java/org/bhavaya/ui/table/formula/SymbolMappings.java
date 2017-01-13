package org.bhavaya.ui.table.formula;

import java.util.*;

/**
 * Set of mappings between formula symbols and bean paths
 * User: ga2mhana
 * Date: 11/03/11
 * Time: 09:41
 */
public class SymbolMappings {
    private Map<String, SymbolBeanPathPair> symbolToBeanPath = new HashMap<String, SymbolBeanPathPair>();
    private List<SymbolBeanPathPair> symbolBeanPathPairs = new ArrayList<SymbolBeanPathPair>();

    public SymbolMappings() {
    }

    public SymbolMappings(Map<String, String> symbolToBeanPath) {
        for(Map.Entry<String, String> entry: symbolToBeanPath.entrySet()) {
            addSymbolMapping(entry.getKey(), entry.getValue());
        }
    }

    public void initialiseFrom(SymbolMappings other) throws FormulaException {
        clear();
        for(SymbolBeanPathPair pair : other.symbolBeanPathPairs) {
            addSymbolMapping(pair.getSymbol(), pair.getBeanPath());
        }
    }

    public void addSymbolMapping() {
        addSymbolMapping(getNextValidSymbol(), "");
    }

    public void addSymbolMapping(String beanPath) {
        addSymbolMapping(getNextValidSymbol(), beanPath);
    }

    private void addSymbolMapping(String symbol, String beanPath) {
        SymbolBeanPathPair pair = new SymbolBeanPathPair(symbol, beanPath);
        SymbolBeanPathPair oldPair = symbolToBeanPath.put(symbol, pair);
        if(oldPair != null) {
            symbolBeanPathPairs.remove(oldPair);
        }
        symbolBeanPathPairs.add(pair);
    }

    private String getNextValidSymbol() {
        String postfix = "";
        int j = 1;
        String newSymbol;
        while (true) {
            for (char c = 'a'; c <= 'z'; c++) {
                newSymbol = c + postfix;
                if (! containsSymbol(newSymbol)) {
                    return newSymbol;
                }
            }
            postfix = "" + j++;
        }
    }

    public boolean containsSymbol(String symbol) {
        //check if we have already used this symbol
        return symbolToBeanPath.containsKey(symbol);
    }

    public String getBeanPathForSymbol(String symbol) {
        SymbolBeanPathPair pair = symbolToBeanPath.get(symbol);
        return pair == null ? null : pair.getBeanPath();
    }

    public Set<String> getSymbolForBeanPath(String beanPath) {
        Set<String> symbols = new HashSet<String>();
        for(SymbolBeanPathPair pair : symbolBeanPathPairs) {
            if(beanPath.equals(pair.getBeanPath())) {
                symbols.add(pair.getSymbol());
            }
        }
        return symbols;
    }

    public void removeSymbolAt(int index) {
        SymbolBeanPathPair pair = symbolBeanPathPairs.remove(index);
        if(pair != null) {
            symbolToBeanPath.remove(pair.getSymbol());
        }
    }

    // Check the manager is in a valid state
    public String[] validate() {
        List<String> errors = new ArrayList<String>();
        //check all symbols are valid
        for(SymbolBeanPathPair pair : symbolBeanPathPairs) {
            if(pair.beanPath == null || pair.beanPath.trim().length() == 0) {
                errors.add("Symbol \""+pair.getSymbol()+"\" does not have an associated property");
            }
        }

        return errors.toArray(new String[errors.size()]);
    }

    public Map<String, String> getSymbolToBeanPathMap() {
        Map<String, String> map = new HashMap<String, String>(symbolBeanPathPairs.size());
        for(SymbolBeanPathPair pair : symbolBeanPathPairs) {
            map.put(pair.getSymbol(), pair.getBeanPath());
        }
        return map;
    }

    public List<SymbolBeanPathPair> getSymbolBeanPathPairs() {
        return symbolBeanPathPairs;
    }

    public void clear() {
        symbolToBeanPath.clear();
        symbolBeanPathPairs.clear();
    }


    class SymbolBeanPathPair {
        private String symbol;
        private String beanPath;

        SymbolBeanPathPair(String symbol, String beanPath) {
            this.symbol = symbol;
            this.beanPath = beanPath;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getBeanPath() {
            return beanPath;
        }

        public void setSymbol(String symbol) throws FormulaException {
            if(symbol == null || symbol.trim().length() == 0) {
                throw new FormulaException("You must provide a valid symbol");
            }
            if(containsSymbol(symbol)) {
                throw new FormulaException("A symbol \""+symbol+"\" already exists");
            }
            symbolToBeanPath.remove(this.symbol);
            this.symbol = symbol;
            symbolToBeanPath.put(symbol, this);
        }

        public void setBeanPath(String beanPath) {
            this.beanPath = beanPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SymbolBeanPathPair that = (SymbolBeanPathPair) o;

            if (beanPath != null ? !beanPath.equals(that.beanPath) : that.beanPath != null) return false;
            if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = symbol != null ? symbol.hashCode() : 0;
            result = 31 * result + (beanPath != null ? beanPath.hashCode() : 0);
            return result;
        }
    }

}
