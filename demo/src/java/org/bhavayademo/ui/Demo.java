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

package org.bhavayademo.ui;

import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.MetaDataSource;
import org.bhavaya.ui.DefaultApplicationContext;
import org.bhavaya.ui.SplashScreen;
import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.diagnostics.*;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyModel;
import org.bhavaya.ui.diagnostics.Profiler;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.12 $
 */
public class Demo extends DefaultApplicationContext {
    private static final Log log = Log.getCategory(Demo.class);

    public Demo() throws Exception {
        SplashScreen splashScreen = ApplicationContext.getSplashScreen();
        splashScreen.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 3) System.err.println("weee!");
            }
        });


        Profiler.addDefaultLogListener();
        ApplicationDiagnostics applicationDiagnostics = ApplicationDiagnostics.getInstance();
        applicationDiagnostics.addDiagnosticContext(new BeanFactoryDiagnosticContext());
        applicationDiagnostics.addDiagnosticContext(new YourKitDiagnosticContext());
        applicationDiagnostics.addDiagnosticContext(new Profiler.ProfilerDiagnosticContext());
        applicationDiagnostics.addDiagnosticContext(ThreadDiagnosticContext.getInstance());
        applicationDiagnostics.createMBeanServer();
    }

    public void prepareStart() {
        try {
            DBUtilities.executeUpdateScript("demoDatabaseInstrumentsTx", "destroyDemo.sql", false);
            DBUtilities.executeUpdateScript("demoDatabaseInstrumentsTx", "createDemo.sql", true);
            DBUtilities.executeUpdateScript("demoDatabaseInstrumentsTx", "createDemoStaticData.sql", true);
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        super.prepareStart();
    }

    protected void postStartImpl() {
        super.postStartImpl();
        try {
            RandomDataGenerator randomDataGenerator = new RandomDataGenerator(20000, 2 * 1000);
            randomDataGenerator.start();
            MetaDataSource.writeMetaDataToFile();
            PropertyModel.writeSubclassMappingsToFile();
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
}
