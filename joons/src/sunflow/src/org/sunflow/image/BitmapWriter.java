package org.sunflow.image;

import java.io.IOException;

/**
 * This interface is used to represents an image output format. The methods are
 * tile oriented so that tiled image formats may be optimally supported. Note
 * that if the header is declared with a 0 tile size, the image will not be
 * written with identical sized tiles. The image should either be buffered so it
 * can be written all at once on close, or an eror should be thrown. The bitmap
 * writer should be designed so that it is thread safe. Specifically, this means
 * that the tile writing method can be called by several threads.
 */
public interface BitmapWriter {

    /**
     * This method will be called before writing begins. It is used to set
     * common attributes to file writers. Currently supported keywords include:
     * <ul>
     * <li>"compression"</li>
     * <li>"channeltype": "byte", "short", "half", "float"</li>
     * </ul>
     * Note that this method should not fail if its input is not supported or
     * invalid. It should gracefully ignore the error and keep its default
     * state.
     *
     * @param option
     * @param value
     */
    public abstract void configure(String option, String value);

    /**
     * Open a handle to the specified file for writing. If the writer buffers
     * the image and writes it on close, then the filename should be stored.
     *
     * @param filename filename to write the bitmap to
     * @throws IOException thrown if an I/O error occurs
     */
    public abstract void openFile(String filename) throws IOException;

    /**
     * Write the bitmap header. This may be defered if the image is buffered for
     * writing all at once on close. Note that if tile size is positive, data
     * sent to this class is guarenteed to arrive in tiles of that size (except
     * at borders). Otherwise, it should be assumed that the data is random, and
     * that it may overlap. The writer should then either throw an error or
     * start buffering data manually.
     *
     * @param width image width
     * @param height image height
     * @param tileSize tile size or 0 if the image will not be sent in tiled
     * form
     * @throws IOException thrown if an I/O error occurs
     * @throws UnsupportedOperationException thrown if this writer does not
     * support writing the image with the supplied tile size
     */
    public abstract void writeHeader(int width, int height, int tileSize) throws IOException, UnsupportedOperationException;

    /**
     * Write a tile of data. Note that this method may be called by more than
     * one thread, so it should be made thread-safe if possible.
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @param w tile width
     * @param h tile height
     * @param color color data
     * @param alpha alpha data
     * @throws IOException thrown if an I/O error occurs
     */
    public abstract void writeTile(int x, int y, int w, int h, Color[] color, float[] alpha) throws IOException;

    /**
     * Close the file, this completes the bitmap writing process.
     *
     * @throws IOException thrown if an I/O error occurs
     */
    public abstract void closeFile() throws IOException;
}