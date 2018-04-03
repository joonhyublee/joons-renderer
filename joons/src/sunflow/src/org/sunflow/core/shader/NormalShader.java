package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class NormalShader implements Shader {

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        Vector3 n = state.getNormal();
        if (n == null) {
            return Color.BLACK;
        }
        float r = (n.x + 1) * 0.5f;
        float g = (n.y + 1) * 0.5f;
        float b = (n.z + 1) * 0.5f;
        return new Color(r, g, b);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}