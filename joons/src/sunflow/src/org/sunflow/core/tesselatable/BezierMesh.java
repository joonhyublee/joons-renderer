package org.sunflow.core.tesselatable;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.FloatParameter;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.primitive.QuadMesh;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class BezierMesh implements Tesselatable {

    private int subdivs;
    private boolean smooth;
    private boolean quads;
    private float[][] patches;

    public BezierMesh() {
        this(null);
    }

    public BezierMesh(float[][] patches) {
        subdivs = 8;
        smooth = true;
        quads = false;
        // convert to single precision
        this.patches = patches;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        if (o2w == null) {
            for (int i = 0; i < patches.length; i++) {
                float[] patch = patches[i];
                for (int j = 0; j < patch.length; j += 3) {
                    bounds.include(patch[j], patch[j + 1], patch[j + 2]);
                }
            }
        } else {
            // transform vertices first
            for (int i = 0; i < patches.length; i++) {
                float[] patch = patches[i];
                for (int j = 0; j < patch.length; j += 3) {
                    float x = patch[j];
                    float y = patch[j + 1];
                    float z = patch[j + 2];
                    float wx = o2w.transformPX(x, y, z);
                    float wy = o2w.transformPY(x, y, z);
                    float wz = o2w.transformPZ(x, y, z);
                    bounds.include(wx, wy, wz);
                }
            }
        }
        return bounds;
    }

    private float[] bernstein(float u) {
        float[] b = new float[4];
        float i = 1 - u;
        b[0] = i * i * i;
        b[1] = 3 * u * i * i;
        b[2] = 3 * u * u * i;
        b[3] = u * u * u;
        return b;
    }

    private float[] bernsteinDeriv(float u) {
        if (!smooth) {
            return null;
        }
        float[] b = new float[4];
        float i = 1 - u;
        b[0] = 3 * (0 - i * i);
        b[1] = 3 * (i * i - 2 * u * i);
        b[2] = 3 * (2 * u * i - u * u);
        b[3] = 3 * (u * u - 0);
        return b;
    }

    private void getPatchPoint(float u, float v, float[] ctrl, float[] bu, float[] bv, float[] bdu, float[] bdv, Point3 p, Vector3 n) {
        float px = 0;
        float py = 0;
        float pz = 0;
        for (int i = 0, index = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++, index += 3) {
                float scale = bu[j] * bv[i];
                px += ctrl[index + 0] * scale;
                py += ctrl[index + 1] * scale;
                pz += ctrl[index + 2] * scale;
            }
        }
        p.x = px;
        p.y = py;
        p.z = pz;
        if (n != null) {
            float dpdux = 0;
            float dpduy = 0;
            float dpduz = 0;
            float dpdvx = 0;
            float dpdvy = 0;
            float dpdvz = 0;
            for (int i = 0, index = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++, index += 3) {
                    float scaleu = bdu[j] * bv[i];
                    dpdux += ctrl[index + 0] * scaleu;
                    dpduy += ctrl[index + 1] * scaleu;
                    dpduz += ctrl[index + 2] * scaleu;
                    float scalev = bu[j] * bdv[i];
                    dpdvx += ctrl[index + 0] * scalev;
                    dpdvy += ctrl[index + 1] * scalev;
                    dpdvz += ctrl[index + 2] * scalev;
                }
            }
            // surface normal
            n.x = (dpduy * dpdvz - dpduz * dpdvy);
            n.y = (dpduz * dpdvx - dpdux * dpdvz);
            n.z = (dpdux * dpdvy - dpduy * dpdvx);
        }
    }

    public PrimitiveList tesselate() {
        float[] vertices = new float[patches.length * (subdivs + 1) * (subdivs + 1) * 3];
        float[] normals = smooth ? new float[patches.length * (subdivs + 1) * (subdivs + 1) * 3] : null;
        float[] uvs = new float[patches.length * (subdivs + 1) * (subdivs + 1) * 2];
        int[] indices = new int[patches.length * subdivs * subdivs * (quads ? 4 : (2 * 3))];

        int vidx = 0, pidx = 0;
        float step = 1.0f / subdivs;
        int vstride = subdivs + 1;
        Point3 p = new Point3();
        Vector3 n = smooth ? new Vector3() : null;
        for (float[] patch : patches) {
            // create patch vertices
            for (int i = 0, voff = 0; i <= subdivs; i++) {
                float u = i * step;
                float[] bu = bernstein(u);
                float[] bdu = bernsteinDeriv(u);
                for (int j = 0; j <= subdivs; j++, voff += 3) {
                    float v = j * step;
                    float[] bv = bernstein(v);
                    float[] bdv = bernsteinDeriv(v);
                    getPatchPoint(u, v, patch, bu, bv, bdu, bdv, p, n);
                    vertices[vidx + voff + 0] = p.x;
                    vertices[vidx + voff + 1] = p.y;
                    vertices[vidx + voff + 2] = p.z;
                    if (smooth) {
                        normals[vidx + voff + 0] = n.x;
                        normals[vidx + voff + 1] = n.y;
                        normals[vidx + voff + 2] = n.z;
                    }
                    uvs[(vidx + voff) / 3 * 2 + 0] = u;
                    uvs[(vidx + voff) / 3 * 2 + 1] = v;
                }
            }
            // generate patch triangles
            for (int i = 0, vbase = vidx / 3; i < subdivs; i++) {
                for (int j = 0; j < subdivs; j++) {
                    int v00 = (i + 0) * vstride + (j + 0);
                    int v10 = (i + 1) * vstride + (j + 0);
                    int v01 = (i + 0) * vstride + (j + 1);
                    int v11 = (i + 1) * vstride + (j + 1);
                    if (quads) {
                        indices[pidx + 0] = vbase + v01;
                        indices[pidx + 1] = vbase + v00;
                        indices[pidx + 2] = vbase + v10;
                        indices[pidx + 3] = vbase + v11;
                        pidx += 4;
                    } else {
                        // add 2 triangles
                        indices[pidx + 0] = vbase + v00;
                        indices[pidx + 1] = vbase + v10;
                        indices[pidx + 2] = vbase + v01;
                        indices[pidx + 3] = vbase + v10;
                        indices[pidx + 4] = vbase + v11;
                        indices[pidx + 5] = vbase + v01;
                        pidx += 6;
                    }
                }
            }
            vidx += vstride * vstride * 3;
        }
        ParameterList pl = new ParameterList();
        pl.addPoints("points", InterpolationType.VERTEX, vertices);
        if (quads) {
            pl.addIntegerArray("quads", indices);
        } else {
            pl.addIntegerArray("triangles", indices);
        }
        pl.addTexCoords("uvs", InterpolationType.VERTEX, uvs);
        if (smooth) {
            pl.addVectors("normals", InterpolationType.VERTEX, normals);
        }
        PrimitiveList m = quads ? new QuadMesh() : new TriangleMesh();
        m.update(pl, null);
        pl.clear(true);
        return m;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        subdivs = pl.getInt("subdivs", subdivs);
        smooth = pl.getBoolean("smooth", smooth);
        quads = pl.getBoolean("quads", quads);
        int nu = pl.getInt("nu", 0);
        int nv = pl.getInt("nv", 0);
        pl.setVertexCount(nu * nv);
        boolean uwrap = pl.getBoolean("uwrap", false);
        boolean vwrap = pl.getBoolean("vwrap", false);
        FloatParameter points = pl.getPointArray("points");
        if (points != null && points.interp == InterpolationType.VERTEX) {
            int numUPatches = uwrap ? nu / 3 : (nu - 4) / 3 + 1;
            int numVPatches = vwrap ? nv / 3 : (nv - 4) / 3 + 1;
            if (numUPatches < 1 || numVPatches < 1) {
                UI.printError(Module.GEOM, "Invalid number of patches for bezier mesh - ignoring");
                return false;
            }
            // generate patches
            patches = new float[numUPatches * numVPatches][];
            for (int v = 0, p = 0; v < numVPatches; v++) {
                for (int u = 0; u < numUPatches; u++, p++) {
                    float[] patch = patches[p] = new float[16 * 3];
                    int up = u * 3;
                    int vp = v * 3;
                    for (int pv = 0; pv < 4; pv++) {
                        for (int pu = 0; pu < 4; pu++) {
                            int meshU = (up + pu) % nu;
                            int meshV = (vp + pv) % nv;
                            // copy point
                            patch[3 * (pv * 4 + pu) + 0] = points.data[3 * (meshU + nu * meshV) + 0];
                            patch[3 * (pv * 4 + pu) + 1] = points.data[3 * (meshU + nu * meshV) + 1];
                            patch[3 * (pv * 4 + pu) + 2] = points.data[3 * (meshU + nu * meshV) + 2];
                        }
                    }
                }
            }
        }
        if (subdivs < 1) {
            UI.printError(Module.GEOM, "Invalid subdivisions for bezier mesh - ignoring");
            return false;
        }
        if (patches == null) {
            UI.printError(Module.GEOM, "No patch data present in bezier mesh - ignoring");
            return false;
        }
        return true;
    }
}