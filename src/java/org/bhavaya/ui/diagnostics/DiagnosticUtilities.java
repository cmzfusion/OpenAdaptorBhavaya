package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.Utilities;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class DiagnosticUtilities {
    public static StringBuffer tableRow(StringBuffer buffer, Object[] objects) {
        buffer.append("<tr>");
        for (int i = 0; i < objects.length; i++) {
            buffer.append("<td><font face=arial>").append(Utilities.surroundUrlsWithHrefs("" + objects[i])).append("</font></td>");
        }
        return buffer.append("</tr>");
    }

    public static StringBuffer contextHeader(StringBuffer buffer, String header) {
        return buffer.append("<b><font face=arial>" + header + "</font></b><br>");
    }

    public static void tableFooter(StringBuffer buffer) {
        buffer.append("</table>");
    }

    public static void tableHeader(StringBuffer buffer) {
        buffer.append("<table border=1 width=800>");
    }

    public static StringBuffer tableHeaderRow(StringBuffer buffer, Object[] objects) {
        buffer.append("<tr>");
        for (int i = 0; i < objects.length; i++) {
            buffer.append("<th><font face=arial>").append(objects[i]).append("</font></th>");
        }
        return buffer.append("</tr>");
    }
}
