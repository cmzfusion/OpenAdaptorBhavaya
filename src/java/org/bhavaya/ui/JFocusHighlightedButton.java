package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * What a load of rubbish this class is.  Should probably be sent to straight to jail without passing begin.  First
 * step would be to make it private.  Hide it somewhere in shame and hope no-one sees it.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class JFocusHighlightedButton extends JExtendedButton implements FocusListener {

    public JFocusHighlightedButton(String buttonText) {
        super(buttonText);
        addFocusListener(this);
    }

    public JFocusHighlightedButton(Action action) {
        super(action);     
        addFocusListener(this);
    }

    public void focusGained(FocusEvent e) {
        focusAction();
    }

    public void focusLost(FocusEvent e) {
        focusAction();
    }

    private void focusAction() {
        if (isFocusOwner()) {
            setFont(getFont().deriveFont(Font.BOLD));
        } else if (getFont().isBold()) {
            setFont(getFont().deriveFont(Font.PLAIN));
        }
    }
}
