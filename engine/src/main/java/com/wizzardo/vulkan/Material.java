package com.wizzardo.vulkan;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Material {
    String vertexShader;
    String fragmentShader;
    TextureImage textureImage;
    long textureSampler;

    public List<VulkanDescriptorSets.DescriptorSetLayoutBinding> bindings;
    public long descriptorSetLayout;
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
        try {
            vkDestroySampler(device, textureSampler, null);
            vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
            textureImage.cleanup(device);
        } finally {
            descriptorSetLayout = 0;
            textureSampler = 0;
        }
    }

    protected void prepare(VulkanApplication application, Viewport viewport) {
        if (descriptorSetLayout == 0L) {
            VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = new VulkanDescriptorSets.DescriptorSetLayoutBuilder();
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingUBO(0, VK_SHADER_STAGE_VERTEX_BIT));
            if (textureImage != null)
                layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingImageWithSampler(1, VK_SHADER_STAGE_FRAGMENT_BIT, textureImage.textureImageView, textureSampler));

            bindings = layoutBuilder.bindings;
            descriptorSetLayout = layoutBuilder.build(application.getDevice());
        }

        if (pipelineLayout == 0L) {
            createPipeline(application, viewport);
        }

    }

    private void createPipeline(VulkanApplication application, Viewport viewport) {
        ByteBuffer vertShaderSPIRV = Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(application.loadAsset(vertexShader));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });
        ByteBuffer fragShaderSPIRV = Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(application.loadAsset(fragmentShader));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });
        CreateGraphicsPipelineResult pipeline = VulkanApplication.createGraphicsPipeline(
                application.getDevice(),
                vertShaderSPIRV,
                fragShaderSPIRV,
                viewport.getExtent(),
                viewport.getRenderPass(),
                descriptorSetLayout
        );
        pipelineLayout = pipeline.pipelineLayout;
        graphicsPipeline = pipeline.graphicsPipeline;
    }
}
