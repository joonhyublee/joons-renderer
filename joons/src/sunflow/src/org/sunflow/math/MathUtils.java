package org.sunflow.math;

public final class MathUtils {

    private MathUtils() {
    }

    public static final int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x > min) {
            return x;
        }
        return min;
    }

    public static final float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        if (x > min) {
            return x;
        }
        return min;
    }

    public static final double clamp(double x, double min, double max) {
        if (x > max) {
            return max;
        }
        if (x > min) {
            return x;
        }
        return min;
    }

    public static final int min(int a, int b, int c) {
        if (a > b) {
            a = b;
        }
        if (a > c) {
            a = c;
        }
        return a;
    }

    public static final float min(float a, float b, float c) {
        if (a > b) {
            a = b;
        }
        if (a > c) {
            a = c;
        }
        return a;
    }

    public static final double min(double a, double b, double c) {
        if (a > b) {
            a = b;
        }
        if (a > c) {
            a = c;
        }
        return a;
    }

    public static final float min(float a, float b, float c, float d) {
        if (a > b) {
            a = b;
        }
        if (a > c) {
            a = c;
        }
        if (a > d) {
            a = d;
        }
        return a;
    }

    public static final int max(int a, int b, int c) {
        if (a < b) {
            a = b;
        }
        if (a < c) {
            a = c;
        }
        return a;
    }

    public static final float max(float a, float b, float c) {
        if (a < b) {
            a = b;
        }
        if (a < c) {
            a = c;
        }
        return a;
    }

    public static final double max(double a, double b, double c) {
        if (a < b) {
            a = b;
        }
        if (a < c) {
            a = c;
        }
        return a;
    }

    public static final float max(float a, float b, float c, float d) {
        if (a < b) {
            a = b;
        }
        if (a < c) {
            a = c;
        }
        if (a < d) {
            a = d;
        }
        return a;
    }

    public static final float smoothStep(float a, float b, float x) {
        if (x <= a) {
            return 0;
        }
        if (x >= b) {
            return 1;
        }
        float t = clamp((x - a) / (b - a), 0.0f, 1.0f);
        return t * t * (3 - 2 * t);
    }

    public static final float frac(float x) {
        return x < 0 ? x - (int) x + 1 : x - (int) x;
    }

    /**
     * Computes a fast approximation to
     * <code>Math.pow(a, b)</code>. Adapted from
     * <url>http://www.dctsystems.co.uk/Software/power.html</url>.
     *
     * @param a a positive number
     * @param b a number
     * @return a^b
     */
    public static final float fastPow(float a, float b) {
        // adapted from: http://www.dctsystems.co.uk/Software/power.html
        float x = Float.floatToRawIntBits(a);
        x *= 1.0f / (1 << 23);
        x = x - 127;
        float y = x - (int) Math.floor(x);
        b *= x + (y - y * y) * 0.346607f;
        y = b - (int) Math.floor(b);
        y = (y - y * y) * 0.33971f;
        return Float.intBitsToFloat((int) ((b + 127 - y) * (1 << 23)));
    }
}