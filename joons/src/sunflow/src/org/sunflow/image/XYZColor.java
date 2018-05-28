package org.sunflow.image;

public final class XYZColor {

    private float X, Y, Z;

    public XYZColor() {
    }

    public XYZColor(float X, float Y, float Z) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    public final float getX() {
        return X;
    }

    public final float getY() {
        return Y;
    }

    public final float getZ() {
        return Z;
    }

    public final XYZColor mul(float s) {
        X *= s;
        Y *= s;
        Z *= s;
        return this;
    }

    public final void normalize() {
        float XYZ = X + Y + Z;
        if (XYZ < 1e-6f) {
            return;
        }
        float s = 1 / XYZ;
        X *= s;
        Y *= s;
        Z *= s;
    }

    @Override
    public final String toString() {
        return String.format("(%.3f, %.3f, %.3f)", X, Y, Z);
    }
}