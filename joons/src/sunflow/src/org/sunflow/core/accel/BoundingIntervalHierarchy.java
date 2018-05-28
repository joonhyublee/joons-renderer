package org.sunflow.core.accel;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.math.BoundingBox;
import org.sunflow.system.Memory;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.IntArray;

public class BoundingIntervalHierarchy implements AccelerationStructure {

    private int[] tree;
    private int[] objects;
    private PrimitiveList primitives;
    private BoundingBox bounds;
    private int maxPrims;

    public BoundingIntervalHierarchy() {
        maxPrims = 2;
    }

    @Override
    public void build(PrimitiveList primitives) {
        this.primitives = primitives;
        int n = primitives.getNumPrimitives();
        UI.printDetailed(Module.ACCEL, "Getting bounding box ...");
        bounds = primitives.getWorldBounds(null);
        objects = new int[n];
        for (int i = 0; i < n; i++) {
            objects[i] = i;
        }
        UI.printDetailed(Module.ACCEL, "Creating tree ...");
        int initialSize = 3 * (2 * 6 * n + 1);
        IntArray tempTree = new IntArray((initialSize + 3) / 4);
        BuildStats stats = new BuildStats();
        Timer t = new Timer();
        t.start();
        buildHierarchy(tempTree, objects, stats);
        t.end();
        UI.printDetailed(Module.ACCEL, "Trimming tree ...");
        tree = tempTree.trim();
        // display stats
        stats.printStats();
        UI.printDetailed(Module.ACCEL, "  * Creation time:  %s", t);
        UI.printDetailed(Module.ACCEL, "  * Usage of init:  %6.2f%%", (double) (100.0 * tree.length) / initialSize);
        UI.printDetailed(Module.ACCEL, "  * Tree memory:    %s", Memory.sizeof(tree));
        UI.printDetailed(Module.ACCEL, "  * Indices memory: %s", Memory.sizeof(objects));
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
        private int numBVH2;

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
            numBVH2 = 0;
        }

        void updateInner() {
            numNodes++;
        }

        void updateBVH2() {
            numBVH2++;
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
            UI.printDetailed(Module.ACCEL, "Tree stats:");
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
            UI.printDetailed(Module.ACCEL, "  * BVH2 nodes:     %d (%3d%%)", numBVH2, 100 * numBVH2 / (numNodes + numLeaves - 2 * numBVH2));
        }
    }

    private void buildHierarchy(IntArray tempTree, int[] indices, BuildStats stats) {
        // create space for the first node
        tempTree.add(3 << 30); // dummy leaf
        tempTree.add(0);
        tempTree.add(0);
        if (objects.length == 0) {
            return;
        }
        // seed bbox
        float[] gridBox = {bounds.getMinimum().x, bounds.getMaximum().x,
            bounds.getMinimum().y, bounds.getMaximum().y,
            bounds.getMinimum().z, bounds.getMaximum().z};
        float[] nodeBox = {bounds.getMinimum().x, bounds.getMaximum().x,
            bounds.getMinimum().y, bounds.getMaximum().y,
            bounds.getMinimum().z, bounds.getMaximum().z};
        // seed subdivide function
        subdivide(0, objects.length - 1, tempTree, indices, gridBox, nodeBox, 0, 1, stats);
    }

    private void createNode(IntArray tempTree, int nodeIndex, int left, int right) {
        // write leaf node
        tempTree.set(nodeIndex + 0, (3 << 30) | left);
        tempTree.set(nodeIndex + 1, right - left + 1);
    }

    private void subdivide(int left, int right, IntArray tempTree, int[] indices, float[] gridBox, float[] nodeBox, int nodeIndex, int depth, BuildStats stats) {
        if ((right - left + 1) <= maxPrims || depth >= 64) {
            // write leaf node
            stats.updateLeaf(depth, right - left + 1);
            createNode(tempTree, nodeIndex, left, right);
            return;
        }
        // calculate extents
        int axis = -1, prevAxis, rightOrig;
        float clipL = Float.NaN, clipR = Float.NaN, prevClip = Float.NaN;
        float split = Float.NaN, prevSplit;
        boolean wasLeft = true;
        while (true) {
            prevAxis = axis;
            prevSplit = split;
            // perform quick consistency checks
            float d[] = {gridBox[1] - gridBox[0], gridBox[3] - gridBox[2],
                gridBox[5] - gridBox[4]};
            if (d[0] < 0 || d[1] < 0 || d[2] < 0) {
                throw new IllegalStateException("negative node extents");
            }
            for (int i = 0; i < 3; i++) {
                if (nodeBox[2 * i + 1] < gridBox[2 * i] || nodeBox[2 * i] > gridBox[2 * i + 1]) {
                    UI.printError(Module.ACCEL, "Reached tree area in error - discarding node with: %d objects", right - left + 1);
                    throw new IllegalStateException("invalid node overlap");
                }
            }
            // find longest axis
            if (d[0] > d[1] && d[0] > d[2]) {
                axis = 0;
            } else if (d[1] > d[2]) {
                axis = 1;
            } else {
                axis = 2;
            }
            split = 0.5f * (gridBox[2 * axis] + gridBox[2 * axis + 1]);
            // partition L/R subsets
            clipL = Float.NEGATIVE_INFINITY;
            clipR = Float.POSITIVE_INFINITY;
            rightOrig = right; // save this for later
            float nodeL = Float.POSITIVE_INFINITY;
            float nodeR = Float.NEGATIVE_INFINITY;
            for (int i = left; i <= right;) {
                int obj = indices[i];
                float minb = primitives.getPrimitiveBound(obj, 2 * axis + 0);
                float maxb = primitives.getPrimitiveBound(obj, 2 * axis + 1);
                float center = (minb + maxb) * 0.5f;
                if (center <= split) {
                    // stay left
                    i++;
                    if (clipL < maxb) {
                        clipL = maxb;
                    }
                } else {
                    // move to the right most
                    int t = indices[i];
                    indices[i] = indices[right];
                    indices[right] = t;
                    right--;
                    if (clipR > minb) {
                        clipR = minb;
                    }
                }
                if (nodeL > minb) {
                    nodeL = minb;
                }
                if (nodeR < maxb) {
                    nodeR = maxb;
                }
            }
            // check for empty space
            if (nodeL > nodeBox[2 * axis + 0] && nodeR < nodeBox[2 * axis + 1]) {
                float nodeBoxW = nodeBox[2 * axis + 1] - nodeBox[2 * axis + 0];
                float nodeNewW = nodeR - nodeL;
                // node box is too big compare to space occupied by primitives?
                if (1.3f * nodeNewW < nodeBoxW) {
                    stats.updateBVH2();
                    int nextIndex = tempTree.getSize();
                    // allocate child
                    tempTree.add(0);
                    tempTree.add(0);
                    tempTree.add(0);
                    // write bvh2 clip node
                    stats.updateInner();
                    tempTree.set(nodeIndex + 0, (axis << 30) | (1 << 29) | nextIndex);
                    tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(nodeL));
                    tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(nodeR));
                    // update nodebox and recurse
                    nodeBox[2 * axis + 0] = nodeL;
                    nodeBox[2 * axis + 1] = nodeR;
                    subdivide(left, rightOrig, tempTree, indices, gridBox, nodeBox, nextIndex, depth + 1, stats);
                    return;
                }
            }
            // ensure we are making progress in the subdivision
            if (right == rightOrig) {
                // all left
                if (clipL <= split) {
                    // keep looping on left half
                    gridBox[2 * axis + 1] = split;
                    prevClip = clipL;
                    wasLeft = true;
                    continue;
                }
                if (prevAxis == axis && prevSplit == split) {
                    // we are stuck here - create a leaf
                    stats.updateLeaf(depth, right - left + 1);
                    createNode(tempTree, nodeIndex, left, right);
                    return;
                }
                gridBox[2 * axis + 1] = split;
                prevClip = Float.NaN;
            } else if (left > right) {
                // all right
                right = rightOrig;
                if (clipR >= split) {
                    // keep looping on right half
                    gridBox[2 * axis + 0] = split;
                    prevClip = clipR;
                    wasLeft = false;
                    continue;
                }
                if (prevAxis == axis && prevSplit == split) {
                    // we are stuck here - create a leaf
                    stats.updateLeaf(depth, right - left + 1);
                    createNode(tempTree, nodeIndex, left, right);
                    return;
                }
                gridBox[2 * axis + 0] = split;
                prevClip = Float.NaN;
            } else {
                // we are actually splitting stuff
                if (prevAxis != -1 && !Float.isNaN(prevClip)) {
                    // second time through - lets create the previous split
                    // since it produced empty space
                    int nextIndex = tempTree.getSize();
                    // allocate child node
                    tempTree.add(0);
                    tempTree.add(0);
                    tempTree.add(0);
                    if (wasLeft) {
                        // create a node with a left child
                        // write leaf node
                        stats.updateInner();
                        tempTree.set(nodeIndex + 0, (prevAxis << 30) | nextIndex);
                        tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(prevClip));
                        tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
                    } else {
                        // create a node with a right child
                        // write leaf node
                        stats.updateInner();
                        tempTree.set(nodeIndex + 0, (prevAxis << 30) | (nextIndex - 3));
                        tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(Float.NEGATIVE_INFINITY));
                        tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(prevClip));
                    }
                    // count stats for the unused leaf
                    depth++;
                    stats.updateLeaf(depth, 0);
                    // now we keep going as we are, with a new nodeIndex:
                    nodeIndex = nextIndex;
                }
                break;
            }
        }
        // compute index of child nodes
        int nextIndex = tempTree.getSize();
        // allocate left node
        int nl = right - left + 1;
        int nr = rightOrig - (right + 1) + 1;
        if (nl > 0) {
            tempTree.add(0);
            tempTree.add(0);
            tempTree.add(0);
        } else {
            nextIndex -= 3;
        }
        // allocate right node
        if (nr > 0) {
            tempTree.add(0);
            tempTree.add(0);
            tempTree.add(0);
        }
        // write leaf node
        stats.updateInner();
        tempTree.set(nodeIndex + 0, (axis << 30) | nextIndex);
        tempTree.set(nodeIndex + 1, Float.floatToRawIntBits(clipL));
        tempTree.set(nodeIndex + 2, Float.floatToRawIntBits(clipR));
        // prepare L/R child boxes
        float[] gridBoxL = new float[6];
        float[] gridBoxR = new float[6];
        float[] nodeBoxL = new float[6];
        float[] nodeBoxR = new float[6];
        for (int i = 0; i < 6; i++) {
            gridBoxL[i] = gridBoxR[i] = gridBox[i];
            nodeBoxL[i] = nodeBoxR[i] = nodeBox[i];
        }
        gridBoxL[2 * axis + 1] = gridBoxR[2 * axis] = split;
        nodeBoxL[2 * axis + 1] = clipL;
        nodeBoxR[2 * axis + 0] = clipR;
        // free memory
        gridBox = nodeBox = null;
        // recurse
        if (nl > 0) {
            subdivide(left, right, tempTree, indices, gridBoxL, nodeBoxL, nextIndex, depth + 1, stats);
        } else {
            stats.updateLeaf(depth + 1, 0);
        }
        if (nr > 0) {
            subdivide(right + 1, rightOrig, tempTree, indices, gridBoxR, nodeBoxR, nextIndex + 3, depth + 1, stats);
        } else {
            stats.updateLeaf(depth + 1, 0);
        }
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

        int offsetXFront = Float.floatToRawIntBits(dirX) >>> 31;
        int offsetYFront = Float.floatToRawIntBits(dirY) >>> 31;
        int offsetZFront = Float.floatToRawIntBits(dirZ) >>> 31;

        int offsetXBack = offsetXFront ^ 1;
        int offsetYBack = offsetYFront ^ 1;
        int offsetZBack = offsetZFront ^ 1;

        int offsetXFront3 = offsetXFront * 3;
        int offsetYFront3 = offsetYFront * 3;
        int offsetZFront3 = offsetZFront * 3;

        int offsetXBack3 = offsetXBack * 3;
        int offsetYBack3 = offsetYBack * 3;
        int offsetZBack3 = offsetZBack * 3;

        // avoid always adding 1 during the inner loop
        offsetXFront++;
        offsetYFront++;
        offsetZFront++;
        offsetXBack++;
        offsetYBack++;
        offsetZBack++;

        IntersectionState.StackNode[] stack = state.getStack();
        int stackPos = 0;
        int node = 0;

        while (true) {
            pushloop:
            while (true) {
                int tn = tree[node];
                int axis = tn & (7 << 29);
                int offset = tn & ~(7 << 29);
                switch (axis) {
                    case 0: {
                        // x axis
                        float tf = (Float.intBitsToFloat(tree[node + offsetXFront]) - orgX) * invDirX;
                        float tb = (Float.intBitsToFloat(tree[node + offsetXBack]) - orgX) * invDirX;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax) {
                            break pushloop;
                        }
                        int back = offset + offsetXBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetXFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        stack[stackPos].node = back;
                        stack[stackPos].near = (tb >= intervalMin) ? tb : intervalMin;
                        stack[stackPos].far = intervalMax;
                        stackPos++;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    case 1 << 30: {
                        float tf = (Float.intBitsToFloat(tree[node + offsetYFront]) - orgY) * invDirY;
                        float tb = (Float.intBitsToFloat(tree[node + offsetYBack]) - orgY) * invDirY;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax) {
                            break pushloop;
                        }
                        int back = offset + offsetYBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetYFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        stack[stackPos].node = back;
                        stack[stackPos].near = (tb >= intervalMin) ? tb : intervalMin;
                        stack[stackPos].far = intervalMax;
                        stackPos++;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    case 2 << 30: {
                        // z axis
                        float tf = (Float.intBitsToFloat(tree[node + offsetZFront]) - orgZ) * invDirZ;
                        float tb = (Float.intBitsToFloat(tree[node + offsetZBack]) - orgZ) * invDirZ;
                        // ray passes between clip zones
                        if (tf < intervalMin && tb > intervalMax) {
                            break pushloop;
                        }
                        int back = offset + offsetZBack3;
                        node = back;
                        // ray passes through far node only
                        if (tf < intervalMin) {
                            intervalMin = (tb >= intervalMin) ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetZFront3; // front
                        // ray passes through near node only
                        if (tb > intervalMax) {
                            intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                            continue;
                        }
                        // ray passes through both nodes
                        // push back node
                        stack[stackPos].node = back;
                        stack[stackPos].near = (tb >= intervalMin) ? tb : intervalMin;
                        stack[stackPos].far = intervalMax;
                        stackPos++;
                        // update ray interval for front node
                        intervalMax = (tf <= intervalMax) ? tf : intervalMax;
                        continue;
                    }
                    case 3 << 30: {
                        // leaf - test some objects
                        int n = tree[node + 1];
                        while (n > 0) {
                            primitives.intersectPrimitive(r, objects[offset], state);
                            n--;
                            offset++;
                        }
                        break pushloop;
                    }
                    case 1 << 29: {
                        float tf = (Float.intBitsToFloat(tree[node + offsetXFront]) - orgX) * invDirX;
                        float tb = (Float.intBitsToFloat(tree[node + offsetXBack]) - orgX) * invDirX;
                        node = offset;
                        intervalMin = (tf >= intervalMin) ? tf : intervalMin;
                        intervalMax = (tb <= intervalMax) ? tb : intervalMax;
                        if (intervalMin > intervalMax) {
                            break pushloop;
                        }
                        continue;
                    }
                    case 3 << 29: {
                        float tf = (Float.intBitsToFloat(tree[node + offsetYFront]) - orgY) * invDirY;
                        float tb = (Float.intBitsToFloat(tree[node + offsetYBack]) - orgY) * invDirY;
                        node = offset;
                        intervalMin = (tf >= intervalMin) ? tf : intervalMin;
                        intervalMax = (tb <= intervalMax) ? tb : intervalMax;
                        if (intervalMin > intervalMax) {
                            break pushloop;
                        }
                        continue;
                    }
                    case 5 << 29: {
                        float tf = (Float.intBitsToFloat(tree[node + offsetZFront]) - orgZ) * invDirZ;
                        float tb = (Float.intBitsToFloat(tree[node + offsetZBack]) - orgZ) * invDirZ;
                        node = offset;
                        intervalMin = (tf >= intervalMin) ? tf : intervalMin;
                        intervalMax = (tb <= intervalMax) ? tb : intervalMax;
                        if (intervalMin > intervalMax) {
                            break pushloop;
                        }
                        continue;
                    }
                    default:
                        return; // should not happen
                } // switch
            } // traversal loop
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
    }
}