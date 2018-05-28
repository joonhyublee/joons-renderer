package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

/**
 * This is a quick way to get a bit of ambient lighting into your scene with
 * hardly any overhead. It's based on the formula found here:
 *
 * @link http://www.cs.utah.edu/~shirley/papers/rtrt/node7.html#SECTION00031100000000000000
 */
public class FakeGIEngine implements GIEngine {

    private Vector3 up;
    private Color sky;
    private Color ground;

    @Override
    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        float cosTheta = Vector3.dot(up, state.getNormal());
        float sin2 = (1 - cosTheta * cosTheta);
        float sine = sin2 > 0 ? (float) Math.sqrt(sin2) * 0.5f : 0;
        if (cosTheta > 0) {
            return Color.blend(sky, ground, sine);
        } else {
            return Color.blend(ground, sky, sine);
        }
    }

    @Override
    public Color getGlobalRadiance(ShadingState state) {
        return Color.BLACK;
    }

    @Override
    public boolean init(Options options, Scene scene) {
        up = options.getVector("gi.fake.up", new Vector3(0, 1, 0)).normalize();
        sky = options.getColor("gi.fake.sky", Color.WHITE).copy();
        ground = options.getColor("gi.fake.ground", Color.BLACK).copy();
        sky.mul((float) Math.PI);
        ground.mul((float) Math.PI);
        return true;
    }
}