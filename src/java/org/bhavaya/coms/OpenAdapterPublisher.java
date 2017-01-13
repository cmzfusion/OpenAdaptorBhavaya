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
import org.openadaptor.adaptor.LocalSource;
import org.openadaptor.dataobjects.DataObject;

import java.util.Properties;

/**
 * Sends dataObjects to the source, where they will be sent on
 * to linked pipes, sinks and possibly over a transport layer to another
 * OpenAdapter adapter's source.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class OpenAdapterPublisher extends OpenAdapter {
    private static final Log log = Log.getCategory(OpenAdapterPublisher.class);

    private LocalSource source;
    private String publisher;
    private int reconnectionPeriod;

    public OpenAdapterPublisher(String adapterName, Properties properties, String publisher, int reconnectionPeriod) {
        super(adapterName, properties);
        this.publisher = publisher;
        this.reconnectionPeriod = reconnectionPeriod;
    }

    public void start() throws NotificationException {
        super.start();
        try {
            source = (LocalSource) adaptor.getComponentByName(publisher);
        } catch (IbafException ibe) {
            log.error(ibe.toString());
            throw new NotificationException(ibe);
        }

        Thread openAdapterhread = Utilities.newThread(new FailoverRunAdaptor(adaptor, this, reconnectionPeriod), "OpenAdapterPublisher", true);
        openAdapterhread.start();
    }


    /**
     * Sends dataObjects from to the source, where they will be sent on
     * to linked pipes, sinks and possibly over a transport layer to another
     * OpenAdapter adapter's source.
     */
    public synchronized void process(DataObject[] dataObjects) throws IbafException {
        source.process(dataObjects);
    }

}
