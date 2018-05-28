package org.sunflow.image;

import java.io.IOException;

/**
 * This is a very simple interface, designed to handle loading of bitmap data.
 */
public interface BitmapReader {

    /**
     * Load the specified filename. This method should throw exception if it
     * encounters any errors. If the file is valid but its contents are not
     * (invalid header for example), a {@link BitmapFormatException} may be
     * thrown. It is an error for this method to return
     * <code>null</code>.
     *
     * @param filename image filename to load
     * @param isLinear if this is <code>true</code>, the bitmap is assumed to be
     * already in linear space. This can be usefull when reading greyscale
     * images for bump mapping for example. HDR formats can ignore this flag
     * since they usually always store data in linear form.
     * @return a new {@link Bitmap} object
     */
    public Bitmap load(String filename, boolean isLinear) throws IOException, BitmapFormatException;

    /**
     * This exception can be used internally by bitmap readers to signal they
     * have encountered a valid file but which contains invalid content.
     */
    @SuppressWarnings("serial")
    public static final class BitmapFormatException extends Exception {

        public BitmapFormatException(String message) {
            super(message);
        }
    }
}