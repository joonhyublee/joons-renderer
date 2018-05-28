package org.sunflow.image;

public final class ColorFactory {

    /**
     * Return the name of the internal color space. This string can be used
     * interchangeably with
     * <code>null</code> in the following methods.
     *
     * @return internal colorspace name
     */
    public static String getInternalColorspace() {
        return "sRGB linear";
    }

    /**
     * Checks to see how many values are required to specify a color using the
     * given colorspace. This number can be variable for spectrum colors, in
     * which case the returned value is -1. If the colorspace name is invalid,
     * this method returns -2. No exception is thrown. This method is intended
     * for parsers that want to know how many floating values to retrieve from a
     * file.
     *
     * @param colorspace
     * @return number of floating point numbers expected, -1 for any, -2 on
     * error
     */
    public static int getRequiredDataValues(String colorspace) {
        if (colorspace == null) {
            return 3;
        }
        if (colorspace.equals("sRGB nonlinear")) {
            return 3;
        } else if (colorspace.equals("sRGB linear")) {
            return 3;
        } else if (colorspace.equals("XYZ")) {
            return 3;
        } else if (colorspace.equals("blackbody")) {
            return 1;
        } else if (colorspace.startsWith("spectrum")) {
            return -1;
        } else {
            return -2;
        }
    }

    /**
     * Creates a color value in the renderer's internal color space from a
     * string (representing the color space name) and an array of floating point
     * values. If the colorspace string is null, we assume the data was supplied
     * in internal space. This method does much error checking and may throw a
     * {@link RuntimeException} if its parameters are not consistent. Here are
     * the currently supported color spaces:
     * <ul>
     * <li><code>"sRGB nonlinear"</code> - requires 3 values</li>
     * <li><code>"sRGB linear"</code> - requires 3 values</li>
     * <li><code>"XYZ"</code> - requires 3 values</li>
     * <li><code>blackbody</code> - requires 1 value (temperature in
     * Kelvins)</li>
     * <li><code>spectrum [min] [max]</code> - any number of values (must be
     * >0), [start] and [stop] is the range over which the spectrum is defined
     * in nanometers.</li>
     * </ul>
     *
     * @param colorspace color space name
     * @param data data describing this color
     * @return a valid color in the renderer's color space
     * @throws ColorSpecificationException
     */
    public static Color createColor(String colorspace, float... data) throws ColorSpecificationException {
        int required = getRequiredDataValues(colorspace);
        if (required == -2) {
            throw new ColorSpecificationException("unknown colorspace %s");
        }
        if (required != -1 && required != data.length) {
            throw new ColorSpecificationException(required, data.length);
        }
        if (colorspace == null) {
            return new Color(data[0], data[1], data[2]);
        } else if (colorspace.equals("sRGB nonlinear")) {
            return new Color(data[0], data[1], data[2]).toLinear();
        } else if (colorspace.equals("sRGB linear")) {
            return new Color(data[0], data[1], data[2]);
        } else if (colorspace.equals("XYZ")) {
            return RGBSpace.SRGB.convertXYZtoRGB(new XYZColor(data[0], data[1], data[2]));
        } else if (colorspace.equals("blackbody")) {
            return RGBSpace.SRGB.convertXYZtoRGB(new BlackbodySpectrum(data[0]).toXYZ());
        } else if (colorspace.startsWith("spectrum")) {
            String[] tokens = colorspace.split("\\s+");
            if (tokens.length != 3) {
                throw new ColorSpecificationException("invalid spectrum specification");
            }
            if (data.length == 0) {
                throw new ColorSpecificationException("missing spectrum data");
            }
            try {
                float lambdaMin = Float.parseFloat(tokens[1]);
                float lambdaMax = Float.parseFloat(tokens[2]);
                return RGBSpace.SRGB.convertXYZtoRGB(new RegularSpectralCurve(data, lambdaMin, lambdaMax).toXYZ());
            } catch (NumberFormatException e) {
                throw new ColorSpecificationException("unable to parse spectrum wavelength range");
            }
        }
        throw new ColorSpecificationException(String.format("Inconsistent code! Please report this error. (Input %s - %d)", colorspace, data.length));
    }

    @SuppressWarnings("serial")
    public static final class ColorSpecificationException extends Exception {

        private ColorSpecificationException() {
            super("Invalid color specification");
        }

        private ColorSpecificationException(String message) {
            super(String.format("Invalid color specification: %s", message));
        }

        private ColorSpecificationException(int expected, int found) {
            this(String.format("invalid data length, expecting %d values, found %d", expected, found));
        }
    }
}