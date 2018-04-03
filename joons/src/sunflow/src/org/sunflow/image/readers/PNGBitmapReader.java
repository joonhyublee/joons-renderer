package org.sunflow.image.readers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.Color;
import org.sunflow.image.formats.BitmapRGBA8;

public class PNGBitmapReader implements BitmapReader {

    public Bitmap load(String filename, boolean isLinear) throws IOException, BitmapFormatException {
        // regular image, load using Java api
        BufferedImage bi = ImageIO.read(new File(filename));
        int width = bi.getWidth();
        int height = bi.getHeight();
        byte[] pixels = new byte[4 * width * height];
        for (int y = 0, index = 0; y < height; y++) {
            for (int x = 0; x < width; x++, index += 4) {
                int argb = bi.getRGB(x, height - 1 - y);
                pixels[index + 0] = (byte) (argb >> 16);
                pixels[index + 1] = (byte) (argb >> 8);
                pixels[index + 2] = (byte) argb;
                pixels[index + 3] = (byte) (argb >> 24);
            }
        }
        if (!isLinear) {
            for (int index = 0; index < pixels.length; index += 4) {
                pixels[index + 0] = Color.NATIVE_SPACE.rgbToLinear(pixels[index + 0]);
                pixels[index + 1] = Color.NATIVE_SPACE.rgbToLinear(pixels[index + 1]);
                pixels[index + 2] = Color.NATIVE_SPACE.rgbToLinear(pixels[index + 2]);
            }
        }
        return new BitmapRGBA8(width, height, pixels);
    }
}