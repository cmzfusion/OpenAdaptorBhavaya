package example8;

import beans.Trade;
import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.beans.criterion.EnumerationCriterion;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.util.Generic;

import java.util.Iterator;
import java.util.List;

/**
 * Date: 13-Apr-2004
 * Time: 13:01:30
 */
public class Example8 {
    private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example8Database", "createExample8.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example8Database", "destroyExample8.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void dispayAllTableData(String tableName, int cols) {
        List data;
        data = DBUtilities.execute("example8Database","select * from "+ tableName);
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


    public void testEnumerationCriterionGroup() {
        System.out.println("");
        System.out.println("Use an enumeration - select all trades whose currency bean has code EUR or USD");
        EnumerationCriterion.EnumElement[] enumElement = {new EnumerationCriterion.EnumElement("EUR",null),
                                                          new EnumerationCriterion.EnumElement("USD",null)};
        Criterion enumCriterion = new EnumerationCriterion("By_Currency", "IN", enumElement);
        CriterionGroup enumGroup = new CriterionGroup("Not Pounds", new Criterion[]{enumCriterion});

        BeanCollection enumCollection = new CriteriaBeanCollection(Trade.class,enumGroup);
        for (Iterator iterator = enumCollection.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            System.out.println("Enum = "+ trade.getInstrument().getCurrency().getCode() +
                    " " + trade.getInstrument().getDescription());
        }
    }

    public void testEnumerationCriterionByTradeType() {
        System.out.println("");
        System.out.println("Use an enumeration - select all buy trades be specifying the BUY bean");
        EnumerationCriterion.EnumElement[] buyElement = {new EnumerationCriterion.EnumElement(new Integer(0),null)};
        Criterion buyCriterion = new EnumerationCriterion("By_Type", "IN", buyElement);
        CriterionGroup buyGroup = new CriterionGroup("Buy trades", new Criterion[]{buyCriterion});

        BeanCollection buyCollection = new CriteriaBeanCollection(Trade.class,buyGroup);
        for (Iterator iterator = buyCollection.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            System.out.println("Enum = "+ trade.getTradeType().getTypeName() +
                    " " + trade.getInstrument().getDescription());
        }

    }

    public static void main(String[] args) throws Exception{

        Example8 example8 = new Example8();
        example8.cleanUP(); // Just to be sure
        example8.setUp();
        example8.dispayAllTableData("INSTRUMENT",6);
        example8.dispayAllTableData("TRADE",8);
        System.out.println("Column 10 of TRADE references column 1 of INSTRUMENT\n");
        example8.dispayAllTableData("TRADE_TYPE",2);
        System.out.println("Column 9 of trade references the TRADE_TYPE");

        example8.testEnumerationCriterionGroup();
        example8.testEnumerationCriterionByTradeType();
        example8.cleanUP();

    }


}
