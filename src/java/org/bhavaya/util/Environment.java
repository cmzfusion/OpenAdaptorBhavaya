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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Properties;

/**
 * @author $Author: jscobbie $
 * @version $Revision: 1.6.24.1 $
 */
public class Environment {
	// DO NOT ADD A LOGGER HERE, Log uses Envrionment to configure itself
	// private static final Log log = Log.getCategory(Environment.class);

	/*
	 * Goodbye to the natives. static { try { System.loadLibrary("Environment");
	 * } catch (Throwable e) { System.err.println("Could not find library: " +
	 * java.lang.System.mapLibraryName("Environment") + ": '" + e + "'"); } }
	 */

	// public static native String getProperty(String property);
	// Replacement
	public static String getProperty(String property) {
		String result = null;
		Map<String, String> env = System.getenv();

		if (env.containsKey(property))
			result = env.get(property);

		return result;
	}

	// private static native void getProperties(ArrayList propertyContainer);
	

	// public static native void setProperty(String name, String value);
	// Replacement
	public static void setProperty(String name, String value) {
		System.out.println("1.7 WARN : Trying to set property [" + name + "|"+ value + "]");
		try
		{			
			System.setProperty(name,value);
		}
		catch (Exception e)
		{
			System.out.println("Error setting system property [" + name + "|" + value+"]");
		}
	}

	/*
	 * public static Map getProperties() { Map environmentMap = new
	 * LinkedHashMap();
	 * 
	 * ArrayList arrayList = new ArrayList(); getProperties(arrayList);
	 * 
	 * Iterator iterator = arrayList.iterator(); iterator.next(); // First
	 * property is current directory of program so ignore. while
	 * (iterator.hasNext()) { String nameValuePair = (String) iterator.next();
	 * String[] split = nameValuePair.split("=", 2);
	 * environmentMap.put(split[0].toUpperCase(), split[1]); } return
	 * environmentMap; }
	 */
	// Replacement
	public static Map<String, String> getProperties() {
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			System.out.format("%s=%s%n", envName, env.get(envName));
		}
		return env;
	}

	public static class DiskSpaceInfo {

		private static final DecimalFormat diskDecimalFormat1 = new DecimalFormat(
				"###,###");
		private static final DecimalFormat diskDecimalFormat2 = new DecimalFormat(
				"###,###.00");

		public boolean successful; // indicates whether the query was successful
		public long totalSpace; // in bytes
		public long availableSpace; // in bytes

		public DiskSpaceInfo(long total, long available) {
			successful = true;
			totalSpace = total;
			availableSpace = available;
		}

		public boolean isSuccessful() {
			return successful;
		}

		public long getTotalSpace() {
			return totalSpace;
		}

		public String getFormattedTotalSpace() {
			return format(totalSpace);
		}

		public long getAvailableSpace() {
			return availableSpace;
		}

		public String getFormattedAvailableSpace() {
			return format(availableSpace);
		}

		public String toString() {
			if (successful) {
				return "Total space: " + format(totalSpace)
						+ ", Available space: " + format(availableSpace);
			} else {
				return "Disk info query failed";
			}
		}

		public String format(long value) {
			if (value > 1024 * 1024 * 1024) {
				return format(((double) value) / (1024 * 1024 * 1024)) + " GB";
			} else if (value > 1024 * 1024) {
				return format(((double) value) / (1024 * 1024)) + " MB";
			} else if (value > 1024) {
				return format(((double) value) / (1024)) + " kB";
			} else {
				return format((double) value) + " bytes";
			}
		}

		protected String format(double value) {
			if (value > 10) {
				return diskDecimalFormat1.format(value);
			} else {
				return diskDecimalFormat2.format(value);
			}
		}
	}

	/**
	 * This method uses on Windows call to GetDiskFreeSpaceEx which returns
	 * numbers for current user. These numbers might be lower than the whole
	 * disk size or available space if the quota is applied for the directory
	 * ...
	 * 
	 * Avoid using directly in cross-platform code. Will throw an
	 * UnsafisfiedLinkError if no native library exists. Use getDiskSpaceInfo
	 * instead.
	 * 
	 * @param path
	 *            path to drive or directory for which information will be
	 *            requested
	 * @param info
	 *            disk info object to be populated with info data
	 */
	// public static native void getDiskSpaceInfo(String path, DiskSpaceInfo info);
	// 
	public static void getDiskSpaceInfo(String path, DiskSpaceInfo info)
	{
		DiskSpaceInfo df = getDiskSpaceInfo(path);
		info.availableSpace = df.getAvailableSpace();
		info.totalSpace = df.getTotalSpace();
		info.successful = df.successful;
		
	}
	

	/**
	 * Returns the available and total space information for requested path.
	 * 
	 * @param path
	 * @return
	 * 
	 *         public static DiskSpaceInfo getDiskSpaceInfo(String path) {
	 *         DiskSpaceInfo info = new DiskSpaceInfo(); try {
	 *         getDiskSpaceInfo(path, info); } catch (Throwable e) { // Native
	 *         link error. Don't care... } return info; }
	 * 
	 *         Replacement /** Returns the available and total space information
	 *         for requested path.
	 * @param path
	 * @return
	 */
	public static DiskSpaceInfo getDiskSpaceInfo(String path)  {

		DiskSpaceInfo info = null;
		try {
			File f = new File(path);
			info = new DiskSpaceInfo(f.getTotalSpace(), f.getFreeSpace());
		} catch (Throwable e) {
			System.out.format("error getting disk space for path " + path);
			System.out.format("error " + e);
		}
		return info;
	}

	/**
	 * 
	 * @return the number of milliseconds of procesor time this vm instance has
	 *         used. You might find the class "org.bhavaya.util.CpuLoad" far
	 *         more useful
	 */
	// public static native long getProcessCPUTime();
	// Replacement cpu time in nano-seconds
	public long getCpuTime() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return bean.isCurrentThreadCpuTimeSupported() ? bean
				.getCurrentThreadCpuTime() : 0L;
	}

	// public static native long getProcessCPUTime(int processId);
	// ooooooh look a replacement...
	public static long getProcessCPUTime(int processId) {
		long cpuTime = 0L;
		ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
		if (tmxb != null) {
			cpuTime = tmxb.getThreadCpuTime(processId);
		}

		return cpuTime;
	}

	/**
	 * Replaced with ThreadDiagnosticContext
	 */
	// @Deprecated
	// public static native void nativeRequestThreadDump(StringBuffer buf);

	public static void nativeRequestThreadDump(StringBuffer buf) {
		System.out.println("WARN : Trying to nativeRequestThreadDump [" + buf
				+ "]");
	}

	// public static native int getNumberOfProcessors()
	// Replacement
	public static int getNumberOfProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Replaced with ThreadDiagnosticContext
	 */
	@Deprecated
	public static void monitorDump() {
		StringBuffer buf = new StringBuffer();
		nativeRequestThreadDump(buf);
		System.out.println(buf.toString());
	}

	private static Object mon1 = new NamedMonitor() {
		public String toString() {
			return "NamedMonitor1!!!";
		}
	};
	private static Object mon2 = new Object();

	public static void main(String[] args) throws Exception {
		System.out.println("Processors " + getNumberOfProcessors());
		System.out.println("Available Space " + getDiskSpaceInfo("c:\\"));

		FileOutputStream out = null;
		try {
			out = new FileOutputStream("javalandTest.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // To change body of catch statement use
									// Options | File Templates.
		}
		// System.setOut(new PrintStream(out));
		Thread thread1 = new MyThread1();
		Thread thread2 = new MyThread2();

		System.out.println("Press a key");
		System.in.read();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace(); // To change body of catch statement use
									// Options | File Templates.
		}

		System.out.println("do deadlock");

		thread1.setDaemon(false);
		thread2.setDaemon(false);
		thread1.start();
		thread2.start();
		System.err.println("Done");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace(); // To change body of catch statement use
									// Options | File Templates.
		}
		System.err.println("Do the thread dump");
		monitorDump();
		System.err.println("done the thread dump");
		System.err.println("Goodbye!");
	}

	private static class MyThread1 extends Thread {
		public void run() {
			doStuff1();
		}

		private void doStuff1() {
			synchronized (mon1) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace(); // To change body of catch statement
											// use Options | File Templates.
				}
				System.out.println("thread 1 about to enter mon2");
				tryToDoInner(0);
			}
		}

		private void tryToDoInner(int depth) {
			if (depth < 21) {
				tryToDoInner(depth + 1);
			} else {
				synchronized (mon2) {
					System.out.println("thread 1 entered mon 2");
				}
			}
		}
	}

	private static class MyThread2 extends Thread {
		public void run() {
			synchronized (mon2) {
				thread2DoStuff();
			}
		}

		private void thread2DoStuff() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace(); // To change body of catch statement use
										// Options | File Templates.
			}
			System.out.println("thread 2 about to enter mon1");
			thread2Deadlocker();
		}

		private void thread2Deadlocker() {
			synchronized (mon1) {
				System.out.println("thread 2 entered mon 1");
			}
		}
	}

	/**
	 * indicates to the ThreadDump utility that it is safe to call the toString
	 * method of the monitor.
	 */
	public interface NamedMonitor {
	}
}
