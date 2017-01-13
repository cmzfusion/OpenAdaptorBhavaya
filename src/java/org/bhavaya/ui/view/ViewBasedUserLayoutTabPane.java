package org.bhavaya.ui.view;

import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.UserLayoutTabPane;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.Encoder;
import java.beans.Statement;
import java.util.*;

/**
 * it's a UserLayoutTabPane, Jim, but not as we know it.
 * <p/>
 * <p/>
 * Note: Sorry, this was done in a serious hurry, and I know there are aspects of the API and implementation that might
 * be rather odd. Unfortunately I can't sort it out, my only goal right now is to get it working.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.11.4.2 $
 */
public class ViewBasedUserLayoutTabPane extends UserLayoutTabPane {
    private Log log = Log.getCategory(ViewBasedUserLayoutTabPane.class);

    static {
        BeanUtilities.addPersistenceDelegate(ViewBasedUserLayoutTabPane.class, new ViewBasedUserLayoutTabPanePersistenceDelegate());
        BeanUtilities.addPersistenceDelegate(ViewBackedTabMetaData.class, new BhavayaPersistenceDelegate(new String[]{"view", "data"}));
    }

    private static final KeyStroke CLOSE_TAB_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    private HashMap viewsToMetaData = new LinkedHashMap();
    private boolean initialised = false;
    private RowLayoutHeaderPanelController controllerToApplyOnInit;

    public final void addView(View view) {
        addView(view, new ViewBackedTabMetaData(view, new HashMap(8)));
    }

    public final void addView(final View view, final ViewBackedTabMetaData viewBackedTabMetaData) {
        addView(view, viewBackedTabMetaData, true);
    }

    public void addView(final View view, final ViewBackedTabMetaData viewBackedTabMetaData, final boolean selectNewTab) {
        viewsToMetaData.put(view, viewBackedTabMetaData);
        historicalViews.add(view);

        if (initialised) {
            try {
                final Component viewComponent = view.getComponent();
                UIUtilities.runInDispatchThread(new Runnable() {
                    public void run() {
                        addTab(viewComponent, viewBackedTabMetaData);
                        if (selectNewTab) setSelectedComponent(viewComponent);

                        if(viewComponent instanceof JComponent){
                            JComponent jComp = (JComponent)viewComponent;
                            if( jComp.getActionMap().get("DISCARD") == null){
                                jComp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(CLOSE_TAB_KEYSTROKE,  "DISCARD");
                                jComp.getActionMap().put("DISCARD", new AbstractAction() {
                                    public void actionPerformed(ActionEvent e) {
                                         Workspace.getInstance().removeView(view);
                    }
                });
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                log.error(t);
                viewsToMetaData.remove(view);
            }
        }
    }

    public ViewBackedTabMetaData getTabMetaData(View view) {
        return (ViewBackedTabMetaData) viewsToMetaData.get(view);
    }

    public void setHeaderPanelController(final RowLayoutHeaderPanelController headerPanelController) {
        if (!initialised) {
            this.controllerToApplyOnInit = headerPanelController;
        } else {
            controllerToApplyOnInit = null;
            superSetHeaderPanelController(headerPanelController);
        }
    }

    public RowLayoutHeaderPanelController getHeaderPanelController() {
        if (!initialised) {
            return this.controllerToApplyOnInit;
        } else {
            return super.getHeaderPanelController();
        }
    }

    private void superSetHeaderPanelController(RowLayoutHeaderPanelController headerPanelController) {
        super.setHeaderPanelController(headerPanelController);
    }


    public Set getViews() {
        return viewsToMetaData.keySet();
    }

    public void removeView(View view) {
        viewsToMetaData.remove(view);
        //historicalViews.remove(view);
        removeTab(view);
    }

    /**
     * used to keep view.getComponent lazy until we are ready to display a UI
     */
    public void init() {
        if (!initialised) {
            initialised = true;
            for (Iterator iterator = getViews().iterator(); iterator.hasNext();) {
                View view = (View) iterator.next();
                ViewBackedTabMetaData tabMetaData = getTabMetaData(view);
                final Component viewComponent = view.getComponent();
                addTab(viewComponent, tabMetaData);
            }
            if (controllerToApplyOnInit != null) {
                setHeaderPanelController(this.controllerToApplyOnInit);
            }
        }
    }



    public void setSelectedComponent(Component component, Component oldComponent, boolean updateHistory) {
        super.setSelectedComponent(component, oldComponent, updateHistory);

        if (component != null) {
            ViewBackedTabMetaData viewBackedTabMetaData = (ViewBackedTabMetaData) getTabMetaData(component);
            log.info("Activating tab \""+viewBackedTabMetaData.getTitle()+"\"");
            viewBackedTabMetaData.lastActivatedTimestamp = System.currentTimeMillis();
            viewBackedTabMetaData.view.setLastActivated(new Date());
            if(updateHistory){
                historicalViews.add(viewBackedTabMetaData.view);
            }
        }
    }

    public static class ViewBackedTabMetaData extends UserLayoutTabPane.TabMetaData {
        private final View view;
        private long lastActivatedTimestamp = System.currentTimeMillis();

        /**
         * for the persister's eyes only
         *
         * @param view
         * @param data
         */
        public ViewBackedTabMetaData(View view, Map data) {
            super(data);
            this.view = view;
        }

        public String getTitle() {
            return view.getTabTitle();
        }

        public Icon getIcon() {
            return view.getViewContext() != null && view.getViewContext().getImageIcon() != null ? view.getViewContext().getImageIcon() : view.getImageIcon();
        }

        public String getToolTip() {
            if (Workspace.getInstance().isTabToolTipsOn()) {
                Describeable describeable = (Describeable) view;
                String tooltipText = describeable.getDescription();

                if (tooltipText != null) {
                    tooltipText = Utilities.wrapWithSplitOnNewLine(tooltipText, 80);
                    return "<HTML>" + tooltipText.replaceAll("\n", "<BR>") + "</HTML>";
                }
            }
            return super.getToolTip();
        }

        public View getView() {
            return view;
        }

        public boolean persistable() {
            return BeanUtilities.hasPersistenceDelegate(view.getClass());
        }
    }

    public static class ViewBasedUserLayoutTabPanePersistenceDelegate extends BhavayaPersistenceDelegate {
        protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            ViewBasedUserLayoutTabPane oldTabPane = ((ViewBasedUserLayoutTabPane) oldInstance);

            ArrayList metaDataList = getMetaDataToPersist(oldTabPane);

            // Sort so that LRU is first
            Collections.sort(metaDataList, new Comparator() {
                public int compare(Object o1, Object o2) {
                    ViewBackedTabMetaData meta1 = (ViewBackedTabMetaData) o1;
                    ViewBackedTabMetaData meta2 = (ViewBackedTabMetaData) o2;

                    return meta1.lastActivatedTimestamp == meta2.lastActivatedTimestamp
                            ? 0
                            : (meta1.lastActivatedTimestamp > meta2.lastActivatedTimestamp ? -1 : 1);
                }
            });

            for (Iterator iterator = metaDataList.iterator(); iterator.hasNext();) {
                ViewBackedTabMetaData tabMetaData = (ViewBackedTabMetaData) iterator.next();
                out.writeStatement(new Statement(oldTabPane, "addView", new Object[]{tabMetaData.getView(), tabMetaData}));

            }
            out.writeStatement(new Statement(oldTabPane, "setHeaderPanelController", new Object[]{oldTabPane.getHeaderPanelController()}));
        }

        private ArrayList getMetaDataToPersist(ViewBasedUserLayoutTabPane oldTabPane) {
            ArrayList metaDataList = new ArrayList();
            for (Iterator iterator = oldTabPane.getViews().iterator(); iterator.hasNext();) {
                View view = (View) iterator.next();
                TabMetaData metaData = oldTabPane.getTabMetaData(view);
                if (metaData.persistable()) {
                    metaDataList.add(metaData);
                }
            }
            return metaDataList;
        }
    }
}
