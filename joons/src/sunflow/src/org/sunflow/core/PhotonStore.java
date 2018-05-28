package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Vector3;

/**
 * Describes an object which can store photons.
 */
public interface PhotonStore {

    /**
     * Number of photons to emit from this surface.
     *
     * @return number of photons
     */
    int numEmit();

    /**
     * Initialize this object for the specified scene size.
     *
     * @param sceneBounds scene bounding box
     */
    void prepare(Options options, BoundingBox sceneBounds);

    /**
     * Store the specified photon.
     *
     * @param state shading state
     * @param dir photon direction
     * @param power photon power
     * @param diffuse diffuse color at the hit point
     */
    void store(ShadingState state, Vector3 dir, Color power, Color diffuse);

    /**
     * Initialize the map after all photons have been stored. This can be used
     * to balance a kd-tree based photon map for example.
     */
    void init();

    /**
     * Allow photons reflected diffusely?
     *
     * @return <code>true</code> if diffuse bounces should be traced
     */
    boolean allowDiffuseBounced();

    /**
     * Allow specularly reflected photons?
     *
     * @return <code>true</code> if specular reflection bounces should be traced
     */
    boolean allowReflectionBounced();

    /**
     * Allow refracted photons?
     *
     * @return <code>true</code> if refracted bounces should be traced
     */
    boolean allowRefractionBounced();
}