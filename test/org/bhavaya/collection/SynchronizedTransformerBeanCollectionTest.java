package org.bhavaya.collection;

import junit.framework.TestCase;
import org.bhavaya.collection.SynchronizedTransformerBeanCollection;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.Transform;


/**
 * @author <a href="mailto:Sabine.Haas@drkw.com">Sabine Haas, Dresdner Kleinwort Wasserstein</a>
 * @version $Revision: 1.1 $
 */
public class SynchronizedTransformerBeanCollectionTest extends TestCase {
    public SynchronizedTransformerBeanCollectionTest(String s) {
        super(s);
    }

    public void testInitialization() {
        DefaultBeanCollection defaultBeanCollection = new DefaultBeanCollection(Double.class);
        defaultBeanCollection.add(new Double(3.4));
        defaultBeanCollection.add(new Double(3.5));
        defaultBeanCollection.add(new Double(6.4));

        BeanCollection transformerCollection = new SynchronizedTransformerBeanCollection(String.class, defaultBeanCollection, new Transform() {
            public Object execute(Object sourceData) {
                return sourceData.toString();
            }
        }, new Transform() {
            public Object execute(Object sourceData) {
                return new Double((String) sourceData);
            }
        });

        assertTrue(defaultBeanCollection.size() == transformerCollection.size());
        assertTrue(transformerCollection.getType().equals(String.class));
    }

    public void testAdd() {
        DefaultBeanCollection defaultBeanCollection = new DefaultBeanCollection(Double.class);
        defaultBeanCollection.add(new Double(3.4));
        defaultBeanCollection.add(new Double(3.5));
        defaultBeanCollection.add(new Double(6.4));

        BeanCollection transformerCollection = new SynchronizedTransformerBeanCollection(String.class, defaultBeanCollection, new Transform() {
            public Object execute(Object sourceData) {
                return sourceData.toString();
            }
        }, new Transform() {
            public Object execute(Object sourceData) {
                return new Double((String) sourceData);
            }
        });

        int formerSize = defaultBeanCollection.size();
        transformerCollection.add("1.2", false);

        assertTrue("Transformed Collection Add leave the collections unsyncronized.", defaultBeanCollection.size() == transformerCollection.size());
        assertTrue("Transformed Collection Add did not changed the origin one.", defaultBeanCollection.size() == formerSize + 1);

        formerSize = defaultBeanCollection.size();
        defaultBeanCollection.add(new Double(1.4));

        assertTrue("Source Collection Add leave the collections unsyncronized.", defaultBeanCollection.size() == transformerCollection.size());
        assertTrue("Source Collection Add did not changed the transformed one.", transformerCollection.size() == formerSize + 1);

    }

    public void testDelete() {
        DefaultBeanCollection defaultBeanCollection = new DefaultBeanCollection(Double.class);
        defaultBeanCollection.add(new Double(3.4));
        defaultBeanCollection.add(new Double(3.5));
        defaultBeanCollection.add(new Double(6.4));

        BeanCollection transformerCollection = new SynchronizedTransformerBeanCollection(String.class, defaultBeanCollection, new Transform() {
            public Object execute(Object sourceData) {
                return sourceData.toString();
            }
        }, new Transform() {
            public Object execute(Object sourceData) {
                return new Double((String) sourceData);
            }
        });

        int formerSize = defaultBeanCollection.size();
        transformerCollection.remove("3.5", false);

        assertTrue("Transformed Collection Remove leave the collections unsyncronized.", defaultBeanCollection.size() == transformerCollection.size());
        assertTrue("Transformed Collection Remove did not changed the origin one.", defaultBeanCollection.size() == formerSize - 1);

        formerSize = defaultBeanCollection.size();
        defaultBeanCollection.remove(new Double(6.4));

        assertTrue("Source Collection Remove leave the collections unsyncronized.", defaultBeanCollection.size() == transformerCollection.size());
        assertTrue("Source Collection Remove did not changed the transformed one.", transformerCollection.size() == formerSize - 1);

    }
}
