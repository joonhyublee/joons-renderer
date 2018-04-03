package org.sunflow.system;

import java.io.File;
import java.util.Locale;

public final class FileUtils {

    /**
     * Extract the file extension from the specified filename.
     *
     * @param filename filename to get the extension of
     * @return a string representing the file extension, or <code>null</code> if
     * the filename doesn't have any extension, or is not a file
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        File f = new File(filename);
        if (f.isDirectory()) {
            return null;
        }
        String name = new File(filename).getName();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? null : name.substring(idx + 1).toLowerCase(Locale.ENGLISH);
    }
}