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

package org.bhavaya.ui.builder;

import org.bhavaya.ui.TableViewConfiguration;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class State {
    private ApplicationCollection applications;
    private FrameConfig frameConfig;
    private FrameConfig processFrameConfig;
    private TableViewConfiguration tableViewConfiguration;

    public State() {
    }

    public ApplicationCollection getApplications() {
        if (applications == null) applications = new ApplicationCollection();
        return applications;
    }

    public void setApplications(ApplicationCollection applications) {
        this.applications = applications;
    }

    public FrameConfig getFrameConfig() {
        return frameConfig;
    }

    public void setFrameConfig(FrameConfig frameConfig) {
        this.frameConfig = frameConfig;
    }

    public TableViewConfiguration getTableViewConfiguration() {
        return tableViewConfiguration;
    }

    public void setTableViewConfiguration(TableViewConfiguration tableViewConfiguration) {
        this.tableViewConfiguration = tableViewConfiguration;
    }

    public FrameConfig getProcessFrameConfig() {
        return processFrameConfig;
    }

    public void setProcessFrameConfig(FrameConfig processFrameConfig) {
        this.processFrameConfig = processFrameConfig;
    }
}
