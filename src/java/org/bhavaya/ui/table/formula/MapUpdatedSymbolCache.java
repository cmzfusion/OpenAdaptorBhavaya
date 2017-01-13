package org.bhavaya.ui.table.formula;

import java.util.*;

/**
 * Standard map based UpdatedSymbolCache.
 * Requires heavy synchronization so we probably won't use this one.
 * User: Jon Moore
 * Date: 20/01/11
 * Time: 16:20
 */
public class MapUpdatedSymbolCache implements UpdatedSymbolCache {

    private Map<Object, Set<String>> symbolsToUpdate = new HashMap<Object, Set<String>>();

    public synchronized void addUpdated(Object obj, Set<String> syms) {
        Set<String> symbols = symbolsToUpdate.get(obj);
        if(symbols == null) {
            symbols = new HashSet<String>();
            symbolsToUpdate.put(obj, symbols);
        }
        symbols.addAll(syms);
    }

    public synchronized Map<Object, Set<String>> getUpdated(){
        Map<Object, Set<String>> copy = new HashMap<Object, Set<String>>(symbolsToUpdate);
        symbolsToUpdate.clear();
        return copy;
    }
}
