/**
 * @author $Author: jscobbie $
 * @version $Revision: 1.5.24.1 $
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

package org.bhavaya.db;

import org.bhavaya.collection.WeakHashSet;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.logging.Logger;

public class JNDIDataSource extends AbstractBhavayaDataSource {
    private static Log log = Log.getCategory(JNDIDataSource.class);

    private DataSource delegate;
    private Set connectionsConfigured = new WeakHashSet();

    public void configure(PropertyGroup propertyGroup) {
        log.info("Locating JNDI DataSource...");
        super.configure(propertyGroup);

        String jndiName = propertyGroup.getMandatoryProperty("jndiName");

        try {
            delegate = (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            log.error("Failed to locate JNDI DataSource", e);
            throw new RuntimeException(e);
        }
    }

    public void close() {
    }

    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        configure(connection);
        return connection;
    }

    private void configure(Connection connection) throws SQLException {
        if (!connectionsConfigured.contains(connection)) {
            connectionsConfigured.add(connection);
            getDialect().configure(connection);
        }
    }


    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
    
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        configure(connection);
        return connection;
    }

    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.getLogWriter();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    public String getConnectionId() {
        return "Not implemented";  
    }

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
	}
}