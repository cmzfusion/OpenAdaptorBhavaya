/**
 * Class ${CLASS_NAME}
 * @author $Author: haass $
 * @version $Revision: 1.3 $
 */
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

package org.bhavaya.coms;

import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LocalNotificationPublisher extends NotificationPublisher {
    private static List subscribers = new ArrayList();
    private static Object subscribersLock = new Object();

    public LocalNotificationPublisher(PropertyGroup subjectPropertyGroup) {
        super(subjectPropertyGroup);
    }

    protected void commit(String message) throws NotificationException {
        if (isConnected()) {
            List subscribersSnapshot;
            synchronized (subscribersLock) {
                subscribersSnapshot = subscribers;
            }
            for (Iterator iterator = subscribersSnapshot.iterator(); iterator.hasNext();) {
                LocalNotificationSubscriber subscriber = (LocalNotificationSubscriber) iterator.next();
                subscriber.addNotification(message);
            }
        }
    }

    protected void connectImplementation() throws NotificationException {
    }

    protected void closeImplementation() {
    }

    protected static void addSubscriber(LocalNotificationSubscriber subscriber) {
        synchronized (subscribersLock) {
            subscribers = Utilities.add(subscriber, subscribers);
        }
    }

    protected static void removeSubscriber(LocalNotificationSubscriber subscriber) {
        synchronized (subscribersLock) {
            subscribers = Utilities.remove(subscriber, subscribers);
        }
    }

}
