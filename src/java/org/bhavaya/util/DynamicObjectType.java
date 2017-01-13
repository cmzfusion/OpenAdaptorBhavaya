package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import java.util.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public class DynamicObjectType extends Generic.ObjectType {

    private final List<AttributeListener> attributeListeners = new LinkedList<AttributeListener>();
    private final List<DynamicAttribute> dynamicAttributes = new ArrayList<DynamicAttribute>();

    private Attribute[] attributes;
    private Map<String, Integer> nameToIndexMap;

    public DynamicObjectType(Class<?> c) {
        super(c);
    }

    public int size() {
        return super.size() + dynamicAttributes.size();
    }

    public Attribute[] getAttributes() {
        initialize();
        return attributes;
    }

    public int getAttributeIndex(String attributeName) {
        initialize();
        Integer index = nameToIndexMap.get(attributeName);
        if (index == null) {
            String error = "Undefined attribute: " + attributeName + " on type " + this;
            if (attributeName.indexOf('.') >= 0) {
                error += " . This looks like a path, not an attribute. Try using 'Generic.beanPathStringToArray' to convert'";
            }
            throw new RuntimeException(error);
        }
        return index;
    }

    public Attribute getAttribute(int attributeIndex) {
        initialize();
        return attributes[attributeIndex];
    }

    public boolean attributeExists(String attributeName) {
        initialize();
        Integer index = nameToIndexMap.get(attributeName);
        return index != null;
    }

    public Object get(Object instance, int attributeIndex) {
        initialize();
        Generic.GenericAttribute getter = (Generic.GenericAttribute) getAttribute(attributeIndex);
        return getter.get(instance);
    }

    public void set(Object instance, int attributeIndex, Object value) {
        initialize();
        Generic.GenericAttribute setter = (Generic.GenericAttribute) getAttribute(attributeIndex);
        setter.set(instance, value);
    }

    public void addAttribute(DynamicAttribute attribute) {
        if (attributeExists(attribute.getName())) {
            throw new RuntimeException("Duplicate attribute definition!");
        }
        synchronized (this) {
            reset();
            dynamicAttributes.add(attribute);
        }
        fireAttributeChange(new AttributeChangeEvent(this, AttributeChangeEvent.ACTION_ADD, attribute));
    }

    public void removeAttribute(DynamicAttribute attribute) {
        boolean removed;
        synchronized (this) {
            reset();
            removed = dynamicAttributes.remove(attribute);
        }
        if (removed) fireAttributeChange(new AttributeChangeEvent(this, AttributeChangeEvent.ACTION_REMOVE, attribute));
    }

    private synchronized void initialize() {
        if (attributes == null) {
            Attribute[] attributes = super.getAttributes();
            if (dynamicAttributes.size() > 0) {
                attributes = Utilities.appendArrays(attributes, dynamicAttributes.toArray(new Attribute[dynamicAttributes.size()]));
            }
            this.attributes = attributes;

            Map tmpNameToIndexMap = new ConcurrentReaderHashMap();
            for (int i = 0; i < attributes.length; i++) {
                Attribute attribute = attributes[i];
                String name = attribute.getName();
                // If there is already an entry for this name, leave the first one.
                if (tmpNameToIndexMap.get(name) == null) {
                    tmpNameToIndexMap.put(name, new Integer(i));
                }
            }
            this.nameToIndexMap = tmpNameToIndexMap;
        }
    }

    private synchronized void reset() {
        attributes = null;
        nameToIndexMap = null;
    }

    public void addAttributeListener(AttributeListener l) {
        synchronized (attributeListeners) {
            attributeListeners.add(l);
        }
    }

    public void removeAttributeListener(AttributeListener l) {
        synchronized (attributeListeners) {
            attributeListeners.remove(l);
        }
    }

    private void fireAttributeChange(AttributeChangeEvent event) {
        AttributeListener[] listenersSnapshot;

        synchronized (attributeListeners) {
            if (attributeListeners.size() == 0) return;
            listenersSnapshot = attributeListeners.toArray(new AttributeListener[attributeListeners.size()]);
        }

        for (AttributeListener attributeListener : listenersSnapshot) {
            attributeListener.attributeChange(event);
        }
    }

    public static class AttributeChangeEvent extends EventObject {
        public static final int ACTION_ADD = 1;
        public static final int ACTION_REMOVE = 2;

        private int action;
        private Attribute attribute;

        public AttributeChangeEvent(Object source, int action, Attribute attribute) {
            super(source);
            this.action = action;
            this.attribute = attribute;
        }

        public int getAction() {
            return action;
        }

        public Attribute getAttribute() {
            return attribute;
        }
    }

    public interface AttributeListener {
        void attributeChange(AttributeChangeEvent evt);
    }
}

