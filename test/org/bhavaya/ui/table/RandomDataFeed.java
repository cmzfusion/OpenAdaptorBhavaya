package org.bhavaya.ui.table;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.util.Generic;

import java.util.Date;
import java.util.Random;

/**
 * Feeds and modifies data in bean collection for analytics table tests.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class RandomDataFeed implements Runnable {

    private static final int ADD_REMOVE_DATA_LOOPS = 50;//300;
    private static final int UPDATE_DATA_LOOPS = 50;//600;
    protected final String[] STRINGS = new String[]{"USD", "EUR", "GBP", "YEN", "FOO"};

    BeanCollection beanCollection;
    protected Random random = new Random(123456);
    protected double nextDouble = 0;
    protected BeanB sharedBean;

    public RandomDataFeed(BeanCollection beanCollection) {
        this.beanCollection = beanCollection;
        sharedBean = (BeanB) getRandom(BeanB.class);
    }

    public void run() {
//        sleep(500);
        addRemoveRows(true);
//        sleep(1000);
        updateData();
    }

    protected void addRemoveRows(boolean removeRows) {
        BeanA first = new BeanA();
        first.setB(sharedBean);
        beanCollection.add(first);

        int added = 1;
        int removed = 0;
        for (int i = 0; i < ADD_REMOVE_DATA_LOOPS; i++) {
            //generate new BeanA
            double rnd = random.nextDouble();
            if (removeRows && (rnd -= 0.20) < 0) { //20% delete
                if (beanCollection.size() > 0) {
                    int row = random.nextInt(beanCollection.size());
                    beanCollection.remove(row);
                    removed++;
                }
            } else if ((rnd -= 0.80) < 0) {//80% insert
                BeanA newBean = (BeanA) getRandom(BeanA.class);
                if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                    newBean.setB(sharedBean);
                }
                beanCollection.add(newBean);
                added++;
            }
        }
        System.out.println("Added " + added + " Removed " + removed + " rows.");
    }

    protected void updateData() {
        System.out.println("Updating data.");
        for (int i = 0; i < UPDATE_DATA_LOOPS; i++) {
            sleep(20);
            BeanB b;
            if (random.nextBoolean()) {//update shared
                b = sharedBean;
            } else {//update a different bean
                int index = random.nextInt(beanCollection.size());
                b = ((BeanA) beanCollection.get(index)).getB();
            }

            //decide what to update:
            int choice = random.nextInt(11);
            BeanB newB;
            BeanC newC;
//            System.out.println("Choice: " + choice);
            switch (choice) {
                case 0: //update b.b (to affect pivot rows, columns and data)
                    newB = (BeanB) getRandom(BeanB.class);
                    newC = (BeanC) getRandom(BeanC.class);
                    newB.setC(newC);
                    Generic.set(b, Generic.beanPathStringToArray("b"), newB, true);
                    break;
                case 1: //update b.b to shared instance
                    Generic.set(b, Generic.beanPathStringToArray("b"), sharedBean, true);
                    break;
                case 2: //update b.b.c (to affect pivot columns and data)
                    newC = (BeanC) getRandom(BeanC.class);
                    Generic.set(b, Generic.beanPathStringToArray("b.c"), newC, true);
                    break;
                case 3: //update b.date and b.b.c.double (to affect pivot rows and data)
                    String currentString = b.getB().getC().getSomeString();
                    newB = (BeanB) getRandom(BeanB.class);
                    newC = (BeanC) getRandom(BeanC.class);
                    newC.setSomeString(currentString);
                    Generic.set(b, Generic.beanPathStringToArray("b"), newB, true);
                    break;
                case 4: //update b.date and b.b.c.someString (to affect pivot rows and columns)
                    double currentDouble = b.getB().getC().getSomeDouble();
                    newB = (BeanB) getRandom(BeanB.class);
                    newC = (BeanC) getRandom(BeanC.class);
                    newC.setSomeDouble(currentDouble);
                    Generic.set(b, Generic.beanPathStringToArray("b"), newB, true);
                    break;
                case 5: //update b.b.c.double (to affect pivot data)
                    Generic.set(b, Generic.beanPathStringToArray("b.c.someDouble"), getRandom(Double.class), true);
                    break;
                case 6: //update b.b.c.someString (to affect pivot columns)
                    Generic.set(b, Generic.beanPathStringToArray("b.c.someString"), getRandom(String.class), true);
                    break;
                case 7: //update b.b.date (to affect pivot rows)
                    Generic.set(b, Generic.beanPathStringToArray("b.date"), getRandom(Date.class), true);
                    break;
                case 8: //update b.b.c.someString (to affect pivot columns) - create new pivot column
                    String str = random.nextBoolean() ? "AUD" : "SKK";
                    Generic.set(b, Generic.beanPathStringToArray("b.c.someString"), str, true);
                    break;
                case 9: // add new bean
                    //generate new BeanA
                    BeanA newBean = (BeanA) getRandom(BeanA.class);
                    if (random.nextDouble() < 0.3) {    //30% of new rows will share values
                        newBean.setB(sharedBean);
                    }
                    beanCollection.add(newBean);
                    break;
                case 10: // remove bean
                    if (beanCollection.size() > 0) {
                        int row = random.nextInt(beanCollection.size());
                        beanCollection.remove(row);
                    }
                    break;
            }
        }
    }

    public Object getRandom(Class type) {
        if (type == Double.class) {
            Double aDouble = new Double(nextDouble++);
            return aDouble;
        } else if (type == String.class) {
            return STRINGS[random.nextInt(STRINGS.length)];
        } else if (type == Date.class) {
            return new Date(System.currentTimeMillis() + (long) (random.nextDouble() * 1000 * 60 * 60 * 24 * 365 * 20));  //a date from now to 20yrs time
        } else if (type == BeanA.class) {
            BeanA newA = new BeanA();
            newA.setB((BeanB) getRandom(BeanB.class));
            newA.setSomeDouble(nextDouble++);
            return newA;
        } else if (type == BeanB.class) {
            BeanB newB = new BeanB();
            newB.setDate((Date) getRandom(Date.class));
            newB.setC((BeanC) getRandom(BeanC.class));
            return newB;
        } else if (type == BeanC.class) {
            BeanC newC = new BeanC();
            newC.setSomeDouble(((Double) getRandom(Double.class)).doubleValue());
            newC.setSomeString((String) getRandom(String.class));
            return newC;
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}

