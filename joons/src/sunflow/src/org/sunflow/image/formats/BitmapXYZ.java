package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;
import org.sunflow.image.XYZColor;

public class BitmapXYZ extends Bitmap {

    private int w, h;
    private float[] data;

    public BitmapXYZ(int w, int h, float[] data) {
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
        int index = 3 * (x + y * w);
        return Color.NATIVE_SPACE.convertXYZtoRGB(new XYZColor(data[index], data[index + 1], data[index + 2])).mul(0.1f);
    }

    @Override
    public float readAlpha(int x, int y) {
        return 1;
    }
}