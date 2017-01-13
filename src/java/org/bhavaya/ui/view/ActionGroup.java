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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.5 $
 */
public class ActionGroup {
    public static final Action SEPERATOR_ACTION = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
        }
    };

    private String name;
    private LinkedList elements = new LinkedList();

    public ActionGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Iterator iterator() {
        return elements.iterator();
    }

    public void addAction(Action action) {
        if (action == null) return;
        elements.add(action);
    }

    public void appendSeperator() {
        if (elements.size() == 0 || elements.get(elements.size() - 1) == SEPERATOR_ACTION) return;
        elements.add(SEPERATOR_ACTION);
    }

    public void addActions(Action[] actions) {
        if (actions == null) return;
        for (int i = 0; i < actions.length; i++) {
            addAction(actions[i]);
        }
    }

    public void addActionGroup(ActionGroup actionGroup) {
        if (actionGroup == null) return;
        elements.add(actionGroup);
    }

    public void addElementsOf(ActionGroup actionGroup) {
        if (actionGroup == null) return;
        Iterator iterator = actionGroup.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof Action) {
                addAction( (Action) element);
            }else if (element instanceof ActionGroup) {
                addActionGroup( (ActionGroup) element);
            }
        }
    }

    public Object getElement(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }
}
