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
import org.bhavaya.db.OracleDatabaseDialect;
import org.bhavaya.db.SqlServerDatabaseDialect;
import org.bhavaya.db.SybaseDatabaseDialect;
import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.DecimalTextField;
import org.bhavaya.ui.RowLayout;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.adaptor.Adapter;
import org.bhavaya.util.IOUtilities;
import org.bhavaya.util.Transform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class ApplicationForm extends JPanel {
    private Adapter adapter;
    private static final Map dialectToDefaultDatabaseDriver = new HashMap();
    private static final Map dialectToDatabaseUrlTemplate = new HashMap();
    private static File lastFileChooserFile;

    static {
        dialectToDatabaseUrlTemplate.put(SybaseDatabaseDialect.class.getName(), "jdbc:sybase:Tds:host:port");
        dialectToDatabaseUrlTemplate.put(SqlServerDatabaseDialect.class.getName(), "jdbc:inetdae7:host:port");
        dialectToDatabaseUrlTemplate.put(OracleDatabaseDialect.class.getName(), "jdbc:oracle:thin:@host:port:sid");

        dialectToDefaultDatabaseDriver.put(SybaseDatabaseDialect.class.getName(), "com.sybase.jdbc2.jdbc.SybDriver");
        dialectToDefaultDatabaseDriver.put(SqlServerDatabaseDialect.class.getName(), "com.inet.tds.TdsDriver");
        dialectToDefaultDatabaseDriver.put(OracleDatabaseDialect.class.getName(), "oracle.jdbc.OracleDriver");
    }

    public void displayForm(JFrame frame, Component source, Action runAction) {
        JButton runButton = new JButton(runAction);
        JButton cancelButton = new JButton(new CloseAction(frame, "Cancel"));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(runButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(this, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, UIUtilities.customForwardTraversalKeystrokes);
        frame.setResizable(false);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        UIUtilities.centreInContainer(UIUtilities.getWindowParent(source), frame, 40, 0);
        frame.setVisible(true);
    }


    public ApplicationForm(final Application application, boolean create) {
        adapter = new Adapter(application);

        Transform trimTransform = new TrimTransform();

        JTextField id = new JTextField();
        adapter.bind("id", id, "text", trimTransform, "text", trimTransform);

        JTextField name = new JTextField();
        adapter.bind("name", name, "text", trimTransform, "text", trimTransform);

        String[] databaseDialects = new String[]{org.bhavaya.db.DefaultDatabaseDialect.class.getName(),
                                                 org.bhavaya.db.SybaseDatabaseDialect.class.getName(),
                                                 org.bhavaya.db.OracleDatabaseDialect.class.getName(),
                                                 org.bhavaya.db.SqlServerDatabaseDialect.class.getName()};
        JComboBox databaseDialect = new JComboBox(databaseDialects);
        adapter.bind("databaseDialect", databaseDialect, "selectedItem");

        JTextField databaseDriver = new JTextField();
        adapter.bind("databaseDriver", databaseDriver, "text", trimTransform, "text", trimTransform);

        if (create) {
            adapter.addPropertyChangeListener("databaseDialect", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    String databaseDialect = (String) evt.getNewValue();
                    String defaultDatabaseDriver = (String) dialectToDefaultDatabaseDriver.get(databaseDialect);
                    if (defaultDatabaseDriver != null) application.setDatabaseDriver(defaultDatabaseDriver);

                    String databaseUrlTemplate = (String) dialectToDatabaseUrlTemplate.get(databaseDialect);
                    if (databaseUrlTemplate != null) application.setDatabaseUrl(databaseUrlTemplate);
                }
            });
        }

        JTextField databaseUrl = new JTextField();
        adapter.bind("databaseUrl", databaseUrl, "text", trimTransform, "text", trimTransform);

        JTextField catalogSchema = new JTextField();
        adapter.bindOneWayBeanToGui("catalogSchema", catalogSchema, "text", Transform.OBJECT_TO_STRING);
        catalogSchema.setEnabled(false);

        JButton setCatalogSchemaButton = new JButton(new SetCatalogSchemaAction(application));
        setCatalogSchemaButton.setPreferredSize(new Dimension(20, 20));

        Box catalogSchemaComponent = new Box(BoxLayout.X_AXIS);
        catalogSchemaComponent.add(catalogSchema);
        catalogSchemaComponent.add(setCatalogSchemaButton);

        JTextField databaseUser = new JTextField();
        adapter.bind("databaseUser", databaseUser, "text", trimTransform, "text", trimTransform);

        JTextField databasePassword = new JTextField();
        adapter.bind("databasePassword", databasePassword, "text", trimTransform, "text", trimTransform);

        JTextField databaseDriverClasspath = new JTextField();
        adapter.bind("databaseDriverClasspath", databaseDriverClasspath, "text", trimTransform, "text", trimTransform);

        JButton fileChooserButton = new JButton(new AbstractAction("...") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
                fileChooser.setDialogTitle("Database driver classpath");
                setInitalDirectory(fileChooser, application);
                javax.swing.filechooser.FileFilter filter = new LibraryFileFilter();
                fileChooser.setFileFilter(filter);

                int returnVal = fileChooser.showOpenDialog(UIUtilities.getWindowParent((Component) e.getSource()));

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    application.setDatabaseDriverClasspath(fileChooser.getSelectedFile().getAbsolutePath());
                    lastFileChooserFile = fileChooser.getSelectedFile();
                }
            }
        });
        fileChooserButton.setPreferredSize(new Dimension(20, 20));
        fileChooserButton.setFocusable(false);

        Box databaseDriverClasspathComponent = new Box(BoxLayout.X_AXIS);
        databaseDriverClasspathComponent.add(databaseDriverClasspath);
        databaseDriverClasspathComponent.add(fileChooserButton);

        DecimalTextField maxMemory = new DecimalTextField("####0", 5);
        adapter.bind("maxMemory", maxMemory, "value");

        RowLayout rowLayout = new RowLayout(600, 10);
        setLayout(rowLayout);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        RowLayout.Row row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Id:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(id), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Name:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(name), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database Dialect:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databaseDialect), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database Driver:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databaseDriver), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database Url:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databaseUrl), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database User:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databaseUser), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database Password:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databasePassword), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Database Driver Classpath:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(databaseDriverClasspathComponent), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Catalog/Schema:")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(catalogSchemaComponent), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);
        row.addComponent(add(new JLabel("Maximum Memory (mb):")), new RowLayout.RelativeWidthConstraint(0.3));
        row.addComponent(add(maxMemory), new RowLayout.RelativeWidthConstraint(0.7));
        rowLayout.addRow(row);

    }

    private void setInitalDirectory(JFileChooser fileChooser, final Application application) {
        String databaseDriverClasspath = application.getDatabaseDriverClasspath();
        if (databaseDriverClasspath != null) {
            File file = new File(databaseDriverClasspath);
            if (file.exists()) {
                fileChooser.setSelectedFile(file);
            }
        }

        if (fileChooser.getSelectedFile() == null && lastFileChooserFile != null) {
            fileChooser.setSelectedFile(lastFileChooserFile);
        }
    }

    public final void dispose() {
        if (adapter != null) adapter.dispose();
    }

    private static class TrimTransform implements Transform {
        public Object execute(Object sourceData) {
            if (sourceData == null) return null;
            return ((String) sourceData).trim();
        }
    }

    private static class LibraryFileFilter extends javax.swing.filechooser.FileFilter {
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".zip");
        }

        public String getDescription() {
            return "Libraries (*.jar, *.zip)";
        }
    }

    private static class SetCatalogSchemaAction extends AbstractAction {
        private Application application;

        public SetCatalogSchemaAction(Application application) {
            super("...");
            this.application = application;
        }

        public void actionPerformed(ActionEvent e) {
            JFrame owner = UIUtilities.getFrameParent((Component) e.getSource());
            CatalogSchemaDialog catalogSchemaDialog = new CatalogSchemaDialog(owner, application);
            catalogSchemaDialog.show();
        }

    }

}
