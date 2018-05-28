package org.sunflow.core;

import org.sunflow.SunflowAPI;
import org.sunflow.SunflowAPIInterface;

/**
 * Simple interface to allow for scene creation from arbitrary file formats.
 */
public interface SceneParser {
    
    final String ASPECT = "aspect";
    final String BACKGROUND = "background";
    final String CENTER = "center";
    final String COLOR = "color";
    final String DIFF = "diff";
    final String DIFFUSE = "diffuse";
    final String EMIT = "emit";
    final String FACEVARYING = "facevarying";
    final String FILTER = "filter";
    final String FOV = "fov";
    final String GI_ENGINE = "gi.engine";
    final String MODIFIER = "modifier";
    final String MODIFIERS = "modifiers";
    final String NAME = "name";
    final String NONE = "none";
    final String NORMALS = "normals";
    final String POINTS = "points";
    final String POINT = "point";
    final String POWER = "power";
    final String VERTEX = "vertex";
    final String RADIUS = "radius";
    final String RADIANCE = "radiance";
    final String REFL = "refl";
    final String SAMPLES = "samples";
    final String SCALE = "scale";
    final String SHADER = "shader";
    final String SHADERS = "shaders";
    final String SMOOTH = "scale";
    final String SUBDIVS = "subdivs";
    final String TEXTURE = "texture";
    final String TEXCOORD = "texcoord";
    final String TRANSFORM = "transform";
    final String TRIANGLES = "triangles";    
    final String TRIANGLE_MESH = "triangle_mesh";
    final String TYPE = "type";
    final String UVS = "uvs";

    /**
     * Parse the specified file to create a scene description into the provided
     * {@link SunflowAPI} object.
     *
     * @param filename filename to parse
     * @param api scene to parse the file into
     * @return <code>true</code> upon success, or <code>false</code> if errors
     * have occurred.
     */
    public boolean parse(String filename, SunflowAPIInterface api);
}