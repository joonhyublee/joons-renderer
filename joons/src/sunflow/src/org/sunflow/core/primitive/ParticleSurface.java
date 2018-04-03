package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.ParameterList.FloatParameter;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;

public class ParticleSurface implements PrimitiveList {

    private float[] particles;
    private float r, r2;
    private int n;

    public ParticleSurface() {
        particles = null;
        r = r2 = 1;
        n = 0;
    }

    @Override
    public int getNumPrimitives() {
        return n;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        float c = particles[primID * 3 + (i >>> 1)];
        return (i & 1) == 0 ? c - r : c + r;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        for (int i = 0, i3 = 0; i < n; i++, i3 += 3) {
            bounds.include(particles[i3], particles[i3 + 1], particles[i3 + 2]);
        }
        bounds.include(bounds.getMinimum().x - r, bounds.getMinimum().y - r, bounds.getMinimum().z - r);
        bounds.include(bounds.getMaximum().x + r, bounds.getMaximum().y + r, bounds.getMaximum().z + r);
        return o2w == null ? bounds : o2w.transform(bounds);
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        int i3 = primID * 3;
        float ocx = r.ox - particles[i3 + 0];
        float ocy = r.oy - particles[i3 + 1];
        float ocz = r.oz - particles[i3 + 2];
        float qa = r.dx * r.dx + r.dy * r.dy + r.dz * r.dz;
        float qb = 2 * ((r.dx * ocx) + (r.dy * ocy) + (r.dz * ocz));
        float qc = ((ocx * ocx) + (ocy * ocy) + (ocz * ocz)) - r2;
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
            state.setIntersection(primID);
        }
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Point3 localPoint = state.transformWorldToObject(state.getPoint());

        localPoint.x -= particles[3 * state.getPrimitiveID() + 0];
        localPoint.y -= particles[3 * state.getPrimitiveID() + 1];
        localPoint.z -= particles[3 * state.getPrimitiveID() + 2];

        state.getNormal().set(localPoint.x, localPoint.y, localPoint.z);
        state.getNormal().normalize();

        state.setShader(state.getInstance().getShader(0));
        state.setModifier(state.getInstance().getModifier(0));
        // into object space
        Vector3 worldNormal = state.transformNormalObjectToWorld(state.getNormal());
        state.getNormal().set(worldNormal);
        state.getNormal().normalize();
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        FloatParameter p = pl.getPointArray("particles");
        if (p != null) {
            particles = p.data;
        }
        r = pl.getFloat("radius", r);
        r2 = r * r;
        n = pl.getInt("num", n);
        return particles != null && n <= (particles.length / 3);
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}