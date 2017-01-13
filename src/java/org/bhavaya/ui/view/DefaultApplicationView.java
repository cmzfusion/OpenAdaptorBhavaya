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
import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.ToolBarGroup;
import org.bhavaya.ui.componentaliasing.alias.AliasAwtComponent;
import org.bhavaya.util.ApplicationInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.10 $
 */
public abstract class DefaultApplicationView implements View {
    private java.util.List listeners = new ArrayList();
    private ApplicationContext applicationContext;
    private boolean isDisplayable = true;
    //Default last activated to time of creation - will be overwritten by config
    private Date lastActivated = new Date();

    public DefaultApplicationView() {
        applicationContext = ApplicationContext.getInstance();
    }

    public synchronized void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
    }

    public synchronized void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
    }

    public String getName() {
        return ApplicationInfo.getInstance().getName();
    }

    public String getFrameTitle() {
        return ApplicationInfo.getInstance().getName();
    }

    public String getTabTitle() {
        return ApplicationInfo.getInstance().getName();
    }

    public ImageIcon getImageIcon() {
        return applicationContext.getApplicationIcon();
    }

    public BeanCollection getBeanCollection() {
        return null;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
    }

    public ViewContext getViewContext() {
        return null;
    }

    public Action[] getAcceleratorActions() {
        return new Action[0];
    }

    public Component getComponentForInitialFocus() {
         return null;
    }

    public void dispose() {
    }

    protected void fireViewChanged() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ((ChangeListener) iterator.next()).stateChanged(new ChangeEvent(this));
        }
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
        //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public void removeAliasComponentFromToolbar(String aliasAwtComponent) {
        //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public void setAliasComponents(Map<String, AliasAwtComponent> aliasElements) {
        //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public Map<String, AliasAwtComponent> getAliasComponents() {
        return null;  //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public void setViewId(long id) {
        //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public long getViewId() {
        return 0;  //not implemented. Do not want alias buttons added to the main ebond screen
    }

    public void exportAction(WeakReference<Action> action) {

    }

    public void unexportAction(WeakReference<Action> action) {

    }

    public List<WeakReference<Action>> getExportedActions() {
        return null;

    }






}
