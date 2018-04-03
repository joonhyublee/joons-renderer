package org.sunflow.core.parser;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.image.ColorFactory;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public abstract class SCAbstractParser implements SceneParser {

  

    @Override
    public boolean parse(String filename, SunflowAPIInterface api) {
        Timer timer = new Timer();
        timer.start();
        UI.printInfo(Module.API, "Parsing \"%s\" ...", filename);
        try {
            openParser(filename);
            parseloop:
            while (true) {
                Keyword k = parseKeyword();
                switch (k) {
                    case RESET:
                        api.reset();
                        break;
                    case PARAMETER:
                        parseParameter(api);
                        break;
                    case GEOMETRY: {
                        String name = parseString();
                        String type = parseString();
                        api.geometry(name, type);
                        break;
                    }
                    case INSTANCE: {
                        String name = parseString();
                        String geoname = parseString();
                        api.instance(name, geoname);
                        break;
                    }
                    case SHADER: {
                        String name = parseString();
                        String type = parseString();
                        api.shader(name, type);
                        break;
                    }
                    case MODIFIER: {
                        String name = parseString();
                        String type = parseString();
                        api.modifier(name, type);
                        break;
                    }
                    case LIGHT: {
                        String name = parseString();
                        String type = parseString();
                        api.light(name, type);
                        break;
                    }
                    case CAMERA: {
                        String name = parseString();
                        String type = parseString();
                        api.camera(name, type);
                        break;
                    }
                    case OPTIONS: {
                        api.options(parseString());
                        break;
                    }
                    case INCLUDE: {
                        String file = parseString();
                        UI.printInfo(Module.API, "Including: \"%s\" ...", file);
                        api.include(file);
                        break;
                    }
                    case REMOVE: {
                        api.remove(parseString());
                        break;
                    }
                    case FRAME: {
                        api.currentFrame(parseInt());
                        break;
                    }
                    case PLUGIN: {
                        String type = parseString();
                        String name = parseString();
                        String code = parseVerbatimString();
                        api.plugin(type, name, code);
                        break;
                    }
                    case SEARCHPATH: {
                        String type = parseString();
                        api.searchpath(type, parseString());
                        break;
                    }
                    case END_OF_FILE: {
                        // clean exit
                        break parseloop;
                    }
                    default: {
                        UI.printWarning(Module.API, "Unexpected token %s", k);
                        break;
                    }
                }
            }
            closeParser();
        } catch (Exception e) {
            // catch all exceptions
            Logger.getLogger(SCAbstractParser.class.getName()).log(Level.SEVERE, null, e);
            UI.printError(Module.API, "%s", e.getMessage());
            return false;
        }
        timer.end();
        UI.printInfo(Module.API, "Done parsing (took %s)", timer.toString());
        return true;
    }

    private void parseParameter(SunflowAPIInterface api) throws IOException {
        String name = parseString();
        Keyword k = parseKeyword();
        switch (k) {
            case STRING: {
                api.parameter(name, parseString());
                break;
            }
            case BOOL: {
                api.parameter(name, parseBoolean());
                break;
            }
            case INT: {
                api.parameter(name, parseInt());
                break;
            }
            case FLOAT: {
                api.parameter(name, parseFloat());
                break;
            }
            case COLOR: {
                String colorspace = parseString();
                int req = ColorFactory.getRequiredDataValues(colorspace);
                if (req == -2) {
                    api.parameter(name, colorspace); // call just to generate
                } // an error
                else {
                    api.parameter(name, colorspace, parseFloatArray(req == -1 ? parseInt() : req));
                }
                break;
            }
            case POINT: {
                api.parameter(name, parsePoint());
                break;
            }
            case VECTOR: {
                api.parameter(name, parseVector());
                break;
            }
            case TEXCOORD: {
                api.parameter(name, parseTexcoord());
                break;
            }
            case MATRIX: {
                api.parameter(name, parseMatrix());
                break;
            }
            case STRING_ARRAY: {
                int n = parseInt();
                api.parameter(name, parseStringArray(n));
                break;
            }
            case INT_ARRAY: {
                int n = parseInt();
                api.parameter(name, parseIntArray(n));
                break;
            }
            case FLOAT_ARRAY: {
                String interp = parseInterpolationType().toString();
                int n = parseInt();
                api.parameter(name, "float", interp, parseFloatArray(n));
                break;
            }
            case POINT_ARRAY: {
                String interp = parseInterpolationType().toString();
                int n = parseInt();
                api.parameter(name, "point", interp, parseFloatArray(3 * n));
                break;
            }
            case VECTOR_ARRAY: {
                String interp = parseInterpolationType().toString();
                int n = parseInt();
                api.parameter(name, "vector", interp, parseFloatArray(3 * n));
                break;
            }
            case TEXCOORD_ARRAY: {
                String interp = parseInterpolationType().toString();
                int n = parseInt();
                api.parameter(name, "texcoord", interp, parseFloatArray(2 * n));
                break;
            }
            case MATRIX_ARRAY: {
                String interp = parseInterpolationType().toString();
                int n = parseInt();
                api.parameter(name, "matrix", interp, parseMatrixArray(n));
                break;
            }
            case END_OF_FILE:
                throw new EOFException();
            default: {
                UI.printWarning(Module.API, "Unexpected keyword: %s", k);
                break;
            }
        }
    }

    private String[] parseStringArray(int size) throws IOException {
        String[] data = new String[size];
        for (int i = 0; i < size; i++) {
            data[i] = parseString();
        }
        return data;
    }

    private int[] parseIntArray(int size) throws IOException {
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = parseInt();
        }
        return data;
    }

    protected float[] parseFloatArray(int size) throws IOException {
        float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = parseFloat();
        }
        return data;
    }

    private float[] parseMatrixArray(int size) throws IOException {
        float[] data = new float[16 * size];
        for (int i = 0, offset = 0; i < size; i++, offset += 16) {
            // copy the next matrix into a linear array - in row major order
            float[] rowdata = parseMatrix().asRowMajor();
            System.arraycopy(rowdata, 0, data, offset, 16);
        }
        return data;
    }

    private Point3 parsePoint() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        float z = parseFloat();
        return new Point3(x, y, z);
    }

    private Vector3 parseVector() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        float z = parseFloat();
        return new Vector3(x, y, z);
    }

    private Point2 parseTexcoord() throws IOException {
        float x = parseFloat();
        float y = parseFloat();
        return new Point2(x, y);
    }

    protected abstract InterpolationType parseInterpolationType() throws IOException;

    // abstract methods - to be implemented by subclasses
    protected abstract void openParser(String filename) throws IOException;

    protected abstract void closeParser() throws IOException;

    protected abstract Keyword parseKeyword() throws IOException;

    protected abstract boolean parseBoolean() throws IOException;

    protected abstract int parseInt() throws IOException;

    protected abstract float parseFloat() throws IOException;

    protected abstract String parseString() throws IOException;

    protected abstract String parseVerbatimString() throws IOException;

    protected abstract Matrix4 parseMatrix() throws IOException;
}