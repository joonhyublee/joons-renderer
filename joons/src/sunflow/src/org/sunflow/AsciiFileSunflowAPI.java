package org.sunflow;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.parser.Keyword;
import org.sunflow.math.Matrix4;

class AsciiFileSunflowAPI extends FileSunflowAPI {

    private OutputStream stream;

    AsciiFileSunflowAPI(String filename) throws IOException {
        stream = new BufferedOutputStream(new FileOutputStream(filename));
    }

    @Override
    protected void writeBoolean(boolean value) {
        if (value) {
            writeString("true");
        } else {
            writeString("false");
        }
    }

    @Override
    protected void writeFloat(float value) {
        writeString(String.format("%s", value));
    }

    @Override
    protected void writeInt(int value) {
        writeString(String.format("%d", value));
    }

    @Override
    protected void writeInterpolationType(InterpolationType interp) {
        writeString(String.format("%s", interp.toString().toLowerCase(Locale.ENGLISH)));
    }

    @Override
    protected void writeKeyword(Keyword keyword) {
        writeString(String.format("%s", keyword.toString().toLowerCase(Locale.ENGLISH).replace("_array", "[]")));
    }

    @Override
    protected void writeMatrix(Matrix4 value) {
        writeString("row");
        for (float f : value.asRowMajor()) {
            writeFloat(f);
        }
    }

    @Override
    protected void writeNewline(int indentNext) {
        try {
            stream.write('\n');
            for (int i = 0; i < indentNext; i++) {
                stream.write('\t');
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeString(String string) {
        try {
            // check if we need to write string with quotes
            if (string.contains(" ") && !string.contains("<code>")) {
                stream.write(String.format("\"%s\"", string).getBytes());
            } else {
                stream.write(string.getBytes());
            }
            stream.write(' ');
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void writeVerbatimString(String string) {
        writeString(String.format("<code>%s\n</code> ", string));
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