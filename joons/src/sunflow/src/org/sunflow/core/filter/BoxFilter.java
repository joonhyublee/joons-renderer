package org.sunflow.core.filter;

import org.sunflow.core.Filter;

public class BoxFilter implements Filter {

    @Override
    public float getSize() {
        return 1.0f;
    }

    @Override
    public float get(float x, float y) {
        return 1.0f;
    }
}