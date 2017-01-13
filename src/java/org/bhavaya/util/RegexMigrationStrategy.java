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

import java.util.regex.Pattern;

/**
 * A configuration migration strategy that implements basic regex find and replace.  The power of this is limited
 * by the power of the Java Regex, but bear in mind that some quite complicated things can be accomplished such
 * as back-references.  e.g. You look for "(H\w*), (W\w*)" and replace with "$2, $1" which would find all occurances
 * of something like "Hello, World" and change it to "World, Hello".  Also, bear in mind that only one regex
 * strategy is allowed per version, so if you refactor 2 class names, the config will have to increment 2 version
 * numbers and have two seperate strategies.  I actually think this is quite powerful as it forces a precise ordering
 * of the patches, which given the holistic insanity of this concept, is a good idea.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class RegexMigrationStrategy implements ConfigMigrationStategy {
    private static final Log log = Log.getCategory(RegexMigrationStrategy.class);

    private static final int FIND_PATTERN_ARG = 0;
    private static final int REPLACE_STRING_ARG = 1;

    private long versionTarget;
    private Pattern findPattern;
    private String replaceString;

    public RegexMigrationStrategy(long versionTarget, String[] arguments) {
        this.versionTarget = versionTarget;
        this.findPattern = Pattern.compile(arguments[FIND_PATTERN_ARG]);
        this.replaceString = arguments[REPLACE_STRING_ARG];
    }

    public String migrate(String configKey, String source) {
        log.info("Migrating " + configKey + " configuration to version " + versionTarget + " using regular expression strategy");
        return findPattern.matcher(source).replaceAll(replaceString);
    }
}
