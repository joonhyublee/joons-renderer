package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ViewIrradianceShader implements Shader {

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        state.faceforward();
        return new Color().set(state.getIrradiance(Color.WHITE)).mul(1.0f / (float) Math.PI);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}