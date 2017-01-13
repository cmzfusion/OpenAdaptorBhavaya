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

package org.bhavaya.ui;

import org.bhavaya.util.Log;
import org.bhavaya.util.SimpleObjectFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */

public class ExceptionPanel extends JFrame {

    private static final Log log= Log.getCategory(ExceptionPanel.class);

    private static final String HTML_HEADER = "<html>";
    private static final String HTML_EXCEPTION_FORMAT = "<p><b><font color=red>%class.name%</font>: %message%</b><br>";
    private static final String HTML_JAVA_FORMAT = " at %className%.%methodName%(<font color=blue>%fileName%:%lineNumber%</font>)<br>";
    private static final String HTML_NATIVE_FORMAT = " at %className%.%methodName%(native)<br>";
    private static final String HTML_FOOTER = "</html>";

    private static final String HEADER = "";
    private static final String EXCEPTION_FORMAT = "%class.name%: %message%\n";
    private static final String JAVA_FORMAT = " at %className%.%methodName%(%fileName%:%lineNumber%)\n";
    private static final String NATIVE_FORMAT = " at %className%.%methodName%(native)\n";
    private static final String FOOTER = "";

    private static final SimpleObjectFormat HTML_EXCEPTION_RENDER = new SimpleObjectFormat(HTML_EXCEPTION_FORMAT, true);
    private static final SimpleObjectFormat HTML_JAVA_RENDERER = new SimpleObjectFormat(HTML_JAVA_FORMAT);
    private static final SimpleObjectFormat HTML_NATIVE_RENDERER = new SimpleObjectFormat(HTML_NATIVE_FORMAT);

    private static final SimpleObjectFormat EXCEPTION_RENDER = new SimpleObjectFormat(EXCEPTION_FORMAT, true);
    private static final SimpleObjectFormat JAVA_RENDERER = new SimpleObjectFormat(JAVA_FORMAT);
    private static final SimpleObjectFormat NATIVE_RENDERER = new SimpleObjectFormat(NATIVE_FORMAT);

    public ExceptionPanel(String title, String contextString, Throwable e) {
        super(title);
        setIconImage(null);

        JPanel contentPanel = new JPanel(new BorderLayout());

        JLabel contextStringLabel = new JLabel(contextString);
        contextStringLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentPanel.add(contextStringLabel, BorderLayout.NORTH);

        JLabel stackTraceTextArea = new JLabel(createHTMLStackTraceString(HTML_HEADER, HTML_EXCEPTION_RENDER, HTML_NATIVE_RENDERER, HTML_JAVA_RENDERER, HTML_FOOTER, e));
        stackTraceTextArea.setVerticalAlignment(JLabel.TOP);
        stackTraceTextArea.setBackground(Color.white);
        stackTraceTextArea.setOpaque(true);
        stackTraceTextArea.setFont(stackTraceTextArea.getFont().deriveFont(Font.PLAIN));
        stackTraceTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane textAreaScrollPane = new JScrollPane(stackTraceTextArea);
        contentPanel.add(textAreaScrollPane, BorderLayout.CENTER);

        JButton copyButton = new JButton(new CopyToClipboardAction(createHTMLStackTraceString(HEADER, EXCEPTION_RENDER, NATIVE_RENDERER, JAVA_RENDERER, FOOTER, e)));
        JButton closeButton = new JButton(new CloseAction());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(contentPanel);
        setSize(500, 500);
    }

    private String createHTMLStackTraceString(String header, SimpleObjectFormat exceptionRenderer, SimpleObjectFormat nativeRenderer, SimpleObjectFormat javaRenderer, String footer, Throwable e) {
        StringBuffer buffer = new StringBuffer(header);

        Throwable currentException = e;
        do {
            log.error(e);
            buffer.append(exceptionRenderer.formatObject(currentException));

            StackTraceElement[] stack = e.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement element = stack[i];
                if (element.isNativeMethod()) {
                    buffer.append(nativeRenderer.formatObject(element));
                } else {
                    buffer.append(javaRenderer.formatObject(element));
                }
            }

            currentException = currentException.getCause();
        } while (currentException != null);

        buffer.append(footer);
        return buffer.toString();
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            putValue(Action.NAME, "Close");
        }

        public void actionPerformed(ActionEvent e) {
            dispose();
            System.exit(1);
        }
    }

    private class CopyToClipboardAction extends AbstractAction {
        private String copyString;

        public CopyToClipboardAction(String copyString) {
            this.copyString = copyString;
            putValue(Action.NAME, "Copy to clipboard");
        }

        public void actionPerformed(ActionEvent e) {
            StringSelection stringSelection = new StringSelection(copyString);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
        }
    }
}
