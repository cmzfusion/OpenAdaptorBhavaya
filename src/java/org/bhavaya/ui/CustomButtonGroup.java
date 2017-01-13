package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Unlike the {@link ButtonGroup} in JDK this group allows for unselected button.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class CustomButtonGroup extends ButtonGroup {

    protected ButtonModel selection;
    private boolean insideSetSelected = false;

    public void add(AbstractButton b) {
        super.add(b);
        selection = getSelection();
    }

    public void remove(AbstractButton b) {
        super.remove(b);
        selection = getSelection();
    }

    public void setSelected(ButtonModel m, boolean b) {
        if (insideSetSelected) return;
        insideSetSelected = true;
        ButtonModel oldSelection = selection;
        selection = b ? m : null;
        if (oldSelection != m && oldSelection != null) {
            oldSelection.setSelected(false);
        }
        insideSetSelected = false;
    }

    public boolean isSelected(ButtonModel m) {
        return (m == selection);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // Test code
    ////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        JFrame frame = new JFrame("title");
        Container cp = frame.getContentPane();
        cp.setLayout(new FlowLayout(FlowLayout.LEFT));

        final JToggleButton btn1 = new JToggleButton("1");
        final JToggleButton btn2 = new JToggleButton("2");
        final JToggleButton btn3 = new JToggleButton("3");
        cp.add(btn1);
        cp.add(btn2);
        cp.add(btn3);

        CustomButtonGroup group = new CustomButtonGroup();
        group.add(btn1);
        group.add(btn2);
        group.add(btn3);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

