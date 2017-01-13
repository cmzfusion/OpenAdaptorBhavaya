package example5;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.beans.BeanFactory;
import beans.Instrument;
import beans.Quantity;

/**
 * Date: 31-Mar-2004
 * Time: 10:54:52
 */
public class Example5 {

    private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example5Database", "createExample5.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example5Database", "destroyExample5.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void testConstructorProperty() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(2));

        Quantity quantity = instrument.getInstrumentQuantity();
        System.out.println("quantity.getPrice() = " + quantity.getPrice());
        System.out.println("quantity.getCurrency() = " + quantity.getCurrency());
        System.out.println("quantity.getAmount() = " + quantity.getAmount());
        System.out.println("instrument.getPrice() = " + instrument.getPrice());
        System.out.println("quantity.getDate() = " + quantity.getDate());


    }

    public static void main(String[] args) throws Exception{

        Example5 example5 = new Example5();
        example5.cleanUP(); // Just to be sure
        example5.setUp();
        example5.testConstructorProperty();
        example5.cleanUP();

    }
}
