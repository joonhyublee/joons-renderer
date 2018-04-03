package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

public class BitmapRGBA8 extends Bitmap {

    private int w, h;
    private byte[] data;

    public BitmapRGBA8(int w, int h, byte[] data) {
        this.w = w;
        this.h = h;
        this.data = data;
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public Color readColor(int x, int y) {
        int index = 4 * (x + y * w);
        float r = (data[index + 0] & 0xFF) * INV255;
        float g = (data[index + 1] & 0xFF) * INV255;
        float b = (data[index + 2] & 0xFF) * INV255;
        return new Color(r, g, b);
    }

    @Override
    public float readAlpha(int x, int y) {
        return (data[4 * (x + y * w) + 3] & 0xFF) * INV255;
    }
}