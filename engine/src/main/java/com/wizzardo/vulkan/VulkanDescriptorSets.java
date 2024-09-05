package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VulkanDescriptorSets {

    public static abstract class DescriptorSetLayoutBinding {
        final int binding;
        final int type;
        final int stageFlags;
        final int count;

        protected DescriptorSetLayoutBinding(int binding, int type, int stageFlags) {
            this(binding, type, stageFlags, 1);
        }

        protected DescriptorSetLayoutBinding(int binding, int type, int stageFlags, int count) {
            this.binding = binding;
            this.type = type;
            this.stageFlags = stageFlags;
            this.count = count;
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
        final UniformBuffer uniformBuffer;

        public DescriptorSetLayoutBindingUniformBuffer(int binding, int stageFlags, UniformBuffer uniformBuffer) {
            super(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, stageFlags);
            this.uniformBuffer = uniformBuffer;
        }
    }

    public static class DescriptorSetLayoutBindingStorageBuffer extends DescriptorSetLayoutBinding {
        final UniformBuffer uniformBuffer;

        public DescriptorSetLayoutBindingStorageBuffer(int binding, int stageFlags, UniformBuffer uniformBuffer) {
            super(binding, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, stageFlags);
            this.uniformBuffer = uniformBuffer;
        }
    }

    public static class DescriptorSetLayoutBindingUBO extends DescriptorSetLayoutBinding {
        public DescriptorSetLayoutBindingUBO(int binding, int stageFlags) {
            super(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, stageFlags);
        }
    }

    public static class DescriptorSetLayoutBindingBindlessTextures extends DescriptorSetLayoutBinding {
        public DescriptorSetLayoutBindingBindlessTextures(int binding, int stageFlags, int count) {
            super(binding, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, stageFlags, count);
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
            return createDescriptorSetLayout(device, bindings, 0, null);
        }
    }

    public static long createDescriptorSetLayout(
            VkDevice device,
            List<DescriptorSetLayoutBinding> bindings,
            int flags,
            VkDescriptorSetLayoutBindingFlagsCreateInfoEXT bindingFlagsCreateInfoEXT
    ) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindingsBuffer = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);

            for (int i = 0; i < bindings.size(); i++) {
                DescriptorSetLayoutBinding binding = bindings.get(i);
                VkDescriptorSetLayoutBinding layoutBinding = bindingsBuffer.get(i);
                layoutBinding.binding(binding.binding);
                layoutBinding.descriptorCount(binding.count);
                layoutBinding.descriptorType(binding.type);
                layoutBinding.pImmutableSamplers(null);
                layoutBinding.stageFlags(binding.stageFlags);
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindingsBuffer);
            layoutInfo.flags(flags);
            if (bindingFlagsCreateInfoEXT != null)
                layoutInfo.pNext(bindingFlagsCreateInfoEXT);

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
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(3, stack);

            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(size);

            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(size);

            VkDescriptorPoolSize storageBuffersPoolSize = poolSizes.get(2);
            storageBuffersPoolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
            storageBuffersPoolSize.descriptorCount(size);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(size);
            poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);


            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);
        }
    }

    static long createDescriptorPoolBindlessTextures(VkDevice device, int size) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, stack);

            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(0);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(size);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(size);
            poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            return pDescriptorPool.get(0);
        }
    }


    public static class DescriptorSetsBuilder {
        List<DescriptorSetLayoutBinding> bindings;
        List<UniformBuffer> uniformBuffers;
        VkDescriptorSetVariableDescriptorCountAllocateInfoEXT allocateInfoEXT;

        public DescriptorSetsBuilder(List<DescriptorSetLayoutBinding> bindings) {
            this.bindings = bindings;
        }

        public DescriptorSetsBuilder withUniformBuffers(List<UniformBuffer> uniformBuffers) {
            this.uniformBuffers = uniformBuffers;
            return this;
        }

        public DescriptorSetsBuilder allocateInfoEXT(VkDescriptorSetVariableDescriptorCountAllocateInfoEXT allocateInfoEXT){
            return this;
        }

        public List<Long> build(
                VkDevice device,
                int count,
                long descriptorSetLayout,
                long descriptorPool
        ) {
            try (MemoryStack stack = stackPush()) {
                LongBuffer layouts = stack.mallocLong(count);
                for (int i = 0; i < layouts.capacity(); i++) {
                    layouts.put(i, descriptorSetLayout);
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layouts);
                if (allocateInfoEXT != null)
                    allocInfo.pNext(allocateInfoEXT);

                LongBuffer pDescriptorSets = stack.mallocLong(count);

                int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets: " + result);
                }

                List<Long> descriptorSets = new ArrayList<>(pDescriptorSets.capacity());
                for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                    long descriptorSet = pDescriptorSets.get(i);
                    descriptorSets.add(descriptorSet);
                }

                int writes = (int) bindings.stream().filter(it -> !(it instanceof DescriptorSetLayoutBindingBindlessTextures)).count();
                if (writes > 0) {
                    VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(writes, stack);

                    for (int i = 0; i < pDescriptorSets.capacity(); i++) {
                        long descriptorSet = pDescriptorSets.get(i);
                        for (int j = 0; j < writes; j++) {
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
                                bufferInfo.range(bindingUniformBuffer.uniformBuffer.size);
                                bufferInfo.buffer(bindingUniformBuffer.uniformBuffer.address);
                                writeDescriptorSet.pBufferInfo(bufferInfo);
                            } else if (binding.type == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER && binding instanceof DescriptorSetLayoutBindingUBO) {
                                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                                bufferInfo.offset(0);
                                bufferInfo.range(uniformBuffers.get(i).size);
                                bufferInfo.buffer(uniformBuffers.get(i).address);
                                writeDescriptorSet.pBufferInfo(bufferInfo);
                            } else if (binding.type == VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER && binding instanceof DescriptorSetLayoutBindingImageWithSampler) {
                                DescriptorSetLayoutBindingImageWithSampler imageWithSampler = (DescriptorSetLayoutBindingImageWithSampler) binding;
                                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                                imageInfo.imageView(imageWithSampler.textureImageView);
                                imageInfo.sampler(imageWithSampler.textureSampler);
                                writeDescriptorSet.pImageInfo(imageInfo);
                            } else if (binding.type == VK_DESCRIPTOR_TYPE_STORAGE_BUFFER && binding instanceof DescriptorSetLayoutBindingStorageBuffer) {
                                DescriptorSetLayoutBindingStorageBuffer storageBuffer = (DescriptorSetLayoutBindingStorageBuffer) binding;
                                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                                bufferInfo.offset(0);
                                bufferInfo.range(storageBuffer.uniformBuffer.size);
                                bufferInfo.buffer(storageBuffer.uniformBuffer.address);
                                writeDescriptorSet.pBufferInfo(bufferInfo);
                            }
                        }

                        vkUpdateDescriptorSets(device, descriptorWrites, null);
                    }
                }
                return descriptorSets;
            }
        }
    }

}
