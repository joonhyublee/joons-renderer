package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.Ray;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class PathTracingGIEngine implements GIEngine {

    private int samples;

    @Override
    public boolean init(Options options, Scene scene) {
        samples = options.getInt("gi.path.samples", 16);
        samples = Math.max(0, samples);
        UI.printInfo(Module.LIGHT, "Path tracer settings:");
        UI.printInfo(Module.LIGHT, "  * Samples: %d", samples);
        return true;
    }

    @Override
    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        if (samples <= 0) {
            return Color.BLACK;
        }
        // compute new sample
        Color irr = Color.black();
        OrthoNormalBasis onb = state.getBasis();
        Vector3 w = new Vector3();
        int n = state.getDiffuseDepth() == 0 ? samples : 1;
        for (int i = 0; i < n; i++) {
            float xi = (float) state.getRandom(i, 0, n);
            float xj = (float) state.getRandom(i, 1, n);
            float phi = (float) (xi * 2 * Math.PI);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            w.x = cosPhi * sinTheta;
            w.y = sinPhi * sinTheta;
            w.z = cosTheta;
            onb.transform(w);
            ShadingState temp = state.traceFinalGather(new Ray(state.getPoint(), w), i);
            if (temp != null) {
                temp.getInstance().prepareShadingState(temp);
                if (temp.getShader() != null) {
                    irr.add(temp.getShader().getRadiance(temp));
                }
            }
        }
        irr.mul((float) Math.PI / n);
        return irr;
    }

    @Override
    public Color getGlobalRadiance(ShadingState state) {
        return Color.BLACK;
    }
}