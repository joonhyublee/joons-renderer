package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;

public class JuliaFractal implements PrimitiveList {

    private static float BOUNDING_RADIUS = (float) Math.sqrt(3);
    private static float BOUNDING_RADIUS2 = 3;
    private static float ESCAPE_THRESHOLD = 1e1f;
    private static float DELTA = 1e-4f;
    // quaternion constant
    private float cx;
    private float cy;
    private float cz;
    private float cw;
    private int maxIterations;
    private float epsilon;

    public JuliaFractal() {
        // good defaults?
        cw = -.4f;
        cx = .2f;
        cy = .3f;
        cz = -.2f;

        maxIterations = 15;
        epsilon = 0.00001f;
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return ((i & 1) == 0) ? -BOUNDING_RADIUS : BOUNDING_RADIUS;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(BOUNDING_RADIUS);
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect with bounding sphere
        float qc = ((r.ox * r.ox) + (r.oy * r.oy) + (r.oz * r.oz)) - BOUNDING_RADIUS2;
        float qt = r.getMin();
        if (qc > 0) {
            // we are starting outside the sphere, find intersection on the
            // sphere
            float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
            float qb = 2 * ((r.dx * r.ox) + (r.dy * r.oy) + (r.dz * r.oz));
            double[] t = Solvers.solveQuadric(qa, qb, qc);
            // early rejection
            if (t == null || t[0] >= r.getMax() || t[1] <= r.getMin()) {
                return;
            }
            qt = (float) t[0];
        }
        float dist = Float.POSITIVE_INFINITY;
        float rox = r.ox + qt * r.dx;
        float roy = r.oy + qt * r.dy;
        float roz = r.oz + qt * r.dz;
        float invRayLength = (float) (1 / Math.sqrt(r.dx * r.dx + r.dy * r.dy + r.dz * r.dz));
        // now we can start intersection
        while (true) {
            float zw = rox;
            float zx = roy;
            float zy = roz;
            float zz = 0;

            float zpw = 1;
            float zpx = 0;
            float zpy = 0;
            float zpz = 0;

            // run several iterations
            float dotz = 0;
            for (int i = 0; i < maxIterations; i++) {
                {
                    // zp = 2 * (z * zp)
                    float nw = zw * zpw - zx * zpx - zy * zpy - zz * zpz;
                    float nx = zw * zpx + zx * zpw + zy * zpz - zz * zpy;
                    float ny = zw * zpy + zy * zpw + zz * zpx - zx * zpz;
                    zpz = 2 * (zw * zpz + zz * zpw + zx * zpy - zy * zpx);
                    zpw = 2 * nw;
                    zpx = 2 * nx;
                    zpy = 2 * ny;
                }
                {
                    // z = z*z + c
                    float nw = zw * zw - zx * zx - zy * zy - zz * zz + cw;
                    zx = 2 * zw * zx + cx;
                    zy = 2 * zw * zy + cy;
                    zz = 2 * zw * zz + cz;
                    zw = nw;
                }
                dotz = zw * zw + zx * zx + zy * zy + zz * zz;
                if (dotz > ESCAPE_THRESHOLD) {
                    break;
                }

            }
            float normZ = (float) Math.sqrt(dotz);
            dist = 0.5f * normZ * (float) Math.log(normZ) / length(zpw, zpx, zpy, zpz);
            rox += dist * r.dx;
            roy += dist * r.dy;
            roz += dist * r.dz;
            qt += dist;
            if (dist * invRayLength < epsilon) {
                break;
            }
            if (rox * rox + roy * roy + roz * roz > BOUNDING_RADIUS2) {
                return;
            }
        }
        // now test t value again
        if (!r.isInside(qt)) {
            return;
        }
        if (dist * invRayLength < epsilon) {
            // valid hit
            r.setMax(qt);
            state.setIntersection(0);
        }
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        // compute local normal
        Point3 p = state.transformWorldToObject(state.getPoint());
        float gx1w = p.x - DELTA;
        float gx1x = p.y;
        float gx1y = p.z;
        float gx1z = 0;
        float gx2w = p.x + DELTA;
        float gx2x = p.y;
        float gx2y = p.z;
        float gx2z = 0;

        float gy1w = p.x;
        float gy1x = p.y - DELTA;
        float gy1y = p.z;
        float gy1z = 0;
        float gy2w = p.x;
        float gy2x = p.y + DELTA;
        float gy2y = p.z;
        float gy2z = 0;

        float gz1w = p.x;
        float gz1x = p.y;
        float gz1y = p.z - DELTA;
        float gz1z = 0;
        float gz2w = p.x;
        float gz2x = p.y;
        float gz2y = p.z + DELTA;
        float gz2z = 0;

        for (int i = 0; i < maxIterations; i++) {
            {
                // z = z*z + c
                float nw = gx1w * gx1w - gx1x * gx1x - gx1y * gx1y - gx1z * gx1z + cw;
                gx1x = 2 * gx1w * gx1x + cx;
                gx1y = 2 * gx1w * gx1y + cy;
                gx1z = 2 * gx1w * gx1z + cz;
                gx1w = nw;
            }
            {
                // z = z*z + c
                float nw = gx2w * gx2w - gx2x * gx2x - gx2y * gx2y - gx2z * gx2z + cw;
                gx2x = 2 * gx2w * gx2x + cx;
                gx2y = 2 * gx2w * gx2y + cy;
                gx2z = 2 * gx2w * gx2z + cz;
                gx2w = nw;
            }
            {
                // z = z*z + c
                float nw = gy1w * gy1w - gy1x * gy1x - gy1y * gy1y - gy1z * gy1z + cw;
                gy1x = 2 * gy1w * gy1x + cx;
                gy1y = 2 * gy1w * gy1y + cy;
                gy1z = 2 * gy1w * gy1z + cz;
                gy1w = nw;
            }
            {
                // z = z*z + c
                float nw = gy2w * gy2w - gy2x * gy2x - gy2y * gy2y - gy2z * gy2z + cw;
                gy2x = 2 * gy2w * gy2x + cx;
                gy2y = 2 * gy2w * gy2y + cy;
                gy2z = 2 * gy2w * gy2z + cz;
                gy2w = nw;
            }
            {
                // z = z*z + c
                float nw = gz1w * gz1w - gz1x * gz1x - gz1y * gz1y - gz1z * gz1z + cw;
                gz1x = 2 * gz1w * gz1x + cx;
                gz1y = 2 * gz1w * gz1y + cy;
                gz1z = 2 * gz1w * gz1z + cz;
                gz1w = nw;
            }
            {
                // z = z*z + c
                float nw = gz2w * gz2w - gz2x * gz2x - gz2y * gz2y - gz2z * gz2z + cw;
                gz2x = 2 * gz2w * gz2x + cx;
                gz2y = 2 * gz2w * gz2y + cy;
                gz2z = 2 * gz2w * gz2z + cz;
                gz2w = nw;
            }
        }
        float gradX = length(gx2w, gx2x, gx2y, gx2z) - length(gx1w, gx1x, gx1y, gx1z);
        float gradY = length(gy2w, gy2x, gy2y, gy2z) - length(gy1w, gy1x, gy1y, gy1z);
        float gradZ = length(gz2w, gz2x, gz2y, gz2z) - length(gz1w, gz1x, gz1y, gz1z);
        Vector3 n = new Vector3(gradX, gradY, gradZ);
        state.getNormal().set(state.transformNormalObjectToWorld(n));
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));

        state.getPoint().x += state.getNormal().x * epsilon * 20;
        state.getPoint().y += state.getNormal().y * epsilon * 20;
        state.getPoint().z += state.getNormal().z * epsilon * 20;

        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
    }

    private static float length(float w, float x, float y, float z) {
        return (float) Math.sqrt(w * w + x * x + y * y + z * z);
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        maxIterations = pl.getInt("iterations", maxIterations);
        epsilon = pl.getFloat("epsilon", epsilon);
        cw = pl.getFloat("cw", cw);
        cx = pl.getFloat("cx", cx);
        cy = pl.getFloat("cy", cy);
        cz = pl.getFloat("cz", cz);
        return true;
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}