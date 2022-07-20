package com.wizzardo.vulkan;

import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.List;

public class ModelViewProjectionUniformBuffers {
    public static final int SIZEOF = 3 * 16 * Float.BYTES;
    public final List<UniformBuffer> uniformBuffers;
    public final Matrix4f modelTemp = new Matrix4f();
    //    public final Matrix4f view;
    //    public final Matrix4f proj;

    ModelViewProjectionUniformBuffers(List<UniformBuffer> uniformBuffers) {
        this.uniformBuffers = uniformBuffers;
    }

    public static ModelViewProjectionUniformBuffers create(VkPhysicalDevice physicalDevice, VkDevice device, List<Long> swapChainImages) {
        List<UniformBuffer> list = UniformBuffers.createUniformBuffers(physicalDevice, device, swapChainImages.size(), SIZEOF);
        list.forEach(uniformBuffer -> uniformBuffer.map(device));
        return new ModelViewProjectionUniformBuffers(list);
    }

    public void cleanup(VkDevice device) {
        uniformBuffers.forEach(ubo -> ubo.cleanup(device));
    }
}
