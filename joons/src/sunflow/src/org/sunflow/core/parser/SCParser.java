package org.sunflow.core.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.PluginRegistry;
import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.image.Color;
import org.sunflow.image.ColorFactory;
import org.sunflow.image.ColorFactory.ColorSpecificationException;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Parser;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;

/**
 * This class provides a static method for loading files in the Sunflow scene
 * file format.
 */
public class SCParser implements SceneParser {
  
    private static int instanceCounter = 0;
    private int instanceNumber;
    private Parser p;
    private int numLightSamples;
    // used to generate unique names inside this parser
    private HashMap<String, Integer> objectNames;
    
    public SCParser() {
        objectNames = new HashMap<String, Integer>();
        instanceCounter++;
        instanceNumber = instanceCounter;
    }

    private String generateUniqueName(String prefix) {
        // generate a unique name for this class:
        int index = 1;
        Integer value = objectNames.get(prefix);
        if (value != null) {
            index = value;
            objectNames.put(prefix, index + 1);
        } else {
            objectNames.put(prefix, index + 1);
        }
        return String.format("@sc_%d::%s_%d", instanceNumber, prefix, index);
    }

    @Override
    public boolean parse(String filename, SunflowAPIInterface api) {
        String localDir = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
        numLightSamples = 1;
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            p = new Parser(filename);
            while (true) {
                String token = p.getNextToken();
                if (token == null) {
                    break;
                }
                if (token.equals("image")) {
                    UI.printInfo(Module.API, "Reading image settings ...");
                    parseImageBlock(api);
                } else if (token.equals(BACKGROUND)) {
                    UI.printInfo(Module.API, "Reading background ...");
                    parseBackgroundBlock(api);
                } else if (token.equals("accel")) {
                    UI.printInfo(Module.API, "Reading accelerator type ...");
                    p.getNextToken();
                    UI.printWarning(Module.API, "Setting accelerator type is not recommended - ignoring");
                } else if (token.equals(FILTER)) {
                    UI.printInfo(Module.API, "Reading image filter type ...");
                    parseFilter(api);
                } else if (token.equals("bucket")) {
                    UI.printInfo(Module.API, "Reading bucket settings ...");
                    api.parameter("bucket.size", p.getNextInt());
                    api.parameter("bucket.order", p.getNextToken());
                    api.options(SunflowAPI.DEFAULT_OPTIONS);
                } else if (token.equals("photons")) {
                    UI.printInfo(Module.API, "Reading photon settings ...");
                    parsePhotonBlock(api);
                } else if (token.equals("gi")) {
                    UI.printInfo(Module.API, "Reading global illumination settings ...");
                    parseGIBlock(api);
                } else if (token.equals("lightserver")) {
                    UI.printInfo(Module.API, "Reading light server settings ...");
                    parseLightserverBlock(api);
                } else if (token.equals("trace-depths")) {
                    UI.printInfo(Module.API, "Reading trace depths ...");
                    parseTraceBlock(api);
                } else if (token.equals("camera")) {
                    parseCamera(api);
                } else if (token.equals(SHADER)) {
                    if (!parseShader(api)) {
                        return false;
                    }
                } else if (token.equals(MODIFIER)) {
                    if (!parseModifier(api)) {
                        return false;
                    }
                } else if (token.equals("override")) {
                    api.parameter("override.shader", p.getNextToken());
                    api.parameter("override.photons", p.getNextBoolean());
                    api.options(SunflowAPI.DEFAULT_OPTIONS);
                } else if (token.equals("object")) {
                    parseObjectBlock(api);
                } else if (token.equals("instance")) {
                    parseInstanceBlock(api);
                } else if (token.equals("light")) {
                    parseLightBlock(api);
                } else if (token.equals("texturepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute()) {
                        path = localDir + File.separator + path;
                    }
                    api.searchpath(TEXTURE, path);
                } else if (token.equals("includepath")) {
                    String path = p.getNextToken();
                    if (!new File(path).isAbsolute()) {
                        path = localDir + File.separator + path;
                    }
                    api.searchpath("include", path);
                } else if (token.equals("include")) {
                    String file = p.getNextToken();
                    UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                    api.include(file);
                } else {
                    UI.printWarning(Module.API, "Unrecognized token %s", token);
                }
            }
            p.close();
        } catch (ParserException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            Logger.getLogger(SCParser.class.getName()).log(Level.SEVERE, null, e);
            return false;
        } catch (FileNotFoundException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (IOException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        } catch (ColorSpecificationException e) {
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        }
        timer.end();
        UI.printInfo(Module.API, "Done parsing.");
        UI.printInfo(Module.API, "Parsing time: %s", timer.toString());
        return true;
    }

    private void parseImageBlock(SunflowAPIInterface api) throws IOException, ParserException {
        p.checkNextToken("{");
        if (p.peekNextToken("resolution")) {
            api.parameter("resolutionX", p.getNextInt());
            api.parameter("resolutionY", p.getNextInt());
        }
        if (p.peekNextToken("sampler")) {
            api.parameter("sampler", p.getNextToken());
        }
        if (p.peekNextToken("aa")) {
            api.parameter("aa.min", p.getNextInt());
            api.parameter("aa.max", p.getNextInt());
        }
        if (p.peekNextToken(SAMPLES)) {
            api.parameter("aa.samples", p.getNextInt());
        }
        if (p.peekNextToken("contrast")) {
            api.parameter("aa.contrast", p.getNextFloat());
        }
        if (p.peekNextToken(FILTER)) {
            api.parameter(FILTER, p.getNextToken());
        }
        if (p.peekNextToken("jitter")) {
            api.parameter("aa.jitter", p.getNextBoolean());
        }
        if (p.peekNextToken("show-aa")) {
            UI.printWarning(Module.API, "Deprecated: show-aa ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("cache")) {
            api.parameter("aa.cache", p.getNextBoolean());
        }
        if (p.peekNextToken("output")) {
            UI.printWarning(Module.API, "Deprecated: output statement ignored");
            p.getNextToken();
        }
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        p.checkNextToken("}");
    }

    private void parseBackgroundBlock(SunflowAPIInterface api) throws IOException, ParserException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken(COLOR);
        api.parameter(COLOR, null, parseColor().getRGB());
        api.shader("background.shader", "constant");
        api.geometry(BACKGROUND, BACKGROUND);
        api.parameter(SHADERS, "background.shader");
        api.instance("background.instance", BACKGROUND);
        p.checkNextToken("}");
    }

    private void parseFilter(SunflowAPIInterface api) throws IOException, ParserException {
        UI.printWarning(Module.API, "Deprecated keyword \"filter\" - set this option in the image block");
        String name = p.getNextToken();
        api.parameter(FILTER, name);
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        boolean hasSizeParams = name.equals("box") || name.equals("gaussian") || name.equals("blackman-harris") || name.equals("sinc") || name.equals("triangle");
        if (hasSizeParams) {
            p.getNextFloat();
            p.getNextFloat();
        }
    }

    private void parsePhotonBlock(SunflowAPIInterface api) throws ParserException, IOException {
        int numEmit = 0;
        boolean globalEmit = false;
        p.checkNextToken("{");
        if (p.peekNextToken(EMIT)) {
            UI.printWarning(Module.API, "Shared photon emit values are deprectated - specify number of photons to emit per map");
            numEmit = p.getNextInt();
            globalEmit = true;
        }
        if (p.peekNextToken("global")) {
            UI.printWarning(Module.API, "Global photon map setting belonds inside the gi block - ignoring");
            if (!globalEmit) {
                p.getNextInt();
            }
            p.getNextToken();
            p.getNextInt();
            p.getNextFloat();
        }
        p.checkNextToken("caustics");
        if (!globalEmit) {
            numEmit = p.getNextInt();
        }
        api.parameter("caustics.emit", numEmit);
        api.parameter("caustics", p.getNextToken());
        api.parameter("caustics.gather", p.getNextInt());
        api.parameter("caustics.radius", p.getNextFloat());
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        p.checkNextToken("}");
    }

    private void parseGIBlock(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken(TYPE);
        if (p.peekNextToken("irr-cache")) {
            api.parameter(GI_ENGINE, "irr-cache");
            p.checkNextToken(SAMPLES);
            api.parameter("gi.irr-cache.samples", p.getNextInt());
            p.checkNextToken("tolerance");
            api.parameter("gi.irr-cache.tolerance", p.getNextFloat());
            p.checkNextToken("spacing");
            api.parameter("gi.irr-cache.min_spacing", p.getNextFloat());
            api.parameter("gi.irr-cache.max_spacing", p.getNextFloat());
            // parse global photon map info
            if (p.peekNextToken("global")) {
                api.parameter("gi.irr-cache.gmap.emit", p.getNextInt());
                api.parameter("gi.irr-cache.gmap", p.getNextToken());
                api.parameter("gi.irr-cache.gmap.gather", p.getNextInt());
                api.parameter("gi.irr-cache.gmap.radius", p.getNextFloat());
            }
        } else if (p.peekNextToken("path")) {
            api.parameter(GI_ENGINE, "path");
            p.checkNextToken(SAMPLES);
            api.parameter("gi.path.samples", p.getNextInt());
            if (p.peekNextToken("bounces")) {
                UI.printWarning(Module.API, "Deprecated setting: bounces - use diffuse trace depth instead");
                p.getNextInt();
            }
        } else if (p.peekNextToken("fake")) {
            api.parameter(GI_ENGINE, "fake");
            p.checkNextToken("up");
            api.parameter("gi.fake.up", parseVector());
            p.checkNextToken("sky");
            api.parameter("gi.fake.sky", null, parseColor().getRGB());
            p.checkNextToken("ground");
            api.parameter("gi.fake.ground", null, parseColor().getRGB());
        } else if (p.peekNextToken("igi")) {
            api.parameter(GI_ENGINE, "igi");
            p.checkNextToken(SAMPLES);
            api.parameter("gi.igi.samples", p.getNextInt());
            p.checkNextToken("sets");
            api.parameter("gi.igi.sets", p.getNextInt());
            if (!p.peekNextToken("b")) {
                p.checkNextToken("c");
            }
            api.parameter("gi.igi.c", p.getNextFloat());
            p.checkNextToken("bias-samples");
            api.parameter("gi.igi.bias_samples", p.getNextInt());
        } else if (p.peekNextToken("ambocc")) {
            api.parameter(GI_ENGINE, "ambocc");
            p.checkNextToken("bright");
            api.parameter("gi.ambocc.bright", null, parseColor().getRGB());
            p.checkNextToken("dark");
            api.parameter("gi.ambocc.dark", null, parseColor().getRGB());
            p.checkNextToken(SAMPLES);
            api.parameter("gi.ambocc.samples", p.getNextInt());
            if (p.peekNextToken("maxdist")) {
                api.parameter("gi.ambocc.maxdist", p.getNextFloat());
            }
        } else if (p.peekNextToken(NONE) || p.peekNextToken("null")) {
            // disable GI
            api.parameter(GI_ENGINE, NONE);
        } else {
            UI.printWarning(Module.API, "Unrecognized gi engine type \"%s\" - ignoring", p.getNextToken());
        }
        api.options(SunflowAPI.DEFAULT_OPTIONS);
        p.checkNextToken("}");
    }

    private void parseLightserverBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        if (p.peekNextToken("shadows")) {
            UI.printWarning(Module.API, "Deprecated: shadows setting ignored");
            p.getNextBoolean();
        }
        if (p.peekNextToken("direct-samples")) {
            UI.printWarning(Module.API, "Deprecated: use samples keyword in area light definitions");
            numLightSamples = p.getNextInt();
        }
        if (p.peekNextToken("glossy-samples")) {
            UI.printWarning(Module.API, "Deprecated: use samples keyword in glossy shader definitions");
            p.getNextInt();
        }
        if (p.peekNextToken("max-depth")) {
            UI.printWarning(Module.API, "Deprecated: max-depth setting - use trace-depths block instead");
            int d = p.getNextInt();
            api.parameter("depths.diffuse", 1);
            api.parameter("depths.reflection", d - 1);
            api.parameter("depths.refraction", 0);
            api.options(SunflowAPI.DEFAULT_OPTIONS);
        }
        if (p.peekNextToken("global")) {
            UI.printWarning(Module.API, "Deprecated: global settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextInt();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("caustics")) {
            UI.printWarning(Module.API, "Deprecated: caustics settings ignored - use photons block instead");
            p.getNextBoolean();
            p.getNextInt();
            p.getNextFloat();
            p.getNextInt();
            p.getNextFloat();
        }
        if (p.peekNextToken("irr-cache")) {
            UI.printWarning(Module.API, "Deprecated: irradiance cache settings ignored - use gi block instead");
            p.getNextInt();
            p.getNextFloat();
            p.getNextFloat();
            p.getNextFloat();
        }
        p.checkNextToken("}");
    }

    private void parseTraceBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        if (p.peekNextToken(DIFF)) {
            api.parameter("depths.diffuse", p.getNextInt());
        }
        if (p.peekNextToken(REFL)) {
            api.parameter("depths.reflection", p.getNextInt());
        }
        if (p.peekNextToken("refr")) {
            api.parameter("depths.refraction", p.getNextInt());
        }
        p.checkNextToken("}");
        api.options(SunflowAPI.DEFAULT_OPTIONS);
    }

    private void parseCamera(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken(TYPE);
        String type = p.getNextToken();
        UI.printInfo(Module.API, "Reading %s camera ...", type);
        if (p.peekNextToken("shutter")) {
            api.parameter("shutter.open", p.getNextFloat());
            api.parameter("shutter.close", p.getNextFloat());
        }
        parseCameraTransform(api);
        String name = generateUniqueName("camera");
        if (type.equals("pinhole")) {
            p.checkNextToken(FOV);
            api.parameter(FOV, p.getNextFloat());
            p.checkNextToken(ASPECT);
            api.parameter(ASPECT, p.getNextFloat());
            if (p.peekNextToken("shift")) {
                api.parameter("shift.x", p.getNextFloat());
                api.parameter("shift.y", p.getNextFloat());
            }
            api.camera(name, "pinhole");
        } else if (type.equals("thinlens")) {
            p.checkNextToken(FOV);
            api.parameter(FOV, p.getNextFloat());
            p.checkNextToken(ASPECT);
            api.parameter(ASPECT, p.getNextFloat());
            if (p.peekNextToken("shift")) {
                api.parameter("shift.x", p.getNextFloat());
                api.parameter("shift.y", p.getNextFloat());
            }
            p.checkNextToken("fdist");
            api.parameter("focus.distance", p.getNextFloat());
            p.checkNextToken("lensr");
            api.parameter("lens.radius", p.getNextFloat());
            if (p.peekNextToken("sides")) {
                api.parameter("lens.sides", p.getNextInt());
            }
            if (p.peekNextToken("rotation")) {
                api.parameter("lens.rotation", p.getNextFloat());
            }
            api.camera(name, "thinlens");
        } else if (type.equals("spherical")) {
            // no extra arguments
            api.camera(name, "spherical");
        } else if (type.equals("fisheye")) {
            // no extra arguments
            api.camera(name, "fisheye");
        } else {
            UI.printWarning(Module.API, "Unrecognized camera type: %s", p.getNextToken());
            p.checkNextToken("}");
            return;
        }
        p.checkNextToken("}");
        if (name != null) {
            api.parameter("camera", name);
            api.options(SunflowAPI.DEFAULT_OPTIONS);
        }
    }

    private void parseCameraTransform(SunflowAPIInterface api) throws ParserException, IOException {
        if (p.peekNextToken("steps")) {
            // motion blur camera
            int n = p.getNextInt();
            api.parameter("transform.steps", n);
            // parse time extents
            p.checkNextToken("times");
            float t0 = p.getNextFloat();
            float t1 = p.getNextFloat();
            api.parameter("transform.times", "float", NONE, new float[]{t0,
                t1});
            for (int i = 0; i < n; i++) {
                parseCameraMatrix(i, api);
            }
        } else {
            parseCameraMatrix(-1, api);
        }
    }

    private void parseCameraMatrix(int index, SunflowAPIInterface api) throws IOException, ParserException {
        String offset = index < 0 ? "" : String.format("[%d]", index);
        if (p.peekNextToken(TRANSFORM)) {
            // advanced camera
            api.parameter(String.format("transform%s", offset), parseMatrix());
        } else {
            if (index >= 0) {
                p.checkNextToken("{");
            }
            // regular camera specification
            p.checkNextToken("eye");
            Point3 eye = parsePoint();
            p.checkNextToken("target");
            Point3 target = parsePoint();
            p.checkNextToken("up");
            Vector3 up = parseVector();
            api.parameter(String.format("transform%s", offset), Matrix4.lookAt(eye, target, up));
            if (index >= 0) {
                p.checkNextToken("}");
            }
        }
    }

    private boolean parseShader(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken(NAME);
        String name = p.getNextToken();
        UI.printInfo(Module.API, "Reading shader: %s ...", name);
        p.checkNextToken(TYPE);
        if (p.peekNextToken(DIFFUSE)) {
            if (p.peekNextToken(DIFF)) {
                api.parameter(DIFFUSE, null, parseColor().getRGB());
                api.shader(name, DIFFUSE);
            } else if (p.peekNextToken(TEXTURE)) {
                api.parameter(TEXTURE, p.getNextToken());
                api.shader(name, "textured_diffuse");
            } else {
                UI.printWarning(Module.API, "Unrecognized option in diffuse shader block: %s", p.getNextToken());
            }
        } else if (p.peekNextToken("phong")) {
            String tex = null;
            if (p.peekNextToken(TEXTURE)) {
                api.parameter(TEXTURE, tex = p.getNextToken());
            } else {
                p.checkNextToken(DIFF);
                api.parameter(DIFFUSE, null, parseColor().getRGB());
            }
            p.checkNextToken("spec");
            api.parameter("specular", null, parseColor().getRGB());
            api.parameter(POWER, p.getNextFloat());
            if (p.peekNextToken(SAMPLES)) {
                api.parameter(SAMPLES, p.getNextInt());
            }
            if (tex != null) {
                api.shader(name, "textured_phong");
            } else {
                api.shader(name, "phong");
            }
        } else if (p.peekNextToken("amb-occ") || p.peekNextToken("amb-occ2")) {
            String tex = null;
            if (p.peekNextToken(DIFF) || p.peekNextToken("bright")) {
                api.parameter("bright", null, parseColor().getRGB());
            } else if (p.peekNextToken(TEXTURE)) {
                api.parameter(TEXTURE, tex = p.getNextToken());
            }
            if (p.peekNextToken("dark")) {
                api.parameter("dark", null, parseColor().getRGB());
                p.checkNextToken(SAMPLES);
                api.parameter(SAMPLES, p.getNextInt());
                p.checkNextToken("dist");
                api.parameter("maxdist", p.getNextFloat());
            }
            if (tex == null) {
                api.shader(name, "ambient_occlusion");
            } else {
                api.shader(name, "textured_ambient_occlusion");
            }
        } else if (p.peekNextToken("mirror")) {
            p.checkNextToken(REFL);
            api.parameter(COLOR, null, parseColor().getRGB());
            api.shader(name, "mirror");
        } else if (p.peekNextToken("glass")) {
            p.checkNextToken("eta");
            api.parameter("eta", p.getNextFloat());
            p.checkNextToken(COLOR);
            api.parameter(COLOR, null, parseColor().getRGB());
            if (p.peekNextToken("absorption.distance") || p.peekNextToken("absorbtion.distance")) {
                api.parameter("absorption.distance", p.getNextFloat());
            }
            if (p.peekNextToken("absorption.color") || p.peekNextToken("absorbtion.color")) {
                api.parameter("absorption.color", null, parseColor().getRGB());
            }
            api.shader(name, "glass");
        } else if (p.peekNextToken("shiny")) {
            String tex = null;
            if (p.peekNextToken(TEXTURE)) {
                api.parameter(TEXTURE, tex = p.getNextToken());
            } else {
                p.checkNextToken(DIFF);
                api.parameter(DIFFUSE, null, parseColor().getRGB());
            }
            p.checkNextToken(REFL);
            api.parameter("shiny", p.getNextFloat());
            if (tex == null) {
                api.shader(name, "shiny_diffuse");
            } else {
                api.shader(name, "textured_shiny_diffuse");
            }
        } else if (p.peekNextToken("ward")) {
            String tex = null;
            if (p.peekNextToken(TEXTURE)) {
                api.parameter(TEXTURE, tex = p.getNextToken());
            } else {
                p.checkNextToken(DIFF);
                api.parameter(DIFFUSE, null, parseColor().getRGB());
            }
            p.checkNextToken("spec");
            api.parameter("specular", null, parseColor().getRGB());
            p.checkNextToken("rough");
            api.parameter("roughnessX", p.getNextFloat());
            api.parameter("roughnessY", p.getNextFloat());
            if (p.peekNextToken(SAMPLES)) {
                api.parameter(SAMPLES, p.getNextInt());
            }
            if (tex != null) {
                api.shader(name, "textured_ward");
            } else {
                api.shader(name, "ward");
            }
        } else if (p.peekNextToken("view-caustics")) {
            api.shader(name, "view_caustics");
        } else if (p.peekNextToken("view-irradiance")) {
            api.shader(name, "view_irradiance");
        } else if (p.peekNextToken("view-global")) {
            api.shader(name, "view_global");
        } else if (p.peekNextToken("constant")) {
            // backwards compatibility -- peek only
            p.peekNextToken(COLOR);
            api.parameter(COLOR, null, parseColor().getRGB());
            api.shader(name, "constant");
        } else if (p.peekNextToken("janino")) {
            String typename = p.peekNextToken("typename") ? p.getNextToken() : PluginRegistry.shaderPlugins.generateUniqueName("janino_shader");
            if (!PluginRegistry.shaderPlugins.registerPlugin(typename, p.getNextCodeBlock())) {
                return false;
            }
            api.shader(name, typename);
        } else if (p.peekNextToken("id")) {
            api.shader(name, "show_instance_id");
        } else if (p.peekNextToken("uber")) {
            if (p.peekNextToken(DIFF)) {
                api.parameter(DIFFUSE, null, parseColor().getRGB());
            }
            if (p.peekNextToken("diff.texture")) {
                api.parameter("diffuse.texture", p.getNextToken());
            }
            if (p.peekNextToken("diff.blend")) {
                api.parameter("diffuse.blend", p.getNextFloat());
            }
            if (p.peekNextToken(REFL) || p.peekNextToken("spec")) {
                api.parameter("specular", null, parseColor().getRGB());
            }
            if (p.peekNextToken(TEXTURE)) {
                // deprecated
                UI.printWarning(Module.API, "Deprecated uber shader parameter \"texture\" - please use \"diffuse.texture\" and \"diffuse.blend\" instead");
                api.parameter("diffuse.texture", p.getNextToken());
                api.parameter("diffuse.blend", p.getNextFloat());
            }
            if (p.peekNextToken("spec.texture")) {
                api.parameter("specular.texture", p.getNextToken());
            }
            if (p.peekNextToken("spec.blend")) {
                api.parameter("specular.blend", p.getNextFloat());
            }
            if (p.peekNextToken("glossy")) {
                api.parameter("glossyness", p.getNextFloat());
            }
            if (p.peekNextToken(SAMPLES)) {
                api.parameter(SAMPLES, p.getNextInt());
            }
            api.shader(name, "uber");
        } else {
            UI.printWarning(Module.API, "Unrecognized shader type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
        return true;
    }

    private boolean parseModifier(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken(NAME);
        String name = p.getNextToken();
        UI.printInfo(Module.API, "Reading modifier: %s ...", name);
        p.checkNextToken(TYPE);
        if (p.peekNextToken("bump")) {
            p.checkNextToken(TEXTURE);
            api.parameter(TEXTURE, p.getNextToken());
            p.checkNextToken(SCALE);
            api.parameter(SCALE, p.getNextFloat());
            api.modifier(name, "bump_map");
        } else if (p.peekNextToken("normalmap")) {
            p.checkNextToken(TEXTURE);
            api.parameter(TEXTURE, p.getNextToken());
            api.modifier(name, "normal_map");
        } else if (p.peekNextToken("perlin")) {
            p.checkNextToken("function");
            api.parameter("function", p.getNextInt());
            p.checkNextToken("size");
            api.parameter("size", p.getNextFloat());
            p.checkNextToken(SCALE);
            api.parameter(SCALE, p.getNextFloat());
            api.modifier(name, "perlin");
        } else {
            UI.printWarning(Module.API, "Unrecognized modifier type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
        return true;
    }

    private void parseObjectBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        boolean noInstance = false;
        Matrix4[] transform = null;
        float transformTime0 = 0, transformTime1 = 0;
        String name = null;
        String[] shaders = null;
        String[] modifiers = null;
        if (p.peekNextToken("noinstance")) {
            // this indicates that the geometry is to be created, but not
            // instanced into the scene
            noInstance = true;
        } else {
            // these are the parameters to be passed to the instance
            if (p.peekNextToken(SHADERS)) {
                int n = p.getNextInt();
                shaders = new String[n];
                for (int i = 0; i < n; i++) {
                    shaders[i] = p.getNextToken();
                }
            } else {
                p.checkNextToken(SHADER);
                shaders = new String[]{p.getNextToken()};
            }
            if (p.peekNextToken(MODIFIERS)) {
                int n = p.getNextInt();
                modifiers = new String[n];
                for (int i = 0; i < n; i++) {
                    modifiers[i] = p.getNextToken();
                }
            } else if (p.peekNextToken(MODIFIER)) {
                modifiers = new String[]{p.getNextToken()};
            }
            if (p.peekNextToken(TRANSFORM)) {
                if (p.peekNextToken("steps")) {
                    transform = new Matrix4[p.getNextInt()];
                    p.checkNextToken("times");
                    transformTime0 = p.getNextFloat();
                    transformTime1 = p.getNextFloat();
                    for (int i = 0; i < transform.length; i++) {
                        transform[i] = parseMatrix();
                    }
                } else {
                    transform = new Matrix4[]{parseMatrix()};
                }
            }
        }
        if (p.peekNextToken("accel")) {
            api.parameter("accel", p.getNextToken());
        }
        p.checkNextToken(TYPE);
        String type = p.getNextToken();
        if (p.peekNextToken(NAME)) {
            name = p.getNextToken();
        } else {
            name = generateUniqueName(type);
        }
        if (type.equals("mesh")) {
            UI.printWarning(Module.API, "Deprecated object type: mesh");
            UI.printInfo(Module.API, "Reading mesh: %s ...", name);
            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[numVertices * 3];
            float[] normals = new float[numVertices * 3];
            float[] uvs = new float[numVertices * 2];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                normals[3 * i + 0] = p.getNextFloat();
                normals[3 * i + 1] = p.getNextFloat();
                normals[3 * i + 2] = p.getNextFloat();
                uvs[2 * i + 0] = p.getNextFloat();
                uvs[2 * i + 1] = p.getNextFloat();
            }
            int[] triangles = new int[numTriangles * 3];
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[i * 3 + 0] = p.getNextInt();
                triangles[i * 3 + 1] = p.getNextInt();
                triangles[i * 3 + 2] = p.getNextInt();
            }
            // create geometry
            api.parameter(TRIANGLES, triangles);
            api.parameter(POINTS, POINT, VERTEX, points);
            api.parameter(NORMALS, "vector", VERTEX, normals);
            api.parameter(UVS, TEXCOORD, VERTEX, uvs);
            api.geometry(name, TRIANGLE_MESH);
        } else if (type.equals("flat-mesh")) {
            UI.printWarning(Module.API, "Deprecated object type: flat-mesh");
            UI.printInfo(Module.API, "Reading flat mesh: %s ...", name);
            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[numVertices * 3];
            float[] uvs = new float[numVertices * 2];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                uvs[2 * i + 0] = p.getNextFloat();
                uvs[2 * i + 1] = p.getNextFloat();
            }
            int[] triangles = new int[numTriangles * 3];
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[i * 3 + 0] = p.getNextInt();
                triangles[i * 3 + 1] = p.getNextInt();
                triangles[i * 3 + 2] = p.getNextInt();
            }
            // create geometry
            api.parameter(TRIANGLES, triangles);
            api.parameter(POINTS, POINT, VERTEX, points);
            api.parameter(UVS, TEXCOORD, VERTEX, uvs);
            api.geometry(name, TRIANGLE_MESH);
        } else if (type.equals("sphere")) {
            UI.printInfo(Module.API, "Reading sphere ...");
            api.geometry(name, "sphere");
            if (transform == null && !noInstance) {
                // legacy method of specifying transformation for spheres
                p.checkNextToken("c");
                float x = p.getNextFloat();
                float y = p.getNextFloat();
                float z = p.getNextFloat();
                p.checkNextToken("r");
                float radius = p.getNextFloat();
                api.parameter(TRANSFORM, Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius)));
                api.parameter(SHADERS, shaders);
                if (modifiers != null) {
                    api.parameter(MODIFIERS, modifiers);
                }
                api.instance(name + ".instance", name);
                // disable future instancing - instance has already been created
                noInstance = true;
            }
        } else if (type.equals("cylinder")) {
            UI.printInfo(Module.API, "Reading cylinder ...");
            api.geometry(name, "cylinder");
        } else if (type.equals("banchoff")) {
            UI.printInfo(Module.API, "Reading banchoff ...");
            api.geometry(name, "banchoff");
        } else if (type.equals("torus")) {
            UI.printInfo(Module.API, "Reading torus ...");
            p.checkNextToken("r");
            api.parameter("radiusInner", p.getNextFloat());
            api.parameter("radiusOuter", p.getNextFloat());
            api.geometry(name, "torus");
        } else if (type.equals("sphereflake")) {
            UI.printInfo(Module.API, "Reading sphereflake ...");
            if (p.peekNextToken("level")) {
                api.parameter("level", p.getNextInt());
            }
            if (p.peekNextToken("axis")) {
                api.parameter("axis", parseVector());
            }
            if (p.peekNextToken(RADIUS)) {
                api.parameter(RADIUS, p.getNextFloat());
            }
            api.geometry(name, "sphereflake");
        } else if (type.equals("plane")) {
            UI.printInfo(Module.API, "Reading plane ...");
            p.checkNextToken("p");
            api.parameter(CENTER, parsePoint());
            if (p.peekNextToken("n")) {
                api.parameter("normal", parseVector());
            } else {
                p.checkNextToken("p");
                api.parameter("point1", parsePoint());
                p.checkNextToken("p");
                api.parameter("point2", parsePoint());
            }
            api.geometry(name, "plane");
        } else if (type.equals("generic-mesh")) {
            UI.printInfo(Module.API, "Reading generic mesh: %s ... ", name);
            // parse vertices
            p.checkNextToken(POINTS);
            int np = p.getNextInt();
            api.parameter(POINTS, POINT, VERTEX, parseFloatArray(np * 3));
            // parse triangle indices
            p.checkNextToken(TRIANGLES);
            int nt = p.getNextInt();
            api.parameter(TRIANGLES, parseIntArray(nt * 3));
            // parse normals
            p.checkNextToken(NORMALS);
            if (p.peekNextToken(VERTEX)) {
                api.parameter(NORMALS, "vector", VERTEX, parseFloatArray(np * 3));
            } else if (p.peekNextToken(FACEVARYING)) {
                api.parameter(NORMALS, "vector", FACEVARYING, parseFloatArray(nt * 9));
            } else {
                p.checkNextToken(NONE);
            }
            // parse texture coordinates
            p.checkNextToken(UVS);
            if (p.peekNextToken(VERTEX)) {
                api.parameter(UVS, TEXCOORD, VERTEX, parseFloatArray(np * 2));
            } else if (p.peekNextToken(FACEVARYING)) {
                api.parameter(UVS, TEXCOORD, FACEVARYING, parseFloatArray(nt * 6));
            } else {
                p.checkNextToken(NONE);
            }
            if (p.peekNextToken("face_shaders")) {
                api.parameter("faceshaders", parseIntArray(nt));
            }
            api.geometry(name, TRIANGLE_MESH);
        } else if (type.equals("hair")) {
            UI.printInfo(Module.API, "Reading hair curves: %s ... ", name);
            p.checkNextToken("segments");
            api.parameter("segments", p.getNextInt());
            p.checkNextToken("width");
            api.parameter("widths", p.getNextFloat());
            p.checkNextToken(POINTS);
            api.parameter(POINTS, POINT, VERTEX, parseFloatArray(p.getNextInt()));
            api.geometry(name, "hair");
        } else if (type.equals("janino-tesselatable")) {
            UI.printInfo(Module.API, "Reading procedural primitive: %s ... ", name);
            String typename = p.peekNextToken("typename") ? p.getNextToken() : PluginRegistry.shaderPlugins.generateUniqueName("janino_shader");
            if (!PluginRegistry.tesselatablePlugins.registerPlugin(typename, p.getNextCodeBlock())) {
                noInstance = true;
            } else {
                api.geometry(name, typename);
            }
        } else if (type.equals("teapot")) {
            UI.printInfo(Module.API, "Reading teapot: %s ... ", name);
            if (p.peekNextToken(SUBDIVS)) {
                api.parameter(SUBDIVS, p.getNextInt());
            }
            if (p.peekNextToken(SMOOTH)) {
                api.parameter(SMOOTH, p.getNextBoolean());
            }
            api.geometry(name, "teapot");
        } else if (type.equals("gumbo")) {
            UI.printInfo(Module.API, "Reading gumbo: %s ... ", name);
            if (p.peekNextToken(SUBDIVS)) {
                api.parameter(SUBDIVS, p.getNextInt());
            }
            if (p.peekNextToken(SMOOTH)) {
                api.parameter(SMOOTH, p.getNextBoolean());
            }
            api.geometry(name, "gumbo");
        } else if (type.equals("julia")) {
            UI.printInfo(Module.API, "Reading julia fractal: %s ... ", name);
            if (p.peekNextToken("q")) {
                api.parameter("cw", p.getNextFloat());
                api.parameter("cx", p.getNextFloat());
                api.parameter("cy", p.getNextFloat());
                api.parameter("cz", p.getNextFloat());
            }
            if (p.peekNextToken("iterations")) {
                api.parameter("iterations", p.getNextInt());
            }
            if (p.peekNextToken("epsilon")) {
                api.parameter("epsilon", p.getNextFloat());
            }
            api.geometry(name, "julia");
        } else if (type.equals("particles") || type.equals("dlasurface")) {
            if (type.equals("dlasurface")) {
                UI.printWarning(Module.API, "Deprecated object type: \"dlasurface\" - please use \"particles\" instead");
            }
            float[] data;
            if (p.peekNextToken("filename")) {
                // FIXME: this code should be moved into an on demand loading
                // primitive
                String filename = p.getNextToken();
                boolean littleEndian = false;
                if (p.peekNextToken("little_endian")) {
                    littleEndian = true;
                }
                UI.printInfo(Module.USER, "Loading particle file: %s", filename);
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                if (littleEndian) {
                    map.order(ByteOrder.LITTLE_ENDIAN);
                }
                FloatBuffer buffer = map.asFloatBuffer();
                data = new float[buffer.capacity()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = buffer.get(i);
                }
                stream.close();
            } else {
                p.checkNextToken(POINTS);
                int n = p.getNextInt();
                data = parseFloatArray(n * 3); // read 3n points
            }
            api.parameter("particles", POINT, VERTEX, data);
            if (p.peekNextToken("num")) {
                api.parameter("num", p.getNextInt());
            } else {
                api.parameter("num", data.length / 3);
            }
            p.checkNextToken(RADIUS);
            api.parameter(RADIUS, p.getNextFloat());
            api.geometry(name, "particles");
        } else if (type.equals("file-mesh")) {
            UI.printInfo(Module.API, "Reading file mesh: %s ... ", name);
            p.checkNextToken("filename");
            api.parameter("filename", p.getNextToken());
            if (p.peekNextToken("smooth_normals")) {
                api.parameter("smooth_normals", p.getNextBoolean());
            }
            api.geometry(name, "file_mesh");
        } else if (type.equals("bezier-mesh")) {
            UI.printInfo(Module.API, "Reading bezier mesh: %s ... ", name);
            p.checkNextToken("n");
            int nu, nv;
            api.parameter("nu", nu = p.getNextInt());
            api.parameter("nv", nv = p.getNextInt());
            if (p.peekNextToken("wrap")) {
                api.parameter("uwrap", p.getNextBoolean());
                api.parameter("vwrap", p.getNextBoolean());
            }
            p.checkNextToken(POINTS);
            float[] points = new float[3 * nu * nv];
            for (int i = 0; i < points.length; i++) {
                points[i] = p.getNextFloat();
            }
            api.parameter(POINTS, POINT, VERTEX, points);
            if (p.peekNextToken(SUBDIVS)) {
                api.parameter(SUBDIVS, p.getNextInt());
            }
            if (p.peekNextToken(SMOOTH)) {
                api.parameter(SMOOTH, p.getNextBoolean());
            }
            api.geometry(name, "bezier_mesh");
        } else {
            UI.printWarning(Module.API, "Unrecognized object type: %s", p.getNextToken());
            noInstance = true;
        }
        if (!noInstance) {
            // create instance
            api.parameter(SHADERS, shaders);
            if (modifiers != null) {
                api.parameter(MODIFIERS, modifiers);
            }
            if (transform != null && transform.length > 0) {
                if (transform.length == 1) {
                    api.parameter(TRANSFORM, transform[0]);
                } else {
                    api.parameter("transform.steps", transform.length);
                    api.parameter("transform.times", "float", NONE, new float[]{
                        transformTime0, transformTime1});
                    for (int i = 0; i < transform.length; i++) {
                        api.parameter(String.format("transform[%d]", i), transform[i]);
                    }
                }
            }
            api.instance(name + ".instance", name);
        }
        p.checkNextToken("}");
    }

    private void parseInstanceBlock(SunflowAPIInterface api) throws ParserException, IOException {
        p.checkNextToken("{");
        p.checkNextToken(NAME);
        String name = p.getNextToken();
        UI.printInfo(Module.API, "Reading instance: %s ...", name);
        p.checkNextToken("geometry");
        String geoname = p.getNextToken();
        p.checkNextToken(TRANSFORM);
        if (p.peekNextToken("steps")) {
            int n = p.getNextInt();
            api.parameter("transform.steps", n);
            p.checkNextToken("times");
            float[] times = new float[2];
            times[0] = p.getNextFloat();
            times[1] = p.getNextFloat();
            api.parameter("transform.times", "float", NONE, times);
            for (int i = 0; i < n; i++) {
                api.parameter(String.format("transform[%d]", i), parseMatrix());
            }
        } else {
            api.parameter(TRANSFORM, parseMatrix());
        }
        String[] shaders;
        if (p.peekNextToken(SHADERS)) {
            int n = p.getNextInt();
            shaders = new String[n];
            for (int i = 0; i < n; i++) {
                shaders[i] = p.getNextToken();
            }
        } else {
            p.checkNextToken(SHADER);
            shaders = new String[]{p.getNextToken()};
        }
        api.parameter(SHADERS, shaders);
        String[] modifiers = null;
        if (p.peekNextToken(MODIFIERS)) {
            int n = p.getNextInt();
            modifiers = new String[n];
            for (int i = 0; i < n; i++) {
                modifiers[i] = p.getNextToken();
            }
        } else if (p.peekNextToken(MODIFIER)) {
            modifiers = new String[]{p.getNextToken()};
        }
        if (modifiers != null) {
            api.parameter(MODIFIERS, modifiers);
        }
        api.instance(name, geoname);
        p.checkNextToken("}");
    }

    private void parseLightBlock(SunflowAPIInterface api) throws ParserException, IOException, ColorSpecificationException {
        p.checkNextToken("{");
        p.checkNextToken(TYPE);
        if (p.peekNextToken("mesh")) {
            UI.printWarning(Module.API, "Deprecated light type: mesh");
            p.checkNextToken(NAME);
            String name = p.getNextToken();
            UI.printInfo(Module.API, "Reading light mesh: %s ...", name);
            p.checkNextToken(EMIT);
            api.parameter(RADIANCE, null, parseColor().getRGB());
            int samples = numLightSamples;
            if (p.peekNextToken(SAMPLES)) {
                samples = p.getNextInt();
            } else {
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", samples);
            }
            api.parameter(SAMPLES, samples);
            int numVertices = p.getNextInt();
            int numTriangles = p.getNextInt();
            float[] points = new float[3 * numVertices];
            int[] triangles = new int[3 * numTriangles];
            for (int i = 0; i < numVertices; i++) {
                p.checkNextToken("v");
                points[3 * i + 0] = p.getNextFloat();
                points[3 * i + 1] = p.getNextFloat();
                points[3 * i + 2] = p.getNextFloat();
                // ignored
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
                p.getNextFloat();
            }
            for (int i = 0; i < numTriangles; i++) {
                p.checkNextToken("t");
                triangles[3 * i + 0] = p.getNextInt();
                triangles[3 * i + 1] = p.getNextInt();
                triangles[3 * i + 2] = p.getNextInt();
            }
            api.parameter(POINTS, POINT, VERTEX, points);
            api.parameter(TRIANGLES, triangles);
            api.light(name, TRIANGLE_MESH);
        } else if (p.peekNextToken(POINT)) {
            UI.printInfo(Module.API, "Reading point light ...");
            Color pow;
            if (p.peekNextToken(COLOR)) {
                pow = parseColor();
                p.checkNextToken(POWER);
                float po = p.getNextFloat();
                pow.mul(po);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use color and power instead");
                p.checkNextToken(POWER);
                pow = parseColor();
            }
            p.checkNextToken("p");
            api.parameter(CENTER, parsePoint());
            api.parameter(POWER, null, pow.getRGB());
            api.light(generateUniqueName("pointlight"), POINT);
        } else if (p.peekNextToken("spherical")) {
            UI.printInfo(Module.API, "Reading spherical light ...");
            p.checkNextToken(COLOR);
            Color pow = parseColor();
            p.checkNextToken(RADIANCE);
            pow.mul(p.getNextFloat());
            api.parameter(RADIANCE, null, pow.getRGB());
            p.checkNextToken(CENTER);
            api.parameter(CENTER, parsePoint());
            p.checkNextToken(RADIUS);
            api.parameter(RADIUS, p.getNextFloat());
            p.checkNextToken(SAMPLES);
            api.parameter(SAMPLES, p.getNextInt());
            api.light(generateUniqueName("spherelight"), "sphere");
        } else if (p.peekNextToken("directional")) {
            UI.printInfo(Module.API, "Reading directional light ...");
            p.checkNextToken("source");
            Point3 s = parsePoint();
            api.parameter("source", s);
            p.checkNextToken("target");
            Point3 t = parsePoint();
            api.parameter("dir", Point3.sub(t, s, new Vector3()));
            p.checkNextToken(RADIUS);
            api.parameter(RADIUS, p.getNextFloat());
            p.checkNextToken(EMIT);
            Color e = parseColor();
            if (p.peekNextToken("intensity")) {
                float i = p.getNextFloat();
                e.mul(i);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use emit and intensity instead");
            }
            api.parameter(RADIANCE, null, e.getRGB());
            api.light(generateUniqueName("dirlight"), "directional");
        } else if (p.peekNextToken("ibl")) {
            UI.printInfo(Module.API, "Reading image based light ...");
            p.checkNextToken("image");
            api.parameter(TEXTURE, p.getNextToken());
            p.checkNextToken(CENTER);
            api.parameter(CENTER, parseVector());
            p.checkNextToken("up");
            api.parameter("up", parseVector());
            p.checkNextToken("lock");
            api.parameter("fixed", p.getNextBoolean());
            int samples = numLightSamples;
            if (p.peekNextToken(SAMPLES)) {
                samples = p.getNextInt();
            } else {
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", samples);
            }
            api.parameter(SAMPLES, samples);
            if (p.peekNextToken("lowsamples")) {
                api.parameter("lowsamples", p.getNextInt());
            } else {
                api.parameter("lowsamples", samples);
            }
            api.light(generateUniqueName("ibl"), "ibl");
        } else if (p.peekNextToken("meshlight")) {
            p.checkNextToken(NAME);
            String name = p.getNextToken();
            UI.printInfo(Module.API, "Reading meshlight: %s ...", name);
            p.checkNextToken(EMIT);
            Color e = parseColor();
            if (p.peekNextToken(RADIANCE)) {
                float r = p.getNextFloat();
                e.mul(r);
            } else {
                UI.printWarning(Module.API, "Deprecated color specification - please use emit and radiance instead");
            }
            api.parameter(RADIANCE, null, e.getRGB());
            int samples = numLightSamples;
            if (p.peekNextToken(SAMPLES)) {
                samples = p.getNextInt();
            } else {
                UI.printWarning(Module.API, "Samples keyword not found - defaulting to %d", samples);
            }
            api.parameter(SAMPLES, samples);
            // parse vertices
            p.checkNextToken(POINTS);
            int np = p.getNextInt();
            api.parameter(POINTS, POINT, VERTEX, parseFloatArray(np * 3));
            // parse triangle indices
            p.checkNextToken(TRIANGLES);
            int nt = p.getNextInt();
            api.parameter(TRIANGLES, parseIntArray(nt * 3));
            api.light(name, TRIANGLE_MESH);
        } else if (p.peekNextToken("sunsky")) {
            p.checkNextToken("up");
            api.parameter("up", parseVector());
            p.checkNextToken("east");
            api.parameter("east", parseVector());
            p.checkNextToken("sundir");
            api.parameter("sundir", parseVector());
            p.checkNextToken("turbidity");
            api.parameter("turbidity", p.getNextFloat());
            if (p.peekNextToken(SAMPLES)) {
                api.parameter(SAMPLES, p.getNextInt());
            }
            if (p.peekNextToken("ground.extendsky")) {
                api.parameter("ground.extendsky", p.getNextBoolean());
            } else if (p.peekNextToken("ground.color")) {
                api.parameter("ground.color", null, parseColor().getRGB());
            }
            api.light(generateUniqueName("sunsky"), "sunsky");
        } else if (p.peekNextToken("cornellbox")) {
            UI.printInfo(Module.API, "Reading cornell box ...");
            p.checkNextToken("corner0");
            api.parameter("corner0", parsePoint());
            p.checkNextToken("corner1");
            api.parameter("corner1", parsePoint());
            p.checkNextToken("left");
            api.parameter("leftColor", null, parseColor().getRGB());
            p.checkNextToken("right");
            api.parameter("rightColor", null, parseColor().getRGB());
            p.checkNextToken("top");
            api.parameter("topColor", null, parseColor().getRGB());
            p.checkNextToken("bottom");
            api.parameter("bottomColor", null, parseColor().getRGB());
            p.checkNextToken("back");
            api.parameter("backColor", null, parseColor().getRGB());
            p.checkNextToken(EMIT);
            api.parameter(RADIANCE, null, parseColor().getRGB());
            if (p.peekNextToken(SAMPLES)) {
                api.parameter(SAMPLES, p.getNextInt());
            }
            api.light(generateUniqueName("cornellbox"), "cornell_box");
        } else {
            UI.printWarning(Module.API, "Unrecognized object type: %s", p.getNextToken());
        }
        p.checkNextToken("}");
    }

    private Color parseColor() throws IOException, ParserException, ColorSpecificationException {
        if (p.peekNextToken("{")) {
            String space = p.getNextToken();
            int req = ColorFactory.getRequiredDataValues(space);
            if (req == -2) {
                UI.printWarning(Module.API, "Unrecognized color space: %s", space);
                return null;
            } else if (req == -1) {
                // array required, parse how many values are required
                req = p.getNextInt();
            }
            Color c = ColorFactory.createColor(space, parseFloatArray(req));
            p.checkNextToken("}");
            return c;
        } else {
            float r = p.getNextFloat();
            float g = p.getNextFloat();
            float b = p.getNextFloat();
            return ColorFactory.createColor(null, r, g, b);
        }
    }

    private Point3 parsePoint() throws IOException {
        float x = p.getNextFloat();
        float y = p.getNextFloat();
        float z = p.getNextFloat();
        return new Point3(x, y, z);
    }

    private Vector3 parseVector() throws IOException {
        float x = p.getNextFloat();
        float y = p.getNextFloat();
        float z = p.getNextFloat();
        return new Vector3(x, y, z);
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = p.getNextInt();
        }
        return data;
    }

    private float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = p.getNextFloat();
        }
        return data;
    }

    private Matrix4 parseMatrix() throws IOException, ParserException {
        if (p.peekNextToken("row")) {
            return new Matrix4(parseFloatArray(16), true);
        } else if (p.peekNextToken("col")) {
            return new Matrix4(parseFloatArray(16), false);
        } else {
            Matrix4 m = Matrix4.IDENTITY;
            p.checkNextToken("{");
            while (!p.peekNextToken("}")) {
                Matrix4 t = null;
                if (p.peekNextToken("translate")) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    t = Matrix4.translation(x, y, z);
                } else if (p.peekNextToken("scaleu")) {
                    float s = p.getNextFloat();
                    t = Matrix4.scale(s);
                } else if (p.peekNextToken(SCALE)) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    t = Matrix4.scale(x, y, z);
                } else if (p.peekNextToken("rotatex")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateX((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotatey")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateY((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotatez")) {
                    float angle = p.getNextFloat();
                    t = Matrix4.rotateZ((float) Math.toRadians(angle));
                } else if (p.peekNextToken("rotate")) {
                    float x = p.getNextFloat();
                    float y = p.getNextFloat();
                    float z = p.getNextFloat();
                    float angle = p.getNextFloat();
                    t = Matrix4.rotate(x, y, z, (float) Math.toRadians(angle));
                } else {
                    UI.printWarning(Module.API, "Unrecognized transformation type: %s", p.getNextToken());
                }
                if (t != null) {
                    m = t.multiply(m);
                }
            }
            return m;
        }
    }
}