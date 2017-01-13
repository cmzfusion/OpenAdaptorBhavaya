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

import javax.jms.*;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
class JMSNotificationPublisher extends NotificationPublisher {
    private TopicConnection topicPublisherConnection;
    private TopicSession topicPublisherSession;
    private TopicPublisher topicPublisher;

    public JMSNotificationPublisher(PropertyGroup subjectPropertyGroup) {
        super(subjectPropertyGroup);
    }

    protected synchronized void connectImplementation() throws NotificationException {
        String publisherConnectionFactoryName = getProperties().getMandatoryProperty("jmsPublisherConnectionFactoryName");

        String jndiFactory = getProperties().getMandatoryProperty("jmsJndiFactory");
        String jndiProvider = getProperties().getMandatoryProperty("jmsJndiProvider");
        String jndiUsername = getProperties().getMandatoryProperty("jmsJndiUsername");
        String jndiPassword = getProperties().getMandatoryProperty("jmsJndiPassword");
        String connectionUsername = getProperties().getMandatoryProperty("jmsConnectionUsername");
        String connectionPassword = getProperties().getMandatoryProperty("jmsConnectionPassword");
        String topicName = getProperties().getMandatoryProperty("jmsTopicName");
        JMSUtil jmsUtil = new JMSUtil(new JndiConnectionParameters(jndiFactory, jndiProvider, jndiUsername, jndiPassword));

        try {
            topicPublisherConnection = jmsUtil.newTopicConnection(publisherConnectionFactoryName, connectionUsername, connectionPassword);
            topicPublisherSession = topicPublisherConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = jmsUtil.newTopic(topicName);
            topicPublisher = topicPublisherSession.createPublisher(topic);
        } catch (JMSException e) {
            throw new NotificationException(e);
        }
    }

    protected void closeImplementation() {
        JMSUtil.closeTopicConnection(topicPublisherConnection);
        topicPublisherConnection = null;
        topicPublisherSession = null;
        topicPublisher = null;
    }


    protected void commit(String message) throws NotificationException {
        try {
            TextMessage jmsMessage = topicPublisherSession.createTextMessage(message);
            topicPublisher.publish(jmsMessage, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, 0);
        } catch (JMSException e) {
            throw new NotificationException(e);
        }
    }
}
