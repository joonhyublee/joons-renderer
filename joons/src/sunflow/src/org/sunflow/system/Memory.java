package org.sunflow.system;

public final class Memory {

    public static String sizeof(int[] array) {
        return bytesToString(array == null ? 0 : 4 * array.length);
    }

    public static String bytesToString(long bytes) {
        if (bytes < 1024) {
            return String.format("%db", bytes);
        }
        if (bytes < 1024 * 1024) {
            return String.format("%dKb", (bytes + 512) >>> 10);
        }
        return String.format("%dMb", (bytes + 512 * 1024) >>> 20);
    }
}