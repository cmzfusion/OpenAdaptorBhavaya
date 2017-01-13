package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.*;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 29-May-2008
 * Time: 11:35:52
 *
 * Send a diagnostic email with a list of attachments zipped up
 */
public class DiagnosticEmail {

    private static final Log log = Log.getCategory(DiagnosticEmail.class);

    private String comments;
    private String[] emailAddresses;
    private String mailSubject;
    private String summaryBodyContent;
    private String fileName;
    private List<? extends DiagnosticContext.Attachment> attachments;

    public DiagnosticEmail(String mailSubject, String emailAddress, String comments , String summaryBodyContent, String fileName, List<? extends DiagnosticContext.Attachment> attachments) {
        this.comments = comments;
        this.emailAddresses = emailAddress == null ? new String[0] : emailAddress.split(";");
        this.mailSubject = mailSubject;
        this.summaryBodyContent = summaryBodyContent;
        this.fileName = fileName;
        this.attachments = attachments;
    }

    public void sendEmail() {
        try {
            String subject = createDiagnosticSubject(mailSubject);
            Message mailMessage = EmailUtilities.getDefaultMimeMessage(emailAddresses, subject);
            MimeBodyPart summaryBodyPart = new MimeBodyPart();

            byte[] attachmentData = zipAttachment(attachments);
            boolean isIncludeAttachments = (attachmentData != null);

            summaryBodyPart.setContent(MimeUtility.encodeText(Utilities.escapeHtmlCharacters(createDiagnosticReport(comments, summaryBodyContent, isIncludeAttachments))), "text/html");
            summaryBodyPart.setHeader("Content-Type", "text/html; charset=iso-8859-1");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(summaryBodyPart);

            if (isIncludeAttachments) {
                MimeBodyPart logAttachment = new MimeBodyPart();
                logAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(attachmentData, "application/zip")));
                logAttachment.setFileName(fileName);
                multipart.addBodyPart(logAttachment);
            }

            mailMessage.setContent(multipart);
            EmailUtilities.sendMessage(mailMessage);
            log.info("Sent diagnostic report to: " + Utilities.asString(emailAddresses, ";"));
        } catch (Throwable ex) {
            log.error("Unable to send exception report", ex);
        }
    }

    static String createDiagnosticSubject(String type) {
        return ApplicationInfo.getInstance().getName() + ": " + type
                + ": environment: " + ApplicationInfo.getInstance().getEnvironmentName()
                + ", username: " + ApplicationInfo.getInstance().getUsername();
    }


    private byte[] zipAttachment(List<? extends DiagnosticContext.Attachment> attachmentsList) throws IOException {
        if (attachmentsList == null || attachmentsList.size() == 0) return null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.setLevel(ZipOutputStream.DEFLATED);

        for (DiagnosticContext.Attachment attachment : attachmentsList) {
            zos.putNextEntry(new ZipEntry(attachment.getName()));
            zos.write(attachment.getData());
        }
        zos.close();

        return bos.toByteArray();
    }

    private String createDiagnosticReport(String comments, String summaryBodyContent, boolean attachments) throws UnsupportedEncodingException {
        StringBuffer reportBuffer = new StringBuffer("<html>"
                + "<font face=arial size=4><center><b>Diagnostic report for " + ApplicationInfo.getInstance().getName() + "</b></center></font><br><br>");

        reportBuffer.append(createUserCommentsSummary(comments)).append("<br><br><br>");
        reportBuffer.append(summaryBodyContent);

        if (attachments) reportBuffer.append("<font face=Arial size=1><i>Files attached in details.zip</i></font>");
        reportBuffer.append("</html>");

        return reportBuffer.toString();
    }

    private String createUserCommentsSummary(String userComments) {
        return "<b><font face=arial>User comments</font></b><br>"
                + (userComments == null || userComments.length() == 0 ? "None" : userComments);
    }

    public static String addDomainToEmail(String emailAddress) {
        //Check whether the email already has a domain 
        if(emailAddress != null && emailAddress.indexOf('@') > -1) {
            return emailAddress;
        }
        return emailAddress + ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.defaultDomain");
    }

}
