package org.sunflow.core.bucket;

import org.sunflow.core.BucketOrder;

public class ColumnBucketOrder implements BucketOrder {

    @Override
    public int[] getBucketSequence(int nbw, int nbh) {
        int[] coords = new int[2 * nbw * nbh];
        for (int i = 0; i < nbw * nbh; i++) {
            int bx = i / nbh;
            int by = i % nbh;
            if ((bx & 1) == 1) {
                by = nbh - 1 - by;
            }
            coords[2 * i + 0] = bx;
            coords[2 * i + 1] = by;
        }
        return coords;
    }
}