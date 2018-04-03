package org.sunflow.core.photonmap;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sunflow.core.GlobalPhotonMapInterface;
import org.sunflow.core.Options;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.MathUtils;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class GridPhotonMap implements GlobalPhotonMapInterface {

    private int numGather;
    private float gatherRadius;
    private int numStoredPhotons;
    private int nx, ny, nz;
    private BoundingBox bounds;
    private PhotonGroup[] cellHash;
    private int hashSize;
    private int hashPrime;
    private ReentrantReadWriteLock rwl;
    private int numEmit;
    private static final float NORMAL_THRESHOLD = (float) Math.cos(10.0 * Math.PI / 180.0);
    private static final int[] PRIMES = {11, 19, 37, 109, 163, 251, 367, 557,
        823, 1237, 1861, 2777, 4177, 6247, 9371, 21089, 31627, 47431,
        71143, 106721, 160073, 240101, 360163, 540217, 810343, 1215497,
        1823231, 2734867, 4102283, 6153409, 9230113, 13845163};

    public GridPhotonMap() {
        numStoredPhotons = 0;
        hashSize = 0; // number of unique IDs in the hash
        rwl = new ReentrantReadWriteLock();
        numEmit = 100000;
    }

    @Override
    public void prepare(Options options, BoundingBox sceneBounds) {
        // get settings
        numEmit = options.getInt("gi.irr-cache.gmap.emit", 100000);
        numGather = options.getInt("gi.irr-cache.gmap.gather", 50);
        gatherRadius = options.getFloat("gi.irr-cache.gmap.radius", 0.5f);

        bounds = new BoundingBox(sceneBounds);
        bounds.enlargeUlps();
        Vector3 w = bounds.getExtents();
        nx = (int) Math.max(((w.x / gatherRadius) + 0.5f), 1);
        ny = (int) Math.max(((w.y / gatherRadius) + 0.5f), 1);
        nz = (int) Math.max(((w.z / gatherRadius) + 0.5f), 1);
        int numCells = nx * ny * nz;
        UI.printInfo(Module.LIGHT, "Initializing grid photon map:");
        UI.printInfo(Module.LIGHT, "  * Resolution:  %dx%dx%d", nx, ny, nz);
        UI.printInfo(Module.LIGHT, "  * Total cells: %d", numCells);
        for (hashPrime = 0; hashPrime < PRIMES.length; hashPrime++) {
            if (PRIMES[hashPrime] > (numCells / 5)) {
                break;
            }
        }
        cellHash = new PhotonGroup[PRIMES[hashPrime]];
        UI.printInfo(Module.LIGHT, "  * Initial hash size: %d", cellHash.length);
    }

    public int size() {
        return numStoredPhotons;
    }

    @Override
    public void store(ShadingState state, Vector3 dir, Color power, Color diffuse) {
        // don't store on the wrong side of a surface
        if (Vector3.dot(state.getNormal(), dir) > 0) {
            return;
        }
        Point3 pt = state.getPoint();
        // outside grid bounds ?
        if (!bounds.contains(pt)) {
            return;
        }
        Vector3 ext = bounds.getExtents();
        int ix = (int) (((pt.x - bounds.getMinimum().x) * nx) / ext.x);
        int iy = (int) (((pt.y - bounds.getMinimum().y) * ny) / ext.y);
        int iz = (int) (((pt.z - bounds.getMinimum().z) * nz) / ext.z);
        ix = MathUtils.clamp(ix, 0, nx - 1);
        iy = MathUtils.clamp(iy, 0, ny - 1);
        iz = MathUtils.clamp(iz, 0, nz - 1);
        int id = ix + iy * nx + iz * nx * ny;
        synchronized (this) {
            int hid = id % cellHash.length;
            PhotonGroup g = cellHash[hid];
            PhotonGroup last = null;
            boolean hasID = false;
            while (g != null) {
                if (g.id == id) {
                    hasID = true;
                    if (Vector3.dot(state.getNormal(), g.normal) > NORMAL_THRESHOLD) {
                        break;
                    }
                }
                last = g;
                g = g.next;
            }
            if (g == null) {
                g = new PhotonGroup(id, state.getNormal());
                if (last == null) {
                    cellHash[hid] = g;
                } else {
                    last.next = g;
                }
                if (!hasID) {
                    hashSize++; // we have not seen this ID before
                    // resize hash if we have grown too large
                    if (hashSize > cellHash.length) {
                        growPhotonHash();
                    }
                }
            }
            g.count++;
            g.flux.add(power);
            g.diffuse.add(diffuse);
            numStoredPhotons++;
        }
    }

    @Override
    public void init() {
        UI.printInfo(Module.LIGHT, "Initializing photon grid ...");
        UI.printInfo(Module.LIGHT, "  * Photon hits:      %d", numStoredPhotons);
        UI.printInfo(Module.LIGHT, "  * Final hash size:  %d", cellHash.length);
        int cells = 0;
        for (int i = 0; i < cellHash.length; i++) {
            for (PhotonGroup g = cellHash[i]; g != null; g = g.next) {
                g.diffuse.mul(1.0f / g.count);
                cells++;
            }
        }
        UI.printInfo(Module.LIGHT, "  * Num photon cells: %d", cells);
    }

    public void precomputeRadiance(boolean includeDirect, boolean includeCaustics) {
    }

    private void growPhotonHash() {
        // enlarge the hash size:
        if (hashPrime >= PRIMES.length - 1) {
            return;
        }
        PhotonGroup[] temp = new PhotonGroup[PRIMES[++hashPrime]];
        for (int i = 0; i < cellHash.length; i++) {
            PhotonGroup g = cellHash[i];
            while (g != null) {
                // re-hash into the new table
                int hid = g.id % temp.length;
                PhotonGroup last = null;
                for (PhotonGroup gn = temp[hid]; gn != null; gn = gn.next) {
                    last = gn;
                }
                if (last == null) {
                    temp[hid] = g;
                } else {
                    last.next = g;
                }
                PhotonGroup next = g.next;
                g.next = null;
                g = next;
            }
        }
        cellHash = temp;
    }

    @Override
    public synchronized Color getRadiance(Point3 p, Vector3 n) {
        if (!bounds.contains(p)) {
            return Color.BLACK;
        }
        Vector3 ext = bounds.getExtents();
        int ix = (int) (((p.x - bounds.getMinimum().x) * nx) / ext.x);
        int iy = (int) (((p.y - bounds.getMinimum().y) * ny) / ext.y);
        int iz = (int) (((p.z - bounds.getMinimum().z) * nz) / ext.z);
        ix = MathUtils.clamp(ix, 0, nx - 1);
        iy = MathUtils.clamp(iy, 0, ny - 1);
        iz = MathUtils.clamp(iz, 0, nz - 1);
        int id = ix + iy * nx + iz * nx * ny;
        rwl.readLock().lock();
        PhotonGroup center = null;
        for (PhotonGroup g = get(ix, iy, iz); g != null; g = g.next) {
            if (g.id == id && Vector3.dot(n, g.normal) > NORMAL_THRESHOLD) {
                if (g.radiance == null) {
                    center = g;
                    break;
                }
                Color r = g.radiance.copy();
                rwl.readLock().unlock();
                return r;
            }
        }
        int vol = 1;
        while (true) {
            int numPhotons = 0;
            int ndiff = 0;
            Color irr = Color.black();
            Color diff = (center == null) ? Color.black() : null;
            for (int z = iz - (vol - 1); z <= iz + (vol - 1); z++) {
                for (int y = iy - (vol - 1); y <= iy + (vol - 1); y++) {
                    for (int x = ix - (vol - 1); x <= ix + (vol - 1); x++) {
                        int vid = x + y * nx + z * nx * ny;
                        for (PhotonGroup g = get(x, y, z); g != null; g = g.next) {
                            if (g.id == vid && Vector3.dot(n, g.normal) > NORMAL_THRESHOLD) {
                                numPhotons += g.count;
                                irr.add(g.flux);
                                if (diff != null) {
                                    diff.add(g.diffuse);
                                    ndiff++;
                                }
                                break; // only one valid group can be found,
                                // skip the others
                            }
                        }
                    }
                }
            }
            if (numPhotons >= numGather || vol >= 3) {
                // we have found enough photons
                // cache irradiance and return
                float area = (2 * vol - 1) / 3.0f * ((ext.x / nx) + (ext.y / ny) + (ext.z / nz));
                area *= area;
                area *= Math.PI;
                irr.mul(1.0f / area);
                // upgrade lock manually
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                try {
                    if (center == null) {
                        if (ndiff > 0) {
                            diff.mul(1.0f / ndiff);
                        }
                        center = new PhotonGroup(id, n);
                        center.diffuse.set(diff);
                        center.next = cellHash[id % cellHash.length];
                        cellHash[id % cellHash.length] = center;
                    }
                    irr.mul(center.diffuse);
                    center.radiance = irr.copy();
                } finally {
                    rwl.writeLock().unlock();
                }
                return irr;
            }
            vol++;
        }
    }

    private PhotonGroup get(int x, int y, int z) {
        // returns the list associated with the specified location
        if (x < 0 || x >= nx) {
            return null;
        }
        if (y < 0 || y >= ny) {
            return null;
        }
        if (z < 0 || z >= nz) {
            return null;
        }
        return cellHash[(x + y * nx + z * nx * ny) % cellHash.length];
    }

    private class PhotonGroup {

        int id;
        int count;
        Vector3 normal;
        Color flux;
        Color radiance;
        Color diffuse;
        PhotonGroup next;

        PhotonGroup(int id, Vector3 n) {
            normal = new Vector3(n);
            flux = Color.black();
            diffuse = Color.black();
            radiance = null;
            count = 0;
            this.id = id;
            next = null;
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