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
import org.bhavaya.ui.GenericWindow;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.ToolBarGroup;
import org.bhavaya.ui.componentaliasing.alias.AliasAwtComponent;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Very important interface.  Please note.  Current gotcha, do as little as possible in view constructors.  I mean it!
 * preferably just get the constructors args into member refs.  Doing anything else puts you in danger several infinite
 * recursion possibilities.  Do everything else in a lazy init method.
 *
 * @author Brendon McLean
 * @version $Revision: 1.8 $
 */

public interface View {
    public void addChangeListener(ChangeListener changeListener);

    public void removeChangeListener(ChangeListener changeListener);

    /**
     * @return a human readable name for this view.
     */
    public String getName();

    /**
     * @return the title that will be used for tabs and frame titles
     */
    public String getFrameTitle();

    /**
     * @return the title that will be used for tabs and frame titles
     */
    public String getTabTitle();

    /**
     * @return the ImageIcon that will be used to represent this view
     */
    public ImageIcon getImageIcon();

    /**
     * Best if keep this lazy to avoid expensive startup times when loading configuration.
     * @return a java.awt.Component that represents this View.
     */
    public Component getComponent();

    /**
     * @return an array of MenuGroup objects that will be used to build the frame menu.
     */
    public MenuGroup[] createMenuGroups(GenericWindow window);

    /**
     * @return an array of actions which will be bound to key strokes on the containing JRootPane's
     * input map, using the ACCELERATOR_KEY set in the action, when the view becomes active
     */
    public Action[] getAcceleratorActions();

    /**
     * @return the component which should request focus when this view becomes active,
     * or null if there is no component requiring focus
     */
    public Component getComponentForInitialFocus();

    /**
     * @return an array of ToolBarGroup objects that will be added to the main application toolbar.
     */
    public ToolBarGroup createToolBarGroup();


    /**
     *  Used to add any alias buttons to the view. The component will be drawn the next time the view is redrawn
     * (View will be redrawn when user switches to it and can not add an alias button to the same view as original button
     * so should be safe)
     * @param aliasAwtComponent
     */
    public void addAliasComponentToToolbar(AliasAwtComponent aliasAwtComponent);

    /**
     * Used to remove an alias button/component from the toolbar of this view
     *
     * @param aliasAwtComponent
     */
    public void removeAliasComponentFromToolbar(String aliasAwtComponent);

    /**
     *
     */
    public void setAliasComponents(Map<String,AliasAwtComponent> aliasComponents );

    public Map<String,AliasAwtComponent> getAliasComponents();

    /**
     * Exposes the specified action for external use. Exported via a weakreference to as action should only be available
     * during the lifetime of the view. An exported action should not prevent a closed view from being disposed and gc'd.
     * Actions that are being exported must also be declared as instance variables of the class.
     * @param action
     */
    public void exportAction(WeakReference<Action> action);

    /**
     * If exposed, will stop making the action available for external use. Otherwise it would do nothing
     * @param action
     */
    public void unexportAction(WeakReference<Action> action);

    /**
     * Returns a list of the actions this view would like to make available for aliasing. These must be exported
     *
     * @return
     */
    public List<WeakReference<Action>> getExportedActions();

    /**
     * A unqiue identifier for this view. Ebond allows the same view to open twice.
     * @return
     */
    public long getViewId();

    /**
     *
     * @return
     */
    public void setViewId(long id);


    /**
     * The Java Listener Pattern is easy easy to setup but difficult to remove.  In cases where components somehow
     * end up listening to singletons or any objects that are statically referenced, garbage collection can be
     * blocked for the lifetime of the application.  Seeming as we always know the exact moment that a view should be
     * collected, a hook is provided by this dispose method.  This event gives the views a chance to unhook all their
     * listeners which will hopefully improve the chance a successful garbage collection.  This method is invoked
     * by Workspace.
     *
     * @see Workspace
     */
    public void dispose();

    /**
     * @return the view context that should be used for this view. can be null.
     */
    public ViewContext getViewContext();

    /**
     * @return the dataset that this view is looking at (can be null)
     */
    public BeanCollection getBeanCollection();

    /**
     * Sets the bean collection that this view is looking at.
     * @param beanCollection the collection.
     */
    public void setBeanCollection(BeanCollection beanCollection);


    /**
     * This method is used by composite views to determine which
     * child views should be displayed
     */
    public boolean isDisplayable();
    
    public void setDisplayable(boolean isDisplayable);

    /**
     * @return The time the view was last activated
     */
    public Date getLastActivated();

    /**
     * Set the time the view was last activated
     * @param date Time view last activated
     */
    public void setLastActivated(Date date);


}
