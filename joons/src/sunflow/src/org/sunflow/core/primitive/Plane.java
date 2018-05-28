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
import org.sunflow.math.Vector3;

public class Plane implements PrimitiveList {

    private Point3 center;
    private Vector3 normal;
    int k;
    private float bnu, bnv, bnd;
    private float cnu, cnv, cnd;

    public Plane() {
        center = new Point3(0, 0, 0);
        normal = new Vector3(0, 1, 0);
        k = 3;
        bnu = bnv = bnd = 0;
        cnu = cnv = cnd = 0;
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        center = pl.getPoint("center", center);
        Point3 b = pl.getPoint("point1", null);
        Point3 c = pl.getPoint("point2", null);
        if (b != null && c != null) {
            Point3 v0 = center;
            Point3 v1 = b;
            Point3 v2 = c;
            Vector3 ng = normal = Vector3.cross(Point3.sub(v1, v0, new Vector3()), Point3.sub(v2, v0, new Vector3()), new Vector3()).normalize();
            if (Math.abs(ng.x) > Math.abs(ng.y) && Math.abs(ng.x) > Math.abs(ng.z)) {
                k = 0;
            } else if (Math.abs(ng.y) > Math.abs(ng.z)) {
                k = 1;
            } else {
                k = 2;
            }
            float ax, ay, bx, by, cx, cy;
            switch (k) {
                case 0: {
                    ax = v0.y;
                    ay = v0.z;
                    bx = v2.y - ax;
                    by = v2.z - ay;
                    cx = v1.y - ax;
                    cy = v1.z - ay;
                    break;
                }
                case 1: {
                    ax = v0.z;
                    ay = v0.x;
                    bx = v2.z - ax;
                    by = v2.x - ay;
                    cx = v1.z - ax;
                    cy = v1.x - ay;
                    break;
                }
                case 2:
                default: {
                    ax = v0.x;
                    ay = v0.y;
                    bx = v2.x - ax;
                    by = v2.y - ay;
                    cx = v1.x - ax;
                    cy = v1.y - ay;
                }
            }
            float det = bx * cy - by * cx;
            bnu = -by / det;
            bnv = bx / det;
            bnd = (by * ax - bx * ay) / det;
            cnu = cy / det;
            cnv = -cx / det;
            cnd = (cx * ay - cy * ax) / det;
        } else {
            normal = pl.getVector("normal", normal);
            k = 3;
            bnu = bnv = bnd = 0;
            cnu = cnv = cnd = 0;
        }
        return true;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        Vector3 worldNormal = state.transformNormalObjectToWorld(normal);
        state.getNormal().set(worldNormal);
        state.getGeoNormal().set(worldNormal);
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        Point3 p = state.transformWorldToObject(state.getPoint());
        float hu, hv;
        switch (k) {
            case 0: {
                hu = p.y;
                hv = p.z;
                break;
            }
            case 1: {
                hu = p.z;
                hv = p.x;
                break;
            }
            case 2: {
                hu = p.x;
                hv = p.y;
                break;
            }
            default:
                hu = hv = 0;
        }
        state.getUV().x = hu * bnu + hv * bnv + bnd;
        state.getUV().y = hu * cnu + hv * cnv + cnd;
        state.setBasis(OrthoNormalBasis.makeFromW(normal));
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        float dn = normal.x * r.dx + normal.y * r.dy + normal.z * r.dz;
        if (dn == 0.0) {
            return;
        }
        float t = (((center.x - r.ox) * normal.x) + ((center.y - r.oy) * normal.y) + ((center.z - r.oz) * normal.z)) / dn;
        if (r.isInside(t)) {
            r.setMax(t);
            state.setIntersection(0);
        }
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return 0;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        return null;
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}