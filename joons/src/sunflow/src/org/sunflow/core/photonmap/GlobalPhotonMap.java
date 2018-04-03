package org.sunflow.core.photonmap;

import java.util.ArrayList;

import org.sunflow.core.GlobalPhotonMapInterface;
import org.sunflow.core.Options;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public final class GlobalPhotonMap implements GlobalPhotonMapInterface {

    private ArrayList<Photon> photonList;
    private Photon[] photons;
    private int storedPhotons;
    private int halfStoredPhotons;
    private int log2n;
    private int numGather;
    private float gatherRadius;
    private BoundingBox bounds;
    private boolean hasRadiance;
    private float maxPower;
    private float maxRadius;
    private int numEmit;

    public GlobalPhotonMap() {
        bounds = new BoundingBox();
        hasRadiance = false;
        maxPower = 0;
        maxRadius = 0;
    }

    @Override
    public void prepare(Options options, BoundingBox sceneBounds) {
        // get settings
        numEmit = options.getInt("gi.irr-cache.gmap.emit", 100000);
        numGather = options.getInt("gi.irr-cache.gmap.gather", 50);
        gatherRadius = options.getFloat("gi.irr-cache.gmap.radius", 0.5f);
        // init
        photonList = new ArrayList<Photon>();
        photonList.add(null);
        photons = null;
        storedPhotons = halfStoredPhotons = 0;
    }

    @Override
    public void store(ShadingState state, Vector3 dir, Color power, Color diffuse) {
        Photon p = new Photon(state.getPoint(), state.getNormal(), dir, power, diffuse);
        synchronized (this) {
            storedPhotons++;
            photonList.add(p);
            bounds.include(new Point3(p.x, p.y, p.z));
            maxPower = Math.max(maxPower, power.getMax());
        }
    }

    private void locatePhotons(NearestPhotons np) {
        float[] dist1d2 = new float[log2n];
        int[] chosen = new int[log2n];
        int i = 1;
        int level = 0;
        int cameFrom;
        while (true) {
            while (i < halfStoredPhotons) {
                float dist1d = photons[i].getDist1(np.px, np.py, np.pz);
                dist1d2[level] = dist1d * dist1d;
                i += i;
                if (dist1d > 0.0f) {
                    i++;
                }
                chosen[level++] = i;
            }
            np.checkAddNearest(photons[i]);
            do {
                cameFrom = i;
                i >>= 1;
                level--;
                if (i == 0) {
                    return;
                }
            } while ((dist1d2[level] >= np.dist2[0]) || (cameFrom != chosen[level]));
            np.checkAddNearest(photons[i]);
            i = chosen[level++] ^ 1;
        }
    }

    private void balance() {
        if (storedPhotons == 0) {
            return;
        }
        photons = photonList.toArray(new Photon[photonList.size()]);
        photonList = null;
        Photon[] temp = new Photon[storedPhotons + 1];
        balanceSegment(temp, 1, 1, storedPhotons);
        photons = temp;
        halfStoredPhotons = storedPhotons / 2;
        log2n = (int) Math.ceil(Math.log(storedPhotons) / Math.log(2.0));
    }

    private void balanceSegment(Photon[] temp, int index, int start, int end) {
        int median = 1;
        while ((4 * median) <= (end - start + 1)) {
            median += median;
        }
        if ((3 * median) <= (end - start + 1)) {
            median += median;
            median += (start - 1);
        } else {
            median = end - median + 1;
        }
        int axis = Photon.SPLIT_Z;
        Vector3 extents = bounds.getExtents();
        if ((extents.x > extents.y) && (extents.x > extents.z)) {
            axis = Photon.SPLIT_X;
        } else if (extents.y > extents.z) {
            axis = Photon.SPLIT_Y;
        }
        int left = start;
        int right = end;
        while (right > left) {
            double v = photons[right].getCoord(axis);
            int i = left - 1;
            int j = right;
            while (true) {
                while (photons[++i].getCoord(axis) < v) {
                }
                while ((photons[--j].getCoord(axis) > v) && (j > left)) {
                }
                if (i >= j) {
                    break;
                }
                swap(i, j);
            }
            swap(i, right);
            if (i >= median) {
                right = i - 1;
            }
            if (i <= median) {
                left = i + 1;
            }
        }
        temp[index] = photons[median];
        temp[index].setSplitAxis(axis);
        if (median > start) {
            if (start < (median - 1)) {
                float tmp;
                switch (axis) {
                    case Photon.SPLIT_X:
                        tmp = bounds.getMaximum().x;
                        bounds.getMaximum().x = temp[index].x;
                        balanceSegment(temp, 2 * index, start, median - 1);
                        bounds.getMaximum().x = tmp;
                        break;
                    case Photon.SPLIT_Y:
                        tmp = bounds.getMaximum().y;
                        bounds.getMaximum().y = temp[index].y;
                        balanceSegment(temp, 2 * index, start, median - 1);
                        bounds.getMaximum().y = tmp;
                        break;
                    default:
                        tmp = bounds.getMaximum().z;
                        bounds.getMaximum().z = temp[index].z;
                        balanceSegment(temp, 2 * index, start, median - 1);
                        bounds.getMaximum().z = tmp;
                }
            } else {
                temp[2 * index] = photons[start];
            }
        }
        if (median < end) {
            if ((median + 1) < end) {
                float tmp;
                switch (axis) {
                    case Photon.SPLIT_X:
                        tmp = bounds.getMinimum().x;
                        bounds.getMinimum().x = temp[index].x;
                        balanceSegment(temp, (2 * index) + 1, median + 1, end);
                        bounds.getMinimum().x = tmp;
                        break;
                    case Photon.SPLIT_Y:
                        tmp = bounds.getMinimum().y;
                        bounds.getMinimum().y = temp[index].y;
                        balanceSegment(temp, (2 * index) + 1, median + 1, end);
                        bounds.getMinimum().y = tmp;
                        break;
                    default:
                        tmp = bounds.getMinimum().z;
                        bounds.getMinimum().z = temp[index].z;
                        balanceSegment(temp, (2 * index) + 1, median + 1, end);
                        bounds.getMinimum().z = tmp;
                }
            } else {
                temp[(2 * index) + 1] = photons[end];
            }
        }
    }

    private void swap(int i, int j) {
        Photon tmp = photons[i];
        photons[i] = photons[j];
        photons[j] = tmp;
    }

    static class Photon {

        float x;
        float y;
        float z;
        short dir;
        short normal;
        int data;
        int power;
        int flags;
        static final int SPLIT_X = 0;
        static final int SPLIT_Y = 1;
        static final int SPLIT_Z = 2;
        static final int SPLIT_MASK = 3;

        Photon(Point3 p, Vector3 n, Vector3 dir, Color power, Color diffuse) {
            x = p.x;
            y = p.y;
            z = p.z;
            this.dir = dir.encode();
            this.power = power.toRGBE();
            flags = 0;
            normal = n.encode();
            data = diffuse.toRGB();
        }

        void setSplitAxis(int axis) {
            flags &= ~SPLIT_MASK;
            flags |= axis;
        }

        float getCoord(int axis) {
            switch (axis) {
                case SPLIT_X:
                    return x;
                case SPLIT_Y:
                    return y;
                default:
                    return z;
            }
        }

        float getDist1(float px, float py, float pz) {
            switch (flags & SPLIT_MASK) {
                case SPLIT_X:
                    return px - x;
                case SPLIT_Y:
                    return py - y;
                default:
                    return pz - z;
            }
        }

        float getDist2(float px, float py, float pz) {
            float dx = x - px;
            float dy = y - py;
            float dz = z - pz;
            return (dx * dx) + (dy * dy) + (dz * dz);
        }
    }

    @Override
    public void init() {
        UI.printInfo(Module.LIGHT, "Balancing global photon map ...");
        UI.taskStart("Balancing global photon map", 0, 1);
        Timer t = new Timer();
        t.start();
        balance();
        t.end();
        UI.taskStop();
        UI.printInfo(Module.LIGHT, "Global photon map:");
        UI.printInfo(Module.LIGHT, "  * Photons stored:   %d", storedPhotons);
        UI.printInfo(Module.LIGHT, "  * Photons/estimate: %d", numGather);
        UI.printInfo(Module.LIGHT, "  * Estimate radius:  %.3f", gatherRadius);
        maxRadius = 1.4f * (float) Math.sqrt(maxPower * numGather);
        UI.printInfo(Module.LIGHT, "  * Maximum radius:   %.3f", maxRadius);
        UI.printInfo(Module.LIGHT, "  * Balancing time:   %s", t.toString());
        if (gatherRadius > maxRadius) {
            gatherRadius = maxRadius;
        }
        t.start();
        precomputeRadiance();
        t.end();
        UI.printInfo(Module.LIGHT, "  * Precompute time:  %s", t.toString());
        UI.printInfo(Module.LIGHT, "  * Radiance photons: %d", storedPhotons);
        UI.printInfo(Module.LIGHT, "  * Search radius:    %.3f", gatherRadius);
    }

    public void precomputeRadiance() {
        if (storedPhotons == 0) {
            return;
        }
        // precompute the radiance for all photons that are neither
        // leaves nor parents of leaves in the tree.
        int quadStoredPhotons = halfStoredPhotons / 2;
        Point3 p = new Point3();
        Vector3 n = new Vector3();
        Point3 ppos = new Point3();
        Vector3 pdir = new Vector3();
        Vector3 pvec = new Vector3();
        Color irr = new Color();
        Color pow = new Color();
        float maxDist2 = gatherRadius * gatherRadius;
        NearestPhotons np = new NearestPhotons(p, numGather, maxDist2);
        Photon[] temp = new Photon[quadStoredPhotons + 1];
        UI.taskStart("Precomputing radiance", 1, quadStoredPhotons);
        for (int i = 1; i <= quadStoredPhotons; i++) {
            UI.taskUpdate(i);
            Photon curr = photons[i];
            p.set(curr.x, curr.y, curr.z);
            Vector3.decode(curr.normal, n);
            irr.set(Color.BLACK);
            np.reset(p, maxDist2);
            locatePhotons(np);
            if (np.found < 8) {
                curr.data = 0;
                temp[i] = curr;
                continue;
            }
            float invArea = 1.0f / ((float) Math.PI * np.dist2[0]);
            float maxNDist = np.dist2[0] * 0.05f;
            for (int j = 1; j <= np.found; j++) {
                Photon phot = np.index[j];
                Vector3.decode(phot.dir, pdir);
                float cos = -Vector3.dot(pdir, n);
                if (cos > 0.01f) {
                    ppos.set(phot.x, phot.y, phot.z);
                    Point3.sub(ppos, p, pvec);
                    float pcos = Vector3.dot(pvec, n);
                    if ((pcos < maxNDist) && (pcos > -maxNDist)) {
                        irr.add(pow.setRGBE(phot.power));
                    }
                }
            }
            irr.mul(invArea);
            // compute radiance
            irr.mul(new Color(curr.data)).mul(1.0f / (float) Math.PI);
            curr.data = irr.toRGBE();
            temp[i] = curr;
        }
        UI.taskStop();

        // resize photon map to only include irradiance photons
        numGather /= 4;
        maxRadius = 1.4f * (float) Math.sqrt(maxPower * numGather);
        if (gatherRadius > maxRadius) {
            gatherRadius = maxRadius;
        }
        storedPhotons = quadStoredPhotons;
        halfStoredPhotons = storedPhotons / 2;
        log2n = (int) Math.ceil(Math.log(storedPhotons) / Math.log(2.0));
        photons = temp;
        hasRadiance = true;
    }

    @Override
    public Color getRadiance(Point3 p, Vector3 n) {
        if (!hasRadiance || (storedPhotons == 0)) {
            return Color.BLACK;
        }
        float px = p.x;
        float py = p.y;
        float pz = p.z;
        int i = 1;
        int level = 0;
        int cameFrom;
        float dist2;
        float maxDist2 = gatherRadius * gatherRadius;
        Photon nearest = null;
        Photon curr;
        Vector3 photN = new Vector3();
        float[] dist1d2 = new float[log2n];
        int[] chosen = new int[log2n];
        while (true) {
            while (i < halfStoredPhotons) {
                float dist1d = photons[i].getDist1(px, py, pz);
                dist1d2[level] = dist1d * dist1d;
                i += i;
                if (dist1d > 0) {
                    i++;
                }
                chosen[level++] = i;
            }
            curr = photons[i];
            dist2 = curr.getDist2(px, py, pz);
            if (dist2 < maxDist2) {
                Vector3.decode(curr.normal, photN);
                float currentDotN = Vector3.dot(photN, n);
                if (currentDotN > 0.9f) {
                    nearest = curr;
                    maxDist2 = dist2;
                }
            }
            do {
                cameFrom = i;
                i >>= 1;
                level--;
                if (i == 0) {
                    return (nearest == null) ? Color.BLACK : new Color().setRGBE(nearest.data);
                }
            } while ((dist1d2[level] >= maxDist2) || (cameFrom != chosen[level]));
            curr = photons[i];
            dist2 = curr.getDist2(px, py, pz);
            if (dist2 < maxDist2) {
                Vector3.decode(curr.normal, photN);
                float currentDotN = Vector3.dot(photN, n);
                if (currentDotN > 0.9f) {
                    nearest = curr;
                    maxDist2 = dist2;
                }
            }
            i = chosen[level++] ^ 1;
        }
    }

    private static class NearestPhotons {

        int found;
        float px, py, pz;
        private int max;
        private boolean gotHeap;
        protected float[] dist2;
        protected Photon[] index;

        NearestPhotons(Point3 p, int n, float maxDist2) {
            max = n;
            found = 0;
            gotHeap = false;
            px = p.x;
            py = p.y;
            pz = p.z;
            dist2 = new float[n + 1];
            index = new Photon[n + 1];
            dist2[0] = maxDist2;
        }

        void reset(Point3 p, float maxDist2) {
            found = 0;
            gotHeap = false;
            px = p.x;
            py = p.y;
            pz = p.z;
            dist2[0] = maxDist2;
        }

        void checkAddNearest(Photon p) {
            float fdist2 = p.getDist2(px, py, pz);
            if (fdist2 < dist2[0]) {
                if (found < max) {
                    found++;
                    dist2[found] = fdist2;
                    index[found] = p;
                } else {
                    int j;
                    int parent;
                    if (!gotHeap) {
                        float dst2;
                        Photon phot;
                        int halfFound = found >> 1;
                        for (int k = halfFound; k >= 1; k--) {
                            parent = k;
                            phot = index[k];
                            dst2 = dist2[k];
                            while (parent <= halfFound) {
                                j = parent + parent;
                                if ((j < found) && (dist2[j] < dist2[j + 1])) {
                                    j++;
                                }
                                if (dst2 >= dist2[j]) {
                                    break;
                                }
                                dist2[parent] = dist2[j];
                                index[parent] = index[j];
                                parent = j;
                            }
                            dist2[parent] = dst2;
                            index[parent] = phot;
                        }
                        gotHeap = true;
                    }
                    parent = 1;
                    j = 2;
                    while (j <= found) {
                        if ((j < found) && (dist2[j] < dist2[j + 1])) {
                            j++;
                        }
                        if (fdist2 > dist2[j]) {
                            break;
                        }
                        dist2[parent] = dist2[j];
                        index[parent] = index[j];
                        parent = j;
                        j += j;
                    }
                    dist2[parent] = fdist2;
                    index[parent] = p;
                    dist2[0] = dist2[1];
                }
            }
        }
    }

    @Override
    public boolean allowDiffuseBounced() {
        return true;
    }

    @Override
    public boolean allowReflectionBounced() {
        return true;
    }

    @Override
    public boolean allowRefractionBounced() {
        return true;
    }

    @Override
    public int numEmit() {
        return numEmit;
    }
}