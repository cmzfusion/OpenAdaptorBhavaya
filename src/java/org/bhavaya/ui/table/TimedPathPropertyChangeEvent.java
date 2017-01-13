package org.bhavaya.ui.table;

import org.bhavaya.util.Log;

import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $                                                                                         De
 */
public class TimedPathPropertyChangeEvent extends PathPropertyChangeEvent {
    private static final Log log = Log.getCategory(TimedPathPropertyChangeEvent.class);
    private long time;

    public TimedPathPropertyChangeEvent(Set roots, Object source, String[] parentPathFromRoot, Object oldValue, long causeTime) {
        super(roots, source, parentPathFromRoot, oldValue);
        time = causeTime;
    }

    public long getTime() {
        return time;
    }
}
