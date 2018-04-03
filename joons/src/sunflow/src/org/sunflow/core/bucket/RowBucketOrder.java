package org.sunflow.core.bucket;

import org.sunflow.core.BucketOrder;

public class RowBucketOrder implements BucketOrder {

    @Override
    public int[] getBucketSequence(int nbw, int nbh) {
        int[] coords = new int[2 * nbw * nbh];
        for (int i = 0; i < nbw * nbh; i++) {
            int by = i / nbw;
            int bx = i % nbw;
            if ((by & 1) == 1) {
                bx = nbw - 1 - bx;
            }
            coords[2 * i + 0] = bx;
            coords[2 * i + 1] = by;
        }
        return coords;
    }
}