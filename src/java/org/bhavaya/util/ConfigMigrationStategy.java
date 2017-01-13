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


/**
 * Yet another String->String transform inteface.  Classes implementing this interface are
 * responsible for patch the configuration file from one version to the next.  In addition
 * to implementing the <code>migrate</code> method, any implementing class should also contain
 * a constructor of the type <code>new (int versionTarget, string[] arguments)</code>.
 * <code>versionTarget</code> is the versionLevel to patch to and really only useful for debugging.
 * The <code>arguments</code> arrays is an array of strings the is passed from the XML file
 * to the class as optional parameters that any strategies may require.  Be brave my son,
 * for the path of patching config files is dangerous, but not as dangerous as blowing away
 * Malcolms column configs.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public interface ConfigMigrationStategy {
    public String migrate(String configKey, String source);
}
