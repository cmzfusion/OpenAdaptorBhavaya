package example9;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Date: 16-Apr-2004
 * Time: 14:13:17
 */
public class HsqldbServerControl {
    private static final Log log = Log.getCategory(DBUpdater.class);
    private String databaseName;

    public HsqldbServerControl(String databaseName) {
        this.databaseName = databaseName;
    }

    public void startServer() {
        String classPath = System.getProperty("java.class.path");
        String fileSeparator = System.getProperty("file.separator");
        String bin = System.getProperty("java.home") + fileSeparator + "bin" + fileSeparator;
        String command = (bin + "java -cp \"" + classPath + "\" org.hsqldb.Server -database " + databaseName);

        try {
            //System.out.println("Running : " + command);
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            log.error("Could not start hsqldb server");
            System.exit(1);
        }

        new ShutDownButton("HSQLDB Server Interface", "HSQLDB database server running","Stop Database",new Quit(),
                false,"Database still running!",
                "Database is still running \nIt is necessary to stop the database to quit");
    }

    public void stopServer() {
        String dataSourceName = ApplicationProperties.getApplicationProperties().getProperty("dataSources.defaultDataSource");
        try {
            DBUtilities.execute(dataSourceName,"SHUTDOWN");
        } finally {
            //Hsqldb generates these files to store the database data
            //They contain a history of all the sql sent to the database
            // and could potentially become very large ...
            File dbFile1 = new  File(databaseName + ".script");
            File dbFile2= new File(databaseName + ".properties");
            dbFile1.delete();
            dbFile2.delete();
            System.exit(0);
        }
        //System.out.println("Server stopped");
    }

    private class Quit implements ShutDownOperation {
        public void shutDown() {
            stopServer();
        }
    }

    public static void main(String[] args) {
        HsqldbServerControl hsqldbServerControl = new HsqldbServerControl("Bhavaya_RT_Demo");
        hsqldbServerControl.startServer();

    }

}
