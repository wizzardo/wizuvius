package com.wizzardo.vulkan;

public class DepthStencilStateOptions {

    public boolean depthTestEnable = true;
    public boolean depthWriteEnable = true;
    public boolean depthBoundsTestEnable = false;
    public boolean stencilTestEnable = false;
    public float minDepthBounds = 0.0f;
    public float maxDepthBounds = 1.0f;
}
