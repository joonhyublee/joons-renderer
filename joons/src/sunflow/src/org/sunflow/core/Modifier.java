package org.sunflow.core;

/**
 * This represents a surface modifier. This is run on each instance prior to
 * shading and can modify the shading state in arbitrary ways to provide effects
 * such as bump mapping.
 */
public interface Modifier extends RenderObject {

    /**
     * Modify the shading state for the point to be shaded.
     *
     * @param state shading state to modify
     */
    public void modify(ShadingState state);
}