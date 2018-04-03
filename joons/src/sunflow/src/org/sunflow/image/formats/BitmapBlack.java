package org.sunflow.image.formats;

import org.sunflow.image.Bitmap;
import org.sunflow.image.Color;

public class BitmapBlack extends Bitmap {

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public Color readColor(int x, int y) {
        return Color.BLACK;
    }

    @Override
    public float readAlpha(int x, int y) {
        return 0;
    }
}