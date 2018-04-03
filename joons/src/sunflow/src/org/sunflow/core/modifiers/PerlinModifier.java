package org.sunflow.core.modifiers;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Modifier;
import org.sunflow.core.ParameterList;
import org.sunflow.core.ShadingState;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.PerlinScalar;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

public class PerlinModifier implements Modifier {

    private int function = 0;
    private float scale = 50;
    private float size = 1;

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        function = pl.getInt("function", function);
        size = pl.getFloat("size", size);
        scale = pl.getFloat("scale", scale);
        return true;
    }

    @Override
    public void modify(ShadingState state) {
        Point3 p = state.transformWorldToObject(state.getPoint());
        p.x *= size;
        p.y *= size;
        p.z *= size;
        Vector3 normal = state.transformNormalWorldToObject(state.getNormal());
        double f0 = f(p.x, p.y, p.z);
        double fx = f(p.x + .0001, p.y, p.z);
        double fy = f(p.x, p.y + .0001, p.z);
        double fz = f(p.x, p.y, p.z + .0001);

        normal.x -= scale * (fx - f0) / .0001;
        normal.y -= scale * (fy - f0) / .0001;
        normal.z -= scale * (fz - f0) / .0001;
        normal.normalize();

        state.getNormal().set(state.transformNormalObjectToWorld(normal));
        state.getNormal().normalize();
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
    }

    double f(double x, double y, double z) {
        switch (function) {
            case 0:
                return .03 * noise(x, y, z, 8);
            case 1:
                return .01 * stripes(x + 2 * turbulence(x, y, z, 1), 1.6);
            default:
                return -.10 * turbulence(x, y, z, 1);
        }
    }

    private static double stripes(double x, double f) {
        double t = .5 + .5 * Math.sin(f * 2 * Math.PI * x);
        return t * t - .5;
    }

    private static double turbulence(double x, double y, double z, double freq) {
        double t = -.5;
        for (; freq <= 300 / 12; freq *= 2) {
            t += Math.abs(noise(x, y, z, freq) / freq);
        }
        return t;
    }

    private static double noise(double x, double y, double z, double freq) {
        double x1, y1, z1;
        x1 = .707 * x - .707 * z;
        z1 = .707 * x + .707 * z;
        y1 = .707 * x1 + .707 * y;
        x1 = .707 * x1 - .707 * y;
        return PerlinScalar.snoise((float) (freq * x1 + 100), (float) (freq * y1), (float) (freq * z1));
    }
}