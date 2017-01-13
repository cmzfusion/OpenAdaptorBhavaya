package org.bhavaya.collection;

import org.bhavaya.beans.SplicedBean;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.collection.SplicedBeanCollection;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew J. Dean
 * @version $Revision: 1.1 $
 */
public class SplicedBeanCollectionTest {
    BeanCollection drinksCollection;
    BeanCollection foodCollection;
    BeanCollection perChefDinnerOrderCollection;
    AnalyticsTable table;
    private boolean run = true;
    private static final int HEAD_CHEF = 1; // We know that this creates a unique set of Food objects
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public SplicedBeanCollectionTest() {
        drinksCollection = new DefaultBeanCollection(Drink.class);
        foodCollection = new DefaultBeanCollection(Food.class);
        for (int i = 0; i < 5; i++) {
            addFood();
            addDrink();
        }
        perChefDinnerOrderCollection = new SplicedBeanCollection(DinnerOrder.class, drinksCollection, foodCollection);
        BeanCollectionTableModel tableModel = new BeanCollectionTableModel(perChefDinnerOrderCollection, false);
        table = new AnalyticsTable(tableModel, true);
        addColumns(tableModel);
//        table.getAnalyticsTableModel().addSortingColumn("uniqueId", false);
        startRandomCollectionUpdates();
    }

    private void addColumns(BeanCollectionTableModel tableModel) {
        tableModel.addColumnLocator("uniqueId");
        tableModel.addColumnLocator("drink");
        tableModel.addColumnLocator("food");
    }

    private void startRandomCollectionUpdates() {
        Thread updateThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (run) {
                        addFood();
                        addDrink();
                        removeRandomBean(drinksCollection);
                        removeRandomBean(foodCollection);
                        try {
                            synchronized (this) {
                                wait((long) ((Math.random() * 5000)) + 1);
                            }
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                } catch (Throwable t) {
                    System.out.println(t);
                }
            }
        });
        updateThread.start();
    }

    private void removeRandomBean(BeanCollection beanCollection) {
        int i = beanCollection.size();
        if (i > 3) {
            beanCollection.remove((int) (Math.random() * (i - 1)));
        }
    }

    private void addDrink() {
        Drink drink = new Drink((int) (Math.random() * 10));
        drinksCollection.add(drink);
    }

    private void addFood() {
        Food food = new Food((int) (Math.random() * 10), HEAD_CHEF);
        foodCollection.add(food);
    }


    private void createAndShowGui() {
        final JFrame frame = new JFrame("Spliced Bean Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        frame.setContentPane(panel);
        panel.add(table);

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    public static class Drink {
        private static String[] drinks = new String[]{"tea", "coffee", "orange", "beer", "wine"};
        private String drink;
        private int id;

        public Drink(int id) {
            this.id = id;
            setDrink(drinks[(int) (Math.random() * 5)]);
        }

        public int getId() {
            return id;
        }

        public String getDrink() {
            return drink;
        }

        private void setDrink(String drink) {
            this.drink = drink;
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Drink drink = (Drink) o;

            return id == drink.id;
        }

        public int hashCode() {
            return id;
        }
    }

    public static class Food {
        private static String[] foods = new String[]{"beef", "pasta", "sandwiches", "chicken", "chips"};
        private String food;
        private int customerId;
        private int chef;

        public Food(int customerId, int chef) {
            this.customerId = customerId;
            this.chef = chef;
            setFood(foods[(int) (Math.random() * 5)]);
        }

        public int getCustomerId() {
            return customerId;
        }

        public String getFood() {
            return food;
        }

        private void setFood(String food) {
            this.food = food;
        }


        // A little contrived, but note that chef is on equals / hashcode. We know that our
        // combined collection only ever uses one chef, so our collection is still unique on customerId, which
        // is our join column.
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Food food = (Food) o;

            if (customerId != food.customerId) return false;
            return chef == food.chef;
        }

        public int hashCode() {
            int result;
            result = customerId;
            result = 31 * result + chef;
            return result;
        }
    }

    public static class DinnerOrder extends SplicedBean {
        private Drink drink;
        private Food food;

        public DinnerOrder(Object eitherMember) {
            super(eitherMember);
        }

        public String getDrink() {
            return drink == null ? null : drink.getDrink();
        }

        public String getFood() {
            return food == null ? null : food.getFood();
        }

        private void setDrink(Drink drink) {
            Drink oldDrink = this.drink;
            this.drink = drink;
            fireNewComponentObject(Drink.class, oldDrink, drink);
        }

        private void setFood(Food food) {
            Food oldFood = this.food;
            this.food = food;
            fireNewComponentObject(Food.class, oldFood, food);
        }

        public Object generateUniqueId(Object object) {
            if (object instanceof Drink) {
                return new Integer(((Drink) object).getId());
            } else if (object instanceof Food) {
                return new Integer(((Food) object).getCustomerId());
            } else {
                throw new RuntimeException("Wrong type!");
            }
        }

        public void update(Object updatedBean) {
            if (updatedBean instanceof Drink) {
                setDrink((Drink) updatedBean);
            } else if (updatedBean instanceof Food) {
                setFood((Food) updatedBean);
            } else {
                throw new RuntimeException("Wrong type");
            }
        }

        public void remove(Object value) {
            if (value instanceof Drink) {
                setDrink(null);
            } else if (value instanceof Food) {
                setFood(null);
            } else {
                throw new RuntimeException("Wrong type");
            }
        }

        public boolean hasData() {
            return drink != null || food != null;
        }

        protected String[] getPropertiesCommonToBothMembers() {
            return EMPTY_STRING_ARRAY;
        }
    }

    public static void main(String[] args) {
        SplicedBeanCollectionTest test = new SplicedBeanCollectionTest();
        test.createAndShowGui();
    }
}
