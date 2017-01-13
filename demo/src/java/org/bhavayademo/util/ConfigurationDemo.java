package org.bhavayademo.util;

import org.bhavaya.util.Configuration;
import org.bhavaya.util.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: vanencd
 * Date: 20-Jan-2004
 * Time: 17:03:58
 * To change this template use Options | File Templates.
 */
public class ConfigurationDemo {
    private static final String STRING_KEY = "myString";
    private static String MAIN_USER_KEY = "mainUser";

    private static User theUser = null;
    private static String aString = null;
    private static Configuration userConfiguration;

    private static void loadSettings() {
        userConfiguration = Configuration.getRoot().getConfiguration("databaseUsers");
        aString = (String) Configuration.getRoot().getObject(STRING_KEY, "aDefaultStringValue", String.class);
        theUser = (User) userConfiguration.getObject(MAIN_USER_KEY, null, User.class);
    }

    private static void saveSettings() {
        Configuration.getRoot().putObject(STRING_KEY, aString);
        userConfiguration.putObject(MAIN_USER_KEY, theUser);
    }

    public static void main(String[] args) {
        //set up a file config source and sink with the given serach directories (in this case the working directory)
        Configuration.FileConfigurationSourceSink f = new Configuration.FileConfigurationSourceSink(new String[]{"."});
        Configuration.addConfigurationSource(f);
        Configuration.addConfigurationSink(f);

        //we add "save tasks" to the configuration. these get executed when we want to save. They can abort the save by
        //throwing an abortTaskException
        Configuration.addSaveTask(new Task("demo save task") {
            public void run() throws Task.AbortTaskException, Throwable {
                saveSettings();
            }
        });

        loadSettings();

        changeSettings();

        //save settings
        try {
            Configuration.save();
        } catch (Throwable throwable) {
            System.out.println("Could not save the config due to: "+throwable);
            throwable.printStackTrace();
        }
    }

    /**
     * just some stuff that displays and changes values we want to save
     */
    private static void changeSettings() {
        System.out.println("myString is ["+aString+"]\n enter new value:");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            aString = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }

        System.out.println("main user is "+theUser);

        theUser = new User();
        try {
            System.out.println("enter new name:");
            theUser.setName( reader.readLine() );
            System.out.println("enter new pass:");
            theUser.setPassword( reader.readLine() );
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

    /**
     * demo of persisting a custom object, not just java objects (see java.beans.XMLEncoder for more detail about the
     * persistence mechanism - basically just make sure you have a getter and setter for each property that should be persisted)
     */
    public static class User {
        private String name;
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String toString() {
            return "name: "+name+" pass: "+password;
        }
    }

}
