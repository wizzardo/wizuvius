package com.wizzardo.vulkan;

import static com.wizzardo.vulkan.SwapChainTools.querySwapChainSupport;
import static org.lwjgl.system.MemoryStack.stackPush;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_FORMAT_FEATURE_TRANSFER_DST_BIT;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

public class VulkanDevices {
    static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, QueueFamilyIndices indices) {
        try (MemoryStack stack = stackPush()) {
            int[] uniqueQueueFamilies = indices.unique();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(Utils.asPointerBuffer(VulkanApplication.DEVICE_EXTENSIONS));

            if (VulkanApplication.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(Utils.asPointerBuffer(VulkanApplication.VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            return new VkDevice(pDevice.get(0), physicalDevice, createInfo);
        }
    }

    static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);
                if (isDeviceSuitable(device, surface)) {
                    return device;
                }
            }

            throw new RuntimeException("Failed to find a suitable GPU");
        }
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(device, surface);

        boolean extensionsSupported = checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;

        if (extensionsSupported) {
            try (MemoryStack stack = stackPush()) {
                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack, surface);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
    }

    public static VkPhysicalDeviceFeatures getDeviceFeatures(MemoryStack stack, VkPhysicalDevice device) {
        VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(device, supportedFeatures);
        return supportedFeatures;
    }

    public static VkFormatProperties getDeviceFormatProperties(MemoryStack stack, VkPhysicalDevice device, int format) {
        VkFormatProperties formatProperties = VkFormatProperties.malloc(stack);
        vkGetPhysicalDeviceFormatProperties(device, format, formatProperties);
        return formatProperties;
    }

    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties(MemoryStack stack, VkPhysicalDevice device) {
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(device, properties);
        return properties;
    }

    public static boolean isFormatSupported(MemoryStack stack, VkPhysicalDevice device, int format) {
        VkFormatProperties properties = getDeviceFormatProperties(stack, device, format);
        return (properties.optimalTilingFeatures() & VK_FORMAT_FEATURE_TRANSFER_DST_BIT) != 0 && (properties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) != 0;
    }

    private static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);
            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(VulkanApplication.DEVICE_EXTENSIONS);
        }
    }
}
