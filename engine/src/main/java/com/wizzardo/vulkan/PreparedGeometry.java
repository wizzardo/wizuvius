package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;

import org.lwjgl.vulkan.VkDevice;

import java.util.List;

public class PreparedGeometry {
    public final Geometry geometry;
    public UniformBuffers uniformBuffers;
    public long descriptorSetLayout;
    public List<Long> descriptorSets;

    public PreparedGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void cleanup(VkDevice device) {
        try {
            geometry.getMesh().cleanup(device);
            vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
        } finally {
            descriptorSetLayout = 0;
        }
    }

    public void cleanupSwapChainObjects(VkDevice device) {
        try {
            uniformBuffers.cleanup(device);
            geometry.getMaterial().cleanupSwapChainObjects(device);
        } finally {
            uniformBuffers = null;
        }
    }
}
