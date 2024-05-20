package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroySampler;

public class TextureSampler {
    public final long sampler;

    public TextureSampler(VulkanApplication application, long sampler) {
        this.sampler = sampler;
        application.addCleanupTask(this, () -> vkDestroySampler(application.getDevice(), sampler, null));
    }
}
