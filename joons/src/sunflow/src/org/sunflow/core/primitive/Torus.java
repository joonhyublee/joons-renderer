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
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;

public class Torus implements PrimitiveList {

    private float ri2, ro2;
    private float ri, ro;

    public Torus() {
        ri = 0.25f;
        ro = 1;
        ri2 = ri * ri;
        ro2 = ro * ro;

    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        ri = pl.getFloat("radiusInner", ri);
        ro = pl.getFloat("radiusOuter", ro);
        ri2 = ri * ri;
        ro2 = ro * ro;
        return true;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(-ro - ri, -ro - ri, -ri);
        bounds.include(ro + ri, ro + ri, ri);
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    public float getPrimitiveBound(int primID, int i) {
        switch (i) {
            case 0:
            case 2:
                return -ro - ri;
            case 1:
            case 3:
                return ro + ri;
            case 4:
                return -ri;
            case 5:
                return ri;
            default:
                return 0;
        }
    }

    public int getNumPrimitives() {
        return 1;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        // get local point
        Point3 p = state.transformWorldToObject(state.getPoint());
        // compute local normal
        float deriv = p.x * p.x + p.y * p.y + p.z * p.z - ri2 - ro2;
        state.getNormal().set(p.x * deriv, p.y * deriv, p.z * deriv + 2 * ro2 * p.z);
        state.getNormal().normalize();

        double phi = Math.asin(MathUtils.clamp(p.z / ri, -1, 1));
        double theta = Math.atan2(p.y, p.x);
        if (theta < 0) {
            theta += 2 * Math.PI;
        }
        state.getUV().x = (float) (theta / (2 * Math.PI));
        state.getUV().y = (float) ((phi + Math.PI / 2) / Math.PI);
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Vector3 worldNormal = state.transformNormalObjectToWorld(state.getNormal());
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        // make basis in world space
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));

    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float rd2x = r.dx * r.dx;
        float rd2y = r.dy * r.dy;
        float rd2z = r.dz * r.dz;
        float ro2x = r.ox * r.ox;
        float ro2y = r.oy * r.oy;
        float ro2z = r.oz * r.oz;
        // compute some common factors
        double alpha = rd2x + rd2y + rd2z;
        double beta = 2 * (r.ox * r.dx + r.oy * r.dy + r.oz * r.dz);
        double gamma = (ro2x + ro2y + ro2z) - ri2 - ro2;
        // setup quartic coefficients
        double A = alpha * alpha;
        double B = 2 * alpha * beta;
        double C = beta * beta + 2 * alpha * gamma + 4 * ro2 * rd2z;
        double D = 2 * beta * gamma + 8 * ro2 * r.oz * r.dz;
        double E = gamma * gamma + 4 * ro2 * ro2z - 4 * ro2 * ri2;
        // solve equation
        double[] t = Solvers.solveQuartic(A, B, C, D, E);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[t.length - 1] <= r.getMin()) {
                return;
            }
            // find first intersection in front of the ray
            for (int i = 0; i < t.length; i++) {
                if (t[i] > r.getMin()) {
                    r.setMax((float) t[i]);
                    state.setIntersection(0);
                    return;
                }
            }
        }
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}