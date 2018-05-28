package org.sunflow.image;

import org.sunflow.math.MathUtils;

public final class RGBSpace {

    public static final RGBSpace ADOBE = new RGBSpace(0.6400f, 0.3300f, 0.2100f, 0.7100f, 0.1500f, 0.0600f, 0.31271f, 0.32902f, 2.2f, 0);
    public static final RGBSpace APPLE = new RGBSpace(0.6250f, 0.3400f, 0.2800f, 0.5950f, 0.1550f, 0.0700f, 0.31271f, 0.32902f, 1.8f, 0);
    public static final RGBSpace NTSC = new RGBSpace(0.6700f, 0.3300f, 0.2100f, 0.7100f, 0.1400f, 0.0800f, 0.31010f, 0.31620f, 20.0f / 9.0f, 0.018f);
    public static final RGBSpace HDTV = new RGBSpace(0.6400f, 0.3300f, 0.3000f, 0.6000f, 0.1500f, 0.0600f, 0.31271f, 0.32902f, 20.0f / 9.0f, 0.018f);
    public static final RGBSpace SRGB = new RGBSpace(0.6400f, 0.3300f, 0.3000f, 0.6000f, 0.1500f, 0.0600f, 0.31271f, 0.32902f, 2.4f, 0.00304f);
    public static final RGBSpace CIE = new RGBSpace(0.7350f, 0.2650f, 0.2740f, 0.7170f, 0.1670f, 0.0090f, 1 / 3.0f, 1 / 3.0f, 2.2f, 0);
    public static final RGBSpace EBU = new RGBSpace(0.6400f, 0.3300f, 0.2900f, 0.6000f, 0.1500f, 0.0600f, 0.31271f, 0.32902f, 20.0f / 9.0f, 0.018f);
    public static final RGBSpace SMPTE_C = new RGBSpace(0.6300f, 0.3400f, 0.3100f, 0.5950f, 0.1550f, 0.0700f, 0.31271f, 0.32902f, 20.0f / 9.0f, 0.018f);
    public static final RGBSpace SMPTE_240M = new RGBSpace(0.6300f, 0.3400f, 0.3100f, 0.5950f, 0.1550f, 0.0700f, 0.31271f, 0.32902f, 20.0f / 9.0f, 0.018f);
    public static final RGBSpace WIDE_GAMUT = new RGBSpace(0.7347f, 0.2653f, 0.1152f, 0.8264f, 0.1566f, 0.0177f, 0.3457f, 0.3585f, 2.2f, 0);
    private final float gamma, breakPoint;
    private final float slope, slopeMatch, segmentOffset;
    private final float xr, yr, zr, xg, yg, zg, xb, yb, zb;
    private final float xw, yw, zw;
    private final float rx, ry, rz, gx, gy, gz, bx, by, bz;
    private final float rw, gw, bw;
    private final int[] GAMMA_CURVE;
    private final int[] INV_GAMMA_CURVE;

    public RGBSpace(float xRed, float yRed, float xGreen, float yGreen, float xBlue, float yBlue, float xWhite, float yWhite, float gamma, float breakPoint) {
        this.gamma = gamma;
        this.breakPoint = breakPoint;

        if (breakPoint > 0) {
            slope = 1 / (gamma / (float) Math.pow(breakPoint, 1 / gamma - 1) - gamma * breakPoint + breakPoint);
            slopeMatch = gamma * slope / (float) Math.pow(breakPoint, 1 / gamma - 1);
            segmentOffset = slopeMatch * (float) Math.pow(breakPoint, 1 / gamma) - slope * breakPoint;
        } else {
            slope = 1;
            slopeMatch = 1;
            segmentOffset = 0;
        }

        // prepare gamma curves
        GAMMA_CURVE = new int[256];
        INV_GAMMA_CURVE = new int[256];
        for (int i = 0; i < 256; i++) {
            float c = i / 255.0f;
            GAMMA_CURVE[i] = MathUtils.clamp((int) (gammaCorrect(c) * 255 + 0.5f), 0, 255);
            INV_GAMMA_CURVE[i] = MathUtils.clamp((int) (ungammaCorrect(c) * 255 + 0.5f), 0, 255);
        }

        float xr = xRed;
        float yr = yRed;
        float zr = 1 - (xr + yr);
        float xg = xGreen;
        float yg = yGreen;
        float zg = 1 - (xg + yg);
        float xb = xBlue;
        float yb = yBlue;
        float zb = 1 - (xb + yb);

        xw = xWhite;
        yw = yWhite;
        zw = 1 - (xw + yw);

        // xyz -> rgb matrix, before scaling to white.
        float rx = (yg * zb) - (yb * zg);
        float ry = (xb * zg) - (xg * zb);
        float rz = (xg * yb) - (xb * yg);
        float gx = (yb * zr) - (yr * zb);
        float gy = (xr * zb) - (xb * zr);
        float gz = (xb * yr) - (xr * yb);
        float bx = (yr * zg) - (yg * zr);
        float by = (xg * zr) - (xr * zg);
        float bz = (xr * yg) - (xg * yr);
        // White scaling factors
        // Dividing by yw scales the white luminance to unity, as conventional
        rw = ((rx * xw) + (ry * yw) + (rz * zw)) / yw;
        gw = ((gx * xw) + (gy * yw) + (gz * zw)) / yw;
        bw = ((bx * xw) + (by * yw) + (bz * zw)) / yw;

        // xyz -> rgb matrix, correctly scaled to white
        this.rx = rx / rw;
        this.ry = ry / rw;
        this.rz = rz / rw;
        this.gx = gx / gw;
        this.gy = gy / gw;
        this.gz = gz / gw;
        this.bx = bx / bw;
        this.by = by / bw;
        this.bz = bz / bw;

        // invert matrix again to get proper rgb -> xyz matrix
        float s = 1 / (this.rx * (this.gy * this.bz - this.by * this.gz) - this.ry * (this.gx * this.bz - this.bx * this.gz) + this.rz * (this.gx * this.by - this.bx * this.gy));
        this.xr = s * (this.gy * this.bz - this.gz * this.by);
        this.xg = s * (this.rz * this.by - this.ry * this.bz);
        this.xb = s * (this.ry * this.gz - this.rz * this.gy);

        this.yr = s * (this.gz * this.bx - this.gx * this.bz);
        this.yg = s * (this.rx * this.bz - this.rz * this.bx);
        this.yb = s * (this.rz * this.gx - this.rx * this.gz);

        this.zr = s * (this.gx * this.by - this.gy * this.bx);
        this.zg = s * (this.ry * this.bx - this.rx * this.by);
        this.zb = s * (this.rx * this.gy - this.ry * this.gx);
    }

    public final Color convertXYZtoRGB(XYZColor c) {
        return convertXYZtoRGB(c.getX(), c.getY(), c.getZ());
    }

    public final Color convertXYZtoRGB(float X, float Y, float Z) {
        float r = (rx * X) + (ry * Y) + (rz * Z);
        float g = (gx * X) + (gy * Y) + (gz * Z);
        float b = (bx * X) + (by * Y) + (bz * Z);
        return new Color(r, g, b);
    }

    public final XYZColor convertRGBtoXYZ(Color c) {
        float[] rgb = c.getRGB();
        float X = (xr * rgb[0]) + (xg * rgb[1]) + (xb * rgb[2]);
        float Y = (yr * rgb[0]) + (yg * rgb[1]) + (yb * rgb[2]);
        float Z = (zr * rgb[0]) + (zg * rgb[1]) + (zb * rgb[2]);
        return new XYZColor(X, Y, Z);
    }

    public final boolean insideGamut(float r, float g, float b) {
        return r >= 0 && g >= 0 && b >= 0;
    }

    public final float gammaCorrect(float v) {
        float gammac = 0;
        if (v >= 0) {
            if (v >= 1) {
                gammac = 1f;
            } else if (v <= breakPoint) {
                gammac = slope * v;
            } else {
                gammac = slopeMatch * (float) Math.pow(v, 1 / gamma) - segmentOffset;
            }
        }
        return gammac;
    }

    public final float ungammaCorrect(float vp) {
        float gammac = 0;
        if (vp > 0) {
            if (vp >= 1) {
                gammac = 1f;
            } else if (vp <= breakPoint * slope) {
                gammac = vp / slope;
            } else {
                gammac = (float) Math.pow((vp + segmentOffset) / slopeMatch, gamma);
            }
        }
        return gammac;
    }

    public final int rgbToNonLinear(int rgb) {
        // gamma correct 24bit rgb value via tables
        int rp = GAMMA_CURVE[(rgb >> 16) & 0xFF];
        int gp = GAMMA_CURVE[(rgb >> 8) & 0xFF];
        int bp = GAMMA_CURVE[rgb & 0xFF];
        return (rp << 16) | (gp << 8) | bp;
    }

    public final int rgbToLinear(int rgb) {
        // convert a packed RGB triplet to a linearized
        // one by applying the proper LUT
        int rp = INV_GAMMA_CURVE[(rgb >> 16) & 0xFF];
        int gp = INV_GAMMA_CURVE[(rgb >> 8) & 0xFF];
        int bp = INV_GAMMA_CURVE[rgb & 0xFF];
        return (rp << 16) | (gp << 8) | bp;
    }

    public final byte rgbToNonLinear(byte r) {
        return (byte) GAMMA_CURVE[r & 0xFF];
    }

    public final byte rgbToLinear(byte r) {
        return (byte) INV_GAMMA_CURVE[r & 0xFF];
    }

    @Override
    public final String toString() {
        String MATRIX_FORMAT = "| %7.4f %7.4f %7.4f|\n";
        StringBuilder info = new StringBuilder(200);
        info.append("Gamma function parameters:\n");
        info.append(String.format("  * Gamma:          %7.4f\n", gamma));
        info.append(String.format("  * Breakpoint:     %7.4f\n", breakPoint));
        info.append(String.format("  * Slope:          %7.4f\n", slope));
        info.append(String.format("  * Slope Match:    %7.4f\n", slopeMatch));
        info.append(String.format("  * Segment Offset: %7.4f\n", segmentOffset));
        info.append("XYZ -> RGB Matrix:\n");
        info.append(String.format(MATRIX_FORMAT, rx, ry, rz));
        info.append(String.format(MATRIX_FORMAT, gx, gy, gz));
        info.append(String.format(MATRIX_FORMAT, bx, by, bz));
        info.append("RGB -> XYZ Matrix:\n");
        info.append(String.format(MATRIX_FORMAT, xr, xg, xb));
        info.append(String.format(MATRIX_FORMAT, yr, yg, yb));
        info.append(String.format(MATRIX_FORMAT, zr, zg, zb));
        return info.toString();
    }

    public static void main(String[] args) {
        System.out.println(SRGB.toString());
        System.out.println(HDTV.toString());
        System.out.println(WIDE_GAMUT.toString());
    }
}