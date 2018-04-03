package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

public class BitmapG8 extends Bitmap {

    private int w, h;
    private byte[] data;

    public BitmapG8(int w, int h, byte[] data) {
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
        return new Color((data[x + y * w] & 0xFF) * INV255);
    }

    @Override
    public float readAlpha(int x, int y) {
        return 1;
    }
}