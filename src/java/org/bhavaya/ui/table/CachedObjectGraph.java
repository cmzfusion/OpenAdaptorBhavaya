/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui.table;

import org.bhavaya.collection.SingleItemSet;
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.util.*;
import org.bhavaya.util.Observable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Provides a thread safe way of monitoring a graph of observable objects. We use this to add "subpropertyListeners" to a graph.
 * e.g. a listener on the property path: Trade.instrument.issuer.equity.price
 * The problem with any normal attempt to maintain the chain of listeners required for this task is that unless you have synchronised
 * every setter in the bean tree on the same lock, then safe listener maintenance becomes impossible (i.e. you may fail to remove some, or some may be added to
 * multiple beans by mistake)
 * <p/>
 * Anyway, it is all a little bit detailed, but suffice to say that this way of doing things acheives guaranteed listener chaining, with the advantage of
 * guaranteed listener cleanup. (i.e. no memory leaks. Hurrah)
 * <p/>
 * Also, the advanatage of this approach is you get perfect agreement between the values of a property change event, and
 * the current value of a bean as interrogated through the CachedModel. This is very important for the value-saftey of our more complex table models (e.g. grouping).
 * <p/>
 * there are still a number of "todo's" in this class, but I'm not 100% sure how many of them are important.
 * I really should get round to checking.
 *
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.33 $
 */
public class CachedObjectGraph {
    private static final Log log = Log.getCategory(CachedObjectGraph.class);

    public final static NotReadyAwareComparator NOT_READY_AWARE_COMPARATOR = new NotReadyAwareComparator();
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public final static Object DATA_NOT_READY = new Object() {
        public String toString() {
            return "Loading...";
        }
    };
    private final static Object UNINITIALISED = new Object();
    private static int graphCount = 0;

    private PropertyPathTree propertyTree = new PropertyPathTree();
    private IdentityHashMap referenceGraph;  //object -> CachedProperty
    private int removals;

//    private IdentityHashMap rootToPropertyChangeDistributer = new IdentityHashMap();
    private HashSet roots;

    private Object changeLock = new ChangeLock();
    private Class beanType; //not really needed other than for checking the bean paths submitted are valid.

    private boolean asynchronous;

    private boolean disposed = false;
    private PropertyLoadQueue propertyLoadQueue;

    private boolean isWithinPropertyGetter = false;
    private IdentityHashMap pendingPropertiesTillReady;   // For a given root, store which properties we need to receive values for before the whole row is ready

    private List rowReadyListeners = new LinkedList();
    private List multiPropertyChangeListeners = new LinkedList();

    private Thread loadQueueThread;

    private ExecutionController executionController;
    private volatile boolean started = false;

    public CachedObjectGraph(Class beanType) {
        this(beanType, false);
    }

    public CachedObjectGraph(Class beanType, boolean asynchronous) {
        this (beanType, asynchronous, null);
    }

    public CachedObjectGraph(Class beanType, boolean asynchronous, String name) {
        initializeCollections();
        this.beanType = beanType;
        this.asynchronous = asynchronous;
        name = name == null ? ClassUtilities.getUnqualifiedClassName(beanType) : name;
        if (asynchronous) {
            propertyLoadQueue = new PropertyLoadQueue();
            loadQueueThread = new Thread(propertyLoadQueue, "OG-load queue: " + name + (graphCount++));
            loadQueueThread.setPriority(Thread.NORM_PRIORITY - 1);
            loadQueueThread.setDaemon(true);
        }
    }

    private void initializeCollections() {
        //many CachedObjectGraph have just one root bean, and we have many thousands of instances
        //so by default these collections need to be very small, to avoid wasting memory
        referenceGraph = new IdentityHashMap(2);
        roots = new HashSet(2);
        pendingPropertiesTillReady = new IdentityHashMap(2);
    }

    public void setExecutionController(ExecutionController executionController) {
        this.executionController = executionController;
    }

    public ExecutionController getExecutionController() {
        return executionController;
    }

    public void addRootObject(Object obj) {
        synchronized (getChangeLock()) {
            if (!started ) {
                if (loadQueueThread != null) loadQueueThread.start();
                started = true;
            }

            if (!roots.contains(obj)) {
                roots.add(obj);
                PropertyPathNode rootProperty = propertyTree.root;
                objectUsedByRoots(obj, rootProperty, new SingleItemSet(obj));

                if (!rowReadyListeners.isEmpty()) {
                    HashSet pendingProperties = new HashSet(16);
                    pendingPropertiesTillReady.put(obj, pendingProperties);
                    addChildPropertiesNeedingLoad(obj, rootProperty);
                }
            }
        }
    }

    /**
     * for each property of the given object that is a child of parentProperty, check to see if its value
     * is "not ready". If so, add that property path to the "waiting for" hashset.
     * @param obj
     * @param parentProperty
     */
    private void addChildPropertiesNeedingLoad(Object obj, PropertyPathNode parentProperty) {
        HashSet pendingProperties = (HashSet) pendingPropertiesTillReady.get(obj);
        Iterator properties = parentProperty.getChildren();
        while (properties.hasNext()) {
            PropertyPathNode node = (PropertyPathNode) properties.next();
            Object currentValue = get(obj, node.pathFromRoot);
            if (currentValue == DATA_NOT_READY) {
                pendingProperties.add(node);
            }
        }
    }

    public void removeRootObject(Object obj) {
        synchronized (getChangeLock()) {
            roots.remove(obj);
            objectNoLongerUsedByRoots(obj, propertyTree.root, new SingleItemSet(obj));
            pendingPropertiesTillReady.remove(obj);
        }
    }

    public boolean isWaitingForProperties(Object root) {
        synchronized (getChangeLock()) {
            return pendingPropertiesTillReady.containsKey(root);
        }
    }

    /**
     * What is this? Very good question as it is not obvious in its purpose.
     * It is only useful when using an asynchronous CachedObjectGraph.
     * The effect is that the listener will be told once all observed properties of a bean have been loaded.
     * The most obvious usage for this is when using CachedObjectGraph to back a table of beans that can have
     * blocking property getters.
     * We can write a listener that prevents new beans from being shown in the table until all its columns have data
     * @param l
     */
    public void addRowReadyListener(RowReadyListener l) {
        rowReadyListeners.add(l);
    }

    public void removeRowReadyListener(RowReadyListener l) {
        rowReadyListeners.remove(l);
    }

    private void fireRowReady(Object root) {
        RowReadyListener[] allListeners = (RowReadyListener[]) rowReadyListeners.toArray(new RowReadyListener[0]);
        for (int i = 0; i < allListeners.length; i++) {
            RowReadyListener rowReadyListener = allListeners[i];
            rowReadyListener.rowReady(root);
        }
    }

    private void addMultipleChangeListener(GraphChangeListener l) {
        multiPropertyChangeListeners.add(l);
    }

    private void removeMultipleChangeListener(GraphChangeListener l) {
        multiPropertyChangeListeners.remove(l);
    }

    private void fireMultiplePropertiesChanged(boolean allAffectSameRoots, Collection changes) {
        for (Iterator iterator = multiPropertyChangeListeners.iterator(); iterator.hasNext();) {
            GraphChangeListener listener = (GraphChangeListener) iterator.next();
            listener.multipleChange(changes, allAffectSameRoots);
        }
    }


    private void objectNoLongerUsedByRoots(Object object, PropertyPathNode pathFromRoot, Set roots) {
        if (hasCacheableProperties(object)) {
            CachedProperties cachedProperties = getCachedProperties(object);
            if (cachedProperties != null) {
                cachedProperties.noLongerUsedByRoots(pathFromRoot, roots);
            }
        }
    }

    private void objectUsedByRoots(Object object, PropertyPathNode pathFromRoot, Set roots) {
        if (hasCacheableProperties(object)) {
            CachedProperties cachedProperties = getCachedProperties(object);
            if (cachedProperties == null) {
                cachedProperties = addNewCachedProperties(pathFromRoot, object);
            }
            cachedProperties.usedByRoots(pathFromRoot, roots);
        }
    }

    private void updateCacheForNewPath(PropertyPathNode newPathNode) {
        SingleItemSet singleRootObject = new SingleItemSet();  // Reuse for garbage minimisation purposes

        String[] parentPath = newPathNode.getParent().getPathFromRoot();

        Iterator rootObjects = getRootObjects();
        while (rootObjects.hasNext()) {
            Object root = rootObjects.next();

            Object ownerOfNewProperty = get(root, parentPath);
            if (hasCacheableProperties(ownerOfNewProperty)) {
                //ensure that the owner container exists, and that it has a storage location for the new property
                CachedProperties ownerCachedProperties = getCachedProperties(ownerOfNewProperty);
                if (ownerCachedProperties == null) {
                    ownerCachedProperties = addNewCachedProperties(newPathNode.getParent(), ownerOfNewProperty);
                }
                Type type = Generic.getType(ownerOfNewProperty.getClass());
                String property = newPathNode.getProperty();
                if (type.attributeExists(property)) {
                    Attribute[] newAttributes = new Attribute[]{type.getAttribute(property)};
                    ownerCachedProperties.reTypeDataStorage(true, newAttributes);
                }
            }

            singleRootObject.clear();
            singleRootObject.add(root);
            objectUsedByRoots(ownerOfNewProperty, newPathNode.getParent(), singleRootObject);
        }
    }

    private void updateCacheForPathRemoval(PropertyPathNode parentNode, PropertyPathNode prunedNode) {
        SingleItemSet singleRootObject = new SingleItemSet();  //reuse for garbage minimisation purposes

        String[] path = prunedNode.getPathFromRoot();
        Iterator rootObjects = getRootObjects();
        while (rootObjects.hasNext()) {
            Object root = rootObjects.next();
            singleRootObject.clear();
            singleRootObject.add(root);

            Object unusedProperty = get(root, path);
            CachedProperties cachedProperties = getCachedProperties(unusedProperty);
            if (cachedProperties != null) {
                cachedProperties.noLongerUsedByRoots(prunedNode, singleRootObject);
            }
        }
        if (!rowReadyListeners.isEmpty()) {
            // note: don't be tempted to move parts of the code below into the above "while" loop. It is safest to do the
            // entire iteration twice - also consider that path removals should not occur all that frequently.
            // One root may be an unready property of another. We must do the "noLongerUsedByRoots" calls for all roots,
            // before we safely say whether a root is ready or not.
            rootObjects = getRootObjects();
            while (rootObjects.hasNext()) {
                Object root = rootObjects.next();
                Set pendingProperties = (Set) pendingPropertiesTillReady.get(root);
                if (pendingProperties != null) {
                    removePropertySubtreeFromSet(prunedNode, pendingProperties);
                    if (pendingProperties.isEmpty()) {
                        pendingPropertiesTillReady.remove(root);
                        fireRowReady(root);
                    }
                }
            }
        }
    }

    private void removePropertySubtreeFromSet(PropertyPathNode prunedNode, Set pendingProperties) {
        // The usage cases of this make me think that walking the prunedNode subtree will be less costly than iterating
        // the pendingProperties set and asking each if it is a child of prunedNode. Have not tested this.
        pendingProperties.remove(prunedNode);
        if (! pendingProperties.isEmpty()) {
            Iterator children = prunedNode.getChildren();
            while (children.hasNext()) {
                PropertyPathNode propertyPath = (PropertyPathNode) children.next();
                removePropertySubtreeFromSet(propertyPath, pendingProperties);
            }
        }
    }

    public void addPathListener(String propertyPath, GraphChangeListener listener) {
        synchronized (getChangeLock()) {
            String[] newPath = Generic.beanPathStringToArray(propertyPath.intern());

            PropertyPathNode insertedNode = propertyTree.addListener(newPath, listener);


            if (insertedNode != null) {
                updateCacheForNewPath(insertedNode);
            }
        }
    }

    public void removePathListener(String propertyPath, GraphChangeListener listener) {
        synchronized (getChangeLock()) {
            String[] removePath = Generic.beanPathStringToArray(propertyPath.intern());

            PropertyPathNode prunedNode = propertyTree.removeListener(removePath, listener);
            if (prunedNode != null) {
                int level = prunedNode.getDepth() - 1;
                String[] subPath = new String[removePath.length - level];
                System.arraycopy(removePath, level, subPath, 0, subPath.length);
                PropertyPathNode parent = prunedNode.getParent();
                prunedNode.removeFromParent();
                updateCacheForPathRemoval(parent, prunedNode);
            }
        }
    }


    protected Iterator getRootObjects() {
        return roots.iterator();
    }

    public Object get(Object root, String propertyPath) {
        synchronized (getChangeLock()) {
//          String[] pathComponents = Generic.beanPathStringToArray(propertyPath.intern());
            String[] pathComponents = Generic.beanPathStringToArray(propertyPath);
            Object value = get(root, pathComponents);
            return value;
        }
    }

    protected Object get(Object root, String[] pathComponents) {
        Object obj = root;
        PropertyPathNode node = propertyTree.root;
        for (int i = 0; i < pathComponents.length; i++) {
            if (node == null) throw new RuntimeException("Either path is invalid, or no listener has been added to: " + Generic.beanPathArrayToString(pathComponents));

            node = node.getChild(pathComponents[i]);
            if (obj == DATA_NOT_READY) return DATA_NOT_READY;

            if (!hasCacheableProperties(obj)) {
                try {
                    return Generic.get(obj, pathComponents, i, true);
                } catch (Exception e) {
                    log.error(e);
                    return null;   //todo: maybe introduce an ERROR object?
                }
            } else {
                String property = pathComponents[i];
                CachedProperties cachedProperties = getCachedProperties(obj);
                Object propertyValue = cachedProperties.getPropertyValue(property);
                obj = propertyValue;
            }
        }
        return obj;
    }

    private CachedProperties addNewCachedProperties(PropertyPathNode pathOfParent, Object parent) {
        Class aClass = parent.getClass();
        Attribute[] attributes = pathOfParent.getChildPropertiesForClass(aClass);

        Type generatedType = Generic.getType(attributes);
        CachedProperties cachedProperties = createCachedProperties(generatedType, parent);
        referenceGraph.put(parent, cachedProperties);
        return cachedProperties;
    }

    protected CachedProperties createCachedProperties(Type type, Object parent) {
        return new CachedProperties(type, parent);
    }

    private boolean hasCacheableProperties(Object object) {
        return object instanceof Observable;
    }

   protected  CachedProperties getCachedProperties(Object object) {
        if (!hasCacheableProperties(object)) {
            return null;
        }

        CachedProperties cachedProperties = (CachedProperties) referenceGraph.get(object);
        return cachedProperties;
    }

    public Object getChangeLock() {
        return changeLock;
    }

    /**
     * This is here so that you can override when cachedObjectGraph actually updates itself in reponse to a property change
     * e.g. override this method and plug in a change queue if you feel like it
     *
     * @param event
     */
    protected void receivePropertyChange(CachedProperties cachedProperties, PropertyChangeEvent event) {
        synchronized (getChangeLock()) {
            if (isWithinPropertyGetter) {
                log.warn("Looks like a property getter is firing a property change event on " + event.getPropertyName() + " of object " + event.getSource() + ". This is bad practice and means I will have to think really hard about whether this could possibly cause invalid state within CachedObjectGraph. I hope you feel bad", new RuntimeException());
            }
            cachedProperties.processPropertyChange(event);
        }
    }

    public void dispose() {
        synchronized (getChangeLock()) {
            clear();
            disposed = true;
            if (loadQueueThread != null) loadQueueThread.interrupt();
            propertyTree = null;
        }
    }

    public void clear() {
        synchronized (getChangeLock()) {
            Iterator iter = referenceGraph.values().iterator();
            while (iter.hasNext()) {
                CachedProperties cachedProperties = (CachedProperties) iter.next();
                cachedProperties.dispose();
            }
            //re-initialize these collections, otherwise this can be a memory leak
            //the internal arrays end up very large, and 100% sparse
            initializeCollections();
            if (propertyLoadQueue != null) {
                propertyLoadQueue.clear();
            }
        }
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    private class PropertyPathTree {
        private PropertyPathNode root = new PropertyPathNode(null, "");

        public PropertyPathNode addListener(String[] newPath, GraphChangeListener listener) {
            if (newPath.length == 0) {
                root.addListener(listener);
                return null;
            } else {
                assert (PropertyModel.getInstance(beanType).getAttribute(newPath) != null) : "No such property path: " + Generic.beanPathArrayToString(newPath) + " from class: " + beanType;
                PropertyPathNode insertedNode = root.mergePath(newPath, 0);
                PropertyPathNode listenerNode = root.findNode(newPath, 0);
                listenerNode.addListener(listener);

                return insertedNode;
            }
        }

        public PropertyPathNode removeListener(String[] removePath, GraphChangeListener listener) {
            PropertyPathNode listenerNode = root.findNode(removePath, 0);
            listenerNode.removeListener(listener);

            PropertyPathNode removePoint = getPrunePoint(listenerNode);
            return removePoint;
        }

        /**
         * answers the question: if the given node is removed from the tree, where is the best prune point.
         * @param startNode
         */
        private PropertyPathNode getPrunePoint(PropertyPathNode startNode) {
            PropertyPathNode pruneNode = null;

            if (!startNode.isLeaf()) {
                // Since the start node is not a leaf, then children depend on it being in the property tree
                return null;
            }

            // Traverse up to root and stop just before the first node that is either a listener destination, or has other
            // children that are part of the path to a listener destination
            PropertyPathNode currentNode = startNode;
            while (currentNode != root && currentNode.getChildCount() <= 1 && !currentNode.isListenerPath()) {
                pruneNode = currentNode;
                currentNode = currentNode.getParent();
            }
            return pruneNode;
        }

        public PropertyPathNode getNode(String[] pathFromRoot) {
            PropertyPathNode node = root;
            for (int i = 0; i < pathFromRoot.length; i++) {
                node = node.getChild(pathFromRoot[i]);
            }
            return node;
        }
    }

    private class PropertyPathNode {
        private String[] pathFromRoot;
        private String property;
        private HashMap propertyToChild = new HashMap(1);
        private HashMap classToChildAttributes = new HashMap(2);
        private PropertyPathNode parent;
        private List listeners;
        private PathPropertyChangeEvent pendingChange = null;
        private boolean disposed = false;


        public PropertyPathNode(PropertyPathNode parent, String property) {
            this.parent = parent;
            this.property = property;

            if (parent != null) {
                String[] parentPath = parent.getPathFromRoot();

                pathFromRoot = new String[parentPath.length + 1];
                System.arraycopy(parentPath, 0, pathFromRoot, 0, parentPath.length);
                pathFromRoot[pathFromRoot.length - 1] = property;
            } else {
                pathFromRoot = EMPTY_STRING_ARRAY;
            }
        }

        protected boolean isDisposed() {
            return disposed;
        }

        protected void setDisposed(boolean disposed) {
            this.disposed = disposed;
            if (disposed) {
                Iterator iter = getChildren();
                while (iter.hasNext()) {
                    PropertyPathNode propertyPathNode = (PropertyPathNode) iter.next();
                    propertyPathNode.setDisposed(true);
                }
            }
        }

        protected boolean isListenerPath() {
            return listeners != null;
        }

        public void addListener(GraphChangeListener listener) {
            if (listeners == null) {
                listeners = new ArrayList(1);
            }
            listeners.add(listener);
            addMultipleChangeListener(listener);
        }

        public void removeListener(GraphChangeListener listener) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                listeners = null;
            }
            removeMultipleChangeListener(listener);
        }


        public String getProperty() {
            return property;
        }

        /**
         * @param path
         * @param node
         * @return the first new node added to the tree
         */
        public PropertyPathNode mergePath(String[] path, int node) {
            if (node < path.length) {
                String property = path[node];
                PropertyPathNode child = getChild(property);
                if (child == null) {
                    child = new PropertyPathNode(this, property);
                    addChildNode(property, child);

                    child.mergePath(path, node + 1);
                    return child;
                } else {
                    return child.mergePath(path, node + 1);
                }
            }
            return null;
        }

        private void addChildNode(String property, PropertyPathNode child) {
            propertyToChild.put(property, child);
            classToChildAttributes.clear();
        }

        public String[] getPathFromRoot() {
            return pathFromRoot;
        }

        public PropertyPathNode findNode(String[] path, int startIndex) {
            if (startIndex == path.length) return this;

            String property = path[startIndex];
            PropertyPathNode child = getChild(property);
            return child.findNode(path, startIndex + 1);
        }

        public Iterator getChildren() {
            return propertyToChild.values().iterator();
        }

        public PropertyPathNode getParent() {
            return parent;
        }

        public PropertyPathNode getChild(String property) {
            return (PropertyPathNode) propertyToChild.get(property);
        }

        public int getDepth() {
            return getPathFromRoot().length;
        }

        public PropertyPathNode getCommonParent(PropertyPathNode another) {
            if (another == null) return this;

            PropertyPathNode leftNode = this;
            int leftDepth = leftNode.getDepth();
            PropertyPathNode rightNode = another;
            int rightDepth = rightNode.getDepth();

            while (leftDepth > rightDepth) {
                leftNode = leftNode.getParent();
                leftDepth--;
            }
            while (rightDepth > leftDepth) {
                rightNode = rightNode.getParent();
                rightDepth--;
            }
            while (leftNode != rightNode) {
                leftNode = leftNode.parent;
                rightNode = rightNode.parent;
            }
            return leftNode;
        }

        public String toString() {
            return Arrays.asList(getPathFromRoot()).toString();
        }

        /**
         * @return true if multiple sets of roots have been merged into one change event
         */
        public boolean gatherOldValuesForListeners(Set roots, Object parent, Object oldValue, boolean hasMultiUsagePaths) {
            if (isDisposed()) return false;

            boolean mergedRoots = false;
            if (isListenerPath()) {
                if (pendingChange != null) {
                    //how can this happen, you might ask? If a bean has a circular reference to itself then two paths
                    //(one being a subpath of the other) will be affected by the same property change.
                    assert (hasMultiUsagePaths) : "Hmmm, somehow a pending change for path: " + Generic.beanPathArrayToString(pendingChange.getPathFromRoot(), false) + " was unexpectedly present. Old: " + pendingChange.getOldValue() + " new: " + pendingChange.getNewValue() + " roots: " + pendingChange.getRoots();
                    pendingChange.addRoots(roots);
                    mergedRoots = true;
                } else
                if (currentEvent instanceof BeanPropertyChangeSupport.TimedPropertyChangeEvent) {
                    long creationTime = ((BeanPropertyChangeSupport.TimedPropertyChangeEvent) currentEvent).getTime();
                    pendingChange = new TimedPathPropertyChangeEvent(roots, parent, getPathFromRoot(), oldValue, creationTime);
                } else {
                    pendingChange = new PathPropertyChangeEvent(roots, parent, getPathFromRoot(), oldValue);
                }
            }

            Iterator children = getChildren();
            while (children.hasNext()) {
                PropertyPathNode child = (PropertyPathNode) children.next();
                Object childValue;
                if (!hasCacheableProperties(oldValue)) {
                    if (oldValue == DATA_NOT_READY) {
                        childValue = DATA_NOT_READY;
                    } else {
                        String[] path = child.getPathFromRoot();
                        try {
                            childValue = Generic.get(oldValue, path, path.length - 1, true);
                        } catch (Exception e) {
                            log.error(e);
                            childValue = null;   //todo: maybe introduce an ERROR object?
                        }
                    }
                } else {
                    CachedProperties cachedProperties = getCachedProperties(oldValue);
                    if (cachedProperties == null) {
                        log.error("this indicates a bug that Dan thinks he's fixed. " + oldValue, new RuntimeException());
                        childValue = null;
                    } else {
                        childValue = cachedProperties.getPropertyValue(child.getProperty());
                    }
                }
                mergedRoots |= child.gatherOldValuesForListeners(roots, oldValue, childValue, hasMultiUsagePaths);
            }
            return mergedRoots;
        }

        public void gatherNewValuesForListeners(Set roots, Object newValue) {
            if (isDisposed()) return;

            if (isListenerPath()) {
                if (Utilities.equals(pendingChange.getOldValue(), newValue)) {
                    pendingChange = null;
                } else {
                    pendingChange.setNewValue(newValue);
                }
            }
            Iterator children = getChildren();
            while (children.hasNext()) {
                PropertyPathNode child = (PropertyPathNode) children.next();
                Object newChild;

                if (!hasCacheableProperties(newValue)) {
                    if (newValue == DATA_NOT_READY) {
                        newChild = DATA_NOT_READY;
                    } else {
                        String[] path = child.getPathFromRoot();
                        try {
                            newChild = Generic.get(newValue, path, path.length - 1, true);
                        } catch (Exception e) {
                            log.error(e);
                            newChild = null;   //todo: maybe introduce an ERROR object?
                        }
                    }
                } else {
                    CachedProperties cachedProperties = getCachedProperties(newValue);
                    if (cachedProperties == null) {
                        //todo: dan remove this as soon as you are happy it is fixed 15-1-2004
                        log.error("dan, about to get null pointer because there are no cached properties for " + newValue + " (instance of" + newValue.getClass() + ")\n" +
                                " we wanted to get property " + child.getProperty() + "\n" +
                                "the full path was: " + getPathFromRoot());
                    }
                    newChild = cachedProperties.getPropertyValue(child.getProperty());
                }
                child.gatherNewValuesForListeners(roots, newChild);
            }
        }

        public void sendPreparedEvents(boolean mergedRoots) {
            if (isDisposed()) return;

            boolean multiplePropertiesWillChange = getChildCount() > 1;
            if (isListenerPath() && !isLeaf()) multiplePropertiesWillChange = true;
            if (pendingChange != null && pendingChange.getRoots().size() > 1) multiplePropertiesWillChange = true;

            if (multiplePropertiesWillChange) {
                // Lots of property paths have changed, collect all the info and send the whole lot as one message
                LinkedList changes = new LinkedList();
                collectEvents(changes);
                if (changes.size() == 0) {
                } else if (changes.size() == 1) {
                    PathPropertyChangeEvent event = (PathPropertyChangeEvent) changes.getFirst();
                    propertyTree.getNode(event.getPathFromRoot()).firePathPropertyChange(event);
                } else {
                    fireMultiplePropertiesChanged(!mergedRoots, changes);
                }
            } else {
                if (isListenerPath()) {
                    try {
                        if (pendingChange != null && !pendingChange.getRoots().isEmpty()) {
                            firePathPropertyChange(pendingChange);
                        }
                    } finally {
                        pendingChange = null;
                    }
                } else {
                    int childCount = getChildCount();
                    if (childCount != 0) {
                        assert (getChildCount() == 1) : "Expecting one child, but " + getChildCount() + " were found";
                        PropertyPathNode child = (PropertyPathNode) getChildren().next();
                        child.sendPreparedEvents(mergedRoots);
                    }
                }
            }
        }

        private void collectEvents(List allChanges) {
            if (isListenerPath()) {
                if (pendingChange != null && !pendingChange.getRoots().isEmpty()) {
                    allChanges.add(pendingChange);
                }
                pendingChange = null;
            }
            Iterator children = getChildren();
            while (children.hasNext()) {
                PropertyPathNode child = (PropertyPathNode) children.next();
                child.collectEvents(allChanges);
            }
        }

        private void firePathPropertyChange(PathPropertyChangeEvent event) {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                try {
                    GraphChangeListener changeListener = (GraphChangeListener) iterator.next();
                    changeListener.graphChanged(event);
                } catch (Exception e) {
                    log.error("Error dispatching PathPropertyChange", e);
                }
            }
        }

        public void removeFromParent() {
            parent.propertyToChild.remove(property);
            setDisposed(true);
        }

        public int getChildCount() {
            return propertyToChild.size();
        }

        public boolean isLeaf() {
            return propertyToChild.isEmpty();
        }

        public boolean isChildOf(PropertyPathNode possibleParent) {
            int thisDepth = getDepth();
            int possibleParentDepth = possibleParent.getDepth();
            int difference = thisDepth - possibleParentDepth;
            if (difference <= 0) return false;
            PropertyPathNode testNode = this;
            for (int i = 0; i < difference; i++) {
                testNode = testNode.getParent();
                if (testNode == possibleParent) return true;
                if (testNode == null) return false;
            }
            return false;
        }

        /**
         * return the interesting attributes that exist on the given class
         *
         * @param parentClass
         */
        public Attribute[] getChildPropertiesForClass(Class parentClass) {
            Attribute[] attributes = (Attribute[]) classToChildAttributes.get(parentClass);
            if (attributes == null) {
                Type type = Generic.getType(parentClass);
                Iterator children = getChildren();
                int i = 0;

                attributes = new Attribute [getChildCount()];
                while (children.hasNext()) {
                    PropertyPathNode node = (PropertyPathNode) children.next();
                    if (type.attributeExists(node.getProperty())) {
                        Attribute attribute = type.getAttribute(node.getProperty());

                        attributes[i++] = attribute;
                    }
                }
                if (i < attributes.length) {
                    Attribute[] shorterAttributes = new Attribute[i];
                    System.arraycopy(attributes, 0, shorterAttributes, 0, i);
                    attributes = shorterAttributes;
                }
                classToChildAttributes.put(parentClass, attributes);
            }
            return attributes;
        }
    }

    private PropertyChangeEvent currentEvent;

    protected class CachedProperties implements PropertyChangeListener {
        private Object parent;
        private Object data;
        private Object[] pathAndRoots = EMPTY_OBJECT_ARRAY;    // Pairs of a PropertyPathNode and a Set : {PropertyPathNode usagePath, Set usedByRoots, ...}

        protected CachedProperties(Type type, Object parent) {
            assert (hasCacheableProperties(parent)) : "Cannot make a cachedProperty from null or not ready parent";
//            if (log.isDebug()) log.debug("Created cached property for parent: "+parent+" property "+propertyName+" value = "+propertyValue);
            this.parent = parent;
            this.data = type.newInstance();
            Attribute[] attributes = type.getAttributes();
            for (int i = 1; i < attributes.length; i++) {
                Attribute attribute = attributes[i];
                Generic.set(data, i, UNINITIALISED);
                if (parent instanceof Observable) {
                    ((Observable) parent).addPropertyChangeListener(attribute.getName(), this);
                }
            }

        }

        private Type getType() {
            return Generic.getType(data);
        }

        private void dispose() {
            if (!isDisposed()) {
                if (parent instanceof Observable) {
                    Attribute[] attributes = getType().getAttributes();
                    for (int i = 0; i < attributes.length; i++) {
                        Attribute attribute = attributes[i];
                        if (!attribute.getName().equals("class")) {
                            try {
                                ((Observable) parent).removePropertyChangeListener(attribute.getName(), this);
                            } catch (Exception e) {
                                if (log.isDebug()) log.debug("Dan, you still have a bug. Dispose is removing listener from " + attribute.getName() + " but is already removed" + e.getMessage());
                            }
                        }
                    }
                }
                pathAndRoots = null; //just used as a "isDisposed" flag, don't add a boolean field to do this, there are rather a lot of instances!
            }
        }

        private boolean isDisposed() {
            return pathAndRoots == null;
        }

        public void propertyChange(java.beans.PropertyChangeEvent event) {
            receivePropertyChange(this, event);
        }

        protected void processPropertyChange(PropertyChangeEvent event) {
            try {
                if (currentEvent != null) {
                    //oh look, firing an event has caused another property change to the same bean!
                    //better check it is not the same property, because I have not decided how to handle this
                    assert (!currentEvent.getPropertyName().equals(event.getPropertyName())) : "I have not catered for an event listener on a property " +
                            "causing a change to exactly that property (i.e. " + event.getPropertyName() + " of bean " + getParent().getClass() + " bean ToString = " + getParent() + ")." +
                            " If you used an asynchronous cachedObjectGraph, this problem would go away.";
                }
                currentEvent = event;
                String changedPropertyName = event.getPropertyName();
                if (isDisposed() || !attributeExists(changedPropertyName)) {
                    return;
                }

                Object newValue = event.getNewValue();
                Object oldValue = isUninitialised(changedPropertyName) ? event.getOldValue() : getPropertyValue(changedPropertyName);

                if (Utilities.equals(newValue, oldValue)) {
                    //nothing to do
                    return;
                }

                if (isWithinPropertyGetter) {
                    log.warn("I wonder if this is dangerous?", new RuntimeException());
                }

                if (event instanceof LoadRequestPropertyChangeEvent && oldValue != DATA_NOT_READY) {
                    // This event is an artificial event casued by the load request. However, we seem to have already receieved a the real value for this property.
                    // just ignore this event.
                    //if (log.isDebug()) log.debug("already got");
                } else {
                    boolean differentPathsHaveDifferentRoots = false;
                    PropertyPathNode uberParentNode = null; //the closest path to the root that has a change to property event.getPropertyName()

                    // Link in new value
                    Set firstRoots = null;
                    for (int i = 0; i < pathAndRoots.length; i += 2) {
                        PropertyPathNode propertyUsagePath = ((PropertyPathNode) pathAndRoots[i]).getChild(changedPropertyName);
                        if (propertyUsagePath != null) {
                            Set roots = (Set) pathAndRoots[i + 1];

                            if (!differentPathsHaveDifferentRoots) {
                                if (firstRoots == null) {
                                    firstRoots = roots;
                                } else {
                                    if (!firstRoots.equals(roots)) {
                                        differentPathsHaveDifferentRoots = true;
                                    }
                                }
                            }
                            uberParentNode = propertyUsagePath.getCommonParent(uberParentNode);
                            differentPathsHaveDifferentRoots |= propertyUsagePath.gatherOldValuesForListeners(roots, parent, oldValue, pathAndRoots.length > 2);
                        }
                    }

                    setPropertyValue(changedPropertyName, newValue);
                    for (int i = 0; i < pathAndRoots.length; i += 2) {
                        Set roots = (Set) pathAndRoots[i + 1];
                        PropertyPathNode propertyUsagePath = ((PropertyPathNode) pathAndRoots[i]).getChild(changedPropertyName);
                        if (propertyUsagePath != null) {
                            if (!propertyUsagePath.isLeaf()) {
                                objectNoLongerUsedByRoots(oldValue, propertyUsagePath, roots);
                                objectUsedByRoots(newValue, propertyUsagePath, roots);
                            }
                            try {
                                propertyUsagePath.gatherNewValuesForListeners(roots, newValue);
                            } catch (NullPointerException e) {
                                //todo: dan remove this as soon as you are happy it is fixed 15-1-2004
                                String message = "Dan, you've got that null pointer exception again.<br>" +
                                        "the usage path was " + propertyUsagePath + " the roots were: " + roots;
                                log.error(e);
                                ApplicationDiagnostics.getInstance().sendDiagnosticReportOnlyOnce(message);
                            }
                        }
                    }

                    if (uberParentNode != null && (uberParentNode.isListenerPath() || !uberParentNode.isLeaf())) {
                        uberParentNode.sendPreparedEvents(differentPathsHaveDifferentRoots);
                    } else {
                        //todo: dan, investigate this to see if all occurances are as expected
//                    if (log.isDebug()) log.debug("Received property change for unused property: "+changedPropertyName+" of "+parent);

                    }
                }

                if (oldValue == DATA_NOT_READY) {
                    if (newValue == DATA_NOT_READY) log.warn("Dan is not expecting this, might be a bug?", new RuntimeException());
                    updatePendingPropertiesForRoots(changedPropertyName);
                }
            } finally {
                currentEvent = null;
            }
        }

        protected String[] getPropertyPaths(String childPropertyName) {
            if(pathAndRoots == null) {
                return new String[0];
            }
            List<String> result = new ArrayList<String>(pathAndRoots.length/2);
            for(int i=0; i<pathAndRoots.length; i+=2) {
                PropertyPathNode propertyUsagePath = ((PropertyPathNode) pathAndRoots[i]).getChild(childPropertyName);
                if(propertyUsagePath != null) {
                    result.add(Generic.beanPathArrayToString(propertyUsagePath.getPathFromRoot()));
                }
            }
            return result.toArray(new String[result.size()]);
        }

        protected Set<Object> getRoots() {
            Set<Object> result = new HashSet<Object>();
            if(pathAndRoots != null) {
                for(int i=1; i<pathAndRoots.length; i+=2) {
                    Set s = (Set)pathAndRoots[i];
                    result.addAll(s);
                }
            }
            return result;
        }

        private void updatePendingPropertiesForRoots(String updatedPropertyName) {
            for (int i = 0; i < pathAndRoots.length; i += 2) {
                PropertyPathNode readyPropertyPath = ((PropertyPathNode) pathAndRoots[i]).getChild(updatedPropertyName);

                Set roots = (Set) pathAndRoots[i + 1];

                for (Iterator iterator = roots.iterator(); iterator.hasNext();) {
                    Object root = iterator.next();
                    Set pending = (Set) pendingPropertiesTillReady.get(root);
                    if (pending != null) {
                        boolean removed = pending.remove(readyPropertyPath);
                        if (removed) {
                            addChildPropertiesNeedingLoad(root, readyPropertyPath);
                        }
                        if (pending.isEmpty()) {
                            pendingPropertiesTillReady.remove(root);
                            fireRowReady(root);
                        }
                    }
                }
            }
        }


        private void reTypeDataStorage(boolean add, Attribute[] changedAttributes) {
            Type currentType = getType();
            Attribute[] currentAttributes = currentType.getAttributes();
            Attribute[] unchangedAttributes;

            Attribute[] retypedAttributes;
            if (add) {
                retypedAttributes = Utilities.appendArrays(currentAttributes, changedAttributes);
                unchangedAttributes = currentAttributes;
            } else {
                retypedAttributes = (Attribute[]) Utilities.difference(currentAttributes, changedAttributes);
                unchangedAttributes = retypedAttributes;
            }

            if (retypedAttributes.length != currentAttributes.length) {
                Type newType = Generic.getType(retypedAttributes);
                Object newData = newType.newInstance();

                if (add) {
                    for (int i = 0; i < changedAttributes.length; i++) {
                        Attribute attribute = changedAttributes[i];
                        if (!currentType.attributeExists(attribute.getName())) {
                            Generic.set(newData, attribute.getName(), UNINITIALISED);
                            if (parent instanceof Observable) {
                                ((Observable) parent).addPropertyChangeListener(attribute.getName(), this);
                            }
                        }
                    }
                }
                // Copy the unchanged data values
                for (int i = 0; i < unchangedAttributes.length; i++) {
                    Attribute attribute = unchangedAttributes[i];
                    if (!attribute.getName().equals("class")) {
                        Object oldValue = Generic.get(data, i);
                        Generic.set(newData, attribute.getName(), oldValue);
                    }
                }
                data = newData;
            }
        }

        private void usedByRoots(PropertyPathNode newUsagePath, Set newUsageRoots) {
            int index = 0;
            while (index < pathAndRoots.length && pathAndRoots[index] != newUsagePath) {
                index += 2;
            }

            Set currentRoots = null;

            if (index == pathAndRoots.length) {    //did not exist
                Object[] newPathAndRoots = new Object[pathAndRoots.length + 2];
                System.arraycopy(pathAndRoots, 0, newPathAndRoots, 0, pathAndRoots.length);
                newPathAndRoots[pathAndRoots.length] = newUsagePath;
                newPathAndRoots[pathAndRoots.length + 1] = null;

                pathAndRoots = newPathAndRoots;

                //now replace the data object with one that can take the additional attributes.
                Attribute[] newAttributes = newUsagePath.getChildPropertiesForClass(parent.getClass());
                if (newAttributes.length > 0) {
                    reTypeDataStorage(true, newAttributes);
                }
            } else {
                currentRoots = (Set) pathAndRoots[index + 1];
            }

//            if (log.isDebug()) log.debug("property: "+propertyName+" of parent: "+parent+" is used by root: "+root+" in context: "+pathFromRoot);

            int newSize = currentRoots != null ? currentRoots.size() : 0;
            newSize += newUsageRoots.size();

            if (currentRoots == null) {
                if (newSize <= 1) {
                    currentRoots = new SingleItemSet();
                } else {
                    currentRoots = new HashSet(newSize);
                }
            }
            if (newSize > 1 && currentRoots instanceof SingleItemSet && !currentRoots.equals(newUsageRoots)) {
                Set oldRoots = currentRoots;
                currentRoots = new HashSet(newSize);
                currentRoots.addAll(oldRoots);
            }
            currentRoots.addAll(newUsageRoots);
            pathAndRoots[index + 1] = currentRoots;

            // Now propagate the root usage to child cached properties
            Iterator iter = newUsagePath.getChildren();
            while (iter.hasNext()) {
                PropertyPathNode node = (PropertyPathNode) iter.next();
                if (!node.isLeaf()) {
                    Object propertyValue = getPropertyValue(node.getProperty());
                    objectUsedByRoots(propertyValue, node, newUsageRoots);
                }
            }
        }

        private void noLongerUsedByRoots(PropertyPathNode oldUsagePath, Set oldUsageRoots) {
            int index = -1;
            boolean stillBeingUsed = false;
            // Find the usage path
            for (int i = 0; i < pathAndRoots.length; i += 2) {
                PropertyPathNode usagePath = (PropertyPathNode) pathAndRoots[i];
                if (usagePath == oldUsagePath) {
                    index = i;

                } else {
                    Set roots = (Set) pathAndRoots[i + 1];
                    if (!roots.isEmpty()) { //if other paths still use this node, then remember it
                        if (oldUsageRoots.equals(roots) && usagePath.isChildOf(oldUsagePath)) {
                            //if this non-empty usage is a subPath of the removalPath and the roots are the same
                            //then we know that removing the parent usage will clean up the child usages (so we do not set
                            //the "stillBeingUsed" flag.
                        } else {
                            stillBeingUsed = true;
                        }
                    }
                }
            }
            if (index >= 0) {
                Iterator iter = oldUsagePath.getChildren();
                while (iter.hasNext()) {
                    PropertyPathNode propertyNode = (PropertyPathNode) iter.next();
                    String property = propertyNode.getProperty();
                    Object propertyValue = getPropertyValue(property);
//                    if (propertyValue != getParent()) { //check that property value is not self referrential
                    CachedProperties childCachedProperties = getCachedProperties(propertyValue);
                    if (childCachedProperties != null) {
                        childCachedProperties.noLongerUsedByRoots(propertyNode, oldUsageRoots);
                    }
//                    }
                    //TODO: if all of my other usage paths do not have "property" as a child, then we can clean up storage for "property"
                }

                Set updatedUsageRoots = (Set) pathAndRoots[index + 1];
                if (updatedUsageRoots == oldUsageRoots) {
                    updatedUsageRoots.clear();
                } else {
                    updatedUsageRoots.removeAll(oldUsageRoots);
                }
                if (updatedUsageRoots.isEmpty()) {
                    // All properties under the oldUsagePath are now potentially unused
                    if (!stillBeingUsed) {
                        Object parent = getParent();
                        referenceGraph.remove(parent);
                        if ( ++removals >= 10 ) {
                            removals = 0;
                            //this is done whenever a few removals have taken place, because the current hash map may otherwise end up with an unnecessarily
                            //large internal array, which can lead to a memory leak
                            referenceGraph = new IdentityHashMap(referenceGraph);
                        }
                        dispose();
                    } else {
                        //TODO: Clean up my storage for this property
                        //reTypeDataStorage(false, removedAttributes);
                    }
                }
            }
        }

        /**
         * this handles removing redundant entries from pathAndRoots
         * At the moment I don't bother, is it worth it? Dunno. Need to profile + see
         */
        private void cleanUpUnusedStorage() {
            int removedCount = 0;
            for (int i = 0; i < pathAndRoots.length; i += 2) {
                Set roots = (Set) pathAndRoots[i + 1];
                if (roots == null || roots.isEmpty()) {
                    pathAndRoots[i] = null;
                    removedCount += 2;
                }
            }
            if (removedCount == 0) {
                return;
            } else if (removedCount == pathAndRoots.length) {
                log.error("I don't think this should happen. It might indicate that the objectNoLongerUsedByRoots mechaism is broken?", new RuntimeException());
            } else {
                Object[] newPathAndRoots = new Object[pathAndRoots.length - removedCount];
                int next = 0;
                for (int i = 0; i < pathAndRoots.length; i += 2) {
                    if (pathAndRoots[i] != null) {
                        newPathAndRoots[next++] = pathAndRoots[i];
                        newPathAndRoots[next++] = pathAndRoots[i + 1];
                    }
                }
                pathAndRoots = newPathAndRoots;
                //todo: rebuildDataObject
                //todo: propagate the cleanup down all subproperties?
            }
        }

        private boolean attributeExists(String propertyName) {
            return Generic.getType(data).attributeExists(propertyName);
        }

        protected boolean isUninitialised(String propertyName) {
            int attributeIndex = Generic.getType(data).getAttributeIndex(propertyName);
            Object propertyValue = Generic.get(data, attributeIndex);
            return propertyValue == UNINITIALISED;
        }

        public Object getPropertyValue(String propertyName) {
            Type type = Generic.getType(data);
            if (!type.attributeExists(propertyName)) return null;

            int attributeIndex = type.getAttributeIndex(propertyName);
            Object propertyValue = Generic.get(data, attributeIndex);
            if (propertyValue == UNINITIALISED) {
                if (isAsynchronous()) {
                    propertyValue = DATA_NOT_READY;
                    propertyLoadQueue.addLoadRequest(parent, new String[]{propertyName});
                } else {
                    propertyValue = getPropertyValueFromBean(propertyName);
                }
                Generic.set(data, attributeIndex, propertyValue);
            }
            return propertyValue;
        }

        private Object getPropertyValueFromBean(String propertyName) {
            Object propertyValue;
            try {
                //sometimes people call the setter from within a getter. They should be spanked for this; in the meantime I'm going to unhook the listener before calling the getter.
                if (parent instanceof Observable) {
                    ((Observable) parent).removePropertyChangeListener(propertyName, this);
                }

                isWithinPropertyGetter = true;
                propertyValue = Generic.get(parent, new String[]{propertyName}, 0, true);
                isWithinPropertyGetter = false;

                if (parent instanceof Observable) {
                    ((Observable) parent).addPropertyChangeListener(propertyName, this);
                }
            } catch (Exception e) {
                propertyValue = null;
                log.error("Got exception calling get " + propertyName + " on object: " + parent, e);
            }

            return propertyValue;
        }


        private void setPropertyValue(String propertyName, Object propertyValue) {
            Generic.set(data, propertyName, propertyValue);
        }

        public Object getParent() {
            return parent;
        }

        /**
         * informs the cachedProperties that the given roots no longer use it for the given property (prunedNode)
         * if no one else is using the given property, this will cause a listener unhook, storage cleanup and re-typing
         *
         * @param parentNode
         * @param prunedNode
         * @param oldUsageRoots
         */
        private void propertyNoLongerUsedByRoots(PropertyPathNode parentNode, PropertyPathNode prunedNode, Set oldUsageRoots) {
            String property = prunedNode.getProperty();
            if (!attributeExists(property)) return;

            boolean deleteProperty = true;
            for (int i = 0; i < pathAndRoots.length; i += 2) {
                PropertyPathNode usagePath = (PropertyPathNode) pathAndRoots[i];
                Set roots = (Set) pathAndRoots[i + 1];
                if (usagePath == parentNode) {
                    //if
                    roots.removeAll(oldUsageRoots);
                    if (!roots.isEmpty()) {
                        deleteProperty = false;
                        break;
                    }
                } else {
                    if (usagePath.getChild(property) != null) {//check to see if property is used in a different context
                        deleteProperty = false;
                        break;
                    }
                }
            }
            //clean up my usage records for the cachedProperties instance for this property
            Object propertyValue = getPropertyValue(property);
            CachedProperties unusedCachedProperties = getCachedProperties(propertyValue);
            if (unusedCachedProperties != null) {
                unusedCachedProperties.noLongerUsedByRoots(prunedNode, oldUsageRoots);
            }

            if (deleteProperty) {
                //clean up my listener
                Object parent = getParent();
                if (parent instanceof Observable) {
                    Observable observable = (Observable) parent;
                    observable.removePropertyChangeListener(property, this);
                }

                //now clean up my storage for this property
                Attribute removedAttribute = getType().getAttribute(property);
                reTypeDataStorage(false, new Attribute[]{removedAttribute});
            }
        }
    }

    private static class PropertyLoadRequest {
        private Object parent;
        private String[] propertyPath;

        public PropertyLoadRequest(Object parent, String[] propertyPath) {
            this.parent = parent;
            this.propertyPath = propertyPath;
        }

        public String toString() {
            return "load request: " + Generic.beanPathArrayToString(propertyPath, false) + " on " + parent;
        }
    }

    private class PropertyLoadQueue implements Runnable {
        private LinkedList loadRequests = new LinkedList();
        private Object changeLock = new Object();

        public PropertyLoadQueue() {
        }

        public void addLoadRequest(Object parent, String[] propertyName) {
            PropertyLoadRequest loadRequest = new PropertyLoadRequest(parent, propertyName);
//            if (log.isDebug()) log.debug("adding request "+loadRequest);
            synchronized (changeLock) {
                loadRequests.add(loadRequest);
                changeLock.notify();
            }
        }

        public void clear() {
            synchronized (changeLock) {
//                if (log.isDebug()) log.debug("Clearing request queue");
                loadRequests.clear();
            }
        }

        public void run() {
            Object[] requests = null;
            while (!disposed) {
                if (executionController != null) {
                    executionController.waitForExecutionSignal();
                }

                synchronized (changeLock) {
                    requests = this.loadRequests.toArray();
                    this.loadRequests.clear();
                }

                for (int i = 0; i < requests.length; i++) {
                    PropertyLoadRequest request = (PropertyLoadRequest) requests[i];
                    loadProperty(request.parent, request.propertyPath);
                    if (i % 10 == 0) Thread.yield();
                }
                requests = null;

                synchronized (changeLock) {
                    if (this.loadRequests.size() == 0) {
                        try {
                            if (executionController != null) executionController.setWaiting(true);
                            changeLock.wait();
                        } catch (InterruptedException e) {
                        } finally{
                            if (executionController != null) executionController.setWaiting(false);
                        }
                    }
                }
            }
        }

        private void loadProperty(Object parent, String[] propertyPath) {
            try {
                Object value = Generic.get(parent, propertyPath, 0, 1, true);
                String propertyName = propertyPath[0];
                PropertyChangeEvent event = new LoadRequestPropertyChangeEvent(parent, propertyName, DATA_NOT_READY, value);

                synchronized (getChangeLock()) {
                    CachedProperties cachedProperties = getCachedProperties(parent);
                    // Although we have loaded the property of parent, we may no longer need it (i.e. it might have been
                    // removed from the graph, therefore cachedProperty could be null!
                    if (cachedProperties != null) {
                        receivePropertyChange(cachedProperties, event);
                    }
                }
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public static class NotReadyAwareComparator implements Comparator {
        private Comparator comparator;

        public NotReadyAwareComparator() {
            this(Utilities.COMPARATOR);
        }

        public NotReadyAwareComparator(Comparator comparator) {
            this.comparator = comparator;
        }

        public int compare(Object o1, Object o2) {
            if (o1 == CachedObjectGraph.DATA_NOT_READY) return -1;
            if (o2 == CachedObjectGraph.DATA_NOT_READY) return 1;
            return comparator.compare(o1, o2);
        }
    }

    public static class LoadRequestPropertyChangeEvent extends NullSourcePropertyChangeEvent {
        public LoadRequestPropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
            super(source, propertyName, oldValue, newValue);
        }
    }

    public interface ExecutionController {
        void setWaiting(boolean waiting);
        void setUserPriority(boolean userPriority);
        void waitForExecutionSignal();
    }

    public interface RowReadyListener {
        public void rowReady(Object root);
    }

    private static class ChangeLock {
    };
}
