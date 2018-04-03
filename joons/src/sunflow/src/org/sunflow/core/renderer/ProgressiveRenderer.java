package org.sunflow.core.renderer;

import java.util.concurrent.PriorityBlockingQueue;

import org.sunflow.core.Display;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.QMC;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class ProgressiveRenderer implements ImageSampler {

    private Scene scene;
    private int imageWidth, imageHeight;
    private PriorityBlockingQueue<SmallBucket> smallBucketQueue;
    private Display display;
    private int counter, counterMax;

    public ProgressiveRenderer() {
        imageWidth = 640;
        imageHeight = 480;
        smallBucketQueue = null;
    }

    public boolean prepare(Options options, Scene scene, int w, int h) {
        this.scene = scene;
        imageWidth = w;
        imageHeight = h;
        // prepare table used by deterministic anti-aliasing
        return true;
    }

    public void render(Display display) {
        this.display = display;
        display.imageBegin(imageWidth, imageHeight, 0);
        // create first bucket
        SmallBucket b = new SmallBucket();
        b.x = b.y = 0;
        int s = Math.max(imageWidth, imageHeight);
        b.size = 1;
        while (b.size < s) {
            b.size <<= 1;
        }
        smallBucketQueue = new PriorityBlockingQueue<SmallBucket>();
        smallBucketQueue.add(b);
        UI.taskStart("Progressive Render", 0, imageWidth * imageHeight);
        Timer t = new Timer();
        t.start();
        counter = 0;
        counterMax = imageWidth * imageHeight;

        SmallBucketThread[] renderThreads = new SmallBucketThread[scene.getThreads()];
        for (int i = 0; i < renderThreads.length; i++) {
            renderThreads[i] = new SmallBucketThread();
            renderThreads[i].start();
        }
        for (int i = 0; i < renderThreads.length; i++) {
            try {
                renderThreads[i].join();
            } catch (InterruptedException e) {
                UI.printError(Module.IPR, "Thread %d of %d was interrupted", i + 1, renderThreads.length);
            } finally {
                renderThreads[i].updateStats();
            }
        }
        UI.taskStop();
        t.end();
        UI.printInfo(Module.IPR, "Rendering time: %s", t.toString());
        display.imageEnd();
    }

    private class SmallBucketThread extends Thread {

        private final IntersectionState istate = new IntersectionState();

        @Override
        public void run() {
            while (true) {
                int n = progressiveRenderNext(istate);
                synchronized (ProgressiveRenderer.this) {
                    if (counter >= counterMax) {
                        return;
                    }
                    counter += n;
                    UI.taskUpdate(counter);
                }
                if (UI.taskCanceled()) {
                    return;
                }
            }
        }

        void updateStats() {
            scene.accumulateStats(istate);
        }
    }

    private int progressiveRenderNext(IntersectionState istate) {
        final int TASK_SIZE = 16;
        SmallBucket first = smallBucketQueue.poll();
        if (first == null) {
            return 0;
        }
        int ds = first.size / TASK_SIZE;
        boolean useMask = !smallBucketQueue.isEmpty();
        int mask = 2 * first.size / TASK_SIZE - 1;
        int pixels = 0;
        for (int i = 0, y = first.y; i < TASK_SIZE && y < imageHeight; i++, y += ds) {
            for (int j = 0, x = first.x; j < TASK_SIZE && x < imageWidth; j++, x += ds) {
                // check to see if this is a pixel from a higher level tile
                if (useMask && (x & mask) == 0 && (y & mask) == 0) {
                    continue;
                }
                int instance = ((x & ((1 << QMC.MAX_SIGMA_ORDER) - 1)) << QMC.MAX_SIGMA_ORDER) + QMC.sigma(y & ((1 << QMC.MAX_SIGMA_ORDER) - 1), QMC.MAX_SIGMA_ORDER);
                double time = QMC.halton(1, instance);
                double lensU = QMC.halton(2, instance);
                double lensV = QMC.halton(3, instance);
                ShadingState state = scene.getRadiance(istate, x, imageHeight - 1 - y, lensU, lensV, time, instance, 4, null);
                Color c = state != null ? state.getResult() : Color.BLACK;
                pixels++;
                // fill region
                display.imageFill(x, y, Math.min(ds, imageWidth - x), Math.min(ds, imageHeight - y), c, state == null ? 0 : 1);
            }
        }
        if (first.size >= 2 * TASK_SIZE) {
            // generate child buckets
            int size = first.size >>> 1;
            for (int i = 0; i < 2; i++) {
                if (first.y + i * size < imageHeight) {
                    for (int j = 0; j < 2; j++) {
                        if (first.x + j * size < imageWidth) {
                            SmallBucket b = new SmallBucket();
                            b.x = first.x + j * size;
                            b.y = first.y + i * size;
                            b.size = size;
                            b.constrast = 1.0f / size;
                            smallBucketQueue.put(b);
                        }
                    }
                }
            }
        }
        return pixels;
    }

    // progressive rendering
    private static class SmallBucket implements Comparable<SmallBucket> {

        int x, y, size;
        float constrast;

        public int compareTo(SmallBucket o) {
            if (constrast < o.constrast) {
                return -1;
            }
            if (constrast == o.constrast) {
                return 0;
            }
            return 1;
        }
    }
}