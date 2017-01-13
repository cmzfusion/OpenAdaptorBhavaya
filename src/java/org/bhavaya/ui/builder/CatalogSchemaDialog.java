/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui.builder;

import org.bhavaya.beans.generator.Application;
import org.bhavaya.db.CatalogSchema;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class CatalogSchemaDialog extends JDialog {
    private static final Log log = Log.getCategory(CatalogSchemaDialog.class);

    public CatalogSchemaDialog(Frame owner, Application application) throws HeadlessException {
        super(owner, "Select Catalog/Schema", true);

        String error = null;
        CatalogSchema[] catalogSchemas = null;
        Connection connection = null;
        try {
            connection = application.newConnection();
            catalogSchemas = DBUtilities.getCatalogSchemas(connection);
        } catch (SQLException e) {
            log.error(e);
            error = e.getMessage();
        } finally {
            DBUtilities.close(connection);
        }


        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        JButton cancelButton = new JButton(new CloseAction(this, "Cancel"));
        buttonPanel.add(cancelButton);

        JList list = null;
        if (catalogSchemas != null) {
            list = new JList(catalogSchemas);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            contentPanel.add(new JScrollPane(list), BorderLayout.CENTER);

            JButton okButton = new JButton(new OkAction(application, list, this));
            buttonPanel.add(okButton);
        } else {
            JLabel errorLabel = new JLabel(error);
            errorLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            contentPanel.add(errorLabel, BorderLayout.CENTER);
        }

        setContentPane(contentPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();

        if (list != null) {
            setSize(getWidth(), (int) (UIUtilities.getDefaultScreenSize().getHeight() * 0.75));
            if (application.getCatalogSchema() != null) list.setSelectedValue(application.getCatalogSchema(), true);
        }
        UIUtilities.centreInContainer(owner, this, 0, 0);
    }

    private static class OkAction extends AbstractAction {
        private Application application;
        private JList list;
        private JDialog dialog;

        public OkAction(Application application, JList list, JDialog dialog) {
            super("Ok");
            this.application = application;
            this.list = list;
            this.dialog = dialog;
        }

        public void actionPerformed(ActionEvent e) {
            CatalogSchema catalogSchema = (CatalogSchema) list.getSelectedValue();
            application.setCatalogSchema(catalogSchema);
            dialog.dispose();
        }
    }
}
