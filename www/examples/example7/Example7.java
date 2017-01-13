package example7;

import beans.Instrument;
import beans.Trade;
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
 * Date: 02-Apr-2004
 * Time: 14:16:07
 */
public class Example7 {

    private Criterion criterion;
    private CriterionGroup criterionGroup;

    public Example7() {
        // Define the GBP criterion to be used by all tests
        criterion = new BasicCriterion("By_Currency_Code", "=", "GBP");
        criterionGroup = new CriterionGroup("GBP currency group", new Criterion[]{criterion});
    }

     private void setUp() throws Exception {
        DBUtilities.executeUpdateScript("example7Database", "createExample7.sql",true);
    }

    public void cleanUP(){
        try {
            DBUtilities.executeUpdateScript("example7Database", "destroyExample7.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
    }

    public void dispayAllTableData(String tableName, int cols) {
        List data;
        data = DBUtilities.execute("example7Database","select * from "+ tableName);
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

    public void testSimpleCriterion() {
        System.out.println("");
        System.out.println("Basic criterion - select instruments where currency string = GBP");
        // criterion and criterionGroup defined in constructor
        BeanCollection beanCollection = new CriteriaBeanCollection(Instrument.class,criterionGroup);
        for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
            Instrument instrument = (Instrument) iterator.next();
            System.out.println(instrument.getDescription() + " currency code = " +
                            instrument.getCurrency().getCode());
        }
    }

    public void testSimpleCriterionTrade() {
        System.out.println("");
        System.out.println("Basic criterion - select trades with currency string = GBP");
         // criterion and criterionGroup defined in constructor
        BeanCollection gbpTrades = new CriteriaBeanCollection(Trade.class,criterionGroup);
        for (Iterator iterator = gbpTrades.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            System.out.println(trade.getTradeType().getTypeName() + " " + trade.getQuantity() + " of instrument " +
                            trade.getInstrument().getDescription() + " at a price of " + trade.getPrice());
        }
    }

    public void testCombinedCriteria() {
        System.out.println("");
        System.out.println("CombinedCriteria - Select all trades priced over 1 and where the currency = GBP");
        // criterion defined in constructor
        Criterion overPoundCriterion = new BasicCriterion("By_Trade_Price", ">", new Double(100));
        CriterionGroup overPoundCriterionGroup = new CriterionGroup("Expensive group", new Criterion[]{criterion,overPoundCriterion});
        BeanCollection overPoundCollection = new CriteriaBeanCollection(Trade.class,overPoundCriterionGroup);
        for (Iterator iterator = overPoundCollection.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            System.out.println("Trade over 1 GBP = "+ trade.getInstrument().getDescription() + " (at a price of " +
                    trade.getPrice() + ")");
        }
    }

    public static void main(String[] args) throws Exception{
        Example7 example7 = new Example7();  // Constructor initialises GBP criterion
        example7.cleanUP(); // Just to be sure
        example7.setUp();
        example7.dispayAllTableData("INSTRUMENT",6);
        example7.dispayAllTableData("TRADE",8);
        System.out.println("Column 10 of TRADE references column 1 of INSTRUMENT\n");
        example7.dispayAllTableData("TRADE_TYPE",2);
        System.out.println("Column 9 of trade references the TRADE_TYPE");
        example7.testSimpleCriterion();  // Instruments where currency = GBP
        example7.testSimpleCriterionTrade(); // Trades where currency = GBP
        example7.testCombinedCriteria(); // Trades where currency = GBP and Price > 100
        example7.cleanUP();
    }

}
