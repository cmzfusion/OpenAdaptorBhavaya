package example3;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.db.DBUtilities;
import beans.Instrument;

/**
 * Date: 25-Mar-2004
 * Time: 09:52:51
 */
public class Example3 {

    private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example3Database", "createExample3.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example3Database", "destroyExample3.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void testGet() {
        BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
        Instrument instrument = (Instrument) instrumentFactory.get(new Integer(3));
        System.out.println("Instrument 3's Currency = " + instrument.getCurrency().getCode());
    }

    public static void main(String[] args) throws Exception{

        Example3 example3 = new Example3();
        example3.cleanUP(); // Just to be sure
        example3.setUp();
        example3.testGet();
        example3.cleanUP();

    }

}
