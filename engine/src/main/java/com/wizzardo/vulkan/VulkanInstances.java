package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class VulkanInstances {
    public VkInstance createInstance() {
        if (VulkanApplication.ENABLE_VALIDATION_LAYERS && !DebugTools.checkValidationLayerSupport()) {
            throw new RuntimeException("Validation requested but not supported");
        }

        try (MemoryStack stack = stackPush()) {
            // Use calloc to initialize the structs with 0s. Otherwise, the program can crash due to random values
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(VulkanApplication.applicationName));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe(VulkanApplication.engineName));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
            createInfo.ppEnabledExtensionNames(getRequiredExtensions());

            if (VulkanApplication.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(Utils.asPointerBuffer(VulkanApplication.VALIDATION_LAYERS));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                DebugTools.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            return new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    public PointerBuffer getRequiredExtensions() {
        MemoryStack stack = stackGet();
        PointerBuffer extensions = stack.mallocPointer(2 + (VulkanApplication.ENABLE_VALIDATION_LAYERS ? 1 : 0));
        extensions.put(stack.UTF8("VK_KHR_surface"));
        extensions.put(stack.UTF8("VK_KHR_android_surface"));

        if (VulkanApplication.ENABLE_VALIDATION_LAYERS) {
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
        }

        // Rewind the buffer before returning it to reset its position back to 0
        return extensions.rewind();
    }
}
