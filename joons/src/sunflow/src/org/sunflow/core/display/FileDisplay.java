package org.sunflow.core.display;

import java.io.IOException;

import org.sunflow.PluginRegistry;
import org.sunflow.core.Display;
import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;
import org.sunflow.system.FileUtils;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class FileDisplay implements Display {

    private BitmapWriter writer;
    private String filename;

    public FileDisplay(boolean saveImage) {
        this(saveImage ? "output.png" : ".none");
    }

    public FileDisplay(String filename) {
        this.filename = filename == null ? "output.png" : filename;
        String extension = FileUtils.getExtension(filename);
        writer = PluginRegistry.bitmapWriterPlugins.createObject(extension);
    }

    @Override
    public void imageBegin(int w, int h, int bucketSize) {
        if (writer == null) {
            return;
        }
        try {
            writer.openFile(filename);
            writer.writeHeader(w, h, bucketSize);
        } catch (IOException e) {
            UI.printError(Module.IMG, "I/O error occured while preparing image for display: %s", e.getMessage());
        }
    }

    @Override
    public void imagePrepare(int x, int y, int w, int h, int id) {
        // does nothing for files
    }

    @Override
    public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
        if (writer == null) {
            return;
        }
        try {
            writer.writeTile(x, y, w, h, data, alpha);
        } catch (IOException e) {
            UI.printError(Module.IMG, "I/O error occured while writing image tile [(%d,%d) %dx%d] image for display: %s", x, y, w, h, e.getMessage());
        }
    }

    @Override
    public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        if (writer == null) {
            return;
        }
        Color[] colorTile = new Color[w * h];
        float[] alphaTile = new float[w * h];
        for (int i = 0; i < colorTile.length; i++) {
            colorTile[i] = c;
            alphaTile[i] = alpha;
        }
        imageUpdate(x, y, w, h, colorTile, alphaTile);
    }

    @Override
    public void imageEnd() {
        if (writer == null) {
            return;
        }
        try {
            writer.closeFile();
        } catch (IOException e) {
            UI.printError(Module.IMG, "I/O error occured while closing the display: %s", e.getMessage());
        }
    }
}