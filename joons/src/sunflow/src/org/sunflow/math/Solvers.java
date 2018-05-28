package org.sunflow.math;

public final class Solvers {

    /**
     * Solves the equation ax^2+bx+c=0. Solutions are returned in a sorted array
     * if they exist.
     *
     * @param a coefficient of x^2
     * @param b coefficient of x^1
     * @param c coefficient of x^0
     * @return an array containing the two real roots, or <code>null</code> if
     * no real solutions exist
     */
    public static double[] solveQuadric(double a, double b, double c) {
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return null;
        }
        disc = Math.sqrt(disc);
        double q = ((b < 0) ? -0.5 * (b - disc) : -0.5 * (b + disc));
        double t0 = q / a;
        double t1 = c / q;
        // return sorted array
        return (t0 > t1) ? new double[]{t1, t0} : new double[]{t0, t1};
    }

    /**
     * Solve a quartic equation of the form ax^4+bx^3+cx^2+cx^1+d=0. The roots
     * are returned in a sorted array of doubles in increasing order.
     *
     * @param a coefficient of x^4
     * @param b coefficient of x^3
     * @param c coefficient of x^2
     * @param d coefficient of x^1
     * @param e coefficient of x^0
     * @return a sorted array of roots, or <code>null</code> if no solutions
     * exist
     */
    public static double[] solveQuartic(double a, double b, double c, double d, double e) {
        double inva = 1 / a;
        double c1 = b * inva;
        double c2 = c * inva;
        double c3 = d * inva;
        double c4 = e * inva;
        // cubic resolvant
        double c12 = c1 * c1;
        double p = -0.375 * c12 + c2;
        double q = 0.125 * c12 * c1 - 0.5 * c1 * c2 + c3;
        double r = -0.01171875 * c12 * c12 + 0.0625 * c12 * c2 - 0.25 * c1 * c3 + c4;
        double z = solveCubicForQuartic(-0.5 * p, -r, 0.5 * r * p - 0.125 * q * q);
        double d1 = 2.0 * z - p;
        if (d1 < 0) {
            if (d1 > 1.0e-10) {
                d1 = 0;
            } else {
                return null;
            }
        }
        double d2;
        if (d1 < 1.0e-10) {
            d2 = z * z - r;
            if (d2 < 0) {
                return null;
            }
            d2 = Math.sqrt(d2);
        } else {
            d1 = Math.sqrt(d1);
            d2 = 0.5 * q / d1;
        }
        // setup usefull values for the quadratic factors
        double q1 = d1 * d1;
        double q2 = -0.25 * c1;
        double pm = q1 - 4 * (z - d2);
        double pp = q1 - 4 * (z + d2);
        if (pm >= 0 && pp >= 0) {
            // 4 roots (!)
            pm = Math.sqrt(pm);
            pp = Math.sqrt(pp);
            double[] results = new double[4];
            results[0] = -0.5 * (d1 + pm) + q2;
            results[1] = -0.5 * (d1 - pm) + q2;
            results[2] = 0.5 * (d1 + pp) + q2;
            results[3] = 0.5 * (d1 - pp) + q2;
            // tiny insertion sort
            for (int i = 1; i < 4; i++) {
                for (int j = i; j > 0 && results[j - 1] > results[j]; j--) {
                    double t = results[j];
                    results[j] = results[j - 1];
                    results[j - 1] = t;
                }
            }
            return results;
        } else if (pm >= 0) {
            pm = Math.sqrt(pm);
            double[] results = new double[2];
            results[0] = -0.5 * (d1 + pm) + q2;
            results[1] = -0.5 * (d1 - pm) + q2;
            return results;
        } else if (pp >= 0) {
            pp = Math.sqrt(pp);
            double[] results = new double[2];
            results[0] = 0.5 * (d1 - pp) + q2;
            results[1] = 0.5 * (d1 + pp) + q2;
            return results;
        }
        return null;
    }

    /**
     * Return only one root for the specified cubic equation. This routine is
     * only meant to be called by the quartic solver. It assumes the cubic is of
     * the form: x^3+px^2+qx+r.
     *
     * @param p
     * @param q
     * @param r
     * @return
     */
    private static double solveCubicForQuartic(double p, double q, double r) {
        double A2 = p * p;
        double Q = (A2 - 3.0 * q) / 9.0;
        double R = (p * (A2 - 4.5 * q) + 13.5 * r) / 27.0;
        double Q3 = Q * Q * Q;
        double R2 = R * R;
        double d = Q3 - R2;
        double an = p / 3.0;
        if (d >= 0) {
            d = R / Math.sqrt(Q3);
            double theta = Math.acos(d) / 3.0;
            double sQ = -2.0 * Math.sqrt(Q);
            return sQ * Math.cos(theta) - an;
        } else {
            double sQ = Math.pow(Math.sqrt(R2 - Q3) + Math.abs(R), 1.0 / 3.0);
            if (R < 0) {
                return (sQ + Q / sQ) - an;
            } else {
                return -(sQ + Q / sQ) - an;
            }
        }
    }
}