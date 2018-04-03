package org.sunflow.core;

import org.sunflow.image.Color;

/**
 * This represents a global illumination algorithm. It provides an interface to
 * compute indirect diffuse bounces of light and make those results available to
 * shaders.
 */
public interface GIEngine {

    /**
     * This is an optional method for engines that contain a secondary
     * illumination engine which can return an approximation of the global
     * radiance in the scene (like a photon map). Engines can safely return
     * <code>Color.BLACK</code> if they can't or don't wish to support this.
     *
     * @param state shading state
     * @return color approximating global radiance
     */
    public Color getGlobalRadiance(ShadingState state);

    /**
     * Initialize the engine. This is called before rendering begins.
     *
     * @return <code>true</code> if the init phase succeeded, <code>false</code>
     * otherwise
     */
    public boolean init(Options options, Scene scene);

    /**
     * Return the incomming irradiance due to indirect diffuse illumination at
     * the specified surface point.
     *
     * @param state current render state describing the point to be computed
     * @param diffuseReflectance diffuse albedo of the point being shaded, this
     * can be used for importance tracking
     * @return irradiance from indirect diffuse illumination at the specified
     * point
     */
    public Color getIrradiance(ShadingState state, Color diffuseReflectance);
}