package org.sunflow.core;

import org.sunflow.SunflowAPI;

/**
 * This is the base interface for all public rendering object interfaces. It
 * handles incremental updates via {@link ParameterList} objects.
 */
public interface RenderObject {

    /**
     * Update this object given a list of parameters. This method is guarenteed
     * to be called at least once on every object, but it should correctly
     * handle empty parameter lists. This means that the object should be in a
     * valid state from the time it is constructed. This method should also
     * return true or false depending on whether the update was succesfull or
     * not.
     *
     * @param pl list of parameters to read from
     * @param api reference to the current scene
     * @return <code>true</code> if the update is succesfull, <code>false</code>
     * otherwise
     */
    public boolean update(ParameterList pl, SunflowAPI api);
}