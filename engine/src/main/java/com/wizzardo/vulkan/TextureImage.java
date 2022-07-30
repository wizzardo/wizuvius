package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import org.lwjgl.vulkan.VkDevice;

import java.io.IOException;

public class TextureImage {
    protected int mipLevels;
    protected long textureImage;
    protected long textureImageMemory;
    protected long textureImageView;
    protected long memoryUsage;
    protected String filename;
    protected Type type = Type.UNKNOWN;
    protected int index = -1;

    public enum Type {
        NONE,
        DIFFUSE,
        SPECULAR,
        AMBIENT,
        EMISSIVE,
        HEIGHT,
        NORMALS,
        SHININESS,
        OPACITY,
        DISPLACEMENT,
        LIGHTMAP,
        REFLECTION,
        BASE_COLOR,
        NORMAL_CAMERA,
        EMISSION_COLOR,
        METALNESS,
        DIFFUSE_ROUGHNESS,
        AMBIENT_OCCLUSION,
        SHEEN,
        CLEARCOAT,
        TRANSMISSION,
        UNKNOWN;
    }

    public TextureImage(int mipLevels, long textureImage, long textureImageMemory, long textureImageView, long memoryUsage) {
        this.mipLevels = mipLevels;
        this.textureImage = textureImage;
        this.textureImageMemory = textureImageMemory;
        this.textureImageView = textureImageView;
        this.memoryUsage = memoryUsage;
    }

    public TextureImage(int mipLevels, long textureImage, long textureImageMemory, long textureImageView) {
        this.mipLevels = mipLevels;
        this.textureImage = textureImage;
        this.textureImageMemory = textureImageMemory;
        this.textureImageView = textureImageView;
    }

    public TextureImage(String filename, Type type) {
        this.filename = filename;
        this.type = type;
    }

    public void load(VulkanApplication application) throws IOException {
        if (textureImage != 0)
            return;

        TextureImage image = application.createTextureImage(filename);
        this.textureImage = image.textureImage;
        this.textureImageMemory = image.textureImageMemory;
        this.textureImageView = image.textureImageView;
    }

    public int getIndex(){
        return index;
    }

    public void cleanup(VkDevice device) {
        if (textureImageView != 0)
            vkDestroyImageView(device, textureImageView, null);
        if (textureImage != 0)
            vkDestroyImage(device, textureImage, null);
        if (textureImageMemory != 0)
            vkFreeMemory(device, textureImageMemory, null);

        TextureLoader.memoryUsed.addAndGet(-memoryUsage);
        memoryUsage = 0;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getMipLevels() {
        return mipLevels;
    }

    public long getTextureImage() {
        return textureImage;
    }

    public long getTextureImageMemory() {
        return textureImageMemory;
    }

    public long getTextureImageView() {
        return textureImageView;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
