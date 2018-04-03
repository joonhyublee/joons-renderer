package org.sunflow.system;

public class Timer {

    private long startTime, endTime;

    public Timer() {
        startTime = endTime = 0;
    }

    public void start() {
        startTime = endTime = System.nanoTime();
    }

    public void end() {
        endTime = System.nanoTime();
    }

    public long nanos() {
        return endTime - startTime;
    }

    public double seconds() {
        return (endTime - startTime) * 1e-9;
    }

    public static String toString(long nanos) {
        Timer t = new Timer();
        t.endTime = nanos;
        return t.toString();
    }

    public static String toString(double seconds) {
        Timer t = new Timer();
        t.endTime = (long) (seconds * 1e9);
        return t.toString();
    }

    @Override
    public String toString() {
        long millis = nanos() / (1000 * 1000);
        if (millis < 10000) {
            return String.format("%dms", millis);
        }
        long hours = millis / (60 * 60 * 1000);
        millis -= hours * 60 * 60 * 1000;
        long minutes = millis / (60 * 1000);
        millis -= minutes * 60 * 1000;
        long seconds = millis / 1000;
        millis -= seconds * 1000;
        return String.format("%d:%02d:%02d.%1d", hours, minutes, seconds, millis / 100);
    }
}