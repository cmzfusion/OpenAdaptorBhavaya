package org.bhavaya.util;

import org.bhavaya.ui.diagnostics.DiagnosticEmail;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 29-May-2008
 * Time: 14:30:32
 *
 * Monitors a directory for file with a name matching a supplied pattern.
 * Orders files by modified time and deletes them oldest first if:
 *  - an optional max number of files is exceeded
 *  - an optional max cumulative size in bytes for all files is exceeded
 *  - an optional maximum age is exceeded
 */
public class FileReaper {
    private static final Log log = Log.getCategory(DiagnosticEmail.class);
    private String name;
    private File parentDirectory;
    private int maxFileCount;
    private int maxCumulativeSize;
    private long maxAgeInMillis;
    private Timer timer;
    private java.util.regex.Pattern pattern;

    /**
     * @param name - name to describe this reaper instance
     * @param parentDirectory - directory to look in for files to reap
     * @param fileSearchRegExp - seach expression to identify files to delete
     * @param maxFileCount - maximum number of files to keep, zero or -1 = no maximum
     * @param maxCumulativeSize - maximum cumulative size in bytes of files to keep, zero or -1 = no maximum
     * @param maxAgeInMillis - maximum age of file in millis, zero or -1 = no maximum
     */
    public FileReaper(String name, File parentDirectory, String fileSearchRegExp, int maxFileCount, int maxCumulativeSize, long maxAgeInMillis) {
        this.name = name;
        this.parentDirectory = parentDirectory;
        this.maxFileCount = maxFileCount;
        this.maxCumulativeSize = maxCumulativeSize;
        this.maxAgeInMillis = maxAgeInMillis;
        pattern = java.util.regex.Pattern.compile(fileSearchRegExp);
        timer = new Timer(true);
    }

    public void startReaper(int reapFrequencyMillis) {
        timer.schedule(
                new TimerTask() {
                    public void run() {
                        reap();

                    }
                },
                reapFrequencyMillis,
                reapFrequencyMillis
        );
    }

    public void stopReaper() {
        timer.cancel();
    }

    /**
     * mow those pesky files
     */
    public void reap() {
        if ( parentDirectory.canRead()) {

            File[] files = getFilesByModifiedDate();

            long cumulativeSize = 0, reapCount = 0, reapFail = 0, matches = 0;
            File currentFile;
            for ( int fileCount=0; fileCount < files.length; fileCount ++ ) {
                currentFile = files[fileCount];
                if ( pattern.matcher(currentFile.getName()).matches()) {
                    matches++;
                    log.debug("FileReaper " + name + "checking matching file " + currentFile);
                    cumulativeSize += currentFile.length();
                    if ( shouldDeleteFile(currentFile, fileCount, cumulativeSize)) {
                        if ( currentFile.delete() ) {
                            reapCount++;
                        } else {
                            reapFail++;
                        }
                    }
                }
            }

            log.info("FileReaper " + name + " deleted " + reapCount + " files out of " + matches + " matching candidates, failed to delete " + reapFail);
        }
    }

    private File[] getFilesByModifiedDate() {
        File[] files = parentDirectory.listFiles();

        //sort by last modified date
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return ((Long)f2.lastModified()).compareTo(f1.lastModified());
            }
        });
        return files;
    }

    private boolean shouldDeleteFile(File file, int currentFileIndex, long cumulativeSize) {
        boolean delete = (isDeleteByCumulativeFileSize() && cumulativeSize > maxCumulativeSize);
        delete |= isDeleteByMaxCount() && currentFileIndex >= maxFileCount;
        delete |= isDeleteByMaxAge() && System.currentTimeMillis() - file.lastModified() > maxAgeInMillis;
        return delete;
    }

    private boolean isDeleteByCumulativeFileSize() {
        return maxCumulativeSize > 0;
    }

    private boolean isDeleteByMaxAge() {
        return maxAgeInMillis > 0;
    }

    private boolean isDeleteByMaxCount() {
        return maxFileCount > 0;
    }

}
