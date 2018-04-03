package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class AmbientOcclusionShader implements Shader {

    private Color bright;
    private Color dark;
    private int samples;
    private float maxDist;

    public AmbientOcclusionShader() {
        bright = Color.WHITE;
        dark = Color.BLACK;
        samples = 32;
        maxDist = Float.POSITIVE_INFINITY;
    }

    public AmbientOcclusionShader(Color c, float d) {
        this();
        bright = c;
        maxDist = d;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        bright = pl.getColor("bright", bright);
        dark = pl.getColor("dark", dark);
        samples = pl.getInt("samples", samples);
        maxDist = pl.getFloat("maxdist", maxDist);
        if (maxDist <= 0) {
            maxDist = Float.POSITIVE_INFINITY;
        }
        return true;
    }

    public Color getBrightColor(ShadingState state) {
        return bright;
    }

    public Color getRadiance(ShadingState state) {
        return state.occlusion(samples, maxDist, getBrightColor(state), dark);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}