package org.sunflow.core.parser;

import java.io.IOException;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.system.Parser;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;

public class SCAsciiParser extends SCAbstractParser {

    private Parser p;

    protected Color parseColor() throws IOException {
        String space = p.getNextToken();
        Color c = null;
        if (space.equals("sRGB nonlinear")) {
            float r = p.getNextFloat();
            float g = p.getNextFloat();
            float b = p.getNextFloat();
            c = new Color(r, g, b);
            c.toLinear();
        } else if (space.equals("sRGB linear")) {
            float r = p.getNextFloat();
            float g = p.getNextFloat();
            float b = p.getNextFloat();
            c = new Color(r, g, b);
        } else {
            UI.printWarning(Module.API, "Unrecognized color space: %s", space);
        }
        return c;
    }

    @Override
    protected Matrix4 parseMatrix() throws IOException {
        if (p.peekNextToken("row")) {
            return new Matrix4(parseFloatArray(16), true);
        } else if (p.peekNextToken("col")) {
            return new Matrix4(parseFloatArray(16), false);
        } else {
            Matrix4 m = Matrix4.IDENTITY;
            try {
                p.checkNextToken("{");
            } catch (ParserException e) {
                throw new IOException(e.getMessage());
            }
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
                } else if (p.peekNextToken("scale")) {
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

    @Override
    protected void closeParser() throws IOException {
        p.close();
    }

    @Override
    protected void openParser(String filename) throws IOException {
        p = new Parser(filename);
    }

    @Override
    protected boolean parseBoolean() throws IOException {
        return Boolean.parseBoolean(parseString());
    }

    @Override
    protected float parseFloat() throws IOException {
        return p.getNextFloat();
    }

    @Override
    protected int parseInt() throws IOException {
        return p.getNextInt();
    }

    @Override
    protected String parseString() throws IOException {
        return p.getNextToken();
    }

    @Override
    protected String parseVerbatimString() throws IOException {
        try {
            return p.getNextCodeBlock();
        } catch (ParserException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    protected InterpolationType parseInterpolationType() throws IOException {
        if (p.peekNextToken("none")) {
            return InterpolationType.NONE;
        } else if (p.peekNextToken("vertex")) {
            return InterpolationType.VERTEX;
        } else if (p.peekNextToken("face")) {
            return InterpolationType.FACE;
        } else if (p.peekNextToken("facevarying")) {
            return InterpolationType.FACEVARYING;
        }
        return InterpolationType.NONE;
    }

    @Override
    protected Keyword parseKeyword() throws IOException {
        String keyword = p.getNextToken();
        if (keyword == null) {
            return Keyword.END_OF_FILE;
        }
        if (anyEqual(keyword, "reset")) {
            return Keyword.RESET;
        }
        if (anyEqual(keyword, "parameter", "param", "p")) {
            return Keyword.PARAMETER;
        }
        if (anyEqual(keyword, "geometry", "geom", "g")) {
            return Keyword.GEOMETRY;
        }
        if (anyEqual(keyword, "instance", "inst", "i")) {
            return Keyword.INSTANCE;
        }
        if (anyEqual(keyword, "shader", "shd", "s")) {
            return Keyword.SHADER;
        }
        if (anyEqual(keyword, "modifier", "mod", "m")) {
            return Keyword.MODIFIER;
        }
        if (anyEqual(keyword, "light", "l")) {
            return Keyword.LIGHT;
        }
        if (anyEqual(keyword, "camera", "cam", "c")) {
            return Keyword.CAMERA;
        }
        if (anyEqual(keyword, "options", "opt", "o")) {
            return Keyword.OPTIONS;
        }
        if (anyEqual(keyword, "include", "inc")) {
            return Keyword.INCLUDE;
        }
        if (anyEqual(keyword, "remove")) {
            return Keyword.REMOVE;
        }
        if (anyEqual(keyword, "frame")) {
            return Keyword.FRAME;
        }
        if (anyEqual(keyword, "plugin", "plug")) {
            return Keyword.PLUGIN;
        }
        if (anyEqual(keyword, "searchpath")) {
            return Keyword.SEARCHPATH;
        }
        if (anyEqual(keyword, "string", "str")) {
            return Keyword.STRING;
        }
        if (anyEqual(keyword, "string[]", "str[]")) {
            return Keyword.STRING_ARRAY;
        }
        if (anyEqual(keyword, "boolean", "bool")) {
            return Keyword.BOOL;
        }
        if (anyEqual(keyword, "integer", "int")) {
            return Keyword.INT;
        }
        if (anyEqual(keyword, "integer[]", "int[]")) {
            return Keyword.INT_ARRAY;
        }
        if (anyEqual(keyword, "float", "flt")) {
            return Keyword.FLOAT;
        }
        if (anyEqual(keyword, "float[]", "flt[]")) {
            return Keyword.FLOAT_ARRAY;
        }
        if (anyEqual(keyword, "color", "col")) {
            return Keyword.COLOR;
        }
        if (anyEqual(keyword, "point", "pnt")) {
            return Keyword.POINT;
        }
        if (anyEqual(keyword, "point[]", "pnt[]")) {
            return Keyword.POINT_ARRAY;
        }
        if (anyEqual(keyword, "vector", "vec")) {
            return Keyword.VECTOR;
        }
        if (anyEqual(keyword, "vector[]", "vec[]")) {
            return Keyword.VECTOR_ARRAY;
        }
        if (anyEqual(keyword, "texcoord", "tex")) {
            return Keyword.TEXCOORD;
        }
        if (anyEqual(keyword, "texcoord[]", "tex[]")) {
            return Keyword.TEXCOORD_ARRAY;
        }
        if (anyEqual(keyword, "matrix", "mat")) {
            return Keyword.MATRIX;
        }
        if (anyEqual(keyword, "matrix[]", "mat[]")) {
            return Keyword.MATRIX_ARRAY;
        }
        return null;
    }

    private boolean anyEqual(String source, String... values) {
        for (String v : values) {
            if (source.equals(v)) {
                return true;
            }
        }
        return false;
    }
}