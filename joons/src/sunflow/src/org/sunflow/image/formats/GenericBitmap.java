package org.sunflow.image.formats;

import java.io.IOException;

import org.sunflow.PluginRegistry;
import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;
import org.sunflow.system.FileUtils;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * This is a generic and inefficient bitmap format which may be used for
 * debugging purposes (dumping small images), when memory usage is not a
 * concern.
 */
public class GenericBitmap extends Bitmap {

    private int w, h;
    private Color[] color;
    private float[] alpha;

    public GenericBitmap(int w, int h) {
        this.w = w;
        this.h = h;
        color = new Color[w * h];
        alpha = new float[w * h];
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public Color readColor(int x, int y) {
        return color[x + y * w];
    }

    @Override
    public float readAlpha(int x, int y) {
        return alpha[x + y * w];
    }

    public void writePixel(int x, int y, Color c, float a) {
        color[x + y * w] = c;
        alpha[x + y * w] = a;
    }

    public void save(String filename) {
        String extension = FileUtils.getExtension(filename);
        BitmapWriter writer = PluginRegistry.bitmapWriterPlugins.createObject(extension);
        if (writer == null) {
            UI.printError(Module.IMG, "Unable to save file \"%s\" - unknown file format: %s", filename, extension);
            return;
        }
        try {
            writer.openFile(filename);
            writer.writeHeader(w, h, Math.max(w, h));
            writer.writeTile(0, 0, w, h, color, alpha);
            writer.closeFile();
        } catch (IOException e) {
            UI.printError(Module.IMG, "Unable to save file \"%s\" - %s", filename, e.getLocalizedMessage());
        }
    }
}