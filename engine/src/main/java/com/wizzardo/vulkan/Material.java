package com.wizzardo.vulkan;

import com.wizzardo.tools.interfaces.BiConsumer;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.material.PushConstantInfo;
import com.wizzardo.vulkan.material.SpecializationConstantInfo;
import com.wizzardo.vulkan.material.Uniform;
import com.wizzardo.vulkan.misc.ResourceChangeListener;
import org.joml.*;
import org.lwjgl.vulkan.VkDevice;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Material {
    public static final VertexLayout DEFAULT_VERTEX_LAYOUT = new VertexLayout(
            VertexLayout.BindingDescription.POSITION,
            VertexLayout.BindingDescription.COLOR,
            VertexLayout.BindingDescription.TEXTURE_COORDINATES
    );
    public static final VertexLayout EMPTY_INSTANCE_LAYOUT = new VertexLayout();

    protected String vertexShader;
    protected String fragmentShader;
    protected List<TextureImage> textures = Collections.emptyList();
    protected TextureSampler textureSampler;
    protected VertexLayout vertexLayout = DEFAULT_VERTEX_LAYOUT;
    protected VertexLayout instanceBindingLayout = EMPTY_INSTANCE_LAYOUT;
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
                instanceBindingLayout,
                constants,
                pushConstants,
                createRasterizationStateOptions(),
                createDepthStencilStateOptions(),
                descriptorSetLayouts
        );
    }

    protected RasterizationStateOptions createRasterizationStateOptions() {
        return new RasterizationStateOptions();
    }

    protected DepthStencilStateOptions createDepthStencilStateOptions() {
        return new DepthStencilStateOptions();
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
            sizeof = size;
        }

        public int offsetOf(int i) {
            int offset = 0;
            for (int j = 0; j < i; j++) {
                offset += locations.get(j).size;
            }
            return offset;
        }

        public static class BindingDescription<T> {
            public static final BindingDescription<Float> F1 = new BindingDescription<>(1 * Float.BYTES, VK_FORMAT_R32_SFLOAT, (v, buffer) -> buffer.putFloat(v));
            public static final BindingDescription<Vector2fc> F2 = new BindingDescription<>(2 * Float.BYTES, VK_FORMAT_R32G32_SFLOAT, (v, buffer) -> {
                buffer.putFloat(v.x());
                buffer.putFloat(v.y());
            });
            public static final BindingDescription<Vector3fc> F3 = new BindingDescription<>(3 * Float.BYTES, VK_FORMAT_R32G32B32_SFLOAT, (v, buffer) -> {
                buffer.putFloat(v.x());
                buffer.putFloat(v.y());
                buffer.putFloat(v.z());
            });
            public static final BindingDescription<Quaternionfc> F4 = new BindingDescription<>(4 * Float.BYTES, VK_FORMAT_R32G32B32A32_SFLOAT, (v, buffer) -> {
                buffer.putFloat(v.x());
                buffer.putFloat(v.y());
                buffer.putFloat(v.z());
                buffer.putFloat(v.w());
            });

            public static final BindingDescription<Integer> I1U = new BindingDescription<>(1 * Integer.BYTES, VK_FORMAT_R32_UINT, (v, buffer) -> buffer.putInt(v));

            public static final BindingDescription<Integer> I1 = new BindingDescription<>(1 * Integer.BYTES, VK_FORMAT_R32_SINT, (v, buffer) -> buffer.putInt(v));
            public static final BindingDescription<Vector2ic> I2 = new BindingDescription<>(2 * Integer.BYTES, VK_FORMAT_R32G32_SINT, (v, buffer) -> {
                buffer.putInt(v.x());
                buffer.putInt(v.y());
            });
            public static final BindingDescription<Vector3ic> I3 = new BindingDescription<>(3 * Integer.BYTES, VK_FORMAT_R32G32B32_SINT, (v, buffer) -> {
                buffer.putInt(v.x());
                buffer.putInt(v.y());
                buffer.putInt(v.z());
            });
            public static final BindingDescription<Vector4ic> I4 = new BindingDescription<>(4 * Integer.BYTES, VK_FORMAT_R32G32B32A32_SINT, (v, buffer) -> {
                buffer.putInt(v.x());
                buffer.putInt(v.y());
                buffer.putInt(v.z());
                buffer.putInt(v.w());
            });

            public static final BindingDescription<Integer> B4UNORM = new BindingDescription<>(4 * Byte.BYTES, VK_FORMAT_R8G8B8A8_UNORM, (v, buffer) -> buffer.putInt(v));

            public static final BindingDescription<Vertex> POSITION = new BindingDescription<>(F3, (vertex, buffer) -> {
                buffer.putFloat(vertex.pos.x());
                buffer.putFloat(vertex.pos.y());
                buffer.putFloat(vertex.pos.z());
            });
            public static final BindingDescription<Vertex> NORMAL = new BindingDescription<>(F3, (vertex, buffer) -> {
                buffer.putFloat(vertex.normal.x());
                buffer.putFloat(vertex.normal.y());
                buffer.putFloat(vertex.normal.z());
            });
            public static final BindingDescription<Vertex> TANGENT = new BindingDescription<>(F3, (vertex, buffer) -> {
                buffer.putFloat(vertex.tangent.x());
                buffer.putFloat(vertex.tangent.y());
                buffer.putFloat(vertex.tangent.z());
            });
            public static final BindingDescription<Vertex> COLOR = new BindingDescription<>(F3, (vertex, buffer) -> {
                buffer.putFloat(vertex.color.x());
                buffer.putFloat(vertex.color.y());
                buffer.putFloat(vertex.color.z());
            });
            public static final BindingDescription<Vertex> TEXTURE_COORDINATES = new BindingDescription<>(F2, (vertex, buffer) -> {
                buffer.putFloat(vertex.texCoords.x());
                buffer.putFloat(vertex.texCoords.y());
            });
            public static final BindingDescription<Vertex> BONE_INDEX = new BindingDescription<>(I4, (vertex, buffer) -> {
                buffer.putInt(vertex.boneIndexes.x());
                buffer.putInt(vertex.boneIndexes.y());
                buffer.putInt(vertex.boneIndexes.z());
                buffer.putInt(vertex.boneIndexes.w());
            });
            public static final BindingDescription<Vertex> BONE_WEIGHT = new BindingDescription<>(F4, (vertex, buffer) -> {
                buffer.putFloat(vertex.boneWeights.x());
                buffer.putFloat(vertex.boneWeights.y());
                buffer.putFloat(vertex.boneWeights.z());
                buffer.putFloat(vertex.boneWeights.w());
            });

            public final int size;
            public final int format;
            public final BiConsumer<T, ByteBuffer> bufferSetter;

//            public BindingDescription(int size, int format) {
//                this(size, format, null);
//            }

            public BindingDescription(int size, int format, BiConsumer<T, ByteBuffer> bufferSetter) {
                this.size = size;
                this.format = format;
                this.bufferSetter = bufferSetter;
            }

            public BindingDescription(BindingDescription bindingDescription, BiConsumer<T, ByteBuffer> bufferSetter) {
                this.size = bindingDescription.size;
                this.format = bindingDescription.format;
                this.bufferSetter = bufferSetter;
            }

            public void put(ByteBuffer buffer, T value) {
                bufferSetter.consume(value, buffer);
            }
        }
    }


}
