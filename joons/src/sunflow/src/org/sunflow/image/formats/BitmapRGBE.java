package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

public class BitmapRGBE extends Bitmap {

    private int w, h;
    private int[] data;
    private static final float[] EXPONENT = new float[256];

    static {
        EXPONENT[0] = 0;
        for (int i = 1; i < 256; i++) {
            float f = 1.0f;
            int e = i - (128 + 8);
            if (e > 0) {
                for (int j = 0; j < e; j++) {
                    f *= 2.0f;
                }
            } else {
                for (int j = 0; j < -e; j++) {
                    f *= 0.5f;
                }
            }
            EXPONENT[i] = f;
        }
    }

    public BitmapRGBE(int w, int h, int[] data) {
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
        int rgbe = data[x + y * w];
        float f = EXPONENT[rgbe & 0xFF];
        float r = f * ((rgbe >>> 24) + 0.5f);
        float g = f * (((rgbe >> 16) & 0xFF) + 0.5f);
        float b = f * (((rgbe >> 8) & 0xFF) + 0.5f);
        return new Color(r, g, b);
    }

    @Override
    public float readAlpha(int x, int y) {
        return 1;
    }
}