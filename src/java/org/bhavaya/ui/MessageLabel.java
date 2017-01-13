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

import org.apache.log4j.spi.LoggingEvent;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;

/**
 * This class is used in conjunction with Logging class to display Log messages.  It can be used on Log instance but
 * typically it used with a well-named log category such as the primary and secondary loading log categories.
 *
 * @see org.bhavaya.util.Log
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class MessageLabel extends JLabel implements Log.Listener {
    public MessageLabel(String preferredFontName, int style, float fontSize) {
        Font font = Font.getFont(preferredFontName);
        if (font == null) font = getFont();
        font = font.deriveFont(style, fontSize);
        setFont(font);
    }

    public void logMessage(final LoggingEvent loggingEvent) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                setText(loggingEvent.getMessage().toString());
            }
        });
    }
}
