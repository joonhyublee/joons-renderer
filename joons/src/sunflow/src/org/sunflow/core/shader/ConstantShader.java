package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ConstantShader implements Shader {

    private Color c;

    public ConstantShader() {
        c = Color.WHITE;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        c = pl.getColor("color", c);
        return true;
    }

    public Color getRadiance(ShadingState state) {
        return c;
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}