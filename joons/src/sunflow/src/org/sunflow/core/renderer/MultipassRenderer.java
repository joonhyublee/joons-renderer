package org.sunflow.core.renderer;

import org.sunflow.core.BucketOrder;
import org.sunflow.core.Display;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingCache;
import org.sunflow.core.ShadingState;
import org.sunflow.core.bucket.BucketOrderFactory;
import org.sunflow.image.Color;
import org.sunflow.math.MathUtils;
import org.sunflow.math.QMC;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class MultipassRenderer implements ImageSampler {

    private Scene scene;
    private Display display;
    // resolution
    private int imageWidth;
    private int imageHeight;
    // bucketing
    private String bucketOrderName;
    private BucketOrder bucketOrder;
    private int bucketSize;
    private int bucketCounter;
    private int[] bucketCoords;
    // anti-aliasing
    private int numSamples;
    private float invNumSamples;
    private boolean shadingCache;

    public MultipassRenderer() {
        bucketSize = 32;
        bucketOrderName = "hilbert";
        numSamples = 16;
        shadingCache = false;
    }

    public boolean prepare(Options options, Scene scene, int w, int h) {
        this.scene = scene;
        imageWidth = w;
        imageHeight = h;

        // fetch options
        bucketSize = options.getInt("bucket.size", bucketSize);
        bucketOrderName = options.getString("bucket.order", bucketOrderName);
        numSamples = options.getInt("aa.samples", numSamples);
        shadingCache = options.getBoolean("aa.cache", shadingCache);

        // limit bucket size and compute number of buckets in each direction
        bucketSize = MathUtils.clamp(bucketSize, 16, 512);
        int numBucketsX = (imageWidth + bucketSize - 1) / bucketSize;
        int numBucketsY = (imageHeight + bucketSize - 1) / bucketSize;
        bucketOrder = BucketOrderFactory.create(bucketOrderName);
        bucketCoords = bucketOrder.getBucketSequence(numBucketsX, numBucketsY);
        // validate AA options
        numSamples = Math.max(1, numSamples);
        invNumSamples = 1.0f / numSamples;
        // prepare QMC sampling
        UI.printInfo(Module.BCKT, "Multipass renderer settings:");
        UI.printInfo(Module.BCKT, "  * Resolution:         %dx%d", imageWidth, imageHeight);
        UI.printInfo(Module.BCKT, "  * Bucket size:        %d", bucketSize);
        UI.printInfo(Module.BCKT, "  * Number of buckets:  %dx%d", numBucketsX, numBucketsY);
        UI.printInfo(Module.BCKT, "  * Samples / pixel:    %d", numSamples);
        UI.printInfo(Module.BCKT, "  * Shading cache:      %s", shadingCache ? "enabled" : "disabled");
        return true;
    }

    public void render(Display display) {
        this.display = display;
        display.imageBegin(imageWidth, imageHeight, bucketSize);
        // set members variables
        bucketCounter = 0;
        // start task
        Timer timer = new Timer();
        timer.start();
        UI.taskStart("Rendering", 0, bucketCoords.length);
        BucketThread[] renderThreads = new BucketThread[scene.getThreads()];
        for (int i = 0; i < renderThreads.length; i++) {
            renderThreads[i] = new BucketThread(i);
            renderThreads[i].setPriority(scene.getThreadPriority());
            renderThreads[i].start();
        }
        for (int i = 0; i < renderThreads.length; i++) {
            try {
                renderThreads[i].join();
            } catch (InterruptedException e) {
                UI.printError(Module.BCKT, "Bucket processing thread %d of %d was interrupted", i + 1, renderThreads.length);
            } finally {
                renderThreads[i].updateStats();
            }
        }
        UI.taskStop();
        timer.end();
        UI.printInfo(Module.BCKT, "Render time: %s", timer.toString());
        display.imageEnd();
    }

    private class BucketThread extends Thread {

        private final int threadID;
        private final IntersectionState istate;
        private final ShadingCache cache;

        BucketThread(int threadID) {
            this.threadID = threadID;
            istate = new IntersectionState();
            cache = shadingCache ? new ShadingCache() : null;
        }

        @Override
        public void run() {
            while (true) {
                int bx, by;
                synchronized (MultipassRenderer.this) {
                    if (bucketCounter >= bucketCoords.length) {
                        return;
                    }
                    UI.taskUpdate(bucketCounter);
                    bx = bucketCoords[bucketCounter + 0];
                    by = bucketCoords[bucketCounter + 1];
                    bucketCounter += 2;
                }
                renderBucket(display, bx, by, threadID, istate, cache);
            }
        }

        void updateStats() {
            scene.accumulateStats(istate);
            if (shadingCache) {
                scene.accumulateStats(cache);
            }
        }
    }

    private void renderBucket(Display display, int bx, int by, int threadID, IntersectionState istate, ShadingCache cache) {
        // pixel sized extents
        int x0 = bx * bucketSize;
        int y0 = by * bucketSize;
        int bw = Math.min(bucketSize, imageWidth - x0);
        int bh = Math.min(bucketSize, imageHeight - y0);

        // prepare bucket
        display.imagePrepare(x0, y0, bw, bh, threadID);

        Color[] bucketRGB = new Color[bw * bh];
        float[] bucketAlpha = new float[bw * bh];

        for (int y = 0, i = 0, cy = imageHeight - 1 - y0; y < bh; y++, cy--) {
            for (int x = 0, cx = x0; x < bw; x++, i++, cx++) {
                // sample pixel
                Color c = Color.black();
                float a = 0;
                int instance = ((cx & ((1 << QMC.MAX_SIGMA_ORDER) - 1)) << QMC.MAX_SIGMA_ORDER) + QMC.sigma(cy & ((1 << QMC.MAX_SIGMA_ORDER) - 1), QMC.MAX_SIGMA_ORDER);
                double jitterX = QMC.halton(0, instance);
                double jitterY = QMC.halton(1, instance);
                double jitterT = QMC.halton(2, instance);
                double jitterU = QMC.halton(3, instance);
                double jitterV = QMC.halton(4, instance);
                for (int s = 0; s < numSamples; s++) {
                    float rx = cx + 0.5f + (float) warpCubic(QMC.mod1(jitterX + s * invNumSamples));
                    float ry = cy + 0.5f + (float) warpCubic(QMC.mod1(jitterY + QMC.halton(0, s)));
                    double time = QMC.mod1(jitterT + QMC.halton(1, s));
                    double lensU = QMC.mod1(jitterU + QMC.halton(2, s));
                    double lensV = QMC.mod1(jitterV + QMC.halton(3, s));
                    ShadingState state = scene.getRadiance(istate, rx, ry, lensU, lensV, time, instance + s, 5, cache);
                    if (state != null) {
                        c.add(state.getResult());
                        a++;
                    }
                }
                bucketRGB[i] = c.mul(invNumSamples);
                bucketAlpha[i] = a * invNumSamples;
                if (cache != null) {
                    cache.reset();
                }
            }
        }
        // update pixels
        display.imageUpdate(x0, y0, bw, bh, bucketRGB, bucketAlpha);
    }

    /**
     * Tent filter warping function.
     *
     * @param x sample in the [0,1) range
     * @return warped sample in the [-1,+1) range
     */
    @SuppressWarnings("unused")
    private static final float warpTent(float x) {
        if (x < 0.5f) {
            return -1 + (float) Math.sqrt(2 * x);
        } else {
            return +1 - (float) Math.sqrt(2 - 2 * x);
        }
    }

    /**
     * Cubic BSpline warping functions. Formulas from: "Generation of Stratified
     * Samples for B-Spline Pixel Filtering"
     * http://www.cs.utah.edu/~mstark/papers/
     *
     * @param x samples in the [0,1) range
     * @return warped sample in the [-2,+2) range
     */
    private static final double warpCubic(double x) {
        if (x < (1.0 / 24)) {
            return qpow(24 * x) - 2;
        }
        if (x < 0.5f) {
            return distb1((24.0 / 11.0) * (x - (1.0 / 24.0))) - 1;
        }
        if (x < (23.0f / 24)) {
            return 1 - distb1((24.0 / 11.0) * ((23.0 / 24.0) - x));
        }
        return 2 - qpow(24 * (1 - x));
    }

    private static final double qpow(double x) {
        return Math.sqrt(Math.sqrt(x));
    }

    private static final double distb1(double x) {
        double u = x;
        for (int i = 0; i < 5; i++) {
            u = (11 * x + u * u * (6 + u * (8 - 9 * u))) / (4 + 12 * u * (1 + u * (1 - u)));
        }
        return u;
    }
}