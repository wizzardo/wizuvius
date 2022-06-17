package com.wizzardo.vulkan;

import org.joml.Matrix4f;

public class UniformBufferObject {

    public static final int SIZEOF = 3 * 16 * Float.BYTES;

    public final Matrix4f model = new Matrix4f();
//    public final Matrix4f view;
//    public final Matrix4f proj;
}
