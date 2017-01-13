package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.GraphChangeListener;
import org.bhavaya.ui.table.PathPropertyChangeEvent;
import org.bhavaya.ui.table.QueuedCachedObjectGraph;
import org.bhavaya.util.*;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Queued cached object graph that deals with formula
 * User: Jon Moore
 * Date: 17/01/11
 * Time: 14:57
 */
public class FormulaEnabledObjectGraph extends QueuedCachedObjectGraph {

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = NamedExecutors.newScheduledThreadPool(
            "FormulaUpdateExecutor",
            FormulaUtils.getFormulaRecalcThreadPoolSize()
    );
    private static final Log log = Log.getCategory(FormulaEnabledObjectGraph.class);

    private FormulaManager formulaManager = new FormulaManager();

    private UpdatedSymbolCache symbolsToUpdate = new ConcurrentMapUpdatedSymbolCache();

    private ScheduledFuture<?> future = null;

    private Map<String, Set<GraphChangeListener>> listenerMap = new HashMap<String, Set<GraphChangeListener>>();

    public FormulaEnabledObjectGraph(Class beanType, boolean asynchronous, String name,
                                     BeanCollectionTableModel beanCollectionTableModel) {
        super(beanType, asynchronous, name, beanCollectionTableModel);
        if(FormulaUtils.formulasEnabled()) {
            int updateInterval = FormulaUtils.getFormulaRecalcInterval();
            //start a timer to periodically update the cache with calculated formula values
            future = SCHEDULED_EXECUTOR.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    updateCache();
                }
            }, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if(future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @Override
    public Object get(Object root, String[] pathComponents) {
        if(FormulaUtils.formulasEnabled() && FormulaUtils.isFormulaPath(pathComponents)) {
            //Get the formula for the path and retrieve the formula value for that object
            String formulaName = FormulaUtils.getFormulaNameFromLocator(pathComponents);
            Formula formula = formulaManager.getFormulaByName(formulaName);
            if(formula != null) {
                FormulaCachedProperties cachedProperties = (FormulaCachedProperties) getCachedProperties(root);
                return cachedProperties.getFormulaValue(formula);
            }
            return null;
        }
        //not a formula so defer to superclass
        return super.get(root, pathComponents);
    }

    public FormulaManager getFormulaManager() {
        return formulaManager;
    }

    public void setFormulaManager(FormulaManager formulaManager) {
        FormulaManager oldFormulaManager = this.formulaManager;
        this.formulaManager = formulaManager == null ? new FormulaManager() : formulaManager;
        initCache(oldFormulaManager);
    }

    @Override
    public void addRootObject(Object obj) {
        super.addRootObject(obj);
        if(FormulaUtils.formulasEnabled()) {
            //initialise formula value for this object
            for(Formula formula : formulaManager.getAllFormulas()) {
                recalculateFormulaForObject(formula, obj);
            }
        }
    }

    private void initCache(FormulaManager oldFormulaManager)  {
        if(!FormulaUtils.formulasEnabled()) {
            return;
        }
        List<Formula> oldFormulas = oldFormulaManager == null ? Collections.<Formula>emptyList() : oldFormulaManager.getAllFormulas();
        List<Formula> newFormulas = formulaManager.getAllFormulas();
        Set<Formula> toIgnore = new HashSet<Formula>();
        for(Formula oldFormula : oldFormulas) {
            if(oldFormula.isEnabled()) {
                int index = formulaManager.getAllFormulas().indexOf(oldFormula);
                if(index < 0) {
                    //formula has been removed
                    beanCollectionTableModel.removeColumnLocator(FormulaUtils.getColumnLocator(oldFormula));
                } else {
                    Formula newFormula = newFormulas.get(index);
                    if(!newFormula.isEnabled()) {
                        //formula has been disabled
                        beanCollectionTableModel.removeColumnLocator(FormulaUtils.getColumnLocator(oldFormula));
                    } else  {
                        if(!Utilities.equals(newFormula.getName(), oldFormula.getName())) {
                            //formula has had a name change
                            beanCollectionTableModel.changeColumnLocator(FormulaUtils.getColumnLocator(oldFormula),
                                    FormulaUtils.getColumnLocator(newFormula));
                        }
                        if(Utilities.equals(newFormula.getExpression(), oldFormula.getExpression())) {
                            //expression is the same so no need to recalc
                            toIgnore.add(newFormula);
                        }
                    }
                }
            }
        }

        for(Formula newFormula : newFormulas) {
            if(newFormula.isEnabled() && !toIgnore.contains(newFormula)) {
                String locator = FormulaUtils.getColumnLocator(newFormula);
                if(!beanCollectionTableModel.isColumnVisible(locator)) {
                    //new column has been added
                    beanCollectionTableModel.addColumnLocator(locator);
                }
                //update this formulas values for all objects
                Iterator objects = getRootObjects();
                while(objects.hasNext()) {
                    Object root = objects.next();
                    recalculateFormulaForObject(newFormula, root);
                }
            }
        }
    }

    private void updateCache()  {
        try {
            List<Formula> formulas = formulaManager.getAllFormulas();
            if(!formulas.isEmpty()) {
                long start = System.currentTimeMillis();
                int updateCount = 0;
                //These are the symbol values that have been updated by object
                Map<Object, Set<String>> updatedSymbolsByObject = symbolsToUpdate.getUpdated();

                for(Object root : updatedSymbolsByObject.keySet()) {
                    Set<String> symbolsChangedForRoot = updatedSymbolsByObject.get(root);
                    for(Formula formula : formulas) {
                        if(formulaDependsOnSymbols(formula, symbolsChangedForRoot)) {
                            //At least one value that this formula depends on has changed for this object, so recalculate
                            recalculateFormulaForObject(formula, root);
                            updateCount++;
                        }
                    }
                }
                long time = System.currentTimeMillis()-start;
                FormulaMonitor monitor = FormulaUtils.getFormulaMonitorInstance();
                if(monitor != null) {
                    monitor.monitorFormulas(updateCount, time);
                }
            }
        } catch (Throwable e) {
            log.warn("Exception thrown updating formula cache", e);
        }
    }

    private boolean formulaDependsOnSymbols(Formula formula, Set<String> symbols) {
        //does this formula depend on any of these symbols?
        for(String symbol : symbols) {
            if(formula.dependsOnSymbol(symbol)) {
                return true;
            }
        }
        return false;
    }

    private void recalculateFormulaForObject(Formula formula, Object obj) {
        try {
            //copy this formula to be safe
            Formula formulaCopy = formula.copy();
            boolean dataReady = true;
            for(String symbol : formulaCopy.getSymbols()) {
                String locator = formulaManager.getBeanPathForSymbol(symbol);
                Object value = Generic.getBeanValueIfExists(obj, locator);
                if(value == null) {
                    try {
                        Attribute attribute = Generic.getAttribute(obj, Generic.beanPathStringToArray(locator), true);
                        formulaCopy.setEmptySymbolValue(symbol, attribute.getType());
                    } catch (Exception e) {
                        log.error("Exception setting empty symbol value - ignoring", e);
                        dataReady = false;
                        break;
                    }                } else if (value != DATA_NOT_READY) {
                    formulaCopy.setSymbolValue(symbol, value);
                } else {
                    dataReady = false;
                    break;
                }
            }
            if(dataReady) {
                //update the new calculate value
                Object newValue = formulaCopy.evaluate();
                FormulaCachedProperties cachedProperties = (FormulaCachedProperties) getCachedProperties(obj);
                Object oldValue = cachedProperties.getFormulaValue(formulaCopy);
                if(!Utilities.equals(oldValue, newValue)) {
                    //Just fire change event - processPropertyChange will actually do the update, and ensure in correct thread
                    cachedProperties.propertyChange(new PropertyChangeEvent(obj, FormulaUtils.getColumnLocator(formula), oldValue, newValue));
                }
            }
        } catch (FormulaException e) {
           throw new RuntimeException("Error evaluating formula", e);
        }
    }

    protected CachedProperties createCachedProperties(Type type, Object parent) {
        return new FormulaCachedProperties (type, parent);
    }

    @Override
    public void addPathListener(String propertyPath, GraphChangeListener listener) {
        if(FormulaUtils.isFormulaPath(propertyPath)) {
            Set<GraphChangeListener> listeners = listenerMap.get(propertyPath);
            if(listeners == null) {
                listeners = new HashSet<GraphChangeListener>(1);
                listenerMap.put(propertyPath, listeners);
            }
            listeners.add(listener);
            //enable the formula
            String formulaName = FormulaUtils.getFormulaNameFromLocator(propertyPath);
            Formula formula = formulaManager.getFormulaByName(formulaName);
            if(formula != null) {
                formula.setEnabled(true);
                //add path listeners for dependents
                for(String symbol : formula.getSymbols()) {
                    super.addPathListener(formulaManager.getBeanPathForSymbol(symbol), listener);
                }
            }
        } else {
            super.addPathListener(propertyPath, listener);
        }
    }

    @Override
    public void removePathListener(String propertyPath, GraphChangeListener listener) {
        if(FormulaUtils.isFormulaPath(propertyPath)) {
            Set<GraphChangeListener> listeners = listenerMap.get(propertyPath);
            if(listeners != null) {
                listeners.remove(listener);
            }
            //disable the formula (this may be called from the popup menu on the table header, so we need to make sure the state is consistent)
            String formulaName = FormulaUtils.getFormulaNameFromLocator(propertyPath);
            Formula formula = formulaManager.getFormulaByName(formulaName);
            if(formula != null) {
                formula.setEnabled(false);
                //remove path listeners for dependents
                for(String symbol : formula.getSymbols()) {
                    super.removePathListener(formulaManager.getBeanPathForSymbol(symbol), listener);
                }
            }
        } else {
            super.removePathListener(propertyPath, listener);
        }
    }

    private void firePathPropertyChanged(String propertyPath, PathPropertyChangeEvent event) {
        Set<GraphChangeListener> listeners = listenerMap.get(propertyPath);
        if(listeners != null) {
            for(GraphChangeListener listener : listeners) {
                listener.graphChanged(event);
            }
        }
    }

    /**
     * Extension of CachedProperties that deals with caching calculated formula values.
     * This class listens for updates to symbols and adds them to the UpdatedSymbolCache, which is checked periodically
     * and formula values recalculated as necessary
     */
    protected class FormulaCachedProperties extends CachedProperties {

        private Map<Formula, Object> formulaProperties = new HashMap<Formula, Object>();

        private FormulaCachedProperties(Type type, Object parent) {
            super(type, parent);
        }

        @Override
        protected void processPropertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();
            if(FormulaUtils.formulasEnabled() && FormulaUtils.isFormulaPath(propertyName)) {
                Formula formula = formulaManager.getFormulaByName(FormulaUtils.getFormulaNameFromLocator(propertyName));
                if(formula != null) {
                    //set the value
                    setFormulaValue(formula, event.getNewValue());
                    //A calculated formula value has changed to fire a PathPropertyChangeEvent (this will notify the table)
                    FormulaPropertyChangeEvent ppce = new FormulaPropertyChangeEvent(getParent(),
                            propertyName, event.getOldValue(), event.getNewValue());
                    firePathPropertyChanged(propertyName, ppce);
                }
            } else {
                super.processPropertyChange(event);
                if(FormulaUtils.formulasEnabled()) {
                    //Check if this update applies to one of our formula symbols, and add to the UpdatedSymbolCache
                    if(formulaManager != null && formulaManager.hasFormulas()) {
                        String[] propertyPaths = getPropertyPaths(propertyName);
                        if(propertyPaths.length > 0) {
                            Set<String> updatedSymbols = new HashSet<String>(propertyPaths.length);
                            for(String propertyPath : propertyPaths) {
                                updatedSymbols.addAll(formulaManager.getSymbolForBeanPath(propertyPath));
                            }
                            Set<Object> roots = getRoots();
                            for(Object root : roots) {
                                symbolsToUpdate.addUpdated(root, updatedSymbols);
                            }
                        }
                    }
                }
            }
        }

        private void setFormulaValue(Formula formula, Object value) {
            formulaProperties.put(formula, value);
        }

        private Object getFormulaValue(Formula formula) {
            return formulaProperties.get(formula);
        }
    }
}
