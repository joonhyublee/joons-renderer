package org.sunflow.core;

import java.util.Locale;

import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.MovingMatrix4;
import org.sunflow.math.Point2;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FastHashMap;

/**
 * This class holds a list of "parameters". These are defined and then passed
 * onto rendering objects through the API. They can hold arbitrary typed and
 * named variables as a unified way of getting data into user objects.
 */
public class ParameterList {

    protected final FastHashMap<String, Parameter> list;
    private int numVerts, numFaces, numFaceVerts;

    private enum ParameterType {

        STRING, INT, BOOL, FLOAT, POINT, VECTOR, TEXCOORD, MATRIX, COLOR
    }

    public enum InterpolationType {

        NONE, FACE, VERTEX, FACEVARYING
    }

    /**
     * Creates an empty ParameterList.
     */
    public ParameterList() {
        list = new FastHashMap<String, Parameter>();
        numVerts = numFaces = numFaceVerts = 0;
    }

    /**
     * Clears the list of all its members. If some members were never used, a
     * warning will be printed to remind the user something may be wrong.
     */
    public void clear(boolean showUnused) {
        if (showUnused) {
            for (FastHashMap.Entry<String, Parameter> e : list) {
                if (!e.getValue().checked) {
                    UI.printWarning(Module.API, "Unused parameter: %s - %s", e.getKey(), e.getValue());
                }
            }
        }
        list.clear();
        numVerts = numFaces = numFaceVerts = 0;
    }

    /**
     * Setup how many faces should be used to check member count on "face"
     * interpolated parameters.
     *
     * @param numFaces number of faces
     */
    public void setFaceCount(int numFaces) {
        this.numFaces = numFaces;
    }

    /**
     * Setup how many vertices should be used to check member count of "vertex"
     * interpolated parameters.
     *
     * @param numVerts number of vertices
     */
    public void setVertexCount(int numVerts) {
        this.numVerts = numVerts;
    }

    /**
     * Setup how many "face-vertices" should be used to check member count of
     * "facevarying" interpolated parameters. This should be equal to the sum of
     * the number of vertices on each face.
     *
     * @param numFaceVerts number of "face-vertices"
     */
    public void setFaceVertexCount(int numFaceVerts) {
        this.numFaceVerts = numFaceVerts;
    }

    /**
     * Add the specified string as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void addString(String name, String value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified integer as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void addInteger(String name, int value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified boolean as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void addBoolean(String name, boolean value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified float as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void addFloat(String name, float value) {
        add(name, new Parameter(value));
    }

    /**
     * Add the specified color as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public void addColor(String name, Color value) {
        if (value == null) {
            throw new NullPointerException();
        }
        add(name, new Parameter(value));
    }

    /**
     * Add the specified array of integers as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param array parameter value
     */
    public void addIntegerArray(String name, int[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        add(name, new Parameter(array));
    }

    /**
     * Add the specified array of integers as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param array parameter value
     */
    public void addStringArray(String name, String[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        add(name, new Parameter(array));
    }

    /**
     * Add the specified floats as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param interp interpolation type
     * @param data parameter value
     */
    public void addFloats(String name, InterpolationType interp, float[] data) {
        if (data == null) {
            UI.printError(Module.API, "Cannot create float parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.FLOAT, interp, data));
    }

    /**
     * Add the specified points as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param interp interpolation type
     * @param data parameter value
     */
    public void addPoints(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 3 != 0) {
            UI.printError(Module.API, "Cannot create point parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.POINT, interp, data));
    }

    /**
     * Add the specified vectors as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param interp interpolation type
     * @param data parameter value
     */
    public void addVectors(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 3 != 0) {
            UI.printError(Module.API, "Cannot create vector parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.VECTOR, interp, data));
    }

    /**
     * Add the specified texture coordinates as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param interp interpolation type
     * @param data parameter value
     */
    public void addTexCoords(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 2 != 0) {
            UI.printError(Module.API, "Cannot create texcoord parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.TEXCOORD, interp, data));
    }

    /**
     * Add the specified matrices as a parameter.
     * <code>null</code> values are not permitted.
     *
     * @param name parameter name
     * @param interp interpolation type
     * @param data parameter value
     */
    public void addMatrices(String name, InterpolationType interp, float[] data) {
        if (data == null || data.length % 16 != 0) {
            UI.printError(Module.API, "Cannot create matrix parameter %s -- invalid data length", name);
            return;
        }
        add(name, new Parameter(ParameterType.MATRIX, interp, data));
    }

    private void add(String name, Parameter param) {
        if (name == null) {
            UI.printError(Module.API, "Cannot declare parameter with null name");
        } else if (list.put(name, param) != null) {
            UI.printWarning(Module.API, "Parameter %s was already defined -- overwriting", name);
        }
    }

    /**
     * Get the specified string parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public String getString(String name, String defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.STRING, InterpolationType.NONE, 1, p)) {
            return p.getStringValue();
        }
        return defaultValue;
    }

    /**
     * Get the specified string array parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public String[] getStringArray(String name, String[] defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.STRING, InterpolationType.NONE, -1, p)) {
            return p.getStrings();
        }
        return defaultValue;
    }

    /**
     * Get the specified integer parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public int getInt(String name, int defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.INT, InterpolationType.NONE, 1, p)) {
            return p.getIntValue();
        }
        return defaultValue;
    }

    /**
     * Get the specified integer array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public int[] getIntArray(String name) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.INT, InterpolationType.NONE, -1, p)) {
            return p.getInts();
        }
        return null;
    }

    /**
     * Get the specified boolean parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.BOOL, InterpolationType.NONE, 1, p)) {
            return p.getBoolValue();
        }
        return defaultValue;
    }

    /**
     * Get the specified float parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public float getFloat(String name, float defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.FLOAT, InterpolationType.NONE, 1, p)) {
            return p.getFloatValue();
        }
        return defaultValue;
    }

    /**
     * Get the specified color parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public Color getColor(String name, Color defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.COLOR, InterpolationType.NONE, 1, p)) {
            return p.getColor();
        }
        return defaultValue;
    }

    /**
     * Get the specified point parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public Point3 getPoint(String name, Point3 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.POINT, InterpolationType.NONE, 1, p)) {
            return p.getPoint();
        }
        return defaultValue;
    }

    /**
     * Get the specified vector parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public Vector3 getVector(String name, Vector3 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.VECTOR, InterpolationType.NONE, 1, p)) {
            return p.getVector();
        }
        return defaultValue;
    }

    /**
     * Get the specified texture coordinate parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public Point2 getTexCoord(String name, Point2 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.TEXCOORD, InterpolationType.NONE, 1, p)) {
            return p.getTexCoord();
        }
        return defaultValue;
    }

    /**
     * Get the specified matrix parameter from this list.
     *
     * @param name name of the parameter
     * @param defaultValue value to return if not found
     * @return the value of the parameter specified or default value if not
     * found
     */
    public Matrix4 getMatrix(String name, Matrix4 defaultValue) {
        Parameter p = list.get(name);
        if (isValidParameter(name, ParameterType.MATRIX, InterpolationType.NONE, 1, p)) {
            return p.getMatrix();
        }
        return defaultValue;
    }

    /**
     * Get the specified float array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public FloatParameter getFloatArray(String name) {
        return getFloatParameter(name, ParameterType.FLOAT, list.get(name));
    }

    /**
     * Get the specified point array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public FloatParameter getPointArray(String name) {
        return getFloatParameter(name, ParameterType.POINT, list.get(name));
    }

    /**
     * Get the specified vector array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public FloatParameter getVectorArray(String name) {
        return getFloatParameter(name, ParameterType.VECTOR, list.get(name));
    }

    /**
     * Get the specified texture coordinate array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public FloatParameter getTexCoordArray(String name) {
        return getFloatParameter(name, ParameterType.TEXCOORD, list.get(name));
    }

    /**
     * Get the specified matrix array parameter from this list.
     *
     * @param name name of the parameter
     * @return the value of the parameter specified or <code>null</code> if not
     * found
     */
    public FloatParameter getMatrixArray(String name) {
        return getFloatParameter(name, ParameterType.MATRIX, list.get(name));
    }

    private boolean isValidParameter(String name, ParameterType type, InterpolationType interp, int requestedSize, Parameter p) {
        if (p == null) {
            return false;
        }
        if (p.type != type) {
            UI.printWarning(Module.API, "Parameter %s requested as a %s - declared as %s", name, type.name().toLowerCase(Locale.ENGLISH), p.type.name().toLowerCase(Locale.ENGLISH));
            return false;
        }
        if (p.interp != interp) {
            UI.printWarning(Module.API, "Parameter %s requested as a %s - declared as %s", name, interp.name().toLowerCase(Locale.ENGLISH), p.interp.name().toLowerCase(Locale.ENGLISH));
            return false;
        }
        if (requestedSize > 0 && p.size() != requestedSize) {
            UI.printWarning(Module.API, "Parameter %s requires %d %s - declared with %d", name, requestedSize, requestedSize == 1 ? "value" : "values", p.size());
            return false;
        }
        p.checked = true;
        return true;
    }

    private FloatParameter getFloatParameter(String name, ParameterType type, Parameter p) {
        if (p == null) {
            return null;
        }
        switch (p.interp) {
            case NONE:
                if (!isValidParameter(name, type, p.interp, -1, p)) {
                    return null;
                }
                break;
            case VERTEX:
                if (!isValidParameter(name, type, p.interp, numVerts, p)) {
                    return null;
                }
                break;
            case FACE:
                if (!isValidParameter(name, type, p.interp, numFaces, p)) {
                    return null;
                }
                break;
            case FACEVARYING:
                if (!isValidParameter(name, type, p.interp, numFaceVerts, p)) {
                    return null;
                }
                break;
            default:
                return null;
        }
        return p.getFloats();
    }

    /**
     * Represents an array of floating point values. This can store single
     * float, points, vectors, texture coordinates or matrices. The parameter
     * should be interpolated over the surface according to the interp parameter
     * when applicable.
     */
    public static final class FloatParameter {

        public final InterpolationType interp;
        public final float[] data;

        public FloatParameter() {
            this(InterpolationType.NONE, null);
        }

        public FloatParameter(float f) {
            this(InterpolationType.NONE, new float[]{f});
        }

        private FloatParameter(InterpolationType interp, float[] data) {
            this.interp = interp;
            this.data = data;
        }
    }

    public final MovingMatrix4 getMovingMatrix(String name, MovingMatrix4 defaultValue) {
        // step 1: check for a non-moving specification:
        Matrix4 m = getMatrix(name, null);
        if (m != null) {
            return new MovingMatrix4(m);
        }
        // step 2: check to see if the time range has been updated
        FloatParameter times = getFloatArray(name + ".times");
        if (times != null) {
            if (times.data.length <= 1) {
                defaultValue.updateTimes(0, 0);
            } else {
                if (times.data.length != 2) {
                    UI.printWarning(Module.API, "Time value specification using only endpoints of %d values specified", times.data.length);
                }
                // get endpoint times - we might allow multiple time values
                // later
                float t0 = times.data[0];
                float t1 = times.data[times.data.length - 1];
                defaultValue.updateTimes(t0, t1);
            }
        } else {
            // time range stays at default
        }
        // step 3: check to see if a number of steps has been specified
        int steps = getInt(name + ".steps", 0);
        if (steps <= 0) {
            // not specified - return default value
        } else {
            // update each element
            defaultValue.setSteps(steps);
            for (int i = 0; i < steps; i++) {
                defaultValue.updateData(i, getMatrix(String.format("%s[%d]", name, i), defaultValue.getData(i)));
            }
        }
        return defaultValue;
    }

    protected static final class Parameter {

        private ParameterType type;
        private InterpolationType interp;
        private Object obj;
        private boolean checked;

        private Parameter(String value) {
            type = ParameterType.STRING;
            interp = InterpolationType.NONE;
            obj = new String[]{value};
            checked = false;
        }

        private Parameter(int value) {
            type = ParameterType.INT;
            interp = InterpolationType.NONE;
            obj = new int[]{value};
            checked = false;
        }

        private Parameter(boolean value) {
            type = ParameterType.BOOL;
            interp = InterpolationType.NONE;
            obj = value;
            checked = false;
        }

        private Parameter(float value) {
            type = ParameterType.FLOAT;
            interp = InterpolationType.NONE;
            obj = new float[]{value};
            checked = false;
        }

        private Parameter(int[] array) {
            type = ParameterType.INT;
            interp = InterpolationType.NONE;
            obj = array;
            checked = false;
        }

        private Parameter(String[] array) {
            type = ParameterType.STRING;
            interp = InterpolationType.NONE;
            obj = array;
            checked = false;
        }

        private Parameter(Color c) {
            type = ParameterType.COLOR;
            interp = InterpolationType.NONE;
            obj = c;
            checked = false;
        }

        private Parameter(ParameterType type, InterpolationType interp, float[] data) {
            this.type = type;
            this.interp = interp;
            obj = data;
            checked = false;
        }

        private int size() {
            // number of elements
            switch (type) {
                case STRING:
                    return ((String[]) obj).length;
                case INT:
                    return ((int[]) obj).length;
                case BOOL:
                    return 1;
                case FLOAT:
                    return ((float[]) obj).length;
                case POINT:
                    return ((float[]) obj).length / 3;
                case VECTOR:
                    return ((float[]) obj).length / 3;
                case TEXCOORD:
                    return ((float[]) obj).length / 2;
                case MATRIX:
                    return ((float[]) obj).length / 16;
                case COLOR:
                    return 1;
                default:
                    return -1;
            }
        }

        protected void check() {
            checked = true;
        }

        @Override
        public String toString() {
            return String.format("%s%s[%d]", interp == InterpolationType.NONE ? "" : interp.name().toLowerCase() + " ", type.name().toLowerCase(), size());
        }

        private String getStringValue() {
            return ((String[]) obj)[0];
        }

        private boolean getBoolValue() {
            return (Boolean) obj;
        }

        private int getIntValue() {
            return ((int[]) obj)[0];
        }

        private int[] getInts() {
            return (int[]) obj;
        }

        private String[] getStrings() {
            return (String[]) obj;
        }

        private float getFloatValue() {
            return ((float[]) obj)[0];
        }

        private FloatParameter getFloats() {
            return new FloatParameter(interp, (float[]) obj);
        }

        private Point3 getPoint() {
            float[] floats = (float[]) obj;
            return new Point3(floats[0], floats[1], floats[2]);
        }

        private Vector3 getVector() {
            float[] floats = (float[]) obj;
            return new Vector3(floats[0], floats[1], floats[2]);
        }

        private Point2 getTexCoord() {
            float[] floats = (float[]) obj;
            return new Point2(floats[0], floats[1]);
        }

        private Matrix4 getMatrix() {
            float[] floats = (float[]) obj;
            return new Matrix4(floats, true);
        }

        private Color getColor() {
            return (Color) obj;
        }
    }
}