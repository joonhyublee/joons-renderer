package org.sunflow;

import java.util.ArrayList;
import java.util.Locale;

import org.sunflow.core.Camera;
import org.sunflow.core.Geometry;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSource;
import org.sunflow.core.Modifier;
import org.sunflow.core.Options;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.RenderObject;
import org.sunflow.core.Scene;
import org.sunflow.core.Shader;
import org.sunflow.core.Tesselatable;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FastHashMap;

final class RenderObjectMap {

    private FastHashMap<String, RenderObjectHandle> renderObjects;
    private boolean rebuildInstanceList;
    private boolean rebuildLightList;

    private enum RenderObjectType {

        UNKNOWN, SHADER, MODIFIER, GEOMETRY, INSTANCE, LIGHT, CAMERA, OPTIONS
    }

    RenderObjectMap() {
        renderObjects = new FastHashMap<String, RenderObjectHandle>();
        rebuildInstanceList = rebuildLightList = false;
    }

    final boolean has(String name) {
        return renderObjects.containsKey(name);
    }

    final void remove(String name) {
        RenderObjectHandle obj = renderObjects.get(name);
        if (obj == null) {
            UI.printWarning(Module.API, "Unable to remove \"%s\" - object was not defined yet");
            return;
        }
        UI.printDetailed(Module.API, "Removing object \"%s\"", name);
        renderObjects.remove(name);
        // scan through all objects to make sure we don't have any
        // references to the old object still around
        switch (obj.type) {
            case SHADER:
                Shader s = obj.getShader();
                for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                    Instance i = e.getValue().getInstance();
                    if (i != null) {
                        UI.printWarning(Module.API, "Removing shader \"%s\" from instance \"%s\"", name, e.getKey());
                        i.removeShader(s);
                    }
                }
                break;
            case MODIFIER:
                Modifier m = obj.getModifier();
                for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                    Instance i = e.getValue().getInstance();
                    if (i != null) {
                        UI.printWarning(Module.API, "Removing modifier \"%s\" from instance \"%s\"", name, e.getKey());
                        i.removeModifier(m);
                    }
                }
                break;
            case GEOMETRY: {
                Geometry g = obj.getGeometry();
                for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                    Instance i = e.getValue().getInstance();
                    if (i != null && i.hasGeometry(g)) {
                        UI.printWarning(Module.API, "Removing instance \"%s\" because it referenced geometry \"%s\"", e.getKey(), name);
                        remove(e.getKey());
                    }
                }
                break;
            }
            case INSTANCE:
                rebuildInstanceList = true;
                break;
            case LIGHT:
                rebuildLightList = true;
                break;
            default:
                // no dependencies
                break;
        }
    }

    final boolean update(String name, ParameterList pl, SunflowAPI api) {
        RenderObjectHandle obj = renderObjects.get(name);
        boolean success;
        if (obj == null) {
            UI.printError(Module.API, "Unable to update \"%s\" - object was not defined yet", name);
            success = false;
        } else {
            UI.printDetailed(Module.API, "Updating %s object \"%s\"", obj.typeName(), name);
            success = obj.update(pl, api);
            if (!success) {
                UI.printError(Module.API, "Unable to update \"%s\" - removing", name);
                remove(name);
            } else {
                switch (obj.type) {
                    case GEOMETRY:
                    case INSTANCE:
                        rebuildInstanceList = true;
                        break;
                    case LIGHT:
                        rebuildLightList = true;
                        break;
                    default:
                        break;
                }
            }
        }
        return success;
    }

    final void updateScene(Scene scene) {
        if (rebuildInstanceList) {
            UI.printInfo(Module.API, "Building scene instance list for rendering ...");
            int numInfinite = 0, numInstance = 0;
            for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                Instance i = e.getValue().getInstance();
                if (i != null) {
                    i.updateBounds();
                    if (i.getBounds() == null) {
                        numInfinite++;
                    } else if (!i.getBounds().isEmpty()) {
                        numInstance++;
                    } else {
                        UI.printWarning(Module.API, "Ignoring empty instance: \"%s\"", e.getKey());
                    }
                }
            }
            Instance[] infinite = new Instance[numInfinite];
            Instance[] instance = new Instance[numInstance];
            numInfinite = numInstance = 0;
            for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                Instance i = e.getValue().getInstance();
                if (i != null) {
                    if (i.getBounds() == null) {
                        infinite[numInfinite] = i;
                        numInfinite++;
                    } else if (!i.getBounds().isEmpty()) {
                        instance[numInstance] = i;
                        numInstance++;
                    }
                }
            }
            scene.setInstanceLists(instance, infinite);
            rebuildInstanceList = false;
        }
        if (rebuildLightList) {
            UI.printInfo(Module.API, "Building scene light list for rendering ...");
            ArrayList<LightSource> lightList = new ArrayList<LightSource>();
            for (FastHashMap.Entry<String, RenderObjectHandle> e : renderObjects) {
                LightSource light = e.getValue().getLight();
                if (light != null) {
                    lightList.add(light);
                }

            }
            scene.setLightList(lightList.toArray(new LightSource[lightList.size()]));
            rebuildLightList = false;
        }
    }

    final void put(String name, Shader shader) {
        renderObjects.put(name, new RenderObjectHandle(shader));
    }

    final void put(String name, Modifier modifier) {
        renderObjects.put(name, new RenderObjectHandle(modifier));
    }

    final void put(String name, PrimitiveList primitives) {
        renderObjects.put(name, new RenderObjectHandle(primitives));
    }

    final void put(String name, Tesselatable tesselatable) {
        renderObjects.put(name, new RenderObjectHandle(tesselatable));
    }

    final void put(String name, Instance instance) {
        renderObjects.put(name, new RenderObjectHandle(instance));
    }

    final void put(String name, LightSource light) {
        renderObjects.put(name, new RenderObjectHandle(light));
    }

    final void put(String name, Camera camera) {
        renderObjects.put(name, new RenderObjectHandle(camera));
    }

    final void put(String name, Options options) {
        renderObjects.put(name, new RenderObjectHandle(options));
    }

    final Geometry lookupGeometry(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getGeometry();
    }

    final Instance lookupInstance(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getInstance();
    }

    final Camera lookupCamera(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getCamera();
    }

    final Options lookupOptions(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getOptions();
    }

    final Shader lookupShader(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getShader();
    }

    final Modifier lookupModifier(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getModifier();
    }

    final LightSource lookupLight(String name) {
        if (name == null) {
            return null;
        }
        RenderObjectHandle handle = renderObjects.get(name);
        return (handle == null) ? null : handle.getLight();
    }

    private static final class RenderObjectHandle {

        private final RenderObject obj;
        private final RenderObjectType type;

        private RenderObjectHandle(Shader shader) {
            obj = shader;
            type = RenderObjectType.SHADER;
        }

        private RenderObjectHandle(Modifier modifier) {
            obj = modifier;
            type = RenderObjectType.MODIFIER;
        }

        private RenderObjectHandle(Tesselatable tesselatable) {
            obj = new Geometry(tesselatable);
            type = RenderObjectType.GEOMETRY;
        }

        private RenderObjectHandle(PrimitiveList prims) {
            obj = new Geometry(prims);
            type = RenderObjectType.GEOMETRY;
        }

        private RenderObjectHandle(Instance instance) {
            obj = instance;
            type = RenderObjectType.INSTANCE;
        }

        private RenderObjectHandle(LightSource light) {
            obj = light;
            type = RenderObjectType.LIGHT;
        }

        private RenderObjectHandle(Camera camera) {
            obj = camera;
            type = RenderObjectType.CAMERA;
        }

        private RenderObjectHandle(Options options) {
            obj = options;
            type = RenderObjectType.OPTIONS;
        }

        private boolean update(ParameterList pl, SunflowAPI api) {
            return obj.update(pl, api);
        }

        private String typeName() {
            return type.name().toLowerCase(Locale.ENGLISH);
        }

        private Shader getShader() {
            return (type == RenderObjectType.SHADER) ? (Shader) obj : null;
        }

        private Modifier getModifier() {
            return (type == RenderObjectType.MODIFIER) ? (Modifier) obj : null;
        }

        private Geometry getGeometry() {
            return (type == RenderObjectType.GEOMETRY) ? (Geometry) obj : null;
        }

        private Instance getInstance() {
            return (type == RenderObjectType.INSTANCE) ? (Instance) obj : null;
        }

        private LightSource getLight() {
            return (type == RenderObjectType.LIGHT) ? (LightSource) obj : null;
        }

        private Camera getCamera() {
            return (type == RenderObjectType.CAMERA) ? (Camera) obj : null;
        }

        private Options getOptions() {
            return (type == RenderObjectType.OPTIONS) ? (Options) obj : null;
        }
    }
}