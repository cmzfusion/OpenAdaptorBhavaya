package example2;

import beans.Instrument;
import org.bhavaya.beans.BeanFactory;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.Persister;

import java.sql.SQLException;

/**
 * Date: 23-Mar-2004
 * Time: 11:30:34
 */
public class Example2b {

    private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example2Database", "createExample2.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example2Database", "destroyExample2.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void testUpdate(){
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(3));
        System.out.println("Current instrument is " + instrument.getDescription() + " with price = " + instrument.getPrice());
        System.out.println("Setting price to 5.99");
        instrument.setPrice(5.99);
        System.out.println("Updating instrument in database");
        Persister persister = new Persister("example2Database",null);
        try {
            persister.updateObject(instrument,new String[]{"Price"});
        } catch (SQLException e) {
            System.err.println("SQL exception!");
        } catch (NotificationException e) {
            System.err.println("Notification exception!");
        }
        System.out.println("Retrieving the updated instrument from the database");
        Instrument updatedInstrument = (Instrument) instrumentFactory.get(new Integer(3));
        System.out.println("Retrieved instrument has description " + updatedInstrument.getDescription() + " with price = " + updatedInstrument.getPrice());
    }

    public void testDelete() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(4));
        Persister persister = new Persister("example2Database",null);
        try {
            System.out.println("Deleting " + instrument.getDescription());
            persister.deleteObject(instrument);
        } catch (SQLException e) {
            System.err.println("SQL exception!");
        } catch (NotificationException e) {
            System.err.println("Notification exception!");
        }
        System.out.println("Trying to retrieve " + instrument.getDescription() + " from database");
        Instrument deletedInstrument = (Instrument) instrumentFactory.get(new Integer(4));
        if(deletedInstrument == null) {
            System.out.println("Instrument not found");
        } else {
            System.out.println("Retrieved instrument " + deletedInstrument.getDescription());
        }
    }

    public void testInsert() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.newBeanInstance();
        instrument.setPrice(3.24);
        instrument.setValid("true");
        instrument.setDescription("Generated bean");

        Persister persister = new Persister("example2Database",null);
        try {
            System.out.println("Inserting into database " + instrument.getDescription());
            persister.insertObject(instrument);
        } catch (SQLException e) {
            System.err.println("SQL exception!");
        } catch (NotificationException e) {
            System.err.println("Notification exception!");
        }

        Instrument newInstrument = (Instrument) instrumentFactory.get(new Integer(4));
        System.out.println("Retrieved instrument " + newInstrument.getDescription() + " from database");
    }

    public static void main(String[] args) throws Exception{

        Example2b example2b = new Example2b();
        example2b.cleanUP(); // Just to be sure
        example2b.setUp();
        example2b.testUpdate();
        example2b.testDelete();
        example2b.testInsert();
        example2b.cleanUP();

    }
}
