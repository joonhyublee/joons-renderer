package org.sunflow.image;

/**
 * This class allows spectral curves to be defined from irregularly sampled
 * data. Note that the wavelength array is assumed to be sorted low to high. Any
 * values beyond the defined range will simply be extended to infinity from the
 * end points. Points inside the valid range will be linearly interpolated
 * between the two nearest samples. No explicit error checking is performed, but
 * this class will run into {@link ArrayIndexOutOfBoundsException}s if the array
 * lengths don't match.
 */
public class IrregularSpectralCurve extends SpectralCurve {

    private final float[] wavelengths;
    private final float[] amplitudes;

    /**
     * Define an irregular spectral curve from the provided (sorted) wavelengths
     * and amplitude data. The wavelength array is assumed to contain values in
     * nanometers. Array lengths must match.
     *
     * @param wavelengths sampled wavelengths in nm
     * @param amplitudes amplitude of the curve at the sampled points
     */
    public IrregularSpectralCurve(float[] wavelengths, float[] amplitudes) {
        this.wavelengths = wavelengths;
        this.amplitudes = amplitudes;
        if (wavelengths.length != amplitudes.length) {
            throw new RuntimeException(String.format("Error creating irregular spectral curve: %d wavelengths and %d amplitudes", wavelengths.length, amplitudes.length));
        }
        for (int i = 1; i < wavelengths.length; i++) {
            if (wavelengths[i - 1] >= wavelengths[i]) {
                throw new RuntimeException(String.format("Error creating irregular spectral curve: values are not sorted - error at index %d", i));
            }
        }
    }

    @Override
    public float sample(float lambda) {
        if (wavelengths.length == 0) {
            return 0; // no data
        }
        if (wavelengths.length == 1 || lambda <= wavelengths[0]) {
            return amplitudes[0];
        }
        if (lambda >= wavelengths[wavelengths.length - 1]) {
            return amplitudes[wavelengths.length - 1];
        }
        for (int i = 1; i < wavelengths.length; i++) {
            if (lambda < wavelengths[i]) {
                float dx = (lambda - wavelengths[i - 1]) / (wavelengths[i] - wavelengths[i - 1]);
                return (1 - dx) * amplitudes[i - 1] + dx * amplitudes[i];
            }
        }
        return amplitudes[wavelengths.length - 1];
    }
}