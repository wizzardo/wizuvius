package com.wizzardo.vulkan;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.material.PushConstantInfo;
import com.wizzardo.vulkan.material.SpecializationConstantInfo;
import com.wizzardo.vulkan.material.Uniform;
import com.wizzardo.vulkan.misc.ResourceChangeListener;
import org.lwjgl.vulkan.VkDevice;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
    protected TextureSampler textureSampler;
    protected VertexLayout vertexLayout = DEFAULT_VERTEX_LAYOUT;
    protected boolean withUBO = true;
    protected List<SpecializationConstantInfo> constants = Collections.emptyList();
    protected List<PushConstantInfo> pushConstants = Collections.emptyList();
    protected List<Uniform> uniforms = Collections.emptyList();

    public List<VulkanDescriptorSets.DescriptorSetLayoutBinding> bindings;
    public long descriptorSetLayout;
    public Pipeline pipeline;

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
        if (textures.contains(textureImage))
            return;

        if (textures.isEmpty())
            textures = new ArrayList<>();
        textures.add(textureImage);
    }

    public void addSpecializationConstant(SpecializationConstantInfo constantInfo) {
        if (constants.isEmpty())
            constants = new ArrayList<>();
        constants.add(constantInfo);
    }

    public void addPushConstant(PushConstantInfo constantInfo) {
        if (pushConstants.isEmpty())
            pushConstants = new ArrayList<>();
        pushConstants.add(constantInfo);
    }

    public void addUniform(Uniform uniform) {
        if (uniforms.isEmpty())
            uniforms = new ArrayList<>();
        uniforms.add(uniform);
    }

    public void updateUniforms() {
        for (int i = 0; i < uniforms.size(); i++) {
            uniforms.get(i).update();
        }
    }

    public TextureSampler getTextureSampler() {
        return textureSampler;
    }

    public void setTextureSampler(TextureSampler textureSampler) {
        this.textureSampler = textureSampler;
    }

    public void cleanupSwapChainObjects(VkDevice device) {
//        try {
//            if (graphicsPipeline != 0)
//                vkDestroyPipeline(device, graphicsPipeline, null);
//            if (pipelineLayout != 0)
//                vkDestroyPipelineLayout(device, pipelineLayout, null);
//        } finally {
//            graphicsPipeline = 0;
//            pipelineLayout = 0;
//        }
        pipeline = null;
    }

    public void prepare(VulkanApplication application, Viewport viewport) {
        for (int i = 0; i < textures.size(); i++) {
            try {
                textures.get(i).load(application);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (descriptorSetLayout == 0L) {
            VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = prepareDescriptorSetLayoutBuilder();
            bindings = layoutBuilder.bindings;
            descriptorSetLayout = layoutBuilder.build(application.getDevice());

            createDescriptorSetLayoutCleanupTask(application, descriptorSetLayout);
        }

        if (pipeline == null) {
            addShaderChangeListener(application, viewport);
            pipeline = createPipeline(application, viewport);
        }

    }

    protected void createDescriptorSetLayoutCleanupTask(VulkanApplication application, long dsl) {
        application.addCleanupTask(this, () -> {
            ResourceCleaner.printDebugInCleanupTask(Material.class);
            vkDestroyDescriptorSetLayout(application.device, dsl, null);
        });
    }

    protected VulkanDescriptorSets.DescriptorSetLayoutBuilder prepareDescriptorSetLayoutBuilder() {
        VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = new VulkanDescriptorSets.DescriptorSetLayoutBuilder();
        if (withUBO)
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingUBO(0, VK_SHADER_STAGE_VERTEX_BIT));

        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.get(i);
            uniform.update();
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingUniformBuffer(uniform.binding, uniform.stage, uniform.uniformBuffer));
        }
        for (int i = 0; i < textures.size(); i++) {
            TextureImage texture = textures.get(i);
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingImageWithSampler(layoutBuilder.bindings.size(), VK_SHADER_STAGE_FRAGMENT_BIT, texture.getTextureImageView(), textureSampler.sampler));
        }
        return layoutBuilder;
    }

    protected void addShaderChangeListener(VulkanApplication application, Viewport viewport) {
        if (application.isDevelopmentEnvironment()) {
            WeakReference<Material> materialReference = new WeakReference<>(this);
            ResourceChangeListener fileChangeListener = file -> {
                Material material = materialReference.get();
                if (material == null)
                    return false;

                if (material.pipeline == null)
                    return true;

                Path path = file.toPath();
                if (path.endsWith(material.fragmentShader) || path.endsWith(material.vertexShader)) {
                    Pipeline pipeline = material.createPipeline(application, viewport);
//                    long prevPipelineLayout = material.pipelineLayout;
//                    long prevGraphicsPipeline = material.graphicsPipeline;
                    application.addTask(() -> {
                        material.pipeline = pipeline;

//                        vkDestroyPipeline(application.device, prevGraphicsPipeline, null);
//                        vkDestroyPipelineLayout(application.device, prevPipelineLayout, null);
                    });
                }
                return true;
            };
            application.addResourceChangeListener(fileChangeListener);
        }
    }

    protected Pipeline createPipeline(VulkanApplication application, Viewport viewport) {
        ByteBuffer vertShaderSPIRV = Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(application.loadAsset(vertexShader));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });
        ByteBuffer fragShaderSPIRV = Unchecked.call(() -> {
            if (fragmentShader == null)
                return null;
            byte[] bytes = IOTools.bytes(application.loadAsset(fragmentShader));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });

        long[] descriptorSetLayouts;
        if (application.bindlessTexturePool != null)
            descriptorSetLayouts = new long[]{descriptorSetLayout, application.bindlessTexturePool.bindlessTexturesDescriptorSetLayout};
        else
            descriptorSetLayouts = new long[]{descriptorSetLayout};

        return VulkanApplication.createGraphicsPipeline(
                application.getDevice(),
                application.resourceCleaner,
                vertShaderSPIRV,
                fragShaderSPIRV,
                viewport,
                vertexLayout,
                constants,
                pushConstants,
                createRasterizationStateOptions(),
                descriptorSetLayouts
        );
    }

    protected RasterizationStateOptions createRasterizationStateOptions() {
        return new RasterizationStateOptions();
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
            BONE_INDEX(4),
            BONE_WEIGHT(4),
            ;

            public final int size;

            BindingDescription(int size) {
                this.size = size;
            }
        }
    }


}
