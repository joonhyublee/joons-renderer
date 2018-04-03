package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class LanczosFilter implements Filter {

    @Override
    public float getSize() {
        return 4.0f;
    }

    @Override
    public float get(float x, float y) {
        return sinc1d(x * 0.5f) * sinc1d(y * 0.5f);
    }

    private float sinc1d(float x) {
        x = Math.abs(x);
        if (x < 1e-5f) {
            return 1;
        }
        if (x > 1.0f) {
            return 0;
        }
        x *= Math.PI;
        float sinc = (float) Math.sin(3 * x) / (3 * x);
        float lanczos = (float) Math.sin(x) / x;
        return sinc * lanczos;
    }
}