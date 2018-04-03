package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

public class Background implements PrimitiveList {

    public Background() {
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        if (state.getDepth() == 0) {
            state.setShader(state.getInstance().getShader(0));
        }
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return 0;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        return null;
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        if (r.getMax() == Float.POSITIVE_INFINITY) {
            state.setIntersection(0);
        }
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}