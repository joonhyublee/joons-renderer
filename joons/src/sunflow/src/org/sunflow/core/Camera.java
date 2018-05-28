package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.math.Matrix4;
import org.sunflow.math.MovingMatrix4;
import org.sunflow.math.Point3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * This class represents a camera to the renderer. It handles the mapping of
 * camera space to world space, as well as the mounting of {@link CameraLens}
 * objects which compute the actual projection.
 */
public class Camera implements RenderObject {

    private final CameraLens lens;
    private float shutterOpen;
    private float shutterClose;
    private MovingMatrix4 c2w;
    private MovingMatrix4 w2c;

    public Camera(CameraLens lens) {
        this.lens = lens;
        c2w = new MovingMatrix4(null);
        w2c = new MovingMatrix4(null);
        shutterOpen = shutterClose = 0;
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        shutterOpen = pl.getFloat("shutter.open", shutterOpen);
        shutterClose = pl.getFloat("shutter.close", shutterClose);
        c2w = pl.getMovingMatrix("transform", c2w);
        w2c = c2w.inverse();
        if (w2c == null) {
            UI.printWarning(Module.CAM, "Unable to compute camera's inverse transform");
            return false;
        }
        return lens.update(pl, api);
    }

    /**
     * Computes actual time from a time sample in the interval [0,1). This
     * random number is mapped somewhere between the shutterOpen and
     * shutterClose times.
     *
     * @param time
     * @return
     */
    public float getTime(float time) {
        if (shutterOpen >= shutterClose) {
            return shutterOpen;
        }
        // warp the time sample by a tent filter - this helps simulates the
        // behaviour of a standard shutter as explained here:
        // "Shutter Efficiency and Temporal Sampling" by "Ian Stephenson"
        // http://www.dctsystems.co.uk/Text/shutter.pdf
        if (time < 0.5) {
            time = -1 + (float) Math.sqrt(2 * time);
        } else {
            time = 1 - (float) Math.sqrt(2 - 2 * time);
        }
        time = 0.5f * (time + 1);
        return (1 - time) * shutterOpen + time * shutterClose;
    }

    /**
     * Generate a ray passing though the specified point on the image plane.
     * Additional random variables are provided for the lens to optionally
     * compute depth-of-field or motion blur effects. Note that the camera may
     * return
     * <code>null</code> for invalid arguments or for pixels which don't project
     * to anything.
     *
     * @param x x pixel coordinate
     * @param y y pixel coordinate
     * @param imageWidth width of the image in pixels
     * @param imageHeight height of the image in pixels
     * @param lensX a random variable in [0,1) to be used for DOF sampling
     * @param lensY a random variable in [0,1) to be used for DOF sampling
     * @param time a random variable in [0,1) to be used for motion blur
     * sampling
     * @return a ray passing through the specified pixel, or <code>null</code>
     */
    public Ray getRay(float x, float y, int imageWidth, int imageHeight, double lensX, double lensY, float time) {
        Ray r = lens.getRay(x, y, imageWidth, imageHeight, lensX, lensY, time);
        if (r != null) {
            // transform from camera space to world space
            r = r.transform(c2w.sample(time));
            // renormalize to account for scale factors embeded in the transform
            r.normalize();
        }
        return r;
    }

    /**
     * Generate a ray from the origin of camera space toward the specified
     * point.
     *
     * @param p point in world space
     * @return ray from the origin of camera space to the specified point
     */
    Ray getRay(Point3 p, float time) {
        return new Ray(c2w == null ? new Point3(0, 0, 0) : c2w.sample(time).transformP(new Point3(0, 0, 0)), p);
    }

    /**
     * Returns a transformation matrix mapping camera space to world space.
     *
     * @return a transformation matrix
     */
    Matrix4 getCameraToWorld(float time) {
        return c2w == null ? Matrix4.IDENTITY : c2w.sample(time);
    }

    /**
     * Returns a transformation matrix mapping world space to camera space.
     *
     * @return a transformation matrix
     */
    Matrix4 getWorldToCamera(float time) {
        return w2c == null ? Matrix4.IDENTITY : w2c.sample(time);
    }
}