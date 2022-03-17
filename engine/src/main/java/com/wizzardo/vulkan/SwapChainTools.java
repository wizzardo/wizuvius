package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.nvkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class SwapChainTools {

    private static final int UINT32_MAX = 0xFFFFFFFF;

    static List<Long> createImageViews(VkDevice device, List<Long> swapChainImages, int swapChainImageFormat) {
        List<Long> swapChainImageViews = new ArrayList<>(swapChainImages.size());
        for (long swapChainImage : swapChainImages) {
            swapChainImageViews.add(VulkanImages.createImageView(device, swapChainImage, swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1));
        }
        return swapChainImageViews;
    }

    protected static List<Long> createFramebuffers(
            VkDevice device,
            List<Long> swapChainImageViews,
            long depthImageView,
            long renderPass,
            VkExtent2D swapChainExtent
    ) {
        List<Long> swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());
        try (MemoryStack stack = stackPush()) {
            LongBuffer attachments = stack.longs(VK_NULL_HANDLE, depthImageView);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            for (long imageView : swapChainImageViews) {
                attachments.put(0, imageView);
                framebufferInfo.pAttachments(attachments);

                if (vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
        return swapChainFramebuffers;
    }

    protected static List<Long> createFramebuffers(
            VkDevice device,
            List<Long> swapChainImageViews,
            long renderPass,
            VkExtent2D swapChainExtent
    ) {
        List<Long> swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());
        try (MemoryStack stack = stackPush()) {
            LongBuffer attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapChainExtent.width());
            framebufferInfo.height(swapChainExtent.height());
            framebufferInfo.layers(1);

            for (long imageView : swapChainImageViews) {
                attachments.put(0, imageView);
                framebufferInfo.pAttachments(attachments);

                if (vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
        return swapChainFramebuffers;
    }

    public static class CreateSwapChainResult {
        public final long swapChain;
        public final List<Long> swapChainImages;
        public final int swapChainImageFormat;
        public final VkExtent2D swapChainExtent;

        public CreateSwapChainResult(long swapChain, List<Long> swapChainImages, int swapChainImageFormat, VkExtent2D swapChainExtent) {
            this.swapChain = swapChain;
            this.swapChainImages = swapChainImages;
            this.swapChainImageFormat = swapChainImageFormat;
            this.swapChainExtent = swapChainExtent;
        }
    }

    public static CreateSwapChainResult createSwapChain(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            long surface,
            int width,
            int height
    ) {
        try (MemoryStack stack = stackPush()) {
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice, stack, surface);
            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);

            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities, width, height);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount());

            if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

//          todo: do something with alignments on x32
//            VkSwapchainCreateInfoKHRAndroid createInfo = VkSwapchainCreateInfoKHRAndroid.calloc(stack);
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

//            Log.v(TAG, "createInfo.ALIGNOF: " + VkSwapchainCreateInfoKHRAndroid.ALIGNOF);
//            Log.v(TAG, "createInfo.stype: " + ((int) (createInfo.sType())) + " " + VkSwapchainCreateInfoKHRAndroid.STYPE);
//            Log.v(TAG, "createInfo.pNext: " + ((int) (createInfo.pNext())) + " " + VkSwapchainCreateInfoKHRAndroid.PNEXT);
//            Log.v(TAG, "createInfo.flags: " + ((int) (createInfo.flags())) + " " + VkSwapchainCreateInfoKHRAndroid.FLAGS);
//            Log.v(TAG, "createInfo.surface: " + ((int) (createInfo.surface())) + " " + VkSwapchainCreateInfoKHRAndroid.SURFACE);
//            Log.v(TAG, "createInfo.minImageCount: " + createInfo.minImageCount() + " " + VkSwapchainCreateInfoKHRAndroid.MINIMAGECOUNT);
//            Log.v(TAG, "createInfo.format: " + createInfo.imageFormat() + " " + VkSwapchainCreateInfoKHRAndroid.IMAGEFORMAT);
//            Log.v(TAG, "createInfo.colorSpace: " + createInfo.imageColorSpace() + " " + VkSwapchainCreateInfoKHRAndroid.IMAGECOLORSPACE);
//            Log.v(TAG, "createInfo.extent.width: " + extent.width());
//            Log.v(TAG, "createInfo.extent.height: " + extent.height());

//            QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

//            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
//                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
//                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
//                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily));
//            } else {
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
//            }

//          do not set preTransform for android, so device will properly rotate the surface, but it's not optimal =(
//          todo: https://arm-software.github.io/vulkan_best_practice_for_mobile_developers/samples/performance/surface_rotation/surface_rotation_tutorial.html
//          todo: https://developer.android.com/games/optimize/vulkan-prerotation
//            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            PointerBuffer pSwapChain = stack.pointers(VK_NULL_HANDLE);

//            if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
            if (nvkCreateSwapchainKHR(device, createInfo.address(), 0L, memAddress(pSwapChain)) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            long swapChain = pSwapChain.get(0);
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);
            List<Long> swapChainImages = new ArrayList<>(imageCount.get(0));
            for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            int swapChainImageFormat = surfaceFormat.format();
            VkExtent2D swapChainExtent = VkExtent2D.create().set(extent);

            return new CreateSwapChainResult(swapChain, swapChainImages, swapChainImageFormat, swapChainExtent);
        }
    }

    static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack, long surface) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }

    private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
//        availableFormats.stream()
//                .forEach(availableFormat -> {
//                    Log.v(TAG, "availableFormat: " + availableFormat.format() + " " + availableFormat.colorSpace());
//                });
        List<VkSurfaceFormatKHR> formats = availableFormats.stream()
                .collect(Collectors.toList());
        VkSurfaceFormatKHR formatKHR = formats.stream()
//                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_UNORM)
//                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_R8G8B8A8_SRGB)
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB || availableFormat.format() == VK_FORMAT_R8G8B8A8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findFirst()
                .orElse(availableFormats.get(0));

//        System.out.println("selectedFormat: " + formatKHR.format() + " " + formatKHR.colorSpace());
//        System.out.println("formats: |" + formats.stream().map(it -> it.format() + " " + it.colorSpace()).collect(Collectors.joining(", ")) + "|");
        return formatKHR;
    }

    private static int chooseSwapPresentMode(IntBuffer availablePresentModes) {
//        for (int i = 0; i < availablePresentModes.capacity(); i++) {
//            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
//                return availablePresentModes.get(i);
//            }
//        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, int width, int height) {
//        Log.v(TAG, "chooseSwapExtent.capabilities.currentExtent(): " + capabilities.currentExtent().width() + "x" + capabilities.currentExtent().height());
//        Log.v(TAG, "chooseSwapExtent.capabilities.currentTransform(): " + capabilities.currentTransform());
//        if ((capabilities.currentTransform() & VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR) != 0 ||
//                (capabilities.currentTransform() & VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR) != 0) {
//            // Swap to get identity width and height
//            capabilities.currentExtent().height(width);
//            capabilities.currentExtent().width(height);
//
//            Log.v(TAG, "chooseSwapExtent.capabilities.currentExtent swapped: " + capabilities.currentExtent().width() + "x" + capabilities.currentExtent().height());
//        }

        if (capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

//        IntBuffer width = stackGet().ints(0);
//        IntBuffer height = stackGet().ints(0);
//
//        glfwGetFramebufferSize(window, width, height);
//
//        VkExtent2D actualExtent = VkExtent2D.malloc().set(width.get(0), height.get(0));

//        Log.v(TAG, "chooseSwapExtent " + width + "x" + height);
        VkExtent2D actualExtent = VkExtent2D.malloc().set(width, height);

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static SyncObjects createSyncObjects(VkDevice device, List<Long> swapChainImages, int maxFramesInFlight) {
        Frame[] frames = new Frame[maxFramesInFlight];

        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < maxFramesInFlight; i++) {
                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                frames[i] = new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0));
            }
            return new SyncObjects(frames, swapChainImages.size());
        }
    }
}
