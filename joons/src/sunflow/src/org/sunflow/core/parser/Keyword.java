package org.sunflow.core.parser;
/**
 * Public enums should live in their own file, we could do more here I am sure
 * including things like enumset getting String value etc.
 * @author Martin Prout
 */
public enum Keyword {
    RESET, 
    PARAMETER, 
    GEOMETRY, 
    INSTANCE, 
    SHADER, 
    MODIFIER, 
    LIGHT, 
    CAMERA, 
    OPTIONS, 
    INCLUDE, 
    REMOVE, 
    FRAME,
    PLUGIN, 
    SEARCHPATH, 
    STRING, 
    BOOL, 
    INT, 
    FLOAT, 
    COLOR, 
    POINT, 
    VECTOR, 
    TEXCOORD, 
    MATRIX, 
    STRING_ARRAY, 
    INT_ARRAY, 
    FLOAT_ARRAY, 
    POINT_ARRAY, 
    VECTOR_ARRAY, 
    TEXCOORD_ARRAY, 
    MATRIX_ARRAY, 
    END_OF_FILE,
}