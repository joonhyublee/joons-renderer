package org.sunflow.core;

import org.sunflow.PluginRegistry;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

class AccelerationStructureFactory {

    static AccelerationStructure create(String name, int n, boolean primitives) {
        if (name == null || name.equals("auto")) {
            if (primitives) {
                if (n > 20000000) {
                    name = "uniformgrid";
                } else if (n > 2000000) {
                    name = "bih";
                } else if (n > 2) {
                    name = "kdtree";
                } else {
                    name = "null";
                }
            } else {
                if (n > 2) {
                    name = "bih";
                } else {
                    name = "null";
                }
            }
        }
        AccelerationStructure accel = PluginRegistry.accelPlugins.createObject(name);
        if (accel == null) {
            UI.printWarning(Module.ACCEL, "Unrecognized intersection accelerator \"%s\" - using auto", name);
            return create(null, n, primitives);
        }
        return accel;
    }
}