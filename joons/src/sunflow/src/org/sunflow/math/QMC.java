package org.sunflow.math;

import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public final class QMC {

    public static final int MAX_SIGMA_ORDER = 15;
    private static final int NUM = 128;
    private static final int[][] SIGMA = new int[NUM][];
    private static final int[] PRIMES = new int[NUM];
    private static final int[] FIBONACCI = new int[47];
    private static final double[] FIBONACCI_INV = new double[FIBONACCI.length];
    private static final double[] KOROBOV = new double[NUM];

    static {
        UI.printInfo(Module.QMC, "Initializing Faure scrambling tables ...");
        // build table of first primes
        PRIMES[0] = 2;
        for (int i = 1; i < PRIMES.length; i++) {
            PRIMES[i] = nextPrime(PRIMES[i - 1]);
        }
        int[][] table = new int[PRIMES[PRIMES.length - 1] + 1][];
        table[2] = new int[2];
        table[2][0] = 0;
        table[2][1] = 1;
        for (int i = 3; i <= PRIMES[PRIMES.length - 1]; i++) {
            table[i] = new int[i];
            if ((i & 1) == 0) {
                int[] prev = table[i >> 1];
                for (int j = 0; j < prev.length; j++) {
                    table[i][j] = 2 * prev[j];
                }
                for (int j = 0; j < prev.length; j++) {
                    table[i][prev.length + j] = 2 * prev[j] + 1;
                }
            } else {
                int[] prev = table[i - 1];
                int med = (i - 1) >> 1;
                for (int j = 0; j < med; j++) {
                    table[i][j] = prev[j] + ((prev[j] >= med) ? 1 : 0);
                }
                table[i][med] = med;
                for (int j = 0; j < med; j++) {
                    table[i][med + j + 1] = prev[j + med] + ((prev[j + med] >= med) ? 1 : 0);
                }
            }
        }
        for (int i = 0; i < PRIMES.length; i++) {
            int p = PRIMES[i];
            SIGMA[i] = new int[p];
            System.arraycopy(table[p], 0, SIGMA[i], 0, p);
        }
        UI.printInfo(Module.QMC, "Initializing lattice tables ...");
        FIBONACCI[0] = 0;
        FIBONACCI[1] = 1;
        for (int i = 2; i < FIBONACCI.length; i++) {
            FIBONACCI[i] = FIBONACCI[i - 1] + FIBONACCI[i - 2];
            FIBONACCI_INV[i] = 1.0 / FIBONACCI[i];
        }
        KOROBOV[0] = 1;
        for (int i = 1; i < KOROBOV.length; i++) {
            KOROBOV[i] = 203 * KOROBOV[i - 1];
        }
    }

    private static final int nextPrime(int p) {
        p = p + (p & 1) + 1;
        while (true) {
            int div = 3;
            boolean isPrime = true;
            while (isPrime && ((div * div) <= p)) {
                isPrime = ((p % div) != 0);
                div += 2;
            }
            if (isPrime) {
                return p;
            }
            p += 2;
        }
    }

    private QMC() {
    }

    public static double riVDC(int bits, int r) {
        bits = (bits << 16) | (bits >>> 16);
        bits = ((bits & 0x00ff00ff) << 8) | ((bits & 0xff00ff00) >>> 8);
        bits = ((bits & 0x0f0f0f0f) << 4) | ((bits & 0xf0f0f0f0) >>> 4);
        bits = ((bits & 0x33333333) << 2) | ((bits & 0xcccccccc) >>> 2);
        bits = ((bits & 0x55555555) << 1) | ((bits & 0xaaaaaaaa) >>> 1);
        bits ^= r;
        return (double) (bits & 0xFFFFFFFFL) / (double) 0x100000000L;
    }

    public static double riS(int i, int r) {
        for (int v = 1 << 31; i != 0; i >>>= 1, v ^= v >>> 1) {
            if ((i & 1) != 0) {
                r ^= v;
            }
        }
        return (double) (r & 0xFFFFFFFFL) / (double) 0x100000000L;
    }

    public static double riLP(int i, int r) {
        for (int v = 1 << 31; i != 0; i >>>= 1, v |= v >>> 1) {
            if ((i & 1) != 0) {
                r ^= v;
            }
        }
        return (double) (r & 0xFFFFFFFFL) / (double) 0x100000000L;
    }

    public static final double halton(int d, int i) {
        // generalized Halton sequence
        switch (d) {
            case 0: {
                i = (i << 16) | (i >>> 16);
                i = ((i & 0x00ff00ff) << 8) | ((i & 0xff00ff00) >>> 8);
                i = ((i & 0x0f0f0f0f) << 4) | ((i & 0xf0f0f0f0) >>> 4);
                i = ((i & 0x33333333) << 2) | ((i & 0xcccccccc) >>> 2);
                i = ((i & 0x55555555) << 1) | ((i & 0xaaaaaaaa) >>> 1);
                return (double) (i & 0xFFFFFFFFL) / (double) 0x100000000L;
            }
            case 1: {
                double v = 0;
                double inv = 1.0 / 3;
                double p;
                int n;
                for (p = inv, n = i; n != 0; p *= inv, n /= 3) {
                    v += (n % 3) * p;
                }
                return v;
            }
            default:
        }
        int base = PRIMES[d];
        int[] perm = SIGMA[d];
        double v = 0;
        double inv = 1.0 / base;
        double p;
        int n;
        for (p = inv, n = i; n != 0; p *= inv, n /= base) {
            v += perm[n % base] * p;
        }
        return v;
    }

    /**
     * Compute mod(x,1), assuming that x is positive or 0.
     *
     * @param x any number >= 0
     * @return mod(x,1)
     */
    public static final double mod1(double x) {
        // assumes x >= 0
        return x - (int) x;
    }

    /**
     * Compute sigma function used to seed QMC sequence trees. The sigma table
     * is exactly 2^order elements long, and therefore i should be in the: [0,
     * 2^order) interval. This function is equal to 2^order*halton(0,i)
     *
     * @param i index
     * @param order
     * @return sigma function
     */
    public static final int sigma(int i, int order) {
        assert order > 0 && order < 32;
        assert i >= 0 && i < (1 << order);
        i = (i << 16) | (i >>> 16);
        i = ((i & 0x00ff00ff) << 8) | ((i & 0xff00ff00) >>> 8);
        i = ((i & 0x0f0f0f0f) << 4) | ((i & 0xf0f0f0f0) >>> 4);
        i = ((i & 0x33333333) << 2) | ((i & 0xcccccccc) >>> 2);
        i = ((i & 0x55555555) << 1) | ((i & 0xaaaaaaaa) >>> 1);
        return i >>> (32 - order);
    }

    public static final int getFibonacciRank(int n) {
        int k = 3;
        while (FIBONACCI[k] <= n) {
            k++;
        }
        return k - 1;
    }

    public static final int fibonacci(int k) {
        return FIBONACCI[k];
    }

    public static final double fibonacciLattice(int k, int i, int d) {
        return d == 0 ? i * FIBONACCI_INV[k] : mod1((i * FIBONACCI[k - 1]) * FIBONACCI_INV[k]);
    }

    public static final double reducedCPRotation(int k, int d, double x0, double x1) {
        int j1 = FIBONACCI[2 * ((k - 1) >> 2) + 1];
        int j2 = FIBONACCI[2 * ((k + 1) >> 2)];
        if (d == 1) {
            j1 = ((j1 * FIBONACCI[k - 1]) % FIBONACCI[k]);
            j2 = ((j2 * FIBONACCI[k - 1]) % FIBONACCI[k]) - FIBONACCI[k];
        }
        return (x0 * j1 + x1 * j2) * FIBONACCI_INV[k];
    }

    public static final double korobovLattice(int m, int i, int d) {
        return mod1(i * KOROBOV[d] / (1 << m));
    }
}