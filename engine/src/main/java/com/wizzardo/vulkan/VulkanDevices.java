package com.wizzardo.vulkan;

import static com.wizzardo.vulkan.SwapChainTools.querySwapChainSupport;
import static org.lwjgl.system.MemoryStack.stackPush;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_FORMAT_FEATURE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.*;

public class VulkanDevices {
    static VkDevice createLogicalDevice(
            VkPhysicalDevice physicalDevice,
            List<VulkanQueues.QueueFamilyProperties> queueFamilyProperties,
            boolean withBindlessTextures,
            Set<DeviceFeature> enabledDeviceFeatures
    ) {
        try (MemoryStack stack = stackPush()) {
            int[] uniqueQueueFamilies = queueFamilyProperties.stream().mapToInt(it -> it.index).toArray();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            enableDeviceFeatures(deviceFeatures, enabledDeviceFeatures);

            Set<String> availableExtensions = getAvailableExtensions(physicalDevice);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);
            Set<String> extensionNames = new HashSet<>(VulkanApplication.DEVICE_EXTENSIONS);
            if (withBindlessTextures)
                extensionNames.add("VK_EXT_descriptor_indexing");
            if (availableExtensions.contains("VK_KHR_portability_subset"))
                extensionNames.add("VK_KHR_portability_subset");

            createInfo.ppEnabledExtensionNames(Utils.asPointerBuffer(extensionNames));


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

    static protected void enableDeviceFeatures(VkPhysicalDeviceFeatures physicalDeviceFeatures, Set<DeviceFeature> toEnable) {
        toEnable.forEach(feature -> feature.enable(physicalDeviceFeatures));
    }

    static class DeviceInfo {
        final VkPhysicalDevice physicalDevice;
        final String name;
        final Type type;

        DeviceInfo(VkPhysicalDevice physicalDevice, String name, Type type) {
            this.physicalDevice = physicalDevice;
            this.name = name;
            this.type = type;
        }

        enum Type {
            DISCRETE_GPU(2),
            INTEGRATED_GPU(1),
            VIRTUAL_GPU(3),
            CPU(4),
            OTHER(0);

            final int vkType;

            Type(int vkType) {
                this.vkType = vkType;
            }

            static Type byVkType(int type) {
                if (type == 0)
                    return OTHER;
                if (type == 1)
                    return INTEGRATED_GPU;
                if (type == 2)
                    return DISCRETE_GPU;
                if (type == 3)
                    return VIRTUAL_GPU;
                if (type == 4)
                    return CPU;
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }

    static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface, boolean withBindlessTextures, boolean headless) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            List<DeviceInfo> devices = new ArrayList<>();

            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);
                VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(device, deviceProperties);

                if (headless || isDeviceSuitable(device, surface, withBindlessTextures)) {
                    devices.add(new DeviceInfo(device, deviceProperties.deviceNameString(), DeviceInfo.Type.byVkType(deviceProperties.deviceType())));
                }
            }

            if (devices.isEmpty())
                throw new RuntimeException("Failed to find a suitable GPU");

            devices.sort(Comparator.comparingInt(it -> it.type.ordinal()));
            return devices.get(0).physicalDevice;
        }
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface, boolean withBindlessTextures) {
        QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(device);

        Set<String> availableExtensions = getAvailableExtensions(device);
//            System.out.println("availableExtensions: ");
//            availableExtensions.stream()
//                    .map(VkExtensionProperties::extensionNameString)
//                    .forEach(ext -> System.out.println("\t" + ext));

        boolean extensionsSupported = availableExtensions.containsAll(VulkanApplication.DEVICE_EXTENSIONS);
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;
        boolean bindlessTexturesFeatureSupported = false;
        boolean bindlessTexturesExtensionSupported = availableExtensions.contains("VK_EXT_descriptor_indexing");

        if (extensionsSupported) {
            try (MemoryStack stack = stackPush()) {
                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack, surface);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();

                if (withBindlessTextures && device.getCapabilities().vkGetPhysicalDeviceProperties2 != 0L) {
                    VkPhysicalDeviceDescriptorIndexingFeatures indexingFeatures = VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack);
                    VkPhysicalDeviceFeatures2 deviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack);
                    deviceFeatures2.pNext(indexingFeatures);
                    vkGetPhysicalDeviceFeatures2(device, deviceFeatures2);
                    bindlessTexturesFeatureSupported = indexingFeatures.descriptorBindingPartiallyBound() && indexingFeatures.runtimeDescriptorArray();
                }
            }
        }
        if (withBindlessTextures && !(bindlessTexturesFeatureSupported || bindlessTexturesExtensionSupported))
            return false;

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

    private static Set<String> getAvailableExtensions(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());
        }
    }
}
