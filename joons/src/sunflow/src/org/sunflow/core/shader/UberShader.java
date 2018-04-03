package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.image.Color;
import org.sunflow.math.MathUtils;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;

public class UberShader implements Shader {

    private Color diff;
    private Color spec;
    private Texture diffmap;
    private Texture specmap;
    private float diffBlend;
    private float specBlend;
    private float glossyness;
    private int numSamples;

    public UberShader() {
        diff = spec = Color.GRAY;
        diffmap = specmap = null;
        diffBlend = specBlend = 1;
        glossyness = 0;
        numSamples = 4;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        diff = pl.getColor("diffuse", diff);
        spec = pl.getColor("specular", spec);
        String filename;
        filename = pl.getString("diffuse.texture", null);
        if (filename != null) {
            diffmap = TextureCache.getTexture(api.resolveTextureFilename(filename), false);
        }
        filename = pl.getString("specular.texture", null);
        if (filename != null) {
            specmap = TextureCache.getTexture(api.resolveTextureFilename(filename), false);
        }
        diffBlend = MathUtils.clamp(pl.getFloat("diffuse.blend", diffBlend), 0, 1);
        specBlend = MathUtils.clamp(pl.getFloat("specular.blend", diffBlend), 0, 1);
        glossyness = MathUtils.clamp(pl.getFloat("glossyness", glossyness), 0, 1);
        numSamples = pl.getInt("samples", numSamples);
        return true;
    }

    public Color getDiffuse(ShadingState state) {
        return diffmap == null ? diff : Color.blend(diff, diffmap.getPixel(state.getUV().x, state.getUV().y), diffBlend);
    }

    public Color getSpecular(ShadingState state) {
        return specmap == null ? spec : Color.blend(spec, specmap.getPixel(state.getUV().x, state.getUV().y), specBlend);
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        // direct lighting
        state.initLightSamples();
        state.initCausticSamples();
        Color d = getDiffuse(state);
        Color lr = state.diffuse(d);
        if (!state.includeSpecular()) {
            return lr;
        }
        if (glossyness == 0) {
            float cos = state.getCosND();
            float dn = 2 * cos;
            Vector3 refDir = new Vector3();
            refDir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
            refDir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
            refDir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
            Ray refRay = new Ray(state.getPoint(), refDir);
            // compute Fresnel term
            cos = 1 - cos;
            float cos2 = cos * cos;
            float cos5 = cos2 * cos2 * cos;
            Color spec = getSpecular(state);
            Color ret = Color.white();
            ret.sub(spec);
            ret.mul(cos5);
            ret.add(spec);
            return lr.add(ret.mul(state.traceReflection(refRay, 0)));
        } else {
            return lr.add(state.specularPhong(getSpecular(state), 2 / glossyness, numSamples));
        }
    }

    public void scatterPhoton(ShadingState state, Color power) {
        Color diffuse, specular;
        // make sure we are on the right side of the material
        state.faceforward();
        diffuse = getDiffuse(state);
        specular = getSpecular(state);
        state.storePhoton(state.getRay().getDirection(), power, diffuse);
        float d = diffuse.getAverage();
        float r = specular.getAverage();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < d) {
            // photon is scattered
            power.mul(diffuse).mul(1.0f / d);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / d;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0 - v);
            Vector3 w = new Vector3((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w, new Vector3());
            state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        } else if (rnd < d + r) {
            if (glossyness == 0) {
                float cos = -Vector3.dot(state.getNormal(), state.getRay().getDirection());
                power.mul(diffuse).mul(1.0f / d);
                // photon is reflected
                float dn = 2 * cos;
                Vector3 dir = new Vector3();
                dir.x = (dn * state.getNormal().x) + state.getRay().getDirection().x;
                dir.y = (dn * state.getNormal().y) + state.getRay().getDirection().y;
                dir.z = (dn * state.getNormal().z) + state.getRay().getDirection().z;
                state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
            } else {
                float dn = 2.0f * state.getCosND();
                // reflected direction
                Vector3 refDir = new Vector3();
                refDir.x = (dn * state.getNormal().x) + state.getRay().dx;
                refDir.y = (dn * state.getNormal().y) + state.getRay().dy;
                refDir.z = (dn * state.getNormal().z) + state.getRay().dz;
                power.mul(spec).mul(1.0f / r);
                OrthoNormalBasis onb = state.getBasis();
                double u = 2 * Math.PI * (rnd - r) / r;
                double v = state.getRandom(0, 1, 1);
                float s = (float) Math.pow(v, 1 / ((1.0f / glossyness) + 1));
                float s1 = (float) Math.sqrt(1 - s * s);
                Vector3 w = new Vector3((float) Math.cos(u) * s1, (float) Math.sin(u) * s1, s);
                w = onb.transform(w, new Vector3());
                state.traceReflectionPhoton(new Ray(state.getPoint(), w), power);
            }
        }
    }
}