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
import org.bhavaya.ui.BeanActionFactory;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.AcceleratorAction;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.view.ActionGroup;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class ApplicationTable extends AnalyticsTable {
    private static final Log log = Log.getCategory(ApplicationTable.class);
    private static final Set validColumnLocators = new LinkedHashSet();

    static {
        validColumnLocators.add("id");
        validColumnLocators.add("name");
        validColumnLocators.add("databaseDriver");
        validColumnLocators.add("databaseUrl");
        validColumnLocators.add("catalogSchema");
        validColumnLocators.add("lastGeneration");
    }

    public ApplicationTable(final State state, String baseDir, String urlClassLoaderHost, int urlClassLoaderPort) {
        super(new BeanCollectionTableModel(state.getApplications(), false), true);
        final AnalyticsTableModel analyticsTableModel = getAnalyticsTableModel();
        final BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) analyticsTableModel.getSourceModel();
        beanCollectionTableModel.removeAllColumnLocators();

        Runnable configureModelRunnable = new Runnable() {
            public void run() {
                if (state.getTableViewConfiguration() == null) {
                    for (Iterator iterator = validColumnLocators.iterator(); iterator.hasNext();) {
                        String locator = (String) iterator.next();
                        beanCollectionTableModel.addColumnLocator(locator);
                    }
                    analyticsTableModel.addSortingColumn(beanCollectionTableModel.getColumnKey(0), false);
                } else {
                    setViewConfiguration(state.getTableViewConfiguration());
                    java.util.List columnLocators = beanCollectionTableModel.getColumnLocators();

                    for (Iterator iterator = validColumnLocators.iterator(); iterator.hasNext();) {
                        String locator = (String) iterator.next();
                        if (!columnLocators.contains(locator)) beanCollectionTableModel.addColumnLocator(locator);
                    }

                    for (Iterator iterator = columnLocators.iterator(); iterator.hasNext();) {
                        String locator = (String) iterator.next();
                        if (!validColumnLocators.contains(locator)) {
                            beanCollectionTableModel.removeColumnLocator(locator);
                        }
                    }
                }
            }
        };
        EventQueue.invokeLater(configureModelRunnable);

        setBeanActionFactory(new ApplicationsBeanActionFactory(this, baseDir, state, urlClassLoaderHost, urlClassLoaderPort));
    }

    private static class ApplicationsBeanActionFactory implements BeanActionFactory {
        private ApplicationTable table;
        private String baseDir;
        private State state;
        private String urlClassLoaderHost;
        private int urlClassLoaderPort;

        public ApplicationsBeanActionFactory(ApplicationTable table, String baseDir, State state, String urlClassLoaderHost,  int urlClassLoaderPort) {
            this.table = table;
            this.baseDir = baseDir;
            this.state = state;
            this.urlClassLoaderHost = urlClassLoaderHost;
            this.urlClassLoaderPort = urlClassLoaderPort;
        }

        public ActionGroup getActions(Object[] beans) {
            ActionGroup actions = new ActionGroup("");

            if (beans.length == 1) {
                Application application = (Application) beans[0];
                actions.addAction(new RunApplicationAction(application, state, baseDir, urlClassLoaderHost, urlClassLoaderPort));
                actions.addAction(new GenerateApplicationAction(application, baseDir));
                actions.addAction(new EditApplicationAction(application, state.getApplications()));
                actions.addAction(new CopyApplicationAction(application, state.getApplications()));
                actions.addAction(new RemoveApplicationAction(table, application, state.getApplications(), baseDir));
            } else if (beans.length > 1) {
            }

            return actions;
        }

        public Action getAction(Object bean, String beanPath) {
            return new EditApplicationAction((Application) bean, state.getApplications());
        }

        public AcceleratorAction[] getAcceleratorActions() {
            return null;
        }
    }

    private static class GenerateApplicationAction extends AbstractAction {
        private Application application;
        private String baseDir;

        public GenerateApplicationAction(Application application, String baseDir) {
            super("Generate", ImageIconCache.getImageIcon(Builder.GENERATE_ICON));
            this.application = application;
            this.baseDir = baseDir;
        }

        public void actionPerformed(final ActionEvent e) {
            String title = "Generating " + application.getId();
            log.info(title);
            Window owner = UIUtilities.getWindowParent((Component) e.getSource());
            UIUtilities.runTaskWithProgressDialog(owner, "", new Task(title) {
                public void run() throws Throwable {
                    application.generate(baseDir);
                }
            });
        }
    }

    private static class RunApplicationAction extends AbstractAction {
        private Application application;
        private String baseDir;
        private State state;
        private String urlClassLoaderHost;
        private int urlClassLoaderPort;

        public RunApplicationAction(Application application, State state, String baseDir, String urlClassLoaderHost, int urlClassLoaderPort) {
            super("Run", ImageIconCache.getImageIcon(Builder.RUN_ICON));
            this.application = application;
            this.baseDir = baseDir;
            this.state = state;
            this.urlClassLoaderHost = urlClassLoaderHost;
            this.urlClassLoaderPort = urlClassLoaderPort;
        }

        public boolean isEnabled() {
            return application.isGenerated(baseDir);
        }

        public void actionPerformed(final ActionEvent e) {
            Window owner = UIUtilities.getWindowParent((Component) e.getSource());
            try {
                Process process = application.run(baseDir, urlClassLoaderHost, urlClassLoaderPort);
                Rectangle bounds = state.getProcessFrameConfig() != null ? state.getProcessFrameConfig().getFrameBounds() : null;
                ProcessFrame processFrame = new ProcessFrame(process, application.getId(), owner, bounds){
                    public void dispose() {
                        state.setProcessFrameConfig(new FrameConfig(getBounds()));
                        super.dispose();
                    }
                };
                processFrame.setIconImage(Builder.FRAME_ICON.getImage());
                processFrame.setVisible(true);
            } catch (IOException e1) {
                log.error(e1);
                String message = "Could not run " + application.getId();
                JOptionPane.showMessageDialog(owner, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private static class CopyApplicationAction extends AbstractAction {
        private Application application;
        private ApplicationCollection applications;
        private Application copy;

        public CopyApplicationAction(Application application, ApplicationCollection applications) {
            super("Copy", ImageIconCache.getImageIcon(Builder.COPY_ICON));
            this.application = application;
            this.applications = applications;
        }

        public void actionPerformed(ActionEvent e) {
            copy = application.copy();
            final ApplicationForm form = new ApplicationForm(copy, true);
            String title = "Copy Application";
            JFrame frame = new JFrame(title) {
                public void dispose() {
                    form.dispose();
                    super.dispose();
                }
            };
            frame.setIconImage(Builder.FRAME_ICON.getImage());
            Action runAction = new CreateApplicationAction(frame, copy, applications);
            Component owner = (Component) e.getSource();
            form.displayForm(frame, owner, runAction);
        }
   }

    private static class EditApplicationAction extends AbstractAction {
        private ApplicationCollection applications;
        private Application originalApplication;
        private Application application;

        public EditApplicationAction(Application application, ApplicationCollection applications) {
            super("Edit", ImageIconCache.getImageIcon(Builder.EDIT_ICON));
            this.applications = applications;
            this.originalApplication = application;
            this.application = (Application) BeanUtilities.verySlowDeepCopy(application);
        }

        public void actionPerformed(ActionEvent e) {
            final ApplicationForm form = new ApplicationForm(application, false);
            String title = "Edit Application";
            JFrame frame = new JFrame(title) {
                public void dispose() {
                    form.dispose();
                    super.dispose();
                }
            };
            frame.setIconImage(Builder.FRAME_ICON.getImage());
            Action runAction = new AmendApplicationAction(frame, originalApplication, application, applications);
            Component owner = (Component) e.getSource();
            form.displayForm(frame, owner, runAction);
        }

    }

    private static class RemoveApplicationAction extends AbstractAction {
        private ApplicationTable table;
        private Application application;
        private ApplicationCollection applications;
        private String baseDir;

        public RemoveApplicationAction(ApplicationTable table, Application application, ApplicationCollection applications, String baseDir) {
            super("Remove", ImageIconCache.getImageIcon(Builder.REMOVE_ICON));
            this.table = table;
            this.application = application;
            this.applications = applications;
            this.baseDir = baseDir;
        }

        public void actionPerformed(ActionEvent e) {
            Component owner = UIUtilities.getWindowParent((Component) e.getSource());

            int ret = JOptionPane.showConfirmDialog(owner, "Remove " + application.getId() + "?", "Remove", JOptionPane.OK_CANCEL_OPTION);
            if (ret == JOptionPane.OK_OPTION) {
                table.getSelectionModel().clearSelection();
                applications.remove(application, baseDir);
            }
        }
    }

}
