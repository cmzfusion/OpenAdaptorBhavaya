package org.bhavaya.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class BeanPropertyChangeSupport {
    private static final PropertyChangeListener[] EMPTY_LISTENER_ARRAY = new PropertyChangeListener[0];

    private Object source;
    private Map namedListeners;
    private Object genericListener; //can either be a single or a list of listeners
    public static boolean logEventTime = false;
    public static boolean safe = false;

    public BeanPropertyChangeSupport(Object source) {
        this.source = source;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        synchronized (this) {
            genericListener = addListener(genericListener, l);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        synchronized (this) {
            genericListener = removeListener(genericListener, l);
        }
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        synchronized (this) {
            assert (Generic.getType(source).attributeExists(propertyName)) : "Tried to add listener for property " + propertyName + " on instanceof " + source.getClass();
            Object singleOrArray = getNamedListeners().get(propertyName);
            Object newSingleOrArray = addListener(singleOrArray, l);
            getNamedListeners().put(propertyName, newSingleOrArray);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        synchronized (this) {
            Object singleOrArray = getNamedListeners().get(propertyName);
            Object newSingleOrArray = removeListener(singleOrArray, l);
            if (newSingleOrArray == null) {
                getNamedListeners().remove(propertyName);
            } else {
                getNamedListeners().put(propertyName, newSingleOrArray);
            }
        }
    }

    private Map getNamedListeners() {
        synchronized (this) {
            if (namedListeners == null) {
                namedListeners = new HashMap(8);
            }
            return namedListeners;
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (Utilities.equals(oldValue, newValue)) {
            return;
        }

        Object genericListenerCopy = null;
        Object namedListenerCopy = null;

        synchronized (this) {
            genericListenerCopy = genericListener;
            namedListenerCopy = (namedListeners != null) ? namedListeners.get(propertyName) : null;
        }

        if (genericListenerCopy != null || namedListenerCopy != null) {
            PropertyChangeEvent evt;
            if (logEventTime) {
                evt = new TimedPropertyChangeEvent(source, propertyName, oldValue, newValue);
            } else {
                evt = new PropertyChangeEvent(source, propertyName, oldValue, newValue);
            }
            fireEventOnListeners(evt, genericListenerCopy);
            fireEventOnListeners(evt, namedListenerCopy);
        }
    }

    private void fireEventOnListeners(PropertyChangeEvent evt, Object singleOrArraylisteners) {
        if (singleOrArraylisteners == null) return;

        if (singleOrArraylisteners instanceof PropertyChangeListener) {
            PropertyChangeListener listener = (PropertyChangeListener) singleOrArraylisteners;
            listener.propertyChange(evt);
        } else {
            PropertyChangeListener[] listeners = (PropertyChangeListener[]) singleOrArraylisteners;
            for (int i = 0; i < listeners.length; i++) {
                PropertyChangeListener listener = listeners[i];
                listener.propertyChange(evt);
            }
        }
    }


    private static Object addListener(Object singleOrArray, PropertyChangeListener listener) {
        assert (listener != null) : "Trying to add a null listener";
        Object newListeners = null;

        if (singleOrArray == null) {
            newListeners = listener;
        } else if (singleOrArray instanceof PropertyChangeListener) {
            newListeners = new PropertyChangeListener[]{(PropertyChangeListener) singleOrArray, listener};
        } else {
            PropertyChangeListener[] oldListeners = (PropertyChangeListener[]) singleOrArray;
            newListeners = new PropertyChangeListener[oldListeners.length + 1];
            System.arraycopy(oldListeners, 0, newListeners, 0, oldListeners.length);
            ((PropertyChangeListener[]) newListeners)[oldListeners.length] = listener;
        }

        return newListeners;
    }

    private static Object removeListener(Object singleOrArray, PropertyChangeListener listener) {
        assert (listener != null) : "Trying to remove a null listener";
        assert (!(singleOrArray instanceof PropertyChangeListener[] && ((PropertyChangeListener[]) singleOrArray).length == 1)) : "Should never have a listener array of length 1";
        assert (!(singleOrArray instanceof PropertyChangeListener[] && ((PropertyChangeListener[]) singleOrArray).length == 0)) : "Should never have a listener array of length 0";

        int currentSize = (singleOrArray == null) ? 0 : (singleOrArray instanceof PropertyChangeListener) ? 1 : ((PropertyChangeListener[]) singleOrArray).length;

        if (currentSize == 0) {
            handleError("Trying to remove a generic listener that was never added: " + listener);
            return singleOrArray;

        } else if (currentSize == 1) {
            if (listener == singleOrArray) return null;
            handleError("Trying to remove a generic listener that was never added: " + listener);
            return singleOrArray;

        } else if (currentSize > 2) {
            PropertyChangeListener[] oldListeners = (PropertyChangeListener[]) singleOrArray;

            PropertyChangeListener[] newListenersArray = new PropertyChangeListener[oldListeners.length - 1];

            for (int i = 0; i < oldListeners.length; i++) {
                if (oldListeners[i] == listener) {
                    int nextIndex = i + 1;
                    System.arraycopy(oldListeners, nextIndex, newListenersArray, i, oldListeners.length - nextIndex);
                    return newListenersArray;
                } else if(i == newListenersArray.length){
                    handleError("Trying to remove a generic listener that was never added: " + listener);
                    return singleOrArray;
                } else {
                    newListenersArray[i] = oldListeners[i];
                }
            }


        } else if (currentSize == 2) {
            PropertyChangeListener[] oldListeners = (PropertyChangeListener[]) singleOrArray;
            if (oldListeners[0] == listener) return oldListeners[1];
            if (oldListeners[1] == listener) return oldListeners[0];
            handleError("Trying to remove a generic listener that was never added: " + listener);
            return singleOrArray;

        }

        handleError("Trying to remove listener, but did nothing");
        return null;
    }

    private static void handleError(String message) {
        RuntimeException t = new RuntimeException(message);
        Log.getCategory(DefaultObservable.class).error(t);
        if (safe) throw t;
    }

    public boolean isObserved(String propertyName) {
        synchronized (this) {
            return genericListener != null || ((namedListeners != null) ? namedListeners.get(propertyName) : null) != null;
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        Object singleOrArray;
        synchronized (this) {
            singleOrArray = genericListener;
        }
        return toListenerArray(singleOrArray);
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        Object singleOrArray;
        synchronized (this) {
            singleOrArray = namedListeners != null ? namedListeners.get(propertyName) : namedListeners;
        }
        return toListenerArray(singleOrArray);
    }

    private static PropertyChangeListener[] toListenerArray(Object singleOrArray) {
        if (singleOrArray == null) return EMPTY_LISTENER_ARRAY;
        if (singleOrArray instanceof PropertyChangeListener) return new PropertyChangeListener[]{(PropertyChangeListener) singleOrArray};
        return (PropertyChangeListener[]) singleOrArray;
    }

    /**
     * used for debugging and seeing how long it takes for events to get processed.
     */
    public static class TimedPropertyChangeEvent extends PropertyChangeEvent {
        private long time;

        public TimedPropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
            super(source, propertyName, oldValue, newValue);
            time = System.currentTimeMillis();
        }

        public long getTime() {
            return time;
        }
    }
}
