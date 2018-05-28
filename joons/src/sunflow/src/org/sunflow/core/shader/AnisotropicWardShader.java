package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.LightSample;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;

public class AnisotropicWardShader implements Shader {

    private Color rhoD; // diffuse reflectance
    private Color rhoS; // specular reflectance
    private float alphaX;
    private float alphaY;
    private int numRays;

    public AnisotropicWardShader() {
        rhoD = Color.GRAY;
        rhoS = Color.GRAY;
        alphaX = 1;
        alphaY = 1;
        numRays = 4;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        rhoD = pl.getColor("diffuse", rhoD);
        rhoS = pl.getColor("specular", rhoS);
        alphaX = pl.getFloat("roughnessX", alphaX);
        alphaY = pl.getFloat("roughnessY", alphaY);
        numRays = pl.getInt("samples", numRays);
        return true;
    }

    protected Color getDiffuse(ShadingState state) {
        return rhoD;
    }

    private float brdf(Vector3 i, Vector3 o, OrthoNormalBasis basis) {
        float fr = 4 * (float) Math.PI * alphaX * alphaY;
        float p = basis.untransformZ(i) * basis.untransformZ(o);
        if (p > 0) {
            fr *= (float) Math.sqrt(p);
        } else {
            fr = 0;
        }
        Vector3 h = Vector3.add(i, o, new Vector3());
        basis.untransform(h);
        float hx = h.x / alphaX;
        hx *= hx;
        float hy = h.y / alphaY;
        hy *= hy;
        float hn = h.z * h.z;
        if (fr > 0) {
            fr = (float) Math.exp(-(hx + hy) / hn) / fr;
        }
        return fr;
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        OrthoNormalBasis onb = state.getBasis();
        // direct lighting and caustics
        state.initLightSamples();
        state.initCausticSamples();
        Color lr = Color.black();
        // compute specular contribution
        if (state.includeSpecular()) {
            Vector3 in = state.getRay().getDirection().negate(new Vector3());
            for (LightSample sample : state) {
                float cosNL = sample.dot(state.getNormal());
                float fr = brdf(in, sample.getShadowRay().getDirection(), onb);
                lr.madd(cosNL * fr, sample.getSpecularRadiance());
            }

            // indirect lighting - specular
            if (numRays > 0) {
                int n = state.getDepth() == 0 ? numRays : 1;
                for (int i = 0; i < n; i++) {
                    // specular indirect lighting
                    double r1 = state.getRandom(i, 0, n);
                    double r2 = state.getRandom(i, 1, n);

                    float alphaRatio = alphaY / alphaX;
                    float phi = 0;
                    if (r1 < 0.25) {
                        double val = 4 * r1;
                        phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                    } else if (r1 < 0.5) {
                        double val = 1 - 4 * (0.5 - r1);
                        phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                        phi = (float) Math.PI - phi;
                    } else if (r1 < 0.75) {
                        double val = 4 * (r1 - 0.5);
                        phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                        phi += Math.PI;
                    } else {
                        double val = 1 - 4 * (1 - r1);
                        phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                        phi = 2 * (float) Math.PI - phi;
                    }

                    float cosPhi = (float) Math.cos(phi);
                    float sinPhi = (float) Math.sin(phi);

                    float denom = (cosPhi * cosPhi) / (alphaX * alphaX) + (sinPhi * sinPhi) / (alphaY * alphaY);
                    float theta = (float) Math.atan(Math.sqrt(-Math.log(1 - r2) / denom));

                    float sinTheta = (float) Math.sin(theta);
                    float cosTheta = (float) Math.cos(theta);

                    Vector3 h = new Vector3();
                    h.x = sinTheta * cosPhi;
                    h.y = sinTheta * sinPhi;
                    h.z = cosTheta;
                    onb.transform(h);

                    Vector3 o = new Vector3();
                    float ih = Vector3.dot(h, in);
                    o.x = 2 * ih * h.x - in.x;
                    o.y = 2 * ih * h.y - in.y;
                    o.z = 2 * ih * h.z - in.z;

                    float no = onb.untransformZ(o);
                    float ni = onb.untransformZ(in);
                    float w = ih * cosTheta * cosTheta * cosTheta * (float) Math.sqrt(Math.abs(no / ni));

                    Ray r = new Ray(state.getPoint(), o);
                    lr.madd(w / n, state.traceGlossy(r, i));
                }
            }
            lr.mul(rhoS);
        }
        // add diffuse contribution
        lr.add(state.diffuse(getDiffuse(state)));
        return lr;
    }

    public void scatterPhoton(ShadingState state, Color power) {
        // make sure we are on the right side of the material
        state.faceforward();
        Color d = getDiffuse(state);
        state.storePhoton(state.getRay().getDirection(), power, d);
        float avgD = d.getAverage();
        float avgS = rhoS.getAverage();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < avgD) {
            // photon is scattered diffusely
            power.mul(d).mul(1.0f / avgD);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / avgD;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0f - v);
            Vector3 w = new Vector3((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w, new Vector3());
            state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        } else if (rnd < avgD + avgS) {
            // photon is scattered specularly
            power.mul(rhoS).mul(1 / avgS);
            OrthoNormalBasis basis = state.getBasis();
            Vector3 in = state.getRay().getDirection().negate(new Vector3());
            double r1 = rnd / avgS;
            double r2 = state.getRandom(0, 1, 1);

            float alphaRatio = alphaY / alphaX;
            float phi = 0;
            if (r1 < 0.25) {
                double val = 4 * r1;
                phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
            } else if (r1 < 0.5) {
                double val = 1 - 4 * (0.5 - r1);
                phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                phi = (float) Math.PI - phi;
            } else if (r1 < 0.75) {
                double val = 4 * (r1 - 0.5);
                phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                phi += Math.PI;
            } else {
                double val = 1 - 4 * (1 - r1);
                phi = (float) Math.atan(alphaRatio * Math.tan(Math.PI / 2 * val));
                phi = 2 * (float) Math.PI - phi;
            }

            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);

            float denom = (cosPhi * cosPhi) / (alphaX * alphaX) + (sinPhi * sinPhi) / (alphaY * alphaY);
            float theta = (float) Math.atan(Math.sqrt(-Math.log(1 - r2) / denom));

            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            Vector3 h = new Vector3();
            h.x = sinTheta * cosPhi;
            h.y = sinTheta * sinPhi;
            h.z = cosTheta;
            basis.transform(h);

            Vector3 o = new Vector3();
            float ih = Vector3.dot(h, in);
            o.x = 2 * ih * h.x - in.x;
            o.y = 2 * ih * h.y - in.y;
            o.z = 2 * ih * h.z - in.z;

            Ray r = new Ray(state.getPoint(), o);
            state.traceReflectionPhoton(r, power);
        }
    }
}