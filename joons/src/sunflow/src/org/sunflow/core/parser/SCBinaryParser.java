package org.sunflow.core.parser;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.math.Matrix4;

public class SCBinaryParser extends SCAbstractParser {

    private DataInputStream stream;

    @Override
    protected void closeParser() throws IOException {
        stream.close();
    }

    @Override
    protected void openParser(String filename) throws IOException {
        stream = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
    }

    @Override
    protected boolean parseBoolean() throws IOException {
        return stream.readUnsignedByte() != 0;
    }

    @Override
    protected float parseFloat() throws IOException {
        return Float.intBitsToFloat(parseInt());
    }

    @Override
    protected int parseInt() throws IOException {
        // note that we use readUnsignedByte(), not read() to get EOF exceptions
        return stream.readUnsignedByte() | (stream.readUnsignedByte() << 8) | (stream.readUnsignedByte() << 16) | (stream.readUnsignedByte() << 24);
    }

    @Override
    protected Matrix4 parseMatrix() throws IOException {
        return new Matrix4(parseFloatArray(16), true);
    }

    @Override
    protected String parseString() throws IOException {
        byte[] b = new byte[parseInt()];
        stream.read(b);
        return new String(b, "UTF-8");
    }

    @Override
    protected String parseVerbatimString() throws IOException {
        return parseString();
    }

    @Override
    protected InterpolationType parseInterpolationType() throws IOException {
        int c;
        switch (c = stream.readUnsignedByte()) {
            case 'n':
                return InterpolationType.NONE;
            case 'v':
                return InterpolationType.VERTEX;
            case 'f':
                return InterpolationType.FACEVARYING;
            case 'p':
                return InterpolationType.FACE;
            default:
                throw new IOException(String.format("Unknown byte found for interpolation type %c", (char) c));
        }
    }

    @Override
    protected Keyword parseKeyword() throws IOException {
        int code = stream.read(); // read a single byte - allow for EOF (<0)
        switch (code) {
            case 'p':
                return Keyword.PARAMETER;
            case 'g':
                return Keyword.GEOMETRY;
            case 'i':
                return Keyword.INSTANCE;
            case 's':
                return Keyword.SHADER;
            case 'm':
                return Keyword.MODIFIER;
            case 'l':
                return Keyword.LIGHT;
            case 'c':
                return Keyword.CAMERA;
            case 'o':
                return Keyword.OPTIONS;
            case 'x': {
                // extended keywords (less frequent)
                // note we don't use stream.read() here because we should throw
                // an exception if the end of the file is reached
                switch (code = stream.readUnsignedByte()) {
                    case 'R':
                        return Keyword.RESET;
                    case 'i':
                        return Keyword.INCLUDE;
                    case 'r':
                        return Keyword.REMOVE;
                    case 'f':
                        return Keyword.FRAME;
                    case 'p':
                        return Keyword.PLUGIN;
                    case 's':
                        return Keyword.SEARCHPATH;
                    default:
                        throw new IOException(String.format("Unknown extended keyword code: %c", (char) code));
                }
            }
            case 't': {
                // data types
                // note we don't use stream.read() here because we should throw
                // an exception if the end of the file is reached
                int type = stream.readUnsignedByte();
                // note that while not all types can be arrays at the moment, we
                // always parse this boolean flag to keep the syntax consistent
                // and allow for future improvements
                boolean isArray = parseBoolean();
                switch (type) {
                    case 's':
                        return isArray ? Keyword.STRING_ARRAY : Keyword.STRING;
                    case 'b':
                        return Keyword.BOOL;
                    case 'i':
                        return isArray ? Keyword.INT_ARRAY : Keyword.INT;
                    case 'f':
                        return isArray ? Keyword.FLOAT_ARRAY : Keyword.FLOAT;
                    case 'c':
                        return Keyword.COLOR;
                    case 'p':
                        return isArray ? Keyword.POINT_ARRAY : Keyword.POINT;
                    case 'v':
                        return isArray ? Keyword.VECTOR_ARRAY : Keyword.VECTOR;
                    case 't':
                        return isArray ? Keyword.TEXCOORD_ARRAY : Keyword.TEXCOORD;
                    case 'm':
                        return isArray ? Keyword.MATRIX_ARRAY : Keyword.MATRIX;
                    default:
                        throw new IOException(String.format("Unknown datatype keyword code: %c", (char) type));
                }
            }
            default:
                if (code < 0) {
                    return Keyword.END_OF_FILE; // normal end of file reached
                } else {
                    throw new IOException(String.format("Unknown keyword code: %c", (char) code));
                }
        }
    }
}