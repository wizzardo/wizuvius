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

class UniformBuffers {
    public final List<Long> uniformBuffers;
    public final List<Long> uniformBuffersMemory;

    UniformBuffers(List<Long> uniformBuffers, List<Long> uniformBuffersMemory) {
        this.uniformBuffers = uniformBuffers;
        this.uniformBuffersMemory = uniformBuffersMemory;
    }

    static UniformBuffers createUniformBuffers(VkPhysicalDevice physicalDevice, VkDevice device, List<Long> swapChainImages) {
        try (MemoryStack stack = stackPush()) {
            List<Long> uniformBuffers = new ArrayList<>(swapChainImages.size());
            List<Long> uniformBuffersMemory = new ArrayList<>(swapChainImages.size());

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);

            for (int i = 0; i < swapChainImages.size(); i++) {
                VulkanBuffers.createBuffer(
                        physicalDevice,
                        device,
                        UniformBufferObject.SIZEOF,
                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer,
                        pBufferMemory);

                uniformBuffers.add(pBuffer.get(0));
                uniformBuffersMemory.add(pBufferMemory.get(0));
            }
            return new UniformBuffers(uniformBuffers, uniformBuffersMemory);
        }
    }

    public void cleanup(VkDevice device) {
        uniformBuffers.forEach(ubo -> vkDestroyBuffer(device, ubo, null));
        uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(device, uboMemory, null));
    }
}
