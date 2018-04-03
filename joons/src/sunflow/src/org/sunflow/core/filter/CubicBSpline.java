package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class CubicBSpline implements Filter {

    @Override
    public float get(float x, float y) {
        return B3(x) * B3(y);
    }

    @Override
    public float getSize() {
        return 4.0f;
    }

    private float B3(float t) {
        t = Math.abs(t);
        if (t <= 1) {
            return b1(1 - t);
        }
        return b0(2 - t);
    }

    private float b0(float t) {
        return t * t * t * (1.0f / 6);
    }

    private float b1(float t) {
        return (1.0f / 6) * (-3 * t * t * t + 3 * t * t + 3 * t + 1);
    }
}