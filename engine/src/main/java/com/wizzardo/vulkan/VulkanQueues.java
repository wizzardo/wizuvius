package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

public class VulkanQueues {
    static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

//            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity() && !indices.isComplete(); i++) {
                VkQueueFamilyProperties vkQueueFamilyProperties = queueFamilies.get(i);
                int flags = vkQueueFamilyProperties.queueFlags();
                if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.setGraphicsFamily(i);
                }
                if ((flags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    indices.setTransferFamily(i);
                }
                if ((flags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    indices.setComputeFamily(i);
                }

//                IntBuffer surfaceSupported = stack.ints(0);
//                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, surfaceSupported);
//
////                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
////
//                if(surfaceSupported.get(0) == VK_TRUE) {
//                    System.out.println(i+" surface "+(surfaceSupported.get(0) == VK_TRUE));
//                }
            }

            return indices;
        }
    }

    static VkQueue createQueue(VkDevice device, int queueFamilyIndex) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, queueFamilyIndex, 0, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }
}
