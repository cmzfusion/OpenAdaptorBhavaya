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

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.spi.ObjectFactory;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
class JMSUtil {
    private static final Log log = Log.getCategory(JMSUtil.class);
    private JndiConnectionParameters jndiConnectionParameters;

    public JMSUtil(JndiConnectionParameters jndiConnectionParameters) {
        this.jndiConnectionParameters = jndiConnectionParameters;
    }

    public Topic newTopic(String topicName) {
        log.info("Opening topic [" + topicName + "]");
        Topic topic = (Topic) getJndiObject(topicName);
        return topic;
    }

    public TopicConnection newTopicConnection(String topicConnectionFactoryName, String username, String password) throws JMSException {
        log.info("Opening topicConnection [" + topicConnectionFactoryName + " : " + username + "]");
        TopicConnectionFactory topicConnectionFactory = newTopicConnectionFactory(topicConnectionFactoryName);
        TopicConnection topicConnection = topicConnectionFactory.createTopicConnection(username, password);
        topicConnection.start();
        return topicConnection;
    }

    private TopicConnectionFactory newTopicConnectionFactory(String topicConnectionFactoryName) {
        log.info("Opening topicConnectionFactory [" + topicConnectionFactoryName + "]");
        TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) getJndiObject(topicConnectionFactoryName);
        return topicConnectionFactory;
    }

    public static void closeTopicConnection(TopicConnection topicConnection) {
        if (topicConnection == null) return;
        try {
            log.info("Closing topicConnection: " + topicConnection);
            topicConnection.close();
        } catch (Throwable e) {
            log.error(e);
        }
    }

    private Object getJndiObject(String name) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        Object objectReference = null;
        try {
            objectReference = jndiConnectionParameters.getContext().lookup(name);
            if (objectReference instanceof javax.naming.Reference) {
                Class factoryClass = ClassUtilities.getClass(((javax.naming.Reference) objectReference).getFactoryClassName());
                ObjectFactory factory = (ObjectFactory) factoryClass.newInstance();
                objectReference = factory.getObjectInstance(objectReference, null, null, null);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return objectReference;
    }
}
