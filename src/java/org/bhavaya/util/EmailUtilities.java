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

package org.bhavaya.util;

import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.6 $
 */
public class EmailUtilities {
    private static final Properties properties = new Properties();
    private static Session defaultSession;

    static {
        properties.put("mail.smtp.host", ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.smtpHost"));
    }

    public static class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            this.data = data;
            this.type = type;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Unsupported");
        }

        public String getContentType() {
            return type;
        }

        public String getName() {
            return "dummy";
        }
    }

    public static Session getDefaultSession() {
        if (defaultSession == null) {
            defaultSession = Session.getDefaultInstance(properties);
        }
        return defaultSession;
    }

    public static MimeMessage getDefaultMimeMessage(String subject) {
        String[] to = ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.to").split(";");
        return getDefaultMimeMessage(to, subject);
    }

    public static MimeMessage getDefaultMimeMessage(String[] to, String subject) {
        String from = ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.from");
        return getDefaultMimeMessage(to, from, subject);
    }

    public static MimeMessage getDefaultMimeMessage(String[] to, String from, String subject) {
        try {
            MimeMessage mailMessage = new MimeMessage(EmailUtilities.getDefaultSession());
            mailMessage.setFrom(new InternetAddress(from));
            mailMessage.setRecipients(Message.RecipientType.TO, convertStringsToAddresses(to));
            mailMessage.setSubject(subject);
            mailMessage.setSentDate(new Date());
            return mailMessage;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Address[] convertStringsToAddresses(String[] addressAsStrings) {
        try {
            Address[] addresses = new Address[addressAsStrings.length];
            for (int i = 0; i < addressAsStrings.length; i++) {
                addresses[i] = new InternetAddress(addressAsStrings[i]);
            }
            return addresses;
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
    }


    public static void sendMessage(Message message) {
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(Message message, String content, boolean html) {
        try {
            if (html) {
                message.setContent(content, "text/html");
            } else {
                message.setContent(content, "text/plain");
            }
            sendMessage(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendQuickMessage(String[] to, String from, String title, String message, boolean html) {
        MimeMessage defaultMimeMessage = EmailUtilities.getDefaultMimeMessage(to, from, title);
        sendMessage(defaultMimeMessage, message, html);
    }

    public static void sendQuickMessage(String[] to, String title, String message, boolean html) {
        MimeMessage defaultMimeMessage = EmailUtilities.getDefaultMimeMessage(to, title);
        sendMessage(defaultMimeMessage, message, html);
    }

    public static void sendQuickMessage(String title, String message, boolean html) {
        if (message == null) message = "";
        MimeMessage defaultMimeMessage = EmailUtilities.getDefaultMimeMessage(title);
        sendMessage(defaultMimeMessage, message, html);
    }
}
