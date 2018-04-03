package org.sunflow.image.writers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;
import org.sunflow.image.ColorEncoder;

public class HDRBitmapWriter implements BitmapWriter {

    private String filename;
    private int width, height;
    private int[] data;

    public void configure(String option, String value) {
    }

    public void openFile(String filename) throws IOException {
        this.filename = filename;
    }

    public void writeHeader(int width, int height, int tileSize) throws IOException, UnsupportedOperationException {
        this.width = width;
        this.height = height;
        data = new int[width * height];
    }

    public void writeTile(int x, int y, int w, int h, Color[] color, float[] alpha) throws IOException {
        int[] tileData = ColorEncoder.encodeRGBE(color);
        for (int j = 0, index = 0, pixel = x + y * width; j < h; j++, pixel += width - w) {
            for (int i = 0; i < w; i++, index++, pixel++) {
                data[pixel] = tileData[index];
            }
        }
    }

    public void closeFile() throws IOException {
        OutputStream f = new BufferedOutputStream(new FileOutputStream(filename));
        f.write("#?RGBE\n".getBytes());
        f.write("FORMAT=32-bit_rle_rgbe\n\n".getBytes());
        f.write(("-Y " + height + " +X " + width + "\n").getBytes());
        for (int i = 0; i < data.length; i++) {
            int rgbe = data[i];
            f.write(rgbe >> 24);
            f.write(rgbe >> 16);
            f.write(rgbe >> 8);
            f.write(rgbe);
        }
        f.close();
    }
}