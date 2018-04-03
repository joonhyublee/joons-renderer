package org.sunflow.math;

/**
 * 3D axis-aligned bounding box. Stores only the minimum and maximum corner
 * points.
 */
public class BoundingBox {

    private Point3 minimum;
    private Point3 maximum;

    /**
     * Creates an empty box. The minimum point will have all components set to
     * positive infinity, and the maximum will have all components set to
     * negative infinity.
     */
    public BoundingBox() {
        minimum = new Point3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        maximum = new Point3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    /**
     * Creates a copy of the given box.
     *
     * @param b bounding box to copy
     */
    public BoundingBox(BoundingBox b) {
        minimum = new Point3(b.minimum);
        maximum = new Point3(b.maximum);
    }

    /**
     * Creates a bounding box containing only the specified point.
     *
     * @param p point to include
     */
    public BoundingBox(Point3 p) {
        this(p.x, p.y, p.z);
    }

    /**
     * Creates a bounding box containing only the specified point.
     *
     * @param x x coordinate of the point to include
     * @param y y coordinate of the point to include
     * @param z z coordinate of the point to include
     */
    public BoundingBox(float x, float y, float z) {
        minimum = new Point3(x, y, z);
        maximum = new Point3(x, y, z);
    }

    /**
     * Creates a bounding box centered around the origin.
     *
     * @param size half edge length of the bounding box
     */
    public BoundingBox(float size) {
        minimum = new Point3(-size, -size, -size);
        maximum = new Point3(size, size, size);
    }

    /**
     * Gets the minimum corner of the box. That is the corner of smallest
     * coordinates on each axis. Note that the returned reference is not cloned
     * for efficiency purposes so care must be taken not to change the
     * coordinates of the point.
     *
     * @return a reference to the minimum corner
     */
    public final Point3 getMinimum() {
        return minimum;
    }

    /**
     * Gets the maximum corner of the box. That is the corner of largest
     * coordinates on each axis. Note that the returned reference is not cloned
     * for efficiency purposes so care must be taken not to change the
     * coordinates of the point.
     *
     * @return a reference to the maximum corner
     */
    public final Point3 getMaximum() {
        return maximum;
    }

    /**
     * Gets the center of the box, computed as (min + max) / 2.
     *
     * @return a reference to the center of the box
     */
    public final Point3 getCenter() {
        return Point3.mid(minimum, maximum, new Point3());
    }

    /**
     * Gets a corner of the bounding box. The index scheme uses the binary
     * representation of the index to decide which corner to return. Corner 0 is
     * equivalent to the minimum and corner 7 is equivalent to the maximum.
     *
     * @param i a corner index, from 0 to 7
     * @return the corresponding corner
     */
    public final Point3 getCorner(int i) {
        float x = (i & 1) == 0 ? minimum.x : maximum.x;
        float y = (i & 2) == 0 ? minimum.y : maximum.y;
        float z = (i & 4) == 0 ? minimum.z : maximum.z;
        return new Point3(x, y, z);
    }

    /**
     * Gets a specific coordinate of the surface's bounding box.
     *
     * @param i index of a side from 0 to 5
     * @return value of the request bounding box side
     */
    public final float getBound(int i) {
        switch (i) {
            case 0:
                return minimum.x;
            case 1:
                return maximum.x;
            case 2:
                return minimum.y;
            case 3:
                return maximum.y;
            case 4:
                return minimum.z;
            case 5:
                return maximum.z;
            default:
                return 0;
        }
    }

    /**
     * Gets the extents vector for the box. This vector is computed as (max -
     * min). Its coordinates are always positive and represent the dimensions of
     * the box along the three axes.
     *
     * @return a refreence to the extent vector
     * @see org.sunflow.math.Vector3#length()
     */
    public final Vector3 getExtents() {
        return Point3.sub(maximum, minimum, new Vector3());
    }

    /**
     * Gets the surface area of the box.
     *
     * @return surface area
     */
    public final float getArea() {
        Vector3 w = getExtents();
        float ax = Math.max(w.x, 0);
        float ay = Math.max(w.y, 0);
        float az = Math.max(w.z, 0);
        return 2 * (ax * ay + ay * az + az * ax);
    }

    /**
     * Gets the box's volume
     *
     * @return volume
     */
    public final float getVolume() {
        Vector3 w = getExtents();
        float ax = Math.max(w.x, 0);
        float ay = Math.max(w.y, 0);
        float az = Math.max(w.z, 0);
        return ax * ay * az;
    }

    /**
     * Enlarge the bounding box by the minimum possible amount to avoid numeric
     * precision related problems.
     */
    public final void enlargeUlps() {
        final float eps = 0.0001f;
        minimum.x -= Math.max(eps, Math.ulp(minimum.x));
        minimum.y -= Math.max(eps, Math.ulp(minimum.y));
        minimum.z -= Math.max(eps, Math.ulp(minimum.z));
        maximum.x += Math.max(eps, Math.ulp(maximum.x));
        maximum.y += Math.max(eps, Math.ulp(maximum.y));
        maximum.z += Math.max(eps, Math.ulp(maximum.z));
    }

    /**
     * Returns
     * <code>true</code> when the box has just been initialized, and is still
     * empty. This method might also return true if the state of the box becomes
     * inconsistent and some component of the minimum corner is larger than the
     * corresponding coordinate of the maximum corner.
     *
     * @return <code>true</code> if the box is empty, <code>false</code>
     * otherwise
     */
    public final boolean isEmpty() {
        return (maximum.x < minimum.x) || (maximum.y < minimum.y) || (maximum.z < minimum.z);
    }

    /**
     * Returns
     * <code>true</code> if the specified bounding box intersects this one. The
     * boxes are treated as volumes, so a box inside another will return true.
     * Returns
     * <code>false</code> if the parameter is
     * <code>null</code>.
     *
     * @param b box to be tested for intersection
     * @return <code>true</code> if the boxes overlap, <code>false</code>
     * otherwise
     */
    public final boolean intersects(BoundingBox b) {
        return ((b != null) && (minimum.x <= b.maximum.x) && (maximum.x >= b.minimum.x) && (minimum.y <= b.maximum.y) && (maximum.y >= b.minimum.y) && (minimum.z <= b.maximum.z) && (maximum.z >= b.minimum.z));
    }

    /**
     * Checks to see if the specified {@link org.sunflow.math.Point3 point}is
     * inside the volume defined by this box. Returns
     * <code>false</code> if the parameter is
     * <code>null</code>.
     *
     * @param p point to be tested for containment
     * @return <code>true</code> if the point is inside the box,
     * <code>false</code> otherwise
     */
    public final boolean contains(Point3 p) {
        return ((p != null) && (p.x >= minimum.x) && (p.x <= maximum.x) && (p.y >= minimum.y) && (p.y <= maximum.y) && (p.z >= minimum.z) && (p.z <= maximum.z));
    }

    /**
     * Check to see if the specified point is inside the volume defined by this
     * box.
     *
     * @param x x coordinate of the point to be tested
     * @param y y coordinate of the point to be tested
     * @param z z coordinate of the point to be tested
     * @return <code>true</code> if the point is inside the box,
     * <code>false</code> otherwise
     */
    public final boolean contains(float x, float y, float z) {
        return ((x >= minimum.x) && (x <= maximum.x) && (y >= minimum.y) && (y <= maximum.y) && (z >= minimum.z) && (z <= maximum.z));
    }

    /**
     * Changes the extents of the box as needed to include the given
     * {@link org.sunflow.math.Point3 point}into this box. Does nothing if the
     * parameter is
     * <code>null</code>.
     *
     * @param p point to be included
     */
    public final void include(Point3 p) {
        if (p != null) {
            if (p.x < minimum.x) {
                minimum.x = p.x;
            }
            if (p.x > maximum.x) {
                maximum.x = p.x;
            }
            if (p.y < minimum.y) {
                minimum.y = p.y;
            }
            if (p.y > maximum.y) {
                maximum.y = p.y;
            }
            if (p.z < minimum.z) {
                minimum.z = p.z;
            }
            if (p.z > maximum.z) {
                maximum.z = p.z;
            }
        }
    }

    /**
     * Changes the extents of the box as needed to include the given point into
     * this box.
     *
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     * @param z z coordinate of the point
     */
    public final void include(float x, float y, float z) {
        if (x < minimum.x) {
            minimum.x = x;
        }
        if (x > maximum.x) {
            maximum.x = x;
        }
        if (y < minimum.y) {
            minimum.y = y;
        }
        if (y > maximum.y) {
            maximum.y = y;
        }
        if (z < minimum.z) {
            minimum.z = z;
        }
        if (z > maximum.z) {
            maximum.z = z;
        }
    }

    /**
     * Changes the extents of the box as needed to include the given box into
     * this box. Does nothing if the parameter is
     * <code>null</code>.
     *
     * @param b box to be included
     */
    public final void include(BoundingBox b) {
        if (b != null) {
            if (b.minimum.x < minimum.x) {
                minimum.x = b.minimum.x;
            }
            if (b.maximum.x > maximum.x) {
                maximum.x = b.maximum.x;
            }
            if (b.minimum.y < minimum.y) {
                minimum.y = b.minimum.y;
            }
            if (b.maximum.y > maximum.y) {
                maximum.y = b.maximum.y;
            }
            if (b.minimum.z < minimum.z) {
                minimum.z = b.minimum.z;
            }
            if (b.maximum.z > maximum.z) {
                maximum.z = b.maximum.z;
            }
        }
    }

    @Override
    public final String toString() {
        return String.format("(%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)", minimum.x, minimum.y, minimum.z, maximum.x, maximum.y, maximum.z);
    }
}