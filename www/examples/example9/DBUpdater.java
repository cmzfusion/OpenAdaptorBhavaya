package example9;

import org.bhavaya.db.DataSourceFactory;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Date: 14-Apr-2004
 * Time: 13:50:45
 */
public class DBUpdater {
    private static final String PRIMARY_KEY_COLUMN_NAME = "INSTRUMENT_ID";
    private static final String COMMIT = "COMMIT";
    private static final String[] CURRENCIES = {"GBP", "EUR", "USD"};
    private static final Log log = Log.getCategory(DBUpdater.class);
    private long delay;
    private String dataSourceName;
    private Connection connection;
    private Statement statement = null;

    public DBUpdater(final long delay) {
        new ShutDownButton("Database Updater", "Database Updater running", "Quit", new Quit(), false,
                "Really exit?", "Terminate the program?");
        this.delay = delay;
        dataSourceName = ApplicationProperties.getApplicationProperties().getProperty("dataSources.defaultDataSource");
        getConnection();
        try {
            populateDB();
        } catch (Exception e) {
            log.error("Error initialising database");
            System.exit(1);
        }

        Thread dBUpdaterThread = new Thread(new RandomUpdates());
        dBUpdaterThread.setDaemon(false);
        dBUpdaterThread.start();
    }

    private void getConnection() {
        try {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
            statement = connection.createStatement();
        } catch (SQLException e) {
            log.error("Unable to connect to datasource " + dataSourceName);
        }
    }

    private void insertRandomInstrument() {
        int keyNum = colMaxValue(PRIMARY_KEY_COLUMN_NAME) + 1;
        String sqlCommand = "insert into INSTRUMENT " +
                "(INSTRUMENT_ID,CURRENCY,INSTRUMENT_TYPE_ID,DESCRIPTION,VALID,PRICE)" +
                " VALUES ("
                + keyNum + ",'"
                + CURRENCIES[(int) (Math.random()*CURRENCIES.length)] + "',0,'NEW RANDOM INSTRUMENT','Y'," + Math.random()*10 + ")";

        try{
            statement.execute(sqlCommand);
            statement.execute(COMMIT);
            System.out.println("Executing " + sqlCommand);
        } catch (SQLException e) {
            System.out.println("Failed to insert row " + e);
        }
    }

    private void updateRandomInstrument() {
        int rowNum = (int) ( colMaxValue(PRIMARY_KEY_COLUMN_NAME) * Math.random() );
        rowNum = (rowNum == 0 ? 1 : rowNum); // Primary key starts at 1
        double newPrice = 1000*Math.random();
        String sqlCommand = "update INSTRUMENT set PRICE = " + newPrice
                + " where " + PRIMARY_KEY_COLUMN_NAME + " = " + rowNum;
        try{
            statement.execute(sqlCommand);
            statement.execute(COMMIT);
            System.out.println("Executing " + sqlCommand);
        } catch (SQLException e) {
            System.out.println("Failed to update row " + e);
        }

    }

    private int colMaxValue(String colName) {
        String sqlCommand = "select MAX(" + colName + ") from INSTRUMENT";
        try{
            ResultSet resultSet = statement.executeQuery(sqlCommand);
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            log.error("Error accessing database! " + e);
            throw new RuntimeException("Error determining maximum of column " + colName);
        }

    }

    private void populateDB() throws Exception {
        try {
            DBUtilities.executeUpdateScript(dataSourceName, "destroyExample9.sql",false);
        } catch (Throwable e) {
            // This is here in case the table is not found
        }
        DBUtilities.executeUpdateScript(dataSourceName, "createExample9.sql",true);

    }

    private static class Quit implements ShutDownOperation {
        public void shutDown() {
            System.exit(0);
        }
    }

    private class RandomUpdates implements Runnable {
        public void run() {
            while(true) {
                insertRandomInstrument();
                try {
                    Thread.sleep((long) (delay*1000*Math.random()) );
                } catch (InterruptedException e) {
                }

                updateRandomInstrument();
                try {
                    Thread.sleep((long) (delay*1000*Math.random()) );
                } catch (InterruptedException e) {
                }
            }

        }
    }

    public static void main(String[] args) {
        new DBUpdater(10);
    }

}
