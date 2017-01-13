package org.bhavaya.collection;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.table.KeyedColumnTableModel;
import org.bhavaya.util.Utilities;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Quantity;
import org.bhavaya.util.Attribute;
import org.bhavaya.beans.BeanFactory;

import java.util.*;

/**
 * A class useful when testing production configuration of bean collections and table models (sometimes chained).
 * Allows to simulate data changes without having to wait for events comming from notification services.
 * <p>
 * Pass in beanCollection containing data beans and table model defining what is visible. This class by default modifies
 * only visible properties. Additional properties can be added or removed by addPathToModify and ignore methods.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class RandomBeanCollectionModifier {

    protected final String[] DEFAULT_STRINGS = new String[]{"USD", "EUR", "GBP", "YEN", "FOO"};

    private BeanCollection beanCollection;
    private KeyedColumnTableModel tableModel;
    private Random random;
    private Set ignoreSet = new HashSet();
    private Set alsoModify = new HashSet();
    private int modificationLoops = 500;
    private int delayBetweenModifications = 0;
    /** Pool of strings to be used by getRandom method to generate string. */
    private String[] strings = DEFAULT_STRINGS;

    public RandomBeanCollectionModifier(BeanCollection beanCollection, KeyedColumnTableModel tableModel) {
        this.beanCollection = beanCollection;
        this.tableModel = tableModel;
    }

    /**
     * Do not modify this property. Useful when modification of the property causes error e.g. is illegal.
     * @param columnKey
     */
    public void ignore(Object columnKey) {
        ignoreSet.add(columnKey);
    }

    /**
     * By default this class modifies only properties displayed in table (those returned by table model).
     * This method adds not visible property to the modification list.
     * Its useful when displayed data change based on not displayed data.
     * @param propertyPath
     */
    public void addPathToModify(String propertyPath) {
        alsoModify.add(propertyPath);
    }

    public void setModificationLoops(int modificationLoops) {
        this.modificationLoops = modificationLoops;
    }

    public void setDelayBetweenModifications(int delayBetweenModifications) {
        this.delayBetweenModifications = delayBetweenModifications;
    }

    /**
     * Set the string loop used by getRandom method to generate string.
     * @param strings
     */
    public void setStrings(String[] strings) {
        this.strings = strings;
    }

    /**
     * Runs data modifications in new Thread.
     */
    public void start() {
        Thread t = new Thread() {
            public void run() {
                runInternal();
            }
        };
        t.start();
    }

    protected void runInternal() {
        random = new Random(); // new random on every run to get more random data :-)
        int columnCount = tableModel.getColumnCount();
        List propertiesToModify = new ArrayList();
        for (int i = 0; i < columnCount; i++) {
            Object key = tableModel.getColumnKey(i);
            propertiesToModify.add(key);
        }
        propertiesToModify.removeAll(ignoreSet);
        propertiesToModify.addAll(alsoModify);

        for (int i = 0; i < modificationLoops; i++) {

            int propertyToUpdate = random.nextInt(propertiesToModify.size());
            String property = (String) propertiesToModify.get(propertyToUpdate);
            int rowToModify = random.nextInt(beanCollection.size());
            Object objectToModify = beanCollection.get(rowToModify);

            Attribute settableAttribute = findSetter(property, objectToModify);
            if (settableAttribute == null) continue; // no way to update the attribute, skip
            Class dataType = settableAttribute.getType();
            String beanPath = settableAttribute.getName();

            // feel free to add any other type of modification
            int choice = random.nextInt(2);
            switch (choice) {
                case 0:
                    Object val = getRandom(dataType);
                    try {
                        Generic.set(objectToModify, Generic.beanPathStringToArray(beanPath), val, false);
                    } catch (Throwable t) {
                        System.out.println("Error setting property: " + beanPath);
                        t.printStackTrace();
                    }
                    break;
                case 1:
                    beanCollection.remove(random.nextInt(beanCollection.size()));
                    break;
            }

            sleep(delayBetweenModifications);
        }

    }

    /**
     * Tries to find a setter method in the property path which can be used to modify the displayed data.
     */
    private Attribute findSetter(String propertyPath, Object object) {
        String[] beanPathArray = Generic.beanPathStringToArray(propertyPath);
        while (beanPathArray.length > 0) {
            Attribute attribute = Generic.getAttribute(object, beanPathArray);
            if (attribute.isWritable()) return attribute;
            String[] newPathArray = new String[beanPathArray.length - 1];
            for (int i = 0; i < newPathArray.length; i++) {
                newPathArray[i] = beanPathArray[i];
            }
            beanPathArray = newPathArray;
        }
        return null;
    }

    public Object getRandom(Class type) {
        if (Number.class.isAssignableFrom(type)) {
            Number number = new Double(random.nextInt() + random.nextDouble());
            Object ret = Utilities.changeType(type, number);
            return ret;
        } else if (type == String.class) {
            return strings[random.nextInt(strings.length)];
        } else if (type == Quantity.class) {
            Quantity ret = new Quantity(random.nextInt(2000000), "EUR");
            return ret;
        } else if (type == Date.class) {
            return new Date(System.currentTimeMillis() + (long) (random.nextDouble() * 1000 * 60 * 60 * 24 * 365 * 20));  //a date from now to 20yrs time
        } else {
            // try to find bean collection for the type and change value to any bean of the collection
            BeanFactory beanFactory = null;
            try {
                beanFactory = BeanFactory.getInstance(type);
            } catch (Throwable t) {
            }
            if (beanFactory != null) {
                synchronized (beanFactory.getLock()) {
                    if (beanFactory.size() == 0) return null;
                    int idx = random.nextInt(beanFactory.size());
                    Collection values = beanFactory.values();
                    Iterator it = values.iterator();
                    for (int i = 0; i < idx; i++) it.next();
                    Object bean = it.next();
                    return bean;
                }
            }
        }
        return null;
    }

    private void sleep(long millis) {
        if (millis <= 0)  return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}
