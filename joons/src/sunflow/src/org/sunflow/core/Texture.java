package org.sunflow.core;

import java.io.IOException;

import org.sunflow.PluginRegistry;
import org.sunflow.image.Bitmap;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.Color;
import org.sunflow.image.BitmapReader.BitmapFormatException;
import org.sunflow.image.formats.BitmapBlack;
import org.sunflow.math.MathUtils;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.system.FileUtils;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

/**
 * Represents a 2D texture, typically used by {@link Shader shaders}.
 */
public class Texture {

    private String filename;
    private boolean isLinear;
    private Bitmap bitmap;
    private int loaded;

    /**
     * Creates a new texture from the specfied file.
     *
     * @param filename image file to load
     * @param isLinear is the texture gamma corrected already?
     */
    Texture(String filename, boolean isLinear) {
        this.filename = filename;
        this.isLinear = isLinear;
        loaded = 0;
    }

    private synchronized void load() {
        if (loaded != 0) {
            return;
        }
        String extension = FileUtils.getExtension(filename);
        try {
            UI.printInfo(Module.TEX, "Reading texture bitmap from: \"%s\" ...", filename);
            BitmapReader reader = PluginRegistry.bitmapReaderPlugins.createObject(extension);
            if (reader != null) {
                bitmap = reader.load(filename, isLinear);
                if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
                    bitmap = null;
                }
            }
            if (bitmap == null) {
                UI.printError(Module.TEX, "Bitmap reading failed");
                bitmap = new BitmapBlack();
            } else {
                UI.printDetailed(Module.TEX, "Texture bitmap reading complete: %dx%d pixels found", bitmap.getWidth(), bitmap.getHeight());
            }
        } catch (IOException e) {
            UI.printError(Module.TEX, "%s", e.getMessage());
        } catch (BitmapFormatException e) {
            UI.printError(Module.TEX, "%s format error: %s", extension, e.getMessage());
        }
        loaded = 1;
    }

    public Bitmap getBitmap() {
        if (loaded == 0) {
            load();
        }
        return bitmap;
    }

    /**
     * Gets the color at location (x,y) in the texture. The lookup is performed
     * using the fractional component of the coordinates, treating the texture
     * as a unit square tiled in both directions. Bicubic filtering is performed
     * on the four nearest pixels to the lookup point.
     *
     * @param x x coordinate into the texture
     * @param y y coordinate into the texture
     * @return filtered color at location (x,y)
     */
    public Color getPixel(float x, float y) {
        Bitmap bitmapc = getBitmap();
        x = MathUtils.frac(x);
        y = MathUtils.frac(y);
        float dx = x * (bitmapc.getWidth() - 1);
        float dy = y * (bitmapc.getHeight() - 1);
        int ix0 = (int) dx;
        int iy0 = (int) dy;
        int ix1 = (ix0 + 1) % bitmapc.getWidth();
        int iy1 = (iy0 + 1) % bitmapc.getHeight();
        float u = dx - ix0;
        float v = dy - iy0;
        u = u * u * (3.0f - (2.0f * u));
        v = v * v * (3.0f - (2.0f * v));
        float k00 = (1.0f - u) * (1.0f - v);
        Color c00 = bitmapc.readColor(ix0, iy0);
        float k01 = (1.0f - u) * v;
        Color c01 = bitmapc.readColor(ix0, iy1);
        float k10 = u * (1.0f - v);
        Color c10 = bitmapc.readColor(ix1, iy0);
        float k11 = u * v;
        Color c11 = bitmapc.readColor(ix1, iy1);
        Color c = Color.mul(k00, c00);
        c.madd(k01, c01);
        c.madd(k10, c10);
        c.madd(k11, c11);
        return c;
    }

    public Vector3 getNormal(float x, float y, OrthoNormalBasis basis) {
        float[] rgb = getPixel(x, y).getRGB();
        return basis.transform(new Vector3(2 * rgb[0] - 1, 2 * rgb[1] - 1, 2 * rgb[2] - 1)).normalize();
    }

    public Vector3 getBump(float x, float y, OrthoNormalBasis basis, float scale) {
        Bitmap bitmapv = getBitmap();
        float dx = 1.0f / bitmapv.getWidth();
        float dy = 1.0f / bitmapv.getHeight();
        float b0 = getPixel(x, y).getLuminance();
        float bx = getPixel(x + dx, y).getLuminance();
        float by = getPixel(x, y + dy).getLuminance();
        return basis.transform(new Vector3(scale * (b0 - bx), scale * (b0 - by), 1)).normalize();
    }
}