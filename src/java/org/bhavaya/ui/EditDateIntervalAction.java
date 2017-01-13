package org.bhavaya.ui;

import org.bhavaya.util.DateFunctionInterval;
import org.bhavaya.util.RelativeDateFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public abstract class EditDateIntervalAction extends AuditedAbstractAction {

    private String editorTitle;
    private DateFunctionInterval interval;
    private String descriptionText;

    public EditDateIntervalAction(String actionName, String editorTitle, String descriptionText, DateFunctionInterval interval) {
        super(actionName);
        this.editorTitle = editorTitle;
        this.descriptionText = descriptionText;
        this.interval = interval;
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

        final DateFunctionPanel fromDatePanel = new DateFunctionPanel(RelativeDateFunction.PREPOSITION_BEFORE);
        fromDatePanel.setValue(interval.getStartDateFunction());
        fromDatePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "From"));

        final DateFunctionPanel toDatePanel = new DateFunctionPanel(RelativeDateFunction.PREPOSITION_AFTER);
        toDatePanel.setValue(interval.getEndDateFunction());
        toDatePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "To"));

        JPanel popupDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        popupDatePanel.add(fromDatePanel);
        popupDatePanel.add(toDatePanel);

        JPanel popupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        popupButtonPanel.add(new JButton(new AuditedAbstractAction("OK") {
            public void auditedActionPerformed(ActionEvent e) {
                interval = new DateFunctionInterval(fromDatePanel.getValue(), toDatePanel.getValue());
                update(interval);
                dialog.dispose();
            }
        }));

        popupButtonPanel.add(new JButton(new AuditedAbstractAction("Cancel") {
            public void auditedActionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        }));

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
     * @param interval
     */
    protected abstract void update(DateFunctionInterval interval);

    public String getEditorTitle() {
        return editorTitle;
    }

    public DateFunctionInterval getInterval() {
        return interval;
    }

    public String getDescriptionText() {
        return descriptionText;
    }
}
