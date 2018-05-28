package org.sunflow.core.accel;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.IntArray;

public final class UniformGrid implements AccelerationStructure {

    private int nx, ny, nz;
    private PrimitiveList primitives;
    private BoundingBox bounds;
    private int[][] cells;
    private float voxelwx, voxelwy, voxelwz;
    private float invVoxelwx, invVoxelwy, invVoxelwz;

    public UniformGrid() {
        nx = ny = nz = 0;
        bounds = null;
        cells = null;
        voxelwx = voxelwy = voxelwz = 0;
        invVoxelwx = invVoxelwy = invVoxelwz = 0;
    }

    @Override
    public void build(PrimitiveList primitives) {
        Timer t = new Timer();
        t.start();
        this.primitives = primitives;
        int n = primitives.getNumPrimitives();
        // compute bounds
        bounds = primitives.getWorldBounds(null);
        // create grid from number of objects
        bounds.enlargeUlps();
        Vector3 w = bounds.getExtents();
        double s = Math.pow((w.x * w.y * w.z) / n, 1 / 3.0);
        nx = MathUtils.clamp((int) ((w.x / s) + 0.5), 1, 128);
        ny = MathUtils.clamp((int) ((w.y / s) + 0.5), 1, 128);
        nz = MathUtils.clamp((int) ((w.z / s) + 0.5), 1, 128);
        voxelwx = w.x / nx;
        voxelwy = w.y / ny;
        voxelwz = w.z / nz;
        invVoxelwx = 1 / voxelwx;
        invVoxelwy = 1 / voxelwy;
        invVoxelwz = 1 / voxelwz;
        UI.printDetailed(Module.ACCEL, "Creating grid: %dx%dx%d ...", nx, ny, nz);
        IntArray[] buildCells = new IntArray[nx * ny * nz];
        // add all objects into the grid cells they overlap
        int[] imin = new int[3];
        int[] imax = new int[3];
        int numCellsPerObject = 0;
        for (int i = 0; i < n; i++) {
            getGridIndex(primitives.getPrimitiveBound(i, 0), primitives.getPrimitiveBound(i, 2), primitives.getPrimitiveBound(i, 4), imin);
            getGridIndex(primitives.getPrimitiveBound(i, 1), primitives.getPrimitiveBound(i, 3), primitives.getPrimitiveBound(i, 5), imax);
            for (int ix = imin[0]; ix <= imax[0]; ix++) {
                for (int iy = imin[1]; iy <= imax[1]; iy++) {
                    for (int iz = imin[2]; iz <= imax[2]; iz++) {
                        int idx = ix + (nx * iy) + (nx * ny * iz);
                        if (buildCells[idx] == null) {
                            buildCells[idx] = new IntArray();
                        }
                        buildCells[idx].add(i);
                        numCellsPerObject++;
                    }
                }
            }
        }
        UI.printDetailed(Module.ACCEL, "Building cells ...");
        int numEmpty = 0;
        int numInFull = 0;
        cells = new int[nx * ny * nz][];
        int i = 0;
        for (IntArray cell : buildCells) {
            if (cell != null) {
                if (cell.getSize() == 0) {
                    numEmpty++;
                    cell = null;
                } else {
                    cells[i] = cell.trim();
                    numInFull += cell.getSize();
                }
            } else {
                numEmpty++;
            }
            i++;
        }
        t.end();
        UI.printDetailed(Module.ACCEL, "Uniform grid statistics:");
        UI.printDetailed(Module.ACCEL, "  * Grid cells:          %d", cells.length);
        UI.printDetailed(Module.ACCEL, "  * Used cells:          %d", cells.length - numEmpty);
        UI.printDetailed(Module.ACCEL, "  * Empty cells:         %d", numEmpty);
        UI.printDetailed(Module.ACCEL, "  * Occupancy:           %.2f%%", 100.0 * (cells.length - numEmpty) / cells.length);
        UI.printDetailed(Module.ACCEL, "  * Objects/Cell:        %.2f", (double) numInFull / (double) cells.length);
        UI.printDetailed(Module.ACCEL, "  * Objects/Used Cell:   %.2f", (double) numInFull / (double) (cells.length - numEmpty));
        UI.printDetailed(Module.ACCEL, "  * Cells/Object:        %.2f", (double) numCellsPerObject / (double) n);
        UI.printDetailed(Module.ACCEL, "  * Build time:          %s", t.toString());
    }

    @Override
    public void intersect(Ray r, IntersectionState state) {
        float intervalMin = r.getMin();
        float intervalMax = r.getMax();
        float orgX = r.ox;
        float dirX = r.dx, invDirX = 1 / dirX;
        float t1, t2;
        t1 = (bounds.getMinimum().x - orgX) * invDirX;
        t2 = (bounds.getMaximum().x - orgX) * invDirX;
        if (invDirX > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
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
        indxX = (int) ((orgX - bounds.getMinimum().x) * invVoxelwx);
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
            tnextX = intervalMin + ((indxX + 1) * voxelwx + bounds.getMinimum().x - orgX) * invDirX;
        } else {
            stepX = -1;
            stopX = -1;
            deltaX = -voxelwx * invDirX;
            tnextX = intervalMin + (indxX * voxelwx + bounds.getMinimum().x - orgX) * invDirX;
        }
        // stepping factors along Y
        indxY = (int) ((orgY - bounds.getMinimum().y) * invVoxelwy);
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
            tnextY = intervalMin + ((indxY + 1) * voxelwy + bounds.getMinimum().y - orgY) * invDirY;
        } else {
            stepY = -1;
            stopY = -1;
            deltaY = -voxelwy * invDirY;
            tnextY = intervalMin + (indxY * voxelwy + bounds.getMinimum().y - orgY) * invDirY;
        }
        // stepping factors along Z
        indxZ = (int) ((orgZ - bounds.getMinimum().z) * invVoxelwz);
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
            tnextZ = intervalMin + ((indxZ + 1) * voxelwz + bounds.getMinimum().z - orgZ) * invDirZ;
        } else {
            stepZ = -1;
            stopZ = -1;
            deltaZ = -voxelwz * invDirZ;
            tnextZ = intervalMin + (indxZ * voxelwz + bounds.getMinimum().z - orgZ) * invDirZ;
        }
        int cellstepX = stepX;
        int cellstepY = stepY * nx;
        int cellstepZ = stepZ * ny * nx;
        int cell = indxX + indxY * nx + indxZ * ny * nx;
        // trace through the grid
        for (;;) {
            if (tnextX < tnextY && tnextX < tnextZ) {
                if (cells[cell] != null) {
                    for (int i : cells[cell]) {
                        primitives.intersectPrimitive(r, i, state);
                    }
                    if (state.hit() && (r.getMax() < tnextX && r.getMax() < intervalMax)) {
                        return;
                    }
                }
                intervalMin = tnextX;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxX += stepX;
                if (indxX == stopX) {
                    return;
                }
                tnextX += deltaX;
                cell += cellstepX;
            } else if (tnextY < tnextZ) {
                if (cells[cell] != null) {
                    for (int i : cells[cell]) {
                        primitives.intersectPrimitive(r, i, state);
                    }
                    if (state.hit() && (r.getMax() < tnextY && r.getMax() < intervalMax)) {
                        return;
                    }
                }
                intervalMin = tnextY;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxY += stepY;
                if (indxY == stopY) {
                    return;
                }
                tnextY += deltaY;
                cell += cellstepY;
            } else {
                if (cells[cell] != null) {
                    for (int i : cells[cell]) {
                        primitives.intersectPrimitive(r, i, state);
                    }
                    if (state.hit() && (r.getMax() < tnextZ && r.getMax() < intervalMax)) {
                        return;
                    }
                }
                intervalMin = tnextZ;
                if (intervalMin > intervalMax) {
                    return;
                }
                indxZ += stepZ;
                if (indxZ == stopZ) {
                    return;
                }
                tnextZ += deltaZ;
                cell += cellstepZ;
            }
        }
    }

    private void getGridIndex(float x, float y, float z, int[] i) {
        i[0] = MathUtils.clamp((int) ((x - bounds.getMinimum().x) * invVoxelwx), 0, nx - 1);
        i[1] = MathUtils.clamp((int) ((y - bounds.getMinimum().y) * invVoxelwy), 0, ny - 1);
        i[2] = MathUtils.clamp((int) ((z - bounds.getMinimum().z) * invVoxelwz), 0, nz - 1);
    }
}