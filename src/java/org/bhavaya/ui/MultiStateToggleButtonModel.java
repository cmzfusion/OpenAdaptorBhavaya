package org.bhavaya.ui;

import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Allows for multiple states of a button. First state obviously would be inactive (default) state of the button.
 * By clicking the button, states are changed in a loop. Initial state must be preset by class user.
 *
 * See {@link CustomButtonGroup} for grouping of buttons using this model.  
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class MultiStateToggleButtonModel extends JToggleButton.ToggleButtonModel {

    private ToggleButtonState[] states;
    private int state = 0;
    private boolean selected = false;
    private JToggleButton button;

    public MultiStateToggleButtonModel(JToggleButton button, ToggleButtonState[] states) {
        this.button = button;
        this.states = states;
    }

    public void setSelected(boolean b) {
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent.getSource() == button) {
            state = (++state) % states.length;
        } else {
            state = 0; // reset state
        }
        setState(state);
    }

    public boolean isSelected() {
        return selected;
    }

    public int getCurrentStateIndex() {
        return state;
    }

    public void setState(int index) {
        assert index < states.length : "Index out of bounds.";
        state = index;

        selected = states[state].selected;
        button.setIcon(states[state].icon);
        if (states[state].text != null) button.setText(states[state].text);
        if (getGroup() != null) getGroup().setSelected(this, selected);

        // Send ChangeEvent
        fireStateChanged();

        // Send ItemEvent
        fireItemStateChanged(new ItemEvent(this,
                ItemEvent.ITEM_STATE_CHANGED,
                this,
                this.isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
    }

    public ToggleButtonState getCurrentState() {
        return states[state];
    }

    public static class ToggleButtonState {
        String text; // null means don't change the text - not clear it. Use empty string to clear the button text.
        Icon icon;
        boolean selected;

        public ToggleButtonState(String text, Icon icon, boolean selected) {
            this.text = text;
            this.icon = icon;
            this.selected = selected;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // Test code
    ////////////////////////////////////////////////////////////////////////////////////////

    private static Icon ascendingIcon = ImageIconCache.getImageIcon("sort_ascending.png");
    private static Icon descendingIcon = ImageIconCache.getImageIcon("sort_descending.png");
    private static Icon blankIcon = ImageIconCache.getImageIcon("blank10by9.gif");

    private static MultiStateToggleButtonModel.ToggleButtonState[] testStates1 = {
        new MultiStateToggleButtonModel.ToggleButtonState("a", blankIcon, false),
        new MultiStateToggleButtonModel.ToggleButtonState("b", ascendingIcon, true),
        new MultiStateToggleButtonModel.ToggleButtonState("c", descendingIcon, true)
    };

    private static MultiStateToggleButtonModel.ToggleButtonState[] testStates2 = {
        new MultiStateToggleButtonModel.ToggleButtonState(null, null, false),
        new MultiStateToggleButtonModel.ToggleButtonState("b", ascendingIcon, true),
        new MultiStateToggleButtonModel.ToggleButtonState(null, descendingIcon, true)
    };



    public static void main(String[] args) {
        JFrame frame = new JFrame("title");
        Container cp = frame.getContentPane();
        cp.setLayout(new FlowLayout(FlowLayout.LEFT));

        final JToggleButton btn1 = new JToggleButton("1", blankIcon);
        final JToggleButton btn2 = new JToggleButton("2", blankIcon);
        final JToggleButton btn3 = new JToggleButton("3", blankIcon);
        btn1.setModel(new MultiStateToggleButtonModel(btn1, testStates1));
        btn2.setModel(new MultiStateToggleButtonModel(btn2, testStates1));
        btn3.setModel(new MultiStateToggleButtonModel(btn3, testStates2));
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
