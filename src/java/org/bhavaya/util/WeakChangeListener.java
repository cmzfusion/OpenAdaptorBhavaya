package org.bhavaya.util;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

//TODO DUPLICATION OF WeakReferencingPropertyChangeListener. REFACTOR
public class WeakChangeListener implements ChangeListener {
    WeakReference listenerRef;
    Object src;

    public WeakChangeListener(ChangeListener listener, Object src){
        listenerRef = new WeakReference(listener);
        this.src = src;
    }

    public void stateChanged(ChangeEvent e) {
        ChangeListener listener = (ChangeListener)listenerRef.get();
        if(listener==null){
            removeListener();
        }
        else {
            listener.stateChanged(e);
        }
    }

    private void removeListener(){
        try{
           Method method = src.getClass().getMethod("removeChangeListener", new Class[] {PropertyChangeListener.class});
           method.invoke(src, new Object[]{ this });
        } catch(Exception e){
            e.printStackTrace();
        }
    }


}

