package org.bhavaya.ui;

import org.bhavaya.util.DateFunction;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public abstract class EditDateFunctionAction extends AuditedAbstractAction {

    private String editorTitle;
    private DateFunction dateFunction;
    private String descriptionText;
    private List defaultDateFunctions;
    private DateFunction referenceDate;

    public EditDateFunctionAction(String actionName, String editorTitle, String descriptionText, DateFunction dateFunction) {
        this(actionName, editorTitle, descriptionText, dateFunction, null, SymbolicDateFunction.TODAY_DATEFUNCTION);
    }

    public EditDateFunctionAction(String actionName, String editorTitle, String descriptionText, DateFunction dateFunction, List defaultDateFunctions, DateFunction referenceDate) {
        super(actionName);
        this.editorTitle = editorTitle;
        this.descriptionText = descriptionText;
        this.dateFunction = dateFunction;
        this.defaultDateFunctions = defaultDateFunctions;
        this.referenceDate = referenceDate;
    }

    public void auditedActionPerformed(ActionEvent e) {
        Window windowParent = UIUtilities.getWindowParent((Component) e.getSource());
        final GenericDialog dialog;
        if (windowParent instanceof Frame) {
            dialog = new GenericDialog((Frame)windowParent, editorTitle, false);
        } else {
            dialog = new GenericDialog((Dialog)windowParent, editorTitle, false);
        }
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final DateFunctionPanel datePanel = new DateFunctionPanel(defaultDateFunctions, referenceDate);
        datePanel.setValue(getDateFunction());

        JPanel popupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        popupButtonPanel.add(new JButton(new AuditedAbstractAction("OK") {
            public void auditedActionPerformed(ActionEvent e) {
                dateFunction = datePanel.getValue();
                update(dateFunction);
                dialog.dispose();
            }
        }));

        popupButtonPanel.add(new JButton(new AuditedAbstractAction("Cancel") {
            public void auditedActionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        }));

        JPanel popupDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        popupDatePanel.add(datePanel);

        Container popupContents = dialog.getContentPane();
        popupContents.setLayout(new BorderLayout());
        popupContents.add(popupDatePanel, BorderLayout.CENTER);
        popupContents.add(popupButtonPanel, BorderLayout.SOUTH);

        if (descriptionText != null) {
            JLabel descriptionLabel = new JLabel(descriptionText);
            descriptionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            popupContents.add(descriptionLabel, BorderLayout.NORTH);
        }

        dialog.pack();
        UIUtilities.centreInContainer(windowParent, dialog, 0, 0);
        dialog.setVisible(true);
    }

    /**
     * Implement this to update your data.
     *
     * @param dateFunction
     */
    protected abstract void update(DateFunction dateFunction);

    public String getEditorTitle() {
        return editorTitle;
    }

    public DateFunction getDateFunction() {
        return dateFunction;
    }

    public String getDescriptionText() {
        return descriptionText;
    }
}
