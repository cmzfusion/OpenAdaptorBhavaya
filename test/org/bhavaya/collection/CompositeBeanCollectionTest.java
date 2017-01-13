package org.bhavaya.collection;

import junit.framework.TestCase;
import org.bhavaya.beans.Bean;

/**
 * Created by IntelliJ IDEA.
 * User: ebbuttn
 * Date: 31-Jan-2008
 * Time: 17:30:46
 * To change this template use File | Settings | File Templates.
 *
 * Far from complete but it's a start
 */
public class CompositeBeanCollectionTest extends TestCase {

    private CompositeBeanCollection compositeBeanCollection;
    private Bean bean1;
    private Bean bean2;
    private DefaultBeanCollection<Bean> childBeanCollection1;
    private DefaultBeanCollection<Bean> childBeanCollection2;

    public void setUp() {

        childBeanCollection1 = new DefaultBeanCollection<Bean>(Bean.class);
        childBeanCollection2 = new DefaultBeanCollection<Bean>(Bean.class);

        compositeBeanCollection = new CompositeBeanCollection(
                Bean.class, new BeanCollection[] {childBeanCollection1, childBeanCollection2});

        bean1 = new Bean();
        bean2 = new Bean();
        childBeanCollection1.add(bean1);
        childBeanCollection2.add(bean2);
    }

    public void testInitialContents() {
        assertEquals(compositeBeanCollection.size(), 2);
    }

    public void testAddDuplicateBeansHasNoEffect() {
        childBeanCollection1.add(bean2);
        childBeanCollection2.add(bean1);
        testInitialContents();
    }

    public void testClearChildCollection() {
        childBeanCollection1.clear();
        assertEquals(compositeBeanCollection.size(), 1);

        //even if no commit is sent, the composite should still update based on structure events
        childBeanCollection2.clear(false);
        assertEquals(compositeBeanCollection.size(), 0);
    }

    public void testClearChildDoesNotAffectCompositeIfBeansInAnotherChild() {
        childBeanCollection1.add(bean2);

        childBeanCollection2.clear(false);
        assertEquals(compositeBeanCollection.size(), 2);
    }

    public void testRemove() {
        childBeanCollection1.remove(bean1);
        assertEquals(compositeBeanCollection.size(), 1);
    }

    public void testAdd() {
        Bean bean3 = new Bean();
        //even if no commit is sent, the composite should still update based on structure events
        childBeanCollection1.add(bean3, false);
        assertEquals(compositeBeanCollection.size(), 3);
    }

}
