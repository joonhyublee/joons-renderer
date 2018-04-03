package org.sunflow.math;

/**
 * This class describes a transformation matrix that changes over time. Note
 * that while unlimited motion segments are supported, it is assumed that these
 * segments represent equidistant samples within a given time range.
 */
public final class MovingMatrix4 {

    private Matrix4[] transforms;
    private float t0, t1, inv;

    /**
     * Constructs a simple static matrix.
     *
     * @param m matrix value at all times
     */
    public MovingMatrix4(Matrix4 m) {
        transforms = new Matrix4[]{m};
        t0 = t1 = 0;
        inv = 1;
    }

    private MovingMatrix4(int n, float t0, float t1, float inv) {
        transforms = new Matrix4[n];
        this.t0 = t0;
        this.t1 = t1;
        this.inv = inv;
    }

    /**
     * Redefines the number of steps in the matrix. The contents are only
     * re-allocated if the number of steps changes. This is to allow the matrix
     * to be incrementally specified.
     *
     * @param n
     */
    public void setSteps(int n) {
        if (transforms.length != n) {
            transforms = new Matrix4[n];
            if (t0 < t1) {
                inv = (transforms.length - 1) / (t1 - t0);
            } else {
                inv = 1;
            }
        }
    }

    /**
     * Updates the matrix for the given time step.
     *
     * @param i time step to update
     * @param m new value for the matrix at this time step
     */
    public void updateData(int i, Matrix4 m) {
        transforms[i] = m;
    }

    /**
     * Get the matrix for the given time step.
     *
     * @param i time step to get
     * @return matrix for the specfied time step
     */
    public Matrix4 getData(int i) {
        return transforms[i];
    }

    /**
     * Get the number of matrix segments
     *
     * @return number of segments
     */
    public int numSegments() {
        return transforms.length;
    }

    /**
     * Update the time extents over which the matrix data is changing. If the
     * interval is empty, no motion will be produced, even if multiple values
     * have been specified.
     *
     * @param t0
     * @param t1
     */
    public void updateTimes(float t0, float t1) {
        this.t0 = t0;
        this.t1 = t1;
        if (t0 < t1) {
            inv = (transforms.length - 1) / (t1 - t0);
        } else {
            inv = 1;
        }
    }

    public MovingMatrix4 inverse() {
        MovingMatrix4 mi = new MovingMatrix4(transforms.length, t0, t1, inv);
        for (int i = 0; i < transforms.length; i++) {
            if (transforms[i] != null) {
                mi.transforms[i] = transforms[i].inverse();
                if (mi.transforms[i] == null) {
                    return null; // unable to invert
                }
            }
        }
        return mi;
    }

    public Matrix4 sample(float time) {
        if (transforms.length == 1 || t0 >= t1) {
            return transforms[0];
        } else {
            float nt = (MathUtils.clamp(time, t0, t1) - t0) * inv;
            int idx0 = (int) nt;
            int idx1 = Math.min(idx0 + 1, transforms.length - 1);
            return Matrix4.blend(transforms[idx0], transforms[idx1], (float) (nt - idx0));
        }
    }
}