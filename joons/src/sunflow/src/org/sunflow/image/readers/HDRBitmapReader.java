package org.sunflow.image.readers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.formats.BitmapRGBE;

public class HDRBitmapReader implements BitmapReader {

    public Bitmap load(String filename, boolean isLinear) throws IOException, BitmapFormatException {
        // load radiance rgbe file
        InputStream f = new BufferedInputStream(new FileInputStream(filename));
        // parse header
        boolean parseWidth = false, parseHeight = false;
        int width = 0, height = 0;
        int last = 0;
        while (width == 0 || height == 0 || last != '\n') {
            int n = f.read();
            switch (n) {
                case 'Y':
                    parseHeight = last == '-';
                    parseWidth = false;
                    break;
                case 'X':
                    parseHeight = false;
                    parseWidth = last == '+';
                    break;
                case ' ':
                    parseWidth &= width == 0;
                    parseHeight &= height == 0;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (parseHeight) {
                        height = 10 * height + (n - '0');
                    } else if (parseWidth) {
                        width = 10 * width + (n - '0');
                    }
                    break;
                default:
                    parseWidth = parseHeight = false;
                    break;
            }
            last = n;
        }
        // allocate image
        int[] pixels = new int[width * height];
        if ((width < 8) || (width > 0x7fff)) {
            // run length encoding is not allowed so read flat
            readFlatRGBE(f, 0, width * height, pixels);
        } else {
            int rasterPos = 0;
            int numScanlines = height;
            int[] scanlineBuffer = new int[4 * width];
            while (numScanlines > 0) {
                int r = f.read();
                int g = f.read();
                int b = f.read();
                int e = f.read();
                if ((r != 2) || (g != 2) || ((b & 0x80) != 0)) {
                    // this file is not run length encoded
                    pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
                    readFlatRGBE(f, rasterPos + 1, width * numScanlines - 1, pixels);
                    break;
                }

                if (((b << 8) | e) != width) {
                    throw new BitmapFormatException("Invalid scanline width");
                }
                int p = 0;
                // read each of the four channels for the scanline into
                // the buffer
                for (int i = 0; i < 4; i++) {
                    if (p % width != 0) {
                        throw new BitmapFormatException("Unaligned access to scanline data");
                    }
                    int end = (i + 1) * width;
                    while (p < end) {
                        int b0 = f.read();
                        int b1 = f.read();
                        if (b0 > 128) {
                            // a run of the same value
                            int count = b0 - 128;
                            if ((count == 0) || (count > (end - p))) {
                                throw new BitmapFormatException("Bad scanline data - invalid RLE run");
                            }

                            while (count-- > 0) {
                                scanlineBuffer[p] = b1;
                                p++;
                            }
                        } else {
                            // a non-run
                            int count = b0;
                            if ((count == 0) || (count > (end - p))) {
                                throw new BitmapFormatException("Bad scanline data - invalid count");
                            }
                            scanlineBuffer[p] = b1;
                            p++;
                            if (--count > 0) {
                                for (int x = 0; x < count; x++) {
                                    scanlineBuffer[p + x] = f.read();
                                }
                                p += count;
                            }
                        }
                    }
                }
                // now convert data from buffer into floats
                for (int i = 0; i < width; i++) {
                    r = scanlineBuffer[i];
                    g = scanlineBuffer[i + width];
                    b = scanlineBuffer[i + 2 * width];
                    e = scanlineBuffer[i + 3 * width];
                    pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
                    rasterPos++;
                }
                numScanlines--;
            }
        }
        f.close();
        // flip image
        for (int y = 0, i = 0, ir = (height - 1) * width; y < height / 2; y++, ir -= width) {
            for (int x = 0, i2 = ir; x < width; x++, i++, i2++) {
                int t = pixels[i];
                pixels[i] = pixels[i2];
                pixels[i2] = t;
            }
        }
        return new BitmapRGBE(width, height, pixels);
    }

    private void readFlatRGBE(InputStream f, int rasterPos, int numPixels, int[] pixels) throws IOException {
        while (numPixels-- > 0) {
            int r = f.read();
            int g = f.read();
            int b = f.read();
            int e = f.read();
            pixels[rasterPos] = (r << 24) | (g << 16) | (b << 8) | e;
            rasterPos++;
        }
    }
}