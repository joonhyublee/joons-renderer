package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class SphereFlake implements PrimitiveList {

    private static final int MAX_LEVEL = 20;
    private static final float[] boundingRadiusOffset = new float[MAX_LEVEL + 1];
    private static final float[] recursivePattern = new float[9 * 3];
    private int level = 2;
    private Vector3 axis = new Vector3(0, 0, 1);
    private float baseRadius = 1;

    static {
        // geometric series table, to compute bounding radius quickly
        for (int i = 0, r = 3; i < boundingRadiusOffset.length; i++, r *= 3) {
            boundingRadiusOffset[i] = (r - 3.0f) / r;
        }
        // lower ring
        double a = 0, daL = 2 * Math.PI / 6, daU = 2 * Math.PI / 3;
        for (int i = 0; i < 6; i++) {
            recursivePattern[3 * i + 0] = -0.3f;
            recursivePattern[3 * i + 1] = (float) Math.sin(a);
            recursivePattern[3 * i + 2] = (float) Math.cos(a);
            a += daL;
        }
        a -= daL / 2; // tweak
        for (int i = 6; i < 9; i++) {
            recursivePattern[3 * i + 0] = +0.7f;
            recursivePattern[3 * i + 1] = (float) Math.sin(a);
            recursivePattern[3 * i + 2] = (float) Math.cos(a);
            a += daU;
        }
        for (int i = 0; i < recursivePattern.length; i += 3) {
            float x = recursivePattern[i + 0];
            float y = recursivePattern[i + 1];
            float z = recursivePattern[i + 2];
            float n = 1 / (float) Math.sqrt(x * x + y * y + z * z);
            recursivePattern[i + 0] = x * n;
            recursivePattern[i + 1] = y * n;
            recursivePattern[i + 2] = z * n;
        }
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        level = MathUtils.clamp(pl.getInt("level", level), 0, 20);
        axis = pl.getVector("axis", axis);
        axis.normalize();
        baseRadius = Math.abs(pl.getFloat("radius", baseRadius));
        return true;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(getPrimitiveBound(0, 1));
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    public float getPrimitiveBound(int primID, int i) {
        float br = 1 + boundingRadiusOffset[level];
        return (i & 1) == 0 ? -br : br;
    }

    public int getNumPrimitives() {
        return 1;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        Point3 localPoint = state.transformWorldToObject(state.getPoint());

        float cx = state.getU();
        float cy = state.getV();
        float cz = state.getW();

        state.getNormal().set(localPoint.x - cx, localPoint.y - cy, localPoint.z - cz);
        state.getNormal().normalize();

        float phi = (float) Math.atan2(state.getNormal().y, state.getNormal().x);
        if (phi < 0) {
            phi += 2 * Math.PI;
        }
        float theta = (float) Math.acos(state.getNormal().z);
        state.getUV().y = theta / (float) Math.PI;
        state.getUV().x = phi / (float) (2 * Math.PI);
        Vector3 v = new Vector3();
        v.x = -2 * (float) Math.PI * state.getNormal().y;
        v.y = 2 * (float) Math.PI * state.getNormal().x;
        v.z = 0;
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Vector3 worldNormal = state.transformNormalObjectToWorld(state.getNormal());
        v = state.transformVectorObjectToWorld(v);
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        // compute basis in world space
        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));

    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
        intersectFlake(r, state, level, qa, 1 / qa, 0, 0, 0, axis.x, axis.y, axis.z, baseRadius);
    }

    private void intersectFlake(Ray r, IntersectionState state, int level, float qa, float qaInv, float cx, float cy, float cz, float dx, float dy, float dz, float radius) {
        if (level <= 0) {
            // we reached the bottom - intersect sphere and bail out
            float vcx = cx - r.ox;
            float vcy = cy - r.oy;
            float vcz = cz - r.oz;
            float b = r.dx * vcx + r.dy * vcy + r.dz * vcz;
            float disc = b * b - qa * ((vcx * vcx + vcy * vcy + vcz * vcz) - radius * radius);
            if (disc > 0) {
                // intersects - check t values
                float d = (float) Math.sqrt(disc);
                float t1 = (b - d) * qaInv;
                float t2 = (b + d) * qaInv;
                if (t1 >= r.getMax() || t2 <= r.getMin()) {
                    return;
                }
                if (t1 > r.getMin()) {
                    r.setMax(t1);
                } else {
                    r.setMax(t2);
                }
                state.setIntersection(0, cx, cy, cz);
            }
        } else {
            float boundRadius = radius * (1 + boundingRadiusOffset[level]);
            float vcx = cx - r.ox;
            float vcy = cy - r.oy;
            float vcz = cz - r.oz;
            float b = r.dx * vcx + r.dy * vcy + r.dz * vcz;
            float vcd = (vcx * vcx + vcy * vcy + vcz * vcz);
            float disc = b * b - qa * (vcd - boundRadius * boundRadius);
            if (disc > 0) {
                // intersects - check t values
                float d = (float) Math.sqrt(disc);
                float t1 = (b - d) * qaInv;
                float t2 = (b + d) * qaInv;
                if (t1 >= r.getMax() || t2 <= r.getMin()) {
                    return;
                }

                // we hit the bounds, now compute intersection with the actual
                // leaf sphere
                disc = b * b - qa * (vcd - radius * radius);
                if (disc > 0) {
                    d = (float) Math.sqrt(disc);
                    t1 = (b - d) * qaInv;
                    t2 = (b + d) * qaInv;
                    if (t1 >= r.getMax() || t2 <= r.getMin()) {
                        // no hit
                    } else {
                        if (t1 > r.getMin()) {
                            r.setMax(t1);
                        } else {
                            r.setMax(t2);
                        }
                        state.setIntersection(0, cx, cy, cz);
                    }
                }

                // recursively intersect 9 other spheres
                // step1: compute basis around displacement vector
                float b1x, b1y, b1z;
                if (dx * dx < dy * dy && dx * dx < dz * dz) {
                    b1x = 0;
                    b1y = dz;
                    b1z = -dy;
                } else if (dy * dy < dz * dz) {
                    b1x = dz;
                    b1y = 0;
                    b1z = -dx;
                } else {
                    b1x = dy;
                    b1y = -dx;
                    b1z = 0;
                }
                float n = 1 / (float) Math.sqrt(b1x * b1x + b1y * b1y + b1z * b1z);
                b1x *= n;
                b1y *= n;
                b1z *= n;
                float b2x = dy * b1z - dz * b1y;
                float b2y = dz * b1x - dx * b1z;
                float b2z = dx * b1y - dy * b1x;
                b1x = dy * b2z - dz * b2y;
                b1y = dz * b2x - dx * b2z;
                b1z = dx * b2y - dy * b2x;
                // step2: generate 9 children recursively
                float nr = radius * (1 / 3.0f), scale = radius + nr;
                for (int i = 0; i < 9 * 3; i += 3) {
                    // transform by basis
                    float ndx = recursivePattern[i] * dx + recursivePattern[i + 1] * b1x + recursivePattern[i + 2] * b2x;
                    float ndy = recursivePattern[i] * dy + recursivePattern[i + 1] * b1y + recursivePattern[i + 2] * b2y;
                    float ndz = recursivePattern[i] * dz + recursivePattern[i + 1] * b1z + recursivePattern[i + 2] * b2z;
                    // recurse!
                    intersectFlake(r, state, level - 1, qa, qaInv, cx + scale * ndx, cy + scale * ndy, cz + scale * ndz, ndx, ndy, ndz, nr);
                }
            }
        }
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}