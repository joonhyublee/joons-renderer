package org.sunflow.core.bucket;

import org.sunflow.core.BucketOrder;

public class SpiralBucketOrder implements BucketOrder {

    @Override
    public int[] getBucketSequence(int nbw, int nbh) {
        int[] coords = new int[2 * nbw * nbh];
        for (int i = 0; i < nbw * nbh; i++) {
            int bx, by;
            int center = (Math.min(nbw, nbh) - 1) / 2;
            int nx = nbw;
            int ny = nbh;
            while (i < (nx * ny)) {
                nx--;
                ny--;
            }
            int nxny = nx * ny;
            int minnxny = Math.min(nx, ny);
            if ((minnxny & 1) == 1) {
                if (i <= (nxny + ny)) {
                    bx = nx - minnxny / 2;
                    by = -minnxny / 2 + i - nxny;
                } else {
                    bx = nx - minnxny / 2 - (i - (nxny + ny));
                    by = ny - minnxny / 2;
                }
            } else {
                if (i <= (nxny + ny)) {
                    bx = -minnxny / 2;
                    by = ny - minnxny / 2 - (i - nxny);
                } else {
                    bx = -minnxny / 2 + (i - (nxny + ny));
                    by = -minnxny / 2;
                }
            }
            coords[2 * i + 0] = bx + center;
            coords[2 * i + 1] = by + center;
        }
        return coords;
    }
}