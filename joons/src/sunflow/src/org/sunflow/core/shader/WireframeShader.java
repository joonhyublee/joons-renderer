package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;

public class WireframeShader implements Shader {

    private Color lineColor;
    private Color fillColor;
    private float width;
    private float cosWidth;

    public WireframeShader() {
        lineColor = Color.BLACK;
        fillColor = Color.WHITE;
        // pick a very small angle - should be roughly the half the angular
        // width of a
        // pixel
        width = (float) (Math.PI * 0.5 / 4096);
        cosWidth = (float) Math.cos(width);
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        lineColor = pl.getColor("line", lineColor);
        fillColor = pl.getColor("fill", fillColor);
        width = pl.getFloat("width", width);
        cosWidth = (float) Math.cos(width);
        return true;
    }

    public Color getFillColor(ShadingState state) {
        return fillColor;
    }

    public Color getLineColor(ShadingState state) {
        return lineColor;
    }

    public Color getRadiance(ShadingState state) {
        Point3[] p = new Point3[3];
        if (!state.getTrianglePoints(p)) {
            return getFillColor(state);
        }
        // transform points into camera space
        Point3 center = state.getPoint();
        Matrix4 w2c = state.getWorldToCamera();
        center = w2c.transformP(center);
        for (int i = 0; i < 3; i++) {
            p[i] = w2c.transformP(state.transformObjectToWorld(p[i]));
        }
        float cn = 1.0f / (float) Math.sqrt(center.x * center.x + center.y * center.y + center.z * center.z);
        for (int i = 0, i2 = 2; i < 3; i2 = i, i++) {
            // compute orthogonal projection of the shading point onto each
            // triangle edge as in:
            // http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
            float t = (center.x - p[i].x) * (p[i2].x - p[i].x);
            t += (center.y - p[i].y) * (p[i2].y - p[i].y);
            t += (center.z - p[i].z) * (p[i2].z - p[i].z);
            t /= p[i].distanceToSquared(p[i2]);
            float projx = (1 - t) * p[i].x + t * p[i2].x;
            float projy = (1 - t) * p[i].y + t * p[i2].y;
            float projz = (1 - t) * p[i].z + t * p[i2].z;
            float n = 1.0f / (float) Math.sqrt(projx * projx + projy * projy + projz * projz);
            // check angular width
            float dot = projx * center.x + projy * center.y + projz * center.z;
            if (dot * n * cn >= cosWidth) {
                return getLineColor(state);
            }
        }
        return getFillColor(state);
    }

    public void scatterPhoton(ShadingState state, Color power) {
    }
}