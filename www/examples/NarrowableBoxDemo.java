
import org.bhavaya.ui.NarrowableComboBox;
import org.bhavaya.ui.NarrowableListModel;

import javax.swing.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;


/**
 * Date: 02-Mar-2004
 * Time: 09:50:53
 */
public class NarrowableBoxDemo implements ActionListener{

    private JLabel label;
    private NarrowableComboBox box;

    public NarrowableBoxDemo(JPanel panel, NarrowableComboBox box) {

        panel.setLayout(new BorderLayout());
        panel.add(box,BorderLayout.WEST);
        label = new JLabel("Empty",JLabel.CENTER);

        panel.add(label,BorderLayout.CENTER);
        this.box = box;
        box.addActionListener(this);

    }


    public static void main(String[] args) {

        JFrame frame = new JFrame("Narrowable Combo Box Demo");
        JPanel panel = new JPanel();

        Collection data = new ArrayList();
        populate(data);


        int numberOfColumns = 10;  // Essentially the width of the text entry box
        int numberOfDisplayedSuggestions = 2; // Number of suggestions to display, use scroll pane if this is exceeded
        NarrowableListModel model = new NarrowableListModel(data);
        NarrowableComboBox ncb = new NarrowableComboBox(numberOfColumns,model,numberOfDisplayedSuggestions);

        new NarrowableBoxDemo(panel,ncb);

        frame.setContentPane(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(250,(int)frame.getPreferredSize().getHeight());
        frame.setVisible(true);
    }

    private static void populate(Collection list){
        list.add("one");
        list.add("two");
        list.add("three");
        list.add("four");
        list.add("five");
        list.add("six");
        list.add("seven");
        list.add("eight");
        list.add("nine");
        list.add("ten");
    }

    public void actionPerformed(ActionEvent event) {
        label.setText(box.getSelectedValue().toString());
    }


}
