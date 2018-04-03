package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class SincFilter implements Filter {

    @Override
    public float getSize() {
        return 4.0f;
    }

    @Override
    public float get(float x, float y) {
        return sinc1d(x) * sinc1d(y);
    }

    private float sinc1d(float x) {
        x = Math.abs(x);
        if (x < 0.0001f) {
            return 1.0f;
        }
        x *= Math.PI;
        return (float) Math.sin(x) / x;
    }
}