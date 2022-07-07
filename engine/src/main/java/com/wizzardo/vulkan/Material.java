package com.wizzardo.vulkan;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import org.lwjgl.vulkan.VkDevice;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class Material {
    public static final VertexLayout DEFAULT_VERTEX_LAYOUT = new VertexLayout(
            VertexLayout.BindingDescription.POSITION,
            VertexLayout.BindingDescription.COLOR,
            VertexLayout.BindingDescription.TEXTURE_COORDINATES
    );

    protected String vertexShader;
    protected String fragmentShader;
    protected List<TextureImage> textures = Collections.emptyList();
    protected long textureSampler;
    protected VertexLayout vertexLayout = DEFAULT_VERTEX_LAYOUT;
    protected boolean withUBO = true;

    public List<VulkanDescriptorSets.DescriptorSetLayoutBinding> bindings;
    public long descriptorSetLayout;
    public long graphicsPipeline;
    public long pipelineLayout;

    public VertexLayout getVertexLayout() {
        return vertexLayout;
    }

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

    public List<TextureImage> getTextures() {
        return textures;
    }

    public void addTextureImage(TextureImage textureImage) {
        if (textures.isEmpty())
            textures = new ArrayList<>();
        textures.add(textureImage);
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
            if (graphicsPipeline != 0)
                vkDestroyPipeline(device, graphicsPipeline, null);
            if (pipelineLayout != 0)
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
            for (int i = 0; i < textures.size(); i++) {
                TextureImage texture = textures.get(i);
                texture.cleanup(device);
            }
        } finally {
            descriptorSetLayout = 0;
            textureSampler = 0;
        }
    }

    public void prepare(VulkanApplication application, Viewport viewport) {
        if (descriptorSetLayout == 0L) {
            VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = new VulkanDescriptorSets.DescriptorSetLayoutBuilder();
            if (withUBO)
                layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingUBO(0, VK_SHADER_STAGE_VERTEX_BIT));

            for (int i = 0; i < textures.size(); i++) {
                TextureImage texture = textures.get(i);
                layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingImageWithSampler(layoutBuilder.bindings.size(), VK_SHADER_STAGE_FRAGMENT_BIT, texture.textureImageView, textureSampler));
            }

            bindings = layoutBuilder.bindings;
            descriptorSetLayout = layoutBuilder.build(application.getDevice());
        }

        if (pipelineLayout == 0L) {
            addShaderChangeListener(application, viewport);
            CreateGraphicsPipelineResult pipeline = createPipeline(application, viewport);

            pipelineLayout = pipeline.pipelineLayout;
            graphicsPipeline = pipeline.graphicsPipeline;
        }

    }

    protected void addShaderChangeListener(VulkanApplication application, Viewport viewport) {
        if (application.isDevelopmentEnvironment()) {
            Consumer<File> fileChangeListener = file -> {
                if (pipelineLayout == 0)
                    return;

                Path path = file.toPath();
                if (path.endsWith(fragmentShader) || path.endsWith(vertexShader)) {
                    CreateGraphicsPipelineResult pipeline = createPipeline(application, viewport);
                    long prevPipelineLayout = pipelineLayout;
                    long prevGraphicsPipeline = graphicsPipeline;
                    application.addTask(() -> {
                        pipelineLayout = pipeline.pipelineLayout;
                        graphicsPipeline = pipeline.graphicsPipeline;

                        vkDestroyPipeline(application.device, prevGraphicsPipeline, null);
                        vkDestroyPipelineLayout(application.device, prevPipelineLayout, null);
                    });
                }
            };
            application.addResourceChangeListener(fileChangeListener);
        }
    }

    protected CreateGraphicsPipelineResult createPipeline(VulkanApplication application, Viewport viewport) {
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
                viewport,
                descriptorSetLayout,
                vertexLayout
        );
        return pipeline;
    }

    public static class VertexLayout {
        public final List<BindingDescription> locations;
        public final int sizeof;

        public VertexLayout(BindingDescription... bindingDescriptions) {
            locations = Collections.unmodifiableList(Arrays.asList(bindingDescriptions));
            int size = 0;
            for (int i = 0; i < locations.size(); i++) {
                BindingDescription location = locations.get(i);
                size += location.size;
            }
            sizeof = size * Float.BYTES;
        }

        public int offsetOf(int i) {
            int offset = 0;
            for (int j = 0; j < i; j++) {
                offset += locations.get(j).size;
            }
            return offset * Float.BYTES;
        }

        public enum BindingDescription {
            POSITION(3),
            NORMAL(3),
            TANGENT(3),
            COLOR(3),
            TEXTURE_COORDINATES(2),
            ;

            public final int size;

            BindingDescription(int size) {
                this.size = size;
            }
        }
    }


}
