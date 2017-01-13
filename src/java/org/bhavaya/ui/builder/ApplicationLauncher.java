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

import org.bhavaya.ui.ArrayEnumeration;
import org.bhavaya.util.MutableUrlClassLoader;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class ApplicationLauncher extends ClassLoader {
    public static void main(String[] args) {
        try {
            String hostPort = System.getProperty("urlClassLoader");
            System.out.println("Connecting to: " + hostPort);
            int separatorIndex = hostPort.indexOf(':');
            String host = hostPort.substring(0, separatorIndex);
            int port = Integer.parseInt(hostPort.substring(separatorIndex + 1, hostPort.length()));

            ClassLoader classLoader = new SocketUrlClassLoader(ApplicationLauncher.class.getClassLoader(), host, port);
            Thread.currentThread().setContextClassLoader(classLoader);

            Class classUtilitiesClass = Class.forName("org.bhavaya.util.ClassUtilities", true, classLoader);
            Method setParentClassLoaderMethod = classUtilitiesClass.getMethod("setParentClassLoader", ClassLoader.class);
            setParentClassLoaderMethod.invoke(classUtilitiesClass, classLoader);

            Class mainClass = Class.forName("org.bhavaya.ui.Main", true, classLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(mainClass);
        } catch (Exception e) {
            handleException(e);
        }
    }

    protected static void handleException(Throwable e) {
        StringBuffer message = new StringBuffer("Could not start: " + ApplicationLauncher.class.getName() + "\n");
        appendException(e, message);
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void appendException(Throwable e, StringBuffer message) {
        message.append(e).append("\n");
        Throwable parent = e;
        Throwable cause = e.getCause();
        while (cause != null && cause != parent) {
            message.append(cause).append("\n");
            parent = cause;
            cause = parent.getCause();
        }
    }

    public static class SocketUrlClassLoader extends MutableUrlClassLoader {
        private Reader in;
        private Writer out;
        private Socket socket;
        private Map validExecutedCommands;
        private Set invalidExecutedCommands;

        public SocketUrlClassLoader(ClassLoader parentClassLoader, String host, int port) throws IOException {
            super(parentClassLoader);
            System.out.println("Connecting to host: " + host + ", port: " + port);
            socket = new Socket(host, port);
            in = new InputStreamReader(socket.getInputStream());
            out = new OutputStreamWriter(socket.getOutputStream());
            validExecutedCommands = new HashMap();
            invalidExecutedCommands = new HashSet();
        }

        public Class findClass(String name) throws ClassNotFoundException {
            if (socket.isClosed()) System.exit(1);
            String command = "findClass:" + name + "\n";
            if (invalidExecutedCommands.contains(command)) throw new ClassNotFoundException(name);

            try {
                synchronized (validExecutedCommands) {
//                    System.out.println("command: " + command);
                    String urlExternalForm = (String) validExecutedCommands.get(command);
                    if (urlExternalForm == null) {
                        out.write(command);
                        out.flush();
                        urlExternalForm = readLine(in);
                        URL url = new URL(urlExternalForm);
                        addURL(url);
                        validExecutedCommands.put(command, urlExternalForm);
                    }
                }
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                invalidExecutedCommands.add(command);
                throw e;
            } catch (Exception e) {
//                System.out.println("Error findClass: " + name + " in thread: " + Thread.currentThread().getName() + ": " + e);
                invalidExecutedCommands.add(command);
                throw new ClassNotFoundException(name, e);
            }
        }

        protected String findLibrary(String libname) {
            if (socket.isClosed()) System.exit(1);
            URL url = resourceUrlRequest("findLibrary", libname);
            String file = null;
            if (url != null) {
                file = url.getFile();
            }
            System.out.println("Mapped library: " + libname + " to: " + file);
            return file;
        }

        public URL findResource(String name) {
            if (socket.isClosed()) System.exit(1);
            return resourceUrlRequest("findResource", name);
        }

        public Enumeration findResources(String name) throws IOException {
            if (socket.isClosed()) System.exit(1);
            final URL[] urls = resourceUrlsRequest("findResources", name);
            Enumeration enumeration = new ArrayEnumeration(urls);
            return enumeration;
        }

        private URL resourceUrlRequest(String commandPrefix, String resourceName) {
            String command = commandPrefix + ":" + resourceName + "\n";
            if (invalidExecutedCommands.contains(command)) return null;

            try {
                synchronized (validExecutedCommands) {
//                    System.out.println("command: " + command);
                    URL url = (URL) validExecutedCommands.get(command);
                    if (url == null) {
                        out.write(command);
                        out.flush();
                        String urlExternalForm = readLine(in);
                        url = new URL(urlExternalForm);
                        validExecutedCommands.put(command, url);
                    }
                    return url;
                }

            } catch (Exception e) {
//                System.out.println("Error resourceUrlRequest: " + resourceName + " in thread: " + Thread.currentThread().getName() + ": " + e);
                invalidExecutedCommands.add(command);
                return null;
            }
        }

        private URL[] resourceUrlsRequest(String commandPrefix, String resourceName) {
            String command = commandPrefix + ":" + resourceName + "\n";
            if (invalidExecutedCommands.contains(command)) return null;

            try {
                synchronized (validExecutedCommands) {
//                    System.out.println("command: " + command);
                    URL[] urls = (URL[]) validExecutedCommands.get(command);
                    if (urls == null) {
                        out.write(command);
                        out.flush();
                        String urlExternalFormsString = readLine(in);

                        if (urlExternalFormsString.equals("")) {
                            invalidExecutedCommands.add(command);
                            return null;
                        }

                        String[] urlExternalForms = urlExternalFormsString.split(",");
                        urls = new URL[urlExternalForms.length];
                        for (int i = 0; i < urlExternalForms.length; i++) {
                            String urlExternalForm = urlExternalForms[i];
                            URL url = new URL(urlExternalForm);
                            urls[i] = url;
                        }

                        validExecutedCommands.put(command, urls);
                    }
                    return urls;
                }
            } catch (Exception e) {
//                System.out.println("Error resourceUrlsRequest: " + resourceName + " in thread: " + Thread.currentThread().getName() + ": " + e);
                invalidExecutedCommands.add(command);
                return null;
            }
        }

        private static String readLine(Reader in) throws IOException {
            StringBuffer line = new StringBuffer();
            char c = (char) in.read();
            while (c != '\n' && c != -1) {
                line.append(c);
                c = (char) in.read();
            }
            String result = line.toString();
//            System.out.println("result: " + result);
            return result;
        }
    }

}
