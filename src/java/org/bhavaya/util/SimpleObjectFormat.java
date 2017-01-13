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

import javax.swing.text.DateFormatter;
import javax.swing.text.NumberFormatter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class renders an object according to a defined pattern using the objects properties as variables.<br>
 *
 * <p>Usage:
 *
 * <p><code>
 * SimpleObjectFormat messageString = new SimpleObjectFormat("Exception message: %message%");<br>
 * System.out.println(messageString.formatObject(new Exception("Fred"));<br>
 * </code>
 * <p>
 * Output: <code>'Exception: Fred'</code>
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */
public class SimpleObjectFormat {
    private static final String DEFAULT_NULL_STRING = "";

    private Map objectRenderers = new HashMap();
    private String renderPattern;
    private String nullString;
    private String nullParameterString = "-";

    private boolean useBeanPaths = false;


    public SimpleObjectFormat(String renderPattern) {
        this.renderPattern = renderPattern;
        this.nullString = DEFAULT_NULL_STRING;
        loadDefaultRenderers();
    }

    public SimpleObjectFormat(String renderPattern, boolean useBeanPaths) {
        this(renderPattern);
        this.useBeanPaths = useBeanPaths;
    }

    public SimpleObjectFormat(String renderPattern, String nullString) {
        this.renderPattern = renderPattern;
        this.nullString = nullString;
        loadDefaultRenderers();
    }

    public SimpleObjectFormat(String renderPattern, String nullString, boolean useBeanPaths) {
        this(renderPattern, nullString);
        this.useBeanPaths = useBeanPaths;
    }

    public String getNullParameterString() {
        return nullParameterString;
    }

    public void setNullParameterString(String nullParameterString) {
        this.nullParameterString = nullParameterString;
    }

    public String formatObject(Object dataBean) {
        if (dataBean != null) {
            return substituteParameters(dataBean);
        } else {
            return nullString;
        }
    }

    public void setRenderer(Class clazz, StringRenderer objectRenderer) {
        objectRenderers.put(clazz, objectRenderer);
    }

    public void setNullString(String nullString) {
        this.nullString = nullString;
    }

    private String substituteParameters(final Object dataBean) {
        return Utilities.substituteTokens(renderPattern, new PatternRendererTransform(dataBean));
    }

    private String renderObject(Object object) {
        if (object == null) {
            return nullParameterString;
        } else {
            for (Iterator itar = objectRenderers.keySet().iterator(); itar.hasNext();) {
                Class clazz = (Class) itar.next();
                if (clazz.isInstance(object)) {
                    StringRenderer stringRenderer = (StringRenderer) objectRenderers.get(clazz);
                    String string = stringRenderer.render(object);
                    return Utilities.escapeHtmlCharacters(string);
                }
            }
            String string = object.toString();
            return Utilities.escapeHtmlCharacters(string);
        }
    }

    private void loadDefaultRenderers() {
        setRenderer(Date.class, DefaultRenderers.DATE_RENDERER);
        setRenderer(Number.class, DefaultRenderers.NUMBER_RENDERER);
    }

    private class PatternRendererTransform implements Transform {
        private final Object dataBean;

        public PatternRendererTransform(Object dataBean) {
            this.dataBean = dataBean;
        }

        public Object execute(Object sourceData) {
            String beanPath = (String) sourceData;
            if (useBeanPaths) {
                String beanPathArray[] = Generic.beanPathStringToArray(beanPath);
                return renderObject(Generic.get(dataBean, beanPathArray));
            } else {
                return renderObject(Generic.get(dataBean, beanPath));
            }
        }
    }

    public static class DefaultRenderers {
        private static final NumberFormatter numberFormatter;
        private static final DateFormatter dateFormatter;
        private static final DateFormatter timeFormatter;

        static {
            numberFormatter = new NumberFormatter(new DecimalFormat("#.###"));
            DateFormat dateFormatterDelegate = DateUtilities.newGmtDateFormat("ddMMMyyyy");
            DateFormat timeFormatterDelegate = DateUtilities.newGmtDateFormat("HH:mm:ss.SSS");
            dateFormatter = new DateFormatter(dateFormatterDelegate);
            timeFormatter = new DateFormatter(timeFormatterDelegate);
        }

        public static final StringRenderer NUMBER_RENDERER = new StringRenderer() {
            public String render(Object o) {
                try {
                    return numberFormatter.valueToString(o);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        public static final StringRenderer DATE_RENDERER = new StringRenderer() {
            public String render(Object o) {
                try {
                    return dateFormatter.valueToString(o);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        public static final StringRenderer TIME_RENDERER = new StringRenderer() {
            public String render(Object o) {
                try {
                    return timeFormatter.valueToString(o);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }
}
