package example2;

import beans.Instrument;
import org.bhavaya.beans.BeanFactory;
import org.bhavaya.db.DBUtilities;

import java.text.DecimalFormat;

/**
 * Date: 23-Mar-2004
 * Time: 11:30:34
 */
public class Example2 {

    private static final DecimalFormat formatter = new DecimalFormat("##.##");

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

    public void testGet() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(3));
        System.out.println("instrument description = " + instrument.getDescription());
        System.out.println("Price = " + formatter.format(instrument.getPrice()) +
                           " VAT Price = " + formatter.format(instrument.getVatPrice()));

        Instrument[] instruments = (Instrument[]) instrumentFactory.getAllObjects();
        System.out.println("instruments.length = " + instruments.length);
        System.out.println("instrumentFactory.size() = " + instrumentFactory.size());
    }

    public void testSet(){
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(3));
        System.out.println("Setting price to 5.99");
        instrument.setPrice(5.99);
        System.out.println("instrument description = " + instrument.getDescription());
        System.out.println("Price = " + formatter.format(instrument.getPrice()) +
                           " VAT Price = " +  formatter.format(instrument.getVatPrice()));
    }

    public static void main(String[] args) throws Exception{

        Example2 example2 = new Example2();
        example2.cleanUP(); // Just to be sure
        example2.setUp();
        example2.testGet();
        example2.testSet();
        example2.cleanUP();

    }
}
