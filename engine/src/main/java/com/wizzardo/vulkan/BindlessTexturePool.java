package com.wizzardo.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.*;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;

public class BindlessTexturePool {
    protected int size;
    protected long bindlessTexturesDescriptorSet;
    protected long bindlessTexturesDescriptorSetLayout;
    protected long bindlessTexturesDescriptorPool;
    protected VkDevice device;
    protected List<TextureImage> images;

    public BindlessTexturePool(VkDevice device, VkPhysicalDevice physicalDevice, int size) {
        this.size = size;
        this.device = device;
        this.images = new ArrayList<>();

        try (MemoryStack stack = stackPush()) {
//            VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.calloc(stack);
//            vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
//
//            VkPhysicalDeviceLimits limits = physicalDeviceProperties.limits();

            bindlessTexturesDescriptorPool = VulkanDescriptorSets.createDescriptorPoolBindlessTextures(device, size);
            VkDescriptorSetLayoutBindingFlagsCreateInfoEXT bindingFlagsCreateInfoEXT = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc(stack);
            bindingFlagsCreateInfoEXT.bindingCount(1);
            bindingFlagsCreateInfoEXT.pBindingFlags(stack.ints(
                    VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT_EXT,
                    VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT_EXT,
                    VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT_EXT
            ));

            List<VulkanDescriptorSets.DescriptorSetLayoutBinding> bindings = Arrays.asList(
                    new VulkanDescriptorSets.DescriptorSetLayoutBindingBindlessTextures(0, VK_SHADER_STAGE_ALL, size)
            );
            bindlessTexturesDescriptorSetLayout = VulkanDescriptorSets.createDescriptorSetLayout(device,
                    bindings,
                    VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT_EXT,
                    bindingFlagsCreateInfoEXT
            );

            bindlessTexturesDescriptorSet = new VulkanDescriptorSets.DescriptorSetsBuilder(bindings)
                    .allocateInfoEXT(VkDescriptorSetVariableDescriptorCountAllocateInfoEXT.calloc(stack)
                            .pDescriptorCounts(stack.ints(size))
                    )
                    .build(device, 1, bindlessTexturesDescriptorSetLayout, bindlessTexturesDescriptorPool).get(0);
        }
    }

    public void cleanup() {
        vkDestroyDescriptorSetLayout(device, bindlessTexturesDescriptorSetLayout, null);
        vkDestroyDescriptorPool(device, bindlessTexturesDescriptorPool, null);
    }

    public void add(TextureImage textureImage, long textureSampler) {
        textureImage.index = images.size();
        images.add(textureImage);


        try (MemoryStack stack = stackPush()) {
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);

            VkWriteDescriptorSet writeDescriptorSet = descriptorWrites.get(0);
            writeDescriptorSet.dstSet(bindlessTexturesDescriptorSet);
            writeDescriptorSet.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            writeDescriptorSet.dstBinding(0);
            writeDescriptorSet.dstArrayElement(textureImage.index);
            writeDescriptorSet.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            writeDescriptorSet.descriptorCount(1);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(textureImage.textureImageView);
            imageInfo.sampler(textureSampler);
            writeDescriptorSet.pImageInfo(imageInfo);

            vkUpdateDescriptorSets(device, descriptorWrites, null);
        }
    }
}
