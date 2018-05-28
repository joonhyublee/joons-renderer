package org.sunflow.core.primitive;

import java.util.Locale;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.LightSample;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.ParameterList.FloatParameter;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class Hair implements PrimitiveList, Shader {

    private int numSegments;
    private float[] points;
    private FloatParameter widths;

    public Hair() {
        numSegments = 1;
        points = null;
        widths = new FloatParameter(1.0f);
    }

    @Override
    public int getNumPrimitives() {
        return numSegments * (points.length / (3 * (numSegments + 1)));
    }

    @Override
    public float getPrimitiveBound(int primID, int i) {
        int hair = primID / numSegments;
        int line = primID % numSegments;
        int vn = hair * (numSegments + 1) + line;
        int vRoot = hair * 3 * (numSegments + 1);
        int v0 = vRoot + line * 3;
        int v1 = v0 + 3;
        int axis = i >>> 1;
        if ((i & 1) == 0) {
            return Math.min(points[v0 + axis] - 0.5f * getWidth(vn), points[v1 + axis] - 0.5f * getWidth(vn + 1));
        } else {
            return Math.max(points[v0 + axis] + 0.5f * getWidth(vn), points[v1 + axis] + 0.5f * getWidth(vn + 1));
        }
    }

    @Override
    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox();
        for (int i = 0, j = 0; i < points.length; i += 3, j++) {
            float w = 0.5f * getWidth(j);
            bounds.include(points[i] - w, points[i + 1] - w, points[i + 2] - w);
            bounds.include(points[i] + w, points[i + 1] + w, points[i + 2] + w);
        }
        if (o2w != null) {
            bounds = o2w.transform(bounds);
        }
        return bounds;
    }

    private float getWidth(int i) {
        switch (widths.interp) {
            case NONE:
                return widths.data[0];
            case VERTEX:
                return widths.data[i];
            default:
                return 0;
        }
    }

    private Vector3 getTangent(int line, int v0, float v) {
        Vector3 vcurr = new Vector3(points[v0 + 3] - points[v0 + 0], points[v0 + 4] - points[v0 + 1], points[v0 + 5] - points[v0 + 2]);
        vcurr.normalize();
        if (line == 0 || line == numSegments - 1) {
            return vcurr;
        }
        if (v <= 0.5f) {
            // get previous segment
            Vector3 vprev = new Vector3(points[v0 + 0] - points[v0 - 3], points[v0 + 1] - points[v0 - 2], points[v0 + 2] - points[v0 - 1]);
            vprev.normalize();
            float t = v + 0.5f;
            float s = 1 - t;
            float vx = vprev.x * s + vcurr.x * t;
            float vy = vprev.y * s + vcurr.y * t;
            float vz = vprev.z * s + vcurr.z * t;
            return new Vector3(vx, vy, vz);
        } else {
            // get next segment
            v0 += 3;
            Vector3 vnext = new Vector3(points[v0 + 3] - points[v0 + 0], points[v0 + 4] - points[v0 + 1], points[v0 + 5] - points[v0 + 2]);
            vnext.normalize();
            float t = 1.5f - v;
            float s = 1 - t;
            float vx = vnext.x * s + vcurr.x * t;
            float vy = vnext.y * s + vcurr.y * t;
            float vz = vnext.z * s + vcurr.z * t;
            return new Vector3(vx, vy, vz);
        }
    }

    @Override
    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        int hair = primID / numSegments;
        int line = primID % numSegments;
        int vRoot = hair * 3 * (numSegments + 1);
        int v0 = vRoot + line * 3;
        int v1 = v0 + 3;
        float vx = points[v1 + 0] - points[v0 + 0];
        float vy = points[v1 + 1] - points[v0 + 1];
        float vz = points[v1 + 2] - points[v0 + 2];
        float ux = r.dy * vz - r.dz * vy;
        float uy = r.dz * vx - r.dx * vz;
        float uz = r.dx * vy - r.dy * vx;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float tden = 1 / (nx * r.dx + ny * r.dy + nz * r.dz);
        float tnum = nx * (points[v0 + 0] - r.ox) + ny * (points[v0 + 1] - r.oy) + nz * (points[v0 + 2] - r.oz);
        float t = tnum * tden;
        if (r.isInside(t)) {
            int vn = hair * (numSegments + 1) + line;
            float px = r.ox + t * r.dx;
            float py = r.oy + t * r.dy;
            float pz = r.oz + t * r.dz;
            float qx = px - points[v0 + 0];
            float qy = py - points[v0 + 1];
            float qz = pz - points[v0 + 2];
            float q = (vx * qx + vy * qy + vz * qz) / (vx * vx + vy * vy + vz * vz);
            if (q <= 0) {
                // don't included rounded tip at root
                if (line == 0) {
                    return;
                }
                float dx = points[v0 + 0] - px;
                float dy = points[v0 + 1] - py;
                float dz = points[v0 + 2] - pz;
                float d2 = dx * dx + dy * dy + dz * dz;
                float width = getWidth(vn);
                if (d2 < (width * width * 0.25f)) {
                    r.setMax(t);
                    state.setIntersection(primID, 0, 0);
                }
            } else if (q >= 1) {
                float dx = points[v1 + 0] - px;
                float dy = points[v1 + 1] - py;
                float dz = points[v1 + 2] - pz;
                float d2 = dx * dx + dy * dy + dz * dz;
                float width = getWidth(vn + 1);
                if (d2 < (width * width * 0.25f)) {
                    r.setMax(t);
                    state.setIntersection(primID, 0, 1);
                }
            } else {
                float dx = points[v0 + 0] + q * vx - px;
                float dy = points[v0 + 1] + q * vy - py;
                float dz = points[v0 + 2] + q * vz - pz;
                float d2 = dx * dx + dy * dy + dz * dz;
                float width = (1 - q) * getWidth(vn) + q * getWidth(vn + 1);
                if (d2 < (width * width * 0.25f)) {
                    r.setMax(t);
                    state.setIntersection(primID, 0, q);
                }
            }
        }
    }

    @Override
    public void prepareShadingState(ShadingState state) {
        state.init();
        Instance i = state.getInstance();
        state.getRay().getPoint(state.getPoint());
        Ray r = state.getRay();
        Shader s = i.getShader(0);
        state.setShader(s != null ? s : this);
        int primID = state.getPrimitiveID();
        int hair = primID / numSegments;
        int line = primID % numSegments;
        int vRoot = hair * 3 * (numSegments + 1);
        int v0 = vRoot + line * 3;

        // tangent vector
        Vector3 v = getTangent(line, v0, state.getV());
        v = state.transformVectorObjectToWorld(v);
        state.setBasis(OrthoNormalBasis.makeFromWV(v, new Vector3(-r.dx, -r.dy, -r.dz)));
        state.getBasis().swapVW();
        // normal
        state.getNormal().set(0, 0, 1);
        state.getBasis().transform(state.getNormal());
        state.getGeoNormal().set(state.getNormal());

        state.getUV().set(0, (line + state.getV()) / numSegments);
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        numSegments = pl.getInt("segments", numSegments);
        if (numSegments < 1) {
            UI.printError(Module.HAIR, "Invalid number of segments: %d", numSegments);
            return false;
        }
        FloatParameter pointsP = pl.getPointArray("points");
        if (pointsP != null) {
            if (pointsP.interp != InterpolationType.VERTEX) {
                UI.printError(Module.HAIR, "Point interpolation type must be set to \"vertex\" - was \"%s\"", pointsP.interp.name().toLowerCase(Locale.ENGLISH));
            } else {
                points = pointsP.data;
            }
        }
        if (points == null) {
            UI.printError(Module.HAIR, "Unabled to update hair - vertices are missing");
            return false;
        }

        pl.setVertexCount(points.length / 3);
        FloatParameter widthsP = pl.getFloatArray("widths");
        if (widthsP != null) {
            if (widthsP.interp == InterpolationType.NONE || widthsP.interp == InterpolationType.VERTEX) {
                widths = widthsP;
            } else {
                UI.printWarning(Module.HAIR, "Width interpolation type %s is not supported -- ignoring", widthsP.interp.name().toLowerCase(Locale.ENGLISH));
            }
        }
        return true;
    }

    @Override
    public Color getRadiance(ShadingState state) {
        // don't use these - gather lights for sphere of directions
        // gather lights
        state.initLightSamples();
        state.initCausticSamples();
        Vector3 v = state.getRay().getDirection();
        v.negate();
        Vector3 h = new Vector3();
        Vector3 t = state.getBasis().transform(new Vector3(0, 1, 0));
        Color diff = Color.black();
        Color spec = Color.black();
        for (LightSample ls : state) {
            Vector3 l = ls.getShadowRay().getDirection();
            float dotTL = Vector3.dot(t, l);
            float sinTL = (float) Math.sqrt(1 - dotTL * dotTL);
            // float dotVL = Vector3.dot(v, l);
            diff.madd(sinTL, ls.getDiffuseRadiance());
            Vector3.add(v, l, h);
            h.normalize();
            float dotTH = Vector3.dot(t, h);
            float sinTH = (float) Math.sqrt(1 - dotTH * dotTH);
            float s = (float) Math.pow(sinTH, 10.0f);
            spec.madd(s, ls.getSpecularRadiance());
        }
        Color c = Color.add(diff, spec, new Color());
        // transparency
        return Color.blend(c, state.traceTransparency(), state.getV(), new Color());
    }

    @Override
    public void scatterPhoton(ShadingState state, Color power) {
    }

    @Override
    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}