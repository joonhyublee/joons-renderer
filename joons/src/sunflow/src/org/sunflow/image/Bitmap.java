package org.sunflow.image;

public abstract class Bitmap {

    protected static final float INV255 = 1.0f / 255;
    protected static final float INV65535 = 1.0f / 65535;

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract Color readColor(int x, int y);

    public abstract float readAlpha(int x, int y);
}