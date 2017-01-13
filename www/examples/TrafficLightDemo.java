
import org.bhavaya.util.*;
import org.bhavaya.ui.TrafficLight;

import javax.swing.*;

/**
 * Date: 01-Mar-2004
 * Time: 10:36:22
  */
public class TrafficLightDemo {


    public static void main(String[] args) throws Exception{

        JFrame frame = new JFrame("Traffic Light Demo");
        JPanel contents = new JPanel();

        //System.setProperty("OVERRIDE_RESOURCE_DIR","C:/Projects/ShivaApps/bhavaya/resources");

        TrafficLightModel model = new DefaultTrafficLightModel();
        TrafficLight trafficLight = new TrafficLight(model);
        contents.add(trafficLight);
        frame.setContentPane(contents);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        while (true) {
            Thread.sleep(1000);
            model.setState(TrafficLightState.YELLOW);
            Thread.sleep(1000);
            model.setState(TrafficLightState.GREEN);
            Thread.sleep(1000);
            model.setState(TrafficLightState.YELLOW);
            Thread.sleep(1000);
            model.setState(TrafficLightState.RED);
        }
    }

}


