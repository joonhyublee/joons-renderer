package org.sunflow.image;

import org.sunflow.math.MathUtils;

/**
 * This class contains many static helper methods that may be helpful for
 * encoding colors into files.
 */
public final class ColorEncoder {

    /**
     * Undoes the premultiplication of the specified color array. The original
     * colors are not modified.
     *
     * @param color an array of premultiplied colors
     * @param alpha alpha values corresponding to the colors
     * @return an array of unpremultiplied colors
     */
    public static final Color[] unpremult(Color[] color, float[] alpha) {
        Color[] output = new Color[color.length];
        for (int i = 0; i < color.length; i++) {
            output[i] = color[i].copy().mul(1 / alpha[i]);
        }
        return output;
    }

    /**
     * Moves the colors in the specified array to non-linear space. The original
     * colors are not modified.
     *
     * @param color an array of colors in linear space
     * @return a new array of the same colors in non-linear space
     */
    public static final Color[] unlinearize(Color[] color) {
        Color[] output = new Color[color.length];
        for (int i = 0; i < color.length; i++) {
            output[i] = color[i].copy().toNonLinear();
        }
        return output;
    }

    /**
     * Quantize the specified colors to 8-bit RGB format. The returned array
     * contains 3 bytes for each color in the original array.
     *
     * @param color array of colors to quantize
     * @return array of quantized RGB values
     */
    public static final byte[] quantizeRGB8(Color[] color) {
        byte[] output = new byte[color.length * 3];
        for (int i = 0, index = 0; i < color.length; i++, index += 3) {
            float[] rgb = color[i].getRGB();
            output[index + 0] = (byte) MathUtils.clamp((int) (rgb[0] * 255 + 0.5f), 0, 255);
            output[index + 1] = (byte) MathUtils.clamp((int) (rgb[1] * 255 + 0.5f), 0, 255);
            output[index + 2] = (byte) MathUtils.clamp((int) (rgb[2] * 255 + 0.5f), 0, 255);
        }
        return output;
    }

    /**
     * Quantize the specified colors to 8-bit RGBA format. The returned array
     * contains 4 bytes for each color in the original array.
     *
     * @param color array of colors to quantize
     * @param alpha array of alpha values (same length as color)
     * @return array of quantized RGBA values
     */
    public static final byte[] quantizeRGBA8(Color[] color, float[] alpha) {
        byte[] output = new byte[color.length * 4];
        for (int i = 0, index = 0; i < color.length; i++, index += 4) {
            float[] rgb = color[i].getRGB();
            output[index + 0] = (byte) MathUtils.clamp((int) (rgb[0] * 255 + 0.5f), 0, 255);
            output[index + 1] = (byte) MathUtils.clamp((int) (rgb[1] * 255 + 0.5f), 0, 255);
            output[index + 2] = (byte) MathUtils.clamp((int) (rgb[2] * 255 + 0.5f), 0, 255);
            output[index + 3] = (byte) MathUtils.clamp((int) (alpha[i] * 255 + 0.5f), 0, 255);
        }
        return output;
    }

    /**
     * Encode the specified colors using Ward's RGBE technique. The returned
     * array contains one int for each color in the original array.
     *
     * @param color array of colors to encode
     * @return array of encoded colors
     */
    public static final int[] encodeRGBE(Color[] color) {
        int[] output = new int[color.length];
        for (int i = 0; i < color.length; i++) {
            output[i] = color[i].toRGBE();
        }
        return output;
    }
}