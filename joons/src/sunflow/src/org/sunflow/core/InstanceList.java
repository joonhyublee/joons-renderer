package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;

final class InstanceList implements PrimitiveList {

    private Instance[] instances;
    private Instance[] lights;

    InstanceList() {
        instances = new Instance[0];
        clearLightSources();
    }

    InstanceList(Instance[] instances) {
        this.instances = instances;
        clearLightSources();
    }

    void addLightSourceInstances(Instance[] lights) {
        this.lights = lights;
    }

    void clearLightSources() {
        lights = new Instance[0];
    }

    @Override
    public final float getPrimitiveBound(int primID, int i) {
        if (primID < instances.length) {
            return instances[primID].getBounds().getBound(i);
        } else {
            return lights[primID - instances.length].getBounds().getBound(i);
        }
    }

    @Override
    public final BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        for (Instance i : instances) {
            bounds.include(i.getBounds());
        }
        for (Instance i : lights) {
            bounds.include(i.getBounds());
        }
        return bounds;
    }

    @Override
    public final void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        if (primID < instances.length) {
            instances[primID].intersect(r, state);
        } else {
            lights[primID - instances.length].intersect(r, state);
        }
    }

    @Override
    public final int getNumPrimitives() {
        return instances.length + lights.length;
    }

    public final int getNumPrimitives(int primID) {
        return primID < instances.length ? instances[primID].getNumPrimitives() : lights[primID - instances.length].getNumPrimitives();
    }

    @Override
    public final void prepareShadingState(ShadingState state) {
        state.getInstance().prepareShadingState(state);
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    @Override
     public PrimitiveList getBakingPrimitives() {
        return null;
    }
}