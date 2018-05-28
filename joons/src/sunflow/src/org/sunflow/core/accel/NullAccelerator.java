package org.sunflow.core.accel;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;

public class NullAccelerator implements AccelerationStructure {

    private PrimitiveList primitives;
    private int n;

    public NullAccelerator() {
        primitives = null;
        n = 0;
    }

    public void build(PrimitiveList primitives) {
        this.primitives = primitives;
        n = primitives.getNumPrimitives();
    }

    public void intersect(Ray r, IntersectionState state) {
        for (int i = 0; i < n; i++) {
            primitives.intersectPrimitive(r, i, state);
        }
    }
}