package org.sunflow.core.gi;

import java.util.ArrayList;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.PhotonStore;
import org.sunflow.core.Ray;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class InstantGI implements GIEngine {

    private int numPhotons;
    private int numSets;
    private float c;
    private int numBias;
    private PointLight[][] virtualLights;

    @Override
    public Color getGlobalRadiance(ShadingState state) {
        Point3 p = state.getPoint();
        Vector3 n = state.getNormal();
        int set = (int) (state.getRandom(0, 1, 1) * numSets);
        float maxAvgPow = 0;
        float minDist = 1;
        Color pow = null;
        for (PointLight vpl : virtualLights[set]) {
            maxAvgPow = Math.max(maxAvgPow, vpl.power.getAverage());
            if (Vector3.dot(n, vpl.n) > 0.9f) {
                float d = vpl.p.distanceToSquared(p);
                if (d < minDist) {
                    pow = vpl.power;
                    minDist = d;
                }
            }
        }
        return pow == null ? Color.BLACK : pow.copy().mul(1.0f / maxAvgPow);
    }

    @Override
    public boolean init(Options options, Scene scene) {
        numPhotons = options.getInt("gi.igi.samples", 64);
        numSets = options.getInt("gi.igi.sets", 1);
        c = options.getFloat("gi.igi.c", 0.00003f);
        numBias = options.getInt("gi.igi.bias_samples", 0);
        virtualLights = null;
        if (numSets < 1) {
            numSets = 1;
        }
        UI.printInfo(Module.LIGHT, "Instant Global Illumination settings:");
        UI.printInfo(Module.LIGHT, "  * Samples:     %d", numPhotons);
        UI.printInfo(Module.LIGHT, "  * Sets:        %d", numSets);
        UI.printInfo(Module.LIGHT, "  * Bias bound:  %f", c);
        UI.printInfo(Module.LIGHT, "  * Bias rays:   %d", numBias);
        virtualLights = new PointLight[numSets][];
        if (numPhotons > 0) {
            for (int i = 0, seed = 0; i < virtualLights.length; i++, seed += numPhotons) {
                PointLightStore map = new PointLightStore();
                if (!scene.calculatePhotons(map, "virtual", seed, options)) {
                    return false;
                }
                virtualLights[i] = map.virtualLights.toArray(new PointLight[map.virtualLights.size()]);
                UI.printInfo(Module.LIGHT, "Stored %d virtual point lights for set %d of %d", virtualLights[i].length, i + 1, numSets);
            }
        } else {
            // create an empty array
            for (int i = 0; i < virtualLights.length; i++) {
                virtualLights[i] = new PointLight[0];
            }
        }
        return true;
    }

    @Override
    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        float b = (float) Math.PI * c / diffuseReflectance.getMax();
        Color irr = Color.black();
        Point3 p = state.getPoint();
        Vector3 n = state.getNormal();
        int set = (int) (state.getRandom(0, 1, 1) * numSets);
        for (PointLight vpl : virtualLights[set]) {
            Ray r = new Ray(p, vpl.p);
            float dotNlD = -(r.dx * vpl.n.x + r.dy * vpl.n.y + r.dz * vpl.n.z);
            float dotND = r.dx * n.x + r.dy * n.y + r.dz * n.z;
            if (dotNlD > 0 && dotND > 0) {
                float r2 = r.getMax() * r.getMax();
                Color opacity = state.traceShadow(r);
                Color power = Color.blend(vpl.power, Color.BLACK, opacity);
                float g = (dotND * dotNlD) / r2;
                irr.madd(0.25f * Math.min(g, b), power);
            }
        }
        // bias compensation
        int nb = (state.getDiffuseDepth() == 0 || numBias <= 0) ? numBias : 1;
        if (nb <= 0) {
            return irr;
        }
        OrthoNormalBasis onb = state.getBasis();
        Vector3 w = new Vector3();
        float scale = (float) Math.PI / nb;
        for (int i = 0; i < nb; i++) {
            float xi = (float) state.getRandom(i, 0, nb);
            float xj = (float) state.getRandom(i, 1, nb);
            float phi = (float) (xi * 2 * Math.PI);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            w.x = cosPhi * sinTheta;
            w.y = sinPhi * sinTheta;
            w.z = cosTheta;
            onb.transform(w);
            Ray r = new Ray(state.getPoint(), w);
            r.setMax((float) Math.sqrt(cosTheta / b));
            ShadingState temp = state.traceFinalGather(r, i);
            if (temp != null) {
                temp.getInstance().prepareShadingState(temp);
                if (temp.getShader() != null) {
                    float dist = temp.getRay().getMax();
                    float r2 = dist * dist;
                    float cosThetaY = -Vector3.dot(w, temp.getNormal());
                    if (cosThetaY > 0) {
                        float g = (cosTheta * cosThetaY) / r2;
                        // was this path accounted for yet?
                        if (g > b) {
                            irr.madd(scale * (g - b) / g, temp.getShader().getRadiance(temp));
                        }
                    }
                }
            }
        }
        return irr;
    }

    private static class PointLight {

        Point3 p;
        Vector3 n;
        Color power;
    }

    private class PointLightStore implements PhotonStore {

        ArrayList<PointLight> virtualLights = new ArrayList<PointLight>();

        @Override
        public int numEmit() {
            return numPhotons;
        }

        @Override
        public void prepare(Options options, BoundingBox sceneBounds) {
        }

        @Override
        public void store(ShadingState state, Vector3 dir, Color power, Color diffuse) {
            state.faceforward();
            PointLight vpl = new PointLight();
            vpl.p = state.getPoint();
            vpl.n = state.getNormal();
            vpl.power = power;
            synchronized (this) {
                virtualLights.add(vpl);
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean allowDiffuseBounced() {
            return true;
        }

        @Override
        public boolean allowReflectionBounced() {
            return true;
        }

        @Override
        public boolean allowRefractionBounced() {
            return true;
        }
    }
}