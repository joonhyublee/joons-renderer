package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.LightSample;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ViewCausticsShader implements Shader {

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        state.faceforward();
        state.initCausticSamples();
        // integrate a diffuse function
        Color lr = Color.black();
        for (LightSample sample : state) {
            lr.madd(sample.dot(state.getNormal()), sample.getDiffuseRadiance());
        }
        return lr.mul(1.0f / (float) Math.PI);

    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}