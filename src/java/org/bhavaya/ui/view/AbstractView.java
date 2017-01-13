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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.ui.*;
import org.bhavaya.ui.componentaliasing.alias.AliasAwtComponent;
import org.bhavaya.util.Describeable;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.11 $
 */

public abstract class AbstractView implements View, Describeable {
    private static final Log log = Log.getCategory(AbstractView.class);

    private static final MenuPanel[] EMPTY_MENU_PANELS = new MenuPanel[0];
    private static final Action[] EMPTY_ACCELERATOR_ACTIONS = new Action[0];
    public static AtomicLong CREATION_TIME = new AtomicLong(System.currentTimeMillis());

    private ArrayList listeners = new ArrayList();
    private String name;
    private String tabTitle;
    private String frameTitle;
    private ViewContext viewContext;
    private boolean disposed;
    private boolean inited;
    private Object initLock = new InitLock();
    protected SplitPanel splitPanel;
    private boolean isDisplayable = true;
    //Default last activated to time of creation - will be overwritten by config
    private Date lastActivated = new Date();
    private Map<String,AliasAwtComponent> aliasComponents = new LinkedHashMap<String,AliasAwtComponent>();
    private List<WeakReference<Action>> exportedActions = new ArrayList<WeakReference<Action>>();
    private long id = CREATION_TIME.incrementAndGet();

    public AbstractView(String name, String tabTitle, String frameTitle) {
        this.name = name;
        this.tabTitle = tabTitle;
        this.frameTitle = frameTitle;
        inited = false;
        disposed = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        BeanCollection beanCollection = getBeanCollection();
        if (beanCollection != null) {
            if (beanCollection instanceof Describeable) {
                Describeable describeable = (Describeable) beanCollection;
                return describeable.getDescription();
            }
        }
        return "";
    }

    public String getTabTitle() {
        return tabTitle;
    }

    public String getFrameTitle() {
        return frameTitle;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
        fireViewChanged();
    }

    public void setFrameTitle(String frameTitle) {
        this.frameTitle = frameTitle;
        fireViewChanged();
    }

    public long getId() {
        return id;
    }

    /**
     * gets the default view context (as determined by applicationContext.getViewContext(View))
     * override this if you want something different
     */
    public ViewContext getViewContext() {
        if (viewContext == null) {
            viewContext = ApplicationContext.getInstance().createViewContext(this);

            // If the viewContext is still null, create a default one.
            if (viewContext == null) {
                viewContext = new DefaultViewContext(this);
            }
        }
        return viewContext;
    }

    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
    }

    public ImageIcon getImageIcon() {
        return null;
    }

    public ToolBarGroup createToolBarGroup() {
        init();
        ToolBarGroup toolBarGroup = new ToolBarGroup("AbstractView");

        if(aliasComponents.size() > 0) {
            renderAliasButtons(toolBarGroup);
        }
        return toolBarGroup;
    }

    private void renderAliasButtons(ToolBarGroup toolBarGroup) {
        toolBarGroup.addElement(new ToolBarGroup.SeperatorElement());
        for (AliasAwtComponent aliasAwtComponent: aliasComponents.values()) {
            toolBarGroup.addElement(new ToolBarGroup.ComponentElement(aliasAwtComponent.getAliasComponent()));
        }
        toolBarGroup.addElement(new ToolBarGroup.SeperatorElement());
    }

    public MenuGroup[] createMenuGroups(GenericWindow window) {
        init();
        return new MenuGroup[0];
    }

    public Component getComponentForInitialFocus() {
         return null;
    }

    protected final void init() {
        synchronized (getInitLock()) {
            if (!inited) {
                inited = true;
                initImpl();
            }
        }
    }

    public boolean isInited() {
        synchronized (getInitLock()) {
            return inited;
        }
    }

    protected void initImpl() {
//            assert EventQueue.isDispatchThread() : "Detected NonGUI thread in init: " + Thread.currentThread().getName();
        log.info(this + ": initImpl");
        // A little bit of a hack.  This causes view context to register its criteria before beanCollection construction.
        getViewContext();
    }

    public final void dispose() {
        synchronized (getInitLock()) {
            if (!disposed) {
                disposed = true;
                disposeImpl();
            }
        }
    }

    protected void disposeImpl() {
//            assert EventQueue.isDispatchThread() : "Detected NonGUI thread in dispose: " + Thread.currentThread().getName();
        log.info(this + ": disposeImpl");
        if (viewContext != null) {
            viewContext.dispose();
        }
        fireViewChanged();
    }

    public boolean isDisposed() {
        boolean isDisposed;
        synchronized (getInitLock()) {
            isDisposed = disposed;
        }
        return isDisposed;
    }

    protected MenuPanel[] createMenuPanels() {
        return EMPTY_MENU_PANELS;
    }

    protected Component createViewComponent(Component mainComponent) {
        Component internalComponent;

        MenuPanel[] menuPanels = createMenuPanels();
        if (menuPanels.length > 0) {
            splitPanel = new SplitPanel("RightSplitPanel", mainComponent, SplitPanel.RIGHT);
            SplitControlPanel splitPanelGutter = new SplitControlPanel(SplitPanel.RIGHT, splitPanel, splitPanel);

            for (int i = 0; i < menuPanels.length; i++) {
                MenuPanel menuPanel = menuPanels[i];
                splitPanelGutter.addMenuPanel(menuPanel);
            }

            internalComponent = splitPanelGutter;
        } else {
            internalComponent = mainComponent;
        }

        JPanel viewComponent = new JPanel(new BorderLayout());
        viewComponent.add(internalComponent, BorderLayout.CENTER);
        if(this.getViewContext().getToolbar() != null){
            viewComponent.add(this.getViewContext().getToolbar(), BorderLayout.NORTH);
        }
        return viewComponent;
    }

    /**
     * If there are accelerator actions which should be registered with the main view component,
     * subclasses should override this method to provide them
     */
    public Action[] getAcceleratorActions() {
        return EMPTY_ACCELERATOR_ACTIONS;
    }

    protected void fireViewChanged() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ChangeListener listener = (ChangeListener) iterator.next();
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    public Object getInitLock() {
        return initLock;
    }

    private static class InitLock {
    }

    public String toString() {
        return name;
    }

    public boolean isDisplayable() {
        return isDisplayable;
    }

    public void setDisplayable(boolean isDisplayable) {
        this.isDisplayable = isDisplayable;
    }

    public Date getLastActivated() {
        return lastActivated;
    }

    public void setLastActivated(Date lastActivated) {
        this.lastActivated = lastActivated;
    }

    public void addAliasComponentToToolbar(AliasAwtComponent aliasAwtComponent) {
        aliasComponents.put(aliasAwtComponent.getIdentifier(), aliasAwtComponent);
        fireViewChanged();
    }

    public void removeAliasComponentFromToolbar(String aliasAwtComponent) {
        aliasComponents.remove(aliasAwtComponent);
        fireViewChanged();
    }

    /**#
     *
     * Only to be used a startup when loading config. Adding alias buttons this way will result in
     * loosing the buttons that have previously been added to this view. Also this method does not
     * trigger the view to be redrawn.
     * @param aliasComponents
     */
    public void setAliasComponents(Map<String, AliasAwtComponent> aliasComponents) {
        this.aliasComponents = aliasComponents;
    }

    public Map<String, AliasAwtComponent> getAliasComponents() {
        return aliasComponents;
    }

    public void exportAction(WeakReference<Action> action) {
        exportedActions.add(action);
    }

    /**
     * If exposed, will stop making the action available for external use. Otherwise it would do nothing
     * @param action
     */
    public void unexportAction(WeakReference<Action> action) {
        exportedActions.remove(action);
    }

    public long getViewId() {
        return id;
    }

    public void setViewId(long id) {
        this.id = id;
    }

    /**
     * Returns a list of the actions this view would like to make available for aliasing. These must be exported
     *
     * @return
     */
    public List<WeakReference<Action>> getExportedActions() {
        return exportedActions;
    }






}
