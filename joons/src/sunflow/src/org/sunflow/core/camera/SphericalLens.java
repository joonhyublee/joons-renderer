package org.sunflow.core.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.core.CameraLens;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;

public class SphericalLens implements CameraLens {

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        // Generate environment camera ray direction
        double theta = 2 * Math.PI * x / imageWidth + Math.PI / 2;
        double phi = Math.PI * (imageHeight - 1 - y) / imageHeight;
        return new Ray(0, 0, 0, (float) (Math.cos(theta) * Math.sin(phi)), (float) (Math.cos(phi)), (float) (Math.sin(theta) * Math.sin(phi)));
    }
}