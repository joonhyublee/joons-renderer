package org.sunflow.core.primitive;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.core.ParameterList.FloatParameter;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class TriangleMesh implements PrimitiveList {

    private static boolean smallTriangles = false;
    protected float[] points;
    protected int[] triangles;
    private WaldTriangle[] triaccel;
    private FloatParameter normals;
    private FloatParameter uvs;
    private byte[] faceShaders;

    public static void setSmallTriangles(boolean smallTriangles) {
        if (smallTriangles) {
            UI.printInfo(Module.GEOM, "Small trimesh mode: enabled");
        } else {
            UI.printInfo(Module.GEOM, "Small trimesh mode: disabled");
        }
        TriangleMesh.smallTriangles = smallTriangles;
    }

    public TriangleMesh() {
        triangles = null;
        points = null;
        normals = uvs = new FloatParameter();
        faceShaders = null;
    }

    public void writeObj(String filename) {
        try {
            FileWriter file = new FileWriter(filename);
            file.write(String.format("o object\n"));
            for (int i = 0; i < points.length; i += 3) {
                file.write(String.format("v %g %g %g\n", points[i], points[i + 1], points[i + 2]));
            }
            file.write("s off\n");
            for (int i = 0; i < triangles.length; i += 3) {
                file.write(String.format("f %d %d %d\n", triangles[i] + 1, triangles[i + 1] + 1, triangles[i + 2] + 1));
            }
            file.close();
        } catch (IOException e) {
            Logger.getLogger(TriangleMesh.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        boolean updatedTopology = false;
        {
            int[] trianglesu = pl.getIntArray("triangles");
            if (trianglesu != null) {
                this.triangles = trianglesu;
                updatedTopology = true;
            }
        }
        if (triangles == null) {
            UI.printError(Module.GEOM, "Unable to update mesh - triangle indices are missing");
            return false;
        }
        if (triangles.length % 3 != 0) {
            UI.printWarning(Module.GEOM, "Triangle index data is not a multiple of 3 - triangles may be missing");
        }
        pl.setFaceCount(triangles.length / 3);
        {
            FloatParameter pointsP = pl.getPointArray("points");
            if (pointsP != null) {
                if (pointsP.interp != InterpolationType.VERTEX) {
                    UI.printError(Module.GEOM, "Point interpolation type must be set to \"vertex\" - was \"%s\"", pointsP.interp.name().toLowerCase(Locale.ENGLISH));
                } else {
                    points = pointsP.data;
                    updatedTopology = true;
                }
            }
        }
        if (points == null) {
            UI.printError(Module.GEOM, "Unable to update mesh - vertices are missing");
            return false;
        }
        pl.setVertexCount(points.length / 3);
        pl.setFaceVertexCount(3 * (triangles.length / 3));
        FloatParameter normalsu = pl.getVectorArray("normals");
        if (normalsu != null) {
            this.normals = normalsu;
        }
        FloatParameter uvsu = pl.getTexCoordArray("uvs");
        if (uvsu != null) {
            this.uvs = uvsu;
        }
        int[] faceShadersu = pl.getIntArray("faceshaders");
        if (faceShadersu != null && faceShadersu.length == triangles.length / 3) {
            this.faceShaders = new byte[faceShadersu.length];
            for (int i = 0; i < faceShadersu.length; i++) {
                int v = faceShadersu[i];
                if (v > 255) {
                    UI.printWarning(Module.GEOM, "Shader index too large on triangle %d", i);
                }
                this.faceShaders[i] = (byte) (v & 0xFF);
            }
        }
        if (updatedTopology) {
            // create triangle acceleration structure
            init();
        }
        return true;
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        int tri = 3 * primID;
        int a = 3 * triangles[tri + 0];
        int b = 3 * triangles[tri + 1];
        int c = 3 * triangles[tri + 2];
        int axis = i >>> 1;
        if ((i & 1) == 0) {
            return MathUtils.min(points[a + axis], points[b + axis], points[c + axis]);
        } else {
            return MathUtils.max(points[a + axis], points[b + axis], points[c + axis]);
        }
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        if (o2w == null) {
            for (int i = 0; i < points.length; i += 3) {
                bounds.include(points[i], points[i + 1], points[i + 2]);
            }
        } else {
            // transform vertices first
            for (int i = 0; i < points.length; i += 3) {
                float x = points[i];
                float y = points[i + 1];
                float z = points[i + 2];
                float wx = o2w.transformPX(x, y, z);
                float wy = o2w.transformPY(x, y, z);
                float wz = o2w.transformPZ(x, y, z);
                bounds.include(wx, wy, wz);
            }
        }
        return bounds;
    }

    private void intersectTriangleKensler(Ray r, int primID, IntersectionState state) {
        int tri = 3 * primID;
        int a = 3 * triangles[tri + 0];
        int b = 3 * triangles[tri + 1];
        int c = 3 * triangles[tri + 2];
        float edge0x = points[b + 0] - points[a + 0];
        float edge0y = points[b + 1] - points[a + 1];
        float edge0z = points[b + 2] - points[a + 2];
        float edge1x = points[a + 0] - points[c + 0];
        float edge1y = points[a + 1] - points[c + 1];
        float edge1z = points[a + 2] - points[c + 2];
        float nx = edge0y * edge1z - edge0z * edge1y;
        float ny = edge0z * edge1x - edge0x * edge1z;
        float nz = edge0x * edge1y - edge0y * edge1x;
        float v = r.dot(nx, ny, nz);
        float iv = 1 / v;
        float edge2x = points[a + 0] - r.ox;
        float edge2y = points[a + 1] - r.oy;
        float edge2z = points[a + 2] - r.oz;
        float va = nx * edge2x + ny * edge2y + nz * edge2z;
        float t = iv * va;
        if (!r.isInside(t)) {
            return;
        }
        float ix = edge2y * r.dz - edge2z * r.dy;
        float iy = edge2z * r.dx - edge2x * r.dz;
        float iz = edge2x * r.dy - edge2y * r.dx;
        float v1 = ix * edge1x + iy * edge1y + iz * edge1z;
        float beta = iv * v1;
        if (beta < 0) {
            return;
        }
        float v2 = ix * edge0x + iy * edge0y + iz * edge0z;
        if ((v1 + v2) * v > v * v) {
            return;
        }
        float gamma = iv * v2;
        if (gamma < 0) {
            return;
        }
        r.setMax(t);
        state.setIntersection(primID, beta, gamma);
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // alternative test -- disabled for now
        // intersectPrimitiveRobust(r, primID, state);

        if (triaccel != null) {
            // optional fast intersection method
            triaccel[primID].intersect(r, primID, state);
            return;
        }
        intersectTriangleKensler(r, primID, state);
    }

    @Override
    public int getNumPrimitives() {
        return triangles.length / 3;
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        Instance parent = state.getInstance();
        int primID = state.getPrimitiveID();
        float u = state.getU();
        float v = state.getV();
        float w = 1 - u - v;
        state.getRay().getPoint(state.getPoint());
        int tri = 3 * primID;
        int index0 = triangles[tri + 0];
        int index1 = triangles[tri + 1];
        int index2 = triangles[tri + 2];
        Point3 v0p = getPoint(index0);
        Point3 v1p = getPoint(index1);
        Point3 v2p = getPoint(index2);
        Vector3 ng = Point3.normal(v0p, v1p, v2p);
        ng = state.transformNormalObjectToWorld(ng);
        ng.normalize();
        state.getGeoNormal().set(ng);
        switch (normals.interp) {
            case NONE:
            case FACE: {
                state.getNormal().set(ng);
                break;
            }
            case VERTEX: {
                int i30 = 3 * index0;
                int i31 = 3 * index1;
                int i32 = 3 * index2;
                float[] normalsu = this.normals.data;
                state.getNormal().x = w * normalsu[i30 + 0] + u * normalsu[i31 + 0] + v * normalsu[i32 + 0];
                state.getNormal().y = w * normalsu[i30 + 1] + u * normalsu[i31 + 1] + v * normalsu[i32 + 1];
                state.getNormal().z = w * normalsu[i30 + 2] + u * normalsu[i31 + 2] + v * normalsu[i32 + 2];
                state.getNormal().set(state.transformNormalObjectToWorld(state.getNormal()));
                state.getNormal().normalize();
                break;
            }
            case FACEVARYING: {
                int idx = 3 * tri;
                float[] normalsu = this.normals.data;
                state.getNormal().x = w * normalsu[idx + 0] + u * normalsu[idx + 3] + v * normalsu[idx + 6];
                state.getNormal().y = w * normalsu[idx + 1] + u * normalsu[idx + 4] + v * normalsu[idx + 7];
                state.getNormal().z = w * normalsu[idx + 2] + u * normalsu[idx + 5] + v * normalsu[idx + 8];
                state.getNormal().set(state.transformNormalObjectToWorld(state.getNormal()));
                state.getNormal().normalize();
                break;
            }
        }
        float uv00 = 0, uv01 = 0, uv10 = 0, uv11 = 0, uv20 = 0, uv21 = 0;
        switch (uvs.interp) {
            case NONE:
            case FACE: {
                state.getUV().x = 0;
                state.getUV().y = 0;
                break;
            }
            case VERTEX: {
                int i20 = 2 * index0;
                int i21 = 2 * index1;
                int i22 = 2 * index2;
                float[] uvsu = this.uvs.data;
                uv00 = uvsu[i20 + 0];
                uv01 = uvsu[i20 + 1];
                uv10 = uvsu[i21 + 0];
                uv11 = uvsu[i21 + 1];
                uv20 = uvsu[i22 + 0];
                uv21 = uvsu[i22 + 1];
                break;
            }
            case FACEVARYING: {
                int idx = tri << 1;
                float[] uvsu = this.uvs.data;
                uv00 = uvsu[idx + 0];
                uv01 = uvsu[idx + 1];
                uv10 = uvsu[idx + 2];
                uv11 = uvsu[idx + 3];
                uv20 = uvsu[idx + 4];
                uv21 = uvsu[idx + 5];
                break;
            }
        }
        if (uvs.interp != InterpolationType.NONE) {
            // get exact uv coords and compute tangent vectors
            state.getUV().x = w * uv00 + u * uv10 + v * uv20;
            state.getUV().y = w * uv01 + u * uv11 + v * uv21;
            float du1 = uv00 - uv20;
            float du2 = uv10 - uv20;
            float dv1 = uv01 - uv21;
            float dv2 = uv11 - uv21;
            Vector3 dp1 = Point3.sub(v0p, v2p, new Vector3()), dp2 = Point3.sub(v1p, v2p, new Vector3());
            float determinant = du1 * dv2 - dv1 * du2;
            if (determinant == 0.0f) {
                // create basis in world space
                state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
            } else {
                float invdet = 1.f / determinant;
                // Vector3 dpdu = new Vector3();
                // dpdu.x = (dv2 * dp1.x - dv1 * dp2.x) * invdet;
                // dpdu.y = (dv2 * dp1.y - dv1 * dp2.y) * invdet;
                // dpdu.z = (dv2 * dp1.z - dv1 * dp2.z) * invdet;
                Vector3 dpdv = new Vector3();
                dpdv.x = (-du2 * dp1.x + du1 * dp2.x) * invdet;
                dpdv.y = (-du2 * dp1.y + du1 * dp2.y) * invdet;
                dpdv.z = (-du2 * dp1.z + du1 * dp2.z) * invdet;
                dpdv = state.transformVectorObjectToWorld(dpdv);
                // create basis in world space
                state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), dpdv));
            }
        } else {
            state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
        }
        int shaderIndex = faceShaders == null ? 0 : (faceShaders[primID] & 0xFF);
        state.setShader(parent.getShader(shaderIndex));
        state.setModifier(parent.getModifier(shaderIndex));
    }

    public void init() {
        triaccel = null;
        int nt = getNumPrimitives();
        if (!smallTriangles) {
            // too many triangles? -- don't generate triaccel to save memory
            if (nt > 2000000) {
                UI.printWarning(Module.GEOM, "TRI - Too many triangles -- triaccel generation skipped");
                return;
            }
            triaccel = new WaldTriangle[nt];
            for (int i = 0; i < nt; i++) {
                triaccel[i] = new WaldTriangle(this, i);
            }
        }
    }

    protected Point3 getPoint(int i) {
        i *= 3;
        return new Point3(points[i], points[i + 1], points[i + 2]);
    }

    public void getPoint(int tri, int i, Point3 p) {
        int index = 3 * triangles[3 * tri + i];
        p.set(points[index], points[index + 1], points[index + 2]);
    }

    private static final class WaldTriangle {
        // private data for fast triangle intersection testing

        private int k;
        private float nu, nv, nd;
        private float bnu, bnv, bnd;
        private float cnu, cnv, cnd;

        private WaldTriangle(TriangleMesh mesh, int tri) {
            k = 0;
            tri *= 3;
            int index0 = mesh.triangles[tri + 0];
            int index1 = mesh.triangles[tri + 1];
            int index2 = mesh.triangles[tri + 2];
            Point3 v0p = mesh.getPoint(index0);
            Point3 v1p = mesh.getPoint(index1);
            Point3 v2p = mesh.getPoint(index2);
            Vector3 ng = Point3.normal(v0p, v1p, v2p);
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
                    nu = ng.y / ng.x;
                    nv = ng.z / ng.x;
                    nd = v0p.x + (nu * v0p.y) + (nv * v0p.z);
                    ax = v0p.y;
                    ay = v0p.z;
                    bx = v2p.y - ax;
                    by = v2p.z - ay;
                    cx = v1p.y - ax;
                    cy = v1p.z - ay;
                    break;
                }
                case 1: {
                    nu = ng.z / ng.y;
                    nv = ng.x / ng.y;
                    nd = (nv * v0p.x) + v0p.y + (nu * v0p.z);
                    ax = v0p.z;
                    ay = v0p.x;
                    bx = v2p.z - ax;
                    by = v2p.x - ay;
                    cx = v1p.z - ax;
                    cy = v1p.x - ay;
                    break;
                }
                case 2:
                default: {
                    nu = ng.x / ng.z;
                    nv = ng.y / ng.z;
                    nd = (nu * v0p.x) + (nv * v0p.y) + v0p.z;
                    ax = v0p.x;
                    ay = v0p.y;
                    bx = v2p.x - ax;
                    by = v2p.y - ay;
                    cx = v1p.x - ax;
                    cy = v1p.y - ay;
                }
            }
            float det = bx * cy - by * cx;
            bnu = -by / det;
            bnv = bx / det;
            bnd = (by * ax - bx * ay) / det;
            cnu = cy / det;
            cnv = -cx / det;
            cnd = (cx * ay - cy * ax) / det;
        }

        void intersect(Ray r, int primID, IntersectionState state) {
            switch (k) {
                case 0: {
                    float det = 1.0f / (r.dx + nu * r.dy + nv * r.dz);
                    float t = (nd - r.ox - nu * r.oy - nv * r.oz) * det;
                    if (!r.isInside(t)) {
                        return;
                    }
                    float hu = r.oy + t * r.dy;
                    float hv = r.oz + t * r.dz;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f) {
                        return;
                    }
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f) {
                        return;
                    }
                    if (u + v > 1.0f) {
                        return;
                    }
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                    return;
                }
                case 1: {
                    float det = 1.0f / (r.dy + nu * r.dz + nv * r.dx);
                    float t = (nd - r.oy - nu * r.oz - nv * r.ox) * det;
                    if (!r.isInside(t)) {
                        return;
                    }
                    float hu = r.oz + t * r.dz;
                    float hv = r.ox + t * r.dx;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f) {
                        return;
                    }
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f) {
                        return;
                    }
                    if (u + v > 1.0f) {
                        return;
                    }
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                    return;
                }
                case 2: {
                    float det = 1.0f / (r.dz + nu * r.dx + nv * r.dy);
                    float t = (nd - r.oz - nu * r.ox - nv * r.oy) * det;
                    if (!r.isInside(t)) {
                        return;
                    }
                    float hu = r.ox + t * r.dx;
                    float hv = r.oy + t * r.dy;
                    float u = hu * bnu + hv * bnv + bnd;
                    if (u < 0.0f) {
                        return;
                    }
                    float v = hu * cnu + hv * cnv + cnd;
                    if (v < 0.0f) {
                        return;
                    }
                    if (u + v > 1.0f) {
                        return;
                    }
                    r.setMax(t);
                    state.setIntersection(primID, u, v);
                }
            }
        }
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        switch (uvs.interp) {
            case NONE:
            case FACE:
                UI.printError(Module.GEOM, "Cannot generate baking surface without texture coordinate data");
                return null;
            default:
                return new BakingSurface();
        }
    }

    private class BakingSurface implements PrimitiveList {

        @Override
        public PrimitiveList getBakingPrimitives() {
            return null;
        }

        @Override
        public int getNumPrimitives() {
            return TriangleMesh.this.getNumPrimitives();
        }

        @Override
        public float getPrimitiveBound(int primID, int i) {
            if (i > 3) {
                return 0;
            }
            switch (uvs.interp) {
                case NONE:
                case FACE:
                default: {
                    return 0;
                }
                case VERTEX: {
                    int tri = 3 * primID;
                    int index0 = triangles[tri + 0];
                    int index1 = triangles[tri + 1];
                    int index2 = triangles[tri + 2];
                    int i20 = 2 * index0;
                    int i21 = 2 * index1;
                    int i22 = 2 * index2;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    switch (i) {
                        case 0:
                            return MathUtils.min(uvs[i20 + 0], uvs[i21 + 0], uvs[i22 + 0]);
                        case 1:
                            return MathUtils.max(uvs[i20 + 0], uvs[i21 + 0], uvs[i22 + 0]);
                        case 2:
                            return MathUtils.min(uvs[i20 + 1], uvs[i21 + 1], uvs[i22 + 1]);
                        case 3:
                            return MathUtils.max(uvs[i20 + 1], uvs[i21 + 1], uvs[i22 + 1]);
                        default:
                            return 0;
                    }
                }
                case FACEVARYING: {
                    int idx = 6 * primID;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    switch (i) {
                        case 0:
                            return MathUtils.min(uvs[idx + 0], uvs[idx + 2], uvs[idx + 4]);
                        case 1:
                            return MathUtils.max(uvs[idx + 0], uvs[idx + 2], uvs[idx + 4]);
                        case 2:
                            return MathUtils.min(uvs[idx + 1], uvs[idx + 3], uvs[idx + 5]);
                        case 3:
                            return MathUtils.max(uvs[idx + 1], uvs[idx + 3], uvs[idx + 5]);
                        default:
                            return 0;
                    }
                }
            }
        }

        @Override
        public BoundingBox getWorldBounds(Matrix4 o2w) {
            BoundingBox bounds = new BoundingBox();
            if (o2w == null) {
                for (int i = 0; i < uvs.data.length; i += 2) {
                    bounds.include(uvs.data[i], uvs.data[i + 1], 0);
                }
            } else {
                // transform vertices first
                for (int i = 0; i < uvs.data.length; i += 2) {
                    float x = uvs.data[i];
                    float y = uvs.data[i + 1];
                    float wx = o2w.transformPX(x, y, 0);
                    float wy = o2w.transformPY(x, y, 0);
                    float wz = o2w.transformPZ(x, y, 0);
                    bounds.include(wx, wy, wz);
                }
            }
            return bounds;
        }

        @Override
        public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
            float uv00 = 0, uv01 = 0, uv10 = 0, uv11 = 0, uv20 = 0, uv21 = 0;
            switch (uvs.interp) {
                case NONE:
                case FACE:
                default:
                    return;
                case VERTEX: {
                    int tri = 3 * primID;
                    int index0 = triangles[tri + 0];
                    int index1 = triangles[tri + 1];
                    int index2 = triangles[tri + 2];
                    int i20 = 2 * index0;
                    int i21 = 2 * index1;
                    int i22 = 2 * index2;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    uv00 = uvs[i20 + 0];
                    uv01 = uvs[i20 + 1];
                    uv10 = uvs[i21 + 0];
                    uv11 = uvs[i21 + 1];
                    uv20 = uvs[i22 + 0];
                    uv21 = uvs[i22 + 1];
                    break;

                }
                case FACEVARYING: {
                    int idx = (3 * primID) << 1;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    uv00 = uvs[idx + 0];
                    uv01 = uvs[idx + 1];
                    uv10 = uvs[idx + 2];
                    uv11 = uvs[idx + 3];
                    uv20 = uvs[idx + 4];
                    uv21 = uvs[idx + 5];
                    break;
                }
            }

            double edge1x = uv10 - uv00;
            double edge1y = uv11 - uv01;
            double edge2x = uv20 - uv00;
            double edge2y = uv21 - uv01;
            double pvecx = r.dy * 0 - r.dz * edge2y;
            double pvecy = r.dz * edge2x - r.dx * 0;
            double pvecz = r.dx * edge2y - r.dy * edge2x;
            double qvecx, qvecy, qvecz;
            double u, v;
            double det = edge1x * pvecx + edge1y * pvecy + 0 * pvecz;
            if (det > 0) {
                double tvecx = r.ox - uv00;
                double tvecy = r.oy - uv01;
                double tvecz = r.oz;
                u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
                if (u < 0.0 || u > det) {
                    return;
                }
                qvecx = tvecy * 0 - tvecz * edge1y;
                qvecy = tvecz * edge1x - tvecx * 0;
                qvecz = tvecx * edge1y - tvecy * edge1x;
                v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
                if (v < 0.0 || u + v > det) {
                    return;
                }
            } else if (det < 0) {
                double tvecx = r.ox - uv00;
                double tvecy = r.oy - uv01;
                double tvecz = r.oz;
                u = (tvecx * pvecx + tvecy * pvecy + tvecz * pvecz);
                if (u > 0.0 || u < det) {
                    return;
                }
                qvecx = tvecy * 0 - tvecz * edge1y;
                qvecy = tvecz * edge1x - tvecx * 0;
                qvecz = tvecx * edge1y - tvecy * edge1x;
                v = (r.dx * qvecx + r.dy * qvecy + r.dz * qvecz);
                if (v > 0.0 || u + v < det) {
                    return;
                }
            } else {
                return;
            }
            double inv_det = 1.0 / det;
            float t = (float) ((edge2x * qvecx + edge2y * qvecy + 0 * qvecz) * inv_det);
            if (r.isInside(t)) {
                r.setMax(t);
                state.setIntersection(primID, (float) (u * inv_det), (float) (v * inv_det));
            }
        }

        @Override
        public void prepareShadingState(ShadingState state) {
            state.init();
            Instance parent = state.getInstance();
            int primID = state.getPrimitiveID();
            float u = state.getU();
            float v = state.getV();
            float w = 1 - u - v;
            // state.getRay().getPoint(state.getPoint());
            int tri = 3 * primID;
            int index0 = triangles[tri + 0];
            int index1 = triangles[tri + 1];
            int index2 = triangles[tri + 2];
            Point3 v0p = getPoint(index0);
            Point3 v1p = getPoint(index1);
            Point3 v2p = getPoint(index2);

            // get object space point from barycentric coordinates
            state.getPoint().x = w * v0p.x + u * v1p.x + v * v2p.x;
            state.getPoint().y = w * v0p.y + u * v1p.y + v * v2p.y;
            state.getPoint().z = w * v0p.z + u * v1p.z + v * v2p.z;
            // move into world space
            state.getPoint().set(state.transformObjectToWorld(state.getPoint()));

            Vector3 ng = Point3.normal(v0p, v1p, v2p);
            if (parent != null) {
                ng = state.transformNormalObjectToWorld(ng);
            }
            ng.normalize();
            state.getGeoNormal().set(ng);
            switch (normals.interp) {
                case NONE:
                case FACE: {
                    state.getNormal().set(ng);
                    break;
                }
                case VERTEX: {
                    int i30 = 3 * index0;
                    int i31 = 3 * index1;
                    int i32 = 3 * index2;
                    float[] normals = TriangleMesh.this.normals.data;
                    state.getNormal().x = w * normals[i30 + 0] + u * normals[i31 + 0] + v * normals[i32 + 0];
                    state.getNormal().y = w * normals[i30 + 1] + u * normals[i31 + 1] + v * normals[i32 + 1];
                    state.getNormal().z = w * normals[i30 + 2] + u * normals[i31 + 2] + v * normals[i32 + 2];
                    if (parent != null) {
                        state.getNormal().set(state.transformNormalObjectToWorld(state.getNormal()));
                    }
                    state.getNormal().normalize();
                    break;
                }
                case FACEVARYING: {
                    int idx = 3 * tri;
                    float[] normals = TriangleMesh.this.normals.data;
                    state.getNormal().x = w * normals[idx + 0] + u * normals[idx + 3] + v * normals[idx + 6];
                    state.getNormal().y = w * normals[idx + 1] + u * normals[idx + 4] + v * normals[idx + 7];
                    state.getNormal().z = w * normals[idx + 2] + u * normals[idx + 5] + v * normals[idx + 8];
                    if (parent != null) {
                        state.getNormal().set(state.transformNormalObjectToWorld(state.getNormal()));
                    }
                    state.getNormal().normalize();
                    break;
                }
            }
            float uv00 = 0, uv01 = 0, uv10 = 0, uv11 = 0, uv20 = 0, uv21 = 0;
            switch (uvs.interp) {
                case NONE:
                case FACE: {
                    state.getUV().x = 0;
                    state.getUV().y = 0;
                    break;
                }
                case VERTEX: {
                    int i20 = 2 * index0;
                    int i21 = 2 * index1;
                    int i22 = 2 * index2;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    uv00 = uvs[i20 + 0];
                    uv01 = uvs[i20 + 1];
                    uv10 = uvs[i21 + 0];
                    uv11 = uvs[i21 + 1];
                    uv20 = uvs[i22 + 0];
                    uv21 = uvs[i22 + 1];
                    break;
                }
                case FACEVARYING: {
                    int idx = tri << 1;
                    float[] uvs = TriangleMesh.this.uvs.data;
                    uv00 = uvs[idx + 0];
                    uv01 = uvs[idx + 1];
                    uv10 = uvs[idx + 2];
                    uv11 = uvs[idx + 3];
                    uv20 = uvs[idx + 4];
                    uv21 = uvs[idx + 5];
                    break;
                }
            }
            if (uvs.interp != InterpolationType.NONE) {
                // get exact uv coords and compute tangent vectors
                state.getUV().x = w * uv00 + u * uv10 + v * uv20;
                state.getUV().y = w * uv01 + u * uv11 + v * uv21;
                float du1 = uv00 - uv20;
                float du2 = uv10 - uv20;
                float dv1 = uv01 - uv21;
                float dv2 = uv11 - uv21;
                Vector3 dp1 = Point3.sub(v0p, v2p, new Vector3()), dp2 = Point3.sub(v1p, v2p, new Vector3());
                float determinant = du1 * dv2 - dv1 * du2;
                if (determinant == 0.0f) {
                    // create basis in world space
                    state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
                } else {
                    float invdet = 1.f / determinant;
                    // Vector3 dpdu = new Vector3();
                    // dpdu.x = (dv2 * dp1.x - dv1 * dp2.x) * invdet;
                    // dpdu.y = (dv2 * dp1.y - dv1 * dp2.y) * invdet;
                    // dpdu.z = (dv2 * dp1.z - dv1 * dp2.z) * invdet;
                    Vector3 dpdv = new Vector3();
                    dpdv.x = (-du2 * dp1.x + du1 * dp2.x) * invdet;
                    dpdv.y = (-du2 * dp1.y + du1 * dp2.y) * invdet;
                    dpdv.z = (-du2 * dp1.z + du1 * dp2.z) * invdet;
                    if (parent != null) {
                        dpdv = state.transformVectorObjectToWorld(dpdv);
                    }
                    // create basis in world space
                    state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), dpdv));
                }
            } else {
                state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
            }
            int shaderIndex = faceShaders == null ? 0 : (faceShaders[primID] & 0xFF);
            state.setShader(parent.getShader(shaderIndex));
        }

        @Override
        public boolean update(ParameterList pl, SunflowAPI api) {
            return true;
        }
    }
}