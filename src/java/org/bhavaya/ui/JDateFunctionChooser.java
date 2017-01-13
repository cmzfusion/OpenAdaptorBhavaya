package org.bhavaya.ui;

import org.bhavaya.util.DateFunction;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class JDateFunctionChooser extends JPanel {
    private DateFunction currentDateFunction;
    private DateFunctionPanel datePanel;

    private JDateFunctionChooser(DateFunction currentDateFunction) {
        this.currentDateFunction = currentDateFunction;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        datePanel = new DateFunctionPanel(null, SymbolicDateFunction.TODAY_DATEFUNCTION);
        datePanel.setValue(currentDateFunction);
        add(datePanel);
    }

    public DateFunction getValue() {
        return currentDateFunction;
    }

    public void updateDate() {
        DateFunction newDateFunction = datePanel.getValue();
        if (newDateFunction != null) {
            currentDateFunction = newDateFunction;
        }
    }

    public static DateFunction showDialog(Component parent, String title,
                                  DateFunction dateFunction) {
        Window windowParent = UIUtilities.getWindowParent(parent);

        final JDialog dialog;
        if (windowParent instanceof Frame) {
            dialog = new JDialog((Frame) windowParent, title, true);
        } else {
            dialog = new JDialog((Dialog) windowParent, title, true);
        }
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final JDateFunctionChooser dateFunctionChooser = new JDateFunctionChooser(dateFunction);

        JPanel popupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(new AuditedAbstractAction("OK") {
            public void auditedActionPerformed(ActionEvent e) {
                dateFunctionChooser.updateDate();
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(okButton);
        popupButtonPanel.add(okButton);

        CloseAction cancelAction = new CloseAction(dialog, "Cancel");
        JButton cancelButton = new JButton(cancelAction);
        popupButtonPanel.add(cancelButton);

        KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke((char) KeyEvent.VK_ESCAPE);
        InputMap inputMap = cancelButton.getInputMap(JComponent.
                WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = cancelButton.getActionMap();
        if (inputMap != null && actionMap != null) {
            inputMap.put(cancelKeyStroke, "cancel");
            actionMap.put("cancel", cancelAction);
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(dateFunctionChooser, BorderLayout.CENTER);
        panel.add(popupButtonPanel, BorderLayout.SOUTH);
        dialog.getContentPane().add(panel);

        dialog.pack();
        UIUtilities.centreInContainer(windowParent, dialog, 0, 0);
        dialog.setVisible(true);

        return dateFunctionChooser.getValue();
    }
}