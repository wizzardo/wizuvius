package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VulkanDescriptorSets {

    public static abstract class DescriptorSetLayoutBinding {
        final int binding;
        final int type;
        final int stageFlags;

        protected DescriptorSetLayoutBinding(int binding, int type, int stageFlags) {
            this.binding = binding;
            this.type = type;
            this.stageFlags = stageFlags;
        }

        @Override
        public String toString() {
            return "DescriptorSetLayoutBinding{" +
                    "binding=" + binding +
                    ", type=" + type +
                    ", stageFlags=" + stageFlags +
                    '}';
        }
    }

    public static class DescriptorSetLayoutBindingUniformBuffer extends DescriptorSetLayoutBinding {
        final UniformBuffers uniformBuffers;

        public DescriptorSetLayoutBindingUniformBuffer(int binding, int stageFlags, UniformBuffers uniformBuffers) {
            super(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, stageFlags);
            this.uniformBuffers = uniformBuffers;
        }
    }

    public static class DescriptorSetLayoutBindingUBO extends DescriptorSetLayoutBinding {
        public DescriptorSetLayoutBindingUBO(int binding, int stageFlags) {
            super(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, stageFlags);
        }
    }

    public static class DescriptorSetLayoutBindingImageWithSampler extends DescriptorSetLayoutBinding {
        final long textureImageView;
        final long textureSampler;

        public DescriptorSetLayoutBindingImageWithSampler(int binding, int stageFlags, long textureImageView, long textureSampler) {
            super(binding, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, stageFlags);
            this.textureImageView = textureImageView;
            this.textureSampler = textureSampler;
        }
    }

    public static class DescriptorSetLayoutBuilder {
        public List<DescriptorSetLayoutBinding> bindings = new ArrayList<>();

        public DescriptorSetLayoutBuilder append(DescriptorSetLayoutBinding descriptorSetLayoutBinding) {
            Optional<DescriptorSetLayoutBinding> binding = bindings.stream().filter(it -> it.binding == descriptorSetLayoutBinding.binding).findFirst();
            if (binding.isPresent())
                throw new IllegalArgumentException("There is a binding with this value: " + binding.get());

            if (descriptorSetLayoutBinding.type < 0 || descriptorSetLayoutBinding.type > 10)
                throw new IllegalArgumentException("descriptorSetLayoutBinding.type should be >= 0 and <= 10, but was " + descriptorSetLayoutBinding.type);

            bindings.add(descriptorSetLayoutBinding);
            return this;
        }

        public long build(VkDevice device) {
            return createDescriptorSetLayout(device, bindings);
        }
    }

    public static long createDescriptorSetLayout(VkDevice device, List<DescriptorSetLayoutBinding> bindings) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindingsBuffer = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);

            for (int i = 0; i < bindings.size(); i++) {
                DescriptorSetLayoutBinding binding = bindings.get(i);
                VkDescriptorSetLayoutBinding layoutBinding = bindingsBuffer.get(i);
                layoutBinding.binding(binding.binding);
                layoutBinding.descriptorCount(1);
                layoutBinding.descriptorType(binding.type);
                layoutBinding.pImmutableSamplers(null);
                layoutBinding.stageFlags(binding.stageFlags);
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindingsBuffer);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    static long createDescriptorSetLayout(VkDevice device) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);

            VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
            uboLayoutBinding.binding(0);
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.pImmutableSamplers(null);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
            samplerLayoutBinding.binding(1);
            samplerLayoutBinding.descriptorCount(1);
            samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerLayoutBinding.pImmutableSamplers(null);
            samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    static long createDescriptorPool(VkDevice device, int size) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);

            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(size);

            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(size);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(size);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);
        }
    }


    public static class DescriptorSetsBuilder {
        List<DescriptorSetLayoutBinding> bindings;
        List<Long> uniformBuffers;

        public DescriptorSetsBuilder(List<DescriptorSetLayoutBinding> bindings) {
            this.bindings = bindings;
        }

        public DescriptorSetsBuilder withUniformBuffers(List<Long> uniformBuffers) {
            this.uniformBuffers = uniformBuffers;
            return this;
        }

        public List<Long> build(
                VkDevice device,
                List<Long> swapChainImages,
                long descriptorSetLayout,
                long descriptorPool
        ) {
            try (MemoryStack stack = stackPush()) {
                LongBuffer layouts = stack.mallocLong(swapChainImages.size());
                for (int i = 0; i < layouts.capacity(); i++) {
                    layouts.put(i, descriptorSetLayout);
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layouts);

                LongBuffer pDescriptorSets = stack.mallocLong(swapChainImages.size());

                int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets: " + result);
                }

                List<Long> descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(bindings.size(), stack);

                for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                    long descriptorSet = pDescriptorSets.get(i);
                    for (int j = 0; j < bindings.size(); j++) {
                        DescriptorSetLayoutBinding binding = bindings.get(j);

                        VkWriteDescriptorSet writeDescriptorSet = descriptorWrites.get(j);
                        writeDescriptorSet.dstSet(descriptorSet);
                        writeDescriptorSet.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                        writeDescriptorSet.dstBinding(binding.binding);
                        writeDescriptorSet.dstArrayElement(0);
                        writeDescriptorSet.descriptorType(binding.type);
                        writeDescriptorSet.descriptorCount(1);

                        if (binding.type == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER && binding instanceof DescriptorSetLayoutBindingUniformBuffer) {
                            DescriptorSetLayoutBindingUniformBuffer bindingUniformBuffer = (DescriptorSetLayoutBindingUniformBuffer) binding;
                            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                            bufferInfo.offset(0);
                            bufferInfo.range(bindingUniformBuffer.uniformBuffers.size);
                            bufferInfo.buffer(bindingUniformBuffer.uniformBuffers.uniformBuffers.get(0));
                            writeDescriptorSet.pBufferInfo(bufferInfo);
                        } else if (binding.type == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER && binding instanceof DescriptorSetLayoutBindingUBO) {
                            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                            bufferInfo.offset(0);
                            bufferInfo.range(UniformBufferObject.SIZEOF);
                            bufferInfo.buffer(uniformBuffers.get(i));
                            writeDescriptorSet.pBufferInfo(bufferInfo);
                        } else if (binding.type == VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER) {
                            DescriptorSetLayoutBindingImageWithSampler imageWithSampler = (DescriptorSetLayoutBindingImageWithSampler) binding;
                            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                            imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                            imageInfo.imageView(imageWithSampler.textureImageView);
                            imageInfo.sampler(imageWithSampler.textureSampler);
                            writeDescriptorSet.pImageInfo(imageInfo);
                        }
                    }

                    vkUpdateDescriptorSets(device, descriptorWrites, null);
                    descriptorSets.add(descriptorSet);
                }
                return descriptorSets;
            }
        }
    }

    public static List<Long> createDescriptorSets(
            VkDevice device,
            List<Long> swapChainImages,
            long descriptorSetLayout,
            long descriptorPool,
            long textureImageView,
            long textureSampler,
            List<Long> uniformBuffers
    ) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer layouts = stack.mallocLong(swapChainImages.size());
            for (int i = 0; i < layouts.capacity(); i++) {
                layouts.put(i, descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(layouts);

            LongBuffer pDescriptorSets = stack.mallocLong(swapChainImages.size());

            int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets: " + result);
            }

            List<Long> descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
            bufferInfo.offset(0);
            bufferInfo.range(UniformBufferObject.SIZEOF);

            int capacity = 1;
            boolean withTexture = textureImageView != 0;
            if (withTexture)
                capacity++;

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(capacity, stack);

            VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(0);
            uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            uboDescriptorWrite.dstBinding(0);
            uboDescriptorWrite.dstArrayElement(0);
            uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboDescriptorWrite.descriptorCount(1);
            uboDescriptorWrite.pBufferInfo(bufferInfo);

            VkWriteDescriptorSet samplerDescriptorWrite = null;

            if (withTexture) {
                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(textureImageView);
                imageInfo.sampler(textureSampler);

                samplerDescriptorWrite = descriptorWrites.get(1);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(1);
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo);
            }

            for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                long descriptorSet = pDescriptorSets.get(i);
                bufferInfo.buffer(uniformBuffers.get(i));
                uboDescriptorWrite.dstSet(descriptorSet);

                if (withTexture)
                    samplerDescriptorWrite.dstSet(descriptorSet);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
                descriptorSets.add(descriptorSet);
            }
            return descriptorSets;
        }
    }
}
