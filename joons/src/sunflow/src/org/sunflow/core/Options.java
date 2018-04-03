package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.util.FastHashMap;

/**
 * This holds rendering objects as key, value pairs.
 */
public final class Options extends ParameterList implements RenderObject {

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        // take all attributes, and update them into the current set
        for (FastHashMap.Entry<String, Parameter> e : pl.list) {
            list.put(e.getKey(), e.getValue());
            e.getValue().check();
        }
        return true;
    }
}