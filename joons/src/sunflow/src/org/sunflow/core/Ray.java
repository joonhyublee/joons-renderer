package org.sunflow.core;

import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

/**
 * This class represents a ray as a oriented half line segment. The ray
 * direction is always normalized. The valid region is delimted by two distances
 * along the ray, tMin and tMax.
 */
public final class Ray {

    public float ox, oy, oz;
    public float dx, dy, dz;
    private float tMin;
    private float tMax;
    private static final float EPSILON = 0;// 0.01f;

    private Ray() {
    }

    /**
     * Creates a new ray that points from the given origin to the given
     * direction. The ray has infinite length. The direction vector is
     * normalized.
     *
     * @param ox ray origin x
     * @param oy ray origin y
     * @param oz ray origin z
     * @param dx ray direction x
     * @param dy ray direction y
     * @param dz ray direction z
     */
    public Ray(float ox, float oy, float oz, float dx, float dy, float dz) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        float in = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.dx *= in;
        this.dy *= in;
        this.dz *= in;
        tMin = EPSILON;
        tMax = Float.POSITIVE_INFINITY;
    }

    /**
     * Creates a new ray that points from the given origin to the given
     * direction. The ray has infinite length. The direction vector is
     * normalized.
     *
     * @param o ray origin
     * @param d ray direction (need not be normalized)
     */
    public Ray(Point3 o, Vector3 d) {
        ox = o.x;
        oy = o.y;
        oz = o.z;
        dx = d.x;
        dy = d.y;
        dz = d.z;
        float in = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= in;
        dy *= in;
        dz *= in;
        tMin = EPSILON;
        tMax = Float.POSITIVE_INFINITY;
    }

    /**
     * Creates a new ray that points from point a to point b. The created ray
     * will set tMin and tMax to limit the ray to the segment (a,b)
     * (non-inclusive of a and b). This is often used to create shadow rays.
     *
     * @param a start point
     * @param b end point
     */
    public Ray(Point3 a, Point3 b) {
        ox = a.x;
        oy = a.y;
        oz = a.z;
        dx = b.x - ox;
        dy = b.y - oy;
        dz = b.z - oz;
        tMin = EPSILON;
        float n = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float in = 1.0f / n;
        dx *= in;
        dy *= in;
        dz *= in;
        tMax = n - EPSILON;
    }

    /**
     * Create a new ray by transforming the supplied one by the given matrix. If
     * the matrix is
     * <code>null</code>, the original ray is returned.
     *
     * @param m matrix to transform the ray by
     */
    public Ray transform(Matrix4 m) {
        if (m == null) {
            return this;
        }
        Ray r = new Ray();
        r.ox = m.transformPX(ox, oy, oz);
        r.oy = m.transformPY(ox, oy, oz);
        r.oz = m.transformPZ(ox, oy, oz);
        r.dx = m.transformVX(dx, dy, dz);
        r.dy = m.transformVY(dx, dy, dz);
        r.dz = m.transformVZ(dx, dy, dz);
        r.tMin = tMin;
        r.tMax = tMax;
        return r;
    }

    /**
     * Normalize the direction component of the ray.
     */
    public void normalize() {
        float in = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= in;
        dy *= in;
        dz *= in;
    }

    /**
     * Gets the minimum distance along the ray - usually 0.
     *
     * @return value of the smallest distance along the ray
     */
    public final float getMin() {
        return tMin;
    }

    /**
     * Gets the maximum distance along the ray. May be infinite.
     *
     * @return value of the largest distance along the ray
     */
    public final float getMax() {
        return tMax;
    }

    /**
     * Creates a vector to represent the direction of the ray.
     *
     * @return a vector equal to the direction of this ray
     */
    public final Vector3 getDirection() {
        return new Vector3(dx, dy, dz);
    }

    /**
     * Checks to see if the specified distance falls within the valid range on
     * this ray. This should always be used before an intersection with the ray
     * is detected.
     *
     * @param t distance to be tested
     * @return <code>true</code> if t falls between the minimum and maximum
     * distance of this ray, <code>false</code> otherwise
     */
    public final boolean isInside(float t) {
        return (tMin < t) && (t < tMax);
    }

    /**
     * Gets the end point of the ray. A reference to
     * <code>dest</code> is returned to support chaining.
     *
     * @param dest reference to the point to store
     * @return reference to <code>dest</code>
     */
    public final Point3 getPoint(Point3 dest) {
        dest.x = ox + (tMax * dx);
        dest.y = oy + (tMax * dy);
        dest.z = oz + (tMax * dz);
        return dest;
    }

    /**
     * Computes the dot product of an arbitrary vector with the direction of the
     * ray. This method avoids having to call getDirection() which would
     * instantiate a new Vector object.
     *
     * @param v vector
     * @return dot product of the ray direction and the specified vector
     */
    public final float dot(Vector3 v) {
        return dx * v.x + dy * v.y + dz * v.z;
    }

    /**
     * Computes the dot product of an arbitrary vector with the direction of the
     * ray. This method avoids having to call getDirection() which would
     * instantiate a new Vector object.
     *
     * @param vx vector x coordinate
     * @param vy vector y coordinate
     * @param vz vector z coordinate
     * @return dot product of the ray direction and the specified vector
     */
    public final float dot(float vx, float vy, float vz) {
        return dx * vx + dy * vy + dz * vz;
    }

    /**
     * Updates the maximum to the specified distance if and only if the new
     * distance is smaller than the current one.
     *
     * @param t new maximum distance
     */
    public final void setMax(float t) {
        tMax = t;
    }
}