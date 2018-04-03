package org.sunflow.image.readers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.Color;
import org.sunflow.image.formats.BitmapG8;
import org.sunflow.image.formats.BitmapRGB8;
import org.sunflow.image.formats.BitmapRGBA8;

public class TGABitmapReader implements BitmapReader {

    private static final int[] CHANNEL_INDEX = {2, 1, 0, 3};

    public Bitmap load(String filename, boolean isLinear) throws IOException, BitmapFormatException {
        InputStream f = new BufferedInputStream(new FileInputStream(filename));
        byte[] read = new byte[4];

        // read header
        int idsize = f.read();
        int cmaptype = f.read(); // cmap byte (unsupported)
        if (cmaptype != 0) {
            throw new BitmapFormatException(String.format("Colormapping (type: %d) is unsupported", cmaptype));
        }
        int datatype = f.read();

        // colormap info (5 bytes ignored)
        f.read();
        f.read();
        f.read();
        f.read();
        f.read();

        f.read(); // xstart, 16 bits (ignored)
        f.read();
        f.read(); // ystart, 16 bits (ignored)
        f.read();

        // read resolution
        int width = f.read();
        width |= f.read() << 8;
        int height = f.read();
        height |= f.read() << 8;

        int bits = f.read();
        int bpp = bits / 8;

        int imgdscr = f.read();

        // skip image ID if present
        if (idsize != 0) {
            f.skip(idsize);
        }

        // allocate byte buffer to hold the image
        byte[] pixels = new byte[width * height * bpp];
        if (datatype == 2 || datatype == 3) {
            if (bpp != 1 && bpp != 3 && bpp != 4) {
                throw new BitmapFormatException(String.format("Invalid bit depth in uncompressed TGA: %d", bits));
            }
            // uncompressed image
            for (int ptr = 0; ptr < pixels.length; ptr += bpp) {
                // read bytes
                f.read(read, 0, bpp);
                for (int i = 0; i < bpp; i++) {
                    pixels[ptr + CHANNEL_INDEX[i]] = read[i];
                }
            }
        } else if (datatype == 10) {
            if (bpp != 3 && bpp != 4) {
                throw new BitmapFormatException(String.format("Invalid bit depth in run-length encoded TGA: %d", bits));
            }
            // RLE encoded image
            for (int ptr = 0; ptr < pixels.length;) {
                int rle = f.read();
                int num = 1 + (rle & 0x7F);
                if ((rle & 0x80) != 0) {
                    // rle packet - decode length and copy pixel
                    f.read(read, 0, bpp);
                    for (int j = 0; j < num; j++) {
                        for (int i = 0; i < bpp; i++) {
                            pixels[ptr + CHANNEL_INDEX[i]] = read[i];
                        }
                        ptr += bpp;
                    }
                } else {
                    // raw packet - decode length and read pixels
                    for (int j = 0; j < num; j++) {
                        f.read(read, 0, bpp);
                        for (int i = 0; i < bpp; i++) {
                            pixels[ptr + CHANNEL_INDEX[i]] = read[i];
                        }
                        ptr += bpp;
                    }
                }
            }
        } else {
            throw new BitmapFormatException(String.format("Unsupported TGA image type: %d", datatype));
        }

        if (!isLinear) {
            // apply reverse correction
            for (int ptr = 0; ptr < pixels.length; ptr += bpp) {
                for (int i = 0; i < 3 && i < bpp; i++) {
                    pixels[ptr + i] = Color.NATIVE_SPACE.rgbToLinear(pixels[ptr + i]);
                }
            }
        }

        // should image be flipped in Y?
        if ((imgdscr & 32) == 32) {
            for (int y = 0, pix_ptr = 0; y < (height / 2); y++) {
                int bot_ptr = bpp * (height - y - 1) * width;
                for (int x = 0; x < width; x++) {
                    for (int i = 0; i < bpp; i++) {
                        byte t = pixels[pix_ptr + i];
                        pixels[pix_ptr + i] = pixels[bot_ptr + i];
                        pixels[bot_ptr + i] = t;
                    }
                    pix_ptr += bpp;
                    bot_ptr += bpp;
                }
            }

        }
        f.close();
        switch (bpp) {
            case 1:
                return new BitmapG8(width, height, pixels);
            case 3:
                return new BitmapRGB8(width, height, pixels);
            case 4:
                return new BitmapRGBA8(width, height, pixels);
        }
        throw new BitmapFormatException("Inconsistent code in TGA reader");
    }
}