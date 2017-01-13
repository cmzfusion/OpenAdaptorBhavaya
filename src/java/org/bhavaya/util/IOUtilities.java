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

import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * DO NOT PUT LOGGING IN THIS CLASS.  Initialisation of Log.class depends on IOUtilities.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.14 $
 */
public class IOUtilities {
    public static final String RESOURCE_DIR = "resources";
    public static final String CONFIG_BASE_DIR = ".bhavaya";

    private static final String OVERRIDE_RESOURCE_DIR_SYSTEM_PROPERTY = "OVERRIDE_RESOURCE_DIR";
    private static final String HOME_PATH_DRIVE_KEY = "HOMEDRIVE";
    private static String userDataDirectory;
    private static String userBaseDirectory;
    private static String tempDirectory;
    private static final String SAMPLE_RESOURCE_NAME = "application.xml";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static String userCacheDirectory;

    public static void closeStream(InputStream stream) {
        try {
            if (stream != null) stream.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void closeStream(OutputStream stream) {
        try {
            if (stream != null) stream.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void closeReader(Reader reader) {
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * This method is the recommended way to retreive resources.  It will return a URL to desired resource
     * using this class' ClassLoader which allows resources to be retrieved from files or JARS.  The method changes
     * behaviour if there a System property called "OVERRIDE_RESOURCE_DIR" is set.
     * In this case, it will always try to load the resource from the override as an absolute location
     * before falling back on the classloader.
     *
     * @return an <code>URL</code> to an application resource.
     */
    public static final URL getResource(String resourceName) {
        URL url = null;

        if (url == null) {
            String overrideResourceDir = System.getProperty(OVERRIDE_RESOURCE_DIR_SYSTEM_PROPERTY);
            if (overrideResourceDir != null) {
                String filename = overrideResourceDir + "/" + resourceName;
                url = getResourceFromFile(filename);
            }
        }

        if (url == null) {
            url = getResourceFromFile(resourceName);
        }

        // Otherwise try the application classloader
        if (url == null) {
            String prefixedResourceName = "/" + RESOURCE_DIR + "/" + resourceName;
            url = getResourceFromClasspath(prefixedResourceName);
        }

        if (url == null) {
            String absoluteResourceName = resourceName;
            if (!resourceName.startsWith("/")) absoluteResourceName = "/" + resourceName; // we dont want to search in the IOUtilities's package
            url = getResourceFromClasspath(absoluteResourceName);
        }

        if (url == null) System.out.println("Could not find resource: " + resourceName);
        return url;
    }

    private static URL getResourceFromClasspath(String prefixedResourceName) {
        URL url = IOUtilities.class.getResource(prefixedResourceName);
        return url;
    }

    private static URL getResourceFromFile(String filename) {
        URL url = null;
        File file = new File(filename);

        if (file.exists()) {
            try {
                url = file.toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return url;
    }

    /**
     * This method is the recommended way to retreive resource streams.  It will return a stream to desired resource
     * using this class' ClassLoader which allows resources to be retrieved from files or JARS.  The method changes
     * behaviour if there a System property called "OVERRIDE_RESOURCE_DIR" is set.
     * In this case, it will always try to load the resource from the override as an absolute location
     * before falling back on the classloader.
     *
     * @return an <code>InputStream</code> to an application resource.
     */
    public static final InputStream getResourceAsStream(String resourceName) {
        InputStream is = null;
        URL url = getResource(resourceName);
        if (url != null) {
            try {
                URLConnection urlConnection = url.openConnection();
                is = urlConnection.getInputStream();
//                System.out.println("Found resource: " + url);
            } catch (IOException e) {
            }
        }
        return is;
    }

    public static final void writeStringToFile(String filename, String text) throws IOException {
        writeStringToFile(new File(filename), text);
    }

    public static final void writeStringToFile(File file, String text) throws IOException {
        System.out.println("Writing: " + file);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        if (text == null) {
            throw new IllegalArgumentException("Text is null");
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(file));

        try {
            out.write(text);
        } catch (IOException e){
            throw e;
        } finally {
            out.close();
        }
    }

    public static final String convertStreamToString(InputStream is) throws IOException {
        final int BUF_SIZE = 4096;
        final byte[] BUFFER = new byte[BUF_SIZE];

        StringBuffer returnBuffer = new StringBuffer();
        try {
            int bytesRead;
            while ((bytesRead = is.read(BUFFER)) != -1) {
                returnBuffer.append(new String(BUFFER, 0, bytesRead));
            }
        } finally {
            IOUtilities.closeStream(is);
        }
        return returnBuffer.toString();
    }

    public static String getTempDirectory() {
        if (tempDirectory == null) {
            try {
                tempDirectory = File.createTempFile("bhavaya", "temp").getParent();
            } catch (Exception e) {
                tempDirectory = System.getProperty("user.home") + "./bhavaya/temp";
            }
        }

        return tempDirectory;
    }


    public static String readFile(String filename) throws IOException {
        return readFile(new File(filename));
    }

    public static String readFile(File file) throws IOException {
        System.out.println("Reading: " + file);
        InputStream in = null;
        String string = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            string = IOUtilities.convertStreamToString(in);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
        return string;
    }

    public static String getUserBaseDirectory() {
        if (userBaseDirectory == null) {
            try {
                userBaseDirectory = Environment.getProperty(HOME_PATH_DRIVE_KEY);
            } catch (Throwable t) {
                System.err.println(t);
            }
            File file = userBaseDirectory != null ? new File(userBaseDirectory) : null;
            if (file == null || !file.exists() || !file.canRead()) {
                userBaseDirectory = System.getProperty("user.home");
            }
        }
        return userBaseDirectory;
    }

    public static String getUserConfigDirectory() {
        if (userDataDirectory == null) {
            userDataDirectory = getUserBaseDirectory() + "/" + CONFIG_BASE_DIR + "/" + ApplicationInfo.getInstance().getId();
        }
        return userDataDirectory;
    }

    public static String getOldUserConfigDirectory() {
        String oldConfigDirName = IOUtilities.getUserBaseDirectory() + "/.shiva/" + ApplicationInfo.getInstance().getId();
        File oldConfigDir = new File(oldConfigDirName);
        if (oldConfigDir.exists()) return oldConfigDirName;
        return null;
    }

    public static String getUserCacheDirectory() {
        if (userCacheDirectory == null) {
            userCacheDirectory = System.getProperty("user.home") + "/" + CONFIG_BASE_DIR + "/caches";
        }
        return userCacheDirectory;
    }

    public static boolean dirExists(String dirString) {
        File dir = new File(dirString);
        return (dir.exists() && dir.isDirectory());
    }

    public static String[] getMatchingFilesInDir(String dir, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        ArrayList matchingFiles = new ArrayList();
        String[] allFiles = new File(dir).list();
        for (int i = 0; i < allFiles.length; i++) {
            String file = allFiles[i];
            if (p.matcher(file).find()) matchingFiles.add(file);
        }
        return (String[]) matchingFiles.toArray(new String[matchingFiles.size()]);
    }

    public static String[] getResourceSubDirectoriesInDir(String dir) {
        return getResourceObjectsInDir(dir, false);
    }

    public static String[] getResourceFilesInDir(String dir) {
        return getResourceObjectsInDir(dir, true);
    }

    private static String[] getResourceObjectsInDir(String dir, final boolean filesNotDirs) {
        try {
            if (dir.endsWith("/")) dir = dir.substring(0, dir.length() - 1);
            if (dir.startsWith("/")) dir = dir.substring(1);

            // Use application XML to locate url for resource directory.
            URL url = getResource(SAMPLE_RESOURCE_NAME);
            String uri = url.getFile();

            // Check for JAR file
            if (url.getProtocol().equals("jar")) {
                ArrayList foundFiles = new ArrayList();

                JarURLConnection connection = (JarURLConnection) url.openConnection();
                //JarFile jarFile = connection.getJarFile(); JNLPCachedJarURLConnection.getJarFile() was overriden
                //in 1.5.0_16 to return null, to fix a security hole, so now we have to use Nick's hack
                JarFile jarFile = ClassUtilities.getJarFile(url);

                String sampleEntryName =  connection.getEntryName();
                String sampleEntryDirectory = sampleEntryName.substring(0, sampleEntryName.length() - SAMPLE_RESOURCE_NAME.length());
                String searchDirectory = sampleEntryDirectory + dir + "/";

                Enumeration enumeration = jarFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
                    String name = zipEntry.getName();
                    if (name.endsWith("/")) name = name.substring(0, name.length() - 1);

                    if (name.startsWith(searchDirectory)) {
                        String normalisedName = name.substring(searchDirectory.length());
                        if (normalisedName.startsWith("/")) normalisedName = normalisedName.substring(1);

                        if (normalisedName.length() == 0) continue;
                        if ((filesNotDirs && !zipEntry.isDirectory()) || (!filesNotDirs && zipEntry.isDirectory())) {
                            foundFiles.add(normalisedName);
                        }
                    }
                }

                return (String[]) foundFiles.toArray(new String[foundFiles.size()]);
            } else {
                // Else we're operating from the filesystem
                String uriDirectory = uri.substring(0, uri.length() - SAMPLE_RESOURCE_NAME.length());
                String searchDirectory = uriDirectory + dir;

                File file = new File(searchDirectory);
                if (file.exists()) {
                    File[] matchingFiles = file.listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            return (filesNotDirs && !pathname.isDirectory()) || (!filesNotDirs && pathname.isDirectory());
                        }
                    });
                    if (matchingFiles == null) return EMPTY_STRING_ARRAY;
                    String[] matchingFilenames = new String[matchingFiles.length];
                    for (int i = 0; i < matchingFiles.length; i++) {
                        File matchingFile = matchingFiles[i];
                        matchingFilenames[i] = matchingFile.getName();
                    }
                    return matchingFilenames;
                }
                return EMPTY_STRING_ARRAY;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies a source file to a destination file or directory OR
     * Copies a source directory to a destination directory.
     *
     * @param source Source file name or directory name
     * @param destination Destination file name or directory name
     * @param recursive boolean flag, if true will recursively copy sub-directories
     *
     * @exception IOException
     */
    public static void copy(String source, String destination, boolean recursive) throws FileNotFoundException, IOException {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);
        copy(sourceFile, destinationFile, recursive);
    }

    /**
     * Copies a source file to a destination file or directory OR
     * Copies a source directory to a destination directory.
     *
     * @param source Source file or directory
     * @param destination Destination file or directory
     * @param recursive boolean flag, if true will recursively copy sub-directories
     *
     * @exception IOException
     */
    public static void copy(File source, File destination, boolean recursive) throws FileNotFoundException, IOException {
        if (source.isFile()) {
            copyFile(source, destination);
        } else if (source.isDirectory()) {
            copyDirectory(source, destination, recursive);
        }
    }


    public static void copyFile(String source, String destination) throws IOException {
        copyFile(new File(source), new File(destination));
    }

    public static void copyFile(File source, File destination) throws IOException {
        if (!source.exists()) throw new FileNotFoundException("Source not found: " + source);
        if (!source.canRead()) throw new IOException("Source is unreadable: " + source);
        if (!source.isFile()) throw new IOException("Source is not a file: " + source);
        if (destination.exists() && !destination.canWrite()) throw new IOException("Destination is unwritable: " + destination);

        if (destination.getParentFile() != null && !destination.getParentFile().exists()) {
            System.out.println("Creating directory: " + destination.getParentFile());
            destination.getParentFile().mkdirs();
        }

        if (destination.isDirectory()) {
            destination = new File(destination.toString() + File.separator + source.toString());
        }

        System.out.println("Copying " + source + " to " + destination);
        copyFile(new FileInputStream(source), new FileOutputStream(destination));
    }

    public static void copyFile(FileInputStream in, FileOutputStream out) throws IOException {
        FileChannel srcChannel = in.getChannel();
        FileChannel destChannel = out.getChannel();
        try {
            srcChannel.transferTo(0, srcChannel.size(), destChannel);
        } finally {
            if (srcChannel != null) srcChannel.close();
            if (destChannel != null) destChannel.close();
        }
    }

    /**
     * Copies a source directory to a destination directory.
     *
     * @param source Source directory
     * @param destination Destination directory
     * @param recursive boolean flag, if true will recursively copy sub-directories
     *
     * @exception IOException
     */
    private static void copyDirectory(File source, File destination, boolean recursive) throws FileNotFoundException, IOException {
        if (!source.exists()) throw new FileNotFoundException("Source not found: " + source);
        if (!source.canRead()) throw new IOException("Source is unreadable: " + source);
        if (!source.isDirectory()) throw new IOException("Source is not a directory: " + source);
        if (destination.exists() && !destination.canWrite()) throw new IOException("Destination is unwritable: " + destination);
        if (destination.isFile()) throw new IOException("Cannot copy directory " + source.toString() + " to file " + destination.toString());

        if (!destination.exists()) {
            System.out.println("Creating directory: " + destination);
            destination.mkdirs();
        }

        String targetName;
        File targetSource;
        File targetDestination;

        String[] files = source.list();

        for (int i = 0; i < files.length; i++) {
            targetName = files[i];
            targetSource = new File(source.toString() + File.separator + targetName);
            targetDestination = new File(destination.toString() + File.separator + targetName);

            if (targetSource.isFile()) {
                copy(targetSource, targetDestination, recursive);
            } else if (targetSource.isDirectory() && recursive) {
                copy(targetSource, targetDestination, recursive);
            }
        }
    }

    public static void delete(String file) {
        delete(new File(file));
    }

    public static void delete(File file) {
        if (!file.exists()) return;

        System.out.println("Deleting: " + file);
        if (file.isFile()) {
            file.delete();
            return;
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
            file.delete();
        }
    }
}
