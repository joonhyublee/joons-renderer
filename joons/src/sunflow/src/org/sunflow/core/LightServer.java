package org.sunflow.core;

import org.sunflow.PluginRegistry;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.QMC;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

class LightServer {
    // parent

    private Scene scene;
    // lighting
    LightSource[] lights;
    // shading override
    private Shader shaderOverride;
    private boolean shaderOverridePhotons;
    // direct illumination
    private int maxDiffuseDepth;
    private int maxReflectionDepth;
    private int maxRefractionDepth;
    // indirect illumination
    private CausticPhotonMapInterface causticPhotonMap;
    private GIEngine giEngine;
    private int photonCounter;

    LightServer(Scene scene) {
        this.scene = scene;
        lights = new LightSource[0];
        causticPhotonMap = null;

        shaderOverride = null;
        shaderOverridePhotons = false;

        maxDiffuseDepth = 1;
        maxReflectionDepth = 4;
        maxRefractionDepth = 4;

        causticPhotonMap = null;
        giEngine = null;
    }

    void setLights(LightSource[] lights) {
        this.lights = lights;
    }

    Scene getScene() {
        return scene;
    }

    void setShaderOverride(Shader shader, boolean photonOverride) {
        shaderOverride = shader;
        shaderOverridePhotons = photonOverride;
    }

    boolean build(Options options) {
        // read options
        maxDiffuseDepth = options.getInt("depths.diffuse", maxDiffuseDepth);
        maxReflectionDepth = options.getInt("depths.reflection", maxReflectionDepth);
        maxRefractionDepth = options.getInt("depths.refraction", maxRefractionDepth);
        String giEngineType = options.getString("gi.engine", null);
        giEngine = PluginRegistry.giEnginePlugins.createObject(giEngineType);
        String caustics = options.getString("caustics", null);
        causticPhotonMap = PluginRegistry.causticPhotonMapPlugins.createObject(caustics);

        // validate options
        maxDiffuseDepth = Math.max(0, maxDiffuseDepth);
        maxReflectionDepth = Math.max(0, maxReflectionDepth);
        maxRefractionDepth = Math.max(0, maxRefractionDepth);

        Timer t = new Timer();
        t.start();
        // count total number of light samples
        int numLightSamples = 0;
        for (int i = 0; i < lights.length; i++) {
            numLightSamples += lights[i].getNumSamples();
        }
        // initialize gi engine
        if (giEngine != null) {
            if (!giEngine.init(options, scene)) {
                return false;
            }
        }

        if (!calculatePhotons(causticPhotonMap, "caustic", 0, options)) {
            return false;
        }
        t.end();
        UI.printInfo(Module.LIGHT, "Light Server stats:");
        UI.printInfo(Module.LIGHT, "  * Light sources found: %d", lights.length);
        UI.printInfo(Module.LIGHT, "  * Light samples:       %d", numLightSamples);
        UI.printInfo(Module.LIGHT, "  * Max raytrace depth:");
        UI.printInfo(Module.LIGHT, "      - Diffuse          %d", maxDiffuseDepth);
        UI.printInfo(Module.LIGHT, "      - Reflection       %d", maxReflectionDepth);
        UI.printInfo(Module.LIGHT, "      - Refraction       %d", maxRefractionDepth);
        UI.printInfo(Module.LIGHT, "  * GI engine            %s", giEngineType == null ? "none" : giEngineType);
        UI.printInfo(Module.LIGHT, "  * Caustics:            %s", caustics == null ? "none" : caustics);
        UI.printInfo(Module.LIGHT, "  * Shader override:     %b", shaderOverride);
        UI.printInfo(Module.LIGHT, "  * Photon override:     %b", shaderOverridePhotons);
        UI.printInfo(Module.LIGHT, "  * Build time:          %s", t.toString());
        return true;
    }

    void showStats() {
    }

    boolean calculatePhotons(final PhotonStore map, String type, final int seed, Options options) {
        if (map == null) {
            return true;
        }
        if (lights.length == 0) {
            UI.printError(Module.LIGHT, "Unable to trace %s photons, no lights in scene", type);
            return false;
        }
        final float[] histogram = new float[lights.length];
        histogram[0] = lights[0].getPower();
        for (int i = 1; i < lights.length; i++) {
            histogram[i] = histogram[i - 1] + lights[i].getPower();
        }
        UI.printInfo(Module.LIGHT, "Tracing %s photons ...", type);
        map.prepare(options, scene.getBounds());
        int numEmittedPhotons = map.numEmit();
        if (numEmittedPhotons <= 0 || histogram[histogram.length - 1] <= 0) {
            UI.printError(Module.LIGHT, "Photon mapping enabled, but no %s photons to emit", type);
            return false;
        }
        UI.taskStart("Tracing " + type + " photons", 0, numEmittedPhotons);
        Thread[] photonThreads = new Thread[scene.getThreads()];
        final float scale = 1.0f / numEmittedPhotons;
        int delta = numEmittedPhotons / photonThreads.length;
        photonCounter = 0;
        Timer photonTimer = new Timer();
        photonTimer.start();
        for (int i = 0; i < photonThreads.length; i++) {
            final int threadID = i;
            final int start = threadID * delta;
            final int end = (threadID == (photonThreads.length - 1)) ? numEmittedPhotons : (threadID + 1) * delta;
            photonThreads[i] = new Thread(new Runnable() {
                public void run() {
                    IntersectionState istate = new IntersectionState();
                    for (int i = start; i < end; i++) {
                        synchronized (LightServer.this) {
                            UI.taskUpdate(photonCounter);
                            photonCounter++;
                            if (UI.taskCanceled()) {
                                return;
                            }
                        }

                        int qmcI = i + seed;

                        double rand = QMC.halton(0, qmcI) * histogram[histogram.length - 1];
                        int j = 0;
                        while (rand >= histogram[j] && j < histogram.length) {
                            j++;
                        }
                        // make sure we didn't pick a zero-probability light
                        if (j == histogram.length) {
                            continue;
                        }

                        double randX1 = (j == 0) ? rand / histogram[0] : (rand - histogram[j]) / (histogram[j] - histogram[j - 1]);
                        double randY1 = QMC.halton(1, qmcI);
                        double randX2 = QMC.halton(2, qmcI);
                        double randY2 = QMC.halton(3, qmcI);
                        Point3 pt = new Point3();
                        Vector3 dir = new Vector3();
                        Color power = new Color();
                        lights[j].getPhoton(randX1, randY1, randX2, randY2, pt, dir, power);
                        power.mul(scale);
                        Ray r = new Ray(pt, dir);
                        scene.trace(r, istate);
                        if (istate.hit()) {
                            shadePhoton(ShadingState.createPhotonState(r, istate, qmcI, map, LightServer.this), power);
                        }
                    }
                }
            });
            photonThreads[i].setPriority(scene.getThreadPriority());
            photonThreads[i].start();
        }
        for (int i = 0; i < photonThreads.length; i++) {
            try {
                photonThreads[i].join();
            } catch (InterruptedException e) {
                UI.printError(Module.LIGHT, "Photon thread %d of %d was interrupted", i + 1, photonThreads.length);
                return false;
            }
        }
        if (UI.taskCanceled()) {
            UI.taskStop(); // shut down task cleanly
            return false;
        }
        photonTimer.end();
        UI.taskStop();
        UI.printInfo(Module.LIGHT, "Tracing time for %s photons: %s", type, photonTimer.toString());
        map.init();
        return true;
    }

    void shadePhoton(ShadingState state, Color power) {
        state.getInstance().prepareShadingState(state);
        Shader shader = getPhotonShader(state);
        // scatter photon
        if (shader != null) {
            shader.scatterPhoton(state, power);
        }
    }

    void traceDiffusePhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getDiffuseDepth() >= maxDiffuseDepth) {
            return;
        }
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createDiffuseBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    void traceReflectionPhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getReflectionDepth() >= maxReflectionDepth) {
            return;
        }
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createReflectionBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    void traceRefractionPhoton(ShadingState previous, Ray r, Color power) {
        if (previous.getRefractionDepth() >= maxRefractionDepth) {
            return;
        }
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        if (previous.getIntersectionState().hit()) {
            // create a new shading context
            ShadingState state = ShadingState.createRefractionBounceState(previous, r, 0);
            shadePhoton(state, power);
        }
    }

    private Shader getShader(ShadingState state) {
        return shaderOverride != null ? shaderOverride : state.getShader();
    }

    private Shader getPhotonShader(ShadingState state) {
        return (shaderOverride != null && shaderOverridePhotons) ? shaderOverride : state.getShader();

    }

    ShadingState getRadiance(float rx, float ry, float time, int i, int d, Ray r, IntersectionState istate, ShadingCache cache) {
        // set this value once - will stay constant for the entire ray-tree
        istate.time = time;
        scene.trace(r, istate);
        if (istate.hit()) {
            ShadingState state = ShadingState.createState(istate, rx, ry, time, r, i, d, this);
            state.getInstance().prepareShadingState(state);
            Shader shader = getShader(state);
            if (shader == null) {
                state.setResult(Color.BLACK);
                return state;
            }
            if (cache != null) {
                Color c = cache.lookup(state, shader);
                if (c != null) {
                    state.setResult(c);
                    return state;
                }
            }
            state.setResult(shader.getRadiance(state));
            if (cache != null) {
                cache.add(state, shader, state.getResult());
            }
            checkNanInf(state.getResult());
            return state;
        } else {
            return null;
        }
    }

    private static final void checkNanInf(Color c) {
        if (c.isNan()) {
            UI.printWarning(Module.LIGHT, "NaN shading sample!");
        } else if (c.isInf()) {
            UI.printWarning(Module.LIGHT, "Inf shading sample!");
        }
    }

    void shadeBakeResult(ShadingState state) {
        Shader shader = getShader(state);
        if (shader != null) {
            state.setResult(shader.getRadiance(state));
        } else {
            state.setResult(Color.BLACK);
        }
    }

    Color shadeHit(ShadingState state) {
        state.getInstance().prepareShadingState(state);
        Shader shader = getShader(state);
        return (shader != null) ? shader.getRadiance(state) : Color.BLACK;
    }

    Color traceGlossy(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getReflectionDepth() >= maxReflectionDepth || previous.getDiffuseDepth() > 0) {
            return Color.BLACK;
        }
        IntersectionState istate = previous.getIntersectionState();
        istate.numGlossyRays++;
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createGlossyBounceState(previous, r, i)) : Color.BLACK;
    }

    Color traceReflection(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getReflectionDepth() >= maxReflectionDepth || previous.getDiffuseDepth() > 0) {
            return Color.BLACK;
        }
        IntersectionState istate = previous.getIntersectionState();
        istate.numReflectionRays++;
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createReflectionBounceState(previous, r, i)) : Color.BLACK;
    }

    Color traceRefraction(ShadingState previous, Ray r, int i) {
        // limit path depth and disable caustic paths
        if (previous.getRefractionDepth() >= maxRefractionDepth || previous.getDiffuseDepth() > 0) {
            return Color.BLACK;
        }
        IntersectionState istate = previous.getIntersectionState();
        istate.numRefractionRays++;
        scene.trace(r, istate);
        return istate.hit() ? shadeHit(ShadingState.createRefractionBounceState(previous, r, i)) : Color.BLACK;
    }

    ShadingState traceFinalGather(ShadingState previous, Ray r, int i) {
        if (previous.getDiffuseDepth() >= maxDiffuseDepth) {
            return null;
        }
        IntersectionState istate = previous.getIntersectionState();
        scene.trace(r, istate);
        return istate.hit() ? ShadingState.createFinalGatherState(previous, r, i) : null;
    }

    Color getGlobalRadiance(ShadingState state) {
        if (giEngine == null) {
            return Color.BLACK;
        }
        return giEngine.getGlobalRadiance(state);
    }

    Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        // no gi engine, or we have already exceeded number of available bounces
        if (giEngine == null || state.getDiffuseDepth() >= maxDiffuseDepth) {
            return Color.BLACK;
        }
        return giEngine.getIrradiance(state, diffuseReflectance);
    }

    void initLightSamples(ShadingState state) {
        for (LightSource l : lights) {
            l.getSamples(state);
        }
    }

    void initCausticSamples(ShadingState state) {
        if (causticPhotonMap != null) {
            causticPhotonMap.getSamples(state);
        }
    }
}