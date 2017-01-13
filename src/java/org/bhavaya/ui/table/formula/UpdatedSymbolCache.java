package org.bhavaya.ui.table.formula;

import java.util.Map;
import java.util.Set;

/**
 * Definition of a cache for storing a set of symbols that have been updated for a particular object.
 * Implementation of this are required to be thread safe
 * User: Jon Moore
 * Date: 20/01/11
 * Time: 16:41
 */
public interface UpdatedSymbolCache {

    void addUpdated(Object obj, Set<String> symbols);

    Map<Object, Set<String>> getUpdated();

}
