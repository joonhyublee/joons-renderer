package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

public class BitmapGA8 extends Bitmap {

    private int w, h;
    private byte[] data;

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
        return new Color((data[2 * (x + y * w) + 0] & 0xFF) * INV255);
    }

    @Override
    public float readAlpha(int x, int y) {
        return (data[2 * (x + y * w) + 1] & 0xFF) * INV255;
    }
}