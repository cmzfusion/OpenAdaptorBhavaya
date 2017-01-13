package org.bhavaya.ui;

import org.bhavaya.util.DateFunction;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public class DateFunctionTextField extends Box {

    private JTextField textField;
    protected DateFunction dateFunction;
    private List defaultDateFunctions;
    private DateFunction referenceDate;

    public DateFunctionTextField() {
        this(SymbolicDateFunction.TODAY_DATEFUNCTION, null, SymbolicDateFunction.TODAY_DATEFUNCTION);
    }

    public DateFunctionTextField(DateFunction dateFunction, List defaultDateFunctions, DateFunction referenceDate) {
        super(BoxLayout.X_AXIS);

        this.dateFunction = dateFunction;
        this.defaultDateFunctions = defaultDateFunctions;
        this.referenceDate = referenceDate;

        textField = new JTextField(14);
        textField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        textField.setEditable(false);
        textField.setFocusable(false);

        JButton editButton = new JButton(new EditDateFunctionAction("...", "Edit date", null, dateFunction, defaultDateFunctions, referenceDate) {
            protected void update(DateFunction dateFunction) {
                DateFunctionTextField.this.dateFunction = dateFunction;
                DateFunctionTextField.this.update();
            }

            public DateFunction getDateFunction() {
                return DateFunctionTextField.this.dateFunction;
            }
        });
        editButton.setPreferredSize(new Dimension(20, 20));

        add(textField);
        add(editButton);

        update();
    }

    protected void update() {
        updateTextField();

        //try to force correct min size
        textField.revalidate();
        revalidate();
        setMinimumSize(getPreferredSize());
        repaint();
    }

    public DateFunction getDateFunction() {
        return dateFunction;
    }

    public void setDateFunction(DateFunction dateFunction) {
        this.dateFunction = dateFunction;
        updateTextField();
    }

    private void updateTextField() {
        String text = dateFunction.getDescription();
        textField.setText(text);
        textField.setToolTipText(text);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new DateFunctionTextField());
        frame.pack();
        frame.setVisible(true);
    }
}
