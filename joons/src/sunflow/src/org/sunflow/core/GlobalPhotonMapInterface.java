package org.sunflow.core;

import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

/**
 * Represents a global photon map. This is a structure which can return a rough
 * approximation of the diffuse radiance at a given surface point.
 */
public interface GlobalPhotonMapInterface extends PhotonStore {

    /**
     * Lookup the global diffuse radiance at the specified surface point.
     *
     * @param p surface position
     * @param n surface normal
     * @return an approximation of global diffuse radiance at this point
     */
    public Color getRadiance(Point3 p, Vector3 n);
}