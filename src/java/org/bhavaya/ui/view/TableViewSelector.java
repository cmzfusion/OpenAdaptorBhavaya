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

package org.bhavaya.ui.view;

import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.ui.*;
import org.bhavaya.util.*;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.18.4.2 $
 */
public class TableViewSelector extends JPanel {
    private static final Log log = Log.getCategory(TableViewSelector.class);

    private static final String REMOVE_ICON = "remove.gif";
    private static final String UPDATE_VIEWS_ICON = "updateViews.gif";
    private static final String SAVE_NEW_ICON = "add.gif";
    private static final String EDIT_ICON = "edit.gif";
    private static final String EMAIL_ICON = "email.png";
    private static final String ACTIVE_LIST_ELEMENT_ICON = "active_list_element.gif";
    private static final String INACTIVE_LIST_ELEMENT_ICON = "inactive_list_element.gif";
    private JList viewConfigsList;
    private ConfigListModel configListModel;
    private TableView tableView;
    private Class<?> recordType;
    private TableViewConfigurationMap userViewConfigurationMap;
    //private FixedViewConfigurationMap fixedViewConfigurationMap;

    private JToggleButton editButton;

    public TableViewSelector(final TableView tableView) {
        super(new BorderLayout());

        this.tableView = tableView;
        this.recordType = tableView.getRecordType();

        userViewConfigurationMap = tableView.getUserViewConfigurationMap();
//        fixedViewConfigurationMap = tableView.getFixedViewConfigurationMap();

        configListModel = tableView.getViewConfigListModel();
        userViewConfigurationMap.addMapChangedListener(configListModel);

        viewConfigsList = new JList(configListModel);
        viewConfigsList.setName(tableView.getName()+"-TableViewSelector-JList");
        viewConfigsList.setCellRenderer( tableView.getConfigNameCellRenderer());
        viewConfigsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(viewConfigsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        viewConfigsList.setSelectedValue(tableView.getViewConfigurationId(), true); // initialise state

        viewConfigsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!tableView.getViewConfigurationId().equals(viewConfigsList.getSelectedValue())) {
                    tableView.setViewConfigurationIdAndApply((String) viewConfigsList.getSelectedValue());
                    viewConfigsList.repaint();
                }
            }
        });

        TitlePanel titlePanel = new TitlePanel(BeanCollectionGroup.getDefaultInstance(recordType).getPluralDisplayName() + " Configurations");

        JButton removeButton = new JButton(new RemoveAction());
        removeButton.setFocusable(false);
        removeButton.setToolTipText("Remove the selected view");

        editButton = new JToggleButton(new EditAction());
        editButton.setFocusable(false);
        editButton.setToolTipText("Edit the selected view");

        JButton saveButton = new JButton(new SaveViewAction());
        saveButton.setFocusable(false);
        saveButton.setToolTipText("Update all views using this configuration");

        JButton saveNewButton = new JButton(new SaveNewViewAction());
        saveNewButton.setFocusable(false);
        saveNewButton.setToolTipText("Save view under a new name");

        JButton exportButton = new JButton(new ExportAction());
        exportButton.setFocusable(false);
        exportButton.setToolTipText("Save configuration to file");

        JButton emailViewButton = new JButton(new EmailViewConfigsAction());
        emailViewButton.setFocusable(false);
        emailViewButton.setToolTipText("Send configuration to support");

        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.add(saveNewButton);
        toolBar.add(removeButton);
        toolBar.add(editButton);
        toolBar.add(saveButton);
        toolBar.add(exportButton);
        toolBar.add(emailViewButton);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(toolBar, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(listScrollPane, BorderLayout.CENTER);
    }

    public void saveNewView() {
        String viewName = JOptionPane.showInputDialog(tableView.getComponent(), "Enter new view name");
        if (viewName != null && viewName.length() > 0) {
            if (userViewConfigurationMap.getViewConfiguration(viewName) != null) {
                int result = JOptionPane.showConfirmDialog(tableView.getComponent(),
                        "Are you sure you want to overwrite existing configuration: \"" + viewName + "\"",
                        "Confirm overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            userViewConfigurationMap.setViewConfiguration(viewName, tableView.getViewConfiguration(), true);
            tableView.setViewConfigurationIdAndApply(viewName, true);
            viewConfigsList.setSelectedValue(tableView.getViewConfigurationId(), true);
        }
    }


    public void dispose() {
        userViewConfigurationMap.removeMapChangedListener( configListModel );
    }

//    private class ViewConfigurationModel implements ConfigListModel {
//        private ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>();
//        private String[] viewConfigurationIds;
//
//        public ViewConfigurationModel() {
//            viewConfigurationIds = Utilities.appendAndSortArrays(userViewConfigurationMap.getNames(), fixedViewConfigurationMap.getNames());
//        }
//
//        @Override
//        public void mapChanged() {
//            viewConfigurationIds = Utilities.appendAndSortArrays(userViewConfigurationMap.getNames(), fixedViewConfigurationMap.getNames());
//            fireChanged();
//        }
//
//        public void addListDataListener(ListDataListener l) {
//            listeners.add(l);
//        }
//
//        public Object getElementAt(int index) {
//            return viewConfigurationIds[index];
//        }
//
//        public int getSize() {
//            return viewConfigurationIds.length;
//        }
//
//        public void removeListDataListener(ListDataListener l) {
//            listeners.remove(l);
//        }
//
//        private void fireChanged() {
//            for (ListDataListener listener : listeners) {
//                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
//            }
//        }
//    }

    private void addListListeners(Action action) {
        configListModel.addListDataListener(new ListActionAdapter(action));
        viewConfigsList.addListSelectionListener(new ActionEnabledListSelectionListener(action));
    }


    private class SaveViewAction extends AuditedAbstractAction {
        public SaveViewAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(UPDATE_VIEWS_ICON));
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            TableView.setSaveAll(false);
            tableView.saveCurrentView();
        }

        public boolean isEnabled() {
            return !FixedViewConfigurationMap.isFixedView((String) viewConfigsList.getSelectedValue());
        }
    }

    private class SaveNewViewAction extends AuditedAbstractAction {
        public SaveNewViewAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(SAVE_NEW_ICON));
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            saveNewView();

        }

        @Override
        public boolean isEnabled() {
            return tableView.canCopyView((String) viewConfigsList.getSelectedValue());
        }
    }

    private class RemoveAction extends AuditedAbstractAction {
        public RemoveAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(REMOVE_ICON));
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            String viewConfigurationId = (String) configListModel.getElementAt(viewConfigsList.getSelectedIndex());
            if (viewConfigurationId.equals(userViewConfigurationMap.getDefaultViewConfigurationId())) {
                JOptionPane.showMessageDialog(tableView.getComponent(), "Cannot delete the default view");
            } else {
                viewConfigsList.setSelectedIndex(0);
                userViewConfigurationMap.removeViewConfiguration(viewConfigurationId);
            }
        }

        public boolean isEnabled() {
            return configListModel.getSize() > 0 && !FixedViewConfigurationMap.isFixedView((String) viewConfigsList.getSelectedValue());
        }
    }

    private class EditAction extends AuditedAbstractAction {
        public EditAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(EDIT_ICON));
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            tableView.openEditPanel();
        }

        public boolean isEnabled() {
            return configListModel.getSize() > 0 && !FixedViewConfigurationMap.isFixedView((String) viewConfigsList.getSelectedValue());
        }

    }

    private class ExportAction extends AuditedAbstractAction {
        public ExportAction() {
            putValue(Action.NAME, "Export...");
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            String viewConfigurationId = (String) viewConfigsList.getSelectedValue();
            TableViewConfiguration tableViewConfiguration = userViewConfigurationMap.getViewConfiguration(viewConfigurationId);
            if (tableViewConfiguration == null) return;

            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException
            File file = null;

            try {
                JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
                fileChooser.setDialogTitle("Export View Configuration");
                fileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
                    }

                    public String getDescription() {
                        return "Bhavaya compatible object format (*.xml)";
                    }
                });

                int returnVal = fileChooser.showSaveDialog(UIUtilities.getWindowParent((Component) e.getSource()));
                if (returnVal != JFileChooser.APPROVE_OPTION) return;
                file = fileChooser.getSelectedFile();
            } finally {
                System.setSecurityManager(backup);
            }

            try {
                Configuration configuration = new Configuration();
                configuration.putObject("1", recordType);
                configuration.putObject("2", viewConfigurationId);
                configuration.putObject("3", tableViewConfiguration);
                Configuration.FileConfigurationSourceSink.saveConfiguration(file.getAbsolutePath(), configuration);
            } catch (Exception e1) {
                log.error(e1);
            }
        }

        public boolean isEnabled() {
            return configListModel.getSize() > 0 && !FixedViewConfigurationMap.isFixedView((String) viewConfigsList.getSelectedValue());
        }
    }

    public static class ImportAction extends AuditedAbstractAction {
        public ImportAction() {
            super("Import View Configurations...");
        }

        public void auditedActionPerformed(ActionEvent e) {
            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException
            File[] files = null;

            try {
                JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setDialogTitle("Import View Configurations");
                fileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
                    }

                    public String getDescription() {
                        return "Bhavaya compatible object format (*.xml)";
                    }
                });

                int returnVal = fileChooser.showOpenDialog(UIUtilities.getWindowParent((Component) e.getSource()));
                if (returnVal != JFileChooser.APPROVE_OPTION) return;
                files = fileChooser.getSelectedFiles();
            } finally {
                System.setSecurityManager(backup);
            }

            for (File file : files) {
                try {
                    InputStream stream = new BufferedInputStream(new FileInputStream(file));
                    importViewConfiguration(stream);
                } catch (Exception e1) {
                    log.error(e1);
                }
            }
        }
    }

    public static void importViewConfiguration(InputStream stream) throws Exception {
        Configuration configuration = Configuration.FileConfigurationSourceSink.loadConfiguration(stream);
        if (configuration != null) {
            Class<?> recordType = configuration.getObject("1", null, Class.class);
            String tableViewConfigurationId = configuration.getObject("2", null, String.class);
            TableViewConfiguration tableViewConfiguration = configuration.getObject("3", null, TableViewConfiguration.class);
            if (recordType != null && tableViewConfigurationId != null && tableViewConfiguration != null) {
                TableViewConfigurationMap.getInstance(recordType).setViewConfiguration(tableViewConfigurationId, tableViewConfiguration, true);
            }
        }
    }

    private class EmailViewConfigsAction extends AuditedAbstractAction {
        public EmailViewConfigsAction() {
            putValue(Action.SMALL_ICON, ImageIconCache.getImageIcon(EMAIL_ICON));
            addListListeners(this);
        }

        public void auditedActionPerformed(ActionEvent e) {
            try {
                String viewConfigurationId = (String) viewConfigsList.getSelectedValue();
                TableViewConfiguration tableViewConfiguration = userViewConfigurationMap.getViewConfiguration(viewConfigurationId);
                Configuration configuration = new Configuration();
                configuration.putObject("1", recordType);
                configuration.putObject("2", viewConfigurationId);
                configuration.putObject("3", tableViewConfiguration);
                byte[] xmlData = configuration.convertToString().getBytes();

                Message mailMessage = EmailUtilities.getDefaultMimeMessage(ApplicationInfo.getInstance().getName() + " "
                        + BeanCollectionGroup.getDefaultInstance(recordType).getPluralDisplayName()
                        + " view email from " + ApplicationInfo.getInstance().getUsername());

                MimeBodyPart summaryBodyPart = new MimeBodyPart();
                summaryBodyPart.setContent("User view '" + viewConfigsList.getSelectedValue() + "' of type '" + recordType.getName() + "' attached.", "text/plain");
                summaryBodyPart.setHeader("Content-Type", "text/plain; charset=iso-8859-1");

                MimeBodyPart xmlAttachment = new MimeBodyPart();
                xmlAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(xmlData, "text/xml")));
                xmlAttachment.setFileName(FixedViewConfigurationMap.getFilename(recordType, viewConfigurationId));

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(summaryBodyPart);
                multipart.addBodyPart(xmlAttachment);

                mailMessage.setContent(multipart);
                EmailUtilities.sendMessage(mailMessage);
                log.info("Sent view configuration by email to: " + Utilities.asString(mailMessage.getAllRecipients(), ", "));
            } catch (Exception ex) {
                log.error("Unable to email view configuration", ex);
            }
        }

        public boolean isEnabled() {
            return viewConfigsList.getSelectedIndex() > -1 && !FixedViewConfigurationMap.isFixedView((String) viewConfigsList.getSelectedValue());
        }
    }

//    private class ConfigNameCellRenderer extends DefaultListCellRenderer {
//        private Font normalFont;
//        private Font selectedFont;
//        private Icon activeListElementIcon = ImageIconCache.getImageIcon(ACTIVE_LIST_ELEMENT_ICON);
//        private Icon inActiveListElementIcon = ImageIconCache.getImageIcon(INACTIVE_LIST_ELEMENT_ICON);
//
//        public ConfigNameCellRenderer() {
//            super();
//            normalFont = getFont().deriveFont(Font.PLAIN);
//            selectedFont = getFont().deriveFont(Font.BOLD);
//        }
//
//        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
//            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, isSelected);
//            String selectedViewConfigurationId = (String) value;
//            String viewConfigurationId = tableView.getViewConfigurationId();
//            if (FixedViewConfigurationMap.isFixedView(selectedViewConfigurationId)) {
//                label.setText(FixedViewConfigurationMap.getDisplayNameForViewId(selectedViewConfigurationId));
//                label.setForeground(Color.blue);
//            } else {
//                label.setForeground(Color.black);
//            }
//
//            boolean sameView = selectedViewConfigurationId.equals(viewConfigurationId);
//            Font font = sameView ? selectedFont : normalFont;
//            if (selectedViewConfigurationId.equals(userViewConfigurationMap.getDefaultViewConfigurationId())) {
//                font = font.deriveFont(Font.ITALIC | font.getStyle());
//            }
//
//            label.setFont(font);
//            label.setIcon(sameView ? activeListElementIcon : inActiveListElementIcon);
//
//            return label;
//        }
//    }

    private static class ListActionAdapter implements ListDataListener {
        private Action action;

        public ListActionAdapter(Action action) {
            this.action = action;
        }

        public void intervalAdded(ListDataEvent e) {
            update();
        }

        private void update() {
            action.setEnabled(action.isEnabled());
        }

        public void intervalRemoved(ListDataEvent e) {
            update();
        }

        public void contentsChanged(ListDataEvent e) {
            update();
        }
    }

    private static class ActionEnabledListSelectionListener implements ListSelectionListener {
        private Action action;

        public ActionEnabledListSelectionListener(Action action) {
            this.action = action;
            update();
        }

        public void valueChanged(ListSelectionEvent e) {
            update();
        }

        private void update() {
            action.setEnabled(action.isEnabled());
        }
    }
}
