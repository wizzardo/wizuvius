package com.example.screenshot;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.image.ImageTools;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.GlfwKey;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryHelpers;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    Spatial scene;

    @Override
    protected void initApp() {
        int cameraOffset = 5;
        getMainViewport().getCamera().setLocation(new Vector3f(cameraOffset, cameraOffset, cameraOffset));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            scene = loadScene();
            scene.getLocalTransform().setScale(0.1f);
            getMainViewport().getScene().attachChild(scene);
        }

        {
            getInputsManager().addKeyListener((key, pressed, repeat) -> {
                if (key == GlfwKey.GLFW_KEY_SPACE && pressed) {
                    saveScreenshot("screenshot-" + System.currentTimeMillis());
                    return false;
                }
                return true;
            });
        }
    }

    public void saveScreenshot(String name) {
        // based on https://github.com/SaschaWillems/Vulkan/blob/master/examples/screenshot/screenshot.cpp
        try (MemoryStack stack = stackPush()) {
            boolean supportsBlit = true;
            int width = this.extentWidth;
            int height = this.extentHeight;

            // Check blit support for source and destination
            VkFormatProperties formatProps = VkFormatProperties.calloc(stack);
            // Check if the device supports blitting from optimal images (the swapchain images are in optimal format)
            vkGetPhysicalDeviceFormatProperties(physicalDevice, swapChainImageFormat, formatProps);
            if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_BLIT_SRC_BIT) == 0) {
                System.out.println("Device does not support blitting from optimal tiled images, using copy instead of blit!");
                supportsBlit = false;
            }

            // Check if the device supports blitting to linear images
            vkGetPhysicalDeviceFormatProperties(physicalDevice, VK_FORMAT_R8G8B8A8_UNORM, formatProps);
            if ((formatProps.linearTilingFeatures() & VK_FORMAT_FEATURE_BLIT_DST_BIT) == 0) {
                System.out.println("Device does not support blitting to linear tiled images, using copy instead of blit!");
                supportsBlit = false;
            }
            System.out.println("supportsBlit: " + supportsBlit);

            LongBuffer srcImage = stack.longs(swapChainImages.get(currentFrame));

            VkImageCreateInfo imageCreateCI = VkImageCreateInfo.calloc(stack);
            imageCreateCI.imageType(VK_IMAGE_TYPE_2D);
            // Note that vkCmdBlitImage (if supported) will also do format conversions if the swapchain color format would differ
            imageCreateCI.format(VK_FORMAT_R8G8B8A8_SRGB);
            imageCreateCI.extent().width(width);
            imageCreateCI.extent().height(height);
            imageCreateCI.extent().depth(1);
            imageCreateCI.arrayLayers(1);
            imageCreateCI.mipLevels(1);
            imageCreateCI.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageCreateCI.samples(VK_SAMPLE_COUNT_1_BIT);
            imageCreateCI.tiling(VK_IMAGE_TILING_LINEAR);
            imageCreateCI.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT);

            LongBuffer dstImage = stack.mallocLong(1);
            if (vkCreateImage(device, imageCreateCI, null, dstImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            // Create memory to back up the image
            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device, dstImage.get(0), memRequirements);
            VkMemoryAllocateInfo memAllocInfo = VkMemoryAllocateInfo.calloc(stack);

            LongBuffer dstImageMemory = stack.mallocLong(1);
            memAllocInfo.allocationSize(memRequirements.size());
            memAllocInfo.memoryTypeIndex(VulkanBuffers.findMemoryTypeIndex(physicalDevice, memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            if (vkAllocateMemory(device, memAllocInfo, null, dstImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to AllocateMemory");
            }
            if (vkBindImageMemory(device, dstImage.get(0), dstImageMemory.get(0), 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to BindImageMemory");
            }

            VkCommandBuffer copyCmd = VulkanCommands.beginSingleTimeCommands(device, commandPool);

            // Transition destination image to transfer destination layout
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    dstImage,
                    0,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );

            // Transition swapchain image from present to transfer source layout
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    srcImage,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_ACCESS_TRANSFER_READ_BIT,
                    KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );
            if (supportsBlit) {
                // Define the region to blit (we will blit the whole swapchain image)
                VkOffset3D blitSize = VkOffset3D.calloc(stack);
                blitSize.set(width, height, 1);
                VkImageBlit.Buffer imageBlitRegion = VkImageBlit.calloc(1, stack);
                imageBlitRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageBlitRegion.srcSubresource().layerCount(1);
                imageBlitRegion.srcOffsets(1).set(blitSize);
                imageBlitRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageBlitRegion.dstSubresource().layerCount(1);
                imageBlitRegion.dstOffsets(1).set(blitSize);

                // Issue the blit command
                vkCmdBlitImage(
                        copyCmd,
                        srcImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        dstImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageBlitRegion,
                        VK_FILTER_NEAREST);
            } else {
                // Otherwise use image copy (requires us to manually flip components)
                VkImageCopy.Buffer imageCopyRegion = VkImageCopy.calloc(1, stack);
                imageCopyRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopyRegion.srcSubresource().layerCount(1);
                imageCopyRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopyRegion.dstSubresource().layerCount(1);
                imageCopyRegion.extent().width(width);
                imageCopyRegion.extent().height(height);
                imageCopyRegion.extent().depth(1);

                // Issue the copy command
                vkCmdCopyImage(
                        copyCmd,
                        srcImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        dstImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageCopyRegion);
            }

            // Transition destination image to general layout, which is the required layout for mapping the image memory later on
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    dstImage,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_GENERAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );


            // Transition back the swap chain image after the blit is done
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    srcImage,
                    VK_ACCESS_TRANSFER_READ_BIT,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));

            VulkanCommands.endSingleTimeCommands(device, graphicsQueue, commandPool, copyCmd);

//                        // Get layout of the image (including row pitch)
//                        VkImageSubresource subResource = VkImageSubresource.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0);
//
//                        VkSubresourceLayout subResourceLayout = VkSubresourceLayout.calloc(stack);
//                        vkGetImageSubresourceLayout(device, dstImage.get(0), subResource, subResourceLayout);

            ByteBuffer byteBufferPointer = MemoryUtil.memByteBufferSafe(-1, 0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, dstImageMemory.get(0), 0, VK_WHOLE_SIZE, 0, data);
            MemoryHelpers.remapByteBuffer(byteBufferPointer, data.get(0), (int) memRequirements.size());
//                        byteBufferPointer.position((int) subResourceLayout.offset());

            // If source is BGR (destination is always RGB) and we can't use blit (which does automatic conversion), we'll have to manually swizzle color components
            boolean colorSwizzle = false;
            // Check if source is BGR
            // Note: Not complete, only contains most common and basic BGR surface formats for demonstration purposes
            if (!supportsBlit) {
                if (swapChainImageFormat == VK_FORMAT_B8G8R8A8_SRGB)
                    colorSwizzle = true;
            }

            int[] row = new int[width];

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            WritableRaster raster = image.getRaster();
            IntBuffer ints = byteBufferPointer.asIntBuffer();
            int[] pixel = new int[3];
            for (int y = 0; y < height; y++) {
                ints.get(row);
                for (int x = 0; x < width; x++) {
                    int value = row[x];
                    if (colorSwizzle) {
                        pixel[0] = ((value >> 16) & 0xFF);
                        pixel[1] = ((value >> 8) & 0xFF);
                        pixel[2] = (value & 0xFF);
                    } else {
                        pixel[2] = ((value >> 16) & 0xFF);
                        pixel[1] = ((value >> 8) & 0xFF);
                        pixel[0] = (value & 0xFF);
                    }
                    raster.setPixel(x, y, pixel);
                }
            }

            FileTools.bytes(name + ".png", ImageTools.savePNGtoBytes(image));

            vkUnmapMemory(device, dstImageMemory.get(0));
            vkFreeMemory(device, dstImageMemory.get(0), null);
            vkDestroyImage(device, dstImage.get(0), null);

            System.out.println("screenshot saved");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        scene.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
    }

    private Spatial loadScene() {
        Spatial scene = Unchecked.call(() -> ModelLoader.loadModel(
                "gltf/models/well.gltf", aiProcess_JoinIdenticalVertices, this::loadAssetAsByteBuffer
        ));
        return scene;
    }

}
