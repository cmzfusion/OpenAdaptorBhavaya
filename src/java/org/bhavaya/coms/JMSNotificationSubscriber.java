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
 * @version $Revision: 1.3 $
 */
class JMSNotificationSubscriber extends NotificationSubscriber {
    private TopicConnection topicSubscriberConnection;

    public JMSNotificationSubscriber(PropertyGroup subjectPropertyGroup) {
        super(subjectPropertyGroup);
    }

    protected void connectImplementation() throws NotificationException {
        String jndiFactory = getProperties().getMandatoryProperty("jmsJndiFactory");
        String jndiProvider = getProperties().getMandatoryProperty("jmsJndiProvider");
        String jndiUsername = getProperties().getMandatoryProperty("jmsJndiUsername");
        String jndiPassword = getProperties().getMandatoryProperty("jmsJndiPassword");
        String subscriberConnectionFactoryName = getProperties().getMandatoryProperty("jmsSubscriberConnectionFactoryName");
        String connectionUsername = getProperties().getMandatoryProperty("jmsConnectionUsername");
        String connectionPassword = getProperties().getMandatoryProperty("jmsConnectionPassword");
        String topicName = getProperties().getMandatoryProperty("jmsTopicName");
        JMSUtil jmsUtil = new JMSUtil(new JndiConnectionParameters(jndiFactory, jndiProvider, jndiUsername, jndiPassword));

        try {
            topicSubscriberConnection = jmsUtil.newTopicConnection(subscriberConnectionFactoryName, connectionUsername, connectionPassword);

            topicSubscriberConnection.setExceptionListener(new ExceptionListener() {
                public void onException(JMSException e) {
                    handleException(e);
                }
            });

            TopicSession topicSubscriberSession = topicSubscriberConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = jmsUtil.newTopic(topicName);
            TopicSubscriber topicSubscriber = topicSubscriberSession.createSubscriber(topic);
            topicSubscriber.setMessageListener(new Receiver());
        } catch (Exception e) {
            throw new NotificationException(e);
        }
    }

    protected void closeImplementation() {
        JMSUtil.closeTopicConnection(topicSubscriberConnection);
    }

    private class Receiver implements MessageListener {
        public void onMessage(Message message) {
            try {
                String incomingNotification = ((TextMessage) message).getText();
                addNotification(incomingNotification);
            } catch (Exception e) {
                if (isConnected()) {
                    handleException(e);
                }
            }
        }
    }
}
