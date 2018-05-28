package org.sunflow.core.bucket;

import org.sunflow.core.BucketOrder;

public class InvertedBucketOrder implements BucketOrder {

    private BucketOrder order;

    public InvertedBucketOrder(BucketOrder order) {
        this.order = order;
    }

    @Override
    public int[] getBucketSequence(int nbw, int nbh) {
        int[] coords = order.getBucketSequence(nbw, nbh);
        for (int i = 0; i < coords.length / 2; i += 2) {
            int src = i;
            int dst = coords.length - 2 - i;
            int tmp = coords[src + 0];
            coords[src + 0] = coords[dst + 0];
            coords[dst + 0] = tmp;
            tmp = coords[src + 1];
            coords[src + 1] = coords[dst + 1];
            coords[dst + 1] = tmp;
        }
        return coords;
    }
}