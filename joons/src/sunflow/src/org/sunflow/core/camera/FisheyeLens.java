package org.sunflow.core.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.core.CameraLens;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;

public class FisheyeLens implements CameraLens {

    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        float cx = 2.0f * x / imageWidth - 1.0f;
        float cy = 2.0f * y / imageHeight - 1.0f;
        float r2 = cx * cx + cy * cy;
        if (r2 > 1) {
            return null; // outside the fisheye
        }
        return new Ray(0, 0, 0, cx, cy, (float) -Math.sqrt(1 - r2));
    }
}