package org.sunflow.core;

import org.sunflow.image.Color;

/**
 * Represents an image output device.
 */
public interface Display {

    /**
     * This is called before an image is rendered to indicate how large the
     * rendered image will be. This allows the display driver to write out image
     * headers or allocate surfaces. Bucket size will be 0 when called from a
     * non-bucket based source.
     *
     * @param w width of the rendered image in pixels
     * @param h height of the rendered image in pixels
     * @param bucketSize size of the buckets in pixels
     */
    void imageBegin(int w, int h, int bucketSize);

    /**
     * Prepare the specified area to be rendered. This may be used to highlight
     * the work in progress area or simply to setup the display driver to
     * receive the specified portion of the image
     *
     * @param x x coordinate of the bucket within the image
     * @param y y coordinate of the bucket within the image
     * @param w width of the bucket in pixels
     * @param h height of the bucket in pixels
     * @param id unique identifier corresponding to the thread which invoked
     * this call
     */
    void imagePrepare(int x, int y, int w, int h, int id);

    /**
     * Update the current image with a bucket of data. The region is guarenteed
     * to be within the bounds created by the call to imageBegin. No clipping is
     * necessary. Colors are passed in unprocessed. It is up the display driver
     * to do any type of quantization, gamma compensation or tone-mapping
     * needed. The array of colors will be exactly
     * <code>w * h</code> long and in row major order.
     *
     * @param x x coordinate of the bucket within the image
     * @param y y coordinate of the bucket within the image
     * @param w width of the bucket in pixels
     * @param h height of the bucket in pixels
     * @param data bucket data, this array will be exactly <code>w * h</code>
     * long
     * @param alpha pixel coverage data, this array will be exactly
     * <code>w * h</code> long
     */
    void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha);

    /**
     * Update the current image with a region of flat color. This is used by
     * progressive rendering to render progressively smaller regions of the
     * screen which will overlap. The region is guarenteed to be within the
     * bounds created by the call to imageBegin. No clipping is necessary.
     * Colors are passed in unprocessed. It is up the display driver to do any
     * type of quantization , gamma compensation or tone-mapping needed.
     *
     * @param x x coordinate of the region within the image
     * @param y y coordinate of the region within the image
     * @param w with of the region in pixels
     * @param h height of the region in pixels
     * @param c color to fill the region with
     * @param alpha pixel coverage
     */
    void imageFill(int x, int y, int w, int h, Color c, float alpha);

    /**
     * This call is made after the image has been rendered. This allows the
     * display driver to close any open files, write the image to disk or flush
     * any other type of buffers.
     */
    void imageEnd();
}