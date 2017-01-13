package org.bhavaya.util;

import org.bhavaya.ui.view.View;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public class WeakReferencingPropertyChangeListener implements PropertyChangeListener {
    WeakReference listenerRef;
    Object src;

    public WeakReferencingPropertyChangeListener(PropertyChangeListener listener, Object src){
        listenerRef = new WeakReference(listener);
        this.src = src;
    }

    public void propertyChange(PropertyChangeEvent evt){
        PropertyChangeListener listener = (PropertyChangeListener)listenerRef.get();
        if(listener==null){
            removeListener();
        }
        else {
            listener.propertyChange(evt);
        }
    }

    private void removeListener(){
        try{
            Method method = src.getClass().getMethod("removePropertyChangeListener", new Class[] {PropertyChangeListener.class});
            method.invoke(src, new Object[]{ this });

        } catch(Exception e){
            e.printStackTrace();
        }
    }
}

