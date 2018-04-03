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
import org.sunflow.math.Vector3;

public abstract class CubeGrid implements PrimitiveList {

    private int nx, ny, nz;
    private float voxelwx, voxelwy, voxelwz;
    private float invVoxelwx, invVoxelwy, invVoxelwz;
    private BoundingBox bounds;

    public CubeGrid() {
        nx = ny = nz = 1;
        bounds = new BoundingBox(1);
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        nx = pl.getInt("resolutionX", nx);
        ny = pl.getInt("resolutionY", ny);
        nz = pl.getInt("resolutionZ", nz);
        voxelwx = 2.0f / nx;
        voxelwy = 2.0f / ny;
        voxelwz = 2.0f / nz;
        invVoxelwx = 1 / voxelwx;
        invVoxelwy = 1 / voxelwy;
        invVoxelwz = 1 / voxelwz;
        return true;
    }

    protected abstract boolean inside(int x, int y, int z);

    public BoundingBox getBounds() {
        return bounds;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        state.getRay().getPoint(state.getPoint());
        Instance parent = state.getInstance();
        Vector3 normal;
        switch (state.getPrimitiveID()) {
            case 0:
                normal = new Vector3(-1, 0, 0);
                break;
            case 1:
                normal = new Vector3(1, 0, 0);
                break;
            case 2:
                normal = new Vector3(0, -1, 0);
                break;
            case 3:
                normal = new Vector3(0, 1, 0);
                break;
            case 4:
                normal = new Vector3(0, 0, -1);
                break;
            case 5:
                normal = new Vector3(0, 0, 1);
                break;
            default:
                normal = new Vector3(0, 0, 0);
                break;
        }
        state.getNormal().set(state.transformNormalObjectToWorld(normal));
        state.getGeoNormal().set(state.getNormal());
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        float intervalMin = r.getMin();
        float intervalMax = r.getMax();
        float orgX = r.ox;
        float orgY = r.oy;
        float orgZ = r.oz;
        float dirX = r.dx, invDirX = 1 / dirX;
        float dirY = r.dy, invDirY = 1 / dirY;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        float t1, t2;
        t1 = (-1 - orgX) * invDirX;
        t2 = (+1 - orgX) * invDirX;
        int curr = -1;
        if (invDirX > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 0;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 1;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        }
        t1 = (-1 - orgY) * invDirY;
        t2 = (+1 - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 2;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 3;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        }
        t1 = (-1 - orgZ) * invDirZ;
        t2 = (+1 - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
                curr = 4;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
                curr = 5;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
            if (intervalMin > intervalMax) {
                return;
            }
        }
        // box is hit at [intervalMin, intervalMax]
        orgX += intervalMin * dirX;
        orgY += intervalMin * dirY;
        orgZ += intervalMin * dirZ;
        // locate starting point inside the grid
        // and set up 3D-DDA vars
        int indxX, indxY, indxZ;
        int stepX, stepY, stepZ;
        int stopX, stopY, stopZ;
        float deltaX, deltaY, deltaZ;
        float tnextX, tnextY, tnextZ;
        // stepping factors along X
        indxX = (int) ((orgX + 1) * invVoxelwx);
        if (indxX < 0) {
            indxX = 0;
        } else if (indxX >= nx) {
            indxX = nx - 1;
        }
        if (Math.abs(dirX) < 1e-6f) {
            stepX = 0;
            stopX = indxX;
            deltaX = 0;
            tnextX = Float.POSITIVE_INFINITY;
        } else if (dirX > 0) {
            stepX = 1;
            stopX = nx;
            deltaX = voxelwx * invDirX;
            tnextX = intervalMin + ((indxX + 1) * voxelwx - 1 - orgX) * invDirX;
        } else {
            stepX = -1;
            stopX = -1;
            deltaX = -voxelwx * invDirX;
            tnextX = intervalMin + (indxX * voxelwx - 1 - orgX) * invDirX;
        }
        // stepping factors along Y
        indxY = (int) ((orgY + 1) * invVoxelwy);
        if (indxY < 0) {
            indxY = 0;
        } else if (indxY >= ny) {
            indxY = ny - 1;
        }
        if (Math.abs(dirY) < 1e-6f) {
            stepY = 0;
            stopY = indxY;
            deltaY = 0;
            tnextY = Float.POSITIVE_INFINITY;
        } else if (dirY > 0) {
            stepY = 1;
            stopY = ny;
            deltaY = voxelwy * invDirY;
            tnextY = intervalMin + ((indxY + 1) * voxelwy - 1 - orgY) * invDirY;
        } else {
            stepY = -1;
            stopY = -1;
            deltaY = -voxelwy * invDirY;
            tnextY = intervalMin + (indxY * voxelwy - 1 - orgY) * invDirY;
        }
        // stepping factors along Z
        indxZ = (int) ((orgZ + 1) * invVoxelwz);
        if (indxZ < 0) {
            indxZ = 0;
        } else if (indxZ >= nz) {
            indxZ = nz - 1;
        }
        if (Math.abs(dirZ) < 1e-6f) {
            stepZ = 0;
            stopZ = indxZ;
            deltaZ = 0;
            tnextZ = Float.POSITIVE_INFINITY;
        } else if (dirZ > 0) {
            stepZ = 1;
            stopZ = nz;
            deltaZ = voxelwz * invDirZ;
            tnextZ = intervalMin + ((indxZ + 1) * voxelwz - 1 - orgZ) * invDirZ;
        } else {
            stepZ = -1;
            stopZ = -1;
            deltaZ = -voxelwz * invDirZ;
            tnextZ = intervalMin + (indxZ * voxelwz - 1 - orgZ) * invDirZ;
        }
        // are we starting inside the cube
        boolean isInside = inside(indxX, indxY, indxZ) && bounds.contains(r.ox, r.oy, r.oz);
        // trace through the grid
        for (;;) {
            if (inside(indxX, indxY, indxZ) != isInside) {
                // we hit a boundary
                r.setMax(intervalMin);
                // if we are inside, the last bit needs to be flipped
                if (isInside) {
                    curr ^= 1;
                }
                state.setIntersection(curr);
                return;
            }
            if (tnextX < tnextY && tnextX < tnextZ) {
                curr = dirX > 0 ? 0 : 1;
                intervalMin = tnextX;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxX += stepX;
                if (indxX == stopX) {
                    return;
                }
                tnextX += deltaX;
            } else if (tnextY < tnextZ) {
                curr = dirY > 0 ? 2 : 3;
                intervalMin = tnextY;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxY += stepY;
                if (indxY == stopY) {
                    return;
                }
                tnextY += deltaY;
            } else {
                curr = dirZ > 0 ? 4 : 5;
                intervalMin = tnextZ;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxZ += stepZ;
                if (indxZ == stopZ) {
                    return;
                }
                tnextZ += deltaZ;
            }
        }
    }

    @Override
    public int getNumPrimitives() {
        return 1;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        return ((i & 1) == 0) ? -1 : 1;
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        if (o2w == null) {
            return bounds;
        }
        return o2w.transform(bounds);
    }
}