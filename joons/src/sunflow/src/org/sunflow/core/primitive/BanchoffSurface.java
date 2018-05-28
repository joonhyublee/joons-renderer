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

public class BanchoffSurface implements PrimitiveList {

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(1.5f);
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return (i & 1) == 0 ? -1.5f : 1.5f;
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        Point3 n = state.transformWorldToObject(state.getPoint());
        state.getNormal().set(n.x * (2 * n.x * n.x - 1), n.y * (2 * n.y * n.y - 1), n.z * (2 * n.z * n.z - 1));
        state.getNormal().normalize();
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Vector3 worldNormal = state.transformNormalObjectToWorld(state.getNormal());
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        // create basis in world space
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float rd2x = r.dx * r.dx;
        float rd2y = r.dy * r.dy;
        float rd2z = r.dz * r.dz;
        float ro2x = r.ox * r.ox;
        float ro2y = r.oy * r.oy;
        float ro2z = r.oz * r.oz;
        // setup the quartic coefficients
        // some common terms could probably be shared across these
        double A = (rd2y * rd2y + rd2z * rd2z + rd2x * rd2x);
        double B = 4 * (r.oy * rd2y * r.dy + r.oz * r.dz * rd2z + r.ox * r.dx * rd2x);
        double C = (-rd2x - rd2y - rd2z + 6 * (ro2y * rd2y + ro2z * rd2z + ro2x * rd2x));
        double D = 2 * (2 * ro2z * r.oz * r.dz - r.oz * r.dz + 2 * ro2x * r.ox * r.dx + 2 * ro2y * r.oy * r.dy - r.ox * r.dx - r.oy * r.dy);
        double E = 3.0f / 8.0f + (-ro2z + ro2z * ro2z - ro2y + ro2y * ro2y - ro2x + ro2x * ro2x);
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

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}