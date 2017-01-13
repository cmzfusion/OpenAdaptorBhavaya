package example9;

import beans.Instrument;
import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.collection.ListEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

/**
 * Date: 02-Apr-2004
 * Time: 14:16:07
 */
public class Example9 {
    private Criterion criterion;
    private CriterionGroup criterionGroup;

    public Example9() {
        new ShutDownButton("Example 9", "Example 9 running", "Quit", new Quit(), false,
                "Really exit?", "Terminate the program?");
    }

    public void getGBPCollection() {
        // Define the GBP criterion to be used by all tests
        criterion = new BasicCriterion("By_Currency_Code", "=", "GBP");
        criterionGroup = new CriterionGroup("GBP currency group", new Criterion[]{criterion});

        BeanCollection gbpInstruments = new CriteriaBeanCollection(Instrument.class,criterionGroup);

        System.out.println("Collection currently contains:");
        for (Iterator iterator = gbpInstruments.iterator(); iterator.hasNext();) {
            Instrument instrument = (Instrument) iterator.next();
            System.out.println(instrument.getInstrumentId() + " "
                    + instrument.getDescription() + " "
                    + instrument.getPrice() + " "
                    + instrument.getCurrency());
        }

        System.out.println("\nListening for changes to GBP collection:");
        gbpInstruments.addCollectionListener(new CollectionListener(){
            public void collectionChanged(ListEvent e) {
                int type = e.getType();
                if(type != ListEvent.COMMIT) { // This is equivalent to e.getValue!=null
                    System.out.print("Collection change - ");
                    switch (type) {
                        case ListEvent.INSERT :
                            System.out.print("Inserting: ");
                            break;
                        case ListEvent.UPDATE:
                            System.out.print("Updating:  ");
                            break;
                        case ListEvent.DELETE:
                            System.out.print("Deleting:  ");
                            break;
                        default:
                            System.out.print("Unknown!");
                    }

                    Instrument instrument = (Instrument) e.getValue();
                    System.out.println(instrument.getInstrumentId() + " "
                            + instrument.getDescription() + " "
                            + instrument.getPrice() + " "
                            + instrument.getCurrency());

                } else {
                    //System.out.println("Commit on database");
                    //SimpleFileSqlBroadcaster does not broadcast SQL until it has received a commit
                }
            }
        });

    }

    public void testSingleBeanRealTime() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(3));
        instrument.addPropertyChangeListener("price", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                System.out.print("Bean with ID=3 changed! ");
                System.out.println("price changed from " + event.getOldValue()
                        + " to " + event.getNewValue());
            }
        });
    }

    private static class Quit implements ShutDownOperation {
        public void shutDown() {
            System.exit(0);
        }
    }

    public static void main(String[] args) throws Exception{
        Example9 example9 = new Example9();
        example9.getGBPCollection(); // Instruments where currency = GBP
        example9.testSingleBeanRealTime();
    }

}
