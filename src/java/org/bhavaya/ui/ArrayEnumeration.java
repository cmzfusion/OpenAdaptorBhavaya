package org.bhavaya.ui;

import java.util.Enumeration;

/**
 * An Enumeration, for..., wait for it, (drum roll...), an array.
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class ArrayEnumeration implements Enumeration {
    int index;
    private final Object[] objects;

    public ArrayEnumeration(Object[] objects) {
        this.objects = objects;
    }

    public boolean hasMoreElements() {
        if (objects == null) return false;
        return index < objects.length;
    }

    public Object nextElement() {
        Object object = objects[index];
        index++;
        return object;
    }
}
