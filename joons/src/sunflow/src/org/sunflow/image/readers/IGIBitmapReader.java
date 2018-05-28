package org.sunflow.image.readers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.formats.BitmapXYZ;

/**
 * Reads images in Indigo's native XYZ format.
 * http://www2.indigorenderer.com/joomla/forum/viewtopic.php?p=11430
 */
public class IGIBitmapReader implements BitmapReader {

    public Bitmap load(String filename, boolean isLinear) throws IOException, BitmapFormatException {
        InputStream stream = new BufferedInputStream(new FileInputStream(filename));
        // read header
        int magic = read32i(stream);
        int version = read32i(stream);
        stream.skip(8); // skip number of samples (double value)
        int width = read32i(stream);
        int height = read32i(stream);
        int superSample = read32i(stream); // super sample factor
        int compression = read32i(stream);
        int dataSize = read32i(stream);
        int colorSpace = read32i(stream);
        stream.skip(5000); // skip the rest of the header (unused for now)
        // error checking
        if (magic != 66613373) {
            throw new BitmapFormatException("wrong magic: " + magic);
        }
        if (version != 1) {
            throw new BitmapFormatException("unsupported version: " + version);
        }
        if (compression != 0) {
            throw new BitmapFormatException("unsupported compression: " + compression);
        }
        if (colorSpace != 0) {
            throw new BitmapFormatException("unsupported color space: " + colorSpace);
        }
        if (dataSize != (width * height * 12)) {
            throw new BitmapFormatException("invalid data block size: " + dataSize);
        }
        if (width <= 0 || height <= 0) {
            throw new BitmapFormatException("invalid image size: " + width + "x" + height);
        }
        if (superSample <= 0) {
            throw new BitmapFormatException("invalid super sample factor: " + superSample);
        }
        if ((width % superSample) != 0 || (height % superSample) != 0) {
            throw new BitmapFormatException("invalid image size: " + width + "x" + height);
        }
        float[] xyz = new float[width * height * 3];
        for (int y = 0, i = 3 * (height - 1) * width; y < height; y++, i -= 6 * width) {
            for (int x = 0; x < width; x++, i += 3) {
                xyz[i + 0] = read32f(stream);
                xyz[i + 1] = read32f(stream);
                xyz[i + 2] = read32f(stream);
            }
        }
        stream.close();
        if (superSample > 1) {
            // rescale image (basic box filtering)
            float[] rescaled = new float[xyz.length / (superSample * superSample)];
            float inv = 1.0f / (superSample * superSample);
            for (int y = 0, i = 0; y < height; y += superSample) {
                for (int x = 0; x < width; x += superSample, i += 3) {
                    float X = 0;
                    float Y = 0;
                    float Z = 0;
                    for (int sy = 0; sy < superSample; sy++) {
                        for (int sx = 0; sx < superSample; sx++) {
                            int offset = 3 * ((x + sx + (y + sy) * width));
                            X += xyz[offset + 0];
                            Y += xyz[offset + 1];
                            Z += xyz[offset + 2];
                        }
                    }
                    rescaled[i + 0] = X * inv;
                    rescaled[i + 1] = Y * inv;
                    rescaled[i + 2] = Z * inv;
                }
            }
            return new BitmapXYZ(width / superSample, height / superSample, rescaled);
        } else {
            return new BitmapXYZ(width, height, xyz);
        }
    }

    private static final int read32i(InputStream stream) throws IOException {
        int i = stream.read();
        i |= stream.read() << 8;
        i |= stream.read() << 16;
        i |= stream.read() << 24;
        return i;
    }

    private static final float read32f(InputStream stream) throws IOException {
        return Float.intBitsToFloat(read32i(stream));
    }
}