package org.sunflow.system;

import org.sunflow.system.UI.Module;

/**
 * This class provides a very simple framework for running a BenchmarkTest
 * kernel several times and time the results.
 */
public class BenchmarkFramework {

    private Timer[] timers;
    private int timeLimit; // time limit in seconds

    public BenchmarkFramework(int iterations, int timeLimit) {
        this.timeLimit = timeLimit;
        timers = new Timer[iterations];
    }

    public void execute(BenchmarkTest test) {
        // clear previous results
        for (int i = 0; i < timers.length; i++) {
            timers[i] = null;
        }
        // loop for the specified number of iterations or until the time limit
        long startTime = System.nanoTime();
        for (int i = 0; i < timers.length && ((System.nanoTime() - startTime) / 1000000000) < timeLimit; i++) {
            UI.printInfo(Module.BENCH, "Running iteration %d", (i + 1));
            timers[i] = new Timer();
            test.kernelBegin();
            timers[i].start();
            test.kernelMain();
            timers[i].end();
            test.kernelEnd();
        }
        // report stats
        double avg = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int n = 0;
        for (Timer t : timers) {
            if (t == null) {
                break;
            }
            double s = t.seconds();
            min = Math.min(min, s);
            max = Math.max(max, s);
            avg += s;
            n++;
        }
        if (n == 0) {
            return;
        }
        avg /= n;
        double stdDev = 0;
        for (Timer t : timers) {
            if (t == null) {
                break;
            }
            double s = t.seconds();
            stdDev += (s - avg) * (s - avg);
        }
        stdDev = Math.sqrt(stdDev / n);
        UI.printInfo(Module.BENCH, "Benchmark results:");
        UI.printInfo(Module.BENCH, "  * Iterations: %d", n);
        UI.printInfo(Module.BENCH, "  * Average:    %s", Timer.toString(avg));
        UI.printInfo(Module.BENCH, "  * Fastest:    %s", Timer.toString(min));
        UI.printInfo(Module.BENCH, "  * Longest:    %s", Timer.toString(max));
        UI.printInfo(Module.BENCH, "  * Deviation:  %s", Timer.toString(stdDev));
        for (int i = 0; i < timers.length && timers[i] != null; i++) {
            UI.printDetailed(Module.BENCH, "  * Iteration %d: %s", i + 1, timers[i]);
        }
    }
}