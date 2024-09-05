package com.wizzardo.vulkan;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;

public class Pipeline {
    public final long graphicsPipeline;
    public final long pipelineLayout;

    public Pipeline(long graphicsPipeline, long pipelineLayout) {
        this.graphicsPipeline = graphicsPipeline;
        this.pipelineLayout = pipelineLayout;
    }

    public Runnable createCleanupTask(VkDevice device) {
        long graphicalPipeline = this.graphicsPipeline;
        long pipelineLayout = this.pipelineLayout;
        return () -> {
            ResourceCleaner.printDebugInCleanupTask(Pipeline.class);
            vkDestroyPipeline(device, graphicalPipeline, null);
            vkDestroyPipelineLayout(device, pipelineLayout, null);
        };
    }
}
