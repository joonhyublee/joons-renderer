package org.sunflow.image.writers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;

public class PNGBitmapWriter implements BitmapWriter {

    private String filename;
    private BufferedImage image;

    public void configure(String option, String value) {
    }

    public void openFile(String filename) throws IOException {
        this.filename = filename;
    }

    public void writeHeader(int width, int height, int tileSize) throws IOException, UnsupportedOperationException {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void writeTile(int x, int y, int w, int h, Color[] color, float[] alpha) throws IOException {
        for (int j = 0, index = 0; j < h; j++) {
            for (int i = 0; i < w; i++, index++) {
                image.setRGB(x + i, y + j, color[index].copy().mul(1.0f / alpha[index]).toNonLinear().toRGBA(alpha[index]));
            }
        }
    }

    public void closeFile() throws IOException {
        ImageIO.write(image, "png", new File(filename));
    }
}