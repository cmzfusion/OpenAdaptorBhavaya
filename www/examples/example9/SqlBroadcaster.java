package example9;

import org.bhavaya.db.broadcaster.SimpleFileSqlBroadcaster;
import org.bhavaya.util.Log;

/**
 * Date: 22-Apr-2004
 * Time: 09:25:05
 */
public class SqlBroadcaster {
    private  static final Log log = Log.getCategory(SqlBroadcaster.class);

    public SqlBroadcaster() {
        new ShutDownButton("SqlBroadcaster", "SQL broadcaster running", "Stop broadcaster", new Quit(), false,
                "Broadcaster still running!", "SQL broadcaster still running, quitting will stop the broadcaster");
        Thread broadcasterThread = new Thread(new Runnable(){
            public void run() {
                //Start the hsqldb file watcher
                try {
                    SimpleFileSqlBroadcaster sqlBroadcaster = new SimpleFileSqlBroadcaster(false);
                    sqlBroadcaster.startWithFailover();
                } catch (Exception e) {
                    log.error("Error starting the file broadcaster " + e);
                }
            }
        });
        broadcasterThread.start();
    }

    private static class Quit implements ShutDownOperation {
        public void shutDown() {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        new SqlBroadcaster();
    }

}
