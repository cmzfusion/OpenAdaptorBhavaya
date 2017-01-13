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

import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;
import org.openadaptor.adaptor.IbafException;
import org.openadaptor.adaptor.LocalSink;

import java.util.Properties;

/**
 * Receives dataObjects from the source, for processing by the subscriberListener.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class OpenAdapterSubscriber extends OpenAdapter {
    private static final Log log = Log.getCategory(OpenAdapterSubscriber.class);

    private LocalSink sink;
    private String subscriber;
    private LocalSink.DataObjectListener subscriberListener;
    private int reconnectionPeriod;

    public OpenAdapterSubscriber(String adapterName, Properties properties, String subscriber, LocalSink.DataObjectListener subscriberListener, int reconnectionPeriod) {
        super(adapterName, properties);
        this.subscriber = subscriber;
        this.subscriberListener = subscriberListener;
        this.reconnectionPeriod = reconnectionPeriod;
    }

    public void start() throws NotificationException {
        super.start();
        try {
            sink = (LocalSink) adaptor.getComponentByName(subscriber);
            sink.setDataObjectListener(subscriberListener);
        } catch (IbafException ibe) {
            log.error(ibe.toString());
            throw new NotificationException(ibe);
        }


        Thread openAdapterhread = Utilities.newThread(new FailoverRunAdaptor(adaptor, this, reconnectionPeriod), "OpenAdapterSubscriber", false);
        openAdapterhread.start();
    }
}
