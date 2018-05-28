package org.sunflow.image;

public class BlackbodySpectrum extends SpectralCurve {

    private float temp;

    public BlackbodySpectrum(float temp) {
        this.temp = temp;
    }

    @Override
    public float sample(float lambda) {
        double wavelength = lambda * 1e-9;
        return (float) ((3.74183e-16 * Math.pow(wavelength, -5.0)) / (Math.exp(1.4388e-2 / (wavelength * temp)) - 1.0));
    }
}