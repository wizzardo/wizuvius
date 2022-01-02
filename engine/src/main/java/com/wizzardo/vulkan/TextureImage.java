package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import org.lwjgl.vulkan.VkDevice;

public class TextureImage {
    public final int mipLevels;
    public final long textureImage;
    public final long textureImageMemory;
    public final long textureImageView;

    TextureImage(int mipLevels, long textureImage, long textureImageMemory, long textureImageView) {
        this.mipLevels = mipLevels;
        this.textureImage = textureImage;
        this.textureImageMemory = textureImageMemory;
        this.textureImageView = textureImageView;
    }

    public void cleanup(VkDevice device) {
        vkDestroyImageView(device, textureImageView, null);
        vkDestroyImage(device, textureImage, null);
        vkFreeMemory(device, textureImageMemory, null);
    }
}
