package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VulkanQueues {
    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

//            IntBuffer presentSupport = stack.ints(VK_FALSE);
            HashSet<Integer> usedQueues = new HashSet<>();

            for (int i = 0; i < queueFamilies.capacity() && !indices.isComplete(); i++) {
                VkQueueFamilyProperties vkQueueFamilyProperties = queueFamilies.get(i);
                int flags = vkQueueFamilyProperties.queueFlags();
                if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0 && indices.getGraphicsFamily() == null && usedQueues.add(i)) {
                    indices.setGraphicsFamily(i);
                    indices.setGraphicsQueueTimestampValidBits(vkQueueFamilyProperties.timestampValidBits());
                }
                if ((flags & VK_QUEUE_TRANSFER_BIT) != 0 && indices.getTransferFamily() == null && usedQueues.add(i)) {
                    indices.setTransferFamily(i);
                }
                if ((flags & VK_QUEUE_COMPUTE_BIT) != 0 && indices.getComputeFamily() == null && usedQueues.add(i)) {
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

    public static class QueueFamilyProperties {
        public final int index;
        public final int flags;

        public QueueFamilyProperties(int index, int flags) {
            this.index = index;
            this.flags = flags;
        }
    }

    public static List<QueueFamilyProperties> getQueueFamilies(VkPhysicalDevice device) {
        List<QueueFamilyProperties> list = new ArrayList<>();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            for (int i = 0; i < queueFamilies.capacity(); i++) {
                VkQueueFamilyProperties vkQueueFamilyProperties = queueFamilies.get(i);
                int flags = vkQueueFamilyProperties.queueFlags();
                list.add(new QueueFamilyProperties(i, flags));
            }
        }
        return list;
    }

    public static VkQueue createQueue(VkDevice device, int queueFamilyIndex) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, queueFamilyIndex, 0, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }
}
