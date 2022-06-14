package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class UniformBuffers {
    public final List<Long> uniformBuffers;
    public final List<Long> uniformBuffersMemory;
    public final int size;
    public final UniformBufferObject uniformBufferObject = new UniformBufferObject();

    UniformBuffers(List<Long> uniformBuffers, List<Long> uniformBuffersMemory, int size) {
        this.uniformBuffers = uniformBuffers;
        this.uniformBuffersMemory = uniformBuffersMemory;
        this.size = size;
    }

    public static UniformBuffers createUniformBuffers(VkPhysicalDevice physicalDevice, VkDevice device, List<Long> swapChainImages) {
        return createUniformBuffers(physicalDevice, device, swapChainImages.size(), UniformBufferObject.SIZEOF);
    }

    public static UniformBuffers createUniformBuffers(VkPhysicalDevice physicalDevice, VkDevice device, int count, int size) {
        try (MemoryStack stack = stackPush()) {
            List<Long> uniformBuffers = new ArrayList<>(count);
            List<Long> uniformBuffersMemory = new ArrayList<>(count);

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);

            for (int i = 0; i < count; i++) {
                VulkanBuffers.createBuffer(
                        physicalDevice,
                        device,
                        size,
                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer,
                        pBufferMemory);

                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
            return new UniformBuffers(uniformBuffers, uniformBuffersMemory, size);
        }
    }

    public void cleanup(VkDevice device) {
        uniformBuffers.forEach(ubo -> vkDestroyBuffer(device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(device, uboMemory, null));
    }
}
