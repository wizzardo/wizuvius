package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;

import org.lwjgl.vulkan.VkDevice;

public class Material {
    String vertexShader;
    String fragmentShader;
    TextureImage textureImage;
    long textureSampler;

    public long graphicsPipeline;
    public long pipelineLayout;

    public String getVertexShader() {
        return vertexShader;
    }

    public void setVertexShader(String vertexShader) {
        this.vertexShader = vertexShader;
    }

    public String getFragmentShader() {
        return fragmentShader;
    }

    public void setFragmentShader(String fragmentShader) {
        this.fragmentShader = fragmentShader;
    }

    public TextureImage getTextureImage() {
        return textureImage;
    }

    public void setTextureImage(TextureImage textureImage) {
        this.textureImage = textureImage;
    }

    public long getTextureSampler() {
        return textureSampler;
    }

    public void setTextureSampler(long textureSampler) {
        this.textureSampler = textureSampler;
    }

    public long getGraphicsPipeline() {
        return graphicsPipeline;
    }

    public void setGraphicsPipeline(long graphicsPipeline) {
        this.graphicsPipeline = graphicsPipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public void setPipelineLayout(long pipelineLayout) {
        this.pipelineLayout = pipelineLayout;
    }

    public void cleanupSwapChainObjects(VkDevice device) {
        try {
            vkDestroyPipeline(device, graphicsPipeline, null);
            vkDestroyPipelineLayout(device, pipelineLayout, null);
        } finally {
            graphicsPipeline = 0;
            pipelineLayout = 0;
        }
    }

    public void cleanup(VkDevice device) {
        vkDestroySampler(device, textureSampler, null);
        textureImage.cleanup(device);
    }
}
