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

package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.util.*;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Description.
 *
 * @author Brendon McLean
 * @version $Revision: 1.20 $
 */
public class ApplicationDiagnostics extends HeadlessApplicationDiagnostics {
    private static final Log log = Log.getCategory(ApplicationDiagnostics.class);

    protected JFrame frame;

    public static ApplicationDiagnostics getInstance() {
        ThreadUtilities.quietAquire(instanceMutext);
        if (instance == null) {
            instance = new ApplicationDiagnostics();
        }
        instanceMutext.release();
        return (ApplicationDiagnostics) instance;
    }

    protected ApplicationDiagnostics() {
    }

    protected boolean isHeadless() {
        return false;
    }

    public void showFrame() {
        getFrame().pack();
        UIUtilities.centreInFocusedWindow(getFrame(), 0, 0);
        getFrame().show();
    }

    public void addDiagnosticContext(DiagnosticContext diagnosticContext) {
        assert frame == null : "This method should not be called after showFrame()";
        super.addDiagnosticContext(diagnosticContext);
    }

    protected JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame("Application Diagnostics");
            frame.setIconImage(ApplicationContext.getInstance().getApplicationView().getImageIcon().getImage());

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setPreferredSize(new Dimension(800, 450));

            for (Iterator iterator = diagnosticContexts.iterator(); iterator.hasNext();) {
                DiagnosticContext diagnosticContext = (DiagnosticContext) iterator.next();
                if (diagnosticContext.createComponent() != null) {
                    tabbedPane.addTab(diagnosticContext.getName(), diagnosticContext.getIcon(), diagnosticContext.getComponent());
                }
            }

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(new JButton(new SendDiagnosticReportAction(frame)));
            buttonPanel.add(new JButton(new CloseAction()));

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
            frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            frame.setJMenuBar(createMenuBar());

            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
        return frame;
    }

    protected JMenuBar createMenuBar() {
        List menuGroupList = new  ArrayList();
        for (Iterator iterator = diagnosticContexts.iterator(); iterator.hasNext();) {
            DiagnosticContext diagnosticContext = (DiagnosticContext) iterator.next();
            menuGroupList.add(diagnosticContext.createMenuGroups());
        }

        JMenuBar menuBar = new JMenuBar();
        for (Iterator iterator = menuGroupList.iterator(); iterator.hasNext();) {
            MenuGroup[] menuGroups = (MenuGroup[]) iterator.next();
            MenuGroup.processMenuGroups(menuBar, menuGroups, MenuGroup.LEFT);
        }
        MenuGroup.processMenuGroups(menuBar, new MenuGroup[]{createFileMenuGroup()}, MenuGroup.LEFT);

        for (Iterator iterator = menuGroupList.iterator(); iterator.hasNext();) {
            MenuGroup[] menuGroups = (MenuGroup[]) iterator.next();
            MenuGroup.processMenuGroups(menuBar, menuGroups, MenuGroup.RIGHT);

        }

        return menuBar;
    }

    protected MenuGroup createFileMenuGroup() {
        MenuGroup fileMenuGroup = new MenuGroup("File", KeyEvent.VK_F);
        fileMenuGroup.setHorizontalLayout(MenuGroup.LEFT);
        fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new SendDiagnosticReportAction(frame))));
        fileMenuGroup.addElement(new MenuGroup.SeparatorElement());
        fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new CloseAction())));
        return fileMenuGroup;
    }

    protected class CloseAction extends AuditedAbstractAction {
        public CloseAction() {
            putValue(Action.NAME, "Close");
        }

        public void auditedActionPerformed(ActionEvent e) {
            getFrame().hide();
        }
    }

    protected class SendDiagnosticReportAction extends AuditedAbstractAction {
        private JFrame owner;

        public SendDiagnosticReportAction(JFrame owner) {
            this.owner = owner;
            putValue(Action.NAME, "Send Diagnostic Report...");
        }

        public void auditedActionPerformed(ActionEvent e) {
            final JDialog dialog = new JDialog(owner, "Diagnostic Detail", true);

            final JTextArea commentsTextArea = new JTextArea(3, 40);
            JScrollPane commentsScrollPane = new JScrollPane(commentsTextArea);

            JButton okButton = new JButton(new AuditedAbstractAction("Ok", "Send Diagnostics Report") {
                public void auditedActionPerformed(ActionEvent e) {
                    dialog.dispose();
                    UIUtilities.runTaskWithProgressDialog("Submit Diagnostic", new Task("Submitting Diagnostic") {
                        public void run() {
                           sendDiagnosticReport(commentsTextArea.getText());
                        }
                    });
                    owner.hide();
                }
            });
            JButton cancelButton = new JButton(new AuditedAbstractAction("Cancel") {
                public void auditedActionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            JPanel commentsPanel = new JPanel(new BorderLayout());
            commentsPanel.add(new JLabel("Please provide as much detail as possible; symptoms of problem, under what conditions it appears..."), BorderLayout.NORTH);
            commentsPanel.add(commentsScrollPane, BorderLayout.CENTER);
            commentsPanel.add(buttonPanel, BorderLayout.SOUTH);
            commentsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            dialog.setContentPane(commentsPanel);
            dialog.pack();
            UIUtilities.centreInContainer(owner, dialog, 0, 0);
            dialog.show();
        }
    }

    public void sendScreenCapture() {
        try {
            boolean proceed = true;
            if (!EventQueueWatchdog.getInstance().isPanicing()) {
                final String AUTHORISE_REQUEST = "Authorise Request";
                final String DISMISS_REQUEST = "Dismiss Request";
                int result = JOptionPane.showOptionDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getCurrentFocusCycleRoot(),
                        "The Support Team have requested the application perform a screen capture", "Screen Capture Request",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{AUTHORISE_REQUEST, DISMISS_REQUEST}, AUTHORISE_REQUEST);
                proceed = (result == 0);
                Toolkit.getDefaultToolkit().sync();
                Thread.sleep(500);
            }
            if (proceed) {
                String subject = ApplicationInfo.getInstance().getName() + ": Screen Capture : environment: "
                        + ApplicationInfo.getInstance().getEnvironmentName() + ", username: " + ApplicationInfo.getInstance().getUsername();
                Message mailMessage = EmailUtilities.getDefaultMimeMessage(subject);
                BufferedImage[] bufferedImages = UIUtilities.getScreenCaptures();

                MimeBodyPart[] imageBodyParts = new MimeBodyPart[bufferedImages.length];
                for (int i = 0; i < bufferedImages.length; i++) {
                    BufferedImage bufferedImage = bufferedImages[i];

                    MimeBodyPart imageAttachment = new MimeBodyPart();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "PNG", bos);
                    imageAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(bos.toByteArray(), "image/png")));
                    imageAttachment.setFileName("screen" + i + ".png");
                    imageAttachment.setContentID("" + System.identityHashCode(imageAttachment));
                    imageBodyParts[i] = imageAttachment;
                }

                MimeBodyPart summaryBodyPart = new MimeBodyPart();
                summaryBodyPart.setContent(createScreenCaptureReport(imageBodyParts), "text/html");
                summaryBodyPart.setHeader("Content-Type", "text/html; charset=iso-8859-1");

                Multipart multipart = new MimeMultipart("related");
                multipart.addBodyPart(summaryBodyPart);
                for (int i = 0; i < imageBodyParts.length; i++) {
                    MimeBodyPart imageBodyPart = imageBodyParts[i];
                    multipart.addBodyPart(imageBodyPart);
                }

                mailMessage.setContent(multipart);
                EmailUtilities.sendMessage(mailMessage);
                log.info("Sent diagnostic report to: " + ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.to"));
            } else {
                log.info("Screen capture requested but dismissed by user.");
            }
        } catch (Exception e) {
            log.error("Unable to send screen capture", e);
        }
    }

    protected String createScreenCaptureReport(MimeBodyPart[] imageBodyParts) throws MessagingException {
        StringBuffer buffer = new StringBuffer("<html>"
                + "<font face=arial><b>Screen Capture for " + ApplicationInfo.getInstance().getName() + "</b></font><br><hr size=1>");
        for (int i = 0; i < imageBodyParts.length; i++) {
            MimeBodyPart imageBodyPart = imageBodyParts[i];
            buffer.append("<table><tr><th><font face=arial>Screen ").append(i)
                    .append("</b></font></th></tr>" + "<tr><td><img src=\"cid:").append(imageBodyPart.getContentID())
                    .append("\" border=1></td></tr></table><br><hr size=1>");
        }
        buffer.append("</html>");
        return buffer.toString();
    }
}
