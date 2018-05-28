package org.sunflow;

import org.sunflow.core.Display;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.RenderObject;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;

/**
 * This interface represents the entry point for rendering scenes using Sunflow.
 * Classes which implement this interface are able to receive input from any of
 * the Sunflow parsers.
 */
public interface SunflowAPIInterface {

    /**
     * Reset the state of the API completely. The object table is cleared, and
     * all search paths are set back to their default values.
     */
    public void reset();

    /**
     * Declare a plugin of the specified type with the given name from a java
     * code string. The code will be compiled with Janino and registered as a
     * new plugin type upon success.
     *
     * @param type
     * @param name
     * @param code
     */
    public void plugin(String type, String name, String code);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, String value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, boolean value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, int value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, float value);

    /**
     * Declare a color parameter in the given colorspace using the specified
     * name and value. This parameter will be added to the currently active
     * parameter list.
     *
     * @param name parameter name
     * @param colorspace color space or <code>null</code> to assume internal
     * color space
     * @param data floating point color data
     */
    public void parameter(String name, String colorspace, float... data);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, Point3 value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, Vector3 value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, Point2 value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, Matrix4 value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, int[] value);

    /**
     * Declare a parameter with the specified name and value. This parameter
     * will be added to the currently active parameter list.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void parameter(String name, String[] value);

    /**
     * Declare a parameter with the specified name. The type may be one of the
     * follow: "float", "point", "vector", "texcoord", "matrix". The
     * interpolation determines how the parameter is to be interpreted over
     * surface (see {@link InterpolationType}). The data is specified in a
     * flattened float array.
     *
     * @param name parameter name
     * @param type parameter data type
     * @param interpolation parameter interpolation mode
     * @param data raw floating point data
     */
    public void parameter(String name, String type, String interpolation, float[] data);

    /**
     * Remove the specified render object. Note that this may cause the removal
     * of other objects which depended on it.
     *
     * @param name name of the object to remove
     */
    public void remove(String name);

    /**
     * Add the specified path to the list of directories which are searched
     * automatically to resolve scene filenames or textures. Currently the
     * supported searchpath types are: "include" and "texture". All other types
     * will be ignored.
     *
     * @param path
     */
    public void searchpath(String type, String path);

    /**
     * Defines a shader with a given name. If the shader type name is left
     * <code>null</code>, the shader with the given name will be updated (if it
     * exists).
     *
     * @param name a unique name given to the shader
     * @param shaderType a shader plugin type
     */
    public void shader(String name, String shaderType);

    /**
     * Defines a modifier with a given name. If the modifier type name is left
     * <code>null</code>, the modifier with the given name will be updated (if
     * it exists).
     *
     * @param name a unique name given to the modifier
     * @param modifierType a modifier plugin type name
     */
    public void modifier(String name, String modifierType);

    /**
     * Defines a geometry with a given name. The geometry is built from the
     * specified type. Note that geometries may be created from
     * {@link Tesselatable} objects or {@link PrimitiveList} objects. This means
     * that two seperate plugin lists will be searched for the geometry type.
     * {@link Tesselatable} objects are search first. If the type name is left
     * <code>null</code>, the geometry with the given name will be updated (if
     * it exists).
     *
     * @param name a unique name given to the geometry
     * @param typeName a tesselatable or primitive plugin type name
     */
    public void geometry(String name, String typeName);

    /**
     * Instance the specified geometry into the scene. If geoname is
     * <code>null</code>, the specified instance object will be updated (if it
     * exists). In order to change the instancing relationship of an existing
     * instance, you should use the "geometry" string attribute.
     *
     * @param name instance name
     * @param geoname name of the geometry to instance
     */
    public void instance(String name, String geoname);

    /**
     * Defines a light source with a given name. If the light type name is left
     * <code>null</code>, the light source with the given name will be updated
     * (if it exists).
     *
     * @param name a unique name given to the light source
     * @param lightType a light source plugin type name
     */
    public void light(String name, String lightType);

    /**
     * Defines a camera with a given name. The camera is built from the
     * specified camera lens type plugin. If the lens type name is left
     * <code>null</code>, the camera with the given name will be updated (if it
     * exists). It is not currently possible to change the lens of a camera
     * after it has been created.
     *
     * @param name camera name
     * @param lensType a camera lens plugin type name
     */
    public void camera(String name, String lensType);

    /**
     * Defines an option object to hold the current parameters. If the object
     * already exists, the values will simply override previous ones.
     *
     * @param name
     */
    public void options(String name);

    /**
     * Render using the specified options and the specified display. If the
     * specified options do not exist - defaults will be used.
     *
     * @param optionsName name of the {@link RenderObject} which contains the
     * options
     * @param display display object
     */
    public void render(String optionsName, Display display);

    /**
     * Parse the specified filename. The include paths are searched first. The
     * contents of the file are simply added to the active scene. This allows to
     * break up a scene into parts, even across file formats. The appropriate
     * parser is chosen based on file extension.
     *
     * @param filename filename to load
     * @return <code>true</code> upon sucess, <code>false</code> if an error
     * occured.
     */
    public boolean include(String filename);

    /**
     * Set the value of the current frame. This value is intended only for
     * procedural animation creation. It is not used by the Sunflow core in
     * anyway. The default value is 1.
     *
     * @param currentFrame current frame number
     */
    public void currentFrame(int currentFrame);
}