package org.bhavaya.ui.table.formula;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of UpdatedSymbolCache that uses a ConcurrentMap.
 * The map only stores data in the keys, which are object/symbol pairs
 * User: Jon Moore
 * Date: 20/01/11
 * Time: 16:20
 */
public class ConcurrentMapUpdatedSymbolCache implements UpdatedSymbolCache {

    private static final String DUMMY_VALUE = "dummy";
    private final ConcurrentMap<Key, String> map = new ConcurrentHashMap<Key, String>();

    static final class Key {
        final Object obj;
        final String str;
        final int hashCode;

        public Key(Object obj, String str) {
            super();
            this.obj = obj;
            this.str = str;
            this.hashCode = calcHashCode(obj, str);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private int calcHashCode(Object o1, Object o2) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
            result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (this.obj == null) {
                if (other.obj != null)
                    return false;
            } else if (!this.obj.equals(other.obj))
                return false;
            if (str == null) {
                if (other.str != null)
                    return false;
            } else if (!str.equals(other.str))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Key [obj=" + obj + ", str=" + str + "]";
        }
    }

    public void addUpdated(Object obj, Set<String> symbols) {
        for(String symbol : symbols) {
            final Key key = new Key(obj, symbol);
            map.put(key, DUMMY_VALUE);
        }
    }

    public Map<Object, Set<String>> getUpdated(){
        final Map<Object, Set<String>> result = new HashMap<Object, Set<String>>();

        final Set<Key> e = map.keySet();
        final Iterator<Key> it = e.iterator();

        while (it.hasNext()) {
            final Key key = it.next();
            final Object obj = key.obj;
            final String str = key.str;

            //get the mapping for the object
            Set<String> mappingsForObj = result.get(obj);
            if (mappingsForObj == null) {
                mappingsForObj = new HashSet<String>();
                result.put(obj, mappingsForObj);
            }

            mappingsForObj.add(str);

            // only removes if the value has not changed, so if it has been updated,
            // then leave in the map for next time.
            map.remove(key);
        }

        return result;
    }

}
