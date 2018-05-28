package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class MitchellFilter implements Filter {

    @Override
    public float getSize() {
        return 4.0f;
    }

    @Override
    public float get(float x, float y) {
        return mitchell(x) * mitchell(y);
    }

    private float mitchell(float x) {
        final float B = 1 / 3.0f;
        final float C = 1 / 3.0f;
        final float SIXTH = 1 / 6.0f;
        x = Math.abs(x);
        float x2 = x * x;
        if (x > 1.0f) {
            return ((-B - 6 * C) * x * x2 + (6 * B + 30 * C) * x2 + (-12 * B - 48 * C) * x + (8 * B + 24 * C)) * SIXTH;
        }
        return ((12 - 9 * B - 6 * C) * x * x2 + (-18 + 12 * B + 6 * C) * x2 + (6 - 2 * B)) * SIXTH;
    }
}
