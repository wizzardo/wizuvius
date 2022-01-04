package com.wizzardo.vulkan;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vectors {
    public final static Vector3fc ZERO = new Vector3f(0, 0, 0);
    public final static Vector3fc NAN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    public final static Vector3fc UNIT_X = new Vector3f(1, 0, 0);
    public final static Vector3fc UNIT_Y = new Vector3f(0, 1, 0);
    public final static Vector3fc UNIT_Z = new Vector3f(0, 0, 1);
    public final static Vector3fc UNIT_XYZ = new Vector3f(1, 1, 1);
    public final static Vector3fc POSITIVE_INFINITY = new Vector3f(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    public final static Vector3fc NEGATIVE_INFINITY = new Vector3f(
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);
}
