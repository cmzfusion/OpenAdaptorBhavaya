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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2.44.2 $
 */
public class NativeProcess extends Process 
{
  
	private static final Log log = Log.getCategory(NativeProcess.class);
	 
	private static final int POLLING_FREQ = 450;
  
    // private static native void terminateProcess(int processId);
    // Replacement
    private static void terminateProcess(int processId)
    {
    	try
    	{
            Runtime rt = Runtime.getRuntime();
            if (isWindows())
               rt.exec("taskkill " + processId);
             else
               rt.exec("kill -9 " + processId);    	    		
    	}
    	catch (Exception e)
    	{
    		log.error("Error in terminateProcess [" + processId + "] " + e);
    	}
    }

    private static boolean isWindows()
    {
    	if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
    		return true;
    	else
    		return false;
    }
    

    //
    // private static native int waitForProcess(int processId);
	private static int waitForProcess(int processId) {
		for (;;) {
			try {
				if (isProcessRunning(processId)) {
					Thread.sleep(POLLING_FREQ);
				} else {
					break;
				}
			} catch (Exception e) {
				log.error("Error waiting for process [" + processId + "] " + e);
			}
		}
		return 0;
	}    

	private static boolean isProcessRunning(int processId)
	{
		boolean result = false;
		try {
			String line;
			if (!isWindows()) {
				Process p = Runtime.getRuntime().exec("ps -e | grep " + processId);
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = input.readLine()) != null) {
					if ((line != null) && (line.isEmpty()))
					{
						result = true;
					}
				}
				if (input != null) input.close();
			} else {
				ArrayList<NativeProcess> procs = getProcessEnumeration();
				for (NativeProcess p : procs)
				{
					if (p.getProcessId() == processId)
					{
						result = true;
						break;
					}
				}
			}
		} catch (Exception err) {
			err.printStackTrace();
		}

		
		return result;
	}
	
	// private static native void getProcessEnumeration(ArrayList processList);
	// Replacement
	private static ArrayList<NativeProcess> getProcessEnumeration() {

		ArrayList<NativeProcess> processList = new ArrayList<NativeProcess>();
		try {
			String line;
			if (!isWindows()) {
				Process p = Runtime.getRuntime().exec("ps -e");
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				while ((line = input.readLine()) != null) {
					String[] elements = line.split(" ");
					int pidI = Integer.parseInt(elements[0].trim());
					String name = elements[3];	
					if (pidI != 0)
					{
						NativeProcess np = new NativeProcess(pidI,name.trim() );
						processList.add(np);						
					}
				}
				input.close();
			} else {
				
				Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\"+ "tasklist.exe");
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				while ((line = input.readLine()) != null) 
				{
					if (line.length() > 32)
					{
					//System.out.println(line);
					String name = line.substring(0,28);
					String pid = line.substring(28,32);
					int pidI = -1;
					try
					{
						pidI = Integer.parseInt(pid.trim());						
					}
					catch (NumberFormatException nfe)
					{
						
					}
					if (pidI > 0)
					{
						NativeProcess np = new NativeProcess(pidI,name.trim() );
						processList.add(np);						
					}
					}
				}
				input.close();
				
				
			}
		} catch (Exception err) {
			err.printStackTrace();
		}

		return processList;

	}
    
    
    private int processId;
    private String executableName;

    // Called natively
    private NativeProcess(int processId, String executableName) {
        this.processId = processId;
        this.executableName = executableName;
    }

    public static NativeProcess[] getAllProcesses() {
        
    	ArrayList<NativeProcess> processList = getProcessEnumeration();
        return (NativeProcess[]) processList.toArray(new NativeProcess[processList.size()]);
    }

    public static NativeProcess findProcess(String executableName) {
        NativeProcess[] nativeProcesses = getAllProcesses();
        for (int i = 0; i < nativeProcesses.length; i++) {
            NativeProcess nativeProcess = nativeProcesses[i];
            if (nativeProcess.getExecutableName().equalsIgnoreCase(executableName)) {
                return nativeProcess;
            }
        }
        return null;
    }

    public OutputStream getOutputStream() {
        return null;
    }

    public InputStream getInputStream() {
        return null;
    }

    public InputStream getErrorStream() {
        return null;
    }

    public int waitFor() throws InterruptedException {
        return waitForProcess(processId);
    }

    public int exitValue() {
        return 0;
    }

    public void destroy() {
        terminateProcess(processId);
    }

    public int getProcessId() {
        return processId;
    }

    public String getExecutableName() {
        return executableName;
    }

    public String toString() {
        return executableName;
    }

    public static void main(String[] args) throws Exception {
        NativeProcess[] nativeProcesses = getAllProcesses();
        NativeProcess rvdProcess = null;
        for (int i = 0; i < nativeProcesses.length; i++) {
            NativeProcess nativeProcess = nativeProcesses[i];
            if (nativeProcess.executableName.toLowerCase().equals("notepad.exe")) {
                System.out.println("Found notepad on: " + nativeProcess.processId);
                rvdProcess = nativeProcess;
            }
        }
        System.out.println("Attempting to terminate");
        int exitCode = rvdProcess.waitFor();
        System.out.println("exitCode = " + exitCode);
    }
}