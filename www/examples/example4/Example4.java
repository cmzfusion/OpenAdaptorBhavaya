package example4;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.beans.BeanFactory;
import beans.Instrument;
import beans.Bond;

/**
 * Date: 26-Mar-2004
 * Time: 12:37:07
 */
public class Example4 {
      private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example4Database", "createExample4.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example4Database", "destroyExample4.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void testSubClass() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(2));
        Bond bond = (Bond) instrumentFactory.get(new Integer(1));

        System.out.println("instrument = " + instrument);
        System.out.println("bond = " + bond);

    }

    public static void main(String[] args) throws Exception{

        Example4 example4 = new Example4();
        example4.cleanUP(); // Just to be sure
        example4.setUp();
        example4.testSubClass();
        example4.cleanUP();

    }
}
