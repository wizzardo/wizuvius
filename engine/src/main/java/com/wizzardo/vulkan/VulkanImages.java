package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanImages {
    static long createImageView(VkDevice device, long image, int format, int aspectFlags, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);

            if (vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }

            return pImageView.get(0);
        }
    }

    static long createImageView(VkDevice device, long image, int format, int aspectFlags, int mipLevels, int layers) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(layers);

            LongBuffer pImageView = stack.mallocLong(1);

            if (vkCreateImageView(device, viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }

            return pImageView.get(0);
        }
    }

    public static class ImageInfo{
       public final long imagePointer;
       public final long memoryPointer;
       public final long sizeBytes;

        public ImageInfo(long imagePointer, long memoryPointer, long sizeBytes) {
            this.imagePointer = imagePointer;
            this.memoryPointer = memoryPointer;
            this.sizeBytes = sizeBytes;
        }
    }

    static ImageInfo createImage(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            int width,
            int height,
            int mipLevels,
            int format,
            int tiling,
            int usage,
            int memProperties,
            LongBuffer pTextureImage,
            LongBuffer pTextureImageMemory
    ) {
        return createImage(physicalDevice, device, width, height, mipLevels, 1, format, tiling, usage, memProperties, pTextureImage, pTextureImageMemory);
    }

    static ImageInfo createImage(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            int width,
            int height,
            int mipLevels,
            int layers,
            int format,
            int tiling,
            int usage,
            int memProperties,
            LongBuffer pTextureImage,
            LongBuffer pTextureImageMemory
    ) {
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(mipLevels);
            imageInfo.arrayLayers(layers);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if (vkCreateImage(device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, pTextureImage.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            long memorySize = memRequirements.size();
            allocInfo.allocationSize(memorySize);
            allocInfo.memoryTypeIndex(VulkanBuffers.findMemoryTypeIndex(physicalDevice, memRequirements.memoryTypeBits(), memProperties));

            if (vkAllocateMemory(device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate image memory");
            }

            vkBindImageMemory(device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
            return new ImageInfo(pTextureImage.get(0), pTextureImageMemory.get(0), memorySize);
        }
    }

    static void transitionImageLayout(VkDevice device, VkQueue graphicsQueue, long commandPool, long image, int format, int oldLayout, int newLayout, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                if (hasStencilComponent(format)) {
                    barrier.subresourceRange().aspectMask(barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
                }
            } else {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            }

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            } else {
                throw new IllegalArgumentException("Unsupported layout transition");
            }

            VkCommandBuffer commandBuffer = VulkanCommands.beginSingleTimeCommands(device, commandPool);
            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

            VulkanCommands.endSingleTimeCommands(device, graphicsQueue, commandPool, commandBuffer);
        }
    }

    static boolean hasStencilComponent(int format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    static DepthResources createDepthResources(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            VkExtent2D swapChainExtent,
            VkQueue graphicsQueue,
            long commandPool
    ) {
        try (MemoryStack stack = stackPush()) {
            int depthFormat = findDepthFormat(physicalDevice);
            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);

            createImage(
                    physicalDevice,
                    device,
                    swapChainExtent.width(), swapChainExtent.height(),
                    1,
                    depthFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pDepthImage,
                    pDepthImageMemory);

            long depthImage = pDepthImage.get(0);
            long depthImageMemory = pDepthImageMemory.get(0);
            long depthImageView = createImageView(device, depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1);

            // Explicitly transitioning the depth image
            transitionImageLayout(device, graphicsQueue, commandPool, depthImage, depthFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, 1);

            return new DepthResources(depthImage, depthImageMemory, depthImageView);
        }
    }

    public static int findDepthFormat(VkPhysicalDevice physicalDevice) {
        return findSupportedFormat(physicalDevice,
                stackGet().ints(
                        VK_FORMAT_D32_SFLOAT,
                        VK_FORMAT_D32_SFLOAT_S8_UINT,
                        VK_FORMAT_D24_UNORM_S8_UINT,
                        VK_FORMAT_D16_UNORM_S8_UINT,
                        VK_FORMAT_D16_UNORM
                ),
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    public static int findSupportedFormat(VkPhysicalDevice physicalDevice, IntBuffer formatCandidates, int tiling, int features) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties props = VkFormatProperties.calloc(stack);
            for (int i = 0; i < formatCandidates.capacity(); ++i) {
                int format = formatCandidates.get(i);
                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }
            }
        }
        throw new RuntimeException("Failed to find supported format");
    }

    public static void insertImageMemoryBarrier(
            MemoryStack stack,
            VkCommandBuffer cmdbuffer,
            LongBuffer image,
            int srcAccessMask,
            int dstAccessMask,
            int oldImageLayout,
            int newImageLayout,
            int srcStageMask,
            int dstStageMask,
            VkImageSubresourceRange subresourceRange
    ) {
        VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.calloc(1, stack);
        imageMemoryBarrier.sType$Default();
        imageMemoryBarrier.srcAccessMask(srcAccessMask);
        imageMemoryBarrier.dstAccessMask(dstAccessMask);
        imageMemoryBarrier.oldLayout(oldImageLayout);
        imageMemoryBarrier.newLayout(newImageLayout);
        imageMemoryBarrier.image(image.get(0));
        imageMemoryBarrier.subresourceRange(subresourceRange);

        vkCmdPipelineBarrier(
                cmdbuffer,
                srcStageMask,
                dstStageMask,
                0,
                null, null,
                imageMemoryBarrier);
    }
}
