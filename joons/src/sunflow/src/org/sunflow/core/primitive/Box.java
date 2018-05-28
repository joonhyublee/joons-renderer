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
import org.sunflow.math.Vector3;

public class Box implements PrimitiveList {

    private float minX, minY, minZ;
    private float maxX, maxY, maxZ;

    public Box() {
        minX = minY = minZ = -1;
        maxX = maxY = maxZ = +1;
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        FloatParameter pts = pl.getPointArray("points");
        if (pts != null) {
            BoundingBox bounds = new BoundingBox();
            for (int i = 0; i < pts.data.length; i += 3) {
                bounds.include(pts.data[i], pts.data[i + 1], pts.data[i + 2]);
            }
            // cube extents
            minX = bounds.getMinimum().x;
            minY = bounds.getMinimum().y;
            minZ = bounds.getMinimum().z;
            maxX = bounds.getMaximum().x;
            maxY = bounds.getMaximum().y;
            maxZ = bounds.getMaximum().z;
        }
        return true;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        int n = state.getPrimitiveID();
        switch (n) {
            case 0:
                state.getNormal().set(new Vector3(1, 0, 0));
                break;
            case 1:
                state.getNormal().set(new Vector3(-1, 0, 0));
                break;
            case 2:
                state.getNormal().set(new Vector3(0, 1, 0));
                break;
            case 3:
                state.getNormal().set(new Vector3(0, -1, 0));
                break;
            case 4:
                state.getNormal().set(new Vector3(0, 0, 1));
                break;
            case 5:
                state.getNormal().set(new Vector3(0, 0, -1));
                break;
            default:
                state.getNormal().set(new Vector3(0, 0, 0));
                break;
        }
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
        state.setShader(state.getInstance().getShader(0));
        state.setModifier(state.getInstance().getModifier(0));
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        float intervalMin = Float.NEGATIVE_INFINITY;
        float intervalMax = Float.POSITIVE_INFINITY;
        float orgX = r.ox;
        float invDirX = 1 / r.dx;
        float t1, t2;
        t1 = (minX - orgX) * invDirX;
        t2 = (maxX - orgX) * invDirX;
        int sideIn = -1, sideOut = -1;
        if (invDirX > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                sideIn = 0;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
                sideOut = 1;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                sideIn = 1;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
                sideOut = 0;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgY = r.oy;
        float invDirY = 1 / r.dy;
        t1 = (minY - orgY) * invDirY;
        t2 = (maxY - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                sideIn = 2;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
                sideOut = 3;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                sideIn = 3;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
                sideOut = 2;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgZ = r.oz;
        float invDirZ = 1 / r.dz;
        t1 = (minZ - orgZ) * invDirZ; // no front wall
        t2 = (maxZ - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                sideIn = 4;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
                sideOut = 5;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                sideIn = 5;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
                sideOut = 4;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        if (r.isInside(intervalMin)) {
            r.setMax(intervalMin);
            state.setIntersection(sideIn);
        } else if (r.isInside(intervalMax)) {
            r.setMax(intervalMax);
            state.setIntersection(sideOut);
        }
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        switch (i) {
            case 0:
                return minX;
            case 1:
                return maxX;
            case 2:
                return minY;
            case 3:
                return maxY;
            case 4:
                return minZ;
            case 5:
                return maxZ;
            default:
                return 0;
        }
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(minX, minY, minZ);
        bounds.include(maxX, maxY, maxZ);
        if (o2w == null) {
            return bounds;
        }
        return o2w.transform(bounds);
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}