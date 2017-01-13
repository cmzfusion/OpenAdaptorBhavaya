package org.bhavaya.ui;

import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * It's often useful to find out if logging is a result of a user action.  This probably doesn't
 * cover all cases, but hopefully will improve the transparency of the logs.
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public abstract class AuditedAbstractAction extends AbstractAction {
    private static final Log log = Log.getCategory(AuditedAbstractAction.class);

    private String auditName;

    protected AuditedAbstractAction() {
    }

    protected AuditedAbstractAction(String name) {
        super(name);
    }

    protected AuditedAbstractAction(String name, String auditName) {
        super(name);
        this.auditName = auditName;
    }

    protected AuditedAbstractAction(String name, Icon icon) {
        this(name, icon, null);
    }

    protected AuditedAbstractAction(String name, Icon icon, String auditName) {
        super(name, icon);
        this.auditName = auditName;
    }

    public final void actionPerformed(ActionEvent e) {
        long startTime = System.currentTimeMillis();
        log.info("User initiated action: " + getName());
        auditedActionPerformed(e);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Action (" + getName() + ") completed in: " + duration + "ms");
    }

    private String getName() {
        //get the auditName, failing that the NAME, failing that the class name
        String result = auditName == null ? (String) getValue(Action.NAME) : auditName;
        result = result == null ? getClass().getName() : result;
        return result;
    }

    protected abstract void auditedActionPerformed(ActionEvent e);

    public String getAuditName() {
        return auditName;
    }

    public void setAuditName(String auditName) {
        this.auditName = auditName;
    }
}
