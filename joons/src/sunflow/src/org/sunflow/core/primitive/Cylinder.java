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

public class Cylinder implements PrimitiveList {

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
        state.getNormal().set(localPoint.x, localPoint.y, 0);
        state.getNormal().normalize();

        float phi = (float) Math.atan2(state.getNormal().y, state.getNormal().x);
        if (phi < 0) {
            phi += 2 * Math.PI;
        }
        state.getUV().x = phi / (float) (2 * Math.PI);
        state.getUV().y = (localPoint.z + 1) * 0.5f;
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Vector3 worldNormal = state.transformNormalObjectToWorld(state.getNormal());
        Vector3 v = state.transformVectorObjectToWorld(new Vector3(0, 0, 1));
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        // compute basis in world space
        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float qa = r.dx * r.dx + r.dy * r.dy;
        float qb = 2 * ((r.dx * r.ox) + (r.dy * r.oy));
        float qc = ((r.ox * r.ox) + (r.oy * r.oy)) - 1;
        double[] t = Solvers.solveQuadric(qa, qb, qc);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[1] <= r.getMin()) {
                return;
            }
            if (t[0] > r.getMin()) {
                float z = r.oz + (float) t[0] * r.dz;
                if (z >= -1 && z <= 1) {
                    r.setMax((float) t[0]);
                    state.setIntersection(0);
                    return;
                }
            }
            if (t[1] < r.getMax()) {
                float z = r.oz + (float) t[1] * r.dz;
                if (z >= -1 && z <= 1) {
                    r.setMax((float) t[1]);
                    state.setIntersection(0);
                }
            }
        }
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}