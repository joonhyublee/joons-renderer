package org.sunflow.image;

public class RegularSpectralCurve extends SpectralCurve {

    private final float[] spectrum;
    private final float lambdaMin, lambdaMax;
    private final float delta, invDelta;

    public RegularSpectralCurve(float[] spectrum, float lambdaMin, float lambdaMax) {
        this.lambdaMin = lambdaMin;
        this.lambdaMax = lambdaMax;
        this.spectrum = spectrum;
        delta = (lambdaMax - lambdaMin) / (spectrum.length - 1);
        invDelta = 1 / delta;
    }

    @Override
    public float sample(float lambda) {
        // reject wavelengths outside the valid range
        if (lambda < lambdaMin || lambda > lambdaMax) {
            return 0;
        }
        // interpolate the two closest samples linearly
        float x = (lambda - lambdaMin) * invDelta;
        int b0 = (int) x;
        int b1 = Math.min(b0 + 1, spectrum.length - 1);
        float dx = x - b0;
        return (1 - dx) * spectrum[b0] + dx * spectrum[b1];
    }
}