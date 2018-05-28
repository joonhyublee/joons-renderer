package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.MovingMatrix4;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * This represents an instance of a {@link Geometry} into the scene. This class
 * maps object space to world space and maintains a list of shaders and
 * modifiers attached to the surface.
 */
public class Instance implements RenderObject {

    private MovingMatrix4 o2w;
    private MovingMatrix4 w2o;
    private BoundingBox bounds;
    private Geometry geometry;
    private Shader[] shaders;
    private Modifier[] modifiers;

    public Instance() {
        o2w = new MovingMatrix4(null);
        w2o = new MovingMatrix4(null);
        bounds = null;
        geometry = null;
        shaders = null;
        modifiers = null;
    }

    public static Instance createTemporary(PrimitiveList primitives, Matrix4 transform, Shader shader) {
        Instance i = new Instance();
        i.o2w = new MovingMatrix4(transform);
        i.w2o = i.o2w.inverse();
        if (i.w2o == null) {
            UI.printError(Module.GEOM, "Unable to compute transform inverse");
            return null;
        }
        i.geometry = new Geometry(primitives);
        i.shaders = new Shader[]{shader};
        i.updateBounds();
        return i;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        String geometryName = pl.getString("geometry", null);
        if (geometry == null || geometryName != null) {
            if (geometryName == null) {
                UI.printError(Module.GEOM, "geometry parameter missing - unable to create instance");
                return false;
            }
            geometry = api.lookupGeometry(geometryName);
            if (geometry == null) {
                UI.printError(Module.GEOM, "Geometry \"%s\" was not declared yet - instance is invalid", geometryName);
                return false;
            }
        }
        String[] shaderNames = pl.getStringArray("shaders", null);
        if (shaderNames != null) {
            // new shader names have been provided
            shaders = new Shader[shaderNames.length];
            for (int i = 0; i < shaders.length; i++) {
                shaders[i] = api.lookupShader(shaderNames[i]);
                if (shaders[i] == null) {
                    UI.printWarning(Module.GEOM, "Shader \"%s\" was not declared yet - ignoring", shaderNames[i]);
                }
            }
        } else {
            // re-use existing shader array
        }
        String[] modifierNames = pl.getStringArray("modifiers", null);
        if (modifierNames != null) {
            // new modifier names have been provided
            modifiers = new Modifier[modifierNames.length];
            for (int i = 0; i < modifiers.length; i++) {
                modifiers[i] = api.lookupModifier(modifierNames[i]);
                if (modifiers[i] == null) {
                    UI.printWarning(Module.GEOM, "Modifier \"%s\" was not declared yet - ignoring", modifierNames[i]);
                }
            }
        }
        o2w = pl.getMovingMatrix("transform", o2w);
        w2o = o2w.inverse();
        if (w2o == null) {
            UI.printError(Module.GEOM, "Unable to compute transform inverse");
            return false;
        }
        return true;
    }

    /**
     * Recompute world space bounding box of this instance.
     */
    public void updateBounds() {
        bounds = geometry.getWorldBounds(o2w.getData(0));
        for (int i = 1; i < o2w.numSegments(); i++) {
            bounds.include(geometry.getWorldBounds(o2w.getData(i)));
        }
    }

    /**
     * Checks to see if this instance is relative to the specified geometry.
     *
     * @param g geometry to check against
     * @return <code>true</code> if the instanced geometry is equals to g,
     * <code>false</code> otherwise
     */
    public boolean hasGeometry(Geometry g) {
        return geometry == g;
    }

    /**
     * Remove the specified shader from the instance's list if it is being used.
     *
     * @param s shader to remove
     */
    public void removeShader(Shader s) {
        if (shaders != null) {
            for (int i = 0; i < shaders.length; i++) {
                if (shaders[i] == s) {
                    shaders[i] = null;
                }
            }
        }
    }

    /**
     * Remove the specified modifier from the instance's list if it is being
     * used.
     *
     * @param m modifier to remove
     */
    public void removeModifier(Modifier m) {
        if (modifiers != null) {
            for (int i = 0; i < modifiers.length; i++) {
                if (modifiers[i] == m) {
                    modifiers[i] = null;
                }
            }
        }
    }

    /**
     * Get the world space bounding box for this instance.
     *
     * @return bounding box in world space
     */
    public BoundingBox getBounds() {
        return bounds;
    }

    int getNumPrimitives() {
        return geometry.getNumPrimitives();
    }

    void intersect(Ray r, IntersectionState state) {
        Ray localRay = r.transform(w2o.sample(state.time));
        state.current = this;
        geometry.intersect(localRay, state);
        // FIXME: transfer max distance to current ray
        r.setMax(localRay.getMax());
    }

    /**
     * Prepare the shading state for shader invocation. This also runs the
     * currently attached surface modifier.
     *
     * @param state shading state to be prepared
     */
    public void prepareShadingState(ShadingState state) {
        geometry.prepareShadingState(state);
        if (state.getNormal() != null && state.getGeoNormal() != null) {
            state.correctShadingNormal();
        }
        // run modifier if it was provided
        if (state.getModifier() != null) {
            state.getModifier().modify(state);
        }
    }

    /**
     * Get a shader for the instance's list.
     *
     * @param i index into the shader list
     * @return requested shader, or <code>null</code> if the input is invalid
     */
    public Shader getShader(int i) {
        if (shaders == null || i < 0 || i >= shaders.length) {
            return null;
        }
        return shaders[i];
    }

    /**
     * Get a modifier for the instance's list.
     *
     * @param i index into the modifier list
     * @return requested modifier, or <code>null</code> if the input is invalid
     */
    public Modifier getModifier(int i) {
        if (modifiers == null || i < 0 || i >= modifiers.length) {
            return null;
        }
        return modifiers[i];
    }

    Matrix4 getObjectToWorld(float time) {
        return o2w.sample(time);
    }

    Matrix4 getWorldToObject(float time) {
        return w2o.sample(time);
    }

    PrimitiveList getBakingPrimitives() {
        return geometry.getBakingPrimitives();
    }

    Geometry getGeometry() {
        return geometry;
    }
}