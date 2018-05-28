package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class GaussianFilter implements Filter {

    private float es2;

    public GaussianFilter() {
        es2 = (float) -Math.exp(-getSize() * getSize());
    }

    @Override
    public final float getSize() {
        return 3.0f;
    }

    @Override
    public float get(float x, float y) {
        float gx = (float) Math.exp(-x * x) + es2;
        float gy = (float) Math.exp(-y * y) + es2;
        return gx * gy;
    }
}