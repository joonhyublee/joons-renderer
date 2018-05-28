package org.sunflow;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.parser.Keyword;
import org.sunflow.math.Matrix4;

class BinaryFileSunflowAPI extends FileSunflowAPI {

    private DataOutputStream stream;

    BinaryFileSunflowAPI(String filename) throws FileNotFoundException {
        stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }

    @Override
    protected void writeBoolean(boolean value) {
        try {
            if (value) {
                stream.write(1);
            } else {
                stream.write(0);
            }
        } catch (IOException e) {
            // throw as a silent exception to avoid having to propage throw
            // declarations upwards
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeFloat(float value) {
        writeInt(Float.floatToRawIntBits(value));
    }

    @Override
    protected void writeInt(int value) {
        try {
            // little endian, LSB first
            stream.write(value & 0xFF);
            stream.write((value >>> 8) & 0xFF);
            stream.write((value >>> 16) & 0xFF);
            stream.write((value >>> 24) & 0xFF);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeInterpolationType(InterpolationType interp) {
        try {
            switch (interp) {
                case NONE:
                    stream.write('n');
                    break;
                case VERTEX:
                    stream.write('v');
                    break;
                case FACE:
                    stream.write('p');
                    break;
                case FACEVARYING:
                    stream.write('f');
                    break;
                default:
                    throw new RuntimeException(String.format("Unknown interpolation type \"%s\"", interp.toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeKeyword(Keyword keyword) {
        try {
            switch (keyword) {
                case RESET:
                    writeExtendedKeyword('R');
                    break;
                case PARAMETER:
                    stream.write('p');
                    break;
                case GEOMETRY:
                    stream.write('g');
                    break;
                case INSTANCE:
                    stream.write('i');
                    break;
                case SHADER:
                    stream.write('s');
                    break;
                case MODIFIER:
                    stream.write('m');
                    break;
                case LIGHT:
                    stream.write('l');
                    break;
                case CAMERA:
                    stream.write('c');
                    break;
                case OPTIONS:
                    stream.write('o');
                    break;
                case INCLUDE:
                    writeExtendedKeyword('i');
                    break;
                case REMOVE:
                    writeExtendedKeyword('r');
                    break;
                case FRAME:
                    writeExtendedKeyword('f');
                    break;
                case PLUGIN:
                    writeExtendedKeyword('p');
                    break;
                case SEARCHPATH:
                    writeExtendedKeyword('s');
                    break;
                case STRING:
                    writeDatatypeKeyword('s', false);
                    break;
                case BOOL:
                    writeDatatypeKeyword('b', false);
                    break;
                case INT:
                    writeDatatypeKeyword('i', false);
                    break;
                case FLOAT:
                    writeDatatypeKeyword('f', false);
                    break;
                case COLOR:
                    writeDatatypeKeyword('c', false);
                    break;
                case POINT:
                    writeDatatypeKeyword('p', false);
                    break;
                case VECTOR:
                    writeDatatypeKeyword('v', false);
                    break;
                case TEXCOORD:
                    writeDatatypeKeyword('t', false);
                    break;
                case MATRIX:
                    writeDatatypeKeyword('m', false);
                    break;
                case STRING_ARRAY:
                    writeDatatypeKeyword('s', true);
                    break;
                case INT_ARRAY:
                    writeDatatypeKeyword('i', true);
                    break;
                case FLOAT_ARRAY:
                    writeDatatypeKeyword('f', true);
                    break;
                case POINT_ARRAY:
                    writeDatatypeKeyword('p', true);
                    break;
                case VECTOR_ARRAY:
                    writeDatatypeKeyword('v', true);
                    break;
                case TEXCOORD_ARRAY:
                    writeDatatypeKeyword('t', true);
                    break;
                case MATRIX_ARRAY:
                    writeDatatypeKeyword('m', true);
                    break;
                default:
                    throw new RuntimeException(String.format("Unknown keyword \"%s\" requested", keyword.toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void writeExtendedKeyword(int code) throws IOException {
        stream.write('x');
        stream.write(code);
    }

    // helper routine for datatype keywords
    private void writeDatatypeKeyword(int type, boolean isArray) throws IOException {
        stream.write('t');
        stream.write(type);
        writeBoolean(isArray);
    }

    @Override
    protected void writeMatrix(Matrix4 value) {
        for (float f : value.asRowMajor()) {
            writeFloat(f);
        }
    }

    @Override
    protected void writeString(String string) {
        try {
            byte[] data = string.getBytes("UTF-8");
            writeInt(data.length);
            stream.write(data);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeVerbatimString(String string) {
        writeString(string);
    }

    @Override
    protected void writeNewline(int indentNext) {
        // does nothing
    }

    @Override
    public void close() {
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}