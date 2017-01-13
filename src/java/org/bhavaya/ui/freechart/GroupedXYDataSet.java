package org.bhavaya.ui.freechart;

import org.bhavaya.beans.Bean;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.util.*;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;

import java.util.*;

/**
 * An XYDataSet based on a BeanCollection, which exposes unique combinations
 * of bean properties as different groups of data.  This allows several properties
 * to be used together as a grouping key (which maps to a series for the Dataset),
 * with other properties used to map to the x and y axes of each series.
 * As an example, the table below represents bean properties we're interested in:<br />
 * <code><pre>
 * X    Y     Shape     Colour
 * ---------------------------
 * 10   12    Circle    Red
 * 56   78    Square    Blue
 * 123  45    Circle    Red
 * 95   134   Circle    Blue
 * 48   345   Square    Blue
 * </pre></code>
 * <p/>
 * If sorted on the Shape and Colour properties, would be presented as 3 series of data.
 * Each series would have a name based on the unique value in the combination of sorting
 * property values (i.e. "Circle Red", "Circle Blue" or "Square Blue").  Each series would
 * have its x and y (X and Y in this instance).
 * <p/>
 * Would be represented as 3 separate groups of data.
 *
 * @author James Langley
 * @version $Revision: 1.4 $
 */
public class GroupedXYDataSet implements XYDataset {
    private static final Log log = Log.getCategory(GroupedXYDataSet.class);

    private HashMap groups = new LinkedHashMap();
    private BeanCollection beanCollection;
    private Collection columnLocators;
    private ArrayList numericLocators = new ArrayList();
    private ArrayList nonNumericLocators = new ArrayList();
    private HashSet listeners = new HashSet();
    private DatasetGroup datasetGroup;
    private boolean hasHorizontalDateAxis = false;
    private int indexOfXLocator = -1;
    private int indexOfYLocator = -1;
    private boolean xAxisIsDateAxis = false;

    /**
     * Creates a GroupedXYDataSet based on the supplied TableModel.
     * All non-numeric columns will be used for sorting.  For the purposes of
     * this class, Numbers, Quantities, ScalableNumbers and Dates (java.util and
     * java.sql) are treated as numeric.  All other types are treated as Strings.
     */
    public GroupedXYDataSet(BeanCollection collection) {
        this.beanCollection = collection;
    }

    /**
     * Initialises the list of groups.  Only needs to be called once to set up the initial
     * groups.
     */
    private void init() {
        //First work out which column locators should be used for x,y values and which for grouping
        //TODO: Handle the cases where the column locators don't contain any numeric information
        //TODO: Or there aren't enough columns etc.
        groups.clear();
        numericLocators.clear();
        nonNumericLocators.clear();
        indexOfXLocator = 0;
        indexOfYLocator = -1;
        xAxisIsDateAxis = false;
        Class type = beanCollection.getType();
        for (Iterator iterator = columnLocators.iterator(); iterator.hasNext();) {
            String locator = (String) iterator.next();
            Class columnClass = PropertyModel.getInstance(type).getAttribute(Generic.beanPathStringToArray(locator)).getType();
            columnClass = displayableType(columnClass);
            if (isNumeric(columnClass)) {
                numericLocators.add(locator);
            } else if (isDate(columnClass)) {
                indexOfXLocator = numericLocators.size();
                xAxisIsDateAxis = true;
                numericLocators.add(locator);
            } else {
                nonNumericLocators.add(locator);
            }
        }
        //If we have a date locator, then use the first available other numeric locator as the Y axis
        indexOfYLocator = indexOfXLocator > 0 ? 0 : 1;

        //Add the beans to their appropriate bins, ditching beans that contain invalid numbers.
        //TODO: Add listeners using cached object graph to update the data.
        for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
            Bean bean = (Bean) iterator.next();
            String groupValue = getGroupName(bean);
            Number xValue = getNumberForObjectUnchecked(Generic.get(bean, Generic.beanPathStringToArray((String) numericLocators.get(indexOfXLocator)), 0, true));
            Number yValue = getNumberForObjectUnchecked(Generic.get(bean, Generic.beanPathStringToArray((String) numericLocators.get(indexOfYLocator)), 0, true));
            if ((xValue instanceof Double && !isLegalXValue((Double) xValue)) ||
                    (yValue instanceof Double && !isLegalValue((Double) yValue))) {
                log.warn("Dropped an illegal double value from the dataset");
                continue;
            }
            ArrayList list = (ArrayList) groups.get(groupValue);
            if (list == null) {
                list = new ArrayList();
                groups.put(groupValue, list);
            }
            list.add(bean);
        }
    }

    private Class displayableType(Class c) {
        if (c == null || c.isInterface()) {
            return Object.class;
        }
        return ClassUtilities.typeToClass(c);
    }

    private String getGroupName(Bean bean) {
        StringBuffer buf = new StringBuffer();
        for (Iterator iterator = nonNumericLocators.iterator(); iterator.hasNext();) {
            buf.append(Generic.get(bean, Generic.beanPathStringToArray((String) iterator.next()), 0, true));
            if (iterator.hasNext()) {
                buf.append(" ");
            }
        }
        return buf.toString();
    }

    /**
     * Gets the name of the locator being used for the X Axis
     *
     * @return the locator.
     */
    public String getXAxisLocator() {
        return (String) numericLocators.get(indexOfXLocator);
    }

    /**
     * Returns whether the X axis represents dates or not.
     *
     * @return <code>true</code> if the X axis values are dates, <code>false</code> otherwise.
     */
    public boolean isXAxisDateAxis() {
        return xAxisIsDateAxis;
    }

    /**
     * Adds the names of the properties to be used for charting.
     *
     * @param locators a collection of bean property paths.
     */
    public void setColumnLocators(Collection locators) {
        this.columnLocators = locators;
        init();
    }

    /**
     * Tests whether an object of the given class can be interpreted as as numeric value.
     *
     * @param c the class to test.
     * @return <code>true</code> if the class can be treated as numeric, <code>false</code> otherwise.
     */
    private boolean isNumeric(Class c) {
        return Number.class.isAssignableFrom(c) || Numeric.class.isAssignableFrom(c);
    }

    /**
     * Tests whether and object of the given class can be interpreted as a Date - either
     * java.sql.Date or java.lang.Date.
     *
     * @param c the class to test.
     * @return <code>true</code> if the class can be treated as a date, <code>false</code> otherwise.
     */
    private boolean isDate(Class c) {
        return java.util.Date.class.isAssignableFrom(c) || java.sql.Date.class.isAssignableFrom(c);
    }

    public DomainOrder getDomainOrder() {
        return DomainOrder.NONE;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getItemCount(int series) {
        assert series < groups.size();
        return ((ArrayList) groups.get(getSeriesName(series))).size();
    }

    public double getXValue(int series, int item) {
        Number number = getX(series, item);
        return number == null ? 0.0d : number.doubleValue();
    }

    public Number getX(int series, int item) {
        Bean bean = ((Bean) ((ArrayList) groups.get(getSeriesName(series))).get(item));
        return getNumberForObject(Generic.get(bean, Generic.beanPathStringToArray((String) numericLocators.get(indexOfXLocator)), 0, true));
    }

    private Number getNumberForObject(Object o) {
        Number n = getNumberForObjectUnchecked(o);
        /*if (n instanceof Double && !isLegalValue((Double)o)) {
            log.error("Whoops, NaN got through into the dataset");
            return new Double(0.0);
        }*/
        return n;
    }

    private boolean isLegalValue(Double d) {
        return !(d == null || d.isNaN() || d.isInfinite());
    }

    private boolean isLegalXValue(Double d) {
        boolean isLegal = isLegalValue(d);
        if (isLegal && isXAxisDateAxis()) {
            return d.doubleValue() > 0;
        }
        return isLegal;
    }

    private Number getNumberForObjectUnchecked(Object o) {
        if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof java.sql.Date) {
            java.sql.Date date = (java.sql.Date) o;
            return new Long(date.getTime());
        } else if (o instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) o;
            return new Long(date.getTime());
        }
        return new Double(0.0);
    }

    public double getYValue(int series, int item) {
        Number number = getY(series, item);
        return number == null ? 0.0d : number.doubleValue();
    }

    public Number getY(int series, int item) {
        Bean bean = ((Bean) ((ArrayList) groups.get(getSeriesName(series))).get(item));
        Number value = getNumberForObject(Generic.get(bean, Generic.beanPathStringToArray((String) numericLocators.get(indexOfYLocator)), 0, true));
        return value;
    }

    public int getSeriesCount() {
        return groups.size();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Comparable getSeriesKey(int series) {
       return getSeriesName(series);
    }

    public int indexOf(Comparable seriesKey) {
       int index = 0;
       for ( Object o : groups.keySet() ) {
          if ( o == seriesKey || ( o != null && o.equals(seriesKey))) {
              break;
          }
          index++;
       }
       //return the index unless we didn't find the series key, in which case return -1
       return index < getSeriesCount() ? index : -1;
    }

   
    public String getSeriesName(int series) {
        assert series < groups.size();
        Set set = groups.keySet();
        Iterator iterator = set.iterator();
        int i = 0;
        String name;
        do {
            name = (String) iterator.next();
        } while (i++ < series);
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addChangeListener(DatasetChangeListener datasetChangeListener) {
        listeners.add(datasetChangeListener);
    }

    public void removeChangeListener(DatasetChangeListener datasetChangeListener) {
        listeners.remove(datasetChangeListener);
    }

    public DatasetGroup getGroup() {
        return datasetGroup;
    }

    public void setGroup(DatasetGroup datasetGroup) {
        this.datasetGroup = datasetGroup;
    }

    /**
     * If the selected bean properties contain a date property, then this will
     * be used as the X axis.  This function tells you whether that's the case.
     *
     * @return <code>true</code> if the X axis is a date axis, <code>false</code> otherwise.
     */
    public boolean hasHorizontalDateAxis() {
        return hasHorizontalDateAxis;
    }
}
