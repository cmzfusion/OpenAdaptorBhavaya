package beans;

import java.util.Date;

/**
 * Date: 31-Mar-2004
 * Time: 10:45:17
 */
public class Quantity {
    private int amount;
    private double price;
    private String currency;
    private Date date;

    public Quantity(double price, String currency, Date date) {
        this(1, price, currency, date);
    }
    
    public Quantity(int amount, double price, String currency, Date date) {
        this.amount = amount;
        this.price = price;
        this.currency = currency;
        this.date = date;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
