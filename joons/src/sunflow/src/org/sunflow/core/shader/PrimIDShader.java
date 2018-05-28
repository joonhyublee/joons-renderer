package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class PrimIDShader implements Shader {

    private static final Color[] BORDERS = {Color.RED, Color.GREEN,
        Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        Vector3 n = state.getNormal();
        float f = n == null ? 1.0f : Math.abs(state.getRay().dot(n));
        return BORDERS[state.getPrimitiveID() % BORDERS.length].copy().mul(f);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}