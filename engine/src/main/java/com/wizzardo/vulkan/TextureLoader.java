package com.wizzardo.vulkan;

import static org.lwjgl.stb.STBImage.STBI_rgb_alpha;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import com.wizzardo.tools.image.ImageTools;
import com.wizzardo.tools.misc.Stopwatch;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.misc.RuntimeTools;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.ktx.ktxTexture;
import org.lwjgl.util.ktx.ktxTexture1;
import org.lwjgl.util.ktx.ktxTexture2;
import org.lwjgl.vulkan.*;
import org.lwjgl.util.ktx.KTX;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class TextureLoader {

    protected static AtomicLong memoryUsed = new AtomicLong(0);

    public static void main(String[] args) {
        Unchecked.run(() -> {
            byte[] bytes = convertImageToKtx2(new File("sample/src/main/resources/textures/viking_room.png"), "/Users/wizzardo/Downloads/ktx/unpack/toktx");
        });
    }

    public static byte[] convertImageToKtx2(File image, String toKtxExecutable) throws IOException, ExecutionException, InterruptedException {
        RuntimeTools.ExecResult result = RuntimeTools.executeToStdout(new String[]{
                toKtxExecutable,
                "--encode",
                "uastc",
                "--t2",
                "--uastc_quality",
                "4",
                "--zcmp",
                "3",
                "-",
                image.getAbsolutePath(),
        }, 30, null);
        if (result.error.length > 0) {
            String error = new String(result.error, StandardCharsets.UTF_8);
            if (error.contains("It has an ICC profile.")) {
                BufferedImage img = ImageTools.read(image);

                File tempFile;
                if (image.getName().toLowerCase().endsWith(".png")) {
                    tempFile = File.createTempFile("img", ".png");
                    ImageTools.savePNG(img, tempFile);
                } else {
                    tempFile = File.createTempFile("img", ".jpg");
                    ImageTools.saveJPG(img, tempFile, 90);
                }
                try {
                    result = RuntimeTools.executeToStdout(new String[]{
                            toKtxExecutable,
                            "--encode",
                            "uastc",
                            "--t2",
                            "--uastc_quality",
                            "4",
                            "--zcmp",
                            "3",
                            "-",
                            tempFile.getAbsolutePath(),
                    }, 180, null);
                } finally {
                    tempFile.delete();
                }
            }
        }

        return result.result;
    }

    public static long getMemoryUsed() {
        return memoryUsed.get();
    }

    public static TextureImage createTextureImage(VulkanApplication application, Supplier<ByteBuffer> dataLoader) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            Stopwatch stopwatch = new Stopwatch("stbi_load_from_memory");
            ByteBuffer pixels = stbi_load_from_memory(dataLoader.get(), pWidth, pHeight, pChannels, STBI_rgb_alpha);
            System.out.println(stopwatch);
            if (pixels == null) {
                throw new RuntimeException("Failed to load texture image");
            }

            int width = pWidth.get(0);
            int height = pHeight.get(0);
            long imageSize = (long) width * height * STBI_rgb_alpha;

            int mipLevels = (int) Math.floor(log2(Math.max(width, height))) + 1;

            try {
//                return createTextureImage(physicalDevice, device, graphicsQueue, commandPool, pixels, width, height, imageSize, VK_FORMAT_R8G8B8A8_SRGB, mipLevels);
                return createTextureImage(application, pixels, width, height, imageSize, VK_FORMAT_R8G8B8A8_UNORM, mipLevels);
            } finally {
                stbi_image_free(pixels);
            }
        }
    }

    public static TextureImage createTextureImage(
            VulkanApplication application,
            ByteBuffer pixels,
            int width,
            int height,
            long imageSize,
            int format,
            int mipLevels
    ) {
        VkDevice device = application.getDevice();
        VkPhysicalDevice physicalDevice = application.getPhysicalDevice();
        VkQueue queue = application.getGraphicsQueue();
        long commandPool = application.getCommandPool();
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingBufferMemory);


            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, pStagingBufferMemory.get(0), 0, imageSize, 0, data);
            memcpy(data.getByteBuffer(0, (int) imageSize), pixels, imageSize);
            vkUnmapMemory(device, pStagingBufferMemory.get(0));


            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            VulkanImages.createImage(physicalDevice, device, width, height,
                    mipLevels,
                    format,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pTextureImageMemory);

            long textureImage = pTextureImage.get(0);
            long textureImageMemory = pTextureImageMemory.get(0);

            VulkanImages.transitionImageLayout(
                    device,
                    queue,
                    commandPool,
                    textureImage,
                    format,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    mipLevels
            );

            copyBufferToImage(device, queue, commandPool, pStagingBuffer.get(0), textureImage, width, height);

            // Transitioned to VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL while generating mipmaps
            if (mipLevels > 1) {
                int mipsSize = generateMipmaps(physicalDevice, device, queue, commandPool, textureImage, format, width, height, mipLevels);
                imageSize += mipsSize;
            } else {
                VulkanImages.transitionImageLayout(
                        device,
                        queue,
                        commandPool,
                        textureImage,
                        format,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                        mipLevels
                );
            }

            vkDestroyBuffer(device, pStagingBuffer.get(0), null);
            vkFreeMemory(device, pStagingBufferMemory.get(0), null);

            long textureImageView = VulkanImages.createImageView(device, textureImage, format, VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);

            memoryUsed.addAndGet(imageSize);
            return new TextureImage(application, mipLevels, textureImage, textureImageMemory, textureImageView, imageSize);
        }
    }

    public static TextureImage createTextureImage(
            VulkanApplication application,
            int width,
            int height,
            int format,
            int mipLevels
    ) {
        VkDevice device = application.getDevice();
        VkPhysicalDevice physicalDevice = application.getPhysicalDevice();
        VkQueue queue = application.getGraphicsQueue();
        long commandPool = application.getCommandPool();
        try (MemoryStack stack = stackPush()) {
            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            VulkanImages.ImageInfo imageInfo = VulkanImages.createImage(physicalDevice, device, width, height,
                    mipLevels,
                    format,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pTextureImageMemory);
            long memorySize = imageInfo.sizeBytes;

            long textureImage = pTextureImage.get(0);
            long textureImageMemory = pTextureImageMemory.get(0);

            VulkanImages.transitionImageLayout(
                    device,
                    queue,
                    commandPool,
                    textureImage,
                    format,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    mipLevels
            );

            // Transitioned to VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL while generating mipmaps
            int mipsMemorySize = generateMipmaps(physicalDevice, device, queue, commandPool, textureImage, format, width, height, mipLevels);

            long textureImageView = VulkanImages.createImageView(device, textureImage, format, VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);

            memorySize += mipsMemorySize;
            memoryUsed.addAndGet(memorySize);
            return new TextureImage(application, mipLevels, textureImage, textureImageMemory, textureImageView, memorySize);
        }
    }

    static int generateMipmaps(VkPhysicalDevice physicalDevice, VkDevice device, VkQueue graphicsQueue, long commandPool, long image, int imageFormat, int width, int height, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            // Check if image format supports linear blitting
            VkFormatProperties formatProperties = VulkanDevices.getDeviceFormatProperties(stack, physicalDevice, imageFormat);

            if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
                throw new RuntimeException("Texture image format does not support linear blitting");
            }

            int totalSize = 0;
            VkCommandBuffer commandBuffer = VulkanCommands.beginSingleTimeCommands(device, commandPool);

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.image(image);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstAccessMask(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);
            barrier.subresourceRange().levelCount(1);

            int mipWidth = width;
            int mipHeight = height;

            for (int i = 1; i < mipLevels; i++) {
                barrier.subresourceRange().baseMipLevel(i - 1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                totalSize += mipWidth * mipHeight * 4 / 4;

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                        null,
                        null,
                        barrier);

                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0).set(0, 0, 0);
                blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
                blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.srcSubresource().mipLevel(i - 1);
                blit.srcSubresource().baseArrayLayer(0);
                blit.srcSubresource().layerCount(1);
                blit.dstOffsets(0).set(0, 0, 0);
                blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
                blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.dstSubresource().mipLevel(i);
                blit.dstSubresource().baseArrayLayer(0);
                blit.dstSubresource().layerCount(1);

                vkCmdBlitImage(commandBuffer,
                        image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit,
                        VK_FILTER_LINEAR);

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                        null,
                        null,
                        barrier);

                if (mipWidth > 1) {
                    mipWidth /= 2;
                }

                if (mipHeight > 1) {
                    mipHeight /= 2;
                }
            }

            barrier.subresourceRange().baseMipLevel(mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                    null,
                    null,
                    barrier);

            VulkanCommands.endSingleTimeCommands(device, graphicsQueue, commandPool, commandBuffer);
            return totalSize;
        }
    }

    static void memcpy(ByteBuffer dst, ByteBuffer src, long size) {
        src.limit((int) size);
        dst.put(src);
        src.limit(src.capacity()).rewind();
    }

    public static void copyBufferToImage(VkDevice device, VkQueue queue, long commandPool, long buffer, long image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = VulkanCommands.beginSingleTimeCommands(device, commandPool);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            VulkanCommands.endSingleTimeCommands(device, queue, commandPool, commandBuffer);
        }
    }

    public static void copyBufferToImage(MemoryStack stack, VkCommandBuffer commandBuffer, long buffer, long image, ktxTexture texture) {
        PointerBuffer pointerBuffer = stack.callocPointer(1);
        int width = texture.baseWidth();
        int height = texture.baseHeight();

        VkBufferImageCopy.Buffer regions = VkBufferImageCopy.calloc(texture.numLevels(), stack);
        for (int i = 0; i < regions.capacity(); i++) {
            VkBufferImageCopy region = regions.get(i);
            int result = KTX.ktxTexture_GetImageOffset(texture, i, 0, 0, pointerBuffer);
            if (result != KTX.KTX_SUCCESS) {
                throw new RuntimeException("ktxTexture_GetImageOffset failed: " + regions);
            }
//            region.bufferOffset(texture.getImageOffset(i, 0, 0));
            region.bufferOffset(pointerBuffer.get(0));
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(i);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width >> i, height >> i, 1));
        }

        vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
    }

    public static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    static long createTextureSampler(VkDevice device, float mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK_FILTER_LINEAR);
            samplerInfo.minFilter(VK_FILTER_LINEAR);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.anisotropyEnable(true);
            samplerInfo.maxAnisotropy(16.0f);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            samplerInfo.minLod(0); // Optional
            samplerInfo.maxLod((float) mipLevels);
            samplerInfo.mipLodBias(0); // Optional

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if (vkCreateSampler(device, samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            return pTextureSampler.get(0);
        }
    }

    public static TextureImage createTextureImageKtx(VulkanApplication application, File assetFile) throws IOException {
        String fileCanonicalPath = assetFile.getCanonicalPath();
        try (MemoryStack stack = stackPush()) {
            ktxTexture1 texture;
//            texture = KtxTexture1.createFromNamedFile(fileCanonicalPath, KtxTextureCreateFlagBits.LOAD_IMAGE_DATA_BIT);
            PointerBuffer pointerBuffer = stack.callocPointer(1);
            int result = KTX.ktxTexture1_CreateFromNamedFile(fileCanonicalPath, KTX.KTX_TEXTURE_CREATE_LOAD_IMAGE_DATA_BIT, pointerBuffer);
            if (result != KTX.KTX_SUCCESS) {
                throw new RuntimeException("ktxTexture_CreateFromNamedFile failed: " + result);
            }
            texture = ktxTexture1.create(pointerBuffer.get(0));
            ktxTexture t = ktxTexture.create(texture.address());
            try {
                return createTextureImage(application, t, VK10.VK_FORMAT_R8G8B8A8_UNORM);
            } finally {
                cleanupAfterKtxRead(assetFile, fileCanonicalPath, t);
            }
        }
    }

    public static TextureImage createTextureImageKtx2(VulkanApplication application, File assetFile) throws IOException {
        VkPhysicalDevice physicalDevice = application.getPhysicalDevice();

        String fileCanonicalPath = assetFile.getCanonicalPath();
        try (MemoryStack stack = stackPush()) {
            Stopwatch stopwatch = new Stopwatch("KtxTexture2.createFromNamedFile");
            PointerBuffer pointerBuffer = stack.callocPointer(1);
            {
                int result = KTX.ktxTexture2_CreateFromNamedFile(fileCanonicalPath, KTX.KTX_TEXTURE_CREATE_LOAD_IMAGE_DATA_BIT, pointerBuffer);
                if (result != KTX.KTX_SUCCESS) {
                    throw new RuntimeException("ktxTexture2_CreateFromNamedFile failed: " + result);
                }
            }

            ktxTexture2 texture = ktxTexture2.create(pointerBuffer.get(0));
            if (texture.vkFormat() == VK10.VK_FORMAT_UNDEFINED) {
                int transcodeFormat = KTX.KTX_TTF_NOSELECTION;
                VkPhysicalDeviceFeatures deviceFeatures = VulkanDevices.getDeviceFeatures(stack, physicalDevice);
//                    System.out.println("textureCompressionBC: " + deviceFeatures.textureCompressionBC());
//                    System.out.println("textureCompressionASTC_LDR: " + deviceFeatures.textureCompressionASTC_LDR());
//                    System.out.println("textureCompressionETC2: " + deviceFeatures.textureCompressionETC2());
                if (deviceFeatures.textureCompressionBC()) {
                    if (VulkanDevices.isFormatSupported(stack, physicalDevice, VK10.VK_FORMAT_BC7_SRGB_BLOCK)) {
                        transcodeFormat = KTX.KTX_TTF_BC7_RGBA;
                    } else if (VulkanDevices.isFormatSupported(stack, physicalDevice, VK10.VK_FORMAT_BC3_SRGB_BLOCK)) {
                        transcodeFormat = KTX.KTX_TTF_BC3_RGBA;
                    }
                } else if (deviceFeatures.textureCompressionASTC_LDR()) {
                    if (VulkanDevices.isFormatSupported(stack, physicalDevice, VK10.VK_FORMAT_ASTC_4x4_SRGB_BLOCK)) {
                        transcodeFormat = KTX.KTX_TTF_ASTC_4x4_RGBA;
                    }
                } else if (deviceFeatures.textureCompressionETC2()) {
                    if (VulkanDevices.isFormatSupported(stack, physicalDevice, VK10.VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK)) {
                        transcodeFormat = KTX.KTX_TTF_ETC2_RGBA;
                    }
                } else {
                    transcodeFormat = KTX.KTX_TTF_RGBA32;
                }
                if (transcodeFormat != KTX.KTX_TTF_NOSELECTION) {
                    int result = KTX.ktxTexture2_TranscodeBasis(texture, transcodeFormat, 0);
                    if (result != KTX.KTX_SUCCESS) {
                        throw new IllegalStateException("Couldn't transcode texture to format " + transcodeFormat + ": " + result);
                    }
                } else
                    throw new IllegalStateException("Couldn't find transcode format");
            }

            ktxTexture t = ktxTexture.create(texture.address());
            try {
                return createTextureImage(application, t, texture.vkFormat());
            } finally {
                cleanupAfterKtxRead(assetFile, fileCanonicalPath, t);
            }
        }
    }

    private static TextureImage createTextureImage(
            VulkanApplication application,
            ktxTexture texture,
            int format
    ) {
        VkDevice device = application.getDevice();
        VkPhysicalDevice physicalDevice = application.getPhysicalDevice();
        VkQueue queue = application.getGraphicsQueue();
        long commandPool = application.getCommandPool();
        ByteBuffer buffer = texture.pData();

        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            int imageSize = buffer.capacity();
            int width = texture.baseWidth();
            int height = texture.baseHeight();
            int mipLevels = texture.numLevels();

            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingBufferMemory);


            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, pStagingBufferMemory.get(0), 0, imageSize, 0, data);
            memcpy(data.getByteBuffer(0, (int) imageSize), buffer, imageSize);
            vkUnmapMemory(device, pStagingBufferMemory.get(0));


            LongBuffer pTextureImage = stack.mallocLong(1);
            LongBuffer pTextureImageMemory = stack.mallocLong(1);
            VulkanImages.createImage(physicalDevice, device, width, height,
                    mipLevels,
                    format,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pTextureImage,
                    pTextureImageMemory);

            long textureImage = pTextureImage.get(0);
            long textureImageMemory = pTextureImageMemory.get(0);

            VkCommandBuffer commandBuffer = VulkanCommands.beginSingleTimeCommands(device, commandPool);

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.image(textureImage);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().layerCount(1);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.srcAccessMask(0);
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            vkCmdPipelineBarrier(
                    commandBuffer,
                    VK_PIPELINE_STAGE_HOST_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    null,
                    null,
                    barrier
            );

            copyBufferToImage(stack, commandBuffer, pStagingBuffer.get(0), textureImage, texture);

            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            vkCmdPipelineBarrier(
                    commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0,
                    null,
                    null,
                    barrier
            );


            VulkanCommands.endSingleTimeCommands(device, queue, commandPool, commandBuffer);

            vkDestroyBuffer(device, pStagingBuffer.get(0), null);
            vkFreeMemory(device, pStagingBufferMemory.get(0), null);

            long textureImageView = VulkanImages.createImageView(device, textureImage, format, VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);

            memoryUsed.addAndGet(imageSize);
            return new TextureImage(application, mipLevels, textureImage, textureImageMemory, textureImageView, imageSize);
        }
    }

    private static void cleanupAfterKtxRead(File assetFile, String fileCanonicalPath, ktxTexture texture) {
        KTX.ktxTexture_Destroy(texture);

        if (fileCanonicalPath.startsWith(System.getProperty("java.io.tmpdir", "---------")))
            assetFile.delete();
    }
}
