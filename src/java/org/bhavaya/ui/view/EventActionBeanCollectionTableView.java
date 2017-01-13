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
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.collection.ListEvent;
import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.SoundHandler;
import org.bhavaya.util.*;

import javax.swing.*;
import java.beans.Encoder;
import java.beans.Expression;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Allows subclasses to apply GUI (or other) actions to list events that occur on the beanCollection.
 * Applies default actions of playing a sound and flashing the tab of the view, whenever an event occurs
 * on the beanCollection.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class EventActionBeanCollectionTableView extends BeanCollectionTableView {
    private static final Log log = Log.getCategory(EventActionBeanCollectionTableView.class);

    private SoundHandler soundHandler;
    private TableViewCollectionListener tableViewCollectionListener;
    private Filter eventFilter;
    private Runnable eventAction;

    static {
        BeanUtilities.addPersistenceDelegate(EventActionBeanCollectionTableView.class, new BhavayaPersistenceDelegate(new String[]{"name", "tabTitle", "frameTitle", "recordType", "viewConfigurationId", "beanCollection", "soundEnabled", "soundClipFilename"}) {
            // DefaultPersistenceDelegate fails trying to call getSoundEnabled rather than isSoundEnabled
            protected Expression instantiate(Object oldInstance, Encoder out) {
                EventActionBeanCollectionTableView view = (EventActionBeanCollectionTableView) oldInstance;
                return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[]{view.getName(), view.getTabTitle(), view.getFrameTitle(), view.getRecordType(), view.getViewConfigurationId(), view.getBeanCollection(), new Boolean(view.isSoundEnabled()), view.getSoundClipFilename()});
            }
        });
    }

    public EventActionBeanCollectionTableView(String name, String tabTitle, String frameTitle, Class recordType, String viewConfigurationId, BeanCollection beanCollection) {
        this(name, tabTitle, frameTitle, recordType, viewConfigurationId, beanCollection, false, null);
    }

    public EventActionBeanCollectionTableView(String name, String tabTitle, String frameTitle, Class recordType, String viewConfigurationId, BeanCollection beanCollection, boolean soundEnabled) {
        this(name, tabTitle, frameTitle, recordType, viewConfigurationId, beanCollection, soundEnabled, null);
    }

    public EventActionBeanCollectionTableView(String name, String tabTitle, String frameTitle, Class recordType, String viewConfigurationId, BeanCollection beanCollection, boolean soundEnabled, String soundClipFilename) {
        super(name, tabTitle, frameTitle, recordType, viewConfigurationId, beanCollection);
        soundHandler = new SoundHandler(soundEnabled, soundClipFilename, tabTitle);
    }

    protected void initImpl() {
        super.initImpl();

        ApplicationContext.getInstance().addGuiTask(new Task("Add event listener to BeanCollection for " + getName()) {
            public void run() {
                if (!isDisposed()) {
                    EventActionBeanCollectionTableView.this.tableViewCollectionListener = createTableViewCollectionListener();
                    getBeanCollection().addCollectionListener(tableViewCollectionListener);
                }
            }
        });
    }

    protected TableViewCollectionListener createTableViewCollectionListener() {
        return new TableViewCollectionListener();
    }

    protected JToolBar createEditControlToolBar(boolean pivoted){
        JToolBar toolBar = super.createEditControlToolBar(pivoted);

        JToggleButton soundEnabledButton = new JToggleButton(SoundHandler.SPEAKER_ICON);
        soundEnabledButton.setModel(soundHandler.getSetSoundEnabledToggleButtonModel());
        soundEnabledButton.setToolTipText("Play sound on new row");
        toolBar.add(soundEnabledButton);

        return toolBar;
    }

    protected JMenu[] createEditControlMenus() {
        java.util.List menus =  new ArrayList(Arrays.asList(super.createEditControlMenus()));
        JMenu soundSettingsMenu = new JMenu("Sound Settings");
        soundSettingsMenu.setOpaque(false);

        JCheckBoxMenuItem soundEnabledMenuItem = new JCheckBoxMenuItem("Play sound on new row");
        soundEnabledMenuItem.setModel(soundHandler.getSetSoundEnabledToggleButtonModel());
        soundSettingsMenu.add(soundEnabledMenuItem);

        soundSettingsMenu.add(new JMenuItem(soundHandler.getSetSoundClipFilenameAction()));

        menus.add(soundSettingsMenu);
        return (JMenu[]) menus.toArray(new JMenu[menus.size()]);
    }

    protected void disposeImpl() {
        getBeanCollection().removeCollectionListener(tableViewCollectionListener);
        super.disposeImpl();
    }

    public boolean isSoundEnabled() {
        return soundHandler.isSoundEnabled();
    }

    public String getSoundClipFilename() {
        return soundHandler.getSoundClipFilename();
    }

    private class TableViewCollectionListener implements CollectionListener {
        public void collectionChanged(ListEvent e) {
            if (getEventFilter().evaluate(e)) {
                try {
                    getEventAction().run();
                } catch (Exception e1) {
                    log.error(e1);
                }
            }
        }
    }

    private Filter getEventFilter() {
        if (eventFilter == null) {
            eventFilter = createEventFilter();
        }
        return eventFilter;
    }

    private Runnable getEventAction() {
        if (eventAction == null) {
            eventAction = createEventAction();
        }
        return eventAction;
    }

    protected Filter createEventFilter() {
        return new EventFilter();
    }

    protected Runnable createEventAction() {
        return new EventAction();
    }

    protected static class EventFilter implements Filter {
        public boolean evaluate(Object obj) {
            ListEvent e = (ListEvent) obj;
            return (e.getType() == ListEvent.INSERT);
        }
    }

    protected class EventAction implements Runnable {
        public void run() {
            soundHandler.play();
            Workspace.getInstance().flashView(EventActionBeanCollectionTableView.this);
        }
    }

}
