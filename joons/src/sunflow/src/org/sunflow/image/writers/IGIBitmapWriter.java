package org.sunflow.image.writers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;
import org.sunflow.image.XYZColor;

/**
 * Writes images in Indigo's native XYZ format.
 * http://www2.indigorenderer.com/joomla/forum/viewtopic.php?p=11430
 */
public class IGIBitmapWriter implements BitmapWriter {

    private String filename;
    private int width, height;
    private float[] xyz;

    public void configure(String option, String value) {
    }

    public void openFile(String filename) throws IOException {
        this.filename = filename;
    }

    public void writeHeader(int width, int height, int tileSize) throws IOException, UnsupportedOperationException {
        this.width = width;
        this.height = height;
        xyz = new float[width * height * 3];
    }

    public void writeTile(int x, int y, int w, int h, Color[] color, float[] alpha) throws IOException {
        for (int j = 0, index = 0, pixel = 3 * (x + y * width); j < h; j++, pixel += 3 * (width - w)) {
            for (int i = 0; i < w; i++, index++, pixel += 3) {
                XYZColor c = Color.NATIVE_SPACE.convertRGBtoXYZ(color[index]);
                xyz[pixel + 0] = c.getX();
                xyz[pixel + 1] = c.getY();
                xyz[pixel + 2] = c.getZ();
            }
        }
    }

    public void closeFile() throws IOException {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(filename));
        write32(stream, 66613373); // magic number
        write32(stream, 1); // version
        write32(stream, 0); // this should be a double - assume it won't be used
        write32(stream, 0);
        write32(stream, width);
        write32(stream, height);
        write32(stream, 1); // super sampling factor
        write32(stream, 0); // compression
        write32(stream, width * height * 12); // data size
        write32(stream, 0); // colorspace
        stream.write(new byte[5000]);
        for (float f : xyz) {
            write32(stream, f);
        }
        stream.close();
    }

    private static final void write32(OutputStream stream, int i) throws IOException {
        stream.write(i & 0xFF);
        stream.write((i >> 8) & 0xFF);
        stream.write((i >> 16) & 0xFF);
        stream.write((i >> 24) & 0xFF);
    }

    private static final void write32(OutputStream stream, float f) throws IOException {
        write32(stream, Float.floatToIntBits(f));
    }
}