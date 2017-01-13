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

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class JndiConnectionParameters {
    private static final Log log = Log.getCategory(JndiConnectionParameters.class);

    private InitialContext jndiContext;
    private String jndiFactory;
    private String jndiProvider;
    private String jndiUsername;
    private String jndiPassword;

    public JndiConnectionParameters(String jndiFactory, String jndiProvider, String jndiUsername, String jndiPassword) {
        this.jndiFactory = jndiFactory;
        this.jndiProvider = jndiProvider;
        this.jndiUsername = jndiUsername;
        this.jndiPassword = jndiPassword;
    }

    public synchronized InitialContext getContext() {
        if (jndiContext == null) {
            Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
            jndiProperties.put(Context.PROVIDER_URL, jndiProvider);
            jndiProperties.put(Context.SECURITY_PRINCIPAL, jndiUsername);
            jndiProperties.put(Context.SECURITY_CREDENTIALS, jndiPassword);
            try {
                log.info("Opening jndiContext [" + jndiFactory + " : " + jndiProvider + " : " + jndiUsername + "]");
                jndiContext = new InitialContext(jndiProperties);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return jndiContext;
    }

    public synchronized void closeContext() {
        try {
            log.info("Closing jndiContext: " + jndiContext);
            if (jndiContext != null) {
                jndiContext.close();
                jndiContext = null;
            }
        } catch (Throwable e) {
            log.error(e);
        }
    }
}
