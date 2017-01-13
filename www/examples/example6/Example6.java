package example6;

import beans.Instrument;
import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.util.Generic;

import java.util.Iterator;
import java.util.List;

/**
 * Date: 01-Apr-2004
 * Time: 16:24:05
 */
public class Example6 {
    private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example6Database", "createExample6.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example6Database", "destroyExample6.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void dispayAllTableData(String tableName, int cols) {
        List data;
        data = DBUtilities.execute("example6Database","select * from "+ tableName);
        System.out.println("All data in table " + tableName + ":");
        for(Iterator i = data.iterator(); i.hasNext();){
            Object item = i.next();
            try {
                for(int j=1; j<=cols; j++){
                    System.out.print(Generic.get(item,j)+ " ");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Attempted to access non-existent column of table");
            }
            System.out.print("\n");
        }
        System.out.println("----------------------------------------------------------------------------------\n");
    }

    public void testCriterion() {
        Criterion criterion = new BasicCriterion("By_Currency_Code", "=", "GBP");
        CriterionGroup criterionGroup = new CriterionGroup("GBP currency group", new Criterion[]{criterion});

        BeanCollection beanCollection = new CriteriaBeanCollection(Instrument.class,criterionGroup);
        for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
            Instrument instrument = (Instrument) iterator.next();
            System.out.println("GBP instrument = " + instrument.getDescription());
        }
    }

    public static void main(String[] args) throws Exception{
        Example6 example6 = new Example6();
        example6.cleanUP(); // Just to be sure
        example6.setUp();
        example6.dispayAllTableData("INSTRUMENT",6);
        example6.testCriterion();
        example6.cleanUP();
    }
}
