package example1;

import org.bhavaya.db.*;
import org.bhavaya.util.Generic;

import java.util.List;
import java.util.Iterator;

/**
 * Date: 19-Mar-2004
 * Time: 16:24:12
 */
public class Example1 {

    private List data;

    private void setUp() throws Exception {

        DBUtilities.executeUpdateScript("example1Database", "createExample1.sql",true);

        // Check it worked
        data = DBUtilities.execute("example1Database","select * from instrument");
        for(Iterator i = data.iterator(); i.hasNext();){
            Object item = i.next();
            for(int j=1; j<=6; j++){
                System.out.print(Generic.get(item,j)+ " ");
            }
            System.out.print("\n");
        }
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example1Database", "destroyExample1.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public static void main(String[] args) throws Exception{

        Example1 example1 = new Example1();
        example1.cleanUP(); // Just to be sure
        example1.setUp();
        example1.cleanUP();

    }
}
