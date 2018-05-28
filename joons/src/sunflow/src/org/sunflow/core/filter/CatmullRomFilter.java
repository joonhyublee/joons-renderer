package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class CatmullRomFilter implements Filter {

    @Override
    public float getSize() {
        return 4.0f;
    }

    @Override
    public float get(float x, float y) {
        return catrom1d(x) * catrom1d(y);
    }

    private float catrom1d(float x) {
        x = Math.abs(x);
        float x2 = x * x;
        float x3 = x * x2;
        if (x >= 2) {
            return 0;
        }
        if (x < 1) {
            return 3 * x3 - 5 * x2 + 2;
        }
        return -x3 + 5 * x2 - 8 * x + 4;
    }
}