package org.sunflow.core.accel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Point3;
import org.sunflow.system.Memory;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.IntArray;

public class KDTree implements AccelerationStructure {

    private int[] tree;
    private int[] primitives;
    private PrimitiveList primitiveList;
    private BoundingBox bounds;
    private int maxPrims;
    private static final float INTERSECT_COST = 0.5f;
    private static final float TRAVERSAL_COST = 1;
    private static final float EMPTY_BONUS = 0.2f;
    private static final int MAX_DEPTH = 64;
    private static boolean dump = false;
    private static String dumpPrefix = "kdtree";

    public KDTree() {
        this(0);
    }

    public KDTree(int maxPrims) {
        this.maxPrims = maxPrims;
    }

    private static class BuildStats {

        private int numNodes;
        private int numLeaves;
        private int sumObjects;
        private int minObjects;
        private int maxObjects;
        private int sumDepth;
        private int minDepth;
        private int maxDepth;
        private int numLeaves0;
        private int numLeaves1;
        private int numLeaves2;
        private int numLeaves3;
        private int numLeaves4;
        private int numLeaves4p;

        BuildStats() {
            numNodes = numLeaves = 0;
            sumObjects = 0;
            minObjects = Integer.MAX_VALUE;
            maxObjects = Integer.MIN_VALUE;
            sumDepth = 0;
            minDepth = Integer.MAX_VALUE;
            maxDepth = Integer.MIN_VALUE;
            numLeaves0 = 0;
            numLeaves1 = 0;
            numLeaves2 = 0;
            numLeaves3 = 0;
            numLeaves4 = 0;
            numLeaves4p = 0;
        }

        void updateInner() {
            numNodes++;
        }

        void updateLeaf(int depth, int n) {
            numLeaves++;
            minDepth = Math.min(depth, minDepth);
            maxDepth = Math.max(depth, maxDepth);
            sumDepth += depth;
            minObjects = Math.min(n, minObjects);
            maxObjects = Math.max(n, maxObjects);
            sumObjects += n;
            switch (n) {
                case 0:
                    numLeaves0++;
                    break;
                case 1:
                    numLeaves1++;
                    break;
                case 2:
                    numLeaves2++;
                    break;
                case 3:
                    numLeaves3++;
                    break;
                case 4:
                    numLeaves4++;
                    break;
                default:
                    numLeaves4p++;
                    break;
            }
        }

        void printStats() {
            UI.printDetailed(Module.ACCEL, "KDTree stats:");
            UI.printDetailed(Module.ACCEL, "  * Nodes:          %d", numNodes);
            UI.printDetailed(Module.ACCEL, "  * Leaves:         %d", numLeaves);
            UI.printDetailed(Module.ACCEL, "  * Objects: min    %d", minObjects);
            UI.printDetailed(Module.ACCEL, "             avg    %.2f", (float) sumObjects / numLeaves);
            UI.printDetailed(Module.ACCEL, "           avg(n>0) %.2f", (float) sumObjects / (numLeaves - numLeaves0));
            UI.printDetailed(Module.ACCEL, "             max    %d", maxObjects);
            UI.printDetailed(Module.ACCEL, "  * Depth:   min    %d", minDepth);
            UI.printDetailed(Module.ACCEL, "             avg    %.2f", (float) sumDepth / numLeaves);
            UI.printDetailed(Module.ACCEL, "             max    %d", maxDepth);
            UI.printDetailed(Module.ACCEL, "  * Leaves w/: N=0  %3d%%", 100 * numLeaves0 / numLeaves);
            UI.printDetailed(Module.ACCEL, "               N=1  %3d%%", 100 * numLeaves1 / numLeaves);
            UI.printDetailed(Module.ACCEL, "               N=2  %3d%%", 100 * numLeaves2 / numLeaves);
            UI.printDetailed(Module.ACCEL, "               N=3  %3d%%", 100 * numLeaves3 / numLeaves);
            UI.printDetailed(Module.ACCEL, "               N=4  %3d%%", 100 * numLeaves4 / numLeaves);
            UI.printDetailed(Module.ACCEL, "               N>4  %3d%%", 100 * numLeaves4p / numLeaves);
        }
    }

    public static void setDumpMode(boolean dump, String prefix) {
        KDTree.dump = dump;
        KDTree.dumpPrefix = prefix;
    }

    @Override
    public void build(PrimitiveList primitives) {
        UI.printDetailed(Module.ACCEL, "KDTree settings");
        UI.printDetailed(Module.ACCEL, "  * Max Leaf Size:  %d", maxPrims);
        UI.printDetailed(Module.ACCEL, "  * Max Depth:      %d", MAX_DEPTH);
        UI.printDetailed(Module.ACCEL, "  * Traversal cost: %.2f", TRAVERSAL_COST);
        UI.printDetailed(Module.ACCEL, "  * Intersect cost: %.2f", INTERSECT_COST);
        UI.printDetailed(Module.ACCEL, "  * Empty bonus:    %.2f", EMPTY_BONUS);
        UI.printDetailed(Module.ACCEL, "  * Dump leaves:    %s", dump ? "enabled" : "disabled");
        Timer total = new Timer();
        total.start();
        primitiveList = primitives;
        // get the object space bounds
        bounds = primitives.getWorldBounds(null);
        int nPrim = primitiveList.getNumPrimitives(), nSplits = 0;
        BuildTask task = new BuildTask(nPrim);
        Timer prepare = new Timer();
        prepare.start();
        for (int i = 0; i < nPrim; i++) {
            for (int axis = 0; axis < 3; axis++) {
                float ls = primitiveList.getPrimitiveBound(i, 2 * axis + 0);
                float rs = primitiveList.getPrimitiveBound(i, 2 * axis + 1);
                if (ls == rs) {
                    // flat in this dimension
                    task.splits[nSplits] = pack(ls, PLANAR, axis, i);
                    nSplits++;
                } else {
                    task.splits[nSplits + 0] = pack(ls, OPENED, axis, i);
                    task.splits[nSplits + 1] = pack(rs, CLOSED, axis, i);
                    nSplits += 2;
                }
            }
        }
        task.n = nSplits;
        prepare.end();
        Timer t = new Timer();
        IntArray tempTree = new IntArray();
        IntArray tempList = new IntArray();
        tempTree.add(0);
        tempTree.add(1);
        t.start();
        // sort it
        Timer sorting = new Timer();
        sorting.start();
        radix12(task.splits, task.n);
        sorting.end();
        // build the actual tree
        BuildStats stats = new BuildStats();
        buildTree(bounds.getMinimum().x, bounds.getMaximum().x, bounds.getMinimum().y, bounds.getMaximum().y, bounds.getMinimum().z, bounds.getMaximum().z, task, 1, tempTree, 0, tempList, stats);
        t.end();
        // write out final arrays
        // free some memory
        task = null;
        tree = tempTree.trim();
        tempTree = null;
        this.primitives = tempList.trim();
        tempList = null;
        total.end();
        // display some extra info
        stats.printStats();
        UI.printDetailed(Module.ACCEL, "  * Node memory:    %s", Memory.sizeof(tree));
        UI.printDetailed(Module.ACCEL, "  * Object memory:  %s", Memory.sizeof(this.primitives));
        UI.printDetailed(Module.ACCEL, "  * Prepare time:   %s", prepare);
        UI.printDetailed(Module.ACCEL, "  * Sorting time:   %s", sorting);
        UI.printDetailed(Module.ACCEL, "  * Tree creation:  %s", t);
        UI.printDetailed(Module.ACCEL, "  * Build time:     %s", total);
        if (dump) {
            try {
                UI.printInfo(Module.ACCEL, "Dumping mtls to %s.mtl ...", dumpPrefix);
                FileWriter mtlFile = new FileWriter(dumpPrefix + ".mtl");
                int maxN = stats.maxObjects;
                for (int n = 0; n <= maxN; n++) {
                    float blend = (float) n / (float) maxN;
                    Color nc;
                    if (blend < 0.25) {
                        nc = Color.blend(Color.BLUE, Color.GREEN, blend / 0.25f);
                    } else if (blend < 0.5) {
                        nc = Color.blend(Color.GREEN, Color.YELLOW, (blend - 0.25f) / 0.25f);
                    } else if (blend < 0.75) {
                        nc = Color.blend(Color.YELLOW, Color.RED, (blend - 0.50f) / 0.25f);
                    } else {
                        nc = Color.MAGENTA;
                    }
                    mtlFile.write(String.format("newmtl mtl%d\n", n));
                    float[] rgb = nc.getRGB();
                    mtlFile.write("Ka 0.1 0.1 0.1\n");
                    mtlFile.write(String.format("Kd %.12g %.12g %.12g\n", rgb[0], rgb[1], rgb[2]));
                    mtlFile.write("illum 1\n\n");
                }
                FileWriter objFile = new FileWriter(dumpPrefix + ".obj");
                UI.printInfo(Module.ACCEL, "Dumping tree to %s.obj ...", dumpPrefix);
                dumpObj(0, 0, maxN, new BoundingBox(bounds), objFile, mtlFile);
                objFile.close();
                mtlFile.close();
            } catch (IOException e) {
                Logger.getLogger(KDTree.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private int dumpObj(int offset, int vertOffset, int maxN, BoundingBox bounds, FileWriter file, FileWriter mtlFile) throws IOException {
        if (offset == 0) {
            file.write(String.format("mtllib %s.mtl\n", dumpPrefix));
        }
        int nextOffset = tree[offset];
        String FACE_FORMAT = "f %d %d %d %d\n";
        String VERTEX_FORMAT = "v %g %g %g\n";
        if ((nextOffset & (3 << 30)) == (3 << 30)) {
            // leaf
            int n = tree[offset + 1];
            if (n > 0) {
                // output the current voxel to the file
                Point3 min = bounds.getMinimum();
                Point3 max = bounds.getMaximum();
                file.write(String.format("o node%d\n", offset));
                file.write(String.format(VERTEX_FORMAT, max.x, max.y, min.z));
                file.write(String.format(VERTEX_FORMAT, max.x, min.y, min.z));
                file.write(String.format(VERTEX_FORMAT, min.x, min.y, min.z));
                file.write(String.format(VERTEX_FORMAT, min.x, max.y, min.z));
                file.write(String.format(VERTEX_FORMAT, max.x, max.y, max.z));
                file.write(String.format(VERTEX_FORMAT, max.x, min.y, max.z));
                file.write(String.format(VERTEX_FORMAT, min.x, min.y, max.z));
                file.write(String.format(VERTEX_FORMAT, min.x, max.y, max.z));
                int v0 = vertOffset;
                file.write(String.format("usemtl mtl%d\n", n));
                file.write("s off\n");
                file.write(String.format(FACE_FORMAT, v0 + 1, v0 + 2, v0 + 3, v0 + 4));
                file.write(String.format(FACE_FORMAT, v0 + 5, v0 + 8, v0 + 7, v0 + 6));
                file.write(String.format(FACE_FORMAT, v0 + 1, v0 + 5, v0 + 6, v0 + 2));
                file.write(String.format(FACE_FORMAT, v0 + 2, v0 + 6, v0 + 7, v0 + 3));
                file.write(String.format(FACE_FORMAT, v0 + 3, v0 + 7, v0 + 8, v0 + 4));
                file.write(String.format(FACE_FORMAT, v0 + 5, v0 + 1, v0 + 4, v0 + 8));
                vertOffset += 8;
            }
            return vertOffset;
        } else {
            // node, recurse
            int axis = nextOffset & (3 << 30), v0;
            float split = Float.intBitsToFloat(tree[offset + 1]), min, max;
            nextOffset &= ~(3 << 30);
            switch (axis) {
                case 0:
                    max = bounds.getMaximum().x;
                    bounds.getMaximum().x = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().x = max;
                    min = bounds.getMinimum().x;
                    bounds.getMinimum().x = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    bounds.getMinimum().x = min;
                    break;
                case 1 << 30:
                    max = bounds.getMaximum().y;
                    bounds.getMaximum().y = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().y = max;
                    min = bounds.getMinimum().y;
                    bounds.getMinimum().y = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    bounds.getMinimum().y = min;
                    break;
                case 2 << 30:
                    max = bounds.getMaximum().z;
                    bounds.getMaximum().z = split;
                    v0 = dumpObj(nextOffset, vertOffset, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMaximum().z = max;
                    min = bounds.getMinimum().z;
                    bounds.getMinimum().z = split;
                    v0 = dumpObj(nextOffset + 2, v0, maxN, bounds, file, mtlFile);
                    // restore and go to other side
                    bounds.getMinimum().z = min;
                    break;
                default:
                    v0 = vertOffset;
                    break;
            }
            return v0;
        }
    }
    // type is encoded as 2 shifted bits
    private static final long CLOSED = 0L << 30;
    private static final long PLANAR = 1L << 30;
    private static final long OPENED = 2L << 30;
    private static final long TYPE_MASK = 3L << 30;

    // pack split values into a 64bit integer
    private static long pack(float split, long type, int axis, int object) {
        // pack float in sortable form
        int f = Float.floatToRawIntBits(split);
        int top = f ^ ((f >> 31) | 0x80000000);
        long p = (top & 0xFFFFFFFFL) << 32;
        p |= type; // encode type as 2 bits
        p |= ((long) axis) << 28; // encode axis as 2 bits
        p |= (object & 0xFFFFFFFL); // pack object number
        return p;
    }

    private static int unpackObject(long p) {
        return (int) (p & 0xFFFFFFFL);
    }

    private static int unpackAxis(long p) {
        return (int) (p >>> 28) & 3;
    }

    private static long unpackSplitType(long p) {
        return p & TYPE_MASK;
    }

    private static float unpackSplit(long p) {
        int f = (int) ((p >>> 32) & 0xFFFFFFFFL);
        int m = ((f >>> 31) - 1) | 0x80000000;
        return Float.intBitsToFloat(f ^ m);
    }

    // radix sort on top 36 bits - returns sorted result
    private static void radix12(long[] splits, int n) {
        // allocate working memory
        final int[] hist = new int[2048];
        final long[] sorted = new long[n];
        // parallel histogramming pass
        for (int i = 0; i < n; i++) {
            long pi = splits[i];
            hist[0x000 + ((int) (pi >>> 28) & 0x1FF)]++;
            hist[0x200 + ((int) (pi >>> 37) & 0x1FF)]++;
            hist[0x400 + ((int) (pi >>> 46) & 0x1FF)]++;
            hist[0x600 + ((int) (pi >>> 55))]++;
        }

        // sum the histograms - each histogram entry records the number of
        // values preceding itself.
        {
            int sum0 = 0, sum1 = 0, sum2 = 0, sum3 = 0;
            int tsum;
            for (int i = 0; i < 512; i++) {
                tsum = hist[0x000 + i] + sum0;
                hist[0x000 + i] = sum0 - 1;
                sum0 = tsum;
                tsum = hist[0x200 + i] + sum1;
                hist[0x200 + i] = sum1 - 1;
                sum1 = tsum;
                tsum = hist[0x400 + i] + sum2;
                hist[0x400 + i] = sum2 - 1;
                sum2 = tsum;
                tsum = hist[0x600 + i] + sum3;
                hist[0x600 + i] = sum3 - 1;
                sum3 = tsum;
            }
        }

        // read/write histogram passes
        for (int i = 0; i < n; i++) {
            long pi = splits[i];
            int pos = (int) (pi >>> 28) & 0x1FF;
            sorted[++hist[0x000 + pos]] = pi;
        }
        for (int i = 0; i < n; i++) {
            long pi = sorted[i];
            int pos = (int) (pi >>> 37) & 0x1FF;
            splits[++hist[0x200 + pos]] = pi;
        }
        for (int i = 0; i < n; i++) {
            long pi = splits[i];
            int pos = (int) (pi >>> 46) & 0x1FF;
            sorted[++hist[0x400 + pos]] = pi;
        }
        for (int i = 0; i < n; i++) {
            long pi = sorted[i];
            int pos = (int) (pi >>> 55);
            splits[++hist[0x600 + pos]] = pi;
        }
    }

    private static class BuildTask {

        long[] splits;
        int numObjects;
        int n;
        byte[] leftRightTable;

        BuildTask(int numObjects) {
            splits = new long[6 * numObjects];
            this.numObjects = numObjects;
            n = 0;
            // 2 bits per object
            leftRightTable = new byte[(numObjects + 3) / 4];
        }

        BuildTask(int numObjects, BuildTask parent) {
            splits = new long[6 * numObjects];
            this.numObjects = numObjects;
            n = 0;
            leftRightTable = parent.leftRightTable;
        }
    }

    private void buildTree(float minx, float maxx, float miny, float maxy, float minz, float maxz, BuildTask task, int depth, IntArray tempTree, int offset, IntArray tempList, BuildStats stats) {
        // get node bounding box extents
        if (task.numObjects > maxPrims && depth < MAX_DEPTH) {
            float dx = maxx - minx;
            float dy = maxy - miny;
            float dz = maxz - minz;
            // search for best possible split
            float bestCost = INTERSECT_COST * task.numObjects;
            int bestAxis = -1;
            int bestOffsetStart = -1;
            int bestOffsetEnd = -1;
            float bestSplit = 0;
            boolean bestPlanarLeft = false;
            int bnl = 0, bnr = 0;
            // inverse area of the bounding box (factor of 2 ommitted)
            float area = (dx * dy + dy * dz + dz * dx);
            float ISECT_COST = INTERSECT_COST / area;
            // setup counts for each axis
            int[] nl = {0, 0, 0};
            int[] nr = {task.numObjects, task.numObjects, task.numObjects};
            // setup bounds for each axis
            float[] dp = {dy * dz, dz * dx, dx * dy};
            float[] ds = {dy + dz, dz + dx, dx + dy};
            float[] nodeMin = {minx, miny, minz};
            float[] nodeMax = {maxx, maxy, maxz};
            // search for best cost
            int nSplits = task.n;
            long[] splits = task.splits;
            byte[] lrtable = task.leftRightTable;
            for (int i = 0; i < nSplits;) {
                // extract current split
                long ptr = splits[i];
                float split = unpackSplit(ptr);
                int axis = unpackAxis(ptr);
                // mark current position
                int currentOffset = i;
                // count number of primitives start/stopping/lying on the
                // current plane
                int pClosed = 0, pPlanar = 0, pOpened = 0;
                long ptrMasked = ptr & (~TYPE_MASK & 0xFFFFFFFFF0000000L);
                long ptrClosed = ptrMasked | CLOSED;
                long ptrPlanar = ptrMasked | PLANAR;
                long ptrOpened = ptrMasked | OPENED;
                while (i < nSplits && (splits[i] & 0xFFFFFFFFF0000000L) == ptrClosed) {
                    int obj = unpackObject(splits[i]);
                    lrtable[obj >>> 2] = 0;
                    pClosed++;
                    i++;
                }
                while (i < nSplits && (splits[i] & 0xFFFFFFFFF0000000L) == ptrPlanar) {
                    int obj = unpackObject(splits[i]);
                    lrtable[obj >>> 2] = 0;
                    pPlanar++;
                    i++;
                }
                while (i < nSplits && (splits[i] & 0xFFFFFFFFF0000000L) == ptrOpened) {
                    int obj = unpackObject(splits[i]);
                    lrtable[obj >>> 2] = 0;
                    pOpened++;
                    i++;
                }
                // now we have summed all contributions from this plane
                nr[axis] -= pPlanar + pClosed;
                // compute cost
                if (split >= nodeMin[axis] && split <= nodeMax[axis]) {
                    // left and right surface area (factor of 2 ommitted)
                    float dl = split - nodeMin[axis];
                    float dr = nodeMax[axis] - split;
                    float lp = dp[axis] + dl * ds[axis];
                    float rp = dp[axis] + dr * ds[axis];
                    // planar prims go to smallest cell always
                    boolean planarLeft = dl < dr;
                    int numLeft = nl[axis] + (planarLeft ? pPlanar : 0);
                    int numRight = nr[axis] + (planarLeft ? 0 : pPlanar);
                    float eb = ((numLeft == 0 && dl > 0) || (numRight == 0 && dr > 0)) ? EMPTY_BONUS : 0;
                    float cost = TRAVERSAL_COST + ISECT_COST * (1 - eb) * (lp * numLeft + rp * numRight);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestAxis = axis;
                        bestSplit = split;
                        bestOffsetStart = currentOffset;
                        bestOffsetEnd = i;
                        bnl = numLeft;
                        bnr = numRight;
                        bestPlanarLeft = planarLeft;
                    }
                }
                // move objects left
                nl[axis] += pOpened + pPlanar;
            }
            // debug check for correctness of the scan
            for (int axis = 0; axis < 3; axis++) {
                int numLeft = nl[axis];
                int numRight = nr[axis];
                if (numLeft != task.numObjects || numRight != 0) {
                    UI.printError(Module.ACCEL, "Didn't scan full range of objects @depth=%d. Left overs for axis %d: [L: %d] [R: %d]", depth, axis, numLeft, numRight);
                }
            }
            // found best split?
            if (bestAxis != -1) {
                // allocate space for child nodes
                BuildTask taskL = new BuildTask(bnl, task);
                BuildTask taskR = new BuildTask(bnr, task);
                int lk = 0, rk = 0;
                for (int i = 0; i < bestOffsetStart; i++) {
                    long ptr = splits[i];
                    if (unpackAxis(ptr) == bestAxis) {
                        if (unpackSplitType(ptr) != CLOSED) {
                            int obj = unpackObject(ptr);
                            lrtable[obj >>> 2] |= 1 << ((obj & 3) << 1);
                            lk++;
                        }
                    }
                }
                for (int i = bestOffsetStart; i < bestOffsetEnd; i++) {
                    long ptr = splits[i];
                    assert unpackAxis(ptr) == bestAxis;
                    if (unpackSplitType(ptr) == PLANAR) {
                        if (bestPlanarLeft) {
                            int obj = unpackObject(ptr);
                            lrtable[obj >>> 2] |= 1 << ((obj & 3) << 1);
                            lk++;
                        } else {
                            int obj = unpackObject(ptr);
                            lrtable[obj >>> 2] |= 2 << ((obj & 3) << 1);
                            rk++;
                        }
                    }
                }
                for (int i = bestOffsetEnd; i < nSplits; i++) {
                    long ptr = splits[i];
                    if (unpackAxis(ptr) == bestAxis) {
                        if (unpackSplitType(ptr) != OPENED) {
                            int obj = unpackObject(ptr);
                            lrtable[obj >>> 2] |= 2 << ((obj & 3) << 1);
                            rk++;
                        }
                    }
                }
                // output new splits while maintaining order
                long[] splitsL = taskL.splits;
                long[] splitsR = taskR.splits;
                int nsl = 0, nsr = 0;
                for (int i = 0; i < nSplits; i++) {
                    long ptr = splits[i];
                    int obj = unpackObject(ptr);
                    int idx = obj >>> 2;
                    int mask = 1 << ((obj & 3) << 1);
                    if ((lrtable[idx] & mask) != 0) {
                        splitsL[nsl] = ptr;
                        nsl++;
                    }
                    if ((lrtable[idx] & (mask << 1)) != 0) {
                        splitsR[nsr] = ptr;
                        nsr++;
                    }
                }
                taskL.n = nsl;
                taskR.n = nsr;
                // free more memory
                task.splits = splits = splitsL = splitsR = null;
                task = null;
                // allocate child nodes
                int nextOffset = tempTree.getSize();
                tempTree.add(0);
                tempTree.add(0);
                tempTree.add(0);
                tempTree.add(0);
                // create current node
                tempTree.set(offset + 0, (bestAxis << 30) | nextOffset);
                tempTree.set(offset + 1, Float.floatToRawIntBits(bestSplit));
                // recurse for child nodes - free object arrays after each step
                stats.updateInner();
                switch (bestAxis) {
                    case 0:
                        buildTree(minx, bestSplit, miny, maxy, minz, maxz, taskL, depth + 1, tempTree, nextOffset, tempList, stats);
                        taskL = null;
                        buildTree(bestSplit, maxx, miny, maxy, minz, maxz, taskR, depth + 1, tempTree, nextOffset + 2, tempList, stats);
                        taskR = null;
                        return;
                    case 1:
                        buildTree(minx, maxx, miny, bestSplit, minz, maxz, taskL, depth + 1, tempTree, nextOffset, tempList, stats);
                        taskL = null;
                        buildTree(minx, maxx, bestSplit, maxy, minz, maxz, taskR, depth + 1, tempTree, nextOffset + 2, tempList, stats);
                        taskR = null;
                        return;
                    case 2:
                        buildTree(minx, maxx, miny, maxy, minz, bestSplit, taskL, depth + 1, tempTree, nextOffset, tempList, stats);
                        taskL = null;
                        buildTree(minx, maxx, miny, maxy, bestSplit, maxz, taskR, depth + 1, tempTree, nextOffset + 2, tempList, stats);
                        taskR = null;
                        return;
                    default:
                        assert false;
                }
            }
        }
        // create leaf node
        int listOffset = tempList.getSize();
        int n = 0;
        for (int i = 0; i < task.n; i++) {
            long ptr = task.splits[i];
            if (unpackAxis(ptr) == 0 && unpackSplitType(ptr) != CLOSED) {
                tempList.add(unpackObject(ptr));
                n++;
            }
        }
        stats.updateLeaf(depth, n);
        if (n != task.numObjects) {
            UI.printError(Module.ACCEL, "Error creating leaf node - expecting %d found %d", task.numObjects, n);
        }
        tempTree.set(offset + 0, (3 << 30) | listOffset);
        tempTree.set(offset + 1, task.numObjects);
        // free some memory
        task.splits = null;
    }

    @Override
    public void intersect(Ray r, IntersectionState state) {
        float intervalMin = r.getMin();
        float intervalMax = r.getMax();
        float orgX = r.ox;
        float dirX = r.dx, invDirX = 1 / dirX;
        float t1, t2;
        t1 = (bounds.getMinimum().x - orgX) * invDirX;
        t2 = (bounds.getMaximum().x - orgX) * invDirX;
        if (invDirX > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgY = r.oy;
        float dirY = r.dy, invDirY = 1 / dirY;
        t1 = (bounds.getMinimum().y - orgY) * invDirY;
        t2 = (bounds.getMaximum().y - orgY) * invDirY;
        if (invDirY > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        float orgZ = r.oz;
        float dirZ = r.dz, invDirZ = 1 / dirZ;
        t1 = (bounds.getMinimum().z - orgZ) * invDirZ;
        t2 = (bounds.getMaximum().z - orgZ) * invDirZ;
        if (invDirZ > 0) {
            if (t1 > intervalMin) {
                intervalMin = t1;
            }
            if (t2 < intervalMax) {
                intervalMax = t2;
            }
        } else {
            if (t2 > intervalMin) {
                intervalMin = t2;
            }
            if (t1 < intervalMax) {
                intervalMax = t1;
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }

        // compute custom offsets from direction sign bit
        int offsetXFront = (Float.floatToRawIntBits(dirX) & (1 << 31)) >>> 30;
        int offsetYFront = (Float.floatToRawIntBits(dirY) & (1 << 31)) >>> 30;
        int offsetZFront = (Float.floatToRawIntBits(dirZ) & (1 << 31)) >>> 30;

        int offsetXBack = offsetXFront ^ 2;
        int offsetYBack = offsetYFront ^ 2;
        int offsetZBack = offsetZFront ^ 2;

        IntersectionState.StackNode[] stack = state.getStack();
        int stackPos = 0;
        int node = 0;

        while (true) {
            int tn = tree[node];
            int axis = tn & (3 << 30);
            int offset = tn & ~(3 << 30);
            switch (axis) {
                case 0: {
                    float d = (Float.intBitsToFloat(tree[node + 1]) - orgX) * invDirX;
                    int back = offset + offsetXBack;
                    node = back;
                    if (d < intervalMin) {
                        continue;
                    }
                    node = offset + offsetXFront; // front
                    if (d > intervalMax) {
                        continue;
                    }
                    // push back node
                    stack[stackPos].node = back;
                    stack[stackPos].near = (d >= intervalMin) ? d : intervalMin;
                    stack[stackPos].far = intervalMax;
                    stackPos++;
                    // update ray interval for front node
                    intervalMax = (d <= intervalMax) ? d : intervalMax;
                    continue;
                }
                case 1 << 30: {
                    // y axis
                    float d = (Float.intBitsToFloat(tree[node + 1]) - orgY) * invDirY;
                    int back = offset + offsetYBack;
                    node = back;
                    if (d < intervalMin) {
                        continue;
                    }
                    node = offset + offsetYFront; // front
                    if (d > intervalMax) {
                        continue;
                    }
                    // push back node
                    stack[stackPos].node = back;
                    stack[stackPos].near = (d >= intervalMin) ? d : intervalMin;
                    stack[stackPos].far = intervalMax;
                    stackPos++;
                    // update ray interval for front node
                    intervalMax = (d <= intervalMax) ? d : intervalMax;
                    continue;
                }
                case 2 << 30: {
                    // z axis
                    float d = (Float.intBitsToFloat(tree[node + 1]) - orgZ) * invDirZ;
                    int back = offset + offsetZBack;
                    node = back;
                    if (d < intervalMin) {
                        continue;
                    }
                    node = offset + offsetZFront; // front
                    if (d > intervalMax) {
                        continue;
                    }
                    // push back node
                    stack[stackPos].node = back;
                    stack[stackPos].near = (d >= intervalMin) ? d : intervalMin;
                    stack[stackPos].far = intervalMax;
                    stackPos++;
                    // update ray interval for front node
                    intervalMax = (d <= intervalMax) ? d : intervalMax;
                    continue;
                }
                default: {
                    // leaf - test some objects
                    int n = tree[node + 1];
                    while (n > 0) {
                        primitiveList.intersectPrimitive(r, primitives[offset], state);
                        n--;
                        offset++;
                    }
                    if (r.getMax() < intervalMax) {
                        return;
                    }
                    do {
                        // stack is empty?
                        if (stackPos == 0) {
                            return;
                        }
                        // move back up the stack
                        stackPos--;
                        intervalMin = stack[stackPos].near;
                        if (r.getMax() < intervalMin) {
                            continue;
                        }
                        node = stack[stackPos].node;
                        intervalMax = stack[stackPos].far;
                        break;
                    } while (true);
                }
            } // switch
        } // traversal loop
    }
}