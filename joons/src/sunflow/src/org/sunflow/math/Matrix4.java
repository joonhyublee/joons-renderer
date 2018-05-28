package org.sunflow.math;

/**
 * This class is used to represent general affine transformations in 3D. The
 * bottom row of the matrix is assumed to be [0,0,0,1]. Note that the rotation
 * matrices assume a right-handed convention.
 */
public final class Matrix4 {
    // matrix elements, m(row,col)

    private float m00;
    private float m01;
    private float m02;
    private float m03;
    private float m10;
    private float m11;
    private float m12;
    private float m13;
    private float m20;
    private float m21;
    private float m22;
    private float m23;
    // usefull constant matrices
    public static final Matrix4 ZERO = new Matrix4();
    public static final Matrix4 IDENTITY = Matrix4.scale(1);

    /**
     * Creates an empty matrix. All elements are 0.
     */
    private Matrix4() {
    }

    /**
     * Creates a matrix with the specified elements
     *
     * @param m00 value at row 0, col 0
     * @param m01 value at row 0, col 1
     * @param m02 value at row 0, col 2
     * @param m03 value at row 0, col 3
     * @param m10 value at row 1, col 0
     * @param m11 value at row 1, col 1
     * @param m12 value at row 1, col 2
     * @param m13 value at row 1, col 3
     * @param m20 value at row 2, col 0
     * @param m21 value at row 2, col 1
     * @param m22 value at row 2, col 2
     * @param m23 value at row 2, col 3
     */
    public Matrix4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
    }

    /**
     * Initialize a matrix from the specified 16 element array. The matrix may
     * be given in row or column major form.
     *
     * @param m a 16 element array in row or column major form
     * @param rowMajor <code>true</code> if the array is in row major form,
     * <code>false</code>if it is in column major form
     */
    public Matrix4(float[] m, boolean rowMajor) {
        if (rowMajor) {
            m00 = m[0];
            m01 = m[1];
            m02 = m[2];
            m03 = m[3];
            m10 = m[4];
            m11 = m[5];
            m12 = m[6];
            m13 = m[7];
            m20 = m[8];
            m21 = m[9];
            m22 = m[10];
            m23 = m[11];
            if (m[12] != 0 || m[13] != 0 || m[14] != 0 || m[15] != 1) {
                throw new RuntimeException(String.format("Matrix is not affine! Bottom row is: [%.3f, %.3f, %.3f, %.3f]", m[12], m[13], m[14], m[15]));
            }
        } else {
            m00 = m[0];
            m01 = m[4];
            m02 = m[8];
            m03 = m[12];
            m10 = m[1];
            m11 = m[5];
            m12 = m[9];
            m13 = m[13];
            m20 = m[2];
            m21 = m[6];
            m22 = m[10];
            m23 = m[14];
            if (m[3] != 0 || m[7] != 0 || m[11] != 0 || m[15] != 1) {
                throw new RuntimeException(String.format("Matrix is not affine! Bottom row is: [%.3f, %.3f, %.3f, %.3f]", m[12], m[13], m[14], m[15]));
            }
        }
    }

    public final boolean isIndentity() {
        return equals(IDENTITY);
    }

    public final boolean equals(Matrix4 m) {
        if (m == null) {
            return false;
        }
        if (this == m) {
            return true;
        }
        return m00 == m.m00 && m01 == m.m01 && m02 == m.m02 && m03 == m.m03 && m10 == m.m10 && m11 == m.m11 && m12 == m.m12 && m13 == m.m13 && m20 == m.m20 && m21 == m.m21 && m22 == m.m22 && m23 == m.m23;
    }

    public final float[] asRowMajor() {
        return new float[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21,
            m22, m23, 0, 0, 0, 1};
    }

    public final float[] asColMajor() {
        return new float[]{m00, m10, m20, 0, m01, m11, m21, 0, m02, m12, m22,
            0, m03, m13, m23, 1};
    }

    /**
     * Compute the matrix determinant.
     *
     * @return determinant of this matrix
     */
    public final float determinant() {
        float A0 = m00 * m11 - m01 * m10;
        float A1 = m00 * m12 - m02 * m10;
        float A3 = m01 * m12 - m02 * m11;

        return A0 * m22 - A1 * m21 + A3 * m20;
    }

    /**
     * Compute the inverse of this matrix and return it as a new object. If the
     * matrix is not invertible,
     * <code>null</code> is returned.
     *
     * @return the inverse of this matrix, or <code>null</code> if not
     * invertible
     */
    public final Matrix4 inverse() {
        float A0 = m00 * m11 - m01 * m10;
        float A1 = m00 * m12 - m02 * m10;
        float A3 = m01 * m12 - m02 * m11;
        float det = A0 * m22 - A1 * m21 + A3 * m20;
        if (Math.abs(det) < 1e-12f) {
            return null; // matrix is not invertible
        }
        float invDet = 1 / det;
        float A2 = m00 * m13 - m03 * m10;
        float A4 = m01 * m13 - m03 * m11;
        float A5 = m02 * m13 - m03 * m12;
        Matrix4 inv = new Matrix4();
        inv.m00 = (+m11 * m22 - m12 * m21) * invDet;
        inv.m10 = (-m10 * m22 + m12 * m20) * invDet;
        inv.m20 = (+m10 * m21 - m11 * m20) * invDet;
        inv.m01 = (-m01 * m22 + m02 * m21) * invDet;
        inv.m11 = (+m00 * m22 - m02 * m20) * invDet;
        inv.m21 = (-m00 * m21 + m01 * m20) * invDet;
        inv.m02 = +A3 * invDet;
        inv.m12 = -A1 * invDet;
        inv.m22 = +A0 * invDet;
        inv.m03 = (-m21 * A5 + m22 * A4 - m23 * A3) * invDet;
        inv.m13 = (+m20 * A5 - m22 * A2 + m23 * A1) * invDet;
        inv.m23 = (-m20 * A4 + m21 * A2 - m23 * A0) * invDet;
        return inv;
    }

    /**
     * Computes this*m and return the result as a new Matrix4
     *
     * @param m right hand side of the multiplication
     * @return a new Matrix4 object equal to <code>this*m</code>
     */
    public final Matrix4 multiply(Matrix4 m) {
        // matrix multiplication is m[r][c] = (row[r]).(col[c])
        float rm00 = m00 * m.m00 + m01 * m.m10 + m02 * m.m20;
        float rm01 = m00 * m.m01 + m01 * m.m11 + m02 * m.m21;
        float rm02 = m00 * m.m02 + m01 * m.m12 + m02 * m.m22;
        float rm03 = m00 * m.m03 + m01 * m.m13 + m02 * m.m23 + m03;

        float rm10 = m10 * m.m00 + m11 * m.m10 + m12 * m.m20;
        float rm11 = m10 * m.m01 + m11 * m.m11 + m12 * m.m21;
        float rm12 = m10 * m.m02 + m11 * m.m12 + m12 * m.m22;
        float rm13 = m10 * m.m03 + m11 * m.m13 + m12 * m.m23 + m13;

        float rm20 = m20 * m.m00 + m21 * m.m10 + m22 * m.m20;
        float rm21 = m20 * m.m01 + m21 * m.m11 + m22 * m.m21;
        float rm22 = m20 * m.m02 + m21 * m.m12 + m22 * m.m22;
        float rm23 = m20 * m.m03 + m21 * m.m13 + m22 * m.m23 + m23;

        return new Matrix4(rm00, rm01, rm02, rm03, rm10, rm11, rm12, rm13, rm20, rm21, rm22, rm23);
    }

    /**
     * Transforms each corner of the specified axis-aligned bounding box and
     * returns a new bounding box which incloses the transformed corners.
     *
     * @param b original bounding box
     * @return a new BoundingBox object which encloses the transform version of
     * b
     */
    public final BoundingBox transform(BoundingBox b) {
        if (b.isEmpty()) {
            return new BoundingBox();
        }
        // special case extreme corners
        BoundingBox rb = new BoundingBox(transformP(b.getMinimum()));
        rb.include(transformP(b.getMaximum()));
        // do internal corners
        for (int i = 1; i < 7; i++) {
            rb.include(transformP(b.getCorner(i)));
        }
        return rb;
    }

    /**
     * Computes this*v and returns the result as a new Vector3 object. This
     * method assumes the bottom row of the matrix is
     * <code>[0,0,0,1]</code>.
     *
     * @param v vector to multiply
     * @return a new Vector3 object equal to <code>this*v</code>
     */
    public final Vector3 transformV(Vector3 v) {
        Vector3 rv = new Vector3();
        rv.x = m00 * v.x + m01 * v.y + m02 * v.z;
        rv.y = m10 * v.x + m11 * v.y + m12 * v.z;
        rv.z = m20 * v.x + m21 * v.y + m22 * v.z;
        return rv;
    }

    /**
     * Computes (this^T)*v and returns the result as a new Vector3 object. This
     * method assumes the bottom row of the matrix is
     * <code>[0,0,0,1]</code>.
     *
     * @param v vector to multiply
     * @return a new Vector3 object equal to <code>(this^T)*v</code>
     */
    public final Vector3 transformTransposeV(Vector3 v) {
        Vector3 rv = new Vector3();
        rv.x = m00 * v.x + m10 * v.y + m20 * v.z;
        rv.y = m01 * v.x + m11 * v.y + m21 * v.z;
        rv.z = m02 * v.x + m12 * v.y + m22 * v.z;
        return rv;
    }

    /**
     * Computes this*p and returns the result as a new Point3 object. This
     * method assumes the bottom row of the matrix is
     * <code>[0,0,0,1]</code>.
     *
     * @param p point to multiply
     * @return a new Point3 object equal to <code>this*v</code>
     */
    public final Point3 transformP(Point3 p) {
        Point3 rp = new Point3();
        rp.x = m00 * p.x + m01 * p.y + m02 * p.z + m03;
        rp.y = m10 * p.x + m11 * p.y + m12 * p.z + m13;
        rp.z = m20 * p.x + m21 * p.y + m22 * p.z + m23;
        return rp;
    }

    /**
     * Computes the x component of this*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return x coordinate transformation result
     */
    public final float transformVX(float x, float y, float z) {
        return m00 * x + m01 * y + m02 * z;
    }

    /**
     * Computes the y component of this*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return y coordinate transformation result
     */
    public final float transformVY(float x, float y, float z) {
        return m10 * x + m11 * y + m12 * z;
    }

    /**
     * Computes the z component of this*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return z coordinate transformation result
     */
    public final float transformVZ(float x, float y, float z) {
        return m20 * x + m21 * y + m22 * z;
    }

    /**
     * Computes the x component of (this^T)*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return x coordinate transformation result
     */
    public final float transformTransposeVX(float x, float y, float z) {
        return m00 * x + m10 * y + m20 * z;
    }

    /**
     * Computes the y component of (this^T)*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return y coordinate transformation result
     */
    public final float transformTransposeVY(float x, float y, float z) {
        return m01 * x + m11 * y + m21 * z;
    }

    /**
     * Computes the z component of (this^T)*(x,y,z,0).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return zcoordinate transformation result
     */
    public final float transformTransposeVZ(float x, float y, float z) {
        return m02 * x + m12 * y + m22 * z;
    }

    /**
     * Computes the x component of this*(x,y,z,1).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return x coordinate transformation result
     */
    public final float transformPX(float x, float y, float z) {
        return m00 * x + m01 * y + m02 * z + m03;
    }

    /**
     * Computes the y component of this*(x,y,z,1).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return y coordinate transformation result
     */
    public final float transformPY(float x, float y, float z) {
        return m10 * x + m11 * y + m12 * z + m13;
    }

    /**
     * Computes the z component of this*(x,y,z,1).
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     * @return z coordinate transformation result
     */
    public final float transformPZ(float x, float y, float z) {
        return m20 * x + m21 * y + m22 * z + m23;
    }

    /**
     * Create a translation matrix for the specified vector.
     *
     * @param x x component of translation
     * @param y y component of translation
     * @param z z component of translation
     * @return a new Matrix4 object representing the translation
     */
    public final static Matrix4 translation(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m00 = m.m11 = m.m22 = 1;
        m.m03 = x;
        m.m13 = y;
        m.m23 = z;
        return m;
    }

    /**
     * Creates a rotation matrix about the X axis.
     *
     * @param theta angle to rotate about the X axis in radians
     * @return a new Matrix4 object representing the rotation
     */
    public final static Matrix4 rotateX(float theta) {
        Matrix4 m = new Matrix4();
        float s = (float) Math.sin(theta);
        float c = (float) Math.cos(theta);
        m.m00 = 1;
        m.m11 = m.m22 = c;
        m.m12 = -s;
        m.m21 = +s;
        return m;
    }

    /**
     * Creates a rotation matrix about the Y axis.
     *
     * @param theta angle to rotate about the Y axis in radians
     * @return a new Matrix4 object representing the rotation
     */
    public final static Matrix4 rotateY(float theta) {
        Matrix4 m = new Matrix4();
        float s = (float) Math.sin(theta);
        float c = (float) Math.cos(theta);
        m.m11 = 1;
        m.m00 = m.m22 = c;
        m.m02 = +s;
        m.m20 = -s;
        return m;
    }

    /**
     * Creates a rotation matrix about the Z axis.
     *
     * @param theta angle to rotate about the Z axis in radians
     * @return a new Matrix4 object representing the rotation
     */
    public final static Matrix4 rotateZ(float theta) {
        Matrix4 m = new Matrix4();
        float s = (float) Math.sin(theta);
        float c = (float) Math.cos(theta);
        m.m22 = 1;
        m.m00 = m.m11 = c;
        m.m01 = -s;
        m.m10 = +s;
        return m;
    }

    /**
     * Creates a rotation matrix about the specified axis. The axis vector need
     * not be normalized.
     *
     * @param x x component of the axis vector
     * @param y y component of the axis vector
     * @param z z component of the axis vector
     * @param theta angle to rotate about the axis in radians
     * @return a new Matrix4 object representing the rotation
     */
    public final static Matrix4 rotate(float x, float y, float z, float theta) {
        Matrix4 m = new Matrix4();
        float invLen = 1 / (float) Math.sqrt(x * x + y * y + z * z);
        x *= invLen;
        y *= invLen;
        z *= invLen;
        float s = (float) Math.sin(theta);
        float c = (float) Math.cos(theta);
        float t = 1 - c;
        m.m00 = t * x * x + c;
        m.m11 = t * y * y + c;
        m.m22 = t * z * z + c;
        float txy = t * x * y;
        float sz = s * z;
        m.m01 = txy - sz;
        m.m10 = txy + sz;
        float txz = t * x * z;
        float sy = s * y;
        m.m02 = txz + sy;
        m.m20 = txz - sy;
        float tyz = t * y * z;
        float sx = s * x;
        m.m12 = tyz - sx;
        m.m21 = tyz + sx;
        return m;
    }

    /**
     * Create a uniform scaling matrix.
     *
     * @param s scale factor for all three axes
     * @return a new Matrix4 object representing the uniform scale
     */
    public final static Matrix4 scale(float s) {
        Matrix4 m = new Matrix4();
        m.m00 = m.m11 = m.m22 = s;
        return m;
    }

    /**
     * Creates a non-uniform scaling matrix.
     *
     * @param sx scale factor in the x dimension
     * @param sy scale factor in the y dimension
     * @param sz scale factor in the z dimension
     * @return a new Matrix4 object representing the non-uniform scale
     */
    public final static Matrix4 scale(float sx, float sy, float sz) {
        Matrix4 m = new Matrix4();
        m.m00 = sx;
        m.m11 = sy;
        m.m22 = sz;
        return m;
    }

    /**
     * Creates a rotation matrix from an OrthonormalBasis.
     *
     * @param basis
     */
    public final static Matrix4 fromBasis(OrthoNormalBasis basis) {
        Matrix4 m = new Matrix4();
        Vector3 u = basis.transform(new Vector3(1, 0, 0));
        Vector3 v = basis.transform(new Vector3(0, 1, 0));
        Vector3 w = basis.transform(new Vector3(0, 0, 1));
        m.m00 = u.x;
        m.m01 = v.x;
        m.m02 = w.x;
        m.m10 = u.y;
        m.m11 = v.y;
        m.m12 = w.y;
        m.m20 = u.z;
        m.m21 = v.z;
        m.m22 = w.z;
        return m;
    }

    /**
     * Creates a camera positioning matrix from the given eye and target points
     * and up vector.
     *
     * @param eye location of the eye
     * @param target location of the target
     * @param up vector pointing upwards
     * @return
     */
    public final static Matrix4 lookAt(Point3 eye, Point3 target, Vector3 up) {
        Matrix4 m = Matrix4.fromBasis(OrthoNormalBasis.makeFromWV(Point3.sub(eye, target, new Vector3()), up));
        return Matrix4.translation(eye.x, eye.y, eye.z).multiply(m);
    }

    public final static Matrix4 blend(Matrix4 m0, Matrix4 m1, float t) {
        Matrix4 m = new Matrix4();
        m.m00 = (1 - t) * m0.m00 + t * m1.m00;
        m.m01 = (1 - t) * m0.m01 + t * m1.m01;
        m.m02 = (1 - t) * m0.m02 + t * m1.m02;
        m.m03 = (1 - t) * m0.m03 + t * m1.m03;

        m.m10 = (1 - t) * m0.m10 + t * m1.m10;
        m.m11 = (1 - t) * m0.m11 + t * m1.m11;
        m.m12 = (1 - t) * m0.m12 + t * m1.m12;
        m.m13 = (1 - t) * m0.m13 + t * m1.m13;

        m.m20 = (1 - t) * m0.m20 + t * m1.m20;
        m.m21 = (1 - t) * m0.m21 + t * m1.m21;
        m.m22 = (1 - t) * m0.m22 + t * m1.m22;
        m.m23 = (1 - t) * m0.m23 + t * m1.m23;
        return m;
    }
}