package com.wizzardo.vulkan;

class DepthResources {
    public final long depthImage;
    public final long depthImageMemory;
    public final long depthImageView;

    DepthResources(long depthImage, long depthImageMemory, long depthImageView) {
        this.depthImage = depthImage;
        this.depthImageMemory = depthImageMemory;
        this.depthImageView = depthImageView;
    }
}
