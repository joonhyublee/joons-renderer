package org.sunflow.core.camera;

import org.sunflow.SunflowAPI;
import org.sunflow.core.CameraLens;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;

public class ThinLens implements CameraLens {

    private float au, av;
    private float aspect, fov;
    private float shiftX, shiftY;
    private float focusDistance;
    private float lensRadius;
    private int lensSides;
    private float lensRotation;
    private float lensRotationRadians;

    public ThinLens() {
        focusDistance = 1;
        lensRadius = 0;
        fov = 90;
        aspect = 1;
        lensSides = 0; // < 3 means use circular lens
        lensRotation = lensRotationRadians = 0; // this rotates polygonal lenses
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        // get parameters
        fov = pl.getFloat("fov", fov);
        aspect = pl.getFloat("aspect", aspect);
        shiftX = pl.getFloat("shift.x", shiftX);
        shiftY = pl.getFloat("shift.y", shiftY);
        focusDistance = pl.getFloat("focus.distance", focusDistance);
        lensRadius = pl.getFloat("lens.radius", lensRadius);
        lensSides = pl.getInt("lens.sides", lensSides);
        lensRotation = pl.getFloat("lens.rotation", lensRotation);
        update();
        return true;
    }

    private void update() {
        au = (float) Math.tan(Math.toRadians(fov * 0.5f)) * focusDistance;
        av = au / aspect;
        lensRotationRadians = (float) Math.toRadians(lensRotation);
    }

    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, double time) {
        float du = shiftX * focusDistance - au + ((2.0f * au * x) / (imageWidth - 1.0f));
        float dv = shiftY * focusDistance - av + ((2.0f * av * y) / (imageHeight - 1.0f));

        float eyeX, eyeY;
        if (lensSides < 3) {
            double angle, r;
            // concentric map sampling
            double r1 = 2 * lensX - 1;
            double r2 = 2 * lensY - 1;
            if (r1 > -r2) {
                if (r1 > r2) {
                    r = r1;
                    angle = 0.25 * Math.PI * r2 / r1;
                } else {
                    r = r2;
                    angle = 0.25 * Math.PI * (2 - r1 / r2);
                }
            } else {
                if (r1 < r2) {
                    r = -r1;
                    angle = 0.25 * Math.PI * (4 + r2 / r1);
                } else {
                    r = -r2;
                    if (r2 != 0) {
                        angle = 0.25 * Math.PI * (6 - r1 / r2);
                    } else {
                        angle = 0;
                    }
                }
            }
            r *= lensRadius;
            // point on the lens
            eyeX = (float) (Math.cos(angle) * r);
            eyeY = (float) (Math.sin(angle) * r);
        } else {
            // sample N-gon
            // FIXME: this could use concentric sampling
            lensY *= lensSides;
            float side = (int) lensY;
            float offs = (float) lensY - side;
            float dist = (float) Math.sqrt(lensX);
            float a0 = (float) (side * Math.PI * 2.0f / lensSides + lensRotationRadians);
            float a1 = (float) ((side + 1.0f) * Math.PI * 2.0f / lensSides + lensRotationRadians);
            eyeX = (float) ((Math.cos(a0) * (1.0f - offs) + Math.cos(a1) * offs) * dist);
            eyeY = (float) ((Math.sin(a0) * (1.0f - offs) + Math.sin(a1) * offs) * dist);
            eyeX *= lensRadius;
            eyeY *= lensRadius;
        }
        float eyeZ = 0;
        // point on the image plane
        float dirX = du;
        float dirY = dv;
        float dirZ = -focusDistance;
        // ray
        return new Ray(eyeX, eyeY, eyeZ, dirX - eyeX, dirY - eyeY, dirZ - eyeZ);
    }
}