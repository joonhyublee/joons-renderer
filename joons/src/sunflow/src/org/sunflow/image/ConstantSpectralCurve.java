package org.sunflow.image;

/**
 * Very simple class equivalent to a constant spectral curve. Note that this is
 * most likely physically impossible for amplitudes > 0, however this class can
 * be handy since in practice spectral curves end up being integrated against
 * the finite width color matching functions.
 */
public class ConstantSpectralCurve extends SpectralCurve {

    private final float amp;

    public ConstantSpectralCurve(float amp) {
        this.amp = amp;
    }

    @Override
    public float sample(float lambda) {
        return amp;
    }
}