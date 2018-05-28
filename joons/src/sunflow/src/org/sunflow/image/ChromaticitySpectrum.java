package org.sunflow.image;

/**
 * This spectral curve represents a given (x,y) chromaticity pair as explained
 * in the sun/sky paper (section A.5)
 */
public final class ChromaticitySpectrum extends SpectralCurve {

    private static final float[] S0Amplitudes = {0.04f, 6.0f, 29.6f, 55.3f,
        57.3f, 61.8f, 61.5f, 68.8f, 63.4f, 65.8f, 94.8f, 104.8f, 105.9f,
        96.8f, 113.9f, 125.6f, 125.5f, 121.3f, 121.3f, 113.5f, 113.1f,
        110.8f, 106.5f, 108.8f, 105.3f, 104.4f, 100.0f, 96.0f, 95.1f,
        89.1f, 90.5f, 90.3f, 88.4f, 84.0f, 85.1f, 81.9f, 82.6f, 84.9f,
        81.3f, 71.9f, 74.3f, 76.4f, 63.3f, 71.7f, 77.0f, 65.2f, 47.7f,
        68.6f, 65.0f, 66.0f, 61.0f, 53.3f, 58.9f, 61.9f};
    private static final float[] S1Amplitudes = {0.02f, 4.5f, 22.4f, 42.0f,
        40.6f, 41.6f, 38.0f, 42.4f, 38.5f, 35.0f, 43.4f, 46.3f, 43.9f,
        37.1f, 36.7f, 35.9f, 32.6f, 27.9f, 24.3f, 20.1f, 16.2f, 13.2f,
        8.6f, 6.1f, 4.2f, 1.9f, 0.0f, -1.6f, -3.5f, -3.5f, -5.8f, -7.2f,
        -8.6f, -9.5f, -10.9f, -10.7f, -12.0f, -14.0f, -13.6f, -12.0f,
        -13.3f, -12.9f, -10.6f, -11.6f, -12.2f, -10.2f, -7.8f, -11.2f,
        -10.4f, -10.6f, -9.7f, -8.3f, -9.3f, -9.8f};
    private static final float[] S2Amplitudes = {0.0f, 2.0f, 4.0f, 8.5f, 7.8f,
        6.7f, 5.3f, 6.1f, 3.0f, 1.2f, -1.1f, -0.5f, -0.7f, -1.2f, -2.6f,
        -2.9f, -2.8f, -2.6f, -2.6f, -1.8f, -1.5f, -1.3f, -1.2f, -1.0f,
        -0.5f, -0.3f, 0.0f, 0.2f, 0.5f, 2.1f, 3.2f, 4.1f, 4.7f, 5.1f, 6.7f,
        7.3f, 8.6f, 9.8f, 10.2f, 8.3f, 9.6f, 8.5f, 7.0f, 7.6f, 8.0f, 6.7f,
        5.2f, 7.4f, 6.8f, 7.0f, 6.4f, 5.5f, 6.1f, 6.5f};
    private static final RegularSpectralCurve kS0Spectrum = new RegularSpectralCurve(S0Amplitudes, 300, 830);
    private static final RegularSpectralCurve kS1Spectrum = new RegularSpectralCurve(S1Amplitudes, 300, 830);
    private static final RegularSpectralCurve kS2Spectrum = new RegularSpectralCurve(S2Amplitudes, 300, 830);
    private static final XYZColor S0xyz = kS0Spectrum.toXYZ();
    private static final XYZColor S1xyz = kS1Spectrum.toXYZ();
    private static final XYZColor S2xyz = kS2Spectrum.toXYZ();
    private final float M1, M2;

    public ChromaticitySpectrum(float x, float y) {
        M1 = (-1.3515f - 1.7703f * x + 5.9114f * y) / (0.0241f + 0.2562f * x - 0.7341f * y);
        M2 = (0.03f - 31.4424f * x + 30.0717f * y) / (0.0241f + 0.2562f * x - 0.7341f * y);
    }

    @Override
    public float sample(float lambda) {
        return kS0Spectrum.sample(lambda) + M1 * kS1Spectrum.sample(lambda) + M2 * kS2Spectrum.sample(lambda);
    }

    public static final XYZColor get(float x, float y) {
        float M1 = (-1.3515f - 1.7703f * x + 5.9114f * y) / (0.0241f + 0.2562f * x - 0.7341f * y);
        float M2 = (0.03f - 31.4424f * x + 30.0717f * y) / (0.0241f + 0.2562f * x - 0.7341f * y);
        float X = S0xyz.getX() + M1 * S1xyz.getX() + M2 * S2xyz.getX();
        float Y = S0xyz.getY() + M1 * S1xyz.getY() + M2 * S2xyz.getY();
        float Z = S0xyz.getZ() + M1 * S1xyz.getZ() + M2 * S2xyz.getZ();
        return new XYZColor(X, Y, Z);
    }
}