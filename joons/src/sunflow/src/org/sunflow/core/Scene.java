package org.sunflow.core;

import java.util.ArrayList;

import org.sunflow.core.display.FrameDisplay;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * Represents a entire scene, defined as a collection of instances viewed by a
 * camera.
 */
public class Scene {
    // scene storage

    private LightServer lightServer;
    private InstanceList instanceList;
    private InstanceList infiniteInstanceList;
    private Camera camera;
    private AccelerationStructure intAccel;
    private String acceltype;
    private Statistics stats;
    // baking
    private boolean bakingViewDependent;
    private Instance bakingInstance;
    private PrimitiveList bakingPrimitives;
    private AccelerationStructure bakingAccel;
    private boolean rebuildAccel;
    // image size
    private int imageWidth;
    private int imageHeight;
    // global options
    private int threads;
    private boolean lowPriority;

    /**
     * Creates an empty scene.
     */
    public Scene() {
        lightServer = new LightServer(this);
        instanceList = new InstanceList();
        infiniteInstanceList = new InstanceList();
        acceltype = "auto";
        stats = new Statistics();

        bakingViewDependent = false;
        bakingInstance = null;
        bakingPrimitives = null;
        bakingAccel = null;

        camera = null;
        imageWidth = 640;
        imageHeight = 480;
        threads = 0;
        lowPriority = true;

        rebuildAccel = true;
    }

    /**
     * Get number of allowed threads for multi-threaded operations.
     *
     * @return number of threads that can be started
     */
    public int getThreads() {
        return threads <= 0 ? Runtime.getRuntime().availableProcessors() : threads;
    }

    /**
     * Get the priority level to assign to multi-threaded operations.
     *
     * @return thread priority
     */
    public int getThreadPriority() {
        return lowPriority ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY;
    }

    /**
     * Sets the current camera (no support for multiple cameras yet).
     *
     * @param camera camera to be used as the viewpoint for the scene
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    Camera getCamera() {
        return camera;
    }

    /**
     * Update the instance lists for this scene.
     *
     * @param instances regular instances
     * @param infinite infinite instances (no bounds)
     */
    public void setInstanceLists(Instance[] instances, Instance[] infinite) {
        infiniteInstanceList = new InstanceList(infinite);
        instanceList = new InstanceList(instances);
        rebuildAccel = true;
    }

    /**
     * Update the light list for this scene.
     *
     * @param lights array of light source objects
     */
    public void setLightList(LightSource[] lights) {
        lightServer.setLights(lights);
    }

    /**
     * Enables shader overiding (set null to disable). The specified shader will
     * be used to shade all surfaces
     *
     * @param shader shader to run over all surfaces, or <code>null</code> to
     * disable overriding
     * @param photonOverride <code>true</code> to override photon scattering
     * with this shader or <code>false</code> to run the regular shaders
     */
    public void setShaderOverride(Shader shader, boolean photonOverride) {
        lightServer.setShaderOverride(shader, photonOverride);
    }

    /**
     * The provided instance will be considered for lightmap baking. If the
     * specified instance is
     * <code>null</code>, lightmap baking will be disabled and normal rendering
     * will occur.
     *
     * @param instance instance to bake
     */
    public void setBakingInstance(Instance instance) {
        bakingInstance = instance;
    }

    /**
     * Get the radiance seen through a particular pixel
     *
     * @param istate intersection state for ray tracing
     * @param rx pixel x coordinate
     * @param ry pixel y coordinate
     * @param lensU DOF sampling variable
     * @param lensV DOF sampling variable
     * @param time motion blur sampling variable
     * @param instance QMC instance seed
     * @return a shading state for the intersected primitive, or
     * <code>null</code> if nothing is seen through the specifieFd point
     */
    public ShadingState getRadiance(IntersectionState istate, float rx, float ry, double lensU, double lensV, double time, int instance, int dim, ShadingCache cache) {
        istate.numEyeRays++;
        float sceneTime = camera.getTime((float) time);
        if (bakingPrimitives == null) {
            Ray r = camera.getRay(rx, ry, imageWidth, imageHeight, lensU, lensV, sceneTime);
            return r != null ? lightServer.getRadiance(rx, ry, sceneTime, instance, dim, r, istate, cache) : null;
        } else {
            Ray r = new Ray(rx / imageWidth, ry / imageHeight, -1, 0, 0, 1);
            traceBake(r, istate);
            if (!istate.hit()) {
                return null;
            }
            ShadingState state = ShadingState.createState(istate, rx, ry, sceneTime, r, instance, dim, lightServer);
            bakingPrimitives.prepareShadingState(state);
            if (bakingViewDependent) {
                state.setRay(camera.getRay(state.getPoint(), sceneTime));
            } else {
                Point3 p = state.getPoint();
                Vector3 n = state.getNormal();
                // create a ray coming from directly above the point being
                // shaded
                Ray incoming = new Ray(p.x + n.x, p.y + n.y, p.z + n.z, -n.x, -n.y, -n.z);
                incoming.setMax(1);
                state.setRay(incoming);
            }
            lightServer.shadeBakeResult(state);
            return state;
        }
    }

    /**
     * Get scene world space bounding box.
     *
     * @return scene bounding box
     */
    public BoundingBox getBounds() {
        return instanceList.getWorldBounds(null);
    }

    public void accumulateStats(IntersectionState state) {
        stats.accumulate(state);
    }

    public void accumulateStats(ShadingCache cache) {
        stats.accumulate(cache);
    }

    void trace(Ray r, IntersectionState state) {
        // stats
        state.numRays++;
        // reset object
        state.instance = null;
        state.current = null;
        for (int i = 0; i < infiniteInstanceList.getNumPrimitives(); i++) {
            infiniteInstanceList.intersectPrimitive(r, i, state);
        }
        // reset for next accel structure
        state.current = null;
        intAccel.intersect(r, state);
    }

    Color traceShadow(Ray r, IntersectionState state) {
        state.numShadowRays++;
        trace(r, state);
        return state.hit() ? Color.WHITE : Color.BLACK;
    }

    void traceBake(Ray r, IntersectionState state) {
        // set the instance as if tracing a regular instanced object
        state.current = bakingInstance;
        // reset object
        state.instance = null;
        bakingAccel.intersect(r, state);
    }

    private void createAreaLightInstances() {
        ArrayList<Instance> infiniteAreaLights = null;
        ArrayList<Instance> areaLights = null;
        // create an area light instance from each light source if possible
        for (LightSource l : lightServer.lights) {
            Instance lightInstance = l.createInstance();
            if (lightInstance != null) {
                if (lightInstance.getBounds() == null) {
                    if (infiniteAreaLights == null) {
                        infiniteAreaLights = new ArrayList<Instance>();
                    }
                    infiniteAreaLights.add(lightInstance);
                } else {
                    if (areaLights == null) {
                        areaLights = new ArrayList<Instance>();
                    }
                    areaLights.add(lightInstance);
                }
            }
        }
        // add area light sources to the list of instances if they exist
        if (infiniteAreaLights != null && infiniteAreaLights.size() > 0) {
            infiniteInstanceList.addLightSourceInstances(infiniteAreaLights.toArray(new Instance[infiniteAreaLights.size()]));
        } else {
            infiniteInstanceList.clearLightSources();
        }
        if (areaLights != null && areaLights.size() > 0) {
            instanceList.addLightSourceInstances(areaLights.toArray(new Instance[areaLights.size()]));
        } else {
            instanceList.clearLightSources();
        }
        // FIXME: this _could_ be done incrementally to avoid top-level rebuilds
        // each frame
        rebuildAccel = true;
    }

    private void removeAreaLightInstances() {
        infiniteInstanceList.clearLightSources();
        instanceList.clearLightSources();
    }

    /**
     * Render the scene using the specified options, image sampler and display.
     *
     * @param options rendering options object
     * @param sampler image sampler
     * @param display display to send the final image to, a default display will
     * be created if <code>null</code>
     */
    public void render(Options options, ImageSampler sampler, Display display) {
        stats.reset();
        if (display == null) {
            display = new FrameDisplay();
        }

        if (bakingInstance != null) {
            UI.printDetailed(Module.SCENE, "Creating primitives for lightmapping ...");
            bakingPrimitives = bakingInstance.getBakingPrimitives();
            if (bakingPrimitives == null) {
                UI.printError(Module.SCENE, "Lightmap baking is not supported for the given instance.");
                return;
            }
            int n = bakingPrimitives.getNumPrimitives();
            UI.printInfo(Module.SCENE, "Building acceleration structure for lightmapping (%d num primitives) ...", n);
            bakingAccel = AccelerationStructureFactory.create("auto", n, true);
            bakingAccel.build(bakingPrimitives);
        } else {
            bakingPrimitives = null;
            bakingAccel = null;
        }
        bakingViewDependent = options.getBoolean("baking.viewdep", bakingViewDependent);

        if ((bakingInstance != null && bakingViewDependent && camera == null) || (bakingInstance == null && camera == null)) {
            UI.printError(Module.SCENE, "No camera found");
            return;
        }

        // read from options
        threads = options.getInt("threads", 0);
        lowPriority = options.getBoolean("threads.lowPriority", true);
        imageWidth = options.getInt("resolutionX", 640);
        imageHeight = options.getInt("resolutionY", 480);
        // limit resolution to 16k
        imageWidth = MathUtils.clamp(imageWidth, 1, 1 << 14);
        imageHeight = MathUtils.clamp(imageHeight, 1, 1 << 14);

        // prepare lights
        createAreaLightInstances();

        // get acceleration structure info
        // count scene primitives
        long numPrimitives = 0;
        for (int i = 0; i < instanceList.getNumPrimitives(); i++) {
            numPrimitives += instanceList.getNumPrimitives(i);
        }
        UI.printInfo(Module.SCENE, "Scene stats:");
        UI.printInfo(Module.SCENE, "  * Infinite instances:  %d", infiniteInstanceList.getNumPrimitives());
        UI.printInfo(Module.SCENE, "  * Instances:           %d", instanceList.getNumPrimitives());
        UI.printInfo(Module.SCENE, "  * Primitives:          %d", numPrimitives);
        String accelName = options.getString("accel", null);
        if (accelName != null) {
            rebuildAccel = rebuildAccel || !acceltype.equals(accelName);
            acceltype = accelName;
        }
        UI.printInfo(Module.SCENE, "  * Instance accel:      %s", acceltype);
        if (rebuildAccel) {
            intAccel = AccelerationStructureFactory.create(acceltype, instanceList.getNumPrimitives(), false);
            intAccel.build(instanceList);
            rebuildAccel = false;
        }
        UI.printInfo(Module.SCENE, "  * Scene bounds:        %s", getBounds());
        UI.printInfo(Module.SCENE, "  * Scene center:        %s", getBounds().getCenter());
        UI.printInfo(Module.SCENE, "  * Scene diameter:      %.2f", getBounds().getExtents().length());
        UI.printInfo(Module.SCENE, "  * Lightmap bake:       %s", bakingInstance != null ? (bakingViewDependent ? "view" : "ortho") : "off");
        if (sampler == null) {
            return;
        }
        if (!lightServer.build(options)) {
            return;
        }
        // render
        UI.printInfo(Module.SCENE, "Rendering ...");
        stats.setResolution(imageWidth, imageHeight);
        sampler.prepare(options, this, imageWidth, imageHeight);
        sampler.render(display);
        // show statistics
        stats.displayStats();
        lightServer.showStats();
        // discard area lights
        removeAreaLightInstances();
        // discard baking tesselation/accel structure
        bakingPrimitives = null;
        bakingAccel = null;
        UI.printInfo(Module.SCENE, "Done.");
    }

    /**
     * Create a photon map as prescribed by the given {@link PhotonStore}.
     *
     * @param map object that will recieve shot photons
     * @param type type of photons being shot
     * @param seed QMC seed parameter
     * @return <code>true</code> upon success
     */
    public boolean calculatePhotons(PhotonStore map, String type, int seed, Options options) {
        return lightServer.calculatePhotons(map, type, seed, options);
    }
}