package com.wizzardo.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UniformBuffers {

    public static List<UniformBuffer> createUniformBuffers(VkPhysicalDevice physicalDevice, VkDevice device, int count, int size) {
        List<UniformBuffer> uniformBuffers = new ArrayList<>(count);
        try (MemoryStack stack = stackPush()) {
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

                uniformBuffers.add(new UniformBuffer(pBuffer.get(0), pBufferMemory.get(0), size));
            }
            return uniformBuffers;
        }
    }

    public static UniformBuffer createUniformBufferObject(VkPhysicalDevice physicalDevice, VkDevice device, int size) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);

            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    size,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            return new UniformBuffer(pBuffer.get(0), pBufferMemory.get(0), size);
        }
    }
}
