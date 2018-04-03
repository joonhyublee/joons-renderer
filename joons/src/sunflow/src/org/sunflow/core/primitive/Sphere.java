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

public class Sphere implements PrimitiveList {

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(1);
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return (i & 1) == 0 ? -1 : 1;
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
        Point3 localPoint = state.transformWorldToObject(state.getPoint());
        state.getNormal().set(localPoint.x, localPoint.y, localPoint.z);
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

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
        float qb = 2 * ((r.dx * r.ox) + (r.dy * r.oy) + (r.dz * r.oz));
        float qc = ((r.ox * r.ox) + (r.oy * r.oy) + (r.oz * r.oz)) - 1;
        double[] t = Solvers.solveQuadric(qa, qb, qc);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[1] <= r.getMin()) {
                return;
            }
            if (t[0] > r.getMin()) {
                r.setMax((float) t[0]);
            } else {
                r.setMax((float) t[1]);
            }
            state.setIntersection(0);
        }
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}