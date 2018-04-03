package org.sunflow.core.bucket;

import org.sunflow.PluginRegistry;
import org.sunflow.core.BucketOrder;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class BucketOrderFactory {

    public static BucketOrder create(String order) {
        boolean flip = false;
        if (order.startsWith("inverse") || order.startsWith("invert") || order.startsWith("reverse")) {
            String[] tokens = order.split("\\s+");
            if (tokens.length == 2) {
                order = tokens[1];
                flip = true;
            }
        }
        BucketOrder o = PluginRegistry.bucketOrderPlugins.createObject(order);
        if (o == null) {
            UI.printWarning(Module.BCKT, "Unrecognized bucket ordering: \"%s\" - using hilbert", order);
            return create("hilbert");
        }
        return flip ? new InvertedBucketOrder(o) : o;
    }
}